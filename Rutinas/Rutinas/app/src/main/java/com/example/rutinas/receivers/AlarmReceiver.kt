package com.example.rutinas.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.rutinas.alarms.AlarmScheduler
import com.example.rutinas.data.model.Routine
import com.example.rutinas.execution.RoutineExecutor // Import RoutineExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

class AlarmReceiver : BroadcastReceiver() {

    // Using Koin for dependency injection (assuming it's configured)
    private val routineExecutor: RoutineExecutor by inject() // Inject RoutineExecutor
    private val alarmScheduler: AlarmScheduler by inject()
    //  We will no longer use RoutineEditViewModel here
    //  private val viewModel: RoutineEditViewModel by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        //  We'll now receive the Routine object directly
        val routine = intent.getParcelableExtra<Routine>(EXTRA_ROUTINE)

        if (alarmId == -1 || routine == null) {
            Timber.e("AlarmReceiver: Invalid intent - alarmId: $alarmId, routine: $routine")
            return
        }

        Timber.d("AlarmReceiver: Received alarm with id: $alarmId, routine: ${routine.name} (${routine.uuid})")

        // Launch a coroutine on the IO dispatcher to execute the routine on a background thread
        CoroutineScope(Dispatchers.IO).launch {
            routineExecutor.executeRoutine(routine, alarmId)
        }

        // If reschedule is needed for repeating alarms, it should be handled in a separate mechanism
        // as this receiver might be killed by the system shortly after onReceive returns.
    }


    companion object {
        const val EXTRA_ALARM_ID = "com.example.rutinas.extra.ALARM_ID"
        //  We'll now pass the entire Routine object
        const val EXTRA_ROUTINE = "com.example.rutinas.extra.ROUTINE"
    }
}
