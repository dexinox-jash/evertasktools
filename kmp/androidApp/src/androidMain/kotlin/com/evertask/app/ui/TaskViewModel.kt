package com.evertask.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evertask.app.scheduler.ReminderScheduler
import com.evertask.app.widget.WidgetUpdater
import com.evertask.shared.domain.model.Task
import com.evertask.shared.domain.usecase.AddSubtaskUseCase
import com.evertask.shared.domain.usecase.ArchiveTaskUseCase
import com.evertask.shared.domain.usecase.CompleteSubtaskUseCase
import com.evertask.shared.domain.usecase.CreateTaskUseCase
import com.evertask.shared.domain.usecase.DeleteTaskUseCase
import com.evertask.shared.domain.usecase.GetArchivedTasksUseCase
import com.evertask.shared.domain.usecase.GetTasksUseCase
import com.evertask.shared.domain.usecase.RemoveSubtaskUseCase
import com.evertask.shared.domain.usecase.ReorderSubtasksUseCase
import com.evertask.shared.domain.usecase.ReorderTasksUseCase
import com.evertask.shared.domain.usecase.UpdateSubtaskTextUseCase
import com.evertask.shared.domain.usecase.UpdateTaskTitleUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TaskViewModel(
    private val app: Application,
    private val createTaskUseCase: CreateTaskUseCase,
    private val getTasksUseCase: GetTasksUseCase,
    private val getArchivedTasksUseCase: GetArchivedTasksUseCase,
    private val completeSubtaskUseCase: CompleteSubtaskUseCase,
    private val archiveTaskUseCase: ArchiveTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val updateTaskTitleUseCase: UpdateTaskTitleUseCase,
    private val addSubtaskUseCase: AddSubtaskUseCase,
    private val removeSubtaskUseCase: RemoveSubtaskUseCase,
    private val updateSubtaskTextUseCase: UpdateSubtaskTextUseCase,
    private val reorderTasksUseCase: ReorderTasksUseCase,
    private val reorderSubtasksUseCase: ReorderSubtasksUseCase,
    private val reminderScheduler: ReminderScheduler,
    private val widgetUpdater: WidgetUpdater
) : AndroidViewModel(app) {
    
    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()
    
    private var archiveJob: kotlinx.coroutines.Job? = null
    private val archiveLock = Object()
    
    init {
        loadTasks()
        loadHistory()
    }
    
    private fun loadTasks() {
        _uiState.update { it.copy(isLoading = true) }
        getTasksUseCase()
            .onEach { tasks ->
                _uiState.update { it.copy(tasks = tasks, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }
    
    private fun loadHistory() {
        getArchivedTasksUseCase()
            .onEach { history ->
                _uiState.update { it.copy(history = history) }
            }
            .launchIn(viewModelScope)
    }
    
    fun createTask(title: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val task = createTaskUseCase(title)
                val scheduled = reminderScheduler.scheduleReminder(task)
                widgetUpdater.updateAllWidgets()
                task to scheduled
            }.onSuccess { (task, scheduled) ->
                val error = if (!scheduled) {
                    "Task created, but reminders are disabled. Please allow exact alarms in Settings."
                } else null
                _uiState.update {
                    it.copy(
                        tasks = it.tasks + task,
                        isLoading = false,
                        error = error,
                        currentInputText = "",
                        voiceInputState = VoiceInputState.Idle
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to create task",
                        voiceInputState = VoiceInputState.Idle
                    )
                }
            }
        }
    }

    fun openExactAlarmSettings() {
        reminderScheduler.openExactAlarmSettings()
    }
    
    fun completeSubtask(taskId: String, subtaskId: String) {
        viewModelScope.launch {
            runCatching {
                completeSubtaskUseCase(taskId, subtaskId)
            }.onSuccess { _ ->
                val updatedTasks = _uiState.value.tasks.map { task ->
                    if (task.id == taskId) {
                        task.copy(
                            subtasks = task.subtasks.map { subtask ->
                                if (subtask.id == subtaskId) subtask.copy(isCompleted = true) else subtask
                            }
                        )
                    } else task
                }
                val completedTask = updatedTasks.find { it.id == taskId }
                val allCompleted = completedTask?.subtasks?.all { it.isCompleted } == true && completedTask.subtasks.isNotEmpty()

                _uiState.update { it.copy(tasks = updatedTasks) }
                widgetUpdater.updateAllWidgets()

                if (allCompleted && completedTask != null) {
                    reminderScheduler.cancelReminder(taskId)
                    _uiState.update { it.copy(completedTask = completedTask) }
                    synchronized(archiveLock) {
                        synchronized(archiveLock) { archiveJob?.cancel() }
                        archiveJob = viewModelScope.launch {
                            delay(3000)
                            archiveTask(taskId)
                            _uiState.update { it.copy(completedTask = null) }
                        }
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(error = error.message ?: "Failed to complete subtask")
                }
            }
        }
    }
    
    fun undoCompleteSubtask(taskId: String, subtaskId: String) {
        viewModelScope.launch {
            runCatching {
                completeSubtaskUseCase(taskId, subtaskId)
            }.onSuccess { _ ->
                val updatedTasks = _uiState.value.tasks.map { task ->
                    if (task.id == taskId) {
                        task.copy(
                            subtasks = task.subtasks.map { subtask ->
                                if (subtask.id == subtaskId) subtask.copy(isCompleted = false) else subtask
                            }
                        )
                    } else task
                }
                val task = updatedTasks.find { it.id == taskId }
                task?.let { reminderScheduler.scheduleReminder(it) }
                widgetUpdater.updateAllWidgets()
                _uiState.update {
                    it.copy(
                        tasks = updatedTasks,
                        completedTask = null
                    )
                }
                synchronized(archiveLock) { archiveJob?.cancel() }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(error = error.message ?: "Failed to undo subtask completion")
                }
            }
        }
    }
    
    fun archiveTask(taskId: String) {
        viewModelScope.launch {
            runCatching {
                archiveTaskUseCase(taskId)
            }.onSuccess {
                reminderScheduler.cancelReminder(taskId)
                widgetUpdater.updateAllWidgets()
                _uiState.update { state ->
                    state.copy(
                        tasks = state.tasks.filter { it.id != taskId },
                        completedTask = if (state.completedTask?.id == taskId) null else state.completedTask
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(error = error.message ?: "Failed to archive task")
                }
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            runCatching {
                deleteTaskUseCase(taskId)
            }.onSuccess {
                reminderScheduler.cancelReminder(taskId)
                widgetUpdater.updateAllWidgets()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(error = error.message ?: "Failed to delete task")
                }
            }
        }
    }

    fun updateTaskTitle(taskId: String, title: String) {
        viewModelScope.launch {
            runCatching {
                updateTaskTitleUseCase(taskId, title)
            }.onSuccess {
                widgetUpdater.updateAllWidgets()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(error = error.message ?: "Failed to update task")
                }
            }
        }
    }

    fun addSubtask(taskId: String, text: String) {
        viewModelScope.launch {
            runCatching {
                addSubtaskUseCase(taskId, text)
            }.onSuccess {
                widgetUpdater.updateAllWidgets()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(error = error.message ?: "Failed to add subtask")
                }
            }
        }
    }

    fun removeSubtask(taskId: String, subtaskId: String) {
        viewModelScope.launch {
            runCatching {
                removeSubtaskUseCase(taskId, subtaskId)
            }.onSuccess {
                widgetUpdater.updateAllWidgets()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(error = error.message ?: "Failed to remove subtask")
                }
            }
        }
    }

    fun updateSubtaskText(taskId: String, subtaskId: String, text: String) {
        viewModelScope.launch {
            runCatching {
                updateSubtaskTextUseCase(taskId, subtaskId, text)
            }.onSuccess {
                widgetUpdater.updateAllWidgets()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(error = error.message ?: "Failed to update subtask")
                }
            }
        }
    }

    fun reorderTasks(orderedIds: List<String>) {
        viewModelScope.launch {
            runCatching { reorderTasksUseCase(orderedIds) }
                .onFailure { error -> _uiState.update { it.copy(error = error.message ?: "Failed to reorder tasks") } }
        }
    }

    fun reorderSubtasks(taskId: String, orderedSubtaskIds: List<String>) {
        viewModelScope.launch {
            runCatching { reorderSubtasksUseCase(taskId, orderedSubtaskIds) }
                .onFailure { error -> _uiState.update { it.copy(error = error.message ?: "Failed to reorder subtasks") } }
        }
    }
    
    fun showHistory() {
        _uiState.update { it.copy(isHistoryVisible = true) }
    }
    
    fun hideHistory() {
        _uiState.update { it.copy(isHistoryVisible = false) }
    }
    
    fun dismissCompletion() {
        synchronized(archiveLock) { archiveJob?.cancel() }
        _uiState.update { it.copy(completedTask = null) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun updateInputText(text: String) {
        _uiState.update { it.copy(currentInputText = text) }
    }
    
    fun submitInputText() {
        val text = _uiState.value.currentInputText
        if (text.isNotBlank()) {
            createTask(text)
        }
    }
    
    // Voice input
    fun startVoiceInput() {
        _uiState.update { it.copy(voiceInputState = VoiceInputState.Listening) }
    }
    
    fun stopVoiceInput() {
        _uiState.update { it.copy(voiceInputState = VoiceInputState.Idle) }
    }
    
    fun onVoiceResult(transcript: String) {
        _uiState.update { it.copy(voiceInputState = VoiceInputState.Processing) }
        createTask(transcript)
    }
    
    fun onVoiceError(error: String) {
        _uiState.update { it.copy(voiceInputState = VoiceInputState.Error(error)) }
        viewModelScope.launch {
            delay(2000)
            _uiState.update { it.copy(voiceInputState = VoiceInputState.Idle) }
        }
    }
}

data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val history: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isHistoryVisible: Boolean = false,
    val completedTask: Task? = null,
    val voiceInputState: VoiceInputState = VoiceInputState.Idle,
    val currentInputText: String = ""
)
