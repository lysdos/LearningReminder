package com.example.learningreminder.data

import android.net.Uri
import androidx.room.withTransaction
import com.example.learningreminder.AppContainer
import com.example.learningreminder.reminder.ReminderRefresher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class BackupExportSummary(
    val children: Int,
    val tasks: Int,
    val records: Int
)

data class BackupImportSummary(
    val childrenAdded: Int,
    val tasksAdded: Int,
    val recordsAdded: Int,
    val skipped: Int
)

/** 通过 SAF URI 导出/恢复完整数据；导入默认不覆盖任何已有业务数据。 */
class BackupService(private val container: AppContainer) {

    suspend fun export(uri: Uri): Result<BackupExportSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val document = BackupDocument(
                exportedAt = System.currentTimeMillis(),
                children = container.childRepository.listChildren(),
                tasks = container.taskRepository.allTasksForExport(),
                records = container.recordRepository.getAllRecordsForExport(),
                reminderPreferences = container.settings.reminderPreferencesSnapshot()
            )
            val resolver = container.context.contentResolver
            val output = runCatching { resolver.openOutputStream(uri, "rwt") }.getOrNull()
                ?: resolver.openOutputStream(uri, "w")
                ?: error("无法打开目标文件")
            output.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(JsonBackupCodec.encode(document))
            }
            BackupExportSummary(
                children = document.children.size,
                tasks = document.tasks.size,
                records = document.records.size
            )
        }
    }

    suspend fun import(uri: Uri): Result<BackupImportSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = container.context.contentResolver
            val text = resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                val content = it.readText()
                require(content.length <= MAX_BACKUP_CHARS) { "备份文件过大" }
                content
            } ?: error("无法读取备份文件")
            val document = JsonBackupCodec.decode(text)
            val summary = restore(document)
            container.settings.restoreReminderPreferences(document.reminderPreferences)
            val currentChild = container.settings.currentChildId.first()
            val allChildren = container.childRepository.listChildren()
            if (currentChild == null || allChildren.none { it.id == currentChild }) {
                val next = container.childRepository.getCurrentChild() ?: allChildren.firstOrNull()
                if (next != null) {
                    container.childRepository.setDefault(next.id)
                    container.settings.setCurrentChildId(next.id)
                }
            }
            ReminderRefresher.rescheduleAll(container)
            summary
        }
    }

    private suspend fun restore(document: BackupDocument): BackupImportSummary {
        var childrenAdded = 0
        var tasksAdded = 0
        var recordsAdded = 0
        var skipped = 0

        container.db.withTransaction {
            val existingChildren = container.db.childDao().getAll()
            val childByKey = existingChildren.associateByTo(mutableMapOf()) { childKey(it) }
            val childIdMap = mutableMapOf<Long, Long>()

            document.children.forEach { imported ->
                val key = childKey(imported)
                val mappedId = childByKey[key]?.id ?: run {
                    val newId = container.db.childDao().insert(
                        imported.copy(id = 0L, isDefault = false)
                    )
                    val inserted = imported.copy(id = newId, isDefault = false)
                    childByKey[key] = inserted
                    childrenAdded++
                    newId
                }
                childIdMap[imported.id] = mappedId
            }

            val existingTasks = container.db.taskDao().getAllIncludingDeleted()
            val taskByKey = existingTasks.associateByTo(mutableMapOf()) { taskKey(it) }
            val taskIdMap = mutableMapOf<Long, Long>()
            val taskChildMap = mutableMapOf<Long, Long>()

            document.tasks.forEach { imported ->
                val mappedChildId = childIdMap[imported.childId]
                if (mappedChildId == null) {
                    skipped++
                    return@forEach
                }
                val candidate = imported.copy(id = 0L, childId = mappedChildId)
                val existingId = taskByKey[taskKey(candidate)]?.id ?: run {
                    val newId = container.db.taskDao().insert(candidate)
                    taskByKey[taskKey(candidate)] = candidate.copy(id = newId)
                    tasksAdded++
                    newId
                }
                taskIdMap[imported.id] = existingId
                taskChildMap[imported.id] = mappedChildId
            }

            val existingRecords = container.db.recordDao().getAll()
            val recordKeys = existingRecords.mapTo(mutableSetOf()) { recordKey(it) }
            document.records.forEach { imported ->
                val mappedTaskId = taskIdMap[imported.taskId]
                val mappedChildId = childIdMap[imported.childId] ?: taskChildMap[imported.taskId]
                if (mappedTaskId == null || mappedChildId == null) {
                    skipped++
                    return@forEach
                }
                val candidate = imported.copy(
                    id = 0L,
                    taskId = mappedTaskId,
                    childId = mappedChildId
                )
                if (!recordKeys.add(recordKey(candidate))) {
                    skipped++
                    return@forEach
                }
                container.db.recordDao().insert(candidate)
                recordsAdded++
            }
        }
        return BackupImportSummary(
            childrenAdded = childrenAdded,
            tasksAdded = tasksAdded,
            recordsAdded = recordsAdded,
            skipped = skipped
        )
    }

    private fun childKey(child: ChildProfile): String =
        "${child.name.trim().lowercase()}|${child.birthDate.orEmpty().trim()}"

    private fun taskKey(task: LearningTask): String = listOf(
        task.childId,
        task.title.trim().lowercase(),
        task.startDate,
        task.endDate,
        task.reminderTime,
        task.repeatRule.uppercase()
    ).joinToString("|")

    private fun recordKey(record: CompletionRecord): String =
        "${record.childId}|${record.taskId}|${record.date}"

    companion object {
        private const val MAX_BACKUP_CHARS = 20 * 1024 * 1024
    }
}
