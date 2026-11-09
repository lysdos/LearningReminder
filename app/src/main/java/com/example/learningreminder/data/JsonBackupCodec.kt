package com.example.learningreminder.data

import org.json.JSONArray
import org.json.JSONObject

data class BackupDocument(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val exportedAt: Long,
    val children: List<ChildProfile>,
    val tasks: List<LearningTask>,
    val records: List<CompletionRecord>,
    val reminderPreferences: ReminderPreferences
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
    }
}

/** JSON 编解码集中在这里，导出与恢复使用同一套字段定义。 */
object JsonBackupCodec {

    fun encode(document: BackupDocument): String {
        val root = JSONObject()
            .put("formatVersion", document.formatVersion)
            .put("exportedAt", document.exportedAt)
            .put("children", JSONArray().apply {
                document.children.forEach { put(childJson(it)) }
            })
            .put("tasks", JSONArray().apply {
                document.tasks.forEach { put(taskJson(it)) }
            })
            .put("records", JSONArray().apply {
                document.records.forEach { put(recordJson(it)) }
            })
            .put("reminderPreferences", preferencesJson(document.reminderPreferences))
        return root.toString(2)
    }

    fun decode(text: String): BackupDocument {
        val root = JSONObject(text)
        val version = root.optInt("formatVersion", BackupDocument.CURRENT_FORMAT_VERSION)
        require(version <= BackupDocument.CURRENT_FORMAT_VERSION) {
            "备份格式版本 $version 高于当前 App 支持的版本"
        }
        require(version >= 1) { "不支持的备份格式版本：$version" }

        val children = root.optJSONArray("children").objects().map(::parseChild)
        val tasks = root.optJSONArray("tasks").objects().map(::parseTask)
        val records = root.optJSONArray("records").objects().map(::parseRecord)
        val preferences = root.optJSONObject("reminderPreferences")
            ?.let(::parsePreferences)
            ?: ReminderPreferences()
        return BackupDocument(
            formatVersion = version,
            exportedAt = root.optLong("exportedAt", 0L),
            children = children,
            tasks = tasks,
            records = records,
            reminderPreferences = preferences
        )
    }

    private fun childJson(child: ChildProfile): JSONObject = JSONObject()
        .put("id", child.id)
        .put("name", child.name)
        .putNullable("birthDate", child.birthDate)
        .putNullable("avatarUri", child.avatarUri)
        .put("createdAt", child.createdAt)
        .put("isDefault", child.isDefault)

    private fun taskJson(task: LearningTask): JSONObject = JSONObject()
        .put("id", task.id)
        .put("childId", task.childId)
        .put("title", task.title)
        .put("subject", task.subject)
        .put("startDate", task.startDate)
        .put("endDate", task.endDate)
        .put("reminderTime", task.reminderTime)
        .putNullable("adjustedReminderTime", task.adjustedReminderTime)
        .put("repeatRule", task.repeatRule)
        .put("suggestedMinutes", task.suggestedMinutes)
        .put("completionPolicy", task.completionPolicy)
        .put("targetValue", task.targetValue)
        .put("requiresEvidence", task.requiresEvidence)
        .put("enabled", task.enabled)
        .put("deleted", task.deleted)
        .put("notes", task.notes)
        .put("lastUpdated", task.lastUpdated)

    private fun recordJson(record: CompletionRecord): JSONObject = JSONObject()
        .put("id", record.id)
        .put("taskId", record.taskId)
        .put("childId", record.childId)
        .put("date", record.date)
        .put("status", record.status)
        .putNullable("completedAt", record.completedAt)
        .putNullable("evidenceUri", record.evidenceUri)
        .putNullable("evidenceText", record.evidenceText)
        .putNullable("childNotes", record.childNotes)
        .putNullable("parentNotes", record.parentNotes)
        .putNullable("selfInitiated", record.selfInitiated)
        .putNullable("exceededTarget", record.exceededTarget)
        .put("backfilled", record.backfilled)
        .put("recordedBy", record.recordedBy)
        .put("autoStatus", record.autoStatus)
        .put("lastUpdated", record.lastUpdated)

    private fun preferencesJson(preferences: ReminderPreferences): JSONObject = JSONObject()
        .put("enabled", preferences.enabled)
        .put("weekdays", JSONArray(preferences.weekdays.sorted()))
        .put("leadMinutes", preferences.leadMinutes)
        .put("snoozeMinutes", preferences.snoozeMinutes)
        .put("followUpEnabled", preferences.followUpEnabled)
        .put("followUpMinutes", preferences.followUpMinutes)
        .put("showOnLockScreen", preferences.showOnLockScreen)
        .put("silentNotifications", preferences.silentNotifications)

