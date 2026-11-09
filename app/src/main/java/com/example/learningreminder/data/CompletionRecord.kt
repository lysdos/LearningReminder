package com.example.learningreminder.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "completion_record",
    foreignKeys = [
        ForeignKey(
            entity = LearningTask::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChildProfile::class,
            parentColumns = ["id"],
            childColumns = ["childId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["childId", "taskId", "date"], unique = true)]
)
data class CompletionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val childId: Long,
    val date: String,
    val status: String,
    val completedAt: Long? = null,
    val evidenceUri: String? = null,
    val evidenceText: String? = null,
    val childNotes: String? = null,
    val parentNotes: String? = null,
    val selfInitiated: Boolean? = null,
    val exceededTarget: Boolean? = null,
    val backfilled: Boolean = false,
    val recordedBy: String = "PARENT",
    val autoStatus: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
