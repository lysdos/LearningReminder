package com.example.learningreminder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** 今日任务 + 当天完成状态（PRD 6.2 典型查询的 LEFT JOIN 形态）。 */
data class TaskWithStatus(
    val task: LearningTask,
    val record: CompletionRecord?
)

@Dao
interface TaskDao {
    @Insert
    suspend fun insert(task: LearningTask): Long

    @Update
    suspend fun update(task: LearningTask)

    @Query("SELECT * FROM learning_task WHERE deleted = 0 ORDER BY reminderTime ASC")
    fun observeAll(): Flow<List<LearningTask>>

    @Query(
        "SELECT * FROM learning_task WHERE childId = :childId AND deleted = 0 " +
            "ORDER BY reminderTime ASC"
    )
    fun observeByChild(childId: Long): Flow<List<LearningTask>>

    @Query(
        "SELECT * FROM learning_task WHERE childId = :childId " +
            "ORDER BY reminderTime ASC"
    )
    fun observeByChildIncludingDeleted(childId: Long): Flow<List<LearningTask>>

    @Query("SELECT * FROM learning_task WHERE deleted = 0 ORDER BY reminderTime ASC")
    suspend fun getAll(): List<LearningTask>

    @Query("SELECT * FROM learning_task ORDER BY reminderTime ASC")
    suspend fun getAllIncludingDeleted(): List<LearningTask>

    @Query("SELECT * FROM learning_task WHERE childId = :childId AND deleted = 0")
    suspend fun getByChild(childId: Long): List<LearningTask>

    @Query(
        """
        SELECT t.* FROM learning_task t
        WHERE t.childId = :childId AND t.enabled = 1 AND t.deleted = 0
          AND (t.startDate IS NULL OR t.startDate <= :date)
          AND (t.endDate IS NULL OR t.endDate >= :date)
        ORDER BY t.reminderTime ASC
        """
    )
    fun observeActiveByChildAndDate(childId: Long, date: String): Flow<List<LearningTask>>

    @Query("SELECT * FROM learning_task WHERE id = :taskId AND deleted = 0")
    suspend fun get(taskId: Long): LearningTask?

    @Query("SELECT * FROM learning_task WHERE id = :taskId")
    suspend fun getIncludingDeleted(taskId: Long): LearningTask?

    @Query("UPDATE learning_task SET enabled = :enabled WHERE id = :taskId AND deleted = 0")
    suspend fun setEnabled(taskId: Long, enabled: Boolean)

    @Query(
        "UPDATE learning_task SET deleted = 1, enabled = 0, lastUpdated = :updatedAt " +
            "WHERE id = :taskId"
    )
    suspend fun softDelete(taskId: Long, updatedAt: Long)

    @Query("DELETE FROM learning_task WHERE id = :taskId")
    suspend fun delete(taskId: Long)
}
