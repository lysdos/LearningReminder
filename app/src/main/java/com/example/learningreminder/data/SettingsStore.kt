package com.example.learningreminder.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.learningDataStore by preferencesDataStore(name = "learning_settings")

data class ReminderPreferences(
    val enabled: Boolean = true,
    val weekdays: Set<String> = DEFAULT_WEEKDAYS,
    val leadMinutes: Int = DEFAULT_LEAD_MINUTES,
    val snoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES,
    val followUpEnabled: Boolean = true,
    val followUpMinutes: Int = DEFAULT_FOLLOW_UP_MINUTES,
    val showOnLockScreen: Boolean = true,
    val silentNotifications: Boolean = false
) {
    companion object {
        val DEFAULT_WEEKDAYS = setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
        val LEAD_MINUTE_OPTIONS = setOf(0, 5, 15, 30)
        val FOLLOW_UP_MINUTE_OPTIONS = setOf(5, 10, 15, 30)
        const val DEFAULT_LEAD_MINUTES = 0
        const val DEFAULT_SNOOZE_MINUTES = 10
        const val DEFAULT_FOLLOW_UP_MINUTES = 10
    }
}

/** 轻量偏好：当前孩子、全局提醒开关、按周几提醒（PRD 6.2 第二类数据）。 */
class SettingsStore(private val context: Context) {

    private val keyCurrentChild = longPreferencesKey("current_child_id")
    private val keyReminderEnabled = booleanPreferencesKey("reminder_enabled")
    private val keyWeekdays = stringSetPreferencesKey("remind_weekdays")
    private val keyLeadMinutes = intPreferencesKey("lead_minutes")
    private val keySnoozeMinutes = intPreferencesKey("snooze_minutes")
    private val keyFollowUpEnabled = booleanPreferencesKey("follow_up_enabled")
    private val keyFollowUpMinutes = intPreferencesKey("follow_up_minutes")
    private val keyShowOnLockScreen = booleanPreferencesKey("show_on_lock_screen")
    private val keySilentNotifications = booleanPreferencesKey("silent_notifications")
    private val keyFirstRunDone = booleanPreferencesKey("first_run_done")

    val currentChildId: Flow<Long?> = context.learningDataStore.data.map { it[keyCurrentChild] }
    val reminderPreferences: Flow<ReminderPreferences> =
        context.learningDataStore.data.map { preferences ->
            ReminderPreferences(
                enabled = preferences[keyReminderEnabled] ?: true,
                weekdays = preferences[keyWeekdays] ?: ReminderPreferences.DEFAULT_WEEKDAYS,
                leadMinutes = (preferences[keyLeadMinutes]
                    ?: ReminderPreferences.DEFAULT_LEAD_MINUTES).takeIf {
                    it in ReminderPreferences.LEAD_MINUTE_OPTIONS
                } ?: ReminderPreferences.DEFAULT_LEAD_MINUTES,
                snoozeMinutes = (preferences[keySnoozeMinutes]
                    ?: ReminderPreferences.DEFAULT_SNOOZE_MINUTES).coerceIn(5, 30),
                followUpEnabled = preferences[keyFollowUpEnabled] ?: true,
                followUpMinutes = (preferences[keyFollowUpMinutes]
                    ?: ReminderPreferences.DEFAULT_FOLLOW_UP_MINUTES).takeIf {
                    it in ReminderPreferences.FOLLOW_UP_MINUTE_OPTIONS
                } ?: ReminderPreferences.DEFAULT_FOLLOW_UP_MINUTES,
                showOnLockScreen = preferences[keyShowOnLockScreen] ?: true,
                silentNotifications = preferences[keySilentNotifications] ?: false
            )
        }
    val reminderEnabled: Flow<Boolean> = reminderPreferences.map { it.enabled }
    val remindWeekdays: Flow<Set<String>> = reminderPreferences.map { it.weekdays }
    val leadMinutes: Flow<Int> = reminderPreferences.map { it.leadMinutes }
    val snoozeMinutes: Flow<Int> = reminderPreferences.map { it.snoozeMinutes }
    val followUpEnabled: Flow<Boolean> = reminderPreferences.map { it.followUpEnabled }
    val followUpMinutes: Flow<Int> = reminderPreferences.map { it.followUpMinutes }
    val showOnLockScreen: Flow<Boolean> = reminderPreferences.map { it.showOnLockScreen }
    val silentNotifications: Flow<Boolean> = reminderPreferences.map { it.silentNotifications }
    val firstRunDone: Flow<Boolean> = context.learningDataStore.data.map { it[keyFirstRunDone] ?: false }

