package com.example.learningreminder.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TodayRulesTest {

    private val monday = LocalDate.of(2026, 10, 19)

    private fun task(
        reminderTime: String = "19:00",
        adjustedReminderTime: String? = null,
        repeatRule: String = "DAILY"
    ) = LearningTask(
        childId = 1,
        title = "朗读",
        startDate = "2026-10-01",
        endDate = "2026-12-31",
        reminderTime = reminderTime,
        adjustedReminderTime = adjustedReminderTime,
        repeatRule = repeatRule
    )

    private fun record(
        status: String,
        autoStatus: Boolean,
        date: LocalDate = monday
    ) = CompletionRecord(
        taskId = 1,
        childId = 1,
        date = date.toString(),
        status = status,
        autoStatus = autoStatus
    )

    @Test
    fun groupOf_mapsAllDashboardStatuses() {
        assertEquals(TodayRules.Group.TODO, TodayRules.groupOf(null))
        assertEquals(TodayRules.Group.TODO, TodayRules.groupOf("NOT_STARTED"))
        assertEquals(TodayRules.Group.TODO, TodayRules.groupOf("PARTIAL"))
        assertEquals(TodayRules.Group.IN_PROGRESS, TodayRules.groupOf("IN_PROGRESS"))
        assertEquals(TodayRules.Group.DONE, TodayRules.groupOf("DONE"))
        assertEquals(TodayRules.Group.LEAVE, TodayRules.groupOf("ASK_LEAVE"))
        assertEquals(TodayRules.Group.LEAVE, TodayRules.groupOf("SKIP_TODAY"))
        assertEquals(TodayRules.Group.EXPIRED, TodayRules.groupOf("NO_SHOW"))
        assertEquals(TodayRules.Group.EXPIRED, TodayRules.groupOf("EXPIRED"))
    }

    @Test
    fun adjustedReminderTime_drivesAutoExpiry() {
        val adjustedTask = task(
            reminderTime = "20:00",
            adjustedReminderTime = "18:30"
        )

        assertFalse(
            TodayRules.shouldAutoExpire(
                adjustedTask,
                null,
                LocalDateTime.of(monday, java.time.LocalTime.of(18, 29))
            )
        )
        assertTrue(
            TodayRules.shouldAutoExpire(
                adjustedTask,
                null,
                LocalDateTime.of(monday, java.time.LocalTime.of(18, 30))
            )
        )
    }

    @Test
    fun nonScheduledDay_isNeverExpired() {
        val mondayOnly = task(repeatRule = "MON")

        assertFalse(
            TodayRules.shouldAutoExpire(
                mondayOnly,
                null,
                LocalDateTime.of(monday.plusDays(1), java.time.LocalTime.of(23, 0))
            )
        )
    }

    @Test
    fun manualRecord_isNeverOverwrittenByAutoExpiry() {
        assertFalse(
            TodayRules.shouldAutoExpire(
                task(),
                record("NOT_STARTED", autoStatus = false),
                LocalDateTime.of(monday, java.time.LocalTime.of(20, 0))
            )
        )
        assertFalse(
            TodayRules.shouldAutoExpire(
                task(),
                record("IN_PROGRESS", autoStatus = false),
                LocalDateTime.of(monday, java.time.LocalTime.of(20, 0))
            )
        )
    }

    @Test
    fun automaticPendingRecord_canExpire() {
        assertTrue(
            TodayRules.shouldAutoExpire(
                task(),
                record("NOT_STARTED", autoStatus = true),
                LocalDateTime.of(monday, java.time.LocalTime.of(20, 0))
            )
        )
        assertTrue(
            TodayRules.shouldAutoExpire(
                task(),
                record("IN_PROGRESS", autoStatus = true),
                LocalDateTime.of(monday, java.time.LocalTime.of(20, 0))
            )
        )
    }

    @Test
    fun completedOrAlreadyExpiredRecord_doesNotChange() {
        val now = LocalDateTime.of(monday, java.time.LocalTime.of(20, 0))

        assertFalse(TodayRules.shouldAutoExpire(task(), record("DONE", true), now))
        assertFalse(TodayRules.shouldAutoExpire(task(), record("PARTIAL", true), now))
        assertFalse(TodayRules.shouldAutoExpire(task(), record("EXPIRED", true), now))
    }

    @Test
    fun previousWeekend_handlesSaturdayAndSundayBoundaries() {
        val saturday = LocalDate.of(2026, 10, 24)
        val sunday = saturday.plusDays(1)

        assertEquals(
            listOf(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 18)),
            TodayRules.previousWeekend(saturday)
        )
        assertEquals(
            listOf(saturday, sunday),
            TodayRules.previousWeekend(sunday)
        )
        assertEquals(
            listOf(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 18)),
            TodayRules.previousWeekend(LocalDate.of(2026, 10, 23))
        )
    }
}
