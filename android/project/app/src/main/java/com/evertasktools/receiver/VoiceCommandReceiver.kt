package com.evertasktools.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import com.evertask.data.repository.TaskRepository
import com.evertasktools.BuildConfig
import com.evertasktools.MainActivity
import com.evertasktools.notification.TaskNotificationManager
import com.evertasktools.notification.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * VoiceCommandReceiver - BroadcastReceiver for handling voice commands
 * 
 * Security: Verifies caller package to prevent malicious intents
 * Supported Actions:
 * - evertask://create?title=xyz - Create new task
 * - evertask://read - Read/display tasks
 * - evertask://complete?item=xyz - Complete a subtask
 * - evertask://delete?item=xyz - Delete a task
 * 
 * Voice Flow:
 * 1. Google Assistant sends Intent.ACTION_VIEW with evertask:// URI
 * 2. Receiver validates caller (com.google.android.googlequicksearchbox)
 * 3. Parses action and parameters
 * 4. Executes command and provides TTS feedback
 */
class VoiceCommandReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    companion object {
        private const val TAG = "VoiceCommandReceiver"
        
        // Authorized caller packages for security
        private val ALLOWED_PACKAGES = setOf(
            "com.google.android.googlequicksearchbox", // Google Assistant
            "com.google.android.apps.googleassistant", // Google Assistant (newer)
            "com.android.shell",                       // For testing via adb
            "com.evertask.debug"                        // Debug builds
        )
        
        // Deep link schemes and hosts
        private const val SCHEME = "evertask"
        private const val HOST_CREATE = "create"
        private const val HOST_READ = "read"
        private const val HOST_COMPLETE = "complete"
        private const val HOST_DELETE = "delete"
        
        // Intent extras
        const val EXTRA_VOICE_COMMAND = "voice_command"
        const val EXTRA_TASK_TITLE = "task_title"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Voice intent received: ${intent.data}")
        // Security: Verify caller package
        if (!isAuthorizedCaller(context, intent)) {
            Log.w(TAG, "Unauthorized caller detected, ignoring command")
            return
        }
        
        val uri = intent.data ?: run {
            Log.w(TAG, "No URI in intent")
            speakError(context, "Sorry, I didn't understand that command")
            return
        }
        
        // Verify scheme
        if (uri.scheme != SCHEME) {
            Log.w(TAG, "Invalid scheme: ${uri.scheme}")
            return
        }
        
