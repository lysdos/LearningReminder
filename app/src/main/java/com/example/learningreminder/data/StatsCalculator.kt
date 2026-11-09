package com.example.learningreminder.data

import java.time.LocalDate

/**
 * 完成率计算（PRD 第 8 节）：
 *  - 分母 = 当天应做任务数（enabled、日期范围内、repeatRule 匹配），再排除请假/跳过。
 *  - DONE 计 1，PARTIAL 计 0.5。
 *  - EXPIRED / NO_SHOW / 未打卡都算未完成。
 */
object StatsCalculator {

    private const val STATUS_DONE = "DONE"
    private const val STATUS_PARTIAL = "PARTIAL"
    private const val STATUS_ASK_LEAVE = "ASK_LEAVE"
    private const val STATUS_SKIP_TODAY = "SKIP_TODAY"

    data class DayStats(
        val scheduled: Int,
        val done: Int,
        val rate: Float,
        /** 请假、跳过之外计入完成率分母的任务数；0 时完成率无意义。 */
        val counted: Int = 0
    )

    fun compute(
        tasks: List<LearningTask>,
        records: List<CompletionRecord>,
        date: LocalDate
    ): DayStats {
        val dateStr = date.toString()
        val scheduledTasks = tasks.filter {
            !it.deleted && it.enabled &&
                it.startDate <= dateStr && it.endDate >= dateStr &&
                RepeatRule.isScheduled(it.repeatRule, date)
        }
        val recordByTask = records.filter { it.date == dateStr }.associateBy { it.taskId }
        var denominator = 0.0
        var score = 0.0
        var doneCount = 0
        for (task in scheduledTasks) {
            val status = recordByTask[task.id]?.status
            when (status) {
                STATUS_ASK_LEAVE, STATUS_SKIP_TODAY -> Unit // 不进分母
                else -> {
                    denominator += 1
                    when (status) {
                        STATUS_DONE -> {
                            score += 1.0
                            doneCount += 1
                        }
                        STATUS_PARTIAL -> score += 0.5
                    }
                }
            }
        }
        val rate = if (denominator == 0.0) 0f else (score / denominator).toFloat()
        return DayStats(
            scheduled = scheduledTasks.size,
            done = doneCount,
            rate = rate,
            counted = denominator.toInt()
        )
    }
}
