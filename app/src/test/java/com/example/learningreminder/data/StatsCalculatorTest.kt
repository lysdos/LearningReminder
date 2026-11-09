package com.example.learningreminder.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatsCalculatorTest {

    private val today = LocalDate.of(2026, 10, 22)

    private fun task(
        id: Long,
        enabled: Boolean = true,
        start: String = "2026-10-01",
        end: String = "2026-12-01",
        rule: String = "DAILY"
    ) = LearningTask(
        id = id, childId = 1, title = "t$id", enabled = enabled,
        startDate = start, endDate = end, repeatRule = rule
    )

    private fun record(taskId: Long, status: String, date: String = "2026-10-22") =
        CompletionRecord(taskId = taskId, childId = 1, date = date, status = status)

    @Test
    fun doneCountsAsOne_partialAsHalf() {
        val tasks = listOf(task(1), task(2), task(3))
        val records = listOf(record(1, "DONE"), record(2, "PARTIAL"))
        val stats = StatsCalculator.compute(tasks, records, today)
        assertEquals(3, stats.scheduled)
        // 1 + 0.5 = 1.5 of 3
        assertEquals(0.5f, stats.rate, 0.001f)
    }

    @Test
    fun leaveAndSkipExcludedFromDenominator() {
        val tasks = listOf(task(1), task(2), task(3), task(4))
        val records = listOf(record(1, "DONE"), record(2, "ASK_LEAVE"), record(3, "SKIP_TODAY"))
        val stats = StatsCalculator.compute(tasks, records, today)
        assertEquals(4, stats.scheduled) // 全部应做任务（请假/跳过仍显示，但不进分母）
        assertEquals(1, stats.done)
        assertEquals(0.5f, stats.rate, 0.001f)
    }

    @Test
    fun expiredAndNoShowCountAsNotDone() {
        val tasks = listOf(task(1), task(2))
        val records = listOf(record(1, "EXPIRED"), record(2, "NO_SHOW"))
        val stats = StatsCalculator.compute(tasks, records, today)
        assertEquals(2, stats.scheduled)
        assertEquals(0, stats.done)
        assertEquals(0f, stats.rate, 0.001f)
    }

    @Test
    fun disabledAndOutOfDateRangeTasksExcluded() {
        val tasks = listOf(
            task(1, enabled = false),
            task(2, start = "2026-11-01"),
            task(3, end = "2026-10-01"),
            task(4)
        )
        val records = listOf(record(4, "DONE"))
        val stats = StatsCalculator.compute(tasks, records, today)
        assertEquals(1, stats.scheduled)
        assertEquals(1, stats.done)
        assertEquals(1f, stats.rate, 0.001f)
    }

    @Test
    fun emptyTasks_zeroRate() {
        val stats = StatsCalculator.compute(emptyList(), emptyList(), today)
        assertEquals(0, stats.scheduled)
        assertEquals(0, stats.done)
        assertEquals(0f, stats.rate, 0.001f)
    }

    @Test
    fun recordsWithoutMatchingTaskIgnored() {
        val tasks = listOf(task(1))
        val records = listOf(record(99, "DONE"), record(1, "DONE"))
        val stats = StatsCalculator.compute(tasks, records, today)
        assertEquals(1, stats.scheduled)
        assertEquals(1, stats.done)
        assertEquals(1f, stats.rate, 0.001f)
    }
}
