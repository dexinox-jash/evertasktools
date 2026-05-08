package com.evertasktools.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.evertasktools.R
import com.evertask.data.entity.TaskEntity
import com.evertasktools.ui.components.SubtaskItem
import com.evertasktools.ui.components.VoiceInputComponent
import com.evertasktools.ui.screens.HistoryScreen
import com.evertasktools.ui.screens.TaskCompletionScreen

import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: TaskViewModel,
    uiState: TaskUiState,
    modifier: Modifier = Modifier,
    onRequestVoicePermission: () -> Unit
) {
    val context = LocalContext.current
    
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = uiState.screenState,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "screen_content"
        ) { screenState ->
            when (screenState) {
                is ScreenState.Empty -> {
                    EmptyStateScreen(
                        voiceInputState = uiState.voiceInputState,
                        currentText = uiState.currentInputText,
                        onTextChange = viewModel::updateInputText,
                        onTextSubmit = viewModel::submitInputText,
                        onStartVoice = viewModel::startVoiceInput,
                        onStopVoice = viewModel::stopVoiceInput,
                        onVoiceResult = viewModel::onVoiceResult,
                        onVoiceError = viewModel::onVoiceError,
                        onShowHistory = viewModel::showHistory
                    )
                }
                is ScreenState.ActiveTask -> {
                    ActiveTaskScreen(
                        task = screenState.task,
                        voiceInputState = uiState.voiceInputState,
                        currentText = uiState.currentInputText,
                        onTextChange = viewModel::updateInputText,
                        onTextSubmit = viewModel::submitInputText,
                        onStartVoice = viewModel::startVoiceInput,
                        onStopVoice = viewModel::stopVoiceInput,
                        onVoiceResult = viewModel::onVoiceResult,
                        onVoiceError = viewModel::onVoiceError,
                        onCompleteSubtask = { subtaskId ->
                            viewModel.completeSubtask(screenState.task.id, subtaskId)
                        },
                        onSkipSubtask = { subtaskId ->
                            viewModel.skipSubtask(screenState.task.id, subtaskId)
                        },
                        onUndoComplete = { subtaskId ->
                            viewModel.undoCompleteSubtask(screenState.task.id, subtaskId)
                        },
                        onUndoSkip = { subtaskId ->
                            viewModel.undoSkipSubtask(screenState.task.id, subtaskId)
                        },
                        onShowHistory = viewModel::showHistory
                    )
                }
                is ScreenState.Completed -> {
                    val task = (uiState.screenState as? ScreenState.Completed)?.task
                        ?: uiState.history.firstOrNull()
                    
                    task?.let {
                        TaskCompletionScreen(
                            taskTitle = it.title,
                            totalMinutes = it.estimatedMinutes,
                            completedCount = it.getSubtasks().count { s -> s.isCompleted },
                            skippedCount = it.getSubtasks().count { s -> s.isSkipped },
                            onUndo = viewModel::undoArchive
                        )
                    }
                }
                is ScreenState.History -> {
                    HistoryScreen(
                        history = uiState.history,
                        onBack = viewModel::hideHistory,
                        onDeleteTask = viewModel::deleteHistoryTask
                    )
                }
            }
        }
        
        // Loading overlay
        AnimatedVisibility(
            visible = uiState.isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyStateScreen(
    voiceInputState: VoiceInputState,
    currentText: String,
    onTextChange: (String) -> Unit,
    onTextSubmit: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onVoiceResult: (String) -> Unit,
    onVoiceError: (String) -> Unit,
    onShowHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        HeaderWithHistory(onShowHistory = onShowHistory)
        
        Spacer(modifier = Modifier.weight(0.3f))
        
        // Main content centered
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App title
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Break down any task into simple steps",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Voice input
            VoiceInputComponent(
                voiceInputState = voiceInputState,
                currentText = currentText,
                onTextChange = onTextChange,
                onTextSubmit = onTextSubmit,
                onStartVoice = onStartVoice,
                onStopVoice = onStopVoice,
                onVoiceResult = onVoiceResult,
                onVoiceError = onVoiceError,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Suggestion chips
            Text(
                text = "Try saying:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SuggestionChips(
                suggestions = listOf(
                    "Clean my desk",
                    "Make coffee",
                    "Plan my day"
                ),
                onSuggestionClick = { suggestion ->
                    onTextChange(suggestion)
                    onTextSubmit()
                }
            )
        }
        
        Spacer(modifier = Modifier.weight(0.5f))
    }
}

@Composable
private fun ActiveTaskScreen(
    task: TaskEntity,
    voiceInputState: VoiceInputState,
    currentText: String,
    onTextChange: (String) -> Unit,
    onTextSubmit: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onVoiceResult: (String) -> Unit,
    onVoiceError: (String) -> Unit,
    onCompleteSubtask: (String) -> Unit,
    onSkipSubtask: (String) -> Unit,
    onUndoComplete: (String) -> Unit,
    onUndoSkip: (String) -> Unit,
    onShowHistory: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Scroll to first unchecked item when task is created
    LaunchedEffect(task.id) {
        val firstUncheckedIndex = task.getSubtasks().indexOfFirst { !it.isCompleted && !it.isSkipped }
        if (firstUncheckedIndex >= 0) {
            scope.launch {
                listState.animateScrollToItem(firstUncheckedIndex)
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        HeaderWithHistory(onShowHistory = onShowHistory)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Voice input (compact)
        VoiceInputComponent(
            voiceInputState = voiceInputState,
            currentText = currentText,
            onTextChange = onTextChange,
            onTextSubmit = onTextSubmit,
            onStartVoice = onStartVoice,
            onStopVoice = onStopVoice,
            onVoiceResult = onVoiceResult,
            onVoiceError = onVoiceError,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Task title
        Text(
            text = task.title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics {
                contentDescription = "Task: ${task.title}"
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val completedCount = task.getSubtasks().count { it.isCompleted }

        // Time estimate
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⏱",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "≈${task.estimatedMinutes} minutes total",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Progress
            val progressText = "$completedCount/${task.getSubtasks().size} done"
            Text(
                text = progressText,
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    completedCount == task.getSubtasks().size -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress bar
        LinearProgressIndicator(
            progress = { 
                if (task.getSubtasks().isEmpty()) 0f 
                else completedCount.toFloat() / task.getSubtasks().size 
            },
            modifier = Modifier.fillMaxWidth(),
            color = when {
                completedCount == task.getSubtasks().size -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Subtasks list
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                items = task.getSubtasks(),
                key = { _, subtask -> subtask.id }
            ) { index, subtask ->
                val isActive = !subtask.isCompleted && !subtask.isSkipped &&
                    task.getSubtasks().take(index).all { it.isCompleted || it.isSkipped }
                
                SubtaskItem(
                    subtask = subtask,
                    isActive = isActive,
                    onComplete = { onCompleteSubtask(subtask.id) },
                    onSkip = { onSkipSubtask(subtask.id) },
                    onUndoComplete = { onUndoComplete(subtask.id) },
                    onUndoSkip = { onUndoSkip(subtask.id) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action buttons
        ActionButtons(
            onCheck = {
                // Find first unchecked subtask and complete it
                val firstUnchecked = task.getSubtasks().firstOrNull { !it.isCompleted && !it.isSkipped }
                firstUnchecked?.let { onCompleteSubtask(it.id) }
            },
            onSkip = {
                // Find first unchecked subtask and skip it
                val firstUnchecked = task.getSubtasks().firstOrNull { !it.isCompleted && !it.isSkipped }
                firstUnchecked?.let { onSkipSubtask(it.id) }
            },
            hasUncheckedItems = task.getSubtasks().any { !it.isCompleted && !it.isSkipped }
        )
    }
}

@Composable
private fun HeaderWithHistory(
    onShowHistory: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo / App name
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // History button
        IconButton(
            onClick = onShowHistory,
            modifier = Modifier.semantics {
                contentDescription = "View task history"
            }
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SuggestionChips(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        suggestions.forEachIndexed { index, suggestion ->
            if (index > 0) {
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            SuggestionChip(
                onClick = { onSuggestionClick(suggestion) },
                label = { Text(suggestion) },
                modifier = Modifier.semantics {
                    contentDescription = "Try: $suggestion"
                }
            )
        }
    }
}

@Composable
private fun ActionButtons(
    onCheck: () -> Unit,
    onSkip: () -> Unit,
    hasUncheckedItems: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Check button
        Button(
            onClick = onCheck,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .semantics {
                    contentDescription = "Mark current subtask as complete"
                },
            shape = RoundedCornerShape(26.dp),
            enabled = hasUncheckedItems,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Check")
        }
        
        // Skip button
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .semantics {
                    contentDescription = "Skip current subtask"
                },
            shape = RoundedCornerShape(26.dp),
            enabled = hasUncheckedItems
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Skip →")
        }
    }
}

