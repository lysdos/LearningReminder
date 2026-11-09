package com.example.learningreminder.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.learningreminder.data.LearningDatabase
import com.example.learningreminder.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 重启后只做重排（PRD 6.2：不改任务/记录）。 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderRefresher.rescheduleAll(context)
            } finally {
                pending.finish()
            }
        }
    }
}

/** 共享的重排逻辑：从 Room 读全部 enabled 任务 + 全局开关后重新注册。 */
object ReminderRefresher {
    suspend fun rescheduleAll(context: Context) {
        val db = LearningDatabase.get(context)
        val settings = SettingsStore(context)
        rescheduleAll(db, settings, ReminderScheduler(context))
    }

    suspend fun rescheduleAll(container: com.example.learningreminder.AppContainer) {
        rescheduleAll(container.db, container.settings, container.scheduler)
    }

    private suspend fun rescheduleAll(
        db: LearningDatabase,
        settings: SettingsStore,
        scheduler: ReminderScheduler
    ) {
        val preferences = settings.reminderPreferencesSnapshot()
        val tasks = db.taskDao().getAll()
        tasks.forEach { scheduler.cancel(it.id) }
        if (!preferences.enabled) return
        tasks.forEach { task ->
            scheduler.schedule(task, preferences)
        }
    }
}
