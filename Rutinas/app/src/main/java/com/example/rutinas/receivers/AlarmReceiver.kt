package com.example.rutinas.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.rutinas.domain.Routine
import com.example.rutinas.data.repository.RoutineRepository
import com.example.rutinas.execution.RoutineExecutor
import com.example.rutinas.alarms.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import android.os.Handler
import android.os.Looper
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var routineRepository: RoutineRepository
    @Inject lateinit var routineExecutor: RoutineExecutor
    @Inject lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val uuid = intent.getStringExtra(EXTRA_ROUTINE_UUID) ?: return
        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        Timber.d("AlarmReceiver: Received alarm id=$alarmId, uuid=$uuid")

        CoroutineScope(Dispatchers.IO).launch {
            val routine = routineRepository.getRoutineByUuid(uuid)
            if (routine != null) {
                if (routine.isEnabled) {
                    Timber.i("AlarmReceiver: Routine ${routine.name} (${routine.uuid}) is enabled. Executing.")
                    routineExecutor.executeRoutine(routine, alarmId)
                    alarmScheduler.schedule(routine)
                } else {
                    Timber.i("AlarmReceiver: Routine ${routine.name} (${routine.uuid}) is disabled. Skipping execution.")
                    alarmScheduler.cancel(routine)
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "Rutina '${routine.name}' está deshabilitada", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Timber.w("Routine no encontrada para uuid=$uuid")
                // CORRECTED: Provide a placeholder name for the dummy Routine
                alarmScheduler.cancel(Routine(uuid = uuid, name = "Unknown Routine"))
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Rutina desconocida (UUID: $uuid) disparada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "com.example.rutinas.extra.ALARM_ID"
        const val EXTRA_ROUTINE_UUID = "com.example.rutinas.extra.ROUTINE_UUID"
    }
}
