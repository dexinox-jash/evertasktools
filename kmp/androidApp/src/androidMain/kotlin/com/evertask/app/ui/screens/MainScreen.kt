package com.evertask.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.compose.animation.AnimatedContent

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.evertask.app.ui.SubtaskItem
import com.evertask.app.ui.TaskViewModel
import com.evertask.app.ui.VoiceInputComponent
import com.evertask.shared.domain.model.Subtask
import com.evertask.shared.domain.model.Task
import androidx.compose.ui.platform.LocalContext
import com.evertask.app.ui.theme.HeroShapes
import com.evertask.app.ui.theme.components.HeroButton
import com.evertask.app.ui.theme.components.HeroButtonVariant
import com.evertask.app.ui.theme.components.HeroCard
import com.evertask.app.ui.theme.components.HeroFab
import com.evertask.app.ui.theme.components.HeroIconButton
import com.evertask.app.ui.theme.components.HeroInput
import com.evertask.app.ui.theme.components.HeroProgress
import com.evertask.app.util.Haptics

private enum class MainScreenState { LIST, HISTORY, COMPLETION }

/**
 * Main screen composable that displays the list of tasks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: TaskViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    
    // Show error messages in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            val isAlarmError = message.contains("exact alarms", ignoreCase = true)
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (isAlarmError) "Settings" else null,
                duration = androidx.compose.material3.SnackbarDuration.Long
            )
            if (isAlarmError && result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                viewModel.openExactAlarmSettings()
            }
            viewModel.clearError()
        }
    }
    
    val screenState = when {
        uiState.isHistoryVisible -> MainScreenState.HISTORY
        uiState.completedTask != null -> MainScreenState.COMPLETION
        else -> MainScreenState.LIST
    }
    
    AnimatedContent(
        targetState = screenState,
        transitionSpec = {
            if (targetState == MainScreenState.HISTORY) {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it / 3 } + fadeOut()
            } else if (initialState == MainScreenState.HISTORY) {
                slideInHorizontally { -it / 3 } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            } else {
                fadeIn() togetherWith fadeOut()
            }
        },
        label = "main_screen"
    ) { state ->
        when (state) {
            MainScreenState.LIST -> {
                Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EverTask Tools") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    HeroIconButton(onClick = { Haptics.performClick(context); viewModel.showHistory() }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            HeroFab(
                text = "New Task",
                onClick = { Haptics.performClick(context); showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.tasks.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.tasks.isEmpty() -> {
                    EmptyTasksView(onCreateClick = { showCreateDialog = true })
                }
                else -> {
                    TaskList(
                        tasks = uiState.tasks,
                        onSubtaskComplete = { taskId, subtaskId ->
                            viewModel.completeSubtask(taskId, subtaskId)
                        },
                        onArchiveTask = { taskId ->
                            viewModel.archiveTask(taskId)
                        },
                        onEditTask = { task ->
                            editingTask = task
                        },
                        onReorderTasks = { orderedIds -> viewModel.reorderTasks(orderedIds) }
                    )
                }
            }
        }
    }
            }
            MainScreenState.HISTORY -> {
                HistoryScreen(
                    history = uiState.history,
                    onBack = { viewModel.hideHistory() },
                    onDeleteTask = { taskId ->
                        viewModel.deleteTask(taskId)
                    }
                )
            }
            MainScreenState.COMPLETION -> {
                uiState.completedTask?.let { completedTask ->
                    TaskCompletionScreen(
                        taskTitle = completedTask.title,
                        totalMinutes = completedTask.subtasks.sumOf { it.durationMinutes },
                        completedCount = completedTask.subtasks.count { it.isCompleted },
                        skippedCount = 0,
                        onUndo = {
                            viewModel.dismissCompletion()
                            val lastCompleted = completedTask.subtasks.lastOrNull { it.isCompleted }
                            lastCompleted?.let {
                                viewModel.undoCompleteSubtask(completedTask.id, it.id)
                            }
                        },
                        onDone = {
                            Haptics.performClick(context)
                            viewModel.dismissCompletion()
                        }
                    )
                }
            }
        }
    }
    
    if (showCreateDialog) {
        CreateTaskDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title ->
                viewModel.createTask(title)
                showCreateDialog = false
            },
            voiceInputState = uiState.voiceInputState,
            currentText = uiState.currentInputText,
            onTextChange = { viewModel.updateInputText(it) },
            onTextSubmit = {
                viewModel.submitInputText()
                showCreateDialog = false
            },
            onStartVoice = { viewModel.startVoiceInput() },
            onStopVoice = { viewModel.stopVoiceInput() },
            onVoiceResult = { viewModel.onVoiceResult(it) },
            onVoiceError = { viewModel.onVoiceError(it) }
        )
    }

    editingTask?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { editingTask = null },
            onUpdateTitle = { taskId, title -> viewModel.updateTaskTitle(taskId, title) },
            onAddSubtask = { taskId, text -> viewModel.addSubtask(taskId, text) },
            onRemoveSubtask = { taskId, subtaskId -> viewModel.removeSubtask(taskId, subtaskId) },
            onUpdateSubtaskText = { taskId, subtaskId, text -> viewModel.updateSubtaskText(taskId, subtaskId, text) },
            onReorderSubtasks = { taskId, ids -> viewModel.reorderSubtasks(taskId, ids) }
        )
    }
}

/**
 * Displays a list of tasks in a lazy column.
 */
