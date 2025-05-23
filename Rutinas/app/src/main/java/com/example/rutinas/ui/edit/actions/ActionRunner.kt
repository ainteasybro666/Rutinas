package com.example.rutinas.actions

import android.content.Context
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay // Para acciones PAUSE

// Usa @Singleton si quieres que haya una única instancia de ActionRunner en la app
@Singleton
class ActionRunner @Inject constructor(
    // Inyecta las dependencias que las diferentes acciones puedan necesitar
    private val context: Context
    // ... otras dependencias (ej: para controlar volumen, brillo, etc.)
) {

    /**
     * Executes a list of actions sequentially.
     * @param context The application context.
     * @param actions The list of actions to execute.
     */
    suspend fun runActions(context: Context, actions: List<Action>) {
        Timber.d("ActionRunner: Running ${actions.size} actions.")
        actions.sortedBy { it.executionOrder }.forEach { action ->
            Timber.d("ActionRunner: Executing action type: ${action.actionType}")
            when (action.actionType) {
                ActionType.ALARM -> {
                    // TODO: Implementar lógica para la acción ALARM
                    Timber.d("ActionRunner: Executing ALARM action.")
                    // Necesitarás reproducir un sonido de alarma, vibrar, etc.
                    // Esto puede requerir interactuar con el sistema de audio/alarma.
                    // Los datos de la acción (action.data) contendrán la configuración de la alarma.
                }
                ActionType.ANNOUNCEMENT -> {
                    // TODO: Implementar lógica para la acción ANNOUNCEMENT
                    Timber.d("ActionRunner: Executing ANNOUNCEMENT action.")
                    // Usar Text-to-Speech (TTS) para anunciar algo.
                    // Los datos de la acción contendrán el texto a anunciar.
                }
                ActionType.BRIGHTNESS -> {
                    // TODO: Implementar lógica para la acción BRIGHTNESS
                    Timber.d("ActionRunner: Executing BRIGHTNESS action.")
                    // Modificar el brillo de la pantalla. Requiere permisos de escritura de configuración.
                    // Los datos de la acción contendrán el nivel de brillo.
                }
                ActionType.PAUSE -> {
                    // TODO: Implementar lógica para la acción PAUSE
                    Timber.d("ActionRunner: Executing PAUSE action.")
                    // Pausar la ejecución por un tiempo. Usar delay() de corrutinas.
                    // Los datos de la acción contendrán la duración de la pausa.
                    val pauseDuration = action.pauseDuration ?: 0L // Duración en milisegundos
                    if (pauseDuration > 0) {
                        Timber.d("ActionRunner: Pausing for $pauseDuration ms.")
                        delay(pauseDuration)
                    }
                }
                ActionType.READ_NOTIFICATIONS -> {
                    // TODO: Implementar lógica para la acción READ_NOTIFICATIONS
                    Timber.d("ActionRunner: Executing READ_NOTIFICATIONS action.")
                    // Leer notificaciones. Requiere el permiso de Listener de Notificaciones.
                    // Puedes necesitar interactuar con un servicio que lea las notificaciones.
                }
                ActionType.SOUND_MODE -> {
                    // TODO: Implementar lógica para la acción SOUND_MODE
                    Timber.d("ActionRunner: Executing SOUND_MODE action.")
                    // Cambiar el modo de sonido (normal, vibrar, silencio).
                    // Requiere permisos. Usar AudioManager.
                    // Los datos de la acción contendrán el modo deseado.
                }
                ActionType.TIME -> {
                    // TODO: Implementar lógica para la acción TIME
                    Timber.d("ActionRunner: Executing TIME action.")
                    // Anunciar la hora actual (similar a ANNOUNCEMENT).
                }
                ActionType.VOLUME -> {
                    // TODO: Implementar lógica para la acción VOLUME
                    Timber.d("ActionRunner: Executing VOLUME action.")
                    // Cambiar el volumen de audio. Requiere permisos. Usar AudioManager.
                    // Los datos de la acción contendrán el stream de audio y el nivel.
                }

                // Añadir otros tipos de acciones aquí
                else -> {
                    Timber.w("ActionRunner: Unknown action type: ${action.actionType}. Skipping.")
                }
            }
        }
        Timber.d("ActionRunner: Finished running actions.")
    }
}
