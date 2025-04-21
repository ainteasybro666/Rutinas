package com.example.rutinasapp.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.rutinasapp.R
import com.example.rutinasapp.receivers.AlarmReceiver

object NotificationHelper {

    fun scheduleNotification(context: Context, triggerTime: Long, routineName: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("routine_name", routineName)
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }

    fun showNotification(context: Context, routineName: String) {
        val builder = NotificationCompat.Builder(context, "routine_channel")
            .setSmallIcon(R.drawable.ic_notification) // Asegúrate de tener un ícono de notificación
            .setContentTitle("Rutina Activada")
            .setContentText("Es hora de: $routineName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(context)) {
            notify(0, builder.build())
        }
    }
}