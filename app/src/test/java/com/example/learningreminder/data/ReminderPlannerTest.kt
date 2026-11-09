package com.example.learningreminder.data

import com.example.learningreminder.reminder.ReminderPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class ReminderPlannerTest {

    private val mondayMorning = LocalDateTime.of(2026, 10, 19, 18, 0)

    private fun task(
        startDate: String = "2026-10-19",
        endDate: String = "2026-12-31",
        reminderTime: String = "19:00",
        adjustedReminderTime: String? = null,
        repeatRule: String = "DAILY",
        enabled: Boolean = true,
        deleted: Boolean = false
    ) = LearningTask(
        childId = 1,
        title = "朗读",
        startDate = startDate,
        endDate = endDate,
        reminderTime = reminderTime,
        adjustedReminderTime = adjustedReminderTime,
        repeatRule = repeatRule,
        enabled = enabled,
        deleted = deleted
    )

    @Test
    fun todayBeforeReminder_returnsToday() {
        val next = ReminderPlanner.nextTrigger(task(), ReminderPreferences(), mondayMorning)

        assertEquals(LocalDateTime.of(2026, 10, 19, 19, 0), next)
    }

    @Test
    fun adjustedReminderTime_takesPriority() {
        val next = ReminderPlanner.nextTrigger(
            task(reminderTime = "20:00", adjustedReminderTime = "18:30"),
            ReminderPreferences(),
            mondayMorning
        )

        assertEquals(LocalDateTime.of(2026, 10, 19, 18, 30), next)
    }

    @Test
    fun globalWeekdays_filterNextOccurrence() {
        val next = ReminderPlanner.nextTrigger(
            task(repeatRule = "DAILY"),
            ReminderPreferences(weekdays = setOf("WED")),
            mondayMorning
        )

        assertEquals(LocalDateTime.of(2026, 10, 21, 19, 0), next)
    }

    @Test
    fun futureStartDate_isFirstEligibleDay() {
        val next = ReminderPlanner.nextTrigger(
            task(startDate = "2026-10-24"),
            ReminderPreferences(),
            mondayMorning
        )

        assertEquals(LocalDateTime.of(2026, 10, 24, 19, 0), next)
    }

    @Test
    fun noWeekdayOverlap_returnsNull() {
        val next = ReminderPlanner.nextTrigger(
            task(repeatRule = "WEEKDAY"),
            ReminderPreferences(weekdays = setOf("SAT", "SUN")),
            mondayMorning
        )

        assertNull(next)
    }

    @Test
    fun leadMinutes_movesTriggerBeforeReminderTime() {
        val occurrence = ReminderPlanner.nextOccurrence(
            task(reminderTime = "19:00"),
            ReminderPreferences(leadMinutes = 15),
            mondayMorning
        )

        assertEquals(LocalDateTime.of(2026, 10, 19, 18, 45), occurrence?.triggerAt)
        assertEquals(java.time.LocalDate.of(2026, 10, 19), occurrence?.reminderDate)
    }

    @Test
    fun leadMinutesAcrossMidnight_keepsNextDaysReminderDate() {
        val occurrence = ReminderPlanner.nextOccurrence(
            task(startDate = "2026-10-19", reminderTime = "00:15"),
            ReminderPreferences(leadMinutes = 30),
            LocalDateTime.of(2026, 10, 19, 23, 30)
        )

        assertEquals(LocalDateTime.of(2026, 10, 19, 23, 45), occurrence?.triggerAt)
        assertEquals(java.time.LocalDate.of(2026, 10, 20), occurrence?.reminderDate)
    }

    @Test
    fun passedLeadTime_rollsToNextScheduledDay() {
        val next = ReminderPlanner.nextTrigger(
            task(reminderTime = "19:00"),
            ReminderPreferences(leadMinutes = 15),
            LocalDateTime.of(2026, 10, 19, 18, 50)
        )

        assertEquals(LocalDateTime.of(2026, 10, 20, 18, 45), next)
    }

    @Test
    fun disabledOrExpiredTask_isNotScheduled() {
        assertNull(
            ReminderPlanner.nextTrigger(
                task(enabled = false),
                ReminderPreferences(),
                mondayMorning
            )
        )
        assertNull(
            ReminderPlanner.nextTrigger(
                task(endDate = "2026-10-18"),
                ReminderPreferences(),
                mondayMorning
            )
        )
        assertNull(
            ReminderPlanner.nextTrigger(
                task(),
                ReminderPreferences(enabled = false),
                mondayMorning
            )
        )
    }
}
