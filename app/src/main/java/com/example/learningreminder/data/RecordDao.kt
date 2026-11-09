package com.example.learningreminder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.learningreminder.data.TaskWithStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: CompletionRecord): Long

    @Update
    suspend fun update(record: CompletionRecord)

    @Query("SELECT * FROM completion_record WHERE taskId = :taskId AND date = :date")
    suspend fun get(taskId: Long, date: String): CompletionRecord?

    @Query("SELECT * FROM completion_record WHERE id = :recordId")
    suspend fun get(recordId: Long): CompletionRecord?

    @Query("SELECT * FROM completion_record WHERE childId = :childId ORDER BY date DESC")
    fun observeByChild(childId: Long): Flow<List<CompletionRecord>>

    @Query(
        "SELECT * FROM completion_record WHERE taskId = :taskId " +
            "ORDER BY date DESC LIMIT 14"
    )
    fun observeByTask(taskId: Long): Flow<List<CompletionRecord>>

    @Query(
        "SELECT * FROM completion_record WHERE childId = :childId " +
            "AND date BETWEEN :from AND :to ORDER BY date DESC"
    )
    fun observeByRange(childId: Long, from: String, to: String): Flow<List<CompletionRecord>>

    @Query(
        "SELECT * FROM completion_record WHERE childId = :childId " +
            "AND date BETWEEN :from AND :to ORDER BY date DESC, id ASC"
    )
    suspend fun getByRange(childId: Long, from: String, to: String): List<CompletionRecord>

    @Query("SELECT * FROM completion_record WHERE childId = :childId ORDER BY date DESC")
    suspend fun getAllByChild(childId: Long): List<CompletionRecord>

    @Query("SELECT * FROM completion_record ORDER BY date DESC")
    suspend fun getAll(): List<CompletionRecord>

    @Query("UPDATE completion_record SET status = :status WHERE taskId = :taskId AND date = :date")
    suspend fun setStatus(taskId: Long, date: String, status: String)

    @Query("DELETE FROM completion_record WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: Long)
}