@Composable
fun TaskList(
    tasks: List<Task>,
    onSubtaskComplete: (String, String) -> Unit,
    onArchiveTask: (String) -> Unit,
    onEditTask: (Task) -> Unit,
    onReorderTasks: (List<String>) -> Unit
) {
    var taskList by remember(tasks) { mutableStateOf(tasks) }
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(taskList.size, key = { taskList[it].id }) { index ->
            val task = taskList[index]
            Box {
                TaskCard(
                task = task,
                onSubtaskComplete = { subtaskId -> onSubtaskComplete(task.id, subtaskId) },
                onArchive = { onArchiveTask(task.id) },
                onEdit = { onEditTask(task) },
                isDragging = draggedItemIndex == index,
                onDragStart = { draggedItemIndex = index },
                onDragEnd = {
                    draggedItemIndex = null
                    onReorderTasks(taskList.map { it.id })
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    val currentIndex = taskList.indexOf(task)
                    if (currentIndex == -1) return@TaskCard
                    val direction = if (dragAmount.y > 0) 1 else -1
                    val target = (currentIndex + direction).coerceIn(0, taskList.size - 1)
                    if (target != currentIndex) {
                        taskList = taskList.toMutableList().apply {
                            add(target, removeAt(currentIndex))
                        }
                        draggedItemIndex = target
                    }
                }
            )
            }
        }
    }
}

/**
 * Dialog for editing a task title and managing subtasks.
 */
