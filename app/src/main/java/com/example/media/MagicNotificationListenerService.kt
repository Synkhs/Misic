package com.example.media

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MagicNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        MediaHubController.init(applicationContext)
        MediaHubController.refreshPermissionsAndServices()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        MediaHubController.refreshPermissionsAndServices()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        MediaHubController.refreshPermissionsAndServices()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        MediaHubController.refreshPermissionsAndServices()
    }
}
