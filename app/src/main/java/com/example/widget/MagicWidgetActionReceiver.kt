package com.example.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.core.media.MediaRepository
import com.example.data.PreferencesRepository
import kotlinx.coroutines.runBlocking

class MagicWidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appCtx = context.applicationContext
        MediaRepository.init(appCtx)
        when (intent.action) {
            ACTION_PLAY_PAUSE -> MediaRepository.playPause()
            ACTION_NEXT -> MediaRepository.next()
            ACTION_PREV -> MediaRepository.previous()
            ACTION_OPEN_SOURCE -> MediaRepository.openActiveOrPreferredApp(appCtx)
            ACTION_CUSTOM_LEFT -> {
                val cfg = runBlocking { PreferencesRepository(appCtx).getWidgetConfigSnapshot() }
                MediaRepository.executeCustomAction(cfg.leftCustomAction)
            }
            ACTION_CUSTOM_RIGHT -> {
                val cfg = runBlocking { PreferencesRepository(appCtx).getWidgetConfigSnapshot() }
                MediaRepository.executeCustomAction(cfg.rightCustomAction)
            }
        }
        MagicWidgetUpdater.updateAllWidgets(appCtx, force = true)
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
