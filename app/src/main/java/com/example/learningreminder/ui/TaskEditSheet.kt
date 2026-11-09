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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.learningreminder.data.ChildProfile
import com.example.learningreminder.data.LearningTask
import com.example.learningreminder.data.RepeatRule
import java.time.LocalDate
import java.time.LocalTime

/** 新建/编辑任务（PRD 3.2 全部字段）。复用一个 ModalBottomSheet，样式对齐原型 #taskSheet。 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun TaskEditSheet(
    existing: LearningTask?,
    childId: Long,
    onDismiss: () -> Unit,
    onSave: (LearningTask) -> Unit,
    children: List<ChildProfile> = emptyList()
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var subject by remember { mutableStateOf(existing?.subject ?: "语文") }
    var selectedChildId by remember { mutableLongStateOf(existing?.childId ?: childId) }
    var startDate by remember {
        mutableStateOf(existing?.startDate ?: LocalDate.now().toString())
    }
    var endDate by remember {
        mutableStateOf(existing?.endDate ?: LocalDate.now().plusMonths(3).toString())
    }
    var reminderHour by remember {
        mutableStateOf(existing?.reminderTime?.substringBefore(":")?.padStart(2, '0') ?: "19")
    }
    var reminderMinute by remember {
        mutableStateOf(existing?.reminderTime?.substringAfter(":")?.padStart(2, '0') ?: "00")
    }
    var adjustedReminderTime by remember {
        mutableStateOf(existing?.adjustedReminderTime.orEmpty())
    }
    var repeatDays by remember {
        mutableStateOf(RepeatRule.toDaysOfWeekSet(existing?.repeatRule ?: "WEEKDAY"))
    }
    var suggestedMinutes by remember {
        mutableStateOf((existing?.suggestedMinutes ?: 20).toString())
    }
    var completionPolicy by remember {
        mutableStateOf(existing?.completionPolicy ?: "PARENT_REPORT")
    }
    var targetValue by remember { mutableStateOf(existing?.targetValue.orEmpty()) }
    var requiresEvidence by remember { mutableStateOf(existing?.requiresEvidence ?: false) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var titleError by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    val subjects = listOf("语文", "数学", "英语", "阅读", "运动", "外语")

    // 原型 .alert.warn：提醒时间与重复频率冲突提示
    val hasWeekendConflict = repeatDays.isNotEmpty() &&
        repeatDays.none { it == "SAT" || it == "SUN" } &&
        reminderHour.toIntOrNull() != null

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SheetHeader(
                title = when {
                    existing == null -> "新建学习任务"
                    existing.id == 0L -> "复制学习任务"
                    else -> "编辑学习任务"
                },
                onClose = onDismiss
            )

            if (hasWeekendConflict) {
                AlertBanner(
                    text = "提醒时间与重复频率冲突：周六不在重复周期内，该提醒将不会触发。",
                    kind = AlertKind.Warn
                )
            }

            FormField(
                label = "任务标题",
                value = title,
                onValueChange = { title = it; titleError = false },
                placeholder = "如：晚上朗读 20 分钟",
                isError = titleError,
                errorText = "标题不能为空"
            )

            // 科目 + 建议时长
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    DropdownField(
                        label = "科目 / 分类",
                        options = subjects,
                        selected = subject,
                        onSelect = { subject = it }
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    FieldLabel(text = "建议时长")
                    FormField(
                        label = null,
                        value = suggestedMinutes,
                        onValueChange = { suggestedMinutes = it.filter { c -> c.isDigit() }.take(3) },
                        placeholder = "约 30 分钟"
                    )
                }
            }

            FormField(
                label = "目标 / 期望值",
                value = targetValue,
                onValueChange = { targetValue = it },
                placeholder = "如：读 2 篇课文"
            )

            // 关联孩子
            if (children.isNotEmpty()) {
                DropdownField(
                    label = "关联孩子",
                    options = children.map { it.name },
                    selected = children.firstOrNull { it.id == selectedChildId }?.name
                        ?: children.first().name,
                    onSelect = { name ->
                        children.firstOrNull { it.name == name }?.let { selectedChildId = it.id }
                    }
                )
            }

            // 建议开始时间 + 校正提醒时间
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    FieldLabel(text = "建议开始时间")
                    FormField(
                        label = null,
                        value = "$reminderHour:$reminderMinute",
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(4)
                            when {
                                digits.length <= 2 -> {
                                    reminderHour = digits.padStart(2, '0')
                                    reminderMinute = "00"
                                }
                                else -> {
                                    reminderHour = digits.substring(0, 2)
                                    reminderMinute = digits.substring(2)
                                }
                            }
                        },
                        placeholder = "19:30"
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    FieldLabel(text = "校正后提醒时间")
                    FormField(
                        label = null,
                        value = adjustedReminderTime,
                        onValueChange = { adjustedReminderTime = it.filter { c -> c.isDigit() || c == ':' }.take(5) },
                        placeholder = "19:15"
                    )
                }
            }

            // 重复频率
            FieldLabel(text = "重复频率")
            WeekdayPicker(
                selected = repeatDays,
                onToggle = { day ->
                    repeatDays = if (day in repeatDays) repeatDays - day else repeatDays + day
                }
            )
            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                AppChip(
                    text = "每天除周末",
                    selected = repeatDays == setOf("MON", "TUE", "WED", "THU", "FRI"),
                    onClick = {
                        repeatDays = setOf("MON", "TUE", "WED", "THU", "FRI")
                    }
                )
                AppChip(
                    text = "每天",
                    selected = repeatDays.size == 7,
                    onClick = {
                        repeatDays = setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                    }
                )
                AppChip(
                    text = "一次性",
                    selected = repeatDays.isEmpty(),
                    onClick = { repeatDays = emptySet() }
                )
            }

            // 开始 / 结束日期
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    FieldLabel(text = "开始日期")
                    FormField(
                        label = null,
                        value = startDate,
                        onValueChange = { startDate = it },
                        placeholder = "2026-10-22"
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    FieldLabel(text = "结束日期")
                    FormField(
                        label = null,
                        value = endDate,
                        onValueChange = { endDate = it },
                        placeholder = "2027-01-22"
                    )
                }
            }

            // 完成方式
            FieldLabel(text = "完成方式")
            SegmentedControl(
                options = listOf(
                    "PARENT_REPORT" to "家长确认",
                    "SELF_REPORT" to "孩子录入",
                    "AUTO" to "自动判断"
                ),
                selected = completionPolicy,
                onSelect = { completionPolicy = it }
            )

            // 备注
            FieldLabel(text = "备注")
            FormField(
                label = null,
                value = notes,
                onValueChange = { notes = it },
                placeholder = "家长补充说明",
                singleLine = false
            )

            ToggleRow(
                title = "要求上传完成依据",
                description = "孩子可拍照或截图上传",
                checked = requiresEvidence,
                onCheckedChange = { requiresEvidence = it },
                showDivider = false
            )

            validationMessage?.let { message ->
                AlertBanner(text = message, kind = AlertKind.Error)
            }

            // 底部按钮：取消 / 删除 / 保存
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
                if (existing != null && existing.id != 0L) {
                    SecondaryButton(
                        text = "删除",
                        onClick = onDismiss,
                        danger = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                PrimaryButton(
                    text = "保存",
                    onClick = {
                        if (title.isBlank()) {
                            titleError = true
                            validationMessage = "任务标题不能为空"
                            return@PrimaryButton
                        }
                        val hour = reminderHour.toIntOrNull()
                        val minute = reminderMinute.toIntOrNull()
                        if (hour == null || minute == null || hour !in 0..23 || minute !in 0..59) {
                            validationMessage = "提醒时间格式不正确，小时需为 0-23，分钟需为 0-59"
                            return@PrimaryButton
                        }
                        val start = runCatching { LocalDate.parse(startDate) }.getOrNull()
                        val end = runCatching { LocalDate.parse(endDate) }.getOrNull()
                        if (start == null || end == null) {
                            validationMessage = "日期格式应为 YYYY-MM-DD"
                            return@PrimaryButton
                        }
                        if (start.isAfter(end)) {
                            validationMessage = "开始日期不能晚于结束日期"
                            return@PrimaryButton
                        }
                        val normalizedAdjusted = adjustedReminderTime.trim().takeIf { it.isNotEmpty() }
                        if (normalizedAdjusted != null &&
                            runCatching { LocalTime.parse(normalizedAdjusted) }.isFailure
                        ) {
                            validationMessage = "校正提醒时间格式应为 HH:mm"
                            return@PrimaryButton
                        }
                        val duration = suggestedMinutes.toIntOrNull()
                        if (duration == null || duration <= 0) {
                            validationMessage = "建议时长需为正整数"
                            return@PrimaryButton
                        }
                        validationMessage = null
                        val task = LearningTask(
                            id = existing?.id ?: 0L,
                            childId = selectedChildId,
                            title = title.trim(),
                            subject = subject,
                            startDate = start.toString(),
                            endDate = end.toString(),
                            reminderTime = LocalTime.of(hour, minute).toString(),
                            adjustedReminderTime = normalizedAdjusted,
                            repeatRule = RepeatRule.fromDaysOfWeekSet(repeatDays),
                            suggestedMinutes = duration,
                            completionPolicy = completionPolicy,
                            targetValue = targetValue.trim(),
                            requiresEvidence = requiresEvidence,
                            enabled = existing?.enabled ?: true,
                            deleted = false,
                            notes = notes.trim()
                        )
                        onSave(task)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** 原型 .field label：字段名。 */
