package com.evertasktools.deeplink

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.evertask.data.repository.TaskRepository
import com.evertasktools.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * DeepLinkHandler - Processes evertask:// deep links
 * 
 * This class handles all deep link routing for the app, including:
 * - Voice command responses from Google Assistant
 * - Widget interactions
 * - External app integrations
 * 
 * URI Schemes:
 * - evertask://create?title=xyz - Create new task
 * - evertask://create - Create task without title (opens UI)
 * - evertask://read - Show all tasks
 * - evertask://read?id=123 - Show specific task
 * - evertask://complete?item=xyz - Complete subtask by name
 * - evertask://complete?id=123&subtask=456 - Complete subtask by ID
 * - evertask://delete?item=xyz - Delete task by name
 * - evertask://delete?id=123 - Delete task by ID
 * - evertask://widget?action=check&id=123&subtask=456 - Widget checkbox action
 * - evertask://widget?action=newtask - Widget new task action
 * 
 * Usage:
 * ```kotlin
 * val handler = DeepLinkHandler(context)
 * handler.processUri(uri)
 * ```
 */
class DeepLinkHandler(private val context: Context) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    companion object {
        private const val TAG = "DeepLinkHandler"
        
        // URI Components
        const val SCHEME = "evertask"
        
        // Hosts
        const val HOST_CREATE = "create"
        const val HOST_READ = "read"
        const val HOST_COMPLETE = "complete"
        const val HOST_DELETE = "delete"
        const val HOST_WIDGET = "widget"
        
        // Query Parameters
        const val PARAM_TITLE = "title"
        const val PARAM_ID = "id"
        const val PARAM_ITEM = "item"
        const val PARAM_SUBTASK = "subtask"
        const val PARAM_ACTION = "action"
        
        // Widget Actions
        const val WIDGET_ACTION_CHECK = "check"
        const val WIDGET_ACTION_NEW_TASK = "newtask"
        const val WIDGET_ACTION_REFRESH = "refresh"
        
