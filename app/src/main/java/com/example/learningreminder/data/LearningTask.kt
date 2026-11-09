package com.example.learningreminder.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "learning_task",
    foreignKeys = [ForeignKey(
        entity = ChildProfile::class,
        parentColumns = ["id"],
        childColumns = ["childId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["childId"])]
)
data class LearningTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val childId: Long,
    val title: String,
    val subject: String = "语文",
    val startDate: String = "2026-10-22",
    val endDate: String = "2027-01-22",
    val reminderTime: String = "19:00",
    val adjustedReminderTime: String? = null,
    val repeatRule: String = "MON,TUE,WED,THU,FRI",
    val suggestedMinutes: Int = 20,
    val completionPolicy: String = "PARENT_REPORT",
    val targetValue: String = "",
    val requiresEvidence: Boolean = false,
    val enabled: Boolean = true,
    val deleted: Boolean = false,
    val notes: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
) {
    /** 提醒使用校正时间；未配置时回退到建议开始时间。 */
    fun effectiveReminderTime(): String =
        adjustedReminderTime?.takeIf { it.isNotBlank() } ?: reminderTime
}
