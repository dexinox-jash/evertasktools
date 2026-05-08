package com.evertask.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.evertask.app.notification.TaskNotification
import com.evertask.shared.data.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("taskId") ?: return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repository = GlobalContext.get().get<TaskRepository>()
                val task = repository.getTaskById(taskId)
                val title = task?.title ?: "Task Reminder"
                val content = task?.subtasks?.firstOrNull { !it.isCompleted }?.text ?: "You have a pending task"

                TaskNotification.showTaskNotification(context, taskId, title, content)
            } catch (e: Exception) {
                Log.e("TaskReminderReceiver", "Failed to show reminder for task $taskId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
