package com.evertasktools.widget

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.clickable
import androidx.compose.ui.unit.DpSize
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import com.evertasktools.R
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.evertask.data.entity.TaskEntity
import com.evertask.data.model.Subtask
import com.evertask.data.repository.TaskRepository
import com.evertasktools.MainActivity
import kotlinx.coroutines.runBlocking

/**
 * Ever Task Tools - Jetpack Glance Widget
 * 
 * Widget Sizes Supported:
 * - 1x1 (40dp x 40dp): Task count badge only
 * - 2x2 (160dp x 160dp): Title + 2 checkboxes + progress bar
 * - 4x1 (320dp x 80dp): Banner with current subtask + actions
 * - 4x2 (320dp x 320dp): Full scrollable list with LazyColumn
 * 
 * Features:
 * - Real-time checkbox updates via actionRunCallback
 * - New Task button launches MainActivity
 * - Progress bars show completion percentage
 * - Material 3 dynamic colors
 * 
 * Implementation Notes:
 * - Uses Glance 1.0+ (AppWidgetProvider is deprecated)
 * - SizeMode.Responsive for multiple widget sizes
 * - LazyColumn for efficient scrolling in 4x2
 * - Database updates happen immediately on checkbox tap
 */

// Enterprise color palette matching the app theme
private val lightColorScheme = lightColorScheme(
    primary = Color(0xFF3EB489),
    onPrimary = Color(0xFFF5F5F0),
    primaryContainer = Color(0xFFD4F5E9),
    onPrimaryContainer = Color(0xFF1A1A1A),
    secondary = Color(0xFFF5F5F0),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFFFAFAF8),
    onSecondaryContainer = Color(0xFF1A1A1A),
    tertiary = Color(0xFF2E9A72),
    onTertiary = Color(0xFFF5F5F0),
    tertiaryContainer = Color(0xFFE8F8F2),
    onTertiaryContainer = Color(0xFF1A1A1A),
    error = Color(0xFFE53E3E),
    onError = Color(0xFFF5F5F0),
    surface = Color(0xFFFAFAF8),
    onSurface = Color(0xFF1A1A1A),
)

private val darkColorScheme = darkColorScheme(
    primary = Color(0xFF3EB489),
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF2E9A72),
    onPrimaryContainer = Color(0xFFF5F5F0),
    secondary = Color(0xFF262626),
    onSecondary = Color(0xFFF5F5F0),
    secondaryContainer = Color(0xFF262626),
    onSecondaryContainer = Color(0xFFF5F5F0),
    tertiary = Color(0xFF2E9A72),
    onTertiary = Color(0xFF1A1A1A),
    tertiaryContainer = Color(0xFF1E4838),
    onTertiaryContainer = Color(0xFF3EB489),
    error = Color(0xFFE53E3E),
    onError = Color(0xFFF5F5F0),
    surface = Color(0xFF262626),
    onSurface = Color(0xFFF5F5F0),
)

val WidgetColorScheme = ColorProviders(light = lightColorScheme, dark = darkColorScheme)

/**
 * Widget Receiver - Entry point for the widget system
 */
class EverTaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EverTaskWidget()
}

/**
 * Main Widget Implementation
 */
class EverTaskWidget : GlanceAppWidget() {
    
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            SMALL_SQUARE,   // 1x1
            MEDIUM_SQUARE,  // 2x2
            HORIZONTAL_RECTANGLE, // 4x1
            LARGE_RECTANGLE // 4x2
        )
    )
    
    companion object {
        // Widget size definitions (in dp)
        val SMALL_SQUARE = DpSize(40.dp, 40.dp)      // 1x1
        val MEDIUM_SQUARE = DpSize(160.dp, 160.dp)   // 2x2
        val HORIZONTAL_RECTANGLE = DpSize(320.dp, 80.dp) // 4x1
        val LARGE_RECTANGLE = DpSize(320.dp, 320.dp) // 4x2
    }
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme(
                colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    GlanceTheme.colors
                } else {
                    WidgetColorScheme
                }
            ) {
                WidgetContent()
            }
        }
    }
}

