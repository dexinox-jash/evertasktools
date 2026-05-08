package com.evertask.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.evertask.data.TaskRepository
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * WidgetActions - Action callbacks for Glance widget interactions
 * 
 * These classes handle user interactions with the widget:
 * - CheckBoxAction: Toggle subtask completion
 * - NewTaskAction: Launch main activity for new task
 * - RefreshAction: Force widget update
 * - SkipAction: Skip current subtask
 * 
 * All database updates happen immediately and trigger widget refresh.
 */

private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/**
 * CheckBoxAction - Toggle subtask completion status
 * 
 * Parameters:
 * - PARAM_TASK_ID (Long): The task ID
 * - PARAM_SUBTASK_ID (Long): The subtask ID  
 * - PARAM_COMPLETED (Boolean): New completion state
 * 
 * Usage in Glance:
 * ```kotlin
 * actionRunCallback<CheckBoxAction>(
 *     actionParametersOf(
 *         CheckBoxAction.PARAM_TASK_ID to taskId,
 *         CheckBoxAction.PARAM_SUBTASK_ID to subtaskId,
 *         CheckBoxAction.PARAM_COMPLETED to !currentState
 *     )
 * )
 * ```
 */
class CheckBoxAction : ActionCallback {
    
    companion object {
        private const val TAG = "CheckBoxAction"
        
        // Action parameter keys
        val PARAM_TASK_ID = ActionParameters.Key<Long>("task_id")
        val PARAM_SUBTASK_ID = ActionParameters.Key<Long>("subtask_id")
        val PARAM_COMPLETED = ActionParameters.Key<Boolean>("completed")
    }
    
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[PARAM_TASK_ID] ?: run {
            Log.w(TAG, "Missing task_id parameter")
            return
        }
        
        val subtaskId = parameters[PARAM_SUBTASK_ID] ?: run {
            Log.w(TAG, "Missing subtask_id parameter")
            return
        }
        
        val completed = parameters[PARAM_COMPLETED] ?: true
        
        Log.d(TAG, "Toggling subtask: taskId=$taskId, subtaskId=$subtaskId, completed=$completed")
        
        // Get repository via entry point
        val repository = getRepository(context)
        
        scope.launch {
            try {
                // Update database immediately
                repository.updateSubtaskCompletion(taskId, subtaskId, completed)
                Log.d(TAG, "Database updated successfully")
                
                // Trigger widget refresh
                withContext(Dispatchers.Main) {
                    updateAllWidgets(context)
                }
                
                // Optionally show notification if task completed
                if (completed) {
                    checkAndNotifyTaskCompletion(context, repository, taskId)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error updating subtask", e)
            }
        }
    }
    
    /**
     * Check if all subtasks are complete and show notification
     */
    private suspend fun checkAndNotifyTaskCompletion(
        context: Context,
        repository: TaskRepository,
        taskId: Long
    ) {
        try {
            val task = repository.getTaskById(taskId).first()
            task?.let {
                val allComplete = it.subtasks.all { sub -> sub.isCompleted }
                if (allComplete) {
                    // Show completion notification
                    val notificationManager = TaskNotificationManager(context)
                    notificationManager.showTaskCompletedNotification(it)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking task completion", e)
        }
    }
}

/**
 * NewTaskAction - Launch main activity to create new task
 * 
 * Usage in Glance:
 * ```kotlin
 * actionRunCallback<NewTaskAction>()
 * ```
 */
class NewTaskAction : ActionCallback {
    
    companion object {
        private const val TAG = "NewTaskAction"
    }
    
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Log.d(TAG, "Launching main activity for new task")
        
        // Launch main activity
        val intent = Intent(context, Class.forName("com.evertask.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("widget_action", "new_task")
        }
        context.startActivity(intent)
    }
}

/**
 * RefreshAction - Force widget refresh
 * 
 * Usage in Glance:
 * ```kotlin
 * actionRunCallback<RefreshAction>()
 * ```
 */
class RefreshAction : ActionCallback {
    
    companion object {
        private const val TAG = "RefreshAction"
    }
    
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Log.d(TAG, "Refreshing widget")
        updateAllWidgets(context)
    }
}

/**
 * SkipAction - Skip current subtask (mark as completed and move to next)
 * 
 * Parameters:
 * - PARAM_TASK_ID (Long): The task ID
 * - PARAM_SUBTASK_ID (Long): The subtask ID to skip
 * 
 * Usage in Glance:
 * ```kotlin
 * actionRunCallback<SkipAction>(
 *     actionParametersOf(
 *         SkipAction.PARAM_TASK_ID to taskId,
 *         SkipAction.PARAM_SUBTASK_ID to subtaskId
 *     )
 * )
 * ```
 */
class SkipAction : ActionCallback {
    
