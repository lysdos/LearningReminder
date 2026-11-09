package com.example.learningreminder.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/** 今日看板的状态分组与自动超时规则。 */
object TodayRules {

    enum class Group {
        TODO,
        IN_PROGRESS,
        DONE,
        LEAVE,
        EXPIRED
    }

    fun groupOf(status: String?): Group = when (status) {
        "IN_PROGRESS" -> Group.IN_PROGRESS
        "DONE" -> Group.DONE
        "ASK_LEAVE", "SKIP_TODAY" -> Group.LEAVE
        "NO_SHOW", "EXPIRED" -> Group.EXPIRED
        else -> Group.TODO
    }

    fun isActiveOnDate(task: LearningTask, date: LocalDate): Boolean {
        val value = date.toString()
        return !task.deleted && task.enabled &&
            task.startDate <= value && task.endDate >= value
    }

    fun isScheduledFor(task: LearningTask, date: LocalDate): Boolean {
        return isActiveOnDate(task, date) &&
            RepeatRule.isScheduled(task.repeatRule, date)
    }

    fun isPastReminderTime(
        task: LearningTask,
        now: LocalDateTime = LocalDateTime.now()
    ): Boolean {
        val time = runCatching { LocalTime.parse(task.effectiveReminderTime()) }.getOrNull()
            ?: return false
        return !now.toLocalTime().isBefore(time)
    }

    fun shouldAutoExpire(
        task: LearningTask,
        record: CompletionRecord?,
        now: LocalDateTime = LocalDateTime.now()
    ): Boolean {
        val today = now.toLocalDate()
        if (!isScheduledFor(task, today) || !isPastReminderTime(task, now)) return false
        if (record == null) return true
        if (record.date != today.toString() || !record.autoStatus) return false
        return record.status == "NOT_STARTED" || record.status == "IN_PROGRESS"
    }

    /**
     * 补任务用的最近周末：工作日返回上个周末，周六回退一周，
     * 周日返回当前周六和周日。
     */
    fun previousWeekend(today: LocalDate = LocalDate.now()): List<LocalDate> {
        var saturday = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SATURDAY))
        if (!saturday.isBefore(today)) saturday = saturday.minusWeeks(1)
        return listOf(saturday, saturday.plusDays(1))
    }
}
