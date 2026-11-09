package com.example.learningreminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.learningreminder.data.LearningTask
import com.example.learningreminder.data.TaskWithStatus
import com.example.learningreminder.viewmodel.TasksViewModel
import com.example.learningreminder.viewmodel.TodayViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * 主壳：底部四 Tab（今日/任务/报告/设置），编辑与详情用 ModalBottomSheet 承载。
 * 数据全部来自 ViewModel，不再直接持有 Repository。
 *
 * 顶栏与底部导航的样式对齐《学习提醒App原型.html》的 .topbar / .bottom-nav。
 */
@Composable
fun LearningApp(
    todayViewModel: TodayViewModel,
    tasksViewModel: TasksViewModel,
    reportViewModel: ReportScreenViewModelHost,
    settingsViewModel: SettingsScreenViewModelHost,
    openTaskId: Long? = null,
    onOpenTaskHandled: () -> Unit = {}
) {
    var currentTab by remember { mutableStateOf(Tab.TODAY) }
    var editingTask by remember { mutableStateOf<LearningTask?>(null) }
    var showEditSheet by remember { mutableStateOf(false) }
    var detailTaskId by remember { mutableStateOf<Long?>(null) }
    var showDetailSheet by remember { mutableStateOf(false) }
    var showChildSheet by remember { mutableStateOf(false) }
    var showNotificationSheet by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val todayItems by todayViewModel.todayItems.collectAsStateWithLifecycle()
    val summary by todayViewModel.summary.collectAsStateWithLifecycle()
    val tasks by tasksViewModel.tasks.collectAsStateWithLifecycle()
    val currentChildId by todayViewModel.currentChildId.collectAsStateWithLifecycle()
    val children by settingsViewModel.viewModel.children.collectAsStateWithLifecycle()
    val currentChild = children.firstOrNull { it.id == currentChildId }

    LaunchedEffect(openTaskId) {
        val taskId = openTaskId ?: return@LaunchedEffect
        currentTab = Tab.TODAY
        detailTaskId = taskId
        showDetailSheet = true
        onOpenTaskHandled()
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            kotlinx.coroutines.delay(2000)
            toastMessage = null
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                child = currentChild,
                subtitle = childSubtitle(currentChild?.birthDate),
                onChildClick = { showChildSheet = true },
                onNotificationClick = { showNotificationSheet = true },
                onDndClick = { toastMessage = "已切换免打扰" }
            )
        },
        bottomBar = {
            AppBottomNav(
                current = currentTab,
                onSelect = { currentTab = it }
            )
        }
    ) { padding ->
        when (currentTab) {
            Tab.TODAY -> TodayScreen(
                items = todayItems,
                summary = summary,
                modifier = Modifier.padding(padding),
                onAddTask = {
                    editingTask = null
                    showEditSheet = true
                },
                onTaskClick = { id ->
                    detailTaskId = id
                    showDetailSheet = true
                },
                onQuickStatus = { item, status ->
                    if (item.task.requiresEvidence &&
                        status in setOf("DONE", "PARTIAL")
                    ) {
                        detailTaskId = item.task.id
                        showDetailSheet = true
                    } else {
                        todayViewModel.markStatus(item.task.id, status)
                    }
                },
                onGenerateToday = {
                    todayViewModel.generateToday()
                },
                onFilterClick = { showNotificationSheet = false }
            )
            Tab.TASKS -> TaskManageScreen(
                tasks = tasks,
                modifier = Modifier.padding(padding),
                onAddTask = {
                    editingTask = null
                    showEditSheet = true
                },
                onToggleEnabled = { id, enabled -> tasksViewModel.setEnabled(id, enabled) },
                onTaskClick = { id ->
                    detailTaskId = id
                    showDetailSheet = true
                },
                onEditTask = { id ->
                    editingTask = tasks.firstOrNull { it.id == id }
                    showEditSheet = true
                },
                onCopyTask = { id ->
                    tasks.firstOrNull { it.id == id }?.let { source ->
                        editingTask = source.copy(
                            id = 0L,
                            title = "${source.title}（副本）"
                        )
                        showEditSheet = true
                    }
                },
                onDelete = { id, keep -> tasksViewModel.deleteTask(id, keep) },
                onBatchEnabled = { ids, enabled ->
                    tasksViewModel.setEnabledBatch(ids, enabled)
                },
                onBackfill = { ids, dates, onDone ->
                    tasksViewModel.backfillSelected(ids, dates, onDone)
                }
            )
            Tab.REPORT -> ReportScreen(
                viewModel = reportViewModel.viewModel,
                modifier = Modifier.padding(padding)
            )
            Tab.SETTINGS -> SettingsScreen(
                viewModel = settingsViewModel.viewModel,
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (showEditSheet) {
        TaskEditSheet(
            existing = editingTask,
            childId = editingTask?.childId ?: currentChildId ?: 0L,
            children = children,
            onDismiss = { showEditSheet = false },
            onSave = { task ->
                tasksViewModel.saveTask(task)
                showEditSheet = false
            }
        )
    }
    if (showDetailSheet) {
        TaskDetailSheet(
            taskId = detailTaskId,
            todayViewModel = todayViewModel,
            onDismiss = { showDetailSheet = false }
        )
    }
    if (showChildSheet) {
        ChildPickerSheet(
            settingsViewModel = settingsViewModel.viewModel,
            onDismiss = { showChildSheet = false }
        )
    }
    if (showNotificationSheet) {
        NotificationCenterSheet(
            onDismiss = { showNotificationSheet = false },
            onOpenTask = { id ->
                showNotificationSheet = false
                detailTaskId = id
                showDetailSheet = true
            },
            onToast = { toastMessage = it }
        )
    }
    toastMessage?.let { message ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 96.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.94f),
                shape = CircleShape,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    }
    ReminderPermissionDialog()
}

enum class Tab { TODAY, TASKS, REPORT, SETTINGS }

/**
 * 原型 .bottom-nav：白色半透明底 + 顶部细线，四等分图标 + 文字。
 */
@Composable
private fun AppBottomNav(
    current: Tab,
    onSelect: (Tab) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 0.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(appPalette.line)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
            ) {
                NavItem(
                    icon = Icons.Rounded.Home,
                    label = "今日",
                    selected = current == Tab.TODAY,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(Tab.TODAY) }
                )
                NavItem(
                    icon = Icons.Rounded.Folder,
                    label = "任务",
                    selected = current == Tab.TASKS,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(Tab.TASKS) }
                )
                NavItem(
                    icon = Icons.Rounded.BarChart,
                    label = "报告",
                    selected = current == Tab.REPORT,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(Tab.REPORT) }
                )
                NavItem(
                    icon = Icons.Rounded.Settings,
                    label = "设置",
                    selected = current == Tab.SETTINGS,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(Tab.SETTINGS) }
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tint = if (selected) appPalette.brand else appPalette.ink3
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = tint
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = tint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/** 顶栏副标题：年龄 + 年级（原型为「9 岁 · 三年级」）。 */
private fun childSubtitle(birthDate: String?): String {
    val today = LocalDate.now()
    if (birthDate.isNullOrBlank()) {
        return "今天 · ${today.monthValue}月${today.dayOfMonth}日 " +
            today.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINESE)
    }
    val birth = runCatching { LocalDate.parse(birthDate) }.getOrNull()
        ?: return "今天 · ${today.monthValue}月${today.dayOfMonth}日"
    val age = today.year - birth.year - if (today.monthValue < birth.monthValue ||
        (today.monthValue == birth.monthValue && today.dayOfMonth < birth.dayOfMonth)
    ) 1 else 0
    return "$age 岁 · ${gradeLabel(birth, today)}"
}

/** 按生日推算年级（小学 1-6 年级，9 月 1 日为分界）。 */
private fun gradeLabel(birth: LocalDate, today: LocalDate): String {
    val grade = today.year - birth.year -
        if (today.monthValue < 9) 1 else 0
    return if (grade in 1..6) "$grade 年级" else "自主学习"
}

/** 屏幕之间的轻包装，避免 UI 文件彼此直接依赖。 */
class ReportScreenViewModelHost(val viewModel: com.example.learningreminder.viewmodel.ReportViewModel)
class SettingsScreenViewModelHost(val viewModel: com.example.learningreminder.viewmodel.SettingsViewModel)
