package com.evertask.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TextToSpeechManager - Voice feedback for Ever Task Tools
 * 
 * Provides spoken confirmations for voice commands:
 * - Task creation: "Created garage cleaning checklist, 5 steps"
 * - Task completion: "Completed clear workspace. 4 steps remaining."
 * - Task finished: "Task complete! Great job!"
 * - Error messages: "Sorry, I couldn't create that task"
 * 
 * Features:
 * - Proper TTS engine lifecycle management
 * - Audio focus handling for voice interactions
 * - Queue management for multiple utterances
 * - Configurable speech rate and pitch
 * 
 * Usage:
 * ```kotlin
 * val ttsManager = TextToSpeechManager(context)
 * ttsManager.initialize()
 * ttsManager.speak("Task created successfully")
 * ttsManager.shutdown() // Call when done
 * ```
 */
@Singleton
class TextToSpeechManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "TextToSpeechManager"
        
        // Default TTS settings
        const val DEFAULT_SPEECH_RATE = 1.0f
        const val DEFAULT_PITCH = 1.0f
        
        // Timeout for TTS operations
        const val INIT_TIMEOUT_MS = 5000L
        const val SPEAK_TIMEOUT_MS = 10000L
        
        // Utterance IDs
        const val UTTERANCE_ID_PREFIX = "evertask_tts_"
    }
    
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var initDeferred: CompletableDeferred<Boolean>? = null
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    
    // Queue for pending speech requests
    private val speechQueue = mutableListOf<SpeechRequest>()
    
    data class SpeechRequest(
        val text: String,
        val queueMode: Int = TextToSpeech.QUEUE_FLUSH,
        val params: Bundle? = null,
        val utteranceId: String = UTTERANCE_ID_PREFIX + UUID.randomUUID().toString()
    )
    
    /**
     * Initialize the TTS engine
     * Must be called before speaking
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.Main) {
        if (isInitialized) {
            return@withContext true
        }
        
        // Check if TTS is available
        if (!isTTSAvailable()) {
            Log.w(TAG, "Text-to-Speech not available on this device")
            return@withContext false
        }
        
        initDeferred = CompletableDeferred()
        
        tts = TextToSpeech(context) { status ->
            when (status) {
                TextToSpeech.SUCCESS -> {
                    Log.d(TAG, "TTS initialized successfully")
                    configureTTS()
                    isInitialized = true
                    initDeferred?.complete(true)
                }
                else -> {
                    Log.e(TAG, "TTS initialization failed with status: $status")
                    isInitialized = false
                    initDeferred?.complete(false)
                }
            }
        }
        
        // Wait for initialization with timeout
        val result = withTimeoutOrNull(INIT_TIMEOUT_MS) {
            initDeferred?.await()
        } ?: false
        
        initDeferred = null
        result
    }
    
    /**
     * Configure TTS settings after initialization
     */
    private fun configureTTS() {
        tts?.apply {
            // Set language
            val result = setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Default language not supported, using US English")
                setLanguage(Locale.US)
            }
            
            // Set speech rate and pitch
            setSpeechRate(DEFAULT_SPEECH_RATE)
            setPitch(DEFAULT_PITCH)
            
            // Set audio attributes for proper audio routing
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                setAudioAttributes(audioAttributes)
            }
            
            // Set utterance progress listener
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "TTS started: $utteranceId")
                    requestAudioFocus()
                }
                
                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "TTS completed: $utteranceId")
                    abandonAudioFocus()
                    processSpeechQueue()
                }
                
                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "TTS error: $utteranceId")
                    abandonAudioFocus()
                    processSpeechQueue()
                }
                
                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.e(TAG, "TTS error: $utteranceId, code: $errorCode")
                    abandonAudioFocus()
                    processSpeechQueue()
                }
            })
        }
    }
    
    /**
     * Speak text immediately
     * 
     * @param text The text to speak
     * @param queueMode QUEUE_FLUSH (default) or QUEUE_ADD
     * @return true if speech was initiated successfully
     */
    suspend fun speak(
        text: String,
        queueMode: Int = TextToSpeech.QUEUE_FLUSH
    ): Boolean {
        // Initialize if needed
        if (!isInitialized) {
            val initialized = initialize()
            if (!initialized) {
                Log.w(TAG, "Cannot speak, TTS not initialized")
                return false
            }
        }
        
        return withContext(Dispatchers.Main) {
            val request = SpeechRequest(text = text, queueMode = queueMode)
            
            if (queueMode == TextToSpeech.QUEUE_FLUSH) {
                // Clear queue and speak immediately
                speechQueue.clear()
                speakInternal(request)
            } else {
                // Add to queue
                speechQueue.add(request)
                if (speechQueue.size == 1) {
                    speakInternal(request)
                }
            }
        }
    }
    
    /**
     * Speak text with custom parameters
     */
    suspend fun speakAdvanced(
        text: String,
        queueMode: Int = TextToSpeech.QUEUE_FLUSH,
        speechRate: Float? = null,
        pitch: Float? = null,
        volume: Float? = null
    ): Boolean {
        if (!isInitialized) {
            val initialized = initialize()
            if (!initialized) return false
        }
        
        return withContext(Dispatchers.Main) {
            val params = Bundle().apply {
                speechRate?.let { putFloat(TextToSpeech.Engine.KEY_PARAM_RATE, it) }
                pitch?.let { putFloat(TextToSpeech.Engine.KEY_PARAM_PITCH, it) }
                volume?.let { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, it) }
            }
            
            val request = SpeechRequest(
                text = text,
                queueMode = queueMode,
                params = params
            )
            
            speakInternal(request)
        }
    }
    
    /**
     * Internal speak implementation
     */
    private fun speakInternal(request: SpeechRequest): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.speak(request.text, request.queueMode, request.params, request.utteranceId)
        } else {
            @Suppress("DEPRECATION")
            val params = HashMap<String, String>().apply {
                put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, request.utteranceId)
            }
            @Suppress("DEPRECATION")
            tts?.speak(request.text, request.queueMode, params)
        }
        
        return result == TextToSpeech.SUCCESS
    }
    
    /**
     * Process next item in speech queue
     */
    private fun processSpeechQueue() {
        speechQueue.removeFirstOrNull()
        speechQueue.firstOrNull()?.let { nextRequest ->
            speakInternal(nextRequest)
        }
    }
    
    /**
     * Stop all speech
     */
    fun stop() {
        tts?.stop()
        speechQueue.clear()
        abandonAudioFocus()
    }
    
    /**
     * Check if TTS is currently speaking
     */
    fun isSpeaking(): Boolean {
        return tts?.isSpeaking == true
    }
    
    /**
     * Shutdown TTS engine
     * Call this when the app is closing or TTS is no longer needed
     */
    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        Log.d(TAG, "TTS shutdown complete")
    }
    
    /**
     * Request audio focus for speech
     */
    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { }
                .build()
            
            audioFocusRequest?.let {
                audioManager.requestAudioFocus(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }
    
    /**
     * Abandon audio focus
     */
    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }
    
    /**
     * Check if TTS is available on this device
     */
    private fun isTTSAvailable(): Boolean {
        // Check if TTS engine is installed
        val intent = android.content.Intent()
        intent.action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
        val resolveInfo = context.packageManager.queryIntentActivities(intent, 0)
        return resolveInfo.isNotEmpty()
    }
    
    /**
     * Get list of available TTS languages
     */
    fun getAvailableLanguages(): Set<Locale>? {
        return tts?.availableLanguages
    }
    
    /**
     * Set TTS language
     */
    fun setLanguage(locale: Locale): Int {
        return tts?.setLanguage(locale) ?: TextToSpeech.ERROR
    }
    
    /**
     * Predefined voice feedback messages
     */
    object VoiceMessages {
        fun taskCreated(taskName: String, stepCount: Int): String {
            return if (stepCount > 0) {
                "Created $taskName checklist, $stepCount steps"
            } else {
                "Created task: $taskName"
            }
        }
        
        fun subtaskCompleted(subtaskName: String, remainingCount: Int): String {
            return if (remainingCount > 0) {
                "Completed $subtaskName. $remainingCount steps remaining."
            } else {
                "Completed $subtaskName. Task finished!"
            }
        }
        
        fun taskCompleted(taskName: String): String {
            return "Task $taskName complete! Great job!"
        }
        
        fun noTasks(): String {
            return "You have no tasks. Say 'add a task' to create one."
        }
        
        fun taskListSummary(count: Int, firstTaskName: String?): String {
            return when {
                count == 0 -> noTasks()
                count == 1 -> "You have one task: $firstTaskName"
                else -> "You have $count tasks pending. Your first task is: $firstTaskName"
            }
        }
        
        fun errorGeneric(): String = "Sorry, something went wrong."
        fun errorTaskNotFound(taskName: String): String = "I couldn't find a task matching $taskName"
        fun errorCreatingTask(): String = "Sorry, I couldn't create that task"
        fun errorReadingTasks(): String = "Sorry, I couldn't read your tasks"
        fun errorCompletingTask(): String = "Sorry, I couldn't complete that item"
        fun errorDeletingTask(): String = "Sorry, I couldn't delete that task"
    }
}

/**
 * Extension function for easy TTS usage in ViewModels/Activities
 */
suspend fun TextToSpeechManager.speakTaskCreated(taskName: String, stepCount: Int) {
    speak(TextToSpeechManager.VoiceMessages.taskCreated(taskName, stepCount))
}

suspend fun TextToSpeechManager.speakSubtaskCompleted(subtaskName: String, remainingCount: Int) {
    speak(TextToSpeechManager.VoiceMessages.subtaskCompleted(subtaskName, remainingCount))
}

suspend fun TextToSpeechManager.speakTaskCompleted(taskName: String) {
    speak(TextToSpeechManager.VoiceMessages.taskCompleted(taskName))
}

suspend fun TextToSpeechManager.speakError(message: String? = null) {
    speak(message ?: TextToSpeechManager.VoiceMessages.errorGeneric())
}
