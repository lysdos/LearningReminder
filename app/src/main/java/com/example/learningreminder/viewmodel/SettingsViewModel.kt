package com.example.learningreminder.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learningreminder.AppContainer
import com.example.learningreminder.data.BackupExportSummary
import com.example.learningreminder.data.BackupImportSummary
import com.example.learningreminder.data.ChildProfile
import com.example.learningreminder.reminder.ReminderRefresher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val container: AppContainer
) : ViewModel() {

    val children: StateFlow<List<ChildProfile>> = container.childRepository.observeChildren()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentChildId: StateFlow<Long?> = container.settings.currentChildId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val reminderEnabled: StateFlow<Boolean> = container.settings.reminderEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val remindWeekdays: StateFlow<Set<String>> = container.settings.remindWeekdays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"))

    val leadMinutes: StateFlow<Int> = container.settings.leadMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val snoozeMinutes: StateFlow<Int> = container.settings.snoozeMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val followUpEnabled: StateFlow<Boolean> = container.settings.followUpEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val followUpMinutes: StateFlow<Int> = container.settings.followUpMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val showOnLockScreen: StateFlow<Boolean> = container.settings.showOnLockScreen
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val silentNotifications: StateFlow<Boolean> = container.settings.silentNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun addChild(name: String, birthDate: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = children.first()
            val isFirst = list.isEmpty()
            val id = container.childRepository.addChild(name, birthDate)
            if (isFirst) container.childRepository.setDefault(id)
            container.settings.setCurrentChildId(id)
        }
    }

    fun selectChild(childId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            container.settings.setCurrentChildId(childId)
            container.childRepository.setDefault(childId)
        }
    }

    fun updateChildName(childId: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val list = children.first()
            val target = list.firstOrNull { it.id == childId } ?: return@launch
            container.childRepository.updateChild(target.copy(name = trimmed))
        }
    }

    suspend fun childCounts(childId: Long): Pair<Int, Int> =
        withContext(Dispatchers.IO) { container.childRepository.countsForChild(childId) }

    fun deleteChild(childId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val before = container.childRepository.listChildren()
            val deletedDefault = before.firstOrNull { it.id == childId }?.isDefault == true
            container.taskRepository.tasksForChild(childId)
                .forEach { container.scheduler.cancel(it.id) }
            container.childRepository.deleteChild(childId)
            val remaining = container.childRepository.listChildren()
            if (currentChildId.first() == childId) {
                val next = remaining.firstOrNull()
                if (next == null) {
                    container.settings.clearCurrentChildId()
                } else {
                    container.childRepository.setDefault(next.id)
                    container.settings.setCurrentChildId(next.id)
                }
            } else if (deletedDefault && remaining.isNotEmpty()) {
                container.childRepository.setDefault(remaining.first().id)
            }
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            container.settings.setReminderEnabled(enabled)
            ReminderRefresher.rescheduleAll(container)
        }
    }

    fun setRemindWeekdays(days: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            container.settings.setRemindWeekdays(days)
            ReminderRefresher.rescheduleAll(container)
        }
    }

    fun setLeadMinutes(minutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            container.settings.setLeadMinutes(minutes)
            ReminderRefresher.rescheduleAll(container)
        }
    }

    fun setSnoozeMinutes(minutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            container.settings.setSnoozeMinutes(minutes)
        }
    }

    fun setFollowUpEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            container.settings.setFollowUpEnabled(enabled)
            ReminderRefresher.rescheduleAll(container)
        }
    }

    fun setFollowUpMinutes(minutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            container.settings.setFollowUpMinutes(minutes)
        }
    }

    fun setShowOnLockScreen(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            container.settings.setShowOnLockScreen(enabled)
        }
    }

    fun setSilentNotifications(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            container.settings.setSilentNotifications(enabled)
        }
    }

    suspend fun exportJson(uri: Uri): Result<BackupExportSummary> =
        container.backupService.export(uri)

    suspend fun importJson(uri: Uri): Result<BackupImportSummary> =
        container.backupService.import(uri)
}