    private fun parseChild(json: JSONObject): ChildProfile = ChildProfile(
        id = json.optLong("id", 0L),
        name = json.optString("name").trim().ifBlank { "未命名孩子" },
        birthDate = json.optionalString("birthDate"),
        avatarUri = json.optionalString("avatarUri"),
        createdAt = json.optLong("createdAt", System.currentTimeMillis()),
        isDefault = json.optBoolean("isDefault", false)
    )

    private fun parseTask(json: JSONObject): LearningTask = LearningTask(
        id = json.optLong("id", 0L),
        childId = json.optLong("childId", 0L),
        title = json.optString("title").trim().ifBlank { "未命名任务" },
        subject = json.optString("subject", "未分类"),
        startDate = json.optString("startDate", "1970-01-01"),
        endDate = json.optString("endDate", "9999-12-31"),
        reminderTime = json.optString("reminderTime", "19:00"),
        adjustedReminderTime = json.optionalString("adjustedReminderTime"),
        repeatRule = json.optString("repeatRule", "DAILY"),
        suggestedMinutes = json.optInt("suggestedMinutes", 20),
        completionPolicy = json.optString("completionPolicy", "PARENT_REPORT"),
        targetValue = json.optString("targetValue", ""),
        requiresEvidence = json.optBoolean("requiresEvidence", false),
        enabled = json.optBoolean("enabled", true),
        deleted = json.optBoolean("deleted", false),
        notes = json.optString("notes", ""),
        lastUpdated = json.optLong("lastUpdated", System.currentTimeMillis())
    )

    private fun parseRecord(json: JSONObject): CompletionRecord = CompletionRecord(
        id = json.optLong("id", 0L),
        taskId = json.optLong("taskId", 0L),
        childId = json.optLong("childId", 0L),
        date = json.optString("date", "1970-01-01"),
        status = json.optString("status", "NOT_STARTED"),
        completedAt = json.optionalLong("completedAt"),
        evidenceUri = json.optionalString("evidenceUri"),
        evidenceText = json.optionalString("evidenceText"),
        childNotes = json.optionalString("childNotes"),
        parentNotes = json.optionalString("parentNotes"),
        selfInitiated = json.optionalBoolean("selfInitiated"),
        exceededTarget = json.optionalBoolean("exceededTarget"),
        backfilled = json.optBoolean("backfilled", false),
        recordedBy = json.optString("recordedBy", "PARENT"),
        autoStatus = json.optBoolean("autoStatus", false),
        lastUpdated = json.optLong("lastUpdated", System.currentTimeMillis())
    )

    private fun parsePreferences(json: JSONObject): ReminderPreferences {
        val weekdays = json.optJSONArray("weekdays")?.strings()?.toSet()
            ?: ReminderPreferences.DEFAULT_WEEKDAYS
        return ReminderPreferences(
            enabled = json.optBoolean("enabled", true),
            weekdays = weekdays.filter { it in VALID_WEEKDAYS }.toSet(),
            leadMinutes = json.optInt(
                "leadMinutes",
                ReminderPreferences.DEFAULT_LEAD_MINUTES
            ).takeIf { it in ReminderPreferences.LEAD_MINUTE_OPTIONS }
                ?: ReminderPreferences.DEFAULT_LEAD_MINUTES,
            snoozeMinutes = json.optInt(
                "snoozeMinutes",
                ReminderPreferences.DEFAULT_SNOOZE_MINUTES
            ).coerceIn(5, 30),
            followUpEnabled = json.optBoolean("followUpEnabled", true),
            followUpMinutes = json.optInt(
                "followUpMinutes",
                ReminderPreferences.DEFAULT_FOLLOW_UP_MINUTES
            ).takeIf { it in ReminderPreferences.FOLLOW_UP_MINUTE_OPTIONS }
                ?: ReminderPreferences.DEFAULT_FOLLOW_UP_MINUTES,
            showOnLockScreen = json.optBoolean("showOnLockScreen", true),
            silentNotifications = json.optBoolean("silentNotifications", false)
        )
    }

    private fun JSONObject.putNullable(key: String, value: Any?): JSONObject =
        put(key, value ?: JSONObject.NULL)

    private fun JSONObject.optionalString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

    private fun JSONObject.optionalLong(key: String): Long? =
        if (!has(key) || isNull(key)) null else optLong(key)

    private fun JSONObject.optionalBoolean(key: String): Boolean? =
        if (!has(key) || isNull(key)) null else optBoolean(key)

    private fun JSONArray?.objects(): List<JSONObject> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optJSONObject(it) }
    }

    private fun JSONArray.strings(): List<String> =
        (0 until length()).mapNotNull { index ->
            optString(index).takeIf { it.isNotBlank() }?.uppercase()
        }

    private val VALID_WEEKDAYS =
        setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
}
