package com.evertask.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.evertask.app.EverTaskApplication
import com.evertask.app.ui.MainActivity

object TaskNotification {
    private const val CHANNEL_ID = "task_channel"

    private fun stableRequestCode(id: String, salt: Int): Int {
        return kotlin.math.abs(id.fold(salt) { acc, c -> 31 * acc + c.code })
    }

    fun showTaskNotification(context: Context, taskId: String, title: String, content: String, subtaskId: String? = null) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Task Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            component = ComponentName(context, MainActivity::class.java)
            putExtra("taskId", taskId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            stableRequestCode(taskId, 0),
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val token = EverTaskApplication.notificationToken
        val completeIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.evertask.app.ACTION_COMPLETE"
            component = ComponentName(context, MainActivity::class.java)
            putExtra("taskId", taskId)
            putExtra("subtaskId", subtaskId)
            putExtra("action", "complete")
            putExtra("evertask_internal_action", true)
            putExtra("evertask_internal_token", token)
        }
        val completePendingIntent = PendingIntent.getActivity(
            context,
            stableRequestCode(taskId, 1),
            completeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val skipIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.evertask.app.ACTION_SKIP"
            component = ComponentName(context, MainActivity::class.java)
            putExtra("taskId", taskId)
            putExtra("subtaskId", subtaskId)
            putExtra("action", "skip")
            putExtra("evertask_internal_action", true)
            putExtra("evertask_internal_token", token)
        }
        val skipPendingIntent = PendingIntent.getActivity(
            context,
            stableRequestCode(taskId, 2),
            skipIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val snoozeIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.evertask.app.ACTION_SNOOZE"
            component = ComponentName(context, MainActivity::class.java)
            putExtra("taskId", taskId)
            putExtra("subtaskId", subtaskId)
            putExtra("action", "snooze")
            putExtra("evertask_internal_action", true)
            putExtra("evertask_internal_token", token)
        }
        val snoozePendingIntent = PendingIntent.getActivity(
            context,
            stableRequestCode(taskId, 3),
            snoozeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_save, "Complete", completePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Skip", skipPendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Snooze", snoozePendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(stableRequestCode(taskId, 4), notification)
        } catch (_: SecurityException) {
            // Notification permission revoked after channel creation
        }
    }
}
