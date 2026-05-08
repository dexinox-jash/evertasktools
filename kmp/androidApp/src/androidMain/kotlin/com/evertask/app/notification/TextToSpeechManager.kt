package com.evertask.app.notification

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TextToSpeechManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.getDefault())
            isInitialized = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!isInitialized) {
                Log.e("TextToSpeechManager", "Language not supported")
            }
        } else {
            Log.e("TextToSpeechManager", "TTS initialization failed with status: $status")
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Log.w("TextToSpeechManager", "TTS not initialized yet, cannot speak")
        }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