@Composable
private fun WidgetContent() {
    val context = LocalContext.current
    val size = LocalSize.current
    
    // Load tasks from repository
    val repository = runBlocking { TaskRepository.getInstance(context) }
    val tasks by repository.getActiveTasks()
        .collectAsState(initial = emptyList())
    
    val incompleteTasks = tasks.filter { !it.isCompleted }
    val currentTask = incompleteTasks.firstOrNull()
    
    // Route to appropriate layout based on size
    when {
        // 1x1 - Small square: Task count only
        size.width <= EverTaskWidget.SMALL_SQUARE.width * 1.5f -> {
            SmallSquareWidget(incompleteTasks.size)
        }
        // 4x1 - Horizontal: Banner with current subtask
        size.height <= EverTaskWidget.HORIZONTAL_RECTANGLE.height * 1.5f -> {
            HorizontalBannerWidget(currentTask)
        }
        // 2x2 - Medium square: Title + 2 checkboxes + progress
        size.width <= EverTaskWidget.MEDIUM_SQUARE.width * 1.5f -> {
            MediumSquareWidget(currentTask)
        }
        // 4x2 - Large rectangle: Full scrollable list
        else -> {
            LargeRectangleWidget(incompleteTasks)
        }
    }
}

/**
 * 1x1 Widget - Task Count Badge
 * Shows: Number of incomplete tasks with app icon
 */
@Composable
private fun SmallSquareWidget(taskCount: Int) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.primaryContainer)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = taskCount.toString(),
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onPrimaryContainer
                )
            )
            Text(
                text = if (taskCount == 1) "task" else "tasks",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = GlanceTheme.colors.onPrimaryContainer
                )
            )
        }
    }
}

/**
 * 2x2 Widget - Medium Square
 * Shows: Task title, 2 checkboxes, progress bar, New Task button
 */
@Composable
private fun MediumSquareWidget(task: TaskEntity?) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(16.dp)
            .padding(12.dp)
    ) {
        if (task != null) {
            // Task title
            Text(
                text = task.title,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface
                ),
                maxLines = 1
            )
            
            Spacer(GlanceModifier.height(8.dp))
            
            // Progress bar
            ProgressBar(task)
            
            Spacer(GlanceModifier.height(8.dp))
            
            // Show up to 2 subtasks
            val subtasksToShow = task.getSubtasks().take(2)
            subtasksToShow.forEach { subtask ->
                SubtaskCheckbox(task.id, subtask)
            }
            
            // Show "+X more" if there are more subtasks
            if (task.getSubtasks().size > 2) {
                Text(
                    text = "+${task.getSubtasks().size - 2} more",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    ),
                    modifier = GlanceModifier.padding(start = 32.dp, top = 4.dp)
                )
            }
        } else {
            // No tasks state
            Box(
                modifier = GlanceModifier.fillMaxWidth().height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tasks",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }
        }
        
        Spacer(GlanceModifier.defaultWeight())
        
        // New Task button
        NewTaskButton()
    }
}

/**
 * 4x1 Widget - Horizontal Banner
 * Shows: Current subtask + check/uncheck actions + New Task
 */
@Composable
private fun HorizontalBannerWidget(task: TaskEntity?) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(12.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (task != null) {
            // App icon
            Image(
                provider = ImageProvider(android.R.drawable.ic_menu_info_details),
                contentDescription = "Task",
                modifier = GlanceModifier.size(32.dp)
            )
            
            Spacer(GlanceModifier.width(12.dp))
            
            // Current subtask or task title
            val currentSubtask = task.getSubtasks().firstOrNull { !it.isCompleted }
            Column(
                modifier = GlanceModifier.defaultWeight()
            ) {
                Text(
                    text = task.title,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    ),
                    maxLines = 1
                )
                Text(
                    text = currentSubtask?.text ?: "All done!",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onSurface
                    ),
                    maxLines = 1
                )
            }
            
            Spacer(GlanceModifier.width(8.dp))
            
            // Action buttons
            if (currentSubtask != null) {
                // Check button
                Box(
                    modifier = GlanceModifier
                        .size(36.dp)
                        .background(GlanceTheme.colors.primary)
                        .cornerRadius(18.dp)
                        .clickable(
                            actionRunCallback<CheckBoxAction>(
                                actionParametersOf(
                                    CheckBoxAction.PARAM_TASK_ID to task.id,
                                    CheckBoxAction.PARAM_SUBTASK_ID to currentSubtask.id,
                                    CheckBoxAction.PARAM_COMPLETED to !currentSubtask.isCompleted
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(android.R.drawable.checkbox_on_background),
                        contentDescription = "Complete",
                        modifier = GlanceModifier.size(20.dp)
                    )
                }
            }
            
            Spacer(GlanceModifier.width(8.dp))
            
            // New Task button (small)
            Box(
                modifier = GlanceModifier
                    .size(36.dp)
                    .background(GlanceTheme.colors.secondaryContainer)
                    .cornerRadius(18.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(android.R.drawable.ic_input_add),
                    contentDescription = "New Task",
                    modifier = GlanceModifier.size(20.dp)
                )
            }
        } else {
            // No tasks state
            Text(
                text = "No pending tasks",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            
            // New Task button
            Box(
                modifier = GlanceModifier
                    .size(40.dp)
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(20.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(android.R.drawable.ic_input_add),
                    contentDescription = "New Task",
                    modifier = GlanceModifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * 4x2 Widget - Large Rectangle with Scrollable List
 * Shows: Full task list with all subtasks using LazyColumn
 */
@Composable
private fun LargeRectangleWidget(tasks: List<TaskEntity>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(16.dp)
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ever Task",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            
            // Task count badge
            Box(
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.primaryContainer)
                    .cornerRadius(12.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${tasks.size}",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onPrimaryContainer
                    )
                )
            }
        }
        
        Spacer(GlanceModifier.height(8.dp))
        
        if (tasks.isNotEmpty()) {
            // Scrollable task list
            LazyColumn(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight()
            ) {
                items(tasks.size) { index ->
                    TaskItem(tasks[index])
                    if (index < tasks.size - 1) {
                        Spacer(GlanceModifier.height(8.dp))
                    }
                }
            }
        } else {
            // Empty state
            Box(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No tasks yet",
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        text = "Tap + to create one",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }
            }
        }
        
        Spacer(GlanceModifier.height(8.dp))
        
        // New Task button at bottom
        NewTaskButton()
    }
}

