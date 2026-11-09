package com.example.learningreminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.learningreminder.data.LearningTask
import com.example.learningreminder.data.TodayRules
import java.time.LocalDate

/**
 * 任务管理页。对齐原型 #page-task：
 * 「任务管理」筛选 chips + .list-item 扁平列表（头像 + 标题 + 副信息 + 开关）。
 */
@Composable
fun TaskManageScreen(
    tasks: List<LearningTask>,
    modifier: Modifier = Modifier,
    onAddTask: () -> Unit,
    onToggleEnabled: (Long, Boolean) -> Unit,
    onTaskClick: (Long) -> Unit,
    onEditTask: (Long) -> Unit,
    onCopyTask: (Long) -> Unit,
    onDelete: (Long, Boolean) -> Unit,
    onBatchEnabled: (Set<Long>, Boolean) -> Unit,
    onBackfill: (Set<Long>, List<LocalDate>, (Int) -> Unit) -> Unit
) {
    var deleteTarget by remember { mutableStateOf<LearningTask?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var batchMessage by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf("all") }

    val visibleTasks = remember(tasks, filter) {
        when (filter) {
            "enabled" -> tasks.filter { it.enabled }
            "paused" -> tasks.filter { !it.enabled }
            else -> tasks
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // 原型 .section > .lbl「任务管理」+ .chips 筛选条
        GroupLabel(text = "任务管理")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            AppChip(
                text = "全部",
                selected = filter == "all",
                onClick = { filter = "all" }
            )
            AppChip(
                text = "进行中",
                selected = filter == "enabled",
                onClick = { filter = "enabled" }
            )
            AppChip(
                text = "已暂停",
                selected = filter == "paused",
                onClick = { filter = "paused" }
            )
            AppChip(
                text = "＋ 新建",
                selected = false,
                onClick = onAddTask
            )
        }

        // 批量操作工具条（保留原有能力）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppChip(
                text = if (selectionMode) "退出批量" else "批量操作",
                selected = false,
                onClick = {
                    selectionMode = !selectionMode
                    if (!selectionMode) selectedIds = emptySet()
                    batchMessage = null
                }
            )
            if (selectionMode) {
                Text(
                    text = "已选 ${selectedIds.size} 项",
                    style = MaterialTheme.typography.labelSmall,
                    color = appPalette.ink3
                )
            }
        }

        if (selectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                AppChip(
                    text = "批量启用",
                    selected = false,
                    onClick = { onBatchEnabled(selectedIds, true) }
                )
                AppChip(
                    text = "批量停用",
                    selected = false,
                    onClick = { onBatchEnabled(selectedIds, false) }
                )
                AppChip(
                    text = "补昨天",
                    selected = false,
                    onClick = {
                        onBackfill(selectedIds, listOf(LocalDate.now().minusDays(1))) {
                            batchMessage = "已为昨天补建 $it 条待打卡记录"
                        }
                    }
                )
                AppChip(
                    text = "补上周末",
                    selected = false,
                    onClick = {
                        onBackfill(selectedIds, TodayRules.previousWeekend()) {
                            batchMessage = "已为最近周末补建 $it 条待打卡记录"
                        }
                    }
                )
            }
        }

        batchMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = appPalette.brand,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        if (visibleTasks.isEmpty()) {
            EmptyState(
                emoji = "📭",
                message = if (tasks.isEmpty()) "还没有任务，点右上角新建" else "该筛选下没有任务",
                actionText = "＋ 新建任务",
                onAction = onAddTask
            )
        }

        // 原型 .list-item 扁平列表
        visibleTasks.forEach { task ->
            TaskListItem(
                task = task,
                selectionMode = selectionMode,
                selected = task.id in selectedIds,
                onClick = {
                    if (selectionMode) {
                        selectedIds = if (task.id in selectedIds) {
                            selectedIds - task.id
                        } else {
                            selectedIds + task.id
                        }
                    } else {
                        onTaskClick(task.id)
                    }
                },
                onCheckedChange = { checked ->
                    selectedIds = if (checked) selectedIds + task.id else selectedIds - task.id
                },
                onCopy = { onCopyTask(task.id) },
                onEdit = { onEditTask(task.id) },
                onDelete = { deleteTarget = task },
                onToggleEnabled = { onToggleEnabled(task.id, it) }
            )
        }
    }

    deleteTarget?.let { task ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除任务") },
            text = {
                Text("请选择是否保留该任务的历史完成记录。保留后任务不再出现在列表中，但历史记录仍可查看。")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(task.id, false)
                    deleteTarget = null
                }) { Text("任务和记录一起删") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { deleteTarget = null }) { Text("取消") }
                    TextButton(onClick = {
                        onDelete(task.id, true)
                        deleteTarget = null
                    }) { Text("仅删任务") }
                }
            }
        )
    }
}

/**
 * 原型 .list-item：小头像 + 标题 + 副信息 + 右侧开关，下方一条分隔线。
 */
@Composable
private fun TaskListItem(
    task: LearningTask,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit
) {
    val palette = appPalette
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                ChildAvatar(name = task.title, size = 30.dp, fontSize = 13)
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${task.subject} · ⏰ ${task.effectiveReminderTime()} · " +
                        "🔁 ${repeatLabel(task.repeatRule)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.ink3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Rounded.ContentCopy,
                        contentDescription = "从任务模板复制",
                        modifier = Modifier.size(15.dp),
                        tint = palette.ink3
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "编辑",
                        modifier = Modifier.size(15.dp),
                        tint = palette.ink3
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(15.dp),
                        tint = palette.red
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                AppSwitch(checked = task.enabled, onCheckedChange = onToggleEnabled)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.line)
        )
    }
}

/** 原型 .repeat-pick：星期选择圆点，用于任务编辑页与设置页。 */
@Composable
fun WeekdayPicker(
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = appPalette
    val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    val labels = mapOf(
        "MON" to "一", "TUE" to "二", "WED" to "三", "THU" to "四",
        "FRI" to "五", "SAT" to "六", "SUN" to "日"
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        days.forEach { day ->
            val on = day in selected
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (on) palette.brand else Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        color = if (on) palette.brand else palette.line,
                        shape = CircleShape
                    )
                    .clickable { onToggle(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labels[day].orEmpty(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (on) Color.White else palette.ink2
                )
            }
        }
    }
}
