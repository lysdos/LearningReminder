package com.example.learningreminder

import android.content.Context
import com.example.learningreminder.data.BackupService
import com.example.learningreminder.data.ChildRepository
import com.example.learningreminder.data.LearningDatabase
import com.example.learningreminder.data.RecordRepository
import com.example.learningreminder.data.SettingsStore
import com.example.learningreminder.data.TaskRepository
import com.example.learningreminder.reminder.ReminderScheduler

/** 手动依赖容器：页面只依赖 ViewModel，ViewModel 依赖容器。 */
class AppContainer(context: Context) {
    val db = LearningDatabase.get(context)
    val taskRepository = TaskRepository(db.taskDao(), db.recordDao())
    val recordRepository = RecordRepository(db.recordDao())
    val childRepository = ChildRepository(db.childDao())
    val settings = SettingsStore(context)
    val scheduler = ReminderScheduler(context)
    val context: Context = context.applicationContext
    val backupService = BackupService(this)
}
