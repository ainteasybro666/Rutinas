package com.example.rutinas.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.rutinas.service.RoutineExecutionService // Import the new service
import timber.log.Timber

// This receiver will be triggered when the "Stop Alarm" button in the notification is clicked
class AlarmStopperReceiver : BroadcastReceiver() {

    companion object {
        // This action is triggered by the notification button
        const val ACTION_STOP_ALARM = "com.example.rutinas.action.STOP_ALARM"
        // We might need a way to identify which alarm instance to stop if multiple can be active
        // For now, we'll assume only one alarm can be active at a time or that stopping any alarm is okay.
        // If needed, we could pass an alarm ID extra in the Intent.
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Timber.e("AlarmStopperReceiver: context or intent is null")
            return
        }

        if (intent.action == ACTION_STOP_ALARM) {
            Timber.d("AlarmStopperReceiver: Received STOP_ALARM action from notification.")
            // Now, send an Intent to the RoutineExecutionService to stop the alarm
            RoutineExecutionService.stopCurrentAlarm(context)
            Timber.i("AlarmStopperReceiver: Sent signal to RoutineExecutionService to stop alarm.")
        }
    }
}
