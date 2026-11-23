package com.example.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.PreferencesRepository
import com.example.media.MediaHubController
import kotlinx.coroutines.runBlocking

class MagicWidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        MediaHubController.init(context.applicationContext)
        when (intent.action) {
            ACTION_PLAY_PAUSE -> MediaHubController.playPause()
            ACTION_NEXT -> MediaHubController.next()
            ACTION_PREV -> MediaHubController.previous()
            ACTION_OPEN_SOURCE -> MediaHubController.openActiveOrPreferredApp(context.applicationContext)
            ACTION_CUSTOM_LEFT -> {
                val cfg = runBlocking { PreferencesRepository(context).getWidgetConfigSnapshot() }
                MediaHubController.executeCustomAction(cfg.leftCustomAction)
            }
            ACTION_CUSTOM_RIGHT -> {
                val cfg = runBlocking { PreferencesRepository(context).getWidgetConfigSnapshot() }
                MediaHubController.executeCustomAction(cfg.rightCustomAction)
            }
        }
        MagicWidgetUpdater.updateAllWidgets(context.applicationContext)
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.widget.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.widget.ACTION_NEXT"
        const val ACTION_PREV = "com.example.widget.ACTION_PREV"
        const val ACTION_CUSTOM_LEFT = "com.example.widget.ACTION_CUSTOM_LEFT"
        const val ACTION_CUSTOM_RIGHT = "com.example.widget.ACTION_CUSTOM_RIGHT"
        const val ACTION_OPEN_SOURCE = "com.example.widget.ACTION_OPEN_SOURCE"
    }
}
