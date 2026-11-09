package com.example.learningreminder.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.learningreminder.MainActivity
import com.example.learningreminder.R
import com.example.learningreminder.data.LearningDatabase
import com.example.learningreminder.data.LearningTask
import com.example.learningreminder.data.ReminderPreferences
import com.example.learningreminder.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_DONE -> handleDone(context, intent)
                    ACTION_SNOOZE -> handleSnooze(context, intent)
                    else -> handleAlarm(context, intent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleDone(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) return
        val date = reminderDate(intent)
        val db = LearningDatabase.get(context)
        val task = db.taskDao().get(taskId)
        if (task != null) {
            db.recordDao().let { recordDao ->
                val existing = recordDao.get(taskId, date.toString())
                val now = System.currentTimeMillis()
                if (existing == null) {
                    recordDao.insert(
                        com.example.learningreminder.data.CompletionRecord(
                            taskId = task.id,
                            childId = task.childId,
                            date = date.toString(),
                            status = "DONE",
                            completedAt = now,
                            recordedBy = "CHILD",
                            autoStatus = false,
                            lastUpdated = now
                        )
                    )
                } else {
                    recordDao.update(
                        existing.copy(
                            status = "DONE",
                            completedAt = now,
                            recordedBy = "CHILD",
                            autoStatus = false,
                            lastUpdated = now
                        )
                    )
                }
            }
        }
        ReminderScheduler(context).apply {
            cancelSlot(taskId, ReminderKind.FOLLOW_UP)
            cancelSlot(taskId, ReminderKind.SNOOZE)
        }
        cancelNotifications(context, taskId)
    }

    private suspend fun handleSnooze(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) return
        val db = LearningDatabase.get(context)
        val task = db.taskDao().get(taskId) ?: run {
            ReminderScheduler(context).cancel(taskId)
            cancelNotifications(context, taskId)
            return
        }
        val preferences = SettingsStore(context).reminderPreferencesSnapshot()
        val date = reminderDate(intent)
        val record = db.recordDao().get(taskId, date.toString())
        val scheduler = ReminderScheduler(context)
        scheduler.cancelSlot(taskId, ReminderKind.FOLLOW_UP)
        cancelNotifications(context, taskId)
        if (!preferences.enabled || !task.enabled || task.deleted) {
            scheduler.cancel(taskId)
            return
        }
        if (isHandled(record?.status)) return
        scheduler.scheduleSnooze(task, date, preferences.snoozeMinutes)
    }

    private suspend fun handleAlarm(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) return
        val kind = reminderKind(intent)
        val date = reminderDate(intent)
        val db = LearningDatabase.get(context)
        val task = db.taskDao().get(taskId) ?: run {
            ReminderScheduler(context).cancel(taskId)
            cancelNotifications(context, taskId)
            return
        }
        val preferences = SettingsStore(context).reminderPreferencesSnapshot()
        val scheduler = ReminderScheduler(context)
        if (!preferences.enabled || !task.enabled || task.deleted) {
            scheduler.cancel(taskId)
            cancelNotifications(context, taskId)
            return
        }

        if (kind == ReminderKind.REGULAR && !ReminderPlanner.isDueOn(task, date, preferences)) {
            scheduler.schedule(task, preferences)
            cancelNotifications(context, taskId)
            return
        }

        val record = db.recordDao().get(taskId, date.toString())
        val handled = isHandled(record?.status)
        if (kind == ReminderKind.REGULAR) {
            // 无论本次是否因已完成而静默，都继续排下一周期，避免提醒链中断。
            scheduler.schedule(task, preferences)
            if (!handled && preferences.followUpEnabled) {
                val dueAtMillis = ReminderPlanner.dueDateTime(task, date)
                    ?.atZone(ZoneId.systemDefault())
                    ?.toInstant()
                    ?.toEpochMilli()
                    ?: System.currentTimeMillis()
                scheduler.scheduleFollowUp(
                    task = task,
                    reminderDate = date,
                    minutes = preferences.followUpMinutes,
                    baseMillis = maxOf(System.currentTimeMillis(), dueAtMillis)
                )
            }
        }
        if (handled) {
            scheduler.cancelSlot(taskId, ReminderKind.FOLLOW_UP)
            scheduler.cancelSlot(taskId, ReminderKind.SNOOZE)
            cancelNotifications(context, taskId)
            return
        }

        showNotification(
            context = context,
            task = task,
            kind = kind,
            reminderDate = date,
            preferences = preferences,
            allowQuickDone = !task.requiresEvidence
        )
    }

    private fun showNotification(
        context: Context,
        task: LearningTask,
        kind: ReminderKind,
        reminderDate: LocalDate,
        preferences: ReminderPreferences,
        allowQuickDone: Boolean
    ) {
        ensureChannels(context)
        val taskId = task.id
        val channelId = when {
            preferences.silentNotifications && kind == ReminderKind.SNOOZE ->
                CHANNEL_ID_SNOOZE_SILENT
            preferences.silentNotifications -> CHANNEL_ID_SILENT
            kind == ReminderKind.SNOOZE -> CHANNEL_ID_SNOOZE
            else -> CHANNEL_ID
        }

        // 点击主体进入 App 的任务详情。
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_SHOW_TASK_ID, taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPending = PendingIntent.getActivity(
            context,
            actionRequestCode(taskId, kind, CONTENT_ACTION_SLOT),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_DONE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_REMINDER_KIND, kind.name)
            putExtra(EXTRA_REMINDER_DATE, reminderDate.toString())
        }
        val donePending = PendingIntent.getBroadcast(
            context,
            actionRequestCode(taskId, kind, DONE_ACTION_SLOT),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_REMINDER_KIND, kind.name)
            putExtra(EXTRA_REMINDER_DATE, reminderDate.toString())
        }
        val snoozePending = PendingIntent.getBroadcast(
            context,
            actionRequestCode(taskId, kind, SNOOZE_ACTION_SLOT),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val reminderTime = task.effectiveReminderTime()
        val body = when (kind) {
            ReminderKind.SNOOZE -> "稍后提醒：${task.subject} · $reminderTime"
            ReminderKind.FOLLOW_UP -> "到点仍未完成：${task.subject} · $reminderTime"
            ReminderKind.REGULAR -> if (preferences.leadMinutes > 0) {
                "${task.subject} · 将在 $reminderTime 开始（提前 ${preferences.leadMinutes} 分钟）"
            } else {
                "${task.subject} · $reminderTime"
            }
        }
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(task.title)
            .setContentText(body)
            .setContentIntent(contentPending)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(
                if (preferences.showOnLockScreen) {
                    NotificationCompat.VISIBILITY_PUBLIC
                } else {
                    NotificationCompat.VISIBILITY_SECRET
                }
            )
            .setSilent(preferences.silentNotifications)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.notification_snooze_action), snoozePending)
        if (allowQuickDone) {
            builder.addAction(0, context.getString(R.string.notification_done_action), donePending)
        }

        val manager = manager(context)
        val notificationsAllowed = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        if (notificationsAllowed && manager.areNotificationsEnabled()) {
            runCatching {
                manager.notify(notificationId(taskId, kind), builder.build())
            }
        }
    }

    private fun manager(context: Context) =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun ensureChannels(context: Context) {
        val manager = manager(context)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_reminder_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = context.getString(R.string.channel_reminder_desc) }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID_SNOOZE,
                context.getString(R.string.channel_snooze_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.channel_snooze_desc) }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID_SILENT,
                context.getString(R.string.channel_reminder_silent_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_reminder_silent_desc)
                setSound(null, null)
                enableVibration(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID_SNOOZE_SILENT,
                context.getString(R.string.channel_snooze_silent_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_snooze_silent_desc)
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    private fun reminderKind(intent: Intent): ReminderKind =
        runCatching {
            ReminderKind.valueOf(
                intent.getStringExtra(EXTRA_REMINDER_KIND) ?: ReminderKind.REGULAR.name
            )
        }.getOrDefault(ReminderKind.REGULAR)

    private fun reminderDate(intent: Intent): LocalDate =
        intent.getStringExtra(EXTRA_REMINDER_DATE)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: LocalDate.now()

    private fun isHandled(status: String?): Boolean =
        status in setOf("DONE", "PARTIAL", "ASK_LEAVE", "SKIP_TODAY", "NO_SHOW")

    private fun cancelNotifications(context: Context, taskId: Long) {
        val manager = manager(context)
        ReminderKind.entries.forEach { manager.cancel(notificationId(taskId, it)) }
    }

    companion object {
        const val CHANNEL_ID = "learning_reminder"
        const val CHANNEL_ID_SNOOZE = "learning_reminder_snooze"
        const val CHANNEL_ID_SILENT = "learning_reminder_silent"
        const val CHANNEL_ID_SNOOZE_SILENT = "learning_reminder_snooze_silent"
        const val ACTION_REMINDER = "com.example.learningreminder.action.REMINDER"
        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_SUBJECT = "subject"
        const val EXTRA_REMINDER_KIND = "reminderKind"
        const val EXTRA_REMINDER_DATE = "reminderDate"
        const val ACTION_DONE = "com.example.learningreminder.action.DONE"
        const val ACTION_SNOOZE = "com.example.learningreminder.action.SNOOZE"
        private const val CONTENT_ACTION_SLOT = 0
        private const val DONE_ACTION_SLOT = 1
        private const val SNOOZE_ACTION_SLOT = 2

        fun notificationId(taskId: Long, kind: ReminderKind): Int =
            (taskId and 0x00FF_FFFF).toInt() + kind.ordinal * 20_000_000

        private fun actionRequestCode(
            taskId: Long,
            kind: ReminderKind,
            actionSlot: Int
        ): Int = (taskId and 0x00FF_FFFF).toInt() * 8 + kind.ordinal * 3 + actionSlot
    }
}
