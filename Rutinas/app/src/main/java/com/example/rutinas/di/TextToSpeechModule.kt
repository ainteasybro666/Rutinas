package com.example.rutinas.di

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.Locale
import javax.inject.Singleton

// Define a state to track TextToSpeech initialization status (same as in ViewModel)
enum class TtsInitStatus {
    NOT_INITIALIZED, INITIALIZING, INITIALIZED_SUCCESS, INITIALIZED_FAILURE
}

interface TtsStatusNotifier {
    val status: StateFlow<TtsInitStatus>
}

@Module // @Module annotation on the object itself
@InstallIn(SingletonComponent::class)
object TextToSpeechModule : TtsStatusNotifier { // Implement the interface directly in the object

    // Implement the interface property
    override val status: StateFlow<TtsInitStatus>
        get() = _initStatus.asStateFlow()

    // Provide a MutableStateFlow to track the initialization status
    private val _initStatus = MutableStateFlow(TtsInitStatus.NOT_INITIALIZED)

    // @Provides functions moved directly inside the object (outside the companion object)
    @Provides
    @Singleton
    fun provideTtsStatusNotifier(): TtsStatusNotifier {
        // Return the module object itself which implements the interface
        return this // 'this' refers to the TextToSpeechModule object
    }

    @Provides
    @Singleton
    fun provideTextToSpeech(
        @ApplicationContext context: Context
    ): TextToSpeech {
        Timber.d("TextToSpeechModule: Providing TextToSpeech instance.")
        _initStatus.value = TtsInitStatus.INITIALIZING // Update status before creating

        val tts = TextToSpeech(context, TextToSpeech.OnInitListener { status ->
            Timber.d("TextToSpeechModule: OnInitListener received status: $status")
            if (status == TextToSpeech.SUCCESS) {
                Timber.d("TextToSpeechModule: TTS initialized successfully via listener.")
                _initStatus.value = TtsInitStatus.INITIALIZED_SUCCESS // Update status on success
            } else {
                Timber.e("TextToSpeechModule: TTS initialization failed via listener. Status: $status")
                _initStatus.value = TtsInitStatus.INITIALIZED_FAILURE // Update status on failure
            }
        })
        // Return the instance immediately.
        return tts
    }
}
