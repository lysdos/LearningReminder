package com.example.learningreminder.reminder

import com.example.learningreminder.data.LearningTask
import com.example.learningreminder.data.ReminderPreferences
import com.example.learningreminder.data.RepeatRule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** 不依赖 Android 的提醒时间规则，便于对日期、重复周期和全局星期开关做单测。 */
object ReminderPlanner {

    data class ReminderOccurrence(
        val triggerAt: LocalDateTime,
        val reminderDate: LocalDate
    )

    fun nextTrigger(
        task: LearningTask,
        preferences: ReminderPreferences,
        now: LocalDateTime = LocalDateTime.now()
    ): LocalDateTime? = nextOccurrence(task, preferences, now)?.triggerAt

    fun nextOccurrence(
        task: LearningTask,
        preferences: ReminderPreferences,
        now: LocalDateTime = LocalDateTime.now()
    ): ReminderOccurrence? {
        if (!preferences.enabled || !task.enabled || task.deleted) return null

        val startDate = runCatching { LocalDate.parse(task.startDate) }.getOrNull() ?: return null
        val endDate = runCatching { LocalDate.parse(task.endDate) }.getOrNull() ?: return null
        val reminderTime = runCatching { LocalTime.parse(task.effectiveReminderTime()) }.getOrNull()
            ?: return null
        if (endDate.isBefore(now.toLocalDate())) return null

        val firstCandidateDate = maxOf(now.toLocalDate(), startDate)
        for (offset in 0L..7L) {
            val candidateDate = firstCandidateDate.plusDays(offset)
            if (candidateDate.isAfter(endDate)) break
            if (!RepeatRule.isScheduled(task.repeatRule, candidateDate)) continue
            if (!isWeekdayAllowed(preferences, candidateDate)) continue
            val candidate = candidateDate.atTime(reminderTime)
                .minusMinutes(effectiveLeadMinutes(preferences).toLong())
            if (candidate.isAfter(now)) {
                return ReminderOccurrence(candidate, candidateDate)
            }
        }
        return null
    }

    fun nextTriggerMillis(
        task: LearningTask,
        preferences: ReminderPreferences,
        now: LocalDateTime = LocalDateTime.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long? = nextOccurrence(task, preferences, now)
        ?.triggerAt
        ?.atZone(zoneId)
        ?.toInstant()
        ?.toEpochMilli()

    fun dueDateTime(task: LearningTask, date: LocalDate): LocalDateTime? {
        val reminderTime = runCatching { LocalTime.parse(task.effectiveReminderTime()) }
            .getOrNull() ?: return null
        return date.atTime(reminderTime)
    }

    fun isDueOn(
        task: LearningTask,
        date: LocalDate,
        preferences: ReminderPreferences
    ): Boolean {
        if (!preferences.enabled || !task.enabled || task.deleted) return false
        val dateText = date.toString()
        if (dateText < task.startDate || dateText > task.endDate) return false
        return RepeatRule.isScheduled(task.repeatRule, date) &&
            isWeekdayAllowed(preferences, date)
    }

    private fun isWeekdayAllowed(
        preferences: ReminderPreferences,
        date: LocalDate
    ): Boolean {
        val allowed = preferences.weekdays.map { it.trim().uppercase() }.filter { it.isNotBlank() }
        if (allowed.isEmpty()) return false
        val weekday = date.dayOfWeek.name.take(3)
        return weekday in allowed
    }

    private fun effectiveLeadMinutes(preferences: ReminderPreferences): Int =
        preferences.leadMinutes.takeIf { it in ReminderPreferences.LEAD_MINUTE_OPTIONS }
            ?: ReminderPreferences.DEFAULT_LEAD_MINUTES
}
