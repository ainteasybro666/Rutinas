package com.example.rutinas.ui.viewmodel

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rutinas.di.TtsInitStatus // Import TtsInitStatus
import com.example.rutinas.di.TtsStatusNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

// Define a state to track TextToSpeech initialization status (can remove if shared from Module)
// enum class TtsInitStatus { ... } // Remove this enum if you want to use the one from the module

@HiltViewModel
class TextToSpeechViewModel @Inject constructor(
    private val textToSpeech: TextToSpeech, // Inject TextToSpeech
    private val ttsStatusNotifier: TtsStatusNotifier // Inject the wrapper interface
) : ViewModel() {

    // Expose the init status from the injected wrapper for UI observation
    val initStatus: StateFlow<TtsInitStatus> = ttsStatusNotifier.status

    // Expose the TextToSpeech instance (use with care, check initStatus)
    val tts: TextToSpeech
        get() {
            // In production, you might throw an error or return null if not initialized
            // For now, assuming the UI checks initStatus before using tts
            return textToSpeech
        }

    init {
        Timber.d("TextToSpeechViewModel: Initialized. Observing TTS init status.")

        // Collect the init status changes from the injected StateFlow via the wrapper
        viewModelScope.launch {
            // Observe the exposed initStatus property
            initStatus.collectLatest { status: TtsInitStatus -> // Explicitly name parameter and specify type
                Timber.d("TextToSpeechViewModel: Observed TTS init status change: $status")
                if (status == TtsInitStatus.INITIALIZED_SUCCESS) {
                    Timber.d("TextToSpeechViewModel: TTS initialized successfully, setting default language.")
                    // Now it's safe to set the language and configure other properties
                    try {
                        // Set default language
                        val languageResult = textToSpeech.setLanguage(Locale.getDefault())
                        if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Timber.e("TextToSpeechViewModel: Default language not supported after init.")
                        } else {
                            Timber.d("TextToSpeechViewModel: Default language set after init.")
                        }

                        // Set other properties like pitch and speech rate here
                        textToSpeech.setPitch(1.0f)
                        textToSpeech.setSpeechRate(1.0f)
                        Timber.d("TextToSpeechViewModel: Pitch and speech rate set.")

                    } catch (e: Exception) {
                        Timber.e(e, "TextToSpeechViewModel: Error configuring TTS after init.")
                        // Handle error, maybe log or update a separate error state in the ViewModel
                    }

                } else if (status == TtsInitStatus.INITIALIZED_FAILURE) {
                    Timber.e("TextToSpeechViewModel: TTS initialization failed.")
                    // Handle the failure, e.g., disable TTS functionality in the UI
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("TextToSpeechViewModel: ViewModel cleared, shutting down TextToSpeech.")
        // Shut down the injected TextToSpeech instance
        textToSpeech.shutdown()
    }

    // Methods to be used by the Fragment/UI
    fun getAvailableLanguages(): Set<Locale> {
        // Check if TTS is initialized before accessing using the exposed initStatus
        return if (initStatus.value == TtsInitStatus.INITIALIZED_SUCCESS) {
            try {
                textToSpeech.availableLanguages
            } catch (e: Exception) {
                Timber.e(e, "TextToSpeechViewModel: Error getting available languages after init success.")
                emptySet()
            }
        } else {
            Timber.w("TextToSpeechViewModel: getAvailableLanguages() called before TTS initialized. Status: ${initStatus.value}")
            emptySet()
        }
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH, params: Bundle? = null, utteranceId: String? = null) {
        // Check if TTS is initialized before speaking using the exposed initStatus
        if (initStatus.value == TtsInitStatus.INITIALIZED_SUCCESS) {
            val result = textToSpeech.speak(text, queueMode, params, utteranceId)
            if (result == TextToSpeech.ERROR) {
                Timber.e("TextToSpeechViewModel: Error speaking text.")
                // Handle the error, e.g., show a message to the user
            } else {
                Timber.d("TextToSpeechViewModel: Speak request successful.")
            }
        } else {
            Timber.w("TextToSpeechViewModel: speak() called before TTS initialized. Status: ${initStatus.value}")
            // Consider queuing the request or showing an error to the user
        }
    }
}