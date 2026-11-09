package com.example.learningreminder.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learningreminder.data.ReportCalculator
import com.example.learningreminder.viewmodel.ReportViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** 报表（PRD 3.5）：时间、孩子、任务、科目和状态维度。对齐原型 #page-report。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val report by viewModel.report.collectAsState()
    val children by viewModel.children.collectAsState()
    val currentChildId by viewModel.currentChildId.collectAsState()
    val period by viewModel.period.collectAsState()
    val filterStatus by viewModel.filterStatus.collectAsState()
    val filterSubject by viewModel.filterSubject.collectAsState()
    val selectedTaskId by viewModel.selectedTaskId.collectAsState()
    var showRangePicker by remember { mutableStateOf(false) }
    var showTaskMenu by remember { mutableStateOf(false) }
    var detailDate by remember { mutableStateOf<LocalDate?>(null) }
    val selectedTask = report.tasks.firstOrNull { it.id == selectedTaskId }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // 原型 .report-summary：渐变摘要卡 + 每周柱状图
        ReportSummaryCard(
            rangeLabel = report.rangeLabel,
            rate = report.rangeRate,
            dailyRates = report.dailyRates
        )

        // 原型 .section「查看范围」
        Spacer(modifier = Modifier.height(16.dp))
        GroupLabel(text = "查看范围")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            ReportViewModel.Period.entries.forEach { item ->
                AppChip(
                    text = item.label,
                    selected = period == item,
                    onClick = {
                        viewModel.setPeriod(item)
                        if (item == ReportViewModel.Period.RANGE) showRangePicker = true
                    }
                )
            }
            AppChip(
                text = "自定义",
                selected = false,
                onClick = { showRangePicker = true },
                leadingIcon = Icons.Rounded.CalendarToday
            )
        }

        // 原型 .section「筛选」
        Spacer(modifier = Modifier.height(16.dp))
        GroupLabel(text = "筛选")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            AppChip(
                text = "全部状态",
                selected = filterStatus == ReportCalculator.ALL_STATUSES,
                onClick = { viewModel.setStatusFilter(ReportCalculator.ALL_STATUSES) }
            )
            AppChip(
                text = "按科目",
                selected = filterSubject != ReportCalculator.ALL_SUBJECTS,
                onClick = {
                    val next = report.subjects.firstOrNull { it != filterSubject }
                        ?: ReportCalculator.ALL_SUBJECTS
                    viewModel.setSubjectFilter(next)
                }
            )
            AppChip(
                text = "按任务",
                selected = selectedTaskId != null,
                onClick = { showTaskMenu = true }
            )
            AppChip(
                text = "导出",
                selected = false,
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "学习报告")
                        putExtra(Intent.EXTRA_TEXT, viewModel.buildShareText())
                    }
                    context.startActivity(Intent.createChooser(send, "分享学习报告"))
                },
                leadingIcon = Icons.Rounded.Share
            )
        }

        // 孩子切换
        if (children.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            GroupLabel(text = "孩子")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                children.forEach { child ->
                    AppChip(
                        text = child.name,
                        selected = child.id == currentChildId,
                        onClick = { viewModel.selectChild(child.id) }
                    )
                }
            }
        }

        // 科目筛选
        if (report.subjects.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            GroupLabel(text = "科目")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                AppChip(
                    text = "全部",
                    selected = filterSubject == ReportCalculator.ALL_SUBJECTS,
                    onClick = { viewModel.setSubjectFilter(ReportCalculator.ALL_SUBJECTS) }
                )
                report.subjects.forEach { subject ->
                    AppChip(
                        text = subject,
                        selected = filterSubject == subject,
                        onClick = { viewModel.setSubjectFilter(subject) }
                    )
                }
            }
        }

        // 状态筛选
        Spacer(modifier = Modifier.height(16.dp))
        GroupLabel(text = "状态")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            STATUS_OPTIONS.forEach { (code, label) ->
                AppChip(
                    text = label,
                    selected = filterStatus == code,
                    onClick = { viewModel.setStatusFilter(code) }
                )
            }
        }

        // 指标卡
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MiniStat(
                value = rateText(report.todayRate),
                label = "今日完成率",
                modifier = Modifier.weight(1f)
            )
            MiniStat(
                value = rateText(report.rangeRate),
                label = "区间完成率",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        AppCard {
            Text(
                text = "完成 ${report.rangeDone} / 计入 ${report.rangeCounted}",
                style = MaterialTheme.typography.bodySmall,
                color = appPalette.ink3
            )
            Text(
                text = "最近一次完成：${timeText(report.recentCompletedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = appPalette.ink3,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // 原型 .section「每日明细」
        Spacer(modifier = Modifier.height(16.dp))
        GroupLabel(text = "每日明细")
        AppCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp, 12.dp)) {
            if (report.dailyRates.isEmpty()) {
                Text(
                    text = "该区间没有日期",
                    style = MaterialTheme.typography.bodySmall,
                    color = appPalette.ink3
                )
            }
            report.dailyRates.forEachIndexed { index, day ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(appPalette.line)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { detailDate = day.date }
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.width(74.dp)) {
                        Text(
                            text = "${day.date.monthValue}/${day.date.dayOfMonth} " +
                                day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINESE),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${day.done} / ${day.counted} 项完成",
                            style = MaterialTheme.typography.labelSmall,
                            color = appPalette.ink3
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((day.rate ?: 0f).coerceIn(0f, 1f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(rateBarColor(day.rate))
                        )
                    }
                    Text(
                        text = day.rate?.let { "${(it * 100).toInt()}%" } ?: "—",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = rateBarColor(day.rate),
                        modifier = Modifier.width(38.dp)
                    )
                }
            }
        }

        // 完成记录
        Spacer(modifier = Modifier.height(16.dp))
        GroupLabel(text = "完成记录")
        if (report.rows.isEmpty()) {
            EmptyState(
                emoji = "📊",
                message = "该筛选条件下暂无记录"
            )
        }
        report.rows.forEach { row ->
            val record = row.record
            AppCard(
                modifier = Modifier.padding(bottom = 10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = row.taskTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusPill(status = record.status)
                }
                Text(
                    text = "${record.date} · ${row.taskSubject}" +
                        if (record.backfilled) " · 补打卡" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = appPalette.ink3,
                    modifier = Modifier.padding(top = 4.dp)
                )
                val notes = listOfNotNull(record.childNotes, record.parentNotes)
                    .filter { it.isNotBlank() }
                    .joinToString(" / ")
                if (notes.isNotBlank()) {
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                record.completedAt?.let {
                    Text(
                        text = "完成于 ${timeText(it)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = appPalette.ink3
                    )
                }
            }
        }
    }

    if (showTaskMenu) {
        Box {
            DropdownMenu(
                expanded = showTaskMenu,
                onDismissRequest = { showTaskMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("全部任务") },
                    onClick = {
                        viewModel.setTaskFilter(null)
                        showTaskMenu = false
                    }
                )
                report.tasks.forEach { task ->
                    DropdownMenuItem(
                        text = { Text("${task.title} · ${task.subject}") },
                        onClick = {
                            viewModel.setTaskFilter(task.id)
                            showTaskMenu = false
                        }
                    )
                }
            }
        }
    }

    if (showRangePicker) {
        RangePickerSheet(
            initialStart = report.from,
            initialEnd = report.to,
            onConfirm = { start, end ->
                viewModel.setCustomRange(start, end)
                showRangePicker = false
            },
            onDismiss = { showRangePicker = false }
        )
    }

    detailDate?.let { date ->
        DailyDetailSheet(
            date = date,
            items = viewModel.dailyDetails(date),
            onDismiss = { detailDate = null }
        )
    }
}

