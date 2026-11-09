package com.example.learningreminder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learningreminder.AppContainer
import com.example.learningreminder.data.LearningTask
import com.example.learningreminder.data.TodayRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** 任务管理页：当前孩子全部任务列表 + CRUD。 */
class TasksViewModel(
    private val container: AppContainer
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val tasksFlow = container.settings.currentChildId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList<LearningTask>())
        else container.taskRepository.observeByChild(id)
    }

    val tasks: StateFlow<List<LearningTask>> = tasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveTask(task: LearningTask, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val id: Long = if (task.id == 0L) container.taskRepository.insert(task)
            else {
                container.taskRepository.update(task)
                task.id
            }
            val saved = task.copy(id = id)
            container.scheduler.schedule(
                saved,
                container.settings.reminderPreferencesSnapshot()
            )
            onSaved(id)
        }
    }

    fun setEnabled(taskId: Long, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            container.taskRepository.setEnabled(taskId, enabled)
            container.taskRepository.get(taskId)?.let {
                if (enabled) {
                    container.scheduler.schedule(
                        it,
                        container.settings.reminderPreferencesSnapshot()
                    )
                }
                else container.scheduler.cancel(taskId)
            }
        }
    }

    fun setEnabledBatch(taskIds: Set<Long>, enabled: Boolean) {
        if (taskIds.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            taskIds.forEach { taskId ->
                container.taskRepository.setEnabled(taskId, enabled)
                val task = container.taskRepository.get(taskId)
                if (enabled && task != null) {
                    container.scheduler.schedule(
                        task,
                        container.settings.reminderPreferencesSnapshot()
                    )
                } else {
                    container.scheduler.cancel(taskId)
                }
            }
        }
    }

    /**
     * 为选中任务补建过去日期的未开始记录，之后可在任务详情中补打卡。
     * 已有记录不会被覆盖。
     */
    fun backfillSelected(
        taskIds: Set<Long>,
        dates: List<LocalDate>,
        onDone: (Int) -> Unit = {}
    ) {
        if (taskIds.isEmpty() || dates.isEmpty()) return
        viewModelScope.launch {
            var created = 0
            withContext(Dispatchers.IO) {
                val today = LocalDate.now()
                val tasks = taskIds.mapNotNull { container.taskRepository.get(it) }
                dates.distinct()
                    .filter { !it.isAfter(today) }
                    .forEach { date ->
                        tasks.forEach { task ->
                            if (!TodayRules.isScheduledFor(task, date)) return@forEach
                            if (container.recordRepository.getRecord(
                                    task.id,
                                    date.toString()
                                ) != null
                            ) {
                                return@forEach
                            }
                            container.recordRepository.setStatus(
                                taskId = task.id,
                                childId = task.childId,
                                date = date.toString(),
                                status = "NOT_STARTED",
                                completedAt = null,
                                childNotes = null,
                                parentNotes = null,
                                recordedBy = "SYSTEM",
                                backfilled = date.isBefore(today),
                                autoStatus = true
                            )
                            created++
                        }
                    }
            }
            onDone(created)
        }
    }

    fun deleteTask(taskId: Long, keepRecords: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            container.scheduler.cancel(taskId)
            container.taskRepository.delete(taskId, keepRecords)
        }
    }
}
