package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.CustomControlAction
import com.example.data.PreferencesRepository
import com.example.data.UnifiedMediaState
import com.example.data.WidgetConfig
import com.example.data.WidgetStyle
import com.example.media.MediaHubController
import kotlinx.coroutines.runBlocking

object MagicWidgetUpdater {

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        val media = MediaHubController.mediaState.value
        val config = runBlocking {
            PreferencesRepository(context).getWidgetConfigSnapshot()
        }

        val capsuleIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, MagicWidgetCapsuleProvider::class.java),
        )
        if (capsuleIds.isNotEmpty()) {
            val capsuleViews = buildCapsuleViews(context, media, config)
            appWidgetManager.updateAppWidget(capsuleIds, capsuleViews)
        }

        val cardIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, MagicWidgetCardProvider::class.java),
        )
        if (cardIds.isNotEmpty()) {
            val cardViews = buildCardViews(context, media, config)
            appWidgetManager.updateAppWidget(cardIds, cardViews)
        }
    }

    private fun buildCapsuleViews(
        context: Context,
        media: UnifiedMediaState,
        config: WidgetConfig,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_magicos_capsule)
        val isLight = config.style == WidgetStyle.PORCELAIN_LIGHT

        views.setInt(R.id.widget_capsule_root, "setBackgroundResource", backgroundResForStyle(config.style))

        val titleColor = if (isLight) Color.parseColor("#0F172A") else Color.WHITE
        val artistColor = if (isLight) Color.parseColor("#475569") else Color.parseColor("#B8C7E0")
        val badgeColor = if (isLight) Color.parseColor("#2563EB") else Color.parseColor("#00D2FF")
        val iconTint = if (isLight) Color.parseColor("#1E293B") else Color.parseColor("#DCE8FF")

        views.setTextViewText(R.id.widget_track_title, media.title)
        views.setTextViewText(R.id.widget_track_artist, media.artist)
        views.setTextColor(R.id.widget_track_title, titleColor)
        views.setTextColor(R.id.widget_track_artist, artistColor)
        views.setTextColor(R.id.widget_service_badge, badgeColor)

        if (config.showServiceBadge) {
            views.setViewVisibility(R.id.widget_service_badge, View.VISIBLE)
            val prefix = if (config.showMagicCapsuleHeader) "MAGIC CAPSULE • " else ""
            views.setTextViewText(
                R.id.widget_service_badge,
                "$prefix${media.service.displayName.uppercase()}",
            )
        } else {
            views.setViewVisibility(R.id.widget_service_badge, View.GONE)
        }

        val artBitmap = prepareRoundedArtwork(context, media.albumArt, 144, 24f)
        views.setImageViewBitmap(R.id.widget_album_art, artBitmap)

        views.setImageViewResource(
            R.id.widget_btn_play_pause,
            if (media.isPlaying) R.drawable.ic_magic_pause else R.drawable.ic_magic_play,
        )
        views.setInt(R.id.widget_btn_prev, "setColorFilter", iconTint)
        views.setInt(R.id.widget_btn_next, "setColorFilter", iconTint)

        views.setOnClickPendingIntent(
            R.id.widget_btn_play_pause,
            actionPendingIntent(context, MagicWidgetActionReceiver.ACTION_PLAY_PAUSE, 11),
        )
        views.setOnClickPendingIntent(
            R.id.widget_btn_prev,
            actionPendingIntent(context, MagicWidgetActionReceiver.ACTION_PREV, 12),
        )
        views.setOnClickPendingIntent(
            R.id.widget_btn_next,
            actionPendingIntent(context, MagicWidgetActionReceiver.ACTION_NEXT, 13),
        )
        views.setOnClickPendingIntent(
            R.id.widget_album_art,
            openAppPendingIntent(context, 14),
        )
        views.setOnClickPendingIntent(
            R.id.widget_track_info_container,
            openAppPendingIntent(context, 15),
        )

        return views
    }

    private fun buildCardViews(
        context: Context,
        media: UnifiedMediaState,
        config: WidgetConfig,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_magicos_card)
        val isLight = config.style == WidgetStyle.PORCELAIN_LIGHT

        views.setInt(R.id.widget_card_root, "setBackgroundResource", backgroundResForStyle(config.style))

        val titleColor = if (isLight) Color.parseColor("#0F172A") else Color.WHITE
        val artistColor = if (isLight) Color.parseColor("#475569") else Color.parseColor("#B8C7E0")
        val badgeColor = if (isLight) Color.parseColor("#2563EB") else Color.parseColor("#00D2FF")
        val primaryTint = if (isLight) Color.parseColor("#0F172A") else Color.WHITE
        val secondaryTint = if (isLight) Color.parseColor("#475569") else Color.parseColor("#9CB3D6")

        views.setTextViewText(R.id.widget_card_title, media.title)
        views.setTextViewText(R.id.widget_card_artist, media.artist)
        views.setTextColor(R.id.widget_card_title, titleColor)
        views.setTextColor(R.id.widget_card_artist, artistColor)
        views.setTextColor(R.id.widget_card_service_pill, badgeColor)

        if (config.showServiceBadge) {
            views.setViewVisibility(R.id.widget_card_service_pill, View.VISIBLE)
            val prefix = if (config.showMagicCapsuleHeader) "MAGIC OS 10 • " else ""
            views.setTextViewText(
                R.id.widget_card_service_pill,
                "$prefix${media.service.displayName.uppercase()}",
            )
        } else {
            views.setViewVisibility(R.id.widget_card_service_pill, View.GONE)
        }

        val artBitmap = prepareRoundedArtwork(context, media.albumArt, 180, 30f)
        views.setImageViewBitmap(R.id.widget_card_album_art, artBitmap)

        if (config.showProgressBar) {
            views.setViewVisibility(R.id.widget_card_progress, View.VISIBLE)
            val progressInt = (media.progressFraction * 1000f).toInt().coerceIn(0, 1000)
            views.setProgressBar(R.id.widget_card_progress, 1000, progressInt, false)
        } else {
            views.setViewVisibility(R.id.widget_card_progress, View.GONE)
        }

        views.setImageViewResource(
            R.id.widget_card_btn_play_pause,
            if (media.isPlaying) R.drawable.ic_magic_pause else R.drawable.ic_magic_play,
        )
        views.setImageViewResource(
            R.id.widget_card_btn_custom_left,
            iconResForCustomAction(config.leftCustomAction),
        )
        views.setImageViewResource(
            R.id.widget_card_btn_custom_right,
            iconResForCustomAction(config.rightCustomAction),
        )

        views.setInt(R.id.widget_card_btn_prev, "setColorFilter", primaryTint)
        views.setInt(R.id.widget_card_btn_next, "setColorFilter", primaryTint)
        views.setInt(R.id.widget_card_btn_custom_left, "setColorFilter", secondaryTint)
        views.setInt(R.id.widget_card_btn_custom_right, "setColorFilter", secondaryTint)
        views.setInt(R.id.widget_card_btn_source, "setColorFilter", badgeColor)

        views.setOnClickPendingIntent(
            R.id.widget_card_btn_play_pause,
            actionPendingIntent(context, MagicWidgetActionReceiver.ACTION_PLAY_PAUSE, 21),
        )
        views.setOnClickPendingIntent(
            R.id.widget_card_btn_prev,
            actionPendingIntent(context, MagicWidgetActionReceiver.ACTION_PREV, 22),
        )
        views.setOnClickPendingIntent(
            R.id.widget_card_btn_next,
            actionPendingIntent(context, MagicWidgetActionReceiver.ACTION_NEXT, 23),
        )
        views.setOnClickPendingIntent(
            R.id.widget_card_btn_custom_left,
            actionPendingIntent(context, MagicWidgetActionReceiver.ACTION_CUSTOM_LEFT, 24),
        )
        views.setOnClickPendingIntent(
            R.id.widget_card_btn_custom_right,
            actionPendingIntent(context, MagicWidgetActionReceiver.ACTION_CUSTOM_RIGHT, 25),
        )
        views.setOnClickPendingIntent(
            R.id.widget_card_btn_source,
            actionPendingIntent(context, MagicWidgetActionReceiver.ACTION_OPEN_SOURCE, 26),
        )
        views.setOnClickPendingIntent(
            R.id.widget_card_album_art,
            openAppPendingIntent(context, 27),
        )
        views.setOnClickPendingIntent(
            R.id.widget_card_info,
            openAppPendingIntent(context, 28),
        )

        return views
    }

    private fun backgroundResForStyle(style: WidgetStyle): Int = when (style) {
        WidgetStyle.CAPSULE_DARK -> R.drawable.bg_widget_capsule_dark
        WidgetStyle.FROSTED_GLASS -> R.drawable.bg_widget_frosted_glass
        WidgetStyle.PORCELAIN_LIGHT -> R.drawable.bg_widget_porcelain_light
        WidgetStyle.DYNAMIC_NEON -> R.drawable.bg_widget_dynamic_neon
    }

    private fun iconResForCustomAction(action: CustomControlAction): Int = when (action) {
        CustomControlAction.REWIND_15 -> R.drawable.ic_magic_replay15
        CustomControlAction.FORWARD_15 -> R.drawable.ic_magic_forward15
        CustomControlAction.SWITCH_SOURCE -> R.drawable.ic_magic_swap
        CustomControlAction.OPEN_PLAYER -> R.drawable.ic_magic_open
        CustomControlAction.MUTE_AUDIO -> R.drawable.ic_magic_mute
    }

    private fun actionPendingIntent(context: Context, action: String, reqCode: Int): PendingIntent {
        val intent = Intent(context, MagicWidgetActionReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openAppPendingIntent(context: Context, reqCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun prepareRoundedArtwork(
        context: Context,
        source: Bitmap?,
        sizePx: Int,
        radiusPx: Float,
    ): Bitmap {
        val raw = source ?: BitmapFactory.decodeResource(context.resources, R.drawable.img_magic_album_art)
        val scaled = Bitmap.createScaledBitmap(raw, sizePx, sizePx, true)
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())
        canvas.drawRoundRect(rect, radiusPx, radiusPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, null, rect, paint)
        return output
    }
}
