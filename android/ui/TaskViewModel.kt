package com.evertasktools.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evertask.data.repository.TaskRepository
import com.evertask.data.entity.TaskEntity
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// UI States
sealed class VoiceInputState {
    data object Idle : VoiceInputState()
    data object Listening : VoiceInputState()
    data object Processing : VoiceInputState()
    data class Error(val message: String) : VoiceInputState()
}

sealed class ScreenState {
    data object Empty : ScreenState()
    data class ActiveTask(val task: TaskEntity) : ScreenState()
    data class Completed(val task: TaskEntity) : ScreenState()
    data object History : ScreenState()
}

sealed class BackupUiState {
    data object Idle : BackupUiState()
    data object Loading : BackupUiState()
    data class Success(val message: String, val isEncrypted: Boolean = true) : BackupUiState()
    data class Error(val message: String) : BackupUiState()
}

data class TaskUiState(
    val screenState: ScreenState = ScreenState.Empty,
    val voiceInputState: VoiceInputState = VoiceInputState.Idle,
    val history: List<TaskEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentInputText: String = "",
    val backupState: BackupUiState = BackupUiState.Idle
)

class TaskViewModel(
    context: Context
) : ViewModel() {

    private lateinit var taskRepository: TaskRepository

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    // Events for one-time actions (like navigation, toasts)
    private val _events = MutableSharedFlow<TaskEvent>()
    val events: SharedFlow<TaskEvent> = _events.asSharedFlow()

    // Timeout for voice input silence
    private var voiceTimeoutJob: kotlinx.coroutines.Job? = null
    private var archiveJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            taskRepository = TaskRepository.getInstance(context)

            launch {
                taskRepository.getActiveTasks().collect { tasks ->
                    if (_uiState.value.screenState !is ScreenState.Completed) {
                        _uiState.update {
                            it.copy(
                                screenState = if (tasks.isNotEmpty()) {
                                    ScreenState.ActiveTask(tasks.first())
                                } else {
                                    ScreenState.Empty
                                }
                            )
                        }
                    }
                }
            }

            launch {
                taskRepository.getCompletedTasks().collect { tasks ->
                    _uiState.update { it.copy(history = tasks) }
                }
            }
        }
    }

    @OptIn(FlowPreview::class)
    fun startVoiceInput() {
        _uiState.update { it.copy(voiceInputState = VoiceInputState.Listening) }
        
        // Start 5-second silence timeout
        voiceTimeoutJob?.cancel()
        voiceTimeoutJob = viewModelScope.launch {
            delay(5000)
            if (_uiState.value.voiceInputState is VoiceInputState.Listening) {
                stopVoiceInput()
                _events.emit(TaskEvent.VoiceTimeout)
            }
        }
    }

    fun stopVoiceInput() {
        voiceTimeoutJob?.cancel()
        _uiState.update { it.copy(voiceInputState = VoiceInputState.Idle) }
    }

    fun onVoiceResult(transcript: String) {
        voiceTimeoutJob?.cancel()
        _uiState.update { it.copy(voiceInputState = VoiceInputState.Processing) }
        
        viewModelScope.launch {
            delay(300) // Brief processing delay for UX
            createTask(transcript)
            _uiState.update { it.copy(voiceInputState = VoiceInputState.Idle) }
        }
    }

    fun onVoiceError(error: String) {
        voiceTimeoutJob?.cancel()
        _uiState.update { it.copy(voiceInputState = VoiceInputState.Error(error)) }
        viewModelScope.launch {
            delay(2000)
            _uiState.update { it.copy(voiceInputState = VoiceInputState.Idle) }
        }
    }

    fun createTask(title: String) {
        if (title.isBlank()) return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            taskRepository.createTask(title)
                .onSuccess { task ->
                    _uiState.update {
                        it.copy(
                            screenState = ScreenState.ActiveTask(task),
                            isLoading = false,
                            currentInputText = ""
                        )
                    }
                    _events.emit(TaskEvent.TaskCreated(task.id))
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to create task: ${e.message}"
                        )
                    }
                }
        }
    }

    fun completeSubtask(taskId: String, subtaskId: String) {
        viewModelScope.launch {
            taskRepository.completeSubtask(taskId, subtaskId)
                .onSuccess { task ->
                    val allCompleted = task.getSubtasks().all { it.isCompleted }
                    if (allCompleted) {
                        _uiState.update { it.copy(screenState = ScreenState.Completed(task)) }
                        archiveJob?.cancel()
                        archiveJob = viewModelScope.launch {
                            delay(3000)
                            archiveTask(taskId)
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
        }
    }

    fun skipSubtask(taskId: String, subtaskId: String) {
        viewModelScope.launch {
            taskRepository.skipSubtask(taskId, subtaskId)
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
        }
    }

    fun undoCompleteSubtask(taskId: String, subtaskId: String) {
        viewModelScope.launch {
            taskRepository.completeSubtask(taskId, subtaskId)
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
        }
    }

    fun undoSkipSubtask(taskId: String, subtaskId: String) {
        viewModelScope.launch {
            taskRepository.unskipSubtask(taskId, subtaskId)
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
        }
    }

    fun archiveTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.archiveTask(taskId)
                .onSuccess {
                    if (_uiState.value.screenState is ScreenState.Completed) {
                        _uiState.update { it.copy(screenState = ScreenState.Empty) }
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
        }
    }

    fun undoArchive() {
        archiveJob?.cancel()
        val currentState = _uiState.value.screenState
        if (currentState is ScreenState.Completed) {
            viewModelScope.launch {
                taskRepository.uncompleteTask(currentState.task.id)
                    .onFailure { e ->
                        _uiState.update { it.copy(errorMessage = e.message) }
                    }
            }
        }
    }

    fun showHistory() {
        _uiState.update { it.copy(screenState = ScreenState.History) }
    }

    fun hideHistory() {
        _uiState.update { 
            it.copy(screenState = if (it.history.isNotEmpty() && it.screenState is ScreenState.History) {
                ScreenState.Empty
            } else {
                ScreenState.Empty
            })
        }
    }

    fun deleteHistoryTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
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

    fun getFirstUncheckedIndex(task: TaskEntity): Int {
        return task.getSubtasks().indexOfFirst { !it.isCompleted && !it.isSkipped }
    }

    // ==================== SECURE BACKUP OPERATIONS ====================

    /**
     * Exports tasks to an encrypted backup file.
     * Uses AES-256-GCM encryption via SecureBackupManager.
     */
    fun exportSecureBackup(context: Context) {
        _uiState.update { it.copy(backupState = BackupUiState.Loading) }

        viewModelScope.launch {
            try {
                val secureBackupManager = com.evertask.data.security.SecureBackupManager.getInstance(context)
                
                // Migrate legacy if exists
                if (secureBackupManager.hasLegacyBackup()) {
                    secureBackupManager.migrateLegacyBackup()
                }
                
                val result = secureBackupManager.exportSecureBackup()

                if (result.success) {
                    val message = if (result.tasksBackedUp > 0) {
                        "Backup created with ${result.tasksBackedUp} tasks"
                    } else {
                        "Backup created (no active tasks)"
                    }
                    _uiState.update { 
                        it.copy(backupState = BackupUiState.Success(message, isEncrypted = true))
                    }
                    _events.emit(TaskEvent.ShowSnackbar("Encrypted backup saved"))
                } else {
                    _uiState.update { 
                        it.copy(backupState = BackupUiState.Error(result.errorMessage ?: "Backup failed"))
                    }
                    _events.emit(TaskEvent.ShowSnackbar("Backup failed: ${result.errorMessage}"))
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(backupState = BackupUiState.Error(e.message ?: "Unknown error"))
                }
                _events.emit(TaskEvent.ShowSnackbar("Backup error: ${e.message}"))
            }
        }
    }

    /**
     * Imports tasks from encrypted backup file.
     * Supports both encrypted and legacy (for migration) backups.
     */
    fun importSecureBackup(context: Context) {
        _uiState.update { it.copy(backupState = BackupUiState.Loading) }

        viewModelScope.launch {
            try {
                val secureBackupManager = com.evertask.data.security.SecureBackupManager.getInstance(context)
                val result = secureBackupManager.importSecureBackup()

                if (result.success) {
                    val message = buildString {
                        append("Restored ${result.tasksRestored} tasks")
                        if (result.tasksSkipped > 0) {
                            append(" (${result.tasksSkipped} skipped as duplicates)")
                        }
                        if (result.migratedFromLegacy) {
                            append(" - Migrated from old format")
                        }
                    }
                    _uiState.update { 
                        it.copy(backupState = BackupUiState.Success(message, isEncrypted = !result.migratedFromLegacy))
                    }
                    _events.emit(TaskEvent.ShowSnackbar("Backup restored successfully"))
                } else {
                    _uiState.update { 
                        it.copy(backupState = BackupUiState.Error(result.errorMessage ?: "Restore failed"))
                    }
                    _events.emit(TaskEvent.ShowSnackbar("Restore failed: ${result.errorMessage}"))
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(backupState = BackupUiState.Error(e.message ?: "Unknown error"))
                }
                _events.emit(TaskEvent.ShowSnackbar("Restore error: ${e.message}"))
            }
        }
    }

    /**
     * Resets backup state to idle.
     */
    fun clearBackupState() {
        _uiState.update { it.copy(backupState = BackupUiState.Idle) }
    }

    /**
     * Checks if a backup exists (encrypted or legacy).
     */
    fun hasBackup(context: Context): Boolean {
        return com.evertask.data.security.SecureBackupManager.getInstance(context).hasBackup() ||
               com.evertask.data.security.SecureBackupManager.getInstance(context).hasLegacyBackup()
    }

    sealed class TaskEvent {
        data class TaskCreated(val taskId: String) : TaskEvent()
        data object VoiceTimeout : TaskEvent()
        data class ShowSnackbar(val message: String) : TaskEvent()
    }
}
