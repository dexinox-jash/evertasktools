package com.evertasktools.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.evertask.data.repository.TaskRepository
import com.evertasktools.notification.TaskNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val taskId = intent?.getStringExtra("task_id") ?: return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val repository = TaskRepository.getInstance(context)
                val task = repository.getTaskById(taskId)
                task?.let {
                    val notificationManager = TaskNotificationManager(context)
                    notificationManager.showTaskNotification(it)
                }
            } catch (e: Exception) {
                android.util.Log.e("TaskReminderReceiver", "Error showing reminder", e)
            }
        }
    }
}
