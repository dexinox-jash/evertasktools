package com.evertasktools.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.evertask.data.repository.TaskRepository
import com.evertasktools.MainActivity
import com.evertasktools.notification.TaskNotificationManager
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
 * - PARAM_TASK_ID (String): The task ID
 * - PARAM_SUBTASK_ID (String): The subtask ID  
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
        val PARAM_TASK_ID = ActionParameters.Key<String>("task_id")
        val PARAM_SUBTASK_ID = ActionParameters.Key<String>("subtask_id")
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
        Log.d(TAG, "Toggling subtask completion: taskId=$taskId, subtaskId=$subtaskId, completed=$completed")
        
        scope.launch {
            try {
                val repository = TaskRepository.getInstance(context)
                // Update database immediately
                repository.completeSubtask(taskId, subtaskId)
                Log.d(TAG, "Subtask toggle complete")
                
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
        taskId: String
    ) {
        try {
            val task = repository.getTaskById(taskId)
            task?.let {
                val allComplete = it.getSubtasks().all { sub -> sub.isCompleted }
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
        Log.d(TAG, "Launching new task activity from widget")
        // Launch main activity
        val intent = Intent(context, MainActivity::class.java).apply {
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
 * - PARAM_TASK_ID (String): The task ID
 * - PARAM_SUBTASK_ID (String): The subtask ID to skip
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
        
        val PARAM_TASK_ID = ActionParameters.Key<String>("task_id")
        val PARAM_SUBTASK_ID = ActionParameters.Key<String>("subtask_id")
    }
    
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[PARAM_TASK_ID] ?: return
        val subtaskId = parameters[PARAM_SUBTASK_ID] ?: return
        Log.d(TAG, "Skipping subtask: taskId=$taskId, subtaskId=$subtaskId")
        
        scope.launch {
            try {
                val repository = TaskRepository.getInstance(context)
                repository.skipSubtask(taskId, subtaskId)
                
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
 * - PARAM_TASK_ID (String): The task ID
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
        
        val PARAM_TASK_ID = ActionParameters.Key<String>("task_id")
    }
    
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[PARAM_TASK_ID] ?: return
        Log.d(TAG, "Completing task from widget: $taskId")
        
        scope.launch {
            try {
                val repository = TaskRepository.getInstance(context)
                repository.archiveTask(taskId)
                
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
 * - PARAM_TASK_ID (String): The task ID
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
        
        val PARAM_TASK_ID = ActionParameters.Key<String>("task_id")
    }
    
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[PARAM_TASK_ID] ?: return
        Log.d(TAG, "Deleting task from widget: $taskId")
        
        scope.launch {
            try {
                val repository = TaskRepository.getInstance(context)
                repository.deleteTaskPermanently(taskId)
                
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
    Log.d("WidgetActions", "Updating all widgets")
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
    } catch (e: Exception) {
        Log.e("WidgetActions", "Error updating widgets", e)
    }
}
