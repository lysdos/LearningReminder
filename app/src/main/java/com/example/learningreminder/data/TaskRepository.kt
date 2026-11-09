package com.example.learningreminder.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val recordDao: RecordDao
) {
    fun observeTasks(): Flow<List<LearningTask>> = taskDao.observeAll()

    fun observeByChild(childId: Long): Flow<List<LearningTask>> = taskDao.observeByChild(childId)

    /** 报表需要历史任务元数据，即使任务已被软删除。 */
    fun observeByChildIncludingDeleted(childId: Long): Flow<List<LearningTask>> =
        taskDao.observeByChildIncludingDeleted(childId)

    suspend fun get(taskId: Long): LearningTask? = taskDao.get(taskId)

    suspend fun getIncludingDeleted(taskId: Long): LearningTask? = taskDao.getIncludingDeleted(taskId)

    suspend fun insert(task: LearningTask): Long = taskDao.insert(task)

    suspend fun update(task: LearningTask) = taskDao.update(task)

    suspend fun delete(taskId: Long, keepRecords: Boolean) {
        if (keepRecords) {
            taskDao.softDelete(taskId, System.currentTimeMillis())
        } else {
            recordDao.deleteForTask(taskId)
            taskDao.delete(taskId)
        }
    }

    suspend fun setEnabled(taskId: Long, enabled: Boolean) = taskDao.setEnabled(taskId, enabled)

    /** 供重启/进入 App 时重排提醒：全部任务交给 Scheduler 过滤。 */
    suspend fun allTasks(): List<LearningTask> = taskDao.getAll()

    /** 导出和报表回看需要保留软删除任务，避免历史记录失去标题与科目。 */
    suspend fun allTasksForExport(): List<LearningTask> = taskDao.getAllIncludingDeleted()

    suspend fun tasksForChild(childId: Long): List<LearningTask> = taskDao.getByChild(childId)

    fun observeRecords(): Flow<List<CompletionRecord>> = recordDao.observeByChild(0)

    fun observeRecordsByChild(childId: Long): Flow<List<CompletionRecord>> =
        recordDao.observeByChild(childId)
}
