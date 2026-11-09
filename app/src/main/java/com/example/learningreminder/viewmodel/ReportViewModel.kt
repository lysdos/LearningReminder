package com.example.learningreminder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learningreminder.AppContainer
import com.example.learningreminder.data.ChildProfile
import com.example.learningreminder.data.CompletionRecord
import com.example.learningreminder.data.LearningTask
import com.example.learningreminder.data.ReportCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class ReportSnapshot(
    val from: LocalDate,
    val to: LocalDate,
    val rangeLabel: String,
    val todayRate: Float?,
    val rangeRate: Float?,
    val rangeScheduled: Int,
    val rangeCounted: Int,
    val rangeDone: Int,
    val recentCompletedAt: Long?,
    val dailyRates: List<ReportCalculator.DayRate>,
    val rows: List<ReportCalculator.RecordRow>,
    val tasks: List<LearningTask>,
    val records: List<CompletionRecord>,
    val subjects: List<String>
)

/** 报表（PRD 3.5）：自然日/周/月、自定义区间、任务维度和按日详情。 */
class ReportViewModel(
    private val container: AppContainer
) : ViewModel() {

    enum class Period(val label: String) {
        DAY("日"), WEEK("周"), MONTH("月"), RANGE("自定义")
    }

    val period = MutableStateFlow(Period.WEEK)
    val filterStatus = MutableStateFlow(ReportCalculator.ALL_STATUSES)
    val filterSubject = MutableStateFlow(ReportCalculator.ALL_SUBJECTS)
    val selectedTaskId = MutableStateFlow<Long?>(null)
    val anchorDate = MutableStateFlow(LocalDate.now())
    val customStart = MutableStateFlow(LocalDate.now().minusDays(6))
    val customEnd = MutableStateFlow(LocalDate.now())

    val children: StateFlow<List<ChildProfile>> =
        container.childRepository.observeChildren().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val currentChildId: StateFlow<Long?> = container.settings.currentChildId.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val childData = container.settings.currentChildId.flatMapLatest { childId ->
        if (childId == null) {
            flowOf(ChildReportData(emptyList(), emptyList()))
        } else {
            combine(
                container.taskRepository.observeByChildIncludingDeleted(childId),
                container.recordRepository.observeByChild(childId)
            ) { tasks, records -> ChildReportData(tasks, records) }
        }
    }

    private val query = combine(
        period,
        anchorDate,
        customStart,
        customEnd
    ) { selectedPeriod, anchor, start, end ->
        when (selectedPeriod) {
            Period.DAY -> anchor to anchor
            Period.WEEK -> {
                val from = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                from to from.plusDays(6)
            }
            Period.MONTH -> {
                val month = YearMonth.from(anchor)
                month.atDay(1) to month.atEndOfMonth()
            }
            Period.RANGE -> start to end
        }
    }

    private val filters = combine(
        selectedTaskId,
        filterSubject,
        filterStatus
    ) { taskId, subject, status ->
        ReportFilters(taskId = taskId, subject = subject, status = status)
    }

    val report: StateFlow<ReportSnapshot> = combine(childData, query, filters) {
            data, (from, to), activeFilters ->
        val tasks = filterTasks(data.tasks, activeFilters)
        val rangeRecords = data.records.filter {
            val date = runCatching { LocalDate.parse(it.date) }.getOrNull()
            date != null && !date.isBefore(from) && !date.isAfter(to)
        }
        val rangeStats = ReportCalculator.rangeStats(tasks, rangeRecords, from, to)
        val todayStats = ReportCalculator.dayRate(tasks, data.records, LocalDate.now())
        val rows = ReportCalculator.recordRows(
            records = rangeRecords,
            tasks = data.tasks,
            taskId = activeFilters.taskId,
            subject = activeFilters.subject,
            status = activeFilters.status
        )
        ReportSnapshot(
            from = from,
            to = to,
            rangeLabel = rangeLabel(from, to),
            todayRate = todayStats.rate,
            rangeRate = rangeStats.rate,
            rangeScheduled = rangeStats.scheduled,
            rangeCounted = rangeStats.counted,
            rangeDone = rangeStats.done,
            recentCompletedAt = ReportCalculator.recentCompletionAt(
                records = data.records,
                tasks = data.tasks,
                taskId = activeFilters.taskId,
                subject = activeFilters.subject
            ),
            dailyRates = ReportCalculator.dailyRates(tasks, rangeRecords, from, to),
            rows = rows,
            tasks = data.tasks,
            records = data.records,
            subjects = data.tasks.map { it.subject }.filter { it.isNotBlank() }.distinct().sorted()
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ReportSnapshot(
            from = LocalDate.now().minusDays(6),
            to = LocalDate.now(),
            rangeLabel = "",
            todayRate = null,
            rangeRate = null,
            rangeScheduled = 0,
            rangeCounted = 0,
            rangeDone = 0,
            recentCompletedAt = null,
            dailyRates = emptyList(),
            rows = emptyList(),
            tasks = emptyList(),
            records = emptyList(),
            subjects = emptyList()
        )
    )

    fun setPeriod(value: Period) {
        period.value = value
    }

    fun movePeriod(offset: Long) {
        anchorDate.value = when (period.value) {
            Period.DAY -> anchorDate.value.plusDays(offset)
            Period.WEEK -> anchorDate.value.plusWeeks(offset)
            Period.MONTH -> anchorDate.value.plusMonths(offset)
            Period.RANGE -> anchorDate.value
        }
    }

    fun setCustomRange(start: LocalDate, end: LocalDate) {
        if (end.isBefore(start)) return
        customStart.value = start
        customEnd.value = end
        anchorDate.value = end
        period.value = Period.RANGE
    }

    fun setStatusFilter(status: String) {
        filterStatus.value = status
    }

    fun setSubjectFilter(subject: String) {
        filterSubject.value = subject
    }

    fun setTaskFilter(taskId: Long?) {
        selectedTaskId.value = taskId
    }

    fun selectChild(childId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            container.settings.setCurrentChildId(childId)
            container.childRepository.setDefault(childId)
            selectedTaskId.value = null
            filterSubject.value = ReportCalculator.ALL_SUBJECTS
        }
    }

    fun dailyDetails(date: LocalDate): List<ReportCalculator.DailyItem> {
        val value = report.value
        if (date.isBefore(value.from) || date.isAfter(value.to)) return emptyList()
        val tasks = filterTasks(
            value.tasks,
            ReportFilters(
                taskId = selectedTaskId.value,
                subject = filterSubject.value,
                status = filterStatus.value
            )
        )
        return ReportCalculator.dailyItems(
            tasks = tasks,
            records = value.records,
            date = date,
            status = filterStatus.value
        )
    }

    fun buildShareText(): String {
        val value = report.value
        val rows = value.rows.joinToString("\n") { row ->
            val record = row.record
            val notes = listOfNotNull(record.childNotes, record.parentNotes)
                .filter { it.isNotBlank() }
                .joinToString(" / ")
                .ifBlank { "无备注" }
            "${record.date} ${row.taskTitle}（${row.taskSubject}）" +
                " ${statusLabel(record.status)} $notes"
        }.ifBlank { "暂无完成记录" }
        return buildString {
            appendLine("学习报告 · ${value.rangeLabel}")
            appendLine("今日完成率：${rateText(value.todayRate)}")
            appendLine(
                "区间完成率：${rateText(value.rangeRate)}" +
                    "（完成 ${value.rangeDone} / 计入 ${value.rangeCounted}）"
            )
            appendLine("最近完成：${timeText(value.recentCompletedAt)}")
            appendLine()
            append(rows)
        }
    }

    private fun filterTasks(tasks: List<LearningTask>, filters: ReportFilters): List<LearningTask> =
        tasks.filter {
            (filters.taskId == null || it.id == filters.taskId) &&
                (filters.subject == ReportCalculator.ALL_SUBJECTS ||
                    it.subject == filters.subject)
        }

    private fun rangeLabel(from: LocalDate, to: LocalDate): String =
        if (from == to) {
            from.format(REPORT_DATE_FORMAT)
        } else {
            "${from.format(REPORT_DATE_FORMAT)} - ${to.format(REPORT_DATE_FORMAT)}"
        }

    private fun rateText(rate: Float?): String =
        rate?.let { "${(it * 100).toInt()}%" } ?: "无应做任务"

    private fun timeText(value: Long?): String =
        value?.let {
            Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .format(REPORT_TIME_FORMAT)
        } ?: "暂无"

    private fun statusLabel(status: String): String = when (status) {
        "NOT_STARTED" -> "未开始"
        "IN_PROGRESS" -> "进行中"
        "DONE" -> "完成"
        "PARTIAL" -> "部分完成"
        "NO_SHOW" -> "没做"
        "ASK_LEAVE" -> "请假"
        "EXPIRED" -> "超时未做"
        "SKIP_TODAY" -> "跳过"
        else -> status
    }

    private data class ChildReportData(
        val tasks: List<LearningTask>,
        val records: List<CompletionRecord>
    )

    private data class ReportFilters(
        val taskId: Long?,
        val subject: String,
        val status: String
    )

    companion object {
        private val REPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val REPORT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
