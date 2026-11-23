package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.example.media.MediaHubController

class MagicWidgetCapsuleProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        MediaHubController.init(context.applicationContext)
        MagicWidgetUpdater.updateAllWidgets(context.applicationContext)
    }
}

class MagicWidgetCardProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        MediaHubController.init(context.applicationContext)
        MagicWidgetUpdater.updateAllWidgets(context.applicationContext)
    }
}
