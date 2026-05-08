package com.evertasktools.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.evertask.data.repository.TaskRepository
import com.evertask.data.entity.TaskEntity
import com.evertask.data.model.Subtask
import com.evertasktools.R
import com.evertasktools.MainActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Objects

/**
 * TaskNotificationManager - Lock screen notifications for active tasks
 * 
 * Features:
 * - CATEGORY_ALARM priority for lock screen visibility
 * - Shows current task and next incomplete subtask
 * - Actions: Complete, Skip, End Session
 * - Full screen intent for alarm-style display
 * - setShowWhenLocked for immediate visibility
 * 
 * Notification Channels:
 * - task_active: Ongoing task notifications (HIGH priority)
 * - task_completed: Completion notifications (DEFAULT priority)
 * - task_reminder: Scheduled reminders (HIGH priority)
 * 
 * Usage:
 * ```kotlin
 * val notificationManager = TaskNotificationManager(context)
 * notificationManager.showTaskNotification(task)
 * notificationManager.dismissNotification()
 * ```
 */
class TaskNotificationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "TaskNotificationManager"
        
        // Channel IDs
        const val CHANNEL_ACTIVE = "task_active"
        const val CHANNEL_COMPLETED = "task_completed"
        const val CHANNEL_REMINDER = "task_reminder"
        
        // Notification IDs
        const val NOTIFICATION_ID_ACTIVE = 1001
        const val NOTIFICATION_ID_COMPLETED = 1002
        const val NOTIFICATION_ID_REMINDER = 1003
        
        // Action IDs
        const val ACTION_COMPLETE = "action_complete"
        const val ACTION_SKIP = "action_skip"
        const val ACTION_END = "action_end"
        const val ACTION_SNOOZE = "action_snooze"
        
        // Extra keys
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_SUBTASK_ID = "subtask_id"
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    
    init {
        createNotificationChannels()
    }
    
    /**
     * Create notification channels (required for Android O+)
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Active task channel - HIGH priority for lock screen
            val activeChannel = NotificationChannel(
                CHANNEL_ACTIVE,
                "Active Tasks",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows your current task on lock screen"
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setBypassDnd(true)
                
                // Sound for notifications
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes)
            }
            
            // Completed task channel - DEFAULT priority
            val completedChannel = NotificationChannel(
                CHANNEL_COMPLETED,
                "Completed Tasks",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when you complete tasks"
                setShowBadge(true)
            }
            
            // Reminder channel - HIGH priority
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled reminders for your tasks"
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            
            // Register channels
            notificationManager.createNotificationChannels(
                listOf(activeChannel, completedChannel, reminderChannel)
            )
        }
    }
    
    /**
     * Show active task notification on lock screen
     * Displays current task and next incomplete subtask
     */
    fun showTaskNotification(task: TaskEntity) {
        // Find next incomplete subtask
        val nextSubtask = task.getSubtasks().firstOrNull { !it.isCompleted }
        
        // Build notification content
        val title = task.title
        val content = nextSubtask?.let {
            "Next: ${it.text}"
        } ?: "All steps complete!"
        
        // Create intents for actions
        val completeIntent = createActionIntent(ACTION_COMPLETE, task.id, nextSubtask?.id)
        val skipIntent = createActionIntent(ACTION_SKIP, task.id, nextSubtask?.id)
        val endIntent = createActionIntent(ACTION_END, task.id)
        
        // Content intent - opens app
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_TASK_ID, task.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ACTIVE)
            .setSmallIcon(R.drawable.ic_notification_task)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle()
                .setBigContentTitle(title)
                .bigText(buildBigText(task, nextSubtask))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentIntent)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            // Actions
            .addAction(
                R.drawable.ic_check,
                "Complete",
                completeIntent
            )
            .apply {
                if (nextSubtask != null) {
                    addAction(R.drawable.ic_skip, "Skip", skipIntent)
                }
            }
            .addAction(
                R.drawable.ic_close,
                "End",
                endIntent
            )
            // Full screen intent for lock screen
            .setFullScreenIntent(contentIntent, true)
            // Progress
            .setProgress(
                task.getSubtasks().size,
                task.getSubtasks().count { it.isCompleted },
                false
            )
            .build()
        
        // Show notification
        notificationManager.notify(NOTIFICATION_ID_ACTIVE, notification)
    }
    
    /**
     * Show task completed notification
     */
    fun showTaskCompletedNotification(task: TaskEntity) {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_TASK_ID, task.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETED)
            .setSmallIcon(R.drawable.ic_notification_complete)
            .setContentTitle("Task Complete!")
            .setContentText("${task.title} - All steps finished!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_COMPLETED, notification)
        
        // Also dismiss active notification
        dismissActiveNotification()
    }
    
    /**
     * Show reminder notification for a task
     */
    fun showReminderNotification(task: TaskEntity, reminderText: String) {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_TASK_ID, task.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val snoozeIntent = createActionIntent(ACTION_SNOOZE, task.id)
        
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle("Reminder: ${task.title}")
            .setContentText(reminderText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_snooze, "Snooze", snoozeIntent)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_REMINDER, notification)
    }
    
    /**
     * Dismiss the active task notification
     */
    fun dismissActiveNotification() {
        notificationManager.cancel(NOTIFICATION_ID_ACTIVE)
    }
    
    /**
     * Dismiss all notifications
     */
    fun dismissAllNotifications() {
        notificationManager.cancelAll()
    }
    
    /**
     * Build detailed text for expanded notification
     */
    private fun buildBigText(task: TaskEntity, nextSubtask: Subtask?): String {
        val completed = task.getSubtasks().count { it.isCompleted }
        val total = task.getSubtasks().size
        
        val builder = StringBuilder()
        builder.appendLine("Progress: $completed/$total steps complete")
        builder.appendLine()
        
        if (nextSubtask != null) {
            builder.appendLine("Current step: ${nextSubtask.text}")
        } else {
            builder.appendLine("All steps complete! Great job!")
        }
        
        // Show remaining steps
        val remaining = task.getSubtasks().filter { !it.isCompleted }.drop(1)
        if (remaining.isNotEmpty()) {
            builder.appendLine()
            builder.appendLine("Up next:")
            remaining.take(3).forEach { subtask ->
                builder.appendLine("• ${subtask.text}")
            }
            if (remaining.size > 3) {
                builder.appendLine("• ... and ${remaining.size - 3} more")
            }
        }
        
        return builder.toString()
    }
    
    /**
     * Create PendingIntent for notification actions
     */
    private fun createActionIntent(
        action: String,
        taskId: String,
        subtaskId: String? = null
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_TASK_ID, taskId)
            subtaskId?.let { putExtra(EXTRA_SUBTASK_ID, it) }
        }
        
        return PendingIntent.getBroadcast(
            context,
            Objects.hash(taskId, action),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

/**
 * NotificationActionReceiver - Handles notification action button clicks
 * 
 * Receives broadcasts from notification actions and processes them:
 * - ACTION_COMPLETE: Mark subtask as complete
 * - ACTION_SKIP: Skip current subtask
 * - ACTION_END: Dismiss notification
 * - ACTION_SNOOZE: Snooze reminder
 */
class NotificationActionReceiver : android.content.BroadcastReceiver() {
    
    companion object {
        private const val TAG = "NotificationActionReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val taskId = intent.getStringExtra(TaskNotificationManager.EXTRA_TASK_ID) ?: return
        val subtaskId = intent.getStringExtra(TaskNotificationManager.EXTRA_SUBTASK_ID)
        android.util.Log.d(TAG, "Notification action received: $action, taskId=$taskId")
        
        when (action) {
            TaskNotificationManager.ACTION_COMPLETE -> {
                handleComplete(context, taskId, subtaskId)
            }
            TaskNotificationManager.ACTION_SKIP -> {
                handleSkip(context, taskId, subtaskId)
            }
            TaskNotificationManager.ACTION_END -> {
                handleEnd(context)
            }
            TaskNotificationManager.ACTION_SNOOZE -> {
                handleSnooze(context, taskId)
            }
        }
    }
    
    private fun handleComplete(context: Context, taskId: String, subtaskId: String?) {
        if (subtaskId == null) return
        
        GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val repository = TaskRepository.getInstance(context)
                repository.completeSubtask(taskId, subtaskId)
                
                // Check if task complete
                val task = repository.getTaskById(taskId)
                task?.let {
                    val allComplete = it.getSubtasks().all { sub -> sub.isCompleted }
                    val notificationManager = TaskNotificationManager(context)
                    if (allComplete) {
                        notificationManager.showTaskCompletedNotification(it)
                    } else {
                        // Update notification with next subtask
                        notificationManager.showTaskNotification(it)
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error completing subtask", e)
            }
        }
    }
    
    private fun handleSkip(context: Context, taskId: String, subtaskId: String?) {
        if (subtaskId == null) return
        
        GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val repository = TaskRepository.getInstance(context)
                repository.skipSubtask(taskId, subtaskId)
                
                val task = repository.getTaskById(taskId)
                task?.let {
                    val notificationManager = TaskNotificationManager(context)
                    notificationManager.showTaskNotification(it)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error skipping subtask", e)
            }
        }
    }
    
    private fun handleEnd(context: Context) {
        val notificationManager = TaskNotificationManager(context)
        notificationManager.dismissActiveNotification()
    }
    
    private fun handleSnooze(context: Context, taskId: String) {
        android.util.Log.d(TAG, "Snoozing reminder for task: $taskId")
        // Schedule reminder for 15 minutes later
        // Implementation depends on your scheduling mechanism (WorkManager/AlarmManager)
        val notificationManager = TaskNotificationManager(context)
        notificationManager.dismissActiveNotification()
        
        // TODO: Schedule snooze using WorkManager
    }
}