@Composable
fun FieldLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = appPalette.ink2,
        modifier = modifier.padding(bottom = 6.dp)
    )
}

/**
 * 原型 .field input：圆角描边输入框。
 * Material3 的 OutlinedTextField 外观与原型差异较大，这里自绘以精确对齐。
 */
@Composable
fun FormField(
    label: String?,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null,
    singleLine: Boolean = true,
    suffix: String? = null
) {
    Column(modifier = modifier) {
        if (label != null) {
            FieldLabel(text = label)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadiusSmall))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.5.dp,
                    color = if (isError) appPalette.red else appPalette.line,
                    shape = RoundedCornerShape(AppRadiusSmall)
                )
                .padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = singleLine,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(appPalette.brand),
                modifier = Modifier.fillMaxWidth()
            )
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = appPalette.ink3
                )
            }
        }
        if (suffix != null && value.isNotEmpty()) {
            Text(
                text = suffix,
                style = MaterialTheme.typography.labelSmall,
                color = appPalette.ink3,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (isError && errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.labelSmall,
                color = appPalette.red,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/** 原型 .seg：分段控制器。 */
@Composable
fun SegmentedControl(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadiusSmall))
            .background(MaterialTheme.colorScheme.background)
            .padding(4.dp)
    ) {
        Row {
            options.forEach { (code, label) ->
                val on = code == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (on) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { onSelect(code) }
                        .padding(vertical = 9.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        color = if (on) appPalette.brand else appPalette.ink2
                    )
                }
            }
        }
    }
}

/** 原型 select：科目下拉（以 chip 形式横向展示）。 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SubjectSelector(
    subjects: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        subjects.forEach { s ->
            AppChip(
                text = s,
                selected = selected == s,
                onClick = { onSelect(s) }
            )
        }
    }
}

/**
 * 原型 .field select：下拉选择框。
 * Material3 的 ExposedDropdownMenu 外观与原型差异较大，这里自绘以精确对齐。
 */
@Composable
fun DropdownField(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = appPalette
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        FieldLabel(text = label)
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppRadiusSmall))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.5.dp,
                        color = palette.line,
                        shape = RoundedCornerShape(AppRadiusSmall)
                    )
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selected,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "▾",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink2
                )
            }
            androidx.compose.material3.DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
