package com.example.rutinas.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

// This receiver will be triggered when the "Stop Alarm" button in the notification is clicked
class AlarmStopperReceiver : BroadcastReceiver() {

    companion object {
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
            Timber.d("AlarmStopperReceiver: Received STOP_ALARM action")
            // TODO: Implement the logic to stop the alarm in RoutineExecutor
            // How to access the running RoutineExecutor instance?
            // Option 1: Send another broadcast that RoutineExecutor listens for.
            // Option 2: If RoutineExecutor is a Service, send an Intent to the Service.
            // Option 3: If RoutineExecutor is a Singleton accessible globally.

            // For now, let's just log and decide on the communication method.
            // A common pattern is to start a foreground service from the receiver
            // if the work is long-running or needs to continue after the receiver finishes.
            // But stopping an alarm is usually quick.
            // Sending a local broadcast might be an option if RoutineExecutor is active in the main thread.

            // Let's plan to use a local broadcast or a direct call if RoutineExecutor can be a Singleton
            // For now, a placeholder log:
            Timber.i("AlarmStopperReceiver: Signal received to stop alarm.")
            // We will replace this with the actual call to stop the alarm in RoutineExecutor
        }
    }
}