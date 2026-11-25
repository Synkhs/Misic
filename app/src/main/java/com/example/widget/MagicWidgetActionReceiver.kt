package com.example.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.core.media.MediaRepository
import com.example.data.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MagicWidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appCtx = context.applicationContext
        MediaRepository.init(appCtx)

        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                MediaRepository.playPause()
                MagicWidgetUpdater.updateAllWidgets(appCtx, force = true)
            }
            ACTION_NEXT -> {
                MediaRepository.next()
                MagicWidgetUpdater.updateAllWidgets(appCtx, force = true)
            }
            ACTION_PREV -> {
                MediaRepository.previous()
                MagicWidgetUpdater.updateAllWidgets(appCtx, force = true)
            }
            ACTION_OPEN_SOURCE -> {
                MediaRepository.openActiveOrPreferredApp(appCtx)
            }
            ACTION_CUSTOM_LEFT, ACTION_CUSTOM_RIGHT -> {
                val pending = goAsync()
                val isLeft = intent.action == ACTION_CUSTOM_LEFT
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val cfg = PreferencesRepository(appCtx).getWidgetConfigSnapshot()
                        val customAction = if (isLeft) cfg.leftCustomAction else cfg.rightCustomAction
                        withContext(Dispatchers.Main.immediate) {
                            MediaRepository.executeCustomAction(customAction)
                            MagicWidgetUpdater.updateAllWidgets(appCtx, force = true)
                        }
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
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