/** 原型 .report-summary：渐变卡 + 周完成率柱状图。 */
@Composable
private fun ReportSummaryCard(
    rangeLabel: String,
    rate: Float?,
    dailyRates: List<ReportCalculator.DayRate>
) {
    val percent = rate?.let { (it * 100).toInt() } ?: 0
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius))
            .background(reportBrush())
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = rangeLabel,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "$percent",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "%",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                text = "本周平均完成率",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.9f)
            )

            // 原型 .weekbars：柱状图（数值在上、柱在中、星期在下）
            if (dailyRates.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    dailyRates.take(7).forEachIndexed { index, day ->
                        WeekBar(
                            label = day.date.dayOfWeek
                                .getDisplayName(TextStyle.SHORT, Locale.CHINESE),
                            value = day.rate,
                            isToday = index == todayBarIndex
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.WeekBar(
    label: String,
    value: Float?,
    isToday: Boolean = false
) {
    val fraction = (value ?: 0f).coerceIn(0f, 1f)
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = value?.let { "${(it * 100).toInt()}%" } ?: "—",
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.9f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((92 * fraction).dp.coerceAtLeast(4.dp))
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(
                    if (isToday) Color.White else Color.White.copy(alpha = 0.85f)
                )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangePickerSheet(
    initialStart: LocalDate,
    initialEnd: LocalDate,
    onConfirm: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var start by remember(initialStart) { mutableStateOf(initialStart.toString()) }
    var end by remember(initialEnd) { mutableStateOf(initialEnd.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SheetHeader(title = "自定义区间", onClose = onDismiss)
            FormField(
                label = "开始日期",
                value = start,
                onValueChange = { start = it; error = null },
                placeholder = "2026-10-16"
            )
            FormField(
                label = "结束日期",
                value = end,
                onValueChange = { end = it; error = null },
                placeholder = "2026-10-22"
            )
            error?.let {
                AlertBanner(text = it, kind = AlertKind.Error)
            }
            PrimaryButton(
                text = "应用区间",
                onClick = {
                    val parsedStart = runCatching { LocalDate.parse(start) }.getOrNull()
                    val parsedEnd = runCatching { LocalDate.parse(end) }.getOrNull()
                    when {
                        parsedStart == null || parsedEnd == null ->
                            error = "请输入 YYYY-MM-DD 格式的日期"
                        parsedEnd.isBefore(parsedStart) ->
                            error = "结束日期不能早于开始日期"
                        else -> onConfirm(parsedStart, parsedEnd)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyDetailSheet(
    date: LocalDate,
    items: List<ReportCalculator.DailyItem>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SheetHeader(
                title = "${date.monthValue}月${date.dayOfMonth}日 当天任务",
                onClose = onDismiss
            )
            if (items.isEmpty()) {
                Text(
                    text = "当天没有安排任务",
                    style = MaterialTheme.typography.bodyMedium,
                    color = appPalette.ink3
                )
            }
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.task.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${item.task.effectiveReminderTime()} · ${item.task.subject}",
                            style = MaterialTheme.typography.labelSmall,
                            color = appPalette.ink3
                        )
                        val notes = listOfNotNull(
                            item.record?.childNotes,
                            item.record?.parentNotes
                        ).filter { it.isNotBlank() }.joinToString(" / ")
                        if (notes.isNotBlank()) {
                            Text(
                                text = notes,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        item.record?.completedAt?.let {
                            Text(
                                text = "完成于 ${timeText(it)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = appPalette.ink3
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    StatusPill(status = item.record?.status)
                }
            }
        }
    }
}

private fun rateText(rate: Float?): String =
    rate?.let { "${(it * 100).toInt()}%" } ?: "—"

/** 原型每日明细进度条配色：≥80% 绿，50-79% 橙，<50% 红。 */
@Composable
private fun rateBarColor(rate: Float?): Color = when {
    rate == null -> appPalette.ink3
    rate >= 0.8f -> appPalette.green
    rate >= 0.5f -> appPalette.orange
    else -> appPalette.red
}

private fun timeText(value: Long?): String =
    value?.let {
        Instant.ofEpochMilli(it)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    } ?: "暂无"

private val STATUS_OPTIONS = listOf(
    ReportCalculator.ALL_STATUSES to "全部",
    "NOT_STARTED" to "未开始",
    "IN_PROGRESS" to "进行中",
    "DONE" to "完成",
    "PARTIAL" to "部分",
    "NO_SHOW" to "没做",
    "ASK_LEAVE" to "请假",
    "EXPIRED" to "超时",
    "SKIP_TODAY" to "跳过"
)

/** 周柱状图中「今天」所在列的下标（周一起算，0=周一）。 */
private val todayBarIndex: Int
    get() = LocalDate.now().dayOfWeek.value - 1