        // Intent Extras
        const val EXTRA_DEEP_LINK_URI = "deep_link_uri"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
        const val EXTRA_SUBTASK_ID = "subtask_id"
        const val EXTRA_WIDGET_ACTION = "widget_action"
    }
    
    /**
     * Process a deep link URI and route to appropriate action
     * 
     * @param uri The deep link URI to process
     * @param fromVoice Whether this originated from a voice command
     * @return Intent to launch the appropriate activity, or null if invalid
     */
    fun processUri(uri: Uri, fromVoice: Boolean = false): Intent? {
        Log.d(TAG, "Processing URI: $uri, fromVoice: $fromVoice")
        // Validate scheme
        if (uri.scheme != SCHEME) {
            Log.w(TAG, "Invalid scheme: ${uri.scheme}")
            return null
        }
        
        return when (uri.host) {
            HOST_CREATE -> handleCreate(uri, fromVoice)
            HOST_READ -> handleRead(uri)
            HOST_COMPLETE -> handleComplete(uri)
            HOST_DELETE -> handleDelete(uri)
            HOST_WIDGET -> handleWidget(uri)
            else -> {
                Log.w(TAG, "Unknown host: ${uri.host}")
                createMainIntent()
            }
        }
    }
    
    /**
     * Handle evertask://create?title=xyz
     * Creates a new task and opens it in the UI
     */
    private fun handleCreate(uri: Uri, fromVoice: Boolean): Intent? {
        val title = uri.getQueryParameter(PARAM_TITLE)
        
        return if (title != null) {
            // Create task immediately
            scope.launch(Dispatchers.IO) {
                try {
                    val repository = TaskRepository.getInstance(context)
                    val result = repository.createTask(title)
                    val task = result.getOrNull()
                    
                    task?.let {
                        withContext(Dispatchers.Main) {
                            // Launch task detail
                            launchTaskDetail(it.id, title, fromVoice)
                        }
                    } ?: run {
                        Log.e(TAG, "Failed to create task")
                        launchMainActivity()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating task from deep link", e)
                    launchMainActivity()
                }
            }
            null // Async handling
        } else {
            // No title - open create UI
            createMainIntent().apply {
                putExtra(EXTRA_WIDGET_ACTION, HOST_CREATE)
            }
        }
    }
    
    /**
     * Handle evertask://read or evertask://read?id=123
     * Opens main activity showing tasks
     */
    private fun handleRead(uri: Uri): Intent {
        val taskId = uri.getQueryParameter(PARAM_ID)
        
        return if (taskId != null) {
            // Show specific task
            createMainIntent().apply {
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_WIDGET_ACTION, HOST_READ)
            }
        } else {
            // Show all tasks
            createMainIntent()
        }
    }
    
    /**
     * Handle evertask://complete?item=xyz or evertask://complete?id=123&subtask=456
     * Completes a subtask and returns to main activity
     */
    private fun handleComplete(uri: Uri): Intent? {
        val itemName = uri.getQueryParameter(PARAM_ITEM)
        val taskId = uri.getQueryParameter(PARAM_ID)
        val subtaskId = uri.getQueryParameter(PARAM_SUBTASK)
        
        return when {
            // Complete by IDs (most reliable)
            taskId != null && subtaskId != null -> {
                scope.launch(Dispatchers.IO) {
                    try {
                        val repository = TaskRepository.getInstance(context)
                        repository.completeSubtask(taskId, subtaskId)
                        withContext(Dispatchers.Main) {
                            launchMainActivity()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error completing subtask from deep link", e)
                        launchMainActivity()
                    }
                }
                null // Async handling
            }
            // Complete by name (voice commands)
            itemName != null -> {
                scope.launch(Dispatchers.IO) {
                    completeByName(itemName)
                    withContext(Dispatchers.Main) {
                        launchMainActivity()
                    }
                }
                null // Async handling
            }
            else -> {
                Log.w(TAG, "Invalid complete parameters")
                createMainIntent()
            }
        }
    }
    
    /**
     * Handle evertask://delete?item=xyz or evertask://delete?id=123
     * Deletes a task
     */
    private fun handleDelete(uri: Uri): Intent? {
        val itemName = uri.getQueryParameter(PARAM_ITEM)
        val taskId = uri.getQueryParameter(PARAM_ID)
        
        return when {
            taskId != null -> {
                scope.launch(Dispatchers.IO) {
                    try {
                        val repository = TaskRepository.getInstance(context)
                        repository.deleteTaskPermanently(taskId)
                        withContext(Dispatchers.Main) {
                            launchMainActivity()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting task from deep link", e)
                        launchMainActivity()
                    }
                }
                null
            }
            itemName != null -> {
                scope.launch(Dispatchers.IO) {
                    deleteByName(itemName)
                    withContext(Dispatchers.Main) {
                        launchMainActivity()
                    }
                }
                null
            }
            else -> createMainIntent()
        }
    }
    
    /**
     * Handle evertask://widget?action=xyz
     * Processes widget interactions
     */
    private fun handleWidget(uri: Uri): Intent? {
        val action = uri.getQueryParameter(PARAM_ACTION) ?: return createMainIntent()
        
        return when (action) {
            WIDGET_ACTION_CHECK -> {
                val taskId = uri.getQueryParameter(PARAM_ID)
                val subtaskId = uri.getQueryParameter(PARAM_SUBTASK)
                
                if (taskId != null && subtaskId != null) {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val repository = TaskRepository.getInstance(context)
                            val task = repository.getTaskById(taskId)
                            val subtask = task?.getSubtasks()?.find { it.id == subtaskId }
                            
                            if (subtask != null) {
                                // Toggle completion
                                repository.completeSubtask(taskId, subtaskId)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error handling widget check action", e)
                        }
                    }
                }
                null // No UI needed for widget actions
            }
            WIDGET_ACTION_NEW_TASK -> {
                createMainIntent().apply {
                    putExtra(EXTRA_WIDGET_ACTION, WIDGET_ACTION_NEW_TASK)
                }
            }
            WIDGET_ACTION_REFRESH -> {
                // Just trigger widget update
                null
            }
            else -> createMainIntent()
        }
    }
    
    /**
     * Complete a subtask by searching for matching name
     */
    private suspend fun completeByName(itemName: String) {
        val repository = TaskRepository.getInstance(context)
        val tasks = repository.getActiveTasks().first()
        
        for (task in tasks) {
            val subtask = task.getSubtasks().find { 
                it.text.contains(itemName, ignoreCase = true) 
            }
            if (subtask != null) {
                repository.completeSubtask(task.id, subtask.id)
                return
            }
        }
        
        // If no subtask found, try matching task title
        val task = tasks.find { it.title.contains(itemName, ignoreCase = true) }
        task?.let {
            repository.archiveTask(it.id)
        }
    }
    
    /**
     * Delete a task by searching for matching name
     */
    private suspend fun deleteByName(itemName: String) {
        val repository = TaskRepository.getInstance(context)
        val tasks = repository.getActiveTasks().first()
        val task = tasks.find { it.title.contains(itemName, ignoreCase = true) }
        task?.let { repository.deleteTaskPermanently(it.id) }
    }
    
    /**
     * Launch main activity
     */
    private fun launchMainActivity() {
        val intent = createMainIntent()
        ContextCompat.startActivity(context, intent, null)
    }
    
    /**
     * Launch task detail view
     */
    private fun launchTaskDetail(taskId: String, title: String, fromVoice: Boolean) {
        val intent = createMainIntent().apply {
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra("from_voice", fromVoice)
        }
        ContextCompat.startActivity(context, intent, null)
    }
    
    /**
     * Create base main activity intent
     */
    private fun createMainIntent(): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEEP_LINK_URI, true)
        }
    }
    
    /**
     * Build a create task deep link URI
     */
    fun buildCreateUri(title: String? = null): Uri {
        return Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST_CREATE)
            .apply {
                title?.let { appendQueryParameter(PARAM_TITLE, it) }
            }
            .build()
    }
    
    /**
     * Build a read tasks deep link URI
     */
    fun buildReadUri(taskId: String? = null): Uri {
        return Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST_READ)
            .apply {
                taskId?.let { appendQueryParameter(PARAM_ID, it) }
            }
            .build()
    }
    
    /**
     * Build a complete subtask deep link URI
     */
    fun buildCompleteUri(taskId: String, subtaskId: String): Uri {
        return Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST_COMPLETE)
            .appendQueryParameter(PARAM_ID, taskId)
            .appendQueryParameter(PARAM_SUBTASK, subtaskId)
            .build()
    }
    
    /**
     * Build a widget action deep link URI
     */
    fun buildWidgetUri(action: String, taskId: String? = null, subtaskId: String? = null): Uri {
        return Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST_WIDGET)
            .appendQueryParameter(PARAM_ACTION, action)
            .apply {
                taskId?.let { appendQueryParameter(PARAM_ID, it) }
                subtaskId?.let { appendQueryParameter(PARAM_SUBTASK, it) }
            }
            .build()
    }
}
