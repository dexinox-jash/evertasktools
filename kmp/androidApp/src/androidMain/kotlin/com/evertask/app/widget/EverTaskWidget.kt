package com.evertask.app.widget

import android.content.Context
import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
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
import com.evertask.app.R
import com.evertask.app.ui.MainActivity
import com.evertask.shared.data.repository.TaskRepository
import com.evertask.shared.domain.model.Subtask
import com.evertask.shared.domain.model.Task
import kotlinx.coroutines.flow.first
import org.koin.java.KoinJavaComponent.getKoin

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
    surfaceVariant = Color(0xFFE8E8E8),
    onSurfaceVariant = Color(0xFF666666)
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
    surfaceVariant = Color(0xFF333333),
    onSurfaceVariant = Color(0xFFAAAAAA)
)

val WidgetColorScheme = ColorProviders(light = lightColorScheme, dark = darkColorScheme)

private object WidgetTypography {
    val displaySmall = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
    val titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
    val bodySmall = TextStyle(fontSize = 12.sp)
    val labelSmall = TextStyle(fontSize = 11.sp)
}

class EverTaskWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            SMALL_SQUARE,
            MEDIUM_SQUARE,
            HORIZONTAL_RECTANGLE,
            LARGE_RECTANGLE
        )
    )

    companion object {
        val SMALL_SQUARE = DpSize(40.dp, 40.dp)
        val MEDIUM_SQUARE = DpSize(120.dp, 120.dp)
        val HORIZONTAL_RECTANGLE = DpSize(250.dp, 60.dp)
        val LARGE_RECTANGLE = DpSize(250.dp, 150.dp)
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = getKoin().get<TaskRepository>()
        val tasks = repository.getActiveTasks().first()
        provideContent {
            GlanceTheme(
                colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    GlanceTheme.colors
                } else {
                    WidgetColorScheme
                }
            ) {
                WidgetContent(tasks = tasks)
            }
        }
    }
}

@Composable
private fun WidgetContent(tasks: List<Task>) {
    val size = LocalSize.current
    val incompleteTasks = tasks.filter { !it.isCompleted }
    val currentTask = incompleteTasks.firstOrNull()

    when {
        size.width <= EverTaskWidget.SMALL_SQUARE.width * 1.5f -> {
            SmallWidget(incompleteTasks.size)
        }
        size.height <= EverTaskWidget.HORIZONTAL_RECTANGLE.height * 1.5f -> {
            HorizontalWidget(currentTask)
        }
        size.width <= EverTaskWidget.MEDIUM_SQUARE.width * 1.5f -> {
            MediumWidget(currentTask)
        }
        else -> {
            LargeWidget(incompleteTasks)
        }
    }
}