/**
 * Task item for large widget
 */
@Composable
private fun TaskItem(task: TaskEntity) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(8.dp)
            .padding(8.dp)
    ) {
        // Task title and progress
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = task.title,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurface
                ),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1
            )
            
            // Progress text
            val completed = task.getSubtasks().count { it.isCompleted }
            val total = task.getSubtasks().size
            Text(
                text = "$completed/$total",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
        }
        
        Spacer(GlanceModifier.height(4.dp))
        
        // Subtasks (max 3 visible)
        task.getSubtasks().take(3).forEach { subtask ->
            SubtaskCheckbox(task.id, subtask, compact = true)
        }
        
        if (task.getSubtasks().size > 3) {
            Text(
                text = "+${task.getSubtasks().size - 3} more",
                style = TextStyle(
                    fontSize = 10.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                ),
                modifier = GlanceModifier.padding(start = 24.dp, top = 2.dp)
            )
        }
    }
}

/**
 * Subtask checkbox row
 */
@Composable
private fun SubtaskCheckbox(taskId: String, subtask: Subtask, compact: Boolean = false) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 2.dp else 4.dp)
            .clickable(
                actionRunCallback<CheckBoxAction>(
                    actionParametersOf(
                        CheckBoxAction.PARAM_TASK_ID to taskId,
                        CheckBoxAction.PARAM_SUBTASK_ID to subtask.id,
                        CheckBoxAction.PARAM_COMPLETED to !subtask.isCompleted
                    )
                )
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox icon
        val iconRes = if (subtask.isCompleted) {
            android.R.drawable.checkbox_on_background
        } else {
            android.R.drawable.checkbox_off_background
        }
        
        Image(
            provider = ImageProvider(resId = iconRes),
            contentDescription = if (subtask.isCompleted) "Completed" else "Not completed",
            modifier = GlanceModifier.size(if (compact) 18.dp else 24.dp)
        )
        
        Spacer(GlanceModifier.width(if (compact) 6.dp else 8.dp))
        
        Text(
            text = subtask.text,
            style = TextStyle(
                fontSize = if (compact) 11.sp else 13.sp,
                color = if (subtask.isCompleted) {
                    GlanceTheme.colors.onSurfaceVariant
                } else {
                    GlanceTheme.colors.onSurface
                }
            ),
            maxLines = 1
        )
    }
}

/**
 * Progress bar showing task completion
 */
@Composable
private fun ProgressBar(task: TaskEntity) {
    val completed = task.getSubtasks().count { it.isCompleted }
    val total = task.getSubtasks().size
    Column {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Progress",
                style = TextStyle(
                    fontSize = 11.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = "$completed/$total",
                style = TextStyle(
                    fontSize = 11.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
        }
        
        Spacer(GlanceModifier.height(4.dp))
        
    }
}

/**
 * New Task button
 */
@Composable
private fun NewTaskButton() {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(40.dp)
            .background(GlanceTheme.colors.primary)
            .cornerRadius(20.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(android.R.drawable.ic_input_add),
                contentDescription = null,
                modifier = GlanceModifier.size(18.dp)
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = "New Task",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onPrimary
                )
            )
        }
    }
}


