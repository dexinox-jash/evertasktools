package com.evertask.app.notification

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class TextToSpeechManagerTest {

    private lateinit var context: Context
    private lateinit var tts: TextToSpeech

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        tts = mockk(relaxed = true)
        every { tts.setLanguage(any()) } returns TextToSpeech.LANG_COUNTRY_AVAILABLE
        every { tts.speak(any(), any(), any(), any()) } returns 0
        every { tts.stop() } returns 0
        every { tts.shutdown() } answers { }
    }

    @Test
    fun `initialization with SUCCESS enables speaking`() {
        val manager = TextToSpeechManager(context)
        injectTts(manager, tts)

        manager.onInit(TextToSpeech.SUCCESS)
        manager.speak("Hello world")

        verify { tts.speak("Hello world", TextToSpeech.QUEUE_FLUSH, null, null) }
    }

    @Test
    fun `initialization with ERROR logs error and does not speak`() {
        val manager = TextToSpeechManager(context)
        injectTts(manager, tts)

        manager.onInit(TextToSpeech.ERROR)
        manager.speak("Hello world")

        verify(exactly = 0) { tts.speak(any(), any(), any(), any()) }
        verify { Log.e("TextToSpeechManager", "TTS initialization failed with status: ${TextToSpeech.ERROR}") }
    }

    @Test
    fun `speak before initialization logs warning and does not speak`() {
        val manager = TextToSpeechManager(context)
        injectTts(manager, tts)

        manager.speak("Hello world")

        verify(exactly = 0) { tts.speak(any(), any(), any(), any()) }
        verify { Log.w("TextToSpeechManager", "TTS not initialized yet, cannot speak") }
    }

    @Test
    fun `shutdown stops and shuts down tts`() {
        val manager = TextToSpeechManager(context)
        injectTts(manager, tts)

        manager.shutdown()

        verify { tts.stop() }
        verify { tts.shutdown() }
    }

    private fun injectTts(manager: TextToSpeechManager, tts: TextToSpeech) {
        val field = TextToSpeechManager::class.java.getDeclaredField("tts")
        field.isAccessible = true
        field.set(manager, tts)
    }
}
