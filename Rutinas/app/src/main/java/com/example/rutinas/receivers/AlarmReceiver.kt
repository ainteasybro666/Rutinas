package com.example.rutinas.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.rutinas.service.RoutineExecutionService
import dagger.hilt.android.AndroidEntryPoint // Aunque no lo usaremos aquí para inyectar directamente en el Receiver, lo mantenemos por si otras clases lo necesitan
import timber.log.Timber

// No necesitas @AndroidEntryPoint en el BroadcastReceiver si solo inicia un servicio
// y no inyecta dependencias que requieran el Receiver en sí como componente de Hilt.
// Lo dejo comentado por ahora, pero podrías quitarlo si solo inicia el servicio.
//@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    // Ya no inyectamos dependencias directamente aquí
    // @Inject lateinit var routineRepository: RoutineRepository
    // @Inject lateinit var routineExecutor: RoutineExecutor
    // @Inject lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val uuid = intent.getStringExtra(EXTRA_ROUTINE_UUID)
        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)

        if (uuid == null || alarmId == -1) {
            Timber.e("AlarmReceiver: Received alarm intent with missing UUID or Alarm ID. UUID: $uuid, Alarm ID: $alarmId")
            // Optionally show a Toast here on the main thread if it's a critical error
            // Handler(Looper.getMainLooper()).post {
            //    Toast.makeText(context, "Error: Datos de alarma faltantes", Toast.LENGTH_SHORT).show()
            // }
            return
        }

        Timber.d("AlarmReceiver: Received alarm id=$alarmId, uuid=$uuid. Starting RoutineExecutionService.")

        // <<-- DELEGAR AL SERVICIO -->>
        // Creamos un Intent para iniciar el RoutineExecutionService
        val serviceIntent = Intent(context, RoutineExecutionService::class.java).apply {
            putExtra(EXTRA_ROUTINE_UUID, uuid)
            putExtra(EXTRA_ALARM_ID, alarmId)
            // Puedes añadir más extras si el servicio los necesita
        }

        // Iniciar el servicio. Usar startForegroundService si tu servicio hará trabajo de larga duración en segundo plano.
        // Para Android 8.0 (API 26) y superior, debes usar startForegroundService
        try {
            // context.startForegroundService(serviceIntent) // Usar si el servicio se ejecuta en primer plano
            context.startService(serviceIntent) // Usar si el servicio puede ser en segundo plano (menos de 10s)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start RoutineExecutionService")
            // Handle potential exceptions, e.g., if the service is not declared properly
        }
        // <<-- FIN DELEGAR -->>

        // El BroadcastReceiver debe terminar su ejecución rápidamente.
        // goAsync() podría ser necesario si necesitas hacer *algún* trabajo asíncrono muy ligero aquí,
        // pero en este caso, simplemente iniciar el servicio es suficiente y rápido.
        // val pendingResult: PendingResult = goAsync()
        // launch coroutine, start service, call pendingResult.finish()
    }

    companion object {
        const val EXTRA_ALARM_ID = "com.example.rutinas.extra.ALARM_ID"
        const val EXTRA_ROUTINE_UUID = "com.example.rutinas.extra.ROUTINE_UUID"
        // Añade aquí otras acciones de Intent que tu Receiver pueda manejar (ej: STOP_ALARM)
        // const val ACTION_STOP_ALARM = "com.example.rutinas.action.STOP_ALARM"
    }
}