@Composable
fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onUpdateTitle: (String, String) -> Unit,
    onAddSubtask: (String, String) -> Unit,
    onRemoveSubtask: (String, String) -> Unit,
    onUpdateSubtaskText: (String, String, String) -> Unit,
    onReorderSubtasks: (String, List<String>) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var title by remember(task.id) { mutableStateOf(task.title) }
    var subtasks by remember(task.id) { mutableStateOf(task.subtasks) }
    var newSubtaskText by remember(task.id) { mutableStateOf("") }
    var draggedSubtaskIndex by remember(task.id) { mutableStateOf<Int?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        HeroCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Edit Task",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))

                HeroInput(
                    value = title,
                    onValueChange = { title = it },
                    label = "Task title",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Subtasks",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                subtasks.forEachIndexed { index, subtask ->
                    val isDragging = draggedSubtaskIndex == index
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { draggedSubtaskIndex = index },
                                    onDragEnd = {
                                        draggedSubtaskIndex = null
                                        onReorderSubtasks(task.id, subtasks.map { it.id })
                                    },
                                    onDragCancel = {
                                        draggedSubtaskIndex = null
                                        onReorderSubtasks(task.id, subtasks.map { it.id })
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val direction = if (dragAmount.y > 0) 1 else -1
                                        val target = (index + direction).coerceIn(0, subtasks.size - 1)
                                        if (target != index) {
                                            subtasks = subtasks.toMutableList().apply {
                                                add(target, removeAt(index))
                                            }
                                            draggedSubtaskIndex = target
                                        }
                                    }
                                )
                            }
                            .shadow(if (isDragging) 8.dp else 0.dp)
                            .scale(if (isDragging) 1.02f else 1f)
                            .zIndex(if (isDragging) 1f else 0f)
                            .background(
                                if (isDragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                                shape = HeroShapes.medium
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeroInput(
                            value = subtask.text,
                            onValueChange = { newText ->
                                subtasks = subtasks.map {
                                    if (it.id == subtask.id) it.copy(text = newText) else it
                                }
                            },
                            modifier = Modifier.weight(1f),
                            label = null,
                            placeholder = "Subtask"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        HeroIconButton(
                            onClick = {
                                Haptics.performClick(context)
                                subtasks = subtasks.filter { it.id != subtask.id }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete subtask",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeroInput(
                        value = newSubtaskText,
                        onValueChange = { newSubtaskText = it },
                        modifier = Modifier.weight(1f),
                        label = null,
                        placeholder = "New subtask"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    HeroIconButton(
                        onClick = {
                            Haptics.performClick(context)
                            if (newSubtaskText.isNotBlank()) {
                                val newSubtask = Subtask(
                                    id = java.util.UUID.randomUUID().toString(),
                                    taskId = task.id,
                                    text = newSubtaskText,
                                    durationMinutes = 5,
                                    sortOrder = subtasks.size,
                                    isCompleted = false,
                                    completedAt = null
                                )
                                subtasks = subtasks + newSubtask
                                newSubtaskText = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add subtask",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    HeroButton(
                        text = "Cancel",
                        onClick = { Haptics.performClick(context); onDismiss() },
                        variant = HeroButtonVariant.SECONDARY
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    HeroButton(
                        text = "Save",
                        onClick = {
                            Haptics.performClick(context)
                            if (title != task.title) {
                                onUpdateTitle(task.id, title)
                            }
                            // Apply subtask changes
                            val originalIds = task.subtasks.map { it.id }.toSet()
                            val currentIds = subtasks.map { it.id }.toSet()
                            // Remove deleted
                            originalIds.filter { it !in currentIds }.forEach { id ->
                                onRemoveSubtask(task.id, id)
                            }
                            // Update existing / add new
                            subtasks.forEach { current ->
                                val original = task.subtasks.find { it.id == current.id }
                                when {
                                    original == null -> onAddSubtask(task.id, current.text)
                                    original.text != current.text -> onUpdateSubtaskText(task.id, current.id, current.text)
                                }
                            }
                            onDismiss()
                        },
                        variant = HeroButtonVariant.PRIMARY
                    )
                }
            }
        }
    }
}

/**
 * Card displaying a single task with its subtasks.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCard(
    task: Task,
    onSubtaskComplete: (String) -> Unit,
    onArchive: () -> Unit,
    onEdit: () -> Unit,
    isDragging: Boolean = false,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDrag: (androidx.compose.ui.input.pointer.PointerInputChange, androidx.compose.ui.geometry.Offset) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val progress = task.calculateProgress()
    val cardBackground = when {
        isDragging -> MaterialTheme.colorScheme.surfaceVariant
        task.isCompleted -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }

    val currentOnDragStart by androidx.compose.runtime.rememberUpdatedState(onDragStart)
    val currentOnDragEnd by androidx.compose.runtime.rememberUpdatedState(onDragEnd)
    val currentOnDrag by androidx.compose.runtime.rememberUpdatedState(onDrag)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { currentOnDragStart() },
                    onDragEnd = { currentOnDragEnd() },
                    onDragCancel = { currentOnDragEnd() },
                    onDrag = { change, offset -> currentOnDrag(change, offset) }
                )
            }
            .shadow(if (isDragging) 16.dp else 0.dp, shape = HeroShapes.medium)
            .scale(if (isDragging) 1.04f else 1f)
            .zIndex(if (isDragging) 1f else 0f)
    ) {
        HeroCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBackground, shape = HeroShapes.medium)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (task.isCompleted)
                            TextDecoration.LineThrough
                        else
                            TextDecoration.None,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Box {
                        HeroIconButton(onClick = { Haptics.performClick(context); showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    Haptics.performClick(context)
                                    onEdit()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Archive") },
                                onClick = {
                                    Haptics.performClick(context)
                                    onArchive()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Refresh, contentDescription = "Archive")
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                HeroProgress(
                    progress = progress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "$progress% complete",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                task.subtasks.forEachIndexed { index, subtask ->
                    val isActive = index == task.subtasks.indexOfFirst { !it.isCompleted }
                    SubtaskItem(
                        subtask = subtask,
                        isActive = isActive,
                        onComplete = { onSubtaskComplete(subtask.id) },
                        onUndoComplete = { onSubtaskComplete(subtask.id) }
                    )
                }
            }
        }
    }
}

/**
 * Empty state view shown when no tasks exist.
 */
@Composable
fun EmptyTasksView(onCreateClick: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier
                .width(64.dp)
                .height(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "You're all set",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Tap the + button to plan your next win",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        HeroButton(
            text = "Create Task",
            onClick = { Haptics.performClick(context); onCreateClick() },
            variant = HeroButtonVariant.PRIMARY
        )
    }
}

/**
 * Dialog for creating a new task with voice input support.
 */
@Composable
fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    voiceInputState: com.evertask.app.ui.VoiceInputState,
    currentText: String,
    onTextChange: (String) -> Unit,
    onTextSubmit: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onVoiceResult: (String) -> Unit,
    onVoiceError: (String) -> Unit
) {
    val context = LocalContext.current
    var taskTitle by remember { mutableStateOf("") }
    val effectiveText = currentText.takeIf { it.isNotBlank() } ?: taskTitle
    
    Dialog(onDismissRequest = onDismiss) {
        HeroCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Create New Task",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                VoiceInputComponent(
                    voiceInputState = voiceInputState,
                    currentText = effectiveText,
                    onTextChange = {
                        taskTitle = it
                        onTextChange(it)
                    },
                    onTextSubmit = {
                        onTextSubmit()
                    },
                    onStartVoice = onStartVoice,
                    onStopVoice = onStopVoice,
                    onVoiceResult = {
                        taskTitle = it
                        onVoiceResult(it)
                    },
                    onVoiceError = onVoiceError,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    HeroButton(
                        text = "Cancel",
                        onClick = { Haptics.performClick(context); onDismiss() },
                        variant = HeroButtonVariant.SECONDARY
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    HeroButton(
                        text = "Create",
                        onClick = {
                            Haptics.performClick(context)
                            if (effectiveText.isNotBlank()) {
                                onCreate(effectiveText)
                            }
                        },
                        variant = HeroButtonVariant.PRIMARY,
                        enabled = effectiveText.isNotBlank()
                    )
                }
            }
        }
    }
}
