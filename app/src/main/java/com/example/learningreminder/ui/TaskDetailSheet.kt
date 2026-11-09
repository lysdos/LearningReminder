package com.example.learningreminder.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.learningreminder.data.CompletionRecord
import com.example.learningreminder.data.LearningTask
import com.example.learningreminder.viewmodel.TodayViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** 任务详情/打卡（PRD 3.3）：可切换日期、补打卡并查看最近历史。对齐原型 #detailSheet。 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun TaskDetailSheet(
    taskId: Long?,
    todayViewModel: TodayViewModel,
    initialDate: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit
) {
    if (taskId == null) {
        onDismiss()
        return
    }

    val context = LocalContext.current
    var task by remember(taskId) { mutableStateOf<LearningTask?>(null) }
    var selectedDate by remember(taskId, initialDate) { mutableStateOf(initialDate) }
    var dateText by remember(taskId, initialDate) { mutableStateOf(initialDate.toString()) }
    var status by remember(taskId) { mutableStateOf("NOT_STARTED") }
    var completedTime by remember(taskId) { mutableStateOf("") }
    var childNotes by remember(taskId) { mutableStateOf("") }
    var parentNotes by remember(taskId) { mutableStateOf("") }
    var evidenceText by remember(taskId) { mutableStateOf("") }
    var evidenceUri by remember(taskId) { mutableStateOf<String?>(null) }
    var selfInitiated by remember(taskId) { mutableStateOf(false) }
    var exceededTarget by remember(taskId) { mutableStateOf(false) }
    var recordedBy by remember(taskId) { mutableStateOf("PARENT") }
    var errorMessage by remember(taskId) { mutableStateOf<String?>(null) }
    var successMessage by remember(taskId) { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        task = todayViewModel.loadTask(taskId)
    }
    val recordsFlow = remember(taskId) { todayViewModel.observeTaskRecords(taskId) }
    val records by recordsFlow.collectAsState(initial = emptyList())
    val record = records.firstOrNull { it.date == selectedDate.toString() }

    LaunchedEffect(record?.id, selectedDate) {
        status = record?.status ?: "NOT_STARTED"
        completedTime = record?.completedAt?.let {
            LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(it),
                ZoneId.systemDefault()
            ).format(DateTimeFormatter.ofPattern("HH:mm"))
        }.orEmpty()
        childNotes = record?.childNotes.orEmpty()
        parentNotes = record?.parentNotes.orEmpty()
        evidenceText = record?.evidenceText.orEmpty()
        evidenceUri = record?.evidenceUri
        selfInitiated = record?.selfInitiated == true
        exceededTarget = record?.exceededTarget == true
        recordedBy = record?.recordedBy?.takeIf { it == "CHILD" } ?: "PARENT"
    }

    val evidenceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            evidenceUri = uri.toString()
            successMessage = "已选择证据图片"
        }
    }

    val loadedTask = task
    if (loadedTask == null) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Text(
                text = "正在读取任务...",
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SheetHeader(title = "任务详情", onClose = onDismiss)

            // 原型 .card 摘要
            AppCard {
                Text(
                    text = loadedTask.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "📚 ${loadedTask.subject} · ⏰ ${loadedTask.effectiveReminderTime()} · " +
                        "🔁 ${repeatLabel(loadedTask.repeatRule)} · ⏱ 约 ${loadedTask.suggestedMinutes} 分钟",
                    style = MaterialTheme.typography.labelSmall,
                    color = appPalette.ink3,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "👤 ${completionPolicyLabel(loadedTask.completionPolicy)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = appPalette.ink3
                )
                if (loadedTask.targetValue.isNotBlank()) {
                    Text(
                        text = "目标：${loadedTask.targetValue}",
                        style = MaterialTheme.typography.labelSmall,
                        color = appPalette.ink3
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill(status = record?.status)
                    if (selectedDate.isBefore(LocalDate.now()) || record?.backfilled == true) {
                        MiniTag(text = "补打卡 · 过去日期", kind = TagKind.Evidence)
                    }
                }
            }

            // 原型 .sec-head「记录完成情况」
            SectionHeader(
                title = "记录完成情况",
                modifier = Modifier.padding(top = 6.dp)
            )
            StatusGrid(
                selected = status,
                onSelect = { code ->
                    status = code
                    errorMessage = null
                }
            )

            // 打卡日期
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FormField(
                    label = "打卡日期",
                    value = dateText,
                    onValueChange = { value ->
                        dateText = value
                        runCatching { LocalDate.parse(value) }.getOrNull()?.let {
                            selectedDate = it
                        }
                    },
                    placeholder = "2026-10-22",
                    modifier = Modifier.weight(1f)
                )
                SecondaryButton(
                    text = "选日期",
                    onClick = { showDatePicker = true },
                    modifier = Modifier.padding(top = 22.dp)
                )
            }

            if (status == "DONE" || status == "PARTIAL") {
                FormField(
                    label = "实际完成时间 HH:mm（可空）",
                    value = completedTime,
                    onValueChange = {
                        completedTime = it.filter { char -> char.isDigit() || char == ':' }.take(5)
                    },
                    placeholder = "19:45"
                )
            }

            // 原型 .field「完成方式」分段
            FieldLabel(text = "记录人")
            SegmentedControl(
                options = listOf(
                    "PARENT" to "家长记录",
                    "CHILD" to "孩子记录"
                ),
                selected = recordedBy,
                onSelect = { recordedBy = it }
            )

            FormField(
                label = "孩子的备注",
                value = childNotes,
                onValueChange = { childNotes = it },
                placeholder = "如：读完 2 篇课文"
            )
            FormField(
                label = "家长备注",
                value = parentNotes,
                onValueChange = { parentNotes = it },
                placeholder = "如：完成得不错"
            )

            // 原型 .toggle-row 组：直接在表单流中，不套卡片
            ToggleRow(
                title = "📷 上传完成证据",
                description = "书籍 / 作业截图（自愿）",
                checked = evidenceUri != null,
                onCheckedChange = { on ->
                    if (on && evidenceUri == null) {
                        evidenceLauncher.launch(arrayOf("image/*"))
                    } else if (!on) {
                        evidenceUri = null
                    }
                }
            )
            ToggleRow(
                title = "自觉完成，无需提醒",
                description = "用于家长评价",
                checked = selfInitiated,
                onCheckedChange = { selfInitiated = it }
            )
            ToggleRow(
                title = "做得比目标更好",
                description = "报表中给予正向标记",
                checked = exceededTarget,
                onCheckedChange = { exceededTarget = it },
                showDivider = false
            )

            FormField(
                label = "打卡证据文字${if (loadedTask.requiresEvidence) "（必填其一）" else ""}",
                value = evidenceText,
                onValueChange = { evidenceText = it },
                placeholder = "描述完成情况"
            )
            evidenceUri?.let { uri ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已选择图片证据",
                        style = MaterialTheme.typography.labelSmall,
                        color = appPalette.ink3,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = { evidenceUri = null }) { Text("移除图片") }
                }
            }
            if (loadedTask.requiresEvidence) {
                Text(
                    text = "该任务要求完成时提供文字或图片依据",
                    style = MaterialTheme.typography.labelSmall,
                    color = appPalette.ink3
                )
            }

            errorMessage?.let {
                AlertBanner(text = it, kind = AlertKind.Error)
            }
            successMessage?.let {
                AlertBanner(text = it, kind = AlertKind.Info)
            }

            // 原型 .sheet-footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SecondaryButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    text = "保存记录",
                    onClick = {
                        val parsedDate = runCatching { LocalDate.parse(dateText) }.getOrNull()
                        if (parsedDate == null) {
                            errorMessage = "打卡日期格式应为 YYYY-MM-DD"
                            return@PrimaryButton
                        }
                        if (parsedDate.isAfter(LocalDate.now())) {
                            errorMessage = "不能为未来日期打卡"
                            return@PrimaryButton
                        }
                        if (loadedTask.requiresEvidence &&
                            status in setOf("DONE", "PARTIAL") &&
                            evidenceText.isBlank() &&
                            evidenceUri.isNullOrBlank()
                        ) {
                            errorMessage = "该任务需要填写文字或图片依据"
                            return@PrimaryButton
                        }
                        val completedAt = completedTime.trim().takeIf { it.isNotEmpty() }?.let { value ->
                            val time = runCatching { LocalTime.parse(value) }.getOrNull()
                            if (time == null) {
                                errorMessage = "完成时间格式应为 HH:mm"
                                return@PrimaryButton
                            }
                            parsedDate.atTime(time).atZone(ZoneId.systemDefault()).toInstant()
                                .toEpochMilli()
                        }
                        successMessage = null
                        errorMessage = null
                        todayViewModel.saveRecord(
                            task = loadedTask,
                            date = parsedDate,
                            status = status,
                            completedAt = completedAt,
                            childNotes = childNotes,
                            parentNotes = parentNotes,
                            evidenceUri = evidenceUri,
                            evidenceText = evidenceText,
                            selfInitiated = if (status == "DONE" || status == "PARTIAL") {
                                selfInitiated
                            } else {
                                null
                            },
                            exceededTarget = if (status == "DONE" || status == "PARTIAL") {
                                exceededTarget
                            } else {
                                null
                            },
                            recordedBy = recordedBy
                        ) { result ->
                            result.fold(
                                onSuccess = {
                                    successMessage = "打卡已保存"
                                    selectedDate = parsedDate
                                },
                                onFailure = { errorMessage = it.message ?: "保存失败" }
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // 原型 .sec-head「最近记录」
            SectionHeader(
                title = "最近记录",
                actionText = "全部 ›",
                onActionClick = { },
                modifier = Modifier.padding(top = 10.dp)
            )
            if (records.isEmpty()) {
                Text(
                    text = "暂无历史记录",
                    style = MaterialTheme.typography.labelSmall,
                    color = appPalette.ink3
                )
            }
            records.take(14).forEach { history ->
                HistoryRecordCard(
                    record = history,
                    selected = history.date == selectedDate.toString(),
                    onClick = {
                        selectedDate = runCatching { LocalDate.parse(history.date) }
                            .getOrDefault(LocalDate.now())
                        dateText = selectedDate.toString()
                    }
                )
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val picked = java.time.Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        selectedDate = picked
                        dateText = picked.toString()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/**
 * 原型 .status-grid：四列打卡状态选择网格。
 */
@Composable
fun StatusGrid(
    selected: String,
    onSelect: (String) -> Unit
) {
    val options = listOf(
        "DONE" to ("✅" to "完成"),
        "PARTIAL" to ("🔶" to "部分"),
        "NO_SHOW" to ("❌" to "没做"),
        "ASK_LEAVE" to ("📝" to "请假"),
        "SKIP_TODAY" to ("⏭️" to "跳过"),
        "IN_PROGRESS" to ("▶️" to "进行中"),
        "EXPIRED" to ("⏰" to "超时"),
        "NOT_STARTED" to ("⬜" to "未开始")
    )
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        options.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                row.forEach { (code, labels) ->
                    StatusOption(
                        emoji = labels.first,
                        label = labels.second,
                        selected = selected == code,
                        onClick = { onSelect(code) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(4 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatusOption(
    emoji: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = appPalette
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(AppRadiusSmall))
            .background(
                if (selected) palette.brandSoft else Color.Transparent
            )
            .border(
                width = 2.dp,
                color = if (selected) palette.brand else palette.line,
                shape = RoundedCornerShape(AppRadiusSmall)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, fontSize = 20.sp)
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) palette.brandDark else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun HistoryRecordCard(
    record: CompletionRecord,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = appPalette
    val emoji = when (record.status) {
        "DONE" -> "✅"
        "PARTIAL" -> "🔶"
        "NO_SHOW" -> "❌"
        "ASK_LEAVE" -> "📝"
        "SKIP_TODAY" -> "⏭️"
        "IN_PROGRESS" -> "▶️"
        "EXPIRED" -> "⏰"
        else -> "⬜"
    }
    val shortDate = record.date
        .takeLast(5)
        .replace('-', '/')
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadiusTiny))
            .background(
                if (selected) palette.brandSoft else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = shortDate,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(52.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$emoji ${statusLabel(record.status)}" +
                    if (record.backfilled) " · 补打卡" else "",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "由 ${if (record.recordedBy == "CHILD") "孩子" else "家长"} 记录",
                style = MaterialTheme.typography.labelSmall,
                color = appPalette.ink3
            )
            val notes = listOfNotNull(
                record.childNotes,
                record.parentNotes,
                record.evidenceText
            ).joinToString(" / ").ifEmpty {
                if (record.evidenceUri.isNullOrBlank()) "无备注" else "含图片证据"
            }
            if (notes != "无备注") {
                Text(
                    text = notes,
                    style = MaterialTheme.typography.labelSmall,
                    color = appPalette.ink2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
