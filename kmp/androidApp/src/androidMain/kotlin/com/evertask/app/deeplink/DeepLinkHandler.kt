package com.evertask.app.deeplink

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.evertask.app.notification.TextToSpeechManager
import com.evertask.app.scheduler.AndroidReminderScheduler
import com.evertask.app.widget.WidgetUpdater
import com.evertask.shared.data.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.getKoin

object DeepLinkHandler {
    internal var ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO

    private fun sanitizeInput(input: String?): String? {
        return input?.trim()?.takeIf { it.isNotEmpty() && it.length <= 200 }
    }

    private fun launchIo(
        context: Context,
        errorMessage: String = "Action failed",
        block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit
    ) {
        kotlinx.coroutines.CoroutineScope(ioDispatcher).launch {
            try {
                block()
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun handleDeepLink(context: Context, intent: Intent) {
        val data: Uri? = intent.data
        val action = intent.action

        // Handle notification actions
        when (action) {
            "com.evertask.app.ACTION_COMPLETE" -> {
                val taskId = intent.getStringExtra("taskId") ?: return
                val subtaskId = intent.getStringExtra("subtaskId")
                val repository: TaskRepository = getKoin().get()
                val widgetUpdater: WidgetUpdater = getKoin().get()
                launchIo(context, "Failed to complete task") {
                    if (subtaskId != null) {
                        repository.completeSubtask(taskId, subtaskId)
                    } else {
                        repository.archiveTask(taskId)
                    }
                    widgetUpdater.updateAllWidgets()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Task completed", Toast.LENGTH_SHORT).show()
                    }
                }
                return
            }
            "com.evertask.app.ACTION_SKIP" -> {
                val taskId = intent.getStringExtra("taskId") ?: return
                val repository: TaskRepository = getKoin().get()
                val widgetUpdater: WidgetUpdater = getKoin().get()
                launchIo(context, "Failed to skip task") {
                    repository.archiveTask(taskId)
                    widgetUpdater.updateAllWidgets()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Task skipped", Toast.LENGTH_SHORT).show()
                    }
                }
                return
            }
            "com.evertask.app.ACTION_SNOOZE" -> {
                val taskId = intent.getStringExtra("taskId") ?: return
                AndroidReminderScheduler(context).snoozeReminder(taskId)
                Toast.makeText(context, "Snoozed for 15 minutes", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Handle URI deep links
        if (data == null || data.scheme != "evertask") return

        val repository: TaskRepository = getKoin().get()

        when (data.host) {
            "create" -> {
                val title = sanitizeInput(data.getQueryParameter("title")) ?: return
                val widgetUpdater: WidgetUpdater = getKoin().get()
                launchIo(context, "Failed to create task") {
                    repository.createTask(title)
                    widgetUpdater.updateAllWidgets()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Task created", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            "read" -> {
                launchIo(context) {
                    val tasks = repository.getActiveTasks().first()
                    val message = if (tasks.isEmpty()) {
                        "You have no active tasks."
                    } else {
                        val taskList = tasks.joinToString(", ") { "${it.title} with ${it.subtasks.count { s -> !s.isCompleted }} steps remaining" }
                        "You have ${tasks.size} active tasks: $taskList"
                    }
                    withContext(Dispatchers.Main) {
                        try {
                            val tts = TextToSpeechManager(context)
                            tts.speak(message)
                            tts.shutdown()
                        } catch (_: Exception) {
                            // TTS unavailable in test or on device
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }
            }
            "complete" -> {
                val item = sanitizeInput(data.getQueryParameter("item"))
                val widgetUpdater: WidgetUpdater = getKoin().get()
                if (item != null) {
                    launchIo(context, "Failed to complete task") {
                        val tasks = repository.getActiveTasks().first()
                        val matchedSubtask = tasks.flatMap { task ->
                            task.subtasks.map { task to it }
                        }.find { (_, subtask) ->
                            subtask.text.contains(item, ignoreCase = true)
                        }
                        if (matchedSubtask != null) {
                            val (task, subtask) = matchedSubtask
                            repository.completeSubtask(task.id, subtask.id)
                            widgetUpdater.updateAllWidgets()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Completed ${subtask.text}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val matchedTask = tasks.find { it.title.contains(item, ignoreCase = true) }
                            if (matchedTask != null) {
                                repository.archiveTask(matchedTask.id)
                                widgetUpdater.updateAllWidgets()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Completed ${matchedTask.title}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                } else {
                    val taskId = data.getQueryParameter("taskId") ?: return
                    val subtaskId = data.getQueryParameter("subtaskId")
                    launchIo(context, "Failed to complete task") {
                        if (subtaskId != null) {
                            repository.completeSubtask(taskId, subtaskId)
                        } else {
                            repository.archiveTask(taskId)
                        }
                        widgetUpdater.updateAllWidgets()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Task completed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            "delete" -> {
                val item = sanitizeInput(data.getQueryParameter("item"))
                val widgetUpdater: WidgetUpdater = getKoin().get()
                if (item != null) {
                    launchIo(context, "Failed to delete task") {
                        val tasks = repository.getActiveTasks().first()
                        val matchedTask = tasks.find { it.title.contains(item, ignoreCase = true) }
                        if (matchedTask != null) {
                            repository.deleteTask(matchedTask.id)
                            widgetUpdater.updateAllWidgets()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Deleted ${matchedTask.title}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    val taskId = data.getQueryParameter("taskId") ?: return
                    launchIo(context, "Failed to delete task") {
                        repository.deleteTask(taskId)
                        widgetUpdater.updateAllWidgets()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Task deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            "edit" -> {
                val oldTitle = sanitizeInput(data.getQueryParameter("oldTitle"))
                val newTitle = sanitizeInput(data.getQueryParameter("newTitle"))
                if (oldTitle != null && newTitle != null) {
                    val widgetUpdater: WidgetUpdater = getKoin().get()
                    launchIo(context, "Failed to edit task") {
                        val tasks = repository.getActiveTasks().first()
                        val matchedTask = tasks.find { it.title.contains(oldTitle, ignoreCase = true) }
                        if (matchedTask != null) {
                            repository.updateTaskTitle(matchedTask.id, newTitle)
                            widgetUpdater.updateAllWidgets()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Renamed to $newTitle", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            "open" -> {
                // Just open the app - MainActivity is already in foreground
                Toast.makeText(context, "EverTask is open", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
