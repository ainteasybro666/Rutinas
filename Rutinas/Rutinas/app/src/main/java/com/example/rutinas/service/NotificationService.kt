package com.example.rutinas.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import timber.log.Timber

class NotificationService : NotificationListenerService() {
    companion object {
        private const val TAG = "NotificationService"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Timber.tag(TAG).d("Nueva notificación: ${sbn.packageName}")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Timber.tag(TAG).d("Notificación removida: ${sbn.packageName}")
    }
}