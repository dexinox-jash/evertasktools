package com.evertask.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.evertask.shared.domain.model.Task
import androidx.compose.ui.platform.LocalContext
import com.evertask.app.ui.theme.HeroShapes
import com.evertask.app.ui.theme.components.HeroCard
import com.evertask.app.ui.theme.components.HeroDivider
import com.evertask.app.ui.theme.components.HeroIconButton
import com.evertask.app.ui.theme.components.HeroListItem
import com.evertask.app.util.Haptics
import kotlinx.datetime.Instant
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    history: List<Task>,
    onBack: () -> Unit,
    onDeleteTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "History",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    HeroIconButton(
                        onClick = { Haptics.performClick(context); onBack() },
                        modifier = Modifier.semantics {
                            contentDescription = "Go back"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (history.isEmpty()) {
                EmptyHistoryView()
            } else {
                HistoryList(
                    history = history,
                    onDeleteTask = onDeleteTask
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "No history yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Completed tasks will appear here. Keep going!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryList(
    history: List<Task>,
    onDeleteTask: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HistoryStatsHeader(history = history)
        }
        
        items(
            items = history,
            key = { it.id }
        ) { task ->
            HistoryTaskItem(
                task = task,
                onDelete = { onDeleteTask(task.id) }
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HistoryStatsHeader(history: List<Task>) {
    val totalTasks = history.size
    val totalMinutes = history.sumOf { task ->
        task.subtasks.sumOf { it.durationMinutes }
    }
    val totalCompletedSubtasks = history.sumOf { task ->
        task.subtasks.count { it.isCompleted }
    }
    
    HeroCard(
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HistoryStat(
                    value = "$totalTasks",
                    label = "Tasks",
                    color = MaterialTheme.colorScheme.primary
                )
                HistoryStat(
                    value = "$totalMinutes",
                    label = "Minutes",
                    color = MaterialTheme.colorScheme.primary
                )
                HistoryStat(
                    value = "$totalCompletedSubtasks",
                    label = "Steps",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun HistoryStat(
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

private fun formatInstant(instant: Instant?): String {
    instant ?: return "Unknown"
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return dateFormat.format(Date(instant.toEpochMilliseconds()))
}

private fun formatTime(instant: Instant?): String {
    instant ?: return ""
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    return timeFormat.format(Date(instant.toEpochMilliseconds()))
}

@Composable
private fun HistoryTaskItem(
    task: Task,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val completedCount = task.subtasks.count { it.isCompleted }
    val totalCount = task.subtasks.size
    
    val completedDate = formatInstant(task.completedAt)
    val completedTime = formatTime(task.completedAt)
    val totalMinutes = task.subtasks.sumOf { it.durationMinutes }
    
    HeroCard {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            HeroListItem(
                headlineContent = {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                supportingContent = {
                    Text(
                        text = "$completedDate at $completedTime • ${totalMinutes}m • $completedCount/$totalCount steps",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                },
                trailingContent = {
                    HeroIconButton(
                        onClick = { Haptics.performClick(context); onDelete() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "${task.title}, completed on $completedDate with $completedCount of $totalCount steps done"
                    }
            )
        }
    }
}