    companion object {
        private const val TAG = "SkipAction"
        
        val PARAM_TASK_ID = ActionParameters.Key<Long>("task_id")
        val PARAM_SUBTASK_ID = ActionParameters.Key<Long>("subtask_id")
    }
    
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[PARAM_TASK_ID] ?: return
        val subtaskId = parameters[PARAM_SUBTASK_ID] ?: return
        
        Log.d(TAG, "Skipping subtask: taskId=$taskId, subtaskId=$subtaskId")
        
        val repository = getRepository(context)
        
        scope.launch {
            try {
                // Mark as completed
                repository.updateSubtaskCompletion(taskId, subtaskId, true)
                
                // Refresh widget
                withContext(Dispatchers.Main) {
                    updateAllWidgets(context)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error skipping subtask", e)
            }
        }
    }
}

/**
 * CompleteTaskAction - Mark entire task as complete
 * 
 * Parameters:
 * - PARAM_TASK_ID (Long): The task ID
 * 
 * Usage in Glance:
 * ```kotlin
 * actionRunCallback<CompleteTaskAction>(
 *     actionParametersOf(
 *         CompleteTaskAction.PARAM_TASK_ID to taskId
 *     )
 * )
 * ```
 */
class CompleteTaskAction : ActionCallback {
    
    companion object {
        private const val TAG = "CompleteTaskAction"
        
        val PARAM_TASK_ID = ActionParameters.Key<Long>("task_id")
    }
    
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[PARAM_TASK_ID] ?: return
        
        Log.d(TAG, "Completing task: taskId=$taskId")
        
        val repository = getRepository(context)
        
        scope.launch {
            try {
                repository.updateTaskCompletion(taskId, true)
                
                withContext(Dispatchers.Main) {
                    updateAllWidgets(context)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error completing task", e)
            }
        }
    }
}

/**
 * DeleteTaskAction - Delete a task
 * 
 * Parameters:
 * - PARAM_TASK_ID (Long): The task ID
 * 
 * Usage in Glance:
 * ```kotlin
 * actionRunCallback<DeleteTaskAction>(
 *     actionParametersOf(
 *         DeleteTaskAction.PARAM_TASK_ID to taskId
 *     )
 * )
 * ```
 */
class DeleteTaskAction : ActionCallback {
    
    companion object {
        private const val TAG = "DeleteTaskAction"
        
        val PARAM_TASK_ID = ActionParameters.Key<Long>("task_id")
    }
    
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[PARAM_TASK_ID] ?: return
        
        Log.d(TAG, "Deleting task: taskId=$taskId")
        
        val repository = getRepository(context)
        
        scope.launch {
            try {
                val task = repository.getTaskById(taskId).first()
                task?.let {
                    repository.deleteTask(it)
                }
                
                withContext(Dispatchers.Main) {
                    updateAllWidgets(context)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting task", e)
            }
        }
    }
}

/**
 * Helper function to update all widgets
 */
suspend fun updateAllWidgets(context: Context) {
    try {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(EverTaskWidget::class.java)
        
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { prefs ->
                // Trigger refresh by updating timestamp
                prefs[longPreferencesKey("last_update")] = System.currentTimeMillis()
            }
            EverTaskWidget().update(context, glanceId)
        }
        
        Log.d("WidgetActions", "Updated ${glanceIds.size} widgets")
    } catch (e: Exception) {
        Log.e("WidgetActions", "Error updating widgets", e)
    }
}

/**
 * Get TaskRepository via Hilt entry point
 */
private fun getRepository(context: Context): TaskRepository {
    val entryPoint = EntryPointAccessors.fromApplication(
        context,
        WidgetEntryPoint::class.java
    )
    return entryPoint.taskRepository()
}

/**
 * Hilt entry point for widget actions
 */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface WidgetEntryPoint {
    fun taskRepository(): TaskRepository
}

// Import needed for preferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.evertask.notification.TaskNotificationManager
import kotlinx.coroutines.flow.first