    suspend fun reminderPreferencesSnapshot(): ReminderPreferences = reminderPreferences.first()

    suspend fun setCurrentChildId(childId: Long) {
        context.learningDataStore.edit { it[keyCurrentChild] = childId }
    }

    suspend fun clearCurrentChildId() {
        context.learningDataStore.edit { it.remove(keyCurrentChild) }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.learningDataStore.edit { it[keyReminderEnabled] = enabled }
    }

    suspend fun setRemindWeekdays(days: Set<String>) {
        context.learningDataStore.edit { it[keyWeekdays] = days }
    }

    suspend fun setLeadMinutes(minutes: Int) {
        val normalized = minutes.takeIf { it in ReminderPreferences.LEAD_MINUTE_OPTIONS }
            ?: ReminderPreferences.DEFAULT_LEAD_MINUTES
        context.learningDataStore.edit { it[keyLeadMinutes] = normalized }
    }

    suspend fun setSnoozeMinutes(minutes: Int) {
        context.learningDataStore.edit { it[keySnoozeMinutes] = minutes.coerceIn(5, 30) }
    }

    suspend fun setFollowUpEnabled(enabled: Boolean) {
        context.learningDataStore.edit { it[keyFollowUpEnabled] = enabled }
    }

    suspend fun setFollowUpMinutes(minutes: Int) {
        val normalized = minutes.takeIf {
            it in ReminderPreferences.FOLLOW_UP_MINUTE_OPTIONS
        } ?: ReminderPreferences.DEFAULT_FOLLOW_UP_MINUTES
        context.learningDataStore.edit { it[keyFollowUpMinutes] = normalized }
    }

    suspend fun setShowOnLockScreen(enabled: Boolean) {
        context.learningDataStore.edit { it[keyShowOnLockScreen] = enabled }
    }

    suspend fun setSilentNotifications(enabled: Boolean) {
        context.learningDataStore.edit { it[keySilentNotifications] = enabled }
    }

    suspend fun restoreReminderPreferences(preferences: ReminderPreferences) {
        val lead = preferences.leadMinutes.takeIf {
            it in ReminderPreferences.LEAD_MINUTE_OPTIONS
        } ?: ReminderPreferences.DEFAULT_LEAD_MINUTES
        val followUp = preferences.followUpMinutes.takeIf {
            it in ReminderPreferences.FOLLOW_UP_MINUTE_OPTIONS
        } ?: ReminderPreferences.DEFAULT_FOLLOW_UP_MINUTES
        val weekdays = preferences.weekdays
            .map { it.trim().uppercase() }
            .filter { it in VALID_WEEKDAYS }
            .toSet()
        context.learningDataStore.edit {
            it[keyReminderEnabled] = preferences.enabled
            it[keyWeekdays] = weekdays
            it[keyLeadMinutes] = lead
            it[keySnoozeMinutes] = preferences.snoozeMinutes.coerceIn(5, 30)
            it[keyFollowUpEnabled] = preferences.followUpEnabled
            it[keyFollowUpMinutes] = followUp
            it[keyShowOnLockScreen] = preferences.showOnLockScreen
            it[keySilentNotifications] = preferences.silentNotifications
        }
    }

    suspend fun markFirstRunDone() {
        context.learningDataStore.edit { it[keyFirstRunDone] = true }
    }

    companion object {
        private val VALID_WEEKDAYS =
            setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    }
}
