package com.example.rutinas.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class AnnouncementPlayer(private val context: Context) {

    private var tts: TextToSpeech? = null

    fun initialize(onInitialized: (Boolean) -> Unit) {
        tts = TextToSpeech(context) { status ->
            onInitialized(status == TextToSpeech.SUCCESS)
        }
    }

    fun setLanguage(languageCode: String) {
        val locale = when (languageCode) {
            "en" -> Locale.ENGLISH
            "pt" -> Locale("pt", "BR")
            else -> Locale("es", "ES")
        }
        tts?.language = locale
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "announcement")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}