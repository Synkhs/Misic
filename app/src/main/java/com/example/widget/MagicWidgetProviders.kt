package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import com.example.core.media.MediaRepository
import com.example.data.PreferencesRepository
import com.example.data.WidgetFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseMagicWidgetProvider(
    private val defaultFormat: WidgetFormat,
) : android.appwidget.AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val appCtx = context.applicationContext
        MediaRepository.init(appCtx)
        MagicWidgetUpdater.updateWidgetsForProvider(
            context = appCtx,
            appWidgetManager = appWidgetManager,
            appWidgetIds = appWidgetIds,
            defaultFormat = defaultFormat,
        )
        MediaRepository.evaluateSystemHealth(appCtx)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        val appCtx = context.applicationContext
        MediaRepository.init(appCtx)
        MagicWidgetUpdater.updateWidgetsForProvider(
            context = appCtx,
            appWidgetManager = appWidgetManager,
            appWidgetIds = intArrayOf(appWidgetId),
            defaultFormat = defaultFormat,
        )
    }

    override fun onEnabled(context: Context) {
        val appCtx = context.applicationContext
        MediaRepository.init(appCtx)
        MediaRepository.evaluateSystemHealth(appCtx)
        MagicWidgetUpdater.publishGeneratedPreviewsIfSupported(appCtx)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val appCtx = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PreferencesRepository(appCtx).deleteWidgetInstances(appWidgetIds)
            } finally {
                pending.finish()
            }
        }
        MediaRepository.evaluateSystemHealth(appCtx)
    }
}

class MagicWidgetCapsuleProvider : BaseMagicWidgetProvider(WidgetFormat.CAPSULE_4X1)

class MagicWidgetCardProvider : BaseMagicWidgetProvider(WidgetFormat.CARD_4X2)

class MagicWidgetSquareProvider : BaseMagicWidgetProvider(WidgetFormat.SQUARE_2X2)

class MagicWidgetMiniProvider : BaseMagicWidgetProvider(WidgetFormat.MINI_2X1)
