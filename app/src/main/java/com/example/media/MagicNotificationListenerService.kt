package com.example.media

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.core.media.MediaRepository

class MagicNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        MediaRepository.init(applicationContext)
        MediaRepository.onListenerConnectionChanged(connected = true)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        MediaRepository.onListenerConnectionChanged(connected = false)
        runCatching {
            requestRebind(ComponentName(this, MagicNotificationListenerService::class.java))
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        MediaRepository.refreshPermissionsAndServices()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        MediaRepository.refreshPermissionsAndServices()
    }
}