@Composable
private fun SmallWidget(taskCount: Int) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(16.dp)
            .padding(1.dp)
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.primaryContainer)
                .cornerRadius(15.dp)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = taskCount.toString(),
                    style = WidgetTypography.displaySmall.copy(
                        color = GlanceTheme.colors.onPrimaryContainer
                    )
                )
                Text(
                    text = if (taskCount == 1) "task" else "tasks",
                    style = WidgetTypography.bodySmall.copy(
                        color = GlanceTheme.colors.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun MediumWidget(task: Task?) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(16.dp)
            .padding(1.dp)
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(15.dp)
                .padding(12.dp)
        ) {
            if (task != null) {
                Text(
                    text = task.title,
                    style = WidgetTypography.titleSmall.copy(
                        color = GlanceTheme.colors.onSurface
                    ),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(6.dp))
                ProgressIndicator(task)
                Spacer(GlanceModifier.height(6.dp))
                val subtasksToShow = task.subtasks.take(2)
                subtasksToShow.forEach { subtask ->
                    SubtaskCheckbox(task.id, subtask)
                }
                if (task.subtasks.size > 2) {
                    Text(
                        text = "+${task.subtasks.size - 2} more",
                        style = WidgetTypography.labelSmall.copy(
                            color = GlanceTheme.colors.onSurfaceVariant
                        ),
                        modifier = GlanceModifier.padding(start = 28.dp, top = 2.dp)
                    )
                }
            } else {
                Box(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks",
                        style = WidgetTypography.bodySmall.copy(
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            NewTaskButton()
        }
    }
}

@Composable
private fun HorizontalWidget(task: Task?) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(12.dp)
            .padding(1.dp)
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(11.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (task != null) {
                Box(
                    modifier = GlanceModifier
                        .size(28.dp)
                        .background(GlanceTheme.colors.primaryContainer)
                        .cornerRadius(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "i",
                        style = WidgetTypography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onPrimaryContainer
                        )
                    )
                }
                Spacer(GlanceModifier.width(10.dp))
                val currentSubtask = task.subtasks.firstOrNull { !it.isCompleted }
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = task.title,
                        style = WidgetTypography.labelSmall.copy(
                            color = GlanceTheme.colors.onSurfaceVariant
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = currentSubtask?.text ?: "All done!",
                        style = WidgetTypography.titleSmall.copy(
                            color = GlanceTheme.colors.onSurface
                        ),
                        maxLines = 1
                    )
                }
                Spacer(GlanceModifier.width(8.dp))
                if (currentSubtask != null) {
                    Box(
                        modifier = GlanceModifier
                            .size(32.dp)
                            .background(GlanceTheme.colors.primary)
                            .cornerRadius(16.dp)
                            .clickable(
                                actionRunCallback<ToggleSubtaskAction>(
                                    actionParametersOf(
                                        WidgetActions.PARAM_TASK_ID to task.id,
                                        WidgetActions.PARAM_SUBTASK_ID to currentSubtask.id
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.widget_checkbox_checked),
                            contentDescription = "Complete",
                            modifier = GlanceModifier.size(18.dp)
                        )
                    }
                }
                Spacer(GlanceModifier.width(8.dp))
                Box(
                    modifier = GlanceModifier
                        .size(32.dp)
                        .background(GlanceTheme.colors.secondaryContainer)
                        .cornerRadius(16.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        style = WidgetTypography.titleSmall.copy(
                            color = GlanceTheme.colors.onSecondaryContainer
                        )
                    )
                }
            } else {
                Text(
                    text = "No pending tasks",
                    style = WidgetTypography.bodySmall.copy(
                        color = GlanceTheme.colors.onSurfaceVariant
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Box(
                    modifier = GlanceModifier
                        .size(36.dp)
                        .background(GlanceTheme.colors.primary)
                        .cornerRadius(18.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        style = WidgetTypography.titleSmall.copy(
                            color = GlanceTheme.colors.onPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LargeWidget(tasks: List<Task>) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(16.dp)
            .padding(1.dp)
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(15.dp)
                .padding(12.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EverTask",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Box(
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.primaryContainer)
                        .cornerRadius(12.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${tasks.size}",
                        style = WidgetTypography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onPrimaryContainer
                        )
                    )
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            if (tasks.isNotEmpty()) {
                LazyColumn(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                ) {
                    items(tasks.size) { index ->
                        TaskListItem(tasks[index])
                        if (index < tasks.size - 1) {
                            Spacer(GlanceModifier.height(6.dp))
                        }
                    }
                }
            } else {
                Box(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No tasks yet",
                            style = WidgetTypography.bodySmall.copy(
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        Text(
                            text = "Tap + to create one",
                            style = WidgetTypography.labelSmall.copy(
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            NewTaskButton()
        }
    }
}

@Composable
private fun TaskListItem(task: Task) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(8.dp)
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = task.title,
                style = WidgetTypography.titleSmall.copy(
                    color = GlanceTheme.colors.onSurface
                ),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1
            )
            val completed = task.subtasks.count { it.isCompleted }
            val total = task.subtasks.size
            Text(
                text = "$completed/$total",
                style = WidgetTypography.labelSmall.copy(
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
        }
        if (task.subtasks.isNotEmpty()) {
            Spacer(GlanceModifier.height(4.dp))
            ProgressIndicator(task, compact = true)
            Spacer(GlanceModifier.height(4.dp))
        }
        task.subtasks.take(3).forEach { subtask ->
            SubtaskCheckbox(task.id, subtask, compact = true)
        }
        if (task.subtasks.size > 3) {
            Text(
                text = "+${task.subtasks.size - 3} more",
                style = WidgetTypography.labelSmall.copy(
                    color = GlanceTheme.colors.onSurfaceVariant
                ),
                modifier = GlanceModifier.padding(start = 24.dp, top = 2.dp)
            )
        }
    }
}

@Composable
private fun SubtaskCheckbox(taskId: String, subtask: Subtask, compact: Boolean = false) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 1.dp else 2.dp)
            .clickable(
                actionRunCallback<ToggleSubtaskAction>(
                    actionParametersOf(
                        WidgetActions.PARAM_TASK_ID to taskId,
                        WidgetActions.PARAM_SUBTASK_ID to subtask.id
                    )
                )
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconRes = if (subtask.isCompleted) {
            R.drawable.widget_checkbox_checked
        } else {
            R.drawable.widget_checkbox_unchecked
        }
        Image(
            provider = ImageProvider(resId = iconRes),
            contentDescription = if (subtask.isCompleted) "Completed" else "Not completed",
            modifier = GlanceModifier.size(if (compact) 16.dp else 20.dp)
        )
        Spacer(GlanceModifier.width(if (compact) 4.dp else 6.dp))
        Text(
            text = subtask.text,
            style = WidgetTypography.bodySmall.copy(
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

@Composable
private fun ProgressIndicator(task: Task, compact: Boolean = false) {
    val progress = task.calculateProgress()
    val totalSegments = if (compact) 5 else 10
    val filled = ((progress * totalSegments) / 100).coerceIn(0, totalSegments)
    val segmentWidth = if (compact) 8.dp else 12.dp
    val segmentHeight = if (compact) 4.dp else 6.dp
    val cornerRadius = if (compact) 2.dp else 3.dp
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalAlignment = Alignment.Start) {
            repeat(totalSegments) { index ->
                val color = if (index < filled) GlanceTheme.colors.primary else GlanceTheme.colors.surfaceVariant
                Box(
                    modifier = GlanceModifier
                        .width(segmentWidth)
                        .height(segmentHeight)
                        .background(color)
                        .cornerRadius(cornerRadius)
                ) {}
                if (index < totalSegments - 1) {
                    Spacer(GlanceModifier.width(2.dp))
                }
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        val completed = task.subtasks.count { it.isCompleted }
        val total = task.subtasks.size
        Text(
            text = "$completed/$total",
            style = WidgetTypography.labelSmall.copy(
                color = GlanceTheme.colors.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun NewTaskButton() {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(36.dp)
            .background(GlanceTheme.colors.primary)
            .cornerRadius(18.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "+",
                style = WidgetTypography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onPrimary
                ),
                modifier = GlanceModifier.padding(bottom = 1.dp)
            )
            Spacer(GlanceModifier.width(4.dp))
            Text(
                text = "New Task",
                style = WidgetTypography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onPrimary
                )
            )
        }
    }
}
