package com.example.learningreminder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildDao {
    @Insert
    suspend fun insert(child: ChildProfile): Long

    @Update
    suspend fun update(child: ChildProfile)

    @Query("SELECT * FROM child_profile ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<ChildProfile>>

    @Query("SELECT * FROM child_profile ORDER BY createdAt ASC")
    suspend fun getAll(): List<ChildProfile>

    @Query("SELECT * FROM child_profile WHERE id = :childId")
    suspend fun get(childId: Long): ChildProfile?

    @Query("SELECT * FROM child_profile WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): ChildProfile?

    @Query("SELECT * FROM child_profile WHERE isDefault = 1 LIMIT 1")
    fun observeDefault(): Flow<ChildProfile?>

    @Query("UPDATE child_profile SET isDefault = 0 WHERE isDefault = 1")
    suspend fun clearDefault()

    @Query("UPDATE child_profile SET isDefault = 1 WHERE id = :childId")
    suspend fun setDefault(childId: Long)

    @Query("SELECT COUNT(*) FROM learning_task WHERE childId = :childId")
    suspend fun countTasks(childId: Long): Int

    @Query("SELECT COUNT(*) FROM completion_record WHERE childId = :childId")
    suspend fun countRecords(childId: Long): Int

    @Query("DELETE FROM child_profile WHERE id = :childId")
    suspend fun delete(childId: Long)

    @Transaction
    suspend fun setDefaultExclusive(childId: Long) {
        clearDefault()
        setDefault(childId)
    }
}
