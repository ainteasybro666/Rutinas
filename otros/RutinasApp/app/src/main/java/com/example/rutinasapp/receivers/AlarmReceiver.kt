package com.example.rutinasapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.rutinasapp.utils.NotificationHelper

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val routineName = intent.getStringExtra("routine_name") ?: return
        NotificationHelper.showNotification(context, routineName)
    }
}