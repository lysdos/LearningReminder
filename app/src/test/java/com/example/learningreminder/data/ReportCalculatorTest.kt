package com.example.learningreminder.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ReportCalculatorTest {

    private val day = LocalDate.of(2026, 10, 22)

    private fun task(
        id: Long,
        title: String = "任务$id",
        subject: String = "语文",
        enabled: Boolean = true,
        deleted: Boolean = false,
        reminderTime: String = "19:00",
        rule: String = "DAILY"
    ) = LearningTask(
        id = id,
        childId = 1,
        title = title,
        subject = subject,
        startDate = "2026-10-01",
        endDate = "2026-12-01",
        reminderTime = reminderTime,
        repeatRule = rule,
        enabled = enabled,
        deleted = deleted
    )

    private fun record(
        taskId: Long,
        status: String,
        date: LocalDate = day,
        completedAt: Long? = null
    ) = CompletionRecord(
        id = taskId,
        taskId = taskId,
        childId = 1,
        date = date.toString(),
        status = status,
        completedAt = completedAt
    )

    @Test
    fun noCountedTasksReturnsNullRateInsteadOfZero() {
        val tasks = listOf(task(1), task(2))
        val records = listOf(record(1, "ASK_LEAVE"), record(2, "SKIP_TODAY"))

        val result = ReportCalculator.dayRate(tasks, records, day)

        assertEquals(0, result.counted)
        assertNull(result.rate)
    }

    @Test
    fun softDeletedTaskDoesNotReduceCompletionRate() {
        val tasks = listOf(task(1), task(2, deleted = true))

        val result = ReportCalculator.dayRate(
            tasks = tasks,
            records = listOf(record(1, "DONE")),
            date = day
        )

        assertEquals(1, result.counted)
        assertEquals(1f, result.rate!!, 0.001f)
    }

    @Test
    fun rangeStatsAggregateDailyPartialScoresAndExcludeLeave() {
        val secondDay = day.plusDays(1)
        val tasks = listOf(task(1), task(2), task(3))
        val records = listOf(
            record(1, "DONE"),
            record(2, "PARTIAL", date = secondDay),
            record(3, "ASK_LEAVE", date = secondDay)
        )

        val result = ReportCalculator.rangeStats(tasks, records, day, secondDay)

        assertEquals(5, result.counted)
        assertEquals(1, result.done)
        assertEquals(1.5f, result.score, 0.001f)
        assertEquals(0.3f, result.rate!!, 0.001f)
    }

    @Test
    fun recordRowsFilterByTaskSubjectAndStatus() {
        val tasks = listOf(
            task(1, title = "读书", subject = "语文"),
            task(2, title = "口算", subject = "数学")
        )
        val records = listOf(
            record(1, "DONE"),
            record(2, "DONE"),
            record(1, "PARTIAL")
        )

        val result = ReportCalculator.recordRows(
            records = records,
            tasks = tasks,
            taskId = 1,
            subject = "语文",
            status = "DONE"
        )

        assertEquals(1, result.size)
        assertEquals("读书", result.single().taskTitle)
    }

    @Test
    fun softDeletedTaskKeepsItsTitleInHistory() {
        val deletedTask = task(1, title = "已停用阅读", subject = "阅读", deleted = true)
        val result = ReportCalculator.recordRows(
            records = listOf(record(1, "DONE")),
            tasks = listOf(deletedTask),
            taskId = null,
            subject = ReportCalculator.ALL_SUBJECTS,
            status = ReportCalculator.ALL_STATUSES
        )

        assertEquals("已停用阅读", result.single().taskTitle)
        assertEquals("阅读", result.single().taskSubject)
    }

    @Test
    fun recentCompletionHonoursTaskAndSubjectFilters() {
        val tasks = listOf(
            task(1, subject = "语文"),
            task(2, subject = "数学")
        )
        val records = listOf(
            record(1, "DONE", completedAt = 1_000L),
            record(2, "DONE", completedAt = 9_000L)
        )

        assertEquals(
            1_000L,
            ReportCalculator.recentCompletionAt(records, tasks, taskId = 1, subject = "ALL")
        )
        assertEquals(
            9_000L,
            ReportCalculator.recentCompletionAt(
                records,
                tasks,
                taskId = null,
                subject = "数学"
            )
        )
    }

    @Test
    fun dailyItemsShowOnlyTasksScheduledThatDaySortedByReminderTime() {
        val tasks = listOf(
            task(1, title = "晚读", reminderTime = "20:00", rule = "MON,TUE,WED,THU,FRI"),
            task(2, title = "早读", reminderTime = "07:00"),
            task(3, title = "未启用", enabled = false)
        )

        val result = ReportCalculator.dailyItems(tasks, listOf(record(1, "DONE")), day)

        assertEquals(listOf("早读", "晚读"), result.map { it.task.title })
        assertEquals("DONE", result.last().record?.status)
    }

    @Test
    fun dailyItemsKeepSoftDeletedTaskWhenItHasARecordThatDay() {
        val tasks = listOf(
            task(1, title = "历史阅读", deleted = true, enabled = false)
        )

        val result = ReportCalculator.dailyItems(
            tasks = tasks,
            records = listOf(record(1, "DONE")),
            date = day
        )

        assertEquals("历史阅读", result.single().task.title)
        assertEquals("DONE", result.single().record?.status)
    }

    @Test
    fun dailyItemsHonourStatusFilter() {
        val tasks = listOf(task(1), task(2, reminderTime = "20:00"))
        val records = listOf(record(1, "DONE"), record(2, "PARTIAL"))

        val result = ReportCalculator.dailyItems(
            tasks = tasks,
            records = records,
            date = day,
            status = "PARTIAL"
        )

        assertEquals(listOf(2L), result.map { it.task.id })
    }
}
