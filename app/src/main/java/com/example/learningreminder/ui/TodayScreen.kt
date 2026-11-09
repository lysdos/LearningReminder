package com.example.learningreminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learningreminder.data.StatsCalculator
import com.example.learningreminder.data.TaskWithStatus
import com.example.learningreminder.data.TodayRules
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** 今日看板（PRD 3.1）：完成率卡片 + 任务卡列表 + 空态。对齐原型 .summary / .datebar / .taskcard。 */
@Composable
fun TodayScreen(
    items: List<TaskWithStatus>,
    summary: StatsCalculator.DayStats,
    modifier: Modifier = Modifier,
    onAddTask: () -> Unit,
    onTaskClick: (Long) -> Unit,
    onQuickStatus: (TaskWithStatus, String) -> Unit,
    onGenerateToday: () -> Unit,
    onFilterClick: () -> Unit = {}
) {
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        TodaySummaryCard(
            summary = summary,
            date = selectedDate
        )

        DateBar(
            selected = selectedDate,
            today = today,
            onSelect = { selectedDate = it }
        )

        if (items.isEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            AlertBanner(
                text = "还没有任务时，可以一键生成今日学习计划。",
                kind = AlertKind.Info
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PrimaryButton(
                    text = "＋ 新建任务",
                    onClick = onAddTask,
                    modifier = Modifier.weight(1f)
                )
                SecondaryButton(
                    text = "⚡ 一键生成今日",
                    onClick = onGenerateToday,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SectionHeader(
            title = if (selectedDate == today) "今日任务" else "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日任务",
            actionText = "筛选",
            onActionClick = onFilterClick,
            modifier = Modifier.padding(top = 18.dp, bottom = 10.dp)
        )

        val visible = if (selectedDate == today) {
            items
        } else {
            items.filter { it.record?.date == selectedDate.toString() }
        }

        if (visible.isEmpty()) {
            EmptyState(
                emoji = "📋",
                message = "这一天还没有任务记录"
            )
        } else {
            visible.forEach { item ->
                TodayTaskCard(
                    item = item,
                    onClick = { onTaskClick(item.task.id) },
                    onQuickStatus = { status -> onQuickStatus(item, status) }
                )
            }
        }
    }
}

/** 原型 .summary：蓝色渐变完成率卡片 + 进度条 + 三格统计。 */
@Composable
private fun TodaySummaryCard(
    summary: StatsCalculator.DayStats,
    date: LocalDate
) {
    val percent = (summary.rate * 100).toInt()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius))
            .background(summaryBrush())
            .padding(18.dp)
    ) {
        // 原型右上角的装饰圆
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-30).dp)
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
        )
        Column {
            Text(
                text = buildString {
                    append(if (date == LocalDate.now()) "今天 · " else "")
                    append("${date.monthValue}月${date.dayOfMonth}日 ")
                    append(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINESE))
                },
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Text(
                    text = "$percent",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "% 已完成",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 6.dp, start = 6.dp)
                )
            }
            // 原型 .progressbar：半透明白色轨道 + 白色填充
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percent.coerceIn(0, 100) / 100f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SummaryStat(value = "${summary.done}", label = "已完成", modifier = Modifier.weight(1f))
                SummaryDivider()
                SummaryStat(value = "${summary.scheduled}", label = "今日任务", modifier = Modifier.weight(1f))
                SummaryDivider()
                SummaryStat(value = "${summary.counted}", label = "计入完成率", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun SummaryDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(Color.White.copy(alpha = 0.25f))
    )
}

/** 原型 .datebar：横向滚动的日期胶囊，选中态主色实心。 */
@Composable
private fun DateBar(
    selected: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit
) {
    val days = remember(today) {
        (-3..7).map { today.plusDays(it.toLong()) }
    }
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(days) { day ->
            DayChip(
                day = day,
                isToday = day == today,
                isSelected = day == selected,
                onClick = { onSelect(day) }
            )
        }
    }
}

@Composable
private fun DayChip(
    day: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val palette = appPalette
    val bg = when {
        isSelected -> palette.brand
        isToday -> palette.brandSoft
        else -> MaterialTheme.colorScheme.surface
    }
    val fg = when {
        isSelected -> Color.White
        isToday -> palette.brandDark
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderColor = when {
        isSelected -> palette.brand
        isToday -> palette.brand
        else -> Color.Transparent
    }
    Column(
        modifier = Modifier
            .width(46.dp)
            .clip(RoundedCornerShape(AppRadiusSmall))
            .background(bg)
            .border(
                width = 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(AppRadiusSmall)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINESE),
            fontSize = 11.sp,
            color = if (isSelected) Color.White else palette.ink3
        )
        Text(
            text = "${day.dayOfMonth}",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/** 原型 .taskcard：左侧状态色条 + 标题 + 标签 + 底部策略与状态药丸。 */
@Composable
private fun TodayTaskCard(
    item: TaskWithStatus,
    onClick: () -> Unit,
    onQuickStatus: (String) -> Unit
) {
    val palette = appPalette
    val status = item.record?.status
    val accent = when (status) {
        "DONE" -> palette.green
        "PARTIAL", "IN_PROGRESS" -> palette.orange
        "ASK_LEAVE" -> palette.purple
        "EXPIRED", "NO_SHOW" -> palette.red
        else -> palette.brand
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(AppRadius)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // 左侧 4px 状态色条
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // 原型 .tc-title .t：标题与「📷 有证据」标签同行
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.task.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (item.task.requiresEvidence) {
                                MiniTag(
                                    text = "📷 有证据",
                                    kind = TagKind.Evidence,
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MiniTag(text = item.task.subject, kind = TagKind.Subject)
                            MiniTag(
                                text = "⏰ ${item.task.effectiveReminderTime()}",
                                kind = TagKind.Time
                            )
                            MiniTag(
                                text = "🔁 ${repeatLabel(item.task.repeatRule)}",
                                kind = TagKind.Time
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    StatusCheck(
                        status = status,
                        onClick = { onQuickStatus("DONE") }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👤 ${completionPolicyLabel(item.task.completionPolicy)}",
                        fontSize = 11.sp,
                        color = palette.ink3,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusPill(status = status)
                }
            }
        }
    }
}

fun statusLabel(status: String?): String = when (status) {
    null -> "待完成"
    "DONE" -> "已完成"
    "PARTIAL" -> "部分完成"
    "IN_PROGRESS" -> "进行中"
    "NO_SHOW" -> "没做"
    "ASK_LEAVE" -> "请假"
    "EXPIRED" -> "超时"
    "SKIP_TODAY" -> "已跳过"
    "NOT_STARTED" -> "待完成"
    else -> status
}

fun completionPolicyLabel(policy: String): String = when (policy) {
    "PARENT_REPORT" -> "家长确认"
    "SELF_REPORT" -> "孩子汇报"
    "AUTO" -> "自动判断"
    else -> policy
}

/** 原型重复周期文案：一、二、三、四、五（不带「周」前缀）。 */
fun repeatLabel(rule: String): String = when (rule) {
    "DAILY" -> "每天"
    "WEEKDAY" -> "一、二、三、四、五"
    else -> {
        val names = mapOf(
            "MON" to "一", "TUE" to "二", "WED" to "三", "THU" to "四",
            "FRI" to "五", "SAT" to "六", "SUN" to "日"
        )
        rule.split(",")
            .filter { it.isNotBlank() }
            .joinToString("、") { names[it.trim().uppercase()] ?: it.trim() }
    }
}