        // Process based on host/action
        when (uri.host) {
            HOST_CREATE -> handleCreateTask(context, uri)
            HOST_READ -> handleReadTasks(context)
            HOST_COMPLETE -> handleCompleteTask(context, uri)
            HOST_DELETE -> handleDeleteTask(context, uri)
            else -> {
                Log.w(TAG, "Unknown host: ${uri.host}")
                speakError(context, "Sorry, I don't know how to do that")
            }
        }
    }
    
    /**
     * Security verification - only allow calls from authorized packages
     */
    private fun isAuthorizedCaller(context: Context, intent: Intent): Boolean {
        // Check calling package
        val callingPackage = intent.`package` 
            ?: context.packageManager.getNameForUid(android.os.Binder.getCallingUid())
        Log.d(TAG, "Authorized caller: $callingPackage")
        
        // Allow if package is in whitelist or if we're in debug mode
        return callingPackage in ALLOWED_PACKAGES || BuildConfig.DEBUG
    }
    
    /**
     * Handle CREATE_TODO intent - create a new task
     * Voice: "Add clean garage to Ever Task"
     */
    private fun handleCreateTask(context: Context, uri: Uri) {
        val title = uri.getQueryParameter("title")?.trim() ?: "New Task"
        Log.d(TAG, "Creating task: $title")
        
        scope.launch(Dispatchers.IO) {
            try {
                val repository = TaskRepository.getInstance(context)
                val result = repository.createTask(title)
                val task = result.getOrNull()
                
                // Load the saved task with ID
                val savedTask = task?.let { repository.getTaskById(it.id) }
                
                withContext(Dispatchers.Main) {
                    val ttsManager = TextToSpeechManager(context)
                    val notificationManager = TaskNotificationManager(context)
                    
                    // Provide voice feedback
                    val subtaskCount = savedTask?.getSubtasks()?.size ?: 0
                    val feedback = if (subtaskCount > 0) {
                        "Created ${title} checklist, $subtaskCount steps"
                    } else {
                        "Created task: $title"
                    }
                    
                    ttsManager.speak(feedback)
                    
                    // Launch main activity with task
                    launchMainActivity(context, EXTRA_VOICE_COMMAND, Bundle().apply {
                        putString(EXTRA_TASK_TITLE, title)
                        task?.id?.let { putString("task_id", it) }
                    })
                    
                    // Show notification with first subtask
                    savedTask?.let { notificationManager.showTaskNotification(it) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating task", e)
                withContext(Dispatchers.Main) {
                    speakError(context, "Sorry, I couldn't create that task")
                }
            }
        }
    }
    
    /**
     * Handle GET_THING intent - read/display tasks
     * Voice: "Show my tasks in Ever Task"
     */
    private fun handleReadTasks(context: Context) {
        Log.d(TAG, "Reading tasks")
        scope.launch(Dispatchers.IO) {
            try {
                val repository = TaskRepository.getInstance(context)
                val tasks = repository.getActiveTasks().first()
                val incompleteTasks = tasks.filter { !it.isCompleted }
                
                withContext(Dispatchers.Main) {
                    val ttsManager = TextToSpeechManager(context)
                    
                    val feedback = when {
                        incompleteTasks.isEmpty() && tasks.isEmpty() -> 
                            "You have no tasks. Say 'add a task' to create one."
                        incompleteTasks.isEmpty() -> 
                            "All ${tasks.size} tasks are complete. Great job!"
                        incompleteTasks.size == 1 -> {
                            val task = incompleteTasks.first()
                            val remaining = task.getSubtasks().count { !it.isCompleted }
                            "You have one task: ${task.title}. " +
                            if (remaining > 0) "$remaining steps remaining." else ""
                        }
                        else -> 
                            "You have ${incompleteTasks.size} tasks pending. " +
                            "Your first task is: ${incompleteTasks.first().title}"
                    }
                    
                    ttsManager.speak(feedback)
                    
                    // Launch main activity
                    launchMainActivity(context, EXTRA_VOICE_COMMAND, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading tasks", e)
                withContext(Dispatchers.Main) {
                    speakError(context, "Sorry, I couldn't read your tasks")
                }
            }
        }
    }
    
    /**
     * Handle UPDATE_TODO intent - complete a subtask
     * Voice: "Complete garage cleaning in Ever Task"
     */
    private fun handleCompleteTask(context: Context, uri: Uri) {
        val itemName = uri.getQueryParameter("item")?.trim() ?: run {
            speakError(context, "Please specify what to complete")
            return
        }
        Log.d(TAG, "Completing item: $itemName")
        
        scope.launch(Dispatchers.IO) {
            try {
                val repository = TaskRepository.getInstance(context)
                // Find matching task or subtask
                val tasks = repository.getActiveTasks().first()
                
                // Try to find subtask first
                var matchedSubtask: Pair<com.evertask.data.entity.TaskEntity, com.evertask.data.model.Subtask>? = null
                var matchedTask: com.evertask.data.entity.TaskEntity? = null
                
                for (task in tasks) {
                    // Check if itemName matches a subtask
                    val subtask = task.getSubtasks().find { 
                        it.text.contains(itemName, ignoreCase = true) 
                    }
                    if (subtask != null) {
                        matchedSubtask = task to subtask
                        break
                    }
                    // Check if itemName matches task title
                    if (task.title.contains(itemName, ignoreCase = true)) {
                        matchedTask = task
                    }
                }
                
                withContext(Dispatchers.Main) {
                    val ttsManager = TextToSpeechManager(context)
                    
                    when {
                        matchedSubtask != null -> {
                            val (task, subtask) = matchedSubtask
                            // Complete the subtask
                            scope.launch(Dispatchers.IO) {
                                repository.completeSubtask(task.id, subtask.id)
                            }
                            
                            val remaining = task.getSubtasks().count { !it.isCompleted && it.id != subtask.id }
                            val feedback = if (remaining > 0) {
                                "Completed ${subtask.text}. $remaining steps remaining."
                            } else {
                                "Completed ${subtask.text}. Task finished!"
                            }
                            ttsManager.speak(feedback)
                        }
                        matchedTask != null -> {
                            // Complete entire task
                            scope.launch(Dispatchers.IO) {
                                repository.archiveTask(matchedTask.id)
                            }
                            ttsManager.speak("Completed ${matchedTask.title}. Great job!")
                        }
                        else -> {
                            ttsManager.speak("I couldn't find a task matching $itemName")
                        }
                    }
                    
                    // Launch main activity
                    launchMainActivity(context, EXTRA_VOICE_COMMAND, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error completing task", e)
                withContext(Dispatchers.Main) {
                    speakError(context, "Sorry, I couldn't complete that item")
                }
            }
        }
    }
    
    /**
     * Handle DELETE_TODO intent - delete a task
     * Voice: "Delete garage cleaning from Ever Task"
     */
    private fun handleDeleteTask(context: Context, uri: Uri) {
        val itemName = uri.getQueryParameter("item")?.trim() ?: run {
            speakError(context, "Please specify what to delete")
            return
        }
        Log.d(TAG, "Deleting item: $itemName")
        
        scope.launch(Dispatchers.IO) {
            try {
                val repository = TaskRepository.getInstance(context)
                val tasks = repository.getActiveTasks().first()
                val matchedTask = tasks.find { 
                    it.title.contains(itemName, ignoreCase = true) 
                }
                
                withContext(Dispatchers.Main) {
                    val ttsManager = TextToSpeechManager(context)
                    
                    if (matchedTask != null) {
                        scope.launch(Dispatchers.IO) {
                            repository.deleteTaskPermanently(matchedTask.id)
                        }
                        ttsManager.speak("Deleted ${matchedTask.title}")
                    } else {
                        ttsManager.speak("I couldn't find a task matching $itemName")
                    }
                    
                    launchMainActivity(context, EXTRA_VOICE_COMMAND, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting task", e)
                withContext(Dispatchers.Main) {
                    speakError(context, "Sorry, I couldn't delete that task")
                }
            }
        }
    }
    
    /**
     * Launch main activity with optional extras
     */
    private fun launchMainActivity(context: Context, action: String?, extras: Bundle?) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action?.let { this.action = it }
            extras?.let { putExtras(it) }
        }
        ContextCompat.startActivity(context, intent, null)
    }
    
    /**
     * Speak error message
     */
    private fun speakError(context: Context, message: String) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val ttsManager = TextToSpeechManager(context)
                ttsManager.speak(message)
            } catch (e: Exception) {
                Log.e(TAG, "Error speaking message", e)
            }
        }
    }
}
