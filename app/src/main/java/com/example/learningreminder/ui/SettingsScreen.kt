package com.example.learningreminder.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.learningreminder.data.ChildProfile
import com.example.learningreminder.reminder.ReminderScheduler
import com.example.learningreminder.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 设置（PRD 3.4 + 6.3）：提醒开关、按周提醒、权限引导、导出。对齐原型 #page-set。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val children by viewModel.children.collectAsState()
    val currentChildId by viewModel.currentChildId.collectAsState()
    val reminderEnabled by viewModel.reminderEnabled.collectAsState()
    val weekdays by viewModel.remindWeekdays.collectAsState()
    val leadMinutes by viewModel.leadMinutes.collectAsState()
    val snoozeMinutes by viewModel.snoozeMinutes.collectAsState()
    val followUpEnabled by viewModel.followUpEnabled.collectAsState()
    val followUpMinutes by viewModel.followUpMinutes.collectAsState()
    val showOnLockScreen by viewModel.showOnLockScreen.collectAsState()
    val silentNotifications by viewModel.silentNotifications.collectAsState()
    var showAddChild by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ChildProfile?>(null) }
    var renameTarget by remember { mutableStateOf<ChildProfile?>(null) }
    var newChildName by remember { mutableStateOf("") }
    var newChildBirth by remember { mutableStateOf("") }
    var notifGranted by remember { mutableStateOf(true) }
    var exactAllowed by remember { mutableStateOf(true) }
    var batteryRestricted by remember { mutableStateOf(true) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    val scheduler = remember { ReminderScheduler(context) }
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                exportMessage = viewModel.exportJson(uri).fold(
                    onSuccess = {
                        "已导出：孩子 ${it.children}，任务 ${it.tasks}，记录 ${it.records}。" +
                            "文件已保存到所选位置。"
                    },
                    onFailure = { "导出失败：${it.message}" }
                )
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                exportMessage = viewModel.importJson(uri).fold(
                    onSuccess = {
                        "恢复完成：新增孩子 ${it.childrenAdded}，任务 ${it.tasksAdded}，" +
                            "记录 ${it.recordsAdded}；跳过已有/无效数据 ${it.skipped}。"
                    },
                    onFailure = { "恢复失败：${it.message}" }
                )
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        notifGranted = if (Build.VERSION.SDK_INT >= 33) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        exactAllowed = scheduler.canScheduleExact()
        batteryRestricted = context.getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(context.packageName) != true
        onPauseOrDispose {}
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // 原型 .section「提醒设置」
        GroupLabel(text = "提醒设置")
        AppCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp)) {
            ToggleRow(
                title = "🔔 系统通知提醒",
                description = "关闭后不再推送提醒",
                checked = reminderEnabled,
                onCheckedChange = { viewModel.setReminderEnabled(it) }
            )
            ToggleRow(
                title = "🔓 锁屏提示",
                description = "允许在锁屏显示任务摘要",
                checked = showOnLockScreen,
                onCheckedChange = { viewModel.setShowOnLockScreen(it) }
            )
            ToggleRow(
                title = "🌙 免打扰时段",
                description = "22:00 - 07:00 不额外提醒",
                checked = silentNotifications,
                onCheckedChange = { viewModel.setSilentNotifications(it) },
                showDivider = false
            )
        }

        // 原型 .section「提醒方式」
        Spacer(modifier = Modifier.height(16.dp))
        GroupLabel(text = "提醒方式")
        AppCard {
            FieldLabel(text = "提前提醒")
            SegmentedControl(
                options = listOf(
                    "0" to "不提前",
                    "5" to "5分钟",
                    "15" to "15分钟",
                    "30" to "30分钟"
                ),
                selected = leadMinutes.toString(),
                onSelect = { viewModel.setLeadMinutes(it.toInt()) }
            )
            Spacer(modifier = Modifier.height(14.dp))
            FieldLabel(text = "稍后提醒")
            SegmentedControl(
                options = listOf(
                    "5" to "5分钟",
                    "10" to "10分钟",
                    "15" to "15分钟",
                    "30" to "30分钟"
                ),
                selected = snoozeMinutes.toString(),
                onSelect = { viewModel.setSnoozeMinutes(it.toInt()) }
            )
            if (followUpEnabled) {
                Spacer(modifier = Modifier.height(14.dp))
                FieldLabel(text = "到点未完成再次提醒")
                SegmentedControl(
                    options = listOf(
                        "5" to "5分钟",
                        "10" to "10分钟",
                        "15" to "15分钟",
                        "30" to "30分钟"
                    ),
                    selected = followUpMinutes.toString(),
                    onSelect = { viewModel.setFollowUpMinutes(it.toInt()) }
                )
            }
        }

        // 原型 .section「每周提醒日」
        Spacer(modifier = Modifier.height(16.dp))
        GroupLabel(text = "每周提醒日")
        AppCard {
            WeekdayPicker(
                selected = weekdays,
                onToggle = { day ->
                    val updated = if (day in weekdays) weekdays - day else weekdays + day
                    viewModel.setRemindWeekdays(updated)
                }
            )
            Text(
                text = "默认工作日开启提醒，周末为自主学习时间。",
                style = MaterialTheme.typography.labelSmall,
                color = appPalette.ink3,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        // 原型 .section「系统权限」
        Spacer(modifier = Modifier.height(16.dp))
        GroupLabel(text = "系统权限")
        AppCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp)) {
            PermissionRow(
                emoji = "🔔",
                title = "通知权限",
                description = "用于发送任务提醒通知",
                statusText = if (notifGranted) "已开启" else "未开启",
                statusKind = if (notifGranted) PermissionStatusKind.Ok else PermissionStatusKind.Missing,
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                }
            )
            PermissionRow(
                emoji = "⏰",
                title = "精确闹钟",
                description = "确保按分钟精度触发提醒",
                statusText = if (exactAllowed) "已开启" else "仅近似",
                statusKind = if (exactAllowed) PermissionStatusKind.Ok else PermissionStatusKind.Partial,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                }
            )
            PermissionRow(
                emoji = "🔋",
                title = "电池优化豁免",
                description = "避免系统清理后台导致提醒失效",
                statusText = if (batteryRestricted) "去修复" else "已豁免",
                statusKind = if (batteryRestricted) PermissionStatusKind.Missing else PermissionStatusKind.Ok,
                onClick = {
                    if (batteryRestricted) {
                        val request = Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        )
                        runCatching { context.startActivity(request) }.onFailure {
                            context.startActivity(
                                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            )
                        }
                    }
                }
            )
            PermissionRow(
                emoji = "📷",
                title = "相机 / 相册",
                description = "仅上传打卡证据时使用",
                statusText = "已开启",
                statusKind = PermissionStatusKind.Ok,
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            )
        }

        // 原型 .section「孩子管理」
        Spacer(modifier = Modifier.height(16.dp))
        GroupLabel(text = "孩子管理")
        AppCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp)) {
            children.forEach { child ->
                ListItemRow(
                    icon = null,
                    title = child.name + if (child.id == currentChildId) "（当前）" else "",
                    subtitle = if (child.id == currentChildId) "正在使用该孩子的任务与记录" else "点击「设为当前」切换",
                    trailingText = if (child.id != currentChildId) "设为当前" else null,
                    showArrow = false,
                    onClick = if (child.id != currentChildId) {
                        { viewModel.selectChild(child.id) }
                    } else {
                        null
                    }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PrimaryButton(
                    text = "＋ 添加孩子",
                    onClick = { showAddChild = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 原型 .section「数据与备份」
        Spacer(modifier = Modifier.height(16.dp))
        GroupLabel(text = "数据与备份")
        AppCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp)) {
            ListItemRow(
                icon = Icons.Rounded.Download,
                title = "导出 JSON 备份",
                subtitle = "包含孩子、任务、记录与提醒配置",
                onClick = {
                    exportLauncher.launch("learning_reminder_export_${LocalDate.now()}.json")
                }
            )
            ListItemRow(
                icon = Icons.Rounded.Upload,
                title = "导入备份",
                subtitle = "按唯一键去重，不覆盖已有记录",
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }
            )
            ListItemRow(
                icon = Icons.Rounded.FolderOpen,
                title = "本地数据库",
                subtitle = "learning_reminder.db · 私有目录",
                showArrow = true,
                onClick = null
            )
        }

        exportMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = appPalette.ink3,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // 原型 .section「多设备同步（预留）」
        Spacer(modifier = Modifier.height(16.dp))
        GroupLabel(text = "多设备同步（预留）")
        AppCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp)) {
            ToggleRow(
                title = "☁️ 启用云同步",
                description = "v1.0 未开启，后续支持家长多设备同步",
                checked = false,
                onCheckedChange = { },
                showDivider = false
            )
        }

        // 原型 .section「无障碍与外观」
        Spacer(modifier = Modifier.height(16.dp))
        GroupLabel(text = "无障碍与外观")
        AppCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp)) {
            ToggleRow(
                title = "🔠 大字号模式",
                description = "放大任务标题与提醒文字",
                checked = false,
                onCheckedChange = { },
                icon = Icons.Rounded.TextFields
            )
            ToggleRow(
                title = "🎙️ 语音播报提醒",
                description = "朗读任务名称与科目",
                checked = false,
                onCheckedChange = { },
                icon = Icons.Rounded.RecordVoiceOver
            )
            ListItemRow(
                icon = null,
                title = "孩子专属简化界面",
                subtitle = "v1.1 支持，大按钮、少文字",
                trailingText = "即将推出",
                showArrow = false
            )
        }
    }

    if (showAddChild) {
        AlertDialog(
            onDismissRequest = { showAddChild = false },
            title = { Text("添加孩子") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = newChildName,
                        onValueChange = { newChildName = it },
                        label = { Text("名字") }
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = newChildBirth,
                        onValueChange = { newChildBirth = it },
                        label = { Text("生日 YYYY-MM-DD（可空）") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newChildName.isNotBlank()) {
                        viewModel.addChild(
                            newChildName.trim(),
                            newChildBirth.takeIf { it.isNotBlank() }
                        )
                        newChildName = ""
                        newChildBirth = ""
                        showAddChild = false
                    }
                }) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddChild = false }) { Text("取消") }
            }
        )
    }

    renameTarget?.let { child ->
        ChildRenameDialog(
            child = child,
            viewModel = viewModel,
            onDismiss = { renameTarget = null }
        )
    }
    deleteTarget?.let { child ->
        ChildDeleteDialog(
            child = child,
            viewModel = viewModel,
            onDismiss = { deleteTarget = null }
        )
    }
}
