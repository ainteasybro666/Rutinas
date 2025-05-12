package com.example.rutinas.utils

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import timber.log.Timber
import java.util.Locale

class NotificationReader(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    companion object {
        private const val TAG = "NotificationReader"
    }

    fun initialize(onInitialized: (Boolean) -> Unit) {
        tts = TextToSpeech(context) { status ->
            isInitialized = status == TextToSpeech.SUCCESS
            if (isInitialized) {
                tts?.language = Locale.getDefault()
            }
            onInitialized(isInitialized)
        }
    }

    fun readNotifications(
        notifications: Array<StatusBarNotification>,
        excludedPackages: List<String>,
        onComplete: () -> Unit,
    ) {
        if (!isInitialized) {
            Timber.w("TTS no inicializado")
            onComplete()
            return
        }

        // Registrar todas las notificaciones para depuración
        logNotifications(notifications)

        val notificationsToRead = notifications
            .filter { shouldReadNotification(it, excludedPackages) }
            .map { notification ->
                val title = notification.notification.extras.getString(Notification.EXTRA_TITLE, "")
                val text = notification.notification.extras.getString(Notification.EXTRA_TEXT, "")
                "$title: $text"
            }

        if (notificationsToRead.isEmpty()) {
            Timber.i("No hay notificaciones para leer")
            tts?.speak(
                "No hay notificaciones pendientes",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "NO_NOTIFICATIONS"
            )
            onComplete()
            return
        }

        // Leer cada notificación
        notificationsToRead.forEachIndexed { index, text ->
            Timber.i("Leyendo notificación $index: $text")
            val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(text, queueMode, null, "NOTIFICATION_$index")
        }

        onComplete()
    }

    private fun shouldReadNotification(
        sbn: StatusBarNotification,
        excludedPackages: List<String>,
    ): Boolean {
        // Excluir notificaciones del sistema
        if (isSystemNotification(sbn)) {
            Timber.d("Notificación excluida (sistema): ${sbn.packageName}")
            return false
        }

        // Excluir notificaciones persistentes
        if (isOngoingNotification(sbn)) {
            Timber.d("Notificación excluida (persistente): ${sbn.packageName}")
            return false
        }

        // Excluir notificaciones de paquetes específicos
        if (excludedPackages.contains(sbn.packageName)) {
            Timber.d("Notificación excluida (lista de exclusión): ${sbn.packageName}")
            return false
        }

        // Verificar que la notificación tenga texto para leer
        val text = sbn.notification.extras.getString(Notification.EXTRA_TEXT)
        if (text.isNullOrBlank()) {
            Timber.d("Notificación excluida (sin texto): ${sbn.packageName}")
            return false
        }

        return true
    }

    private fun isSystemNotification(sbn: StatusBarNotification): Boolean {
        return sbn.packageName.startsWith("android") ||
                sbn.packageName.startsWith("com.android")
    }

    private fun isOngoingNotification(sbn: StatusBarNotification): Boolean {
        return sbn.notification.flags and Notification.FLAG_ONGOING_EVENT != 0
    }

    private fun logNotifications(notifications: Array<StatusBarNotification>) {
        Timber.d("=== Notificaciones Activas ===")
        notifications.forEach { sbn ->
            val packageName = sbn.packageName
            val title = sbn.notification.extras.getString(Notification.EXTRA_TITLE, "Sin título")
            val text = sbn.notification.extras.getString(Notification.EXTRA_TEXT, "Sin texto")
            val isOngoing = isOngoingNotification(sbn)

            Timber.tag(TAG).d(
                """
                Paquete: $packageName
                Título: $title
                Texto: $text
                Persistente: $isOngoing
                ===
            """.trimIndent()
            )
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        isInitialized = false
    }
}