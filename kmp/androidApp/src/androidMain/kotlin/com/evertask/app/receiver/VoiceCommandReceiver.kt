package com.evertask.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import com.evertask.app.notification.TextToSpeechManager
import com.evertask.app.widget.WidgetUpdater
import com.evertask.shared.data.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext

class VoiceCommandReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val data: Uri? = intent.data

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repository = GlobalContext.get().get<TaskRepository>()
                val widgetUpdater = GlobalContext.get().get<WidgetUpdater>()
                when (action) {
                    ACTION_CREATE_TASK -> handleCreateTask(context, repository, widgetUpdater, data)
                    ACTION_READ_TASKS -> handleReadTasks(context, repository, data)
                    ACTION_COMPLETE_ITEM -> handleCompleteItem(context, repository, widgetUpdater, data)
                    ACTION_DELETE_ITEM -> handleDeleteItem(context, repository, widgetUpdater, data)
                    ACTION_EDIT_TASK -> handleEditTask(context, repository, widgetUpdater, data)
                    ACTION_REORDER_SUBTASKS -> handleReorderSubtasks(context, repository, widgetUpdater, data)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleCreateTask(context: Context, repository: TaskRepository, widgetUpdater: WidgetUpdater, data: Uri?) {
        val title = data?.getQueryParameter("title")?.trim().takeIf { it?.isNotEmpty() == true } ?: "New Task"
        val task = repository.createTask(title)
        widgetUpdater.updateAllWidgets()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Created task: ${task.title}", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun handleReadTasks(context: Context, repository: TaskRepository, data: Uri?) {
        val tasks = repository.getActiveTasks().first()
        val speak = data?.getQueryParameter("speak")?.toBoolean() ?: false
        val message = if (tasks.isEmpty()) {
            "You have no active tasks."
        } else {
            val taskList = tasks.joinToString(", ") { "${it.title} with ${it.subtasks.count { s -> !s.isCompleted }} steps remaining" }
            "You have ${tasks.size} active tasks: $taskList"
        }
        withContext(Dispatchers.Main) {
            if (speak) {
                val tts = TextToSpeechManager(context)
                try {
                    tts.speak(message)
                } finally {
                    tts.shutdown()
                }
            } else {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun handleCompleteItem(context: Context, repository: TaskRepository, widgetUpdater: WidgetUpdater, data: Uri?) {
        val itemName = data?.getQueryParameter("item")?.trim()
        if (itemName == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Please specify what to complete", Toast.LENGTH_SHORT).show()
            }
            return
        }
        val tasks = repository.getActiveTasks().first()
        val matchedSubtask = tasks.flatMap { task ->
            task.subtasks.map { task to it }
        }.find { (_, subtask) ->
            subtask.text.contains(itemName, ignoreCase = true)
        }

        if (matchedSubtask != null) {
            val (task, subtask) = matchedSubtask
            repository.completeSubtask(task.id, subtask.id)
            widgetUpdater.updateAllWidgets()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Completed ${subtask.text}", Toast.LENGTH_SHORT).show()
            }
        } else {
            val matchedTask = tasks.find { it.title.contains(itemName, ignoreCase = true) }
            if (matchedTask != null) {
                repository.archiveTask(matchedTask.id)
                widgetUpdater.updateAllWidgets()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Completed ${matchedTask.title}", Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Item not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun handleDeleteItem(context: Context, repository: TaskRepository, widgetUpdater: WidgetUpdater, data: Uri?) {
        val itemName = data?.getQueryParameter("item")?.trim()
        if (itemName == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Please specify what to delete", Toast.LENGTH_SHORT).show()
            }
            return
        }
        val tasks = repository.getActiveTasks().first()
        val matchedTask = tasks.find { it.title.contains(itemName, ignoreCase = true) }
        if (matchedTask != null) {
            repository.deleteTask(matchedTask.id)
            widgetUpdater.updateAllWidgets()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Deleted ${matchedTask.title}", Toast.LENGTH_SHORT).show()
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Item not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun handleEditTask(context: Context, repository: TaskRepository, widgetUpdater: WidgetUpdater, data: Uri?) {
        val oldTitle = data?.getQueryParameter("oldTitle")?.trim()
        val newTitle = data?.getQueryParameter("newTitle")?.trim()
        if (oldTitle.isNullOrEmpty() || newTitle.isNullOrEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Please specify both current and new task names", Toast.LENGTH_SHORT).show()
            }
            return
        }
        val tasks = repository.getActiveTasks().first()
        val matchedTask = tasks.find { it.title.contains(oldTitle, ignoreCase = true) }
        if (matchedTask != null) {
            repository.updateTaskTitle(matchedTask.id, newTitle)
            widgetUpdater.updateAllWidgets()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Renamed to $newTitle", Toast.LENGTH_SHORT).show()
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Task not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun handleReorderSubtasks(context: Context, repository: TaskRepository, widgetUpdater: WidgetUpdater, data: Uri?) {
        val title = data?.getQueryParameter("title")?.trim()
        if (title.isNullOrEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Please specify which task to reorder", Toast.LENGTH_SHORT).show()
            }
            return
        }
        val tasks = repository.getActiveTasks().first()
        val matchedTask = tasks.find { it.title.contains(title, ignoreCase = true) }
        if (matchedTask != null) {
            val reversedIds = matchedTask.subtasks.reversed().map { it.id }
            repository.reorderSubtasks(matchedTask.id, reversedIds)
            widgetUpdater.updateAllWidgets()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Reordered ${matchedTask.title}", Toast.LENGTH_SHORT).show()
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Task not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val ACTION_CREATE_TASK = "com.evertask.app.CREATE_TASK"
        const val ACTION_READ_TASKS = "com.evertask.app.READ_TASKS"
        const val ACTION_COMPLETE_ITEM = "com.evertask.app.COMPLETE_ITEM"
        const val ACTION_DELETE_ITEM = "com.evertask.app.DELETE_ITEM"
        const val ACTION_EDIT_TASK = "com.evertask.app.EDIT_TASK"
        const val ACTION_REORDER_SUBTASKS = "com.evertask.app.REORDER_SUBTASKS"
    }
}
