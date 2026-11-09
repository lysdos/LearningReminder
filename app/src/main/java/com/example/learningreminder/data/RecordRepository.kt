package com.example.learningreminder.data

import kotlinx.coroutines.flow.Flow

class RecordRepository(
    private val recordDao: RecordDao
) {
    fun observeByChild(childId: Long): Flow<List<CompletionRecord>> =
        recordDao.observeByChild(childId)

    fun observeByTask(taskId: Long): Flow<List<CompletionRecord>> =
        recordDao.observeByTask(taskId)

    fun observeRange(childId: Long, from: String, to: String): Flow<List<CompletionRecord>> =
        recordDao.observeByRange(childId, from, to)

    suspend fun getRecord(taskId: Long, date: String): CompletionRecord? =
        recordDao.get(taskId, date)

    /** 导出用：一次拉取全部孩子的全部记录。 */
    suspend fun getAllRecordsForExport(): List<CompletionRecord> = recordDao.getAll()

    /**
     * 设置某任务某天的状态。同任务同天只保留一条：
     * 先 IGNORE 插入，若冲突（返回 -1）则查回旧记录更新，不产生重复行。
     */
    suspend fun setStatus(
        taskId: Long,
        childId: Long,
        date: String,
        status: String,
        completedAt: Long?,
        childNotes: String?,
        parentNotes: String?,
        recordedBy: String,
        evidenceUri: String? = null,
        evidenceText: String? = null,
        selfInitiated: Boolean? = null,
        exceededTarget: Boolean? = null,
        backfilled: Boolean = false,
        autoStatus: Boolean = false
    ): CompletionRecord {
        val now = System.currentTimeMillis()
        val insertedId = recordDao.insert(
            CompletionRecord(
                taskId = taskId,
                childId = childId,
                date = date,
                status = status,
                completedAt = completedAt,
                evidenceUri = evidenceUri,
                evidenceText = evidenceText,
                childNotes = childNotes,
                parentNotes = parentNotes,
                selfInitiated = selfInitiated,
                exceededTarget = exceededTarget,
                backfilled = backfilled,
                recordedBy = recordedBy,
                autoStatus = autoStatus,
                lastUpdated = now
            )
        )
        if (insertedId != -1L) {
            return recordDao.get(insertedId) ?: error("record vanished after insert")
        }
        val existing = recordDao.get(taskId, date) ?: error("unique record missing")
        val merged = existing.copy(
            status = status,
            completedAt = completedAt,
            evidenceUri = evidenceUri,
            evidenceText = evidenceText,
            childNotes = childNotes,
            parentNotes = parentNotes,
            selfInitiated = selfInitiated,
            exceededTarget = exceededTarget,
            backfilled = backfilled,
            recordedBy = recordedBy,
            autoStatus = autoStatus,
            lastUpdated = now
        )
        recordDao.update(merged)
        return merged
    }

    /**
     * 快速打卡只改变状态，不清空用户已经填写的备注、证据和完成属性。
     */
    suspend fun updateStatusPreservingDetails(
        taskId: Long,
        childId: Long,
        date: String,
        status: String,
        completedAt: Long?,
        recordedBy: String
    ): CompletionRecord {
        val existing = recordDao.get(taskId, date)
        if (existing == null) {
            return setStatus(
                taskId = taskId,
                childId = childId,
                date = date,
                status = status,
                completedAt = completedAt,
                childNotes = null,
                parentNotes = null,
                recordedBy = recordedBy
            )
        }
        val updated = existing.copy(
            status = status,
            completedAt = completedAt,
            recordedBy = recordedBy,
            autoStatus = false,
            lastUpdated = System.currentTimeMillis()
        )
        recordDao.update(updated)
        return updated
    }

    suspend fun autoMark(
        taskId: Long,
        childId: Long,
        date: String,
        status: String
    ): CompletionRecord {
        val now = System.currentTimeMillis()
        val insertedId = recordDao.insert(
            CompletionRecord(
                taskId = taskId,
                childId = childId,
                date = date,
                status = status,
                recordedBy = "SYSTEM",
                autoStatus = true,
                lastUpdated = now
            )
        )
        if (insertedId != -1L) {
            return recordDao.get(insertedId) ?: error("record vanished after insert")
        }
        val existing = recordDao.get(taskId, date) ?: error("unique record missing")
        return existing
    }

    /**
     * 仅在没有任何人工记录时写入超时状态。已有自动状态可以升级为超时，
     * 但家长/孩子手工标记过的记录永不覆盖。
     */
    suspend fun markExpiredIfEligible(
        taskId: Long,
        childId: Long,
        date: String
    ): CompletionRecord? {
        val existing = recordDao.get(taskId, date)
        if (existing == null) {
            return autoMark(taskId, childId, date, "EXPIRED")
        }
        if (!existing.autoStatus ||
            existing.status !in setOf("NOT_STARTED", "IN_PROGRESS")
        ) {
            return null
        }
        val updated = existing.copy(
            status = "EXPIRED",
            lastUpdated = System.currentTimeMillis()
        )
        recordDao.update(updated)
        return updated
    }

    suspend fun updateNotes(recordId: Long, childNotes: String?, parentNotes: String?) {
        val existing = recordDao.get(recordId) ?: return
        recordDao.update(
            existing.copy(
                childNotes = childNotes ?: existing.childNotes,
                parentNotes = parentNotes ?: existing.parentNotes,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }

    suspend fun addEvidence(recordId: Long, evidenceUri: String) {
        val existing = recordDao.get(recordId) ?: return
        recordDao.update(
            existing.copy(
                evidenceUri = evidenceUri,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }
}
