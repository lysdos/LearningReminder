package com.example.learningreminder.data

import java.time.LocalDate

/**
 * 报表统计与列表投影。完成率只统计实际应做且未请假/跳过的任务，
 * 未安排任务的日期返回 null，界面不需要把 0/0 误显示成 0%。
 */
object ReportCalculator {

    data class DayRate(
        val date: LocalDate,
        val scheduled: Int,
        val counted: Int,
        val done: Int,
        val rate: Float?
    )

    data class RangeStats(
        val scheduled: Int,
        val counted: Int,
        val done: Int,
        val score: Float,
        val rate: Float?
    )

    data class RecordRow(
        val record: CompletionRecord,
        val taskTitle: String,
        val taskSubject: String
    )

    data class DailyItem(
        val task: LearningTask,
        val record: CompletionRecord?
    )

    fun dayRate(
        tasks: List<LearningTask>,
        records: List<CompletionRecord>,
        date: LocalDate
    ): DayRate {
        val stats = StatsCalculator.compute(tasks, records, date)
        return DayRate(
            date = date,
            scheduled = stats.scheduled,
            counted = stats.counted,
            done = stats.done,
            rate = stats.rate.takeIf { stats.counted > 0 }
        )
    }

    fun rangeStats(
        tasks: List<LearningTask>,
        records: List<CompletionRecord>,
        from: LocalDate,
        to: LocalDate
    ): RangeStats {
        if (to.isBefore(from)) {
            return RangeStats(0, 0, 0, 0f, null)
        }
        var scheduled = 0
        var counted = 0
        var done = 0
        var score = 0f
        var date = from
        while (!date.isAfter(to)) {
            val day = StatsCalculator.compute(tasks, records, date)
            scheduled += day.scheduled
            counted += day.counted
            done += day.done
            score += day.rate * day.counted
            date = date.plusDays(1)
        }
        return RangeStats(
            scheduled = scheduled,
            counted = counted,
            done = done,
            score = score,
            rate = (score / counted).takeIf { counted > 0 }
        )
    }

    fun dailyRates(
        tasks: List<LearningTask>,
        records: List<CompletionRecord>,
        from: LocalDate,
        to: LocalDate,
        maxDays: Int = 31
    ): List<DayRate> {
        if (to.isBefore(from)) return emptyList()
        val earliest = maxOf(from, to.minusDays((maxDays - 1).coerceAtLeast(0).toLong()))
        val rates = mutableListOf<DayRate>()
        var date = earliest
        while (!date.isAfter(to)) {
            rates += dayRate(tasks, records, date)
            date = date.plusDays(1)
        }
        return rates
    }

    fun recordRows(
        records: List<CompletionRecord>,
        tasks: List<LearningTask>,
        taskId: Long?,
        subject: String,
        status: String
    ): List<RecordRow> {
        val taskById = tasks.associateBy { it.id }
        return records.asSequence()
            .filter { taskId == null || it.taskId == taskId }
            .map { record ->
                val task = taskById[record.taskId]
                RecordRow(
                    record = record,
                    taskTitle = task?.title ?: "已删除任务",
                    taskSubject = task?.subject ?: "未分类"
                )
            }
            .filter { subject == ALL_SUBJECTS || it.taskSubject == subject }
            .filter { status == ALL_STATUSES || it.record.status == status }
            .sortedWith(compareByDescending<RecordRow> { it.record.date }.thenBy { it.record.id })
            .toList()
    }

    fun dailyItems(
        tasks: List<LearningTask>,
        records: List<CompletionRecord>,
        date: LocalDate,
        status: String = ALL_STATUSES
    ): List<DailyItem> {
        val recordByTask = records.asSequence()
            .filter { it.date == date.toString() }
            .associateBy { it.taskId }
        return tasks.filter {
            TodayRules.isScheduledFor(it, date) || recordByTask.containsKey(it.id)
        }
            .sortedBy { it.effectiveReminderTime() }
            .map { DailyItem(task = it, record = recordByTask[it.id]) }
            .filter { status == ALL_STATUSES || it.record?.status == status }
    }

    fun recentCompletionAt(
        records: List<CompletionRecord>,
        tasks: List<LearningTask>,
        taskId: Long?,
        subject: String
    ): Long? {
        val taskSubjectById = tasks.associate { it.id to it.subject }
        return records.asSequence()
            .filter {
                it.status == "DONE" &&
                    it.completedAt != null &&
                    (taskId == null || it.taskId == taskId) &&
                    (subject == ALL_SUBJECTS || taskSubjectById[it.taskId] == subject)
            }
            .mapNotNull { it.completedAt }
            .maxOrNull()
    }

    const val ALL_STATUSES = "ALL"
    const val ALL_SUBJECTS = "ALL"
}
