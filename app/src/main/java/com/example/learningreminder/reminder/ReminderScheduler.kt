package com.example.learningreminder.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.learningreminder.data.LearningTask
import com.example.learningreminder.data.ReminderPreferences
import java.time.LocalDate
import java.time.ZoneId

enum class ReminderKind {
    REGULAR,
    SNOOZE,
    FOLLOW_UP
}

/**
 * Room 是提醒的数据源。普通周期、手动稍后和到点补提醒使用独立 requestCode，
 * 任一任务最多保留三个待触发闹钟。
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** 取消旧闹钟后按最新设置排下一次周期提醒。没有可触发日期时保持取消状态。 */
    fun schedule(task: LearningTask, preferences: ReminderPreferences): Boolean {
        cancel(task.id)
        val occurrence = ReminderPlanner.nextOccurrence(task, preferences) ?: return false
        val triggerAt = occurrence.triggerAt
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        scheduleAt(
            task = task,
            triggerAt = triggerAt,
            kind = ReminderKind.REGULAR,
            reminderDate = occurrence.reminderDate
        )
        return true
    }

    fun scheduleSnooze(
        task: LearningTask,
        reminderDate: LocalDate,
        minutes: Int,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        cancelSlot(task.id, ReminderKind.SNOOZE)
        cancelSlot(task.id, ReminderKind.FOLLOW_UP)
        scheduleAt(
            task = task,
            triggerAt = nowMillis + minutes.coerceIn(5, 30) * 60_000L,
            kind = ReminderKind.SNOOZE,
            reminderDate = reminderDate
        )
    }

    fun scheduleFollowUp(
        task: LearningTask,
        reminderDate: LocalDate,
        minutes: Int,
        baseMillis: Long = System.currentTimeMillis()
    ) {
        cancelSlot(task.id, ReminderKind.FOLLOW_UP)
        scheduleAt(
            task = task,
            triggerAt = baseMillis + minutes.coerceAtLeast(1) * 60_000L,
            kind = ReminderKind.FOLLOW_UP,
            reminderDate = reminderDate
        )
    }

    fun cancel(taskId: Long) {
        ReminderKind.entries.forEach { cancelSlot(taskId, it) }
    }

    fun cancelSlot(taskId: Long, kind: ReminderKind) {
        pendingIntent(taskId, kind, create = false)?.let(alarmManager::cancel)
    }

    /** minSdk 26：API < 31 无需声明权限即可精确闹钟；31+ 需 SCHEDULE_EXACT_ALARM 授权。 */
    fun canScheduleExact(): Boolean {
        return if (Build.VERSION.SDK_INT >= 31) {
            alarmManager.canScheduleExactAlarms()
        } else true
    }

    private fun scheduleAt(
        task: LearningTask,
        triggerAt: Long,
        kind: ReminderKind,
        reminderDate: LocalDate
    ) {
        val pending = requireNotNull(
            pendingIntent(task.id, kind, create = true) {
                putExtra(ReminderReceiver.EXTRA_TASK_ID, task.id)
                putExtra(ReminderReceiver.EXTRA_TITLE, task.title)
                putExtra(ReminderReceiver.EXTRA_SUBJECT, task.subject)
                putExtra(ReminderReceiver.EXTRA_REMINDER_KIND, kind.name)
                putExtra(ReminderReceiver.EXTRA_REMINDER_DATE, reminderDate.toString())
            }
        )
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            // 精确闹钟权限受限时降级为近似提醒（PRD 3.4 验收），仍尽力送达。
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun pendingIntent(
        taskId: Long,
        kind: ReminderKind,
        create: Boolean,
        configure: Intent.() -> Unit = {}
    ): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "${ReminderReceiver.ACTION_REMINDER}.${kind.name}"
            configure()
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(context, requestCode(taskId, kind), intent, flags)
    }

    private fun requestCode(taskId: Long, kind: ReminderKind): Int {
        val base = (taskId and 0x0FFF_FFFF).toInt() * ReminderKind.entries.size
        return base + kind.ordinal
    }
}
