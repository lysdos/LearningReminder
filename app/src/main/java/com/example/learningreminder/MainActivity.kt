package com.example.learningreminder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.learningreminder.data.LearningDatabase
import com.example.learningreminder.ui.LearningApp
import com.example.learningreminder.ui.LearningReminderTheme
import com.example.learningreminder.viewmodel.ReportViewModel
import com.example.learningreminder.viewmodel.SettingsViewModel
import com.example.learningreminder.viewmodel.TasksViewModel
import com.example.learningreminder.viewmodel.TodayViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SHOW_TASK_ID = "showForTask"
    }

    private val container by lazy { AppContainer(this) }
    // 通知点击进入时带出的 taskId（PRD 3.4）
    private var showTaskId by mutableStateOf<Long?>(null)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // 打开 App / 新任务都从 Room 重排（PRD 6.2：系统闹钟不是最终数据源）
        CoroutineScope(Dispatchers.IO).launch {
            ReminderRefresherBootstrap.rescheduleAll(container)
        }

        setContent {
            LearningReminderTheme {
                val vmFactory = viewModelFactory {
                    initializer { TodayViewModel(container) }
                    initializer { TasksViewModel(container) }
                    initializer { ReportViewModel(container) }
                    initializer { SettingsViewModel(container) }
                }
                val todayVm: TodayViewModel = viewModel(factory = vmFactory)
                val tasksVm: TasksViewModel = viewModel(factory = vmFactory)
                val reportVm: ReportViewModel = viewModel(factory = vmFactory)
                val settingsVm: SettingsViewModel = viewModel(factory = vmFactory)
                LearningApp(
                    todayViewModel = todayVm,
                    tasksViewModel = tasksVm,
                    reportViewModel = com.example.learningreminder.ui.ReportScreenViewModelHost(reportVm),
                    settingsViewModel = com.example.learningreminder.ui.SettingsScreenViewModelHost(settingsVm),
                    openTaskId = showTaskId,
                    onOpenTaskHandled = { showTaskId = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val taskId = intent?.getLongExtra(EXTRA_SHOW_TASK_ID, -1L)?.takeIf { it > 0 }
            ?: return
        showTaskId = taskId
    }
}

/** 首次进入时确保至少有一个孩子被选中并完成重排。 */
object ReminderRefresherBootstrap {
    suspend fun rescheduleAll(container: AppContainer) {
        val existing = container.childRepository.getCurrentChild()
        val childId: Long = existing?.id ?: container.childRepository.ensureDefaultChild()
        val storedId = container.settings.currentChildId.first()
        if (storedId == null) {
            container.settings.setCurrentChildId(childId)
        }
        com.example.learningreminder.reminder.ReminderRefresher.rescheduleAll(container)
    }
}
