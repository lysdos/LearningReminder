package com.example.learningreminder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learningreminder.AppContainer
import com.example.learningreminder.data.CompletionRecord
import com.example.learningreminder.data.LearningTask
import com.example.learningreminder.data.StatsCalculator
import com.example.learningreminder.data.TaskWithStatus
import com.example.learningreminder.data.TodayRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

class TodayViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val childId = container.settings.currentChildId

    val currentChildId: StateFlow<Long?> = childId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val todayTasks: kotlinx.coroutines.flow.Flow<List<LearningTask>> =
        childId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else container.taskRepository.observeByChild(id)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val todayRecords: Flow<List<CompletionRecord>> = childId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else container.recordRepository.observeByChild(id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayItems: StateFlow<List<TaskWithStatus>> =
        combine(todayTasks, todayRecords) { tasks, records ->
            val today = LocalDate.now()
            val todayStr = today.toString()
            val recordByTask = records.filter { it.date == todayStr }.associateBy { it.taskId }
            tasks.filter {
                TodayRules.isScheduledFor(it, today) || recordByTask[it.id] != null
            }
                .sortedWith(
                    compareBy(
                        {
                            runCatching { java.time.LocalTime.parse(it.effectiveReminderTime()) }
                                .getOrDefault(java.time.LocalTime.MAX)
                        },
                        { it.title }
                    )
                )
                .map { TaskWithStatus(it, recordByTask[it.id]) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<StatsCalculator.DayStats> = todayItems.map { items ->
        StatsCalculator.compute(
            items.map { it.task },
            items.mapNotNull { it.record },
            LocalDate.now()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsCalculator.DayStats(0, 0, 0f))

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                runExpiryCheck()
                delay(30_000)
            }
        }
    }

    private suspend fun runExpiryCheck() {
        val id = childId.first() ?: return
        val today = LocalDate.now()
        val todayStr = today.toString()
        val tasks = container.taskRepository.tasksForChild(id)
            .filter { TodayRules.isScheduledFor(it, today) }
        if (tasks.isEmpty()) return
        val records = container.recordRepository.observeByChild(id).first()
            .filter { it.date == todayStr }
            .associateBy { it.taskId }
        val now = LocalDateTime.now()
        tasks.forEach { task ->
            if (TodayRules.shouldAutoExpire(task, records[task.id], now)) {
                container.recordRepository.markExpiredIfEligible(task.id, id, todayStr)
            }
        }
    }

    fun markStatus(taskId: Long, status: String) {
        val id = currentChildId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            container.recordRepository.updateStatusPreservingDetails(
                taskId = taskId, childId = id, date = LocalDate.now().toString(),
                status = status,
                completedAt = if (status == "DONE" || status == "PARTIAL") {
                    System.currentTimeMillis()
                } else {
                    null
                },
                recordedBy = "PARENT"
            )
        }
    }

    fun saveRecord(
        task: LearningTask,
        date: LocalDate,
        status: String,
        completedAt: Long?,
        childNotes: String?,
        parentNotes: String?,
        evidenceUri: String?,
        evidenceText: String?,
        selfInitiated: Boolean?,
        exceededTarget: Boolean?,
        recordedBy: String,
        onResult: (Result<CompletionRecord>) -> Unit
    ) {
        viewModelScope.launch {
            val result = runCatching {
                require(!date.isAfter(LocalDate.now())) { "不能为未来日期打卡" }
                require(
                    !task.requiresEvidence ||
                        status !in setOf("DONE", "PARTIAL") ||
                        !evidenceUri.isNullOrBlank() ||
                        !evidenceText.isNullOrBlank()
                ) { "该任务需要填写文字或图片依据" }
                withContext(Dispatchers.IO) {
                    container.recordRepository.setStatus(
                        taskId = task.id,
                        childId = task.childId,
                        date = date.toString(),
                        status = status,
                        completedAt = completedAt,
                        childNotes = childNotes?.trim()?.takeIf { it.isNotEmpty() },
                        parentNotes = parentNotes?.trim()?.takeIf { it.isNotEmpty() },
                        recordedBy = recordedBy,
                        evidenceUri = evidenceUri?.takeIf { it.isNotBlank() },
                        evidenceText = evidenceText?.trim()?.takeIf { it.isNotEmpty() },
                        selfInitiated = selfInitiated,
                        exceededTarget = exceededTarget,
                        backfilled = date.isBefore(LocalDate.now())
                    )
                }
            }
            onResult(result)
        }
    }

    fun generateToday(onDone: (Int) -> Unit = {}) {
        val id = currentChildId.value ?: return
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                val today = LocalDate.now()
                val todayStr = today.toString()
                val existing = container.recordRepository.observeByChild(id).first()
                    .filter { it.date == todayStr }
                    .map { it.taskId }
                    .toSet()
                val tasks = container.taskRepository.tasksForChild(id)
                    .filter { TodayRules.isScheduledFor(it, today) && it.id !in existing }
                tasks.forEach { task ->
                    container.recordRepository.setStatus(
                        taskId = task.id,
                        childId = id,
                        date = todayStr,
                        status = "NOT_STARTED",
                        completedAt = null,
                        childNotes = null,
                        parentNotes = null,
                        recordedBy = "SYSTEM",
                        autoStatus = true
                    )
                }
                tasks.size
            }
            onDone(count)
        }
    }

    suspend fun loadTask(taskId: Long): LearningTask? =
        withContext(Dispatchers.IO) { container.taskRepository.getIncludingDeleted(taskId) }

    fun observeTaskRecords(taskId: Long): Flow<List<CompletionRecord>> =
        container.recordRepository.observeByTask(taskId)

    fun saveTask(
        task: LearningTask,
        onSaved: (Long) -> Unit = {}
    ) {
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

    fun toggleEnabled(taskId: Long, enabled: Boolean) {
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

    fun deleteTask(taskId: Long, keepRecords: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            container.scheduler.cancel(taskId)
            container.taskRepository.delete(taskId, keepRecords)
        }
    }

    fun saveNotes(taskId: Long, childNotes: String, parentNotes: String) {
        val id = currentChildId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val existing = container.recordRepository.getRecord(taskId, LocalDate.now().toString())
            if (existing != null) {
                container.recordRepository.updateNotes(existing.id, childNotes, parentNotes)
            } else {
                container.recordRepository.setStatus(
                    taskId = taskId,
                    childId = id,
                    date = LocalDate.now().toString(),
                    status = "NOT_STARTED",
                    completedAt = null,
                    childNotes = childNotes,
                    parentNotes = parentNotes,
                    recordedBy = "PARENT"
                )
            }
        }
    }
}
