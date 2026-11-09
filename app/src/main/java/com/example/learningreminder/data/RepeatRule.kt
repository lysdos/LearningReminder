package com.example.learningreminder.data

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 重复规则解析。
 * 支持格式（大小写不敏感，逗号分隔，允许空白）：
 *  - DAILY   每天
 *  - WEEKDAY 工作日（周一至周五）
 *  - MON,TUE,WED,THU,FRI,SAT,SUN 指定星期
 * 无法识别的规则按 DAILY 处理，避免任务静默消失。
 */
object RepeatRule {

    private val dayNames = mapOf(
        "MON" to DayOfWeek.MONDAY,
        "TUE" to DayOfWeek.TUESDAY,
        "WED" to DayOfWeek.WEDNESDAY,
        "THU" to DayOfWeek.THURSDAY,
        "FRI" to DayOfWeek.FRIDAY,
        "SAT" to DayOfWeek.SATURDAY,
        "SUN" to DayOfWeek.SUNDAY
    )

    const val DAILY = "DAILY"
    const val WEEKDAY = "WEEKDAY"

    fun isScheduled(rule: String, date: LocalDate): Boolean {
        val normalized = rule.trim().uppercase().replace(" ", "")
        return when {
            normalized.isEmpty() || normalized == DAILY -> true
            normalized == WEEKDAY -> date.dayOfWeek != DayOfWeek.SATURDAY &&
                date.dayOfWeek != DayOfWeek.SUNDAY
            else -> {
                val days = normalized.split(",").mapNotNull { dayNames[it] }
                if (days.isEmpty()) true else date.dayOfWeek in days
            }
        }
    }

    /** 把任务整理成 UI 可编辑的星期集合（MON..SUN）。 */
    fun toDaysOfWeekSet(rule: String): Set<String> {
        val normalized = rule.trim().uppercase().replace(" ", "")
        if (normalized.isEmpty() || normalized == DAILY) return dayNames.keys.toSet()
        if (normalized == WEEKDAY) return dayNames.keys.filter { it != "SAT" && it != "SUN" }.toSet()
        return normalized.split(",").filter { it in dayNames }.toSet()
    }

    /** 从 UI 选中的星期集合生成规则串；全选输出 DAILY，仅工作日输出 WEEKDAY。 */
    fun fromDaysOfWeekSet(days: Set<String>): String {
        val normalized = days.mapNotNull { it.trim().uppercase() }.filter { it in dayNames }.toSet()
        return when {
            normalized.isEmpty() -> DAILY
            normalized.size == 7 -> DAILY
            normalized.size == 5 && "SAT" !in normalized && "SUN" !in normalized -> WEEKDAY
            else -> dayNames.keys.filter { it in normalized }.joinToString(",")
        }
    }
}
