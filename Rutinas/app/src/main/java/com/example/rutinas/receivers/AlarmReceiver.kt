package com.example.rutinas.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import com.example.rutinas.data.repository.RoutineRepository
import com.example.rutinas.alarms.AlarmScheduler
import com.example.rutinas.execution.RoutineExecutor

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var routineRepository: RoutineRepository
    @Inject lateinit var routineExecutor: RoutineExecutor
    // Si quieres reprogramar tiempo de triggers, también puedes inyectar AlarmScheduler
    @Inject lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val uuid = intent.getStringExtra(EXTRA_ROUTINE_UUID) ?: return
        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        Timber.d("AlarmReceiver: Received alarm id=$alarmId, uuid=$uuid")

        CoroutineScope(Dispatchers.IO).launch {
            // Obtiene rutina del repositorio
            val routine = routineRepository.getRoutineByUuid(uuid)
            if (routine != null) {
                // Ejecuta la rutina
                routineExecutor.executeRoutine(routine, alarmId)
                // Opcional: si necesitas reprogramar futuros triggers:
                alarmScheduler.schedule(routine)
            } else {
                Timber.w("Routine no encontrada para uuid=$uuid")
            }
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "com.example.rutinas.extra.ALARM_ID"
        const val EXTRA_ROUTINE_UUID = "com.example.rutinas.extra.ROUTINE_UUID"
    }
}