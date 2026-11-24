package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.util.LruCache
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.palette.graphics.Palette
import com.example.MainActivity
import com.example.R
import com.example.core.compat.AndroidVersionCompat
import com.example.core.media.MediaRepository
import com.example.data.CustomControlAction
import com.example.data.PreferencesRepository
import com.example.data.TrackHistoryEntry
import com.example.data.UnifiedMediaState
import com.example.data.WidgetConfig
import com.example.data.WidgetFormat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object MagicWidgetUpdater {

    private var lastRenderSignature: String? = null
    private var lastProgressUpdateElapsedMs: Long = 0L
    private var lastPreviewPublishMs: Long = 0L

    private val artworkCache = object : LruCache<String, Bitmap>(16) {}
    private val backgroundCache = object : LruCache<String, Bitmap>(16) {}
    private val dominantColorCache = object : LruCache<String, Int>(24) {}

    fun updateAllWidgets(context: Context, force: Boolean = false) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        val capsuleIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, MagicWidgetCapsuleProvider::class.java),
        )
        val cardIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, MagicWidgetCardProvider::class.java),
        )
        val squareIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, MagicWidgetSquareProvider::class.java),
        )
        val miniIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, MagicWidgetMiniProvider::class.java),
        )

        if (capsuleIds.isEmpty() && cardIds.isEmpty() && squareIds.isEmpty() && miniIds.isEmpty()) {
            return
        }

        val media = MediaRepository.mediaState.value
        val snapshot = readSnapshot(context)

        // Progress throttling: at most 1 update/sec while screen is on, unless metadata/playstate/config changed
        val signatureWithoutProgress = buildSignatureWithoutProgress(media, snapshot.config, snapshot.preferredPkg)
        val now = android.os.SystemClock.elapsedRealtime()
        val progressBucket = (media.progressFraction * 100f).toInt()
        val fullSignature = "$signatureWithoutProgress|p=$progressBucket"
        if (!force && fullSignature == lastRenderSignature) {
            return
        }
        val onlyProgressChanged = !force &&
            lastRenderSignature?.substringBefore("|p=") == signatureWithoutProgress &&
            (now - lastProgressUpdateElapsedMs) < 950L
        if (onlyProgressChanged) {
            return
        }
        lastRenderSignature = fullSignature
        lastProgressUpdateElapsedMs = now

        val copy = WidgetRenderModel.resolveDisplayCopy(
            media = media,
            config = snapshot.config,
            lastHistoryTrack = snapshot.lastHistoryTrack,
            preferredPackage = snapshot.preferredPkg,
        )
        val artDominant = extractDominantColorCached(media)
        val palette = WidgetRenderModel.resolvePalette(
            style = snapshot.config.style,
            glassOpacity = snapshot.config.glassOpacity,
            dominantArtworkArgb = artDominant,
            serviceAccentArgb = if (media.hasActiveMedia) media.service.accentColor.hashCode() else null,
        )

        if (capsuleIds.isNotEmpty()) {
            updateIds(context, appWidgetManager, capsuleIds, WidgetFormat.CAPSULE_4X1, media, snapshot.config, copy, palette)
        }
        if (cardIds.isNotEmpty()) {
            updateIds(context, appWidgetManager, cardIds, WidgetFormat.CARD_4X2, media, snapshot.config, copy, palette)
        }
        if (squareIds.isNotEmpty()) {
            updateIds(context, appWidgetManager, squareIds, WidgetFormat.SQUARE_2X2, media, snapshot.config, copy, palette)
        }
        if (miniIds.isNotEmpty()) {
            updateIds(context, appWidgetManager, miniIds, WidgetFormat.MINI_2X1, media, snapshot.config, copy, palette)
        }
    }

    fun updateWidgetsForProvider(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        defaultFormat: WidgetFormat,
    ) {
        if (appWidgetIds.isEmpty()) return
        val media = MediaRepository.mediaState.value
        val snapshot = readSnapshot(context)
        val copy = WidgetRenderModel.resolveDisplayCopy(
            media = media,
            config = snapshot.config,
            lastHistoryTrack = snapshot.lastHistoryTrack,
            preferredPackage = snapshot.preferredPkg,
        )
        val palette = WidgetRenderModel.resolvePalette(
            style = snapshot.config.style,
            glassOpacity = snapshot.config.glassOpacity,
            dominantArtworkArgb = extractDominantColorCached(media),
            serviceAccentArgb = null,
        )
        updateIds(context, appWidgetManager, appWidgetIds, defaultFormat, media, snapshot.config, copy, palette)
    }

    fun publishGeneratedPreviewsIfSupported(context: Context) {
        if (!AndroidVersionCompat.supportsGeneratedWidgetPreview()) return
        val now = System.currentTimeMillis()
        // AppWidgetManager.setWidgetPreview is rate-limited by the OS (~2/hour per provider)
        if (now - lastPreviewPublishMs < 30 * 60_000L) return
        lastPreviewPublishMs = now

        val awm = AppWidgetManager.getInstance(context) ?: return
        val media = MediaRepository.mediaState.value
        val snapshot = readSnapshot(context)
        val copy = WidgetRenderModel.resolveDisplayCopy(
            media = media,
            config = snapshot.config,
            lastHistoryTrack = snapshot.lastHistoryTrack,
            preferredPackage = snapshot.preferredPkg,
        )
        val palette = WidgetRenderModel.resolvePalette(
            style = snapshot.config.style,
            glassOpacity = snapshot.config.glassOpacity,
            dominantArtworkArgb = extractDominantColorCached(media),
            serviceAccentArgb = null,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val pairs = listOf(
                ComponentName(context, MagicWidgetCapsuleProvider::class.java) to WidgetFormat.CAPSULE_4X1,
                ComponentName(context, MagicWidgetCardProvider::class.java) to WidgetFormat.CARD_4X2,
                ComponentName(context, MagicWidgetSquareProvider::class.java) to WidgetFormat.SQUARE_2X2,
                ComponentName(context, MagicWidgetMiniProvider::class.java) to WidgetFormat.MINI_2X1,
            )
            pairs.forEach { (component, fmt) ->
                runCatching {
                    val rv = buildSingleFormatViews(context, fmt, media, snapshot.config, copy, palette)
                    awm.setWidgetPreview(component, AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN, rv)
                }
            }
        }
    }

    private fun updateIds(
        context: Context,
        awm: AppWidgetManager,
        ids: IntArray,
        defaultFormat: WidgetFormat,
        media: UnifiedMediaState,
        config: WidgetConfig,
        copy: WidgetDisplayCopy,
        palette: WidgetPaletteArgb,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val sizeMap = mapOf(
                SizeF(110f, 64f) to buildSingleFormatViews(context, WidgetFormat.MINI_2X1, media, config, copy, palette),
                SizeF(165f, 150f) to buildSingleFormatViews(context, WidgetFormat.SQUARE_2X2, media, config, copy, palette),
                SizeF(250f, 70f) to buildSingleFormatViews(context, WidgetFormat.CAPSULE_4X1, media, config, copy, palette),
                SizeF(260f, 150f) to buildSingleFormatViews(context, WidgetFormat.CARD_4X2, media, config, copy, palette),
            )
            val responsiveViews = RemoteViews(sizeMap)
            awm.updateAppWidget(ids, responsiveViews)
        } else {
            ids.forEach { widgetId ->
                val opts = awm.getAppWidgetOptions(widgetId)
                val minW = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
                val minH = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
                val resolvedFmt = WidgetRenderModel.resolveFormatForSizeDp(minW, minH, defaultFormat)
                val rv = buildSingleFormatViews(context, resolvedFmt, media, config, copy, palette)
                awm.updateAppWidget(widgetId, rv)
            }
        }
    }

    private fun buildSingleFormatViews(
        context: Context,
        format: WidgetFormat,
        media: UnifiedMediaState,
        config: WidgetConfig,
        copy: WidgetDisplayCopy,
        palette: WidgetPaletteArgb,
    ): RemoteViews = when (format) {
        WidgetFormat.CAPSULE_4X1 -> buildCapsuleViews(context, media, config, copy, palette)
        WidgetFormat.CARD_4X2 -> buildCardViews(context, media, config, copy, palette)
        WidgetFormat.SQUARE_2X2 -> buildSquareViews(context, media, config, copy, palette)
        WidgetFormat.MINI_2X1 -> buildMiniViews(context, media, config, copy, palette)
    }

    private fun buildCapsuleViews(
        context: Context,
        media: UnifiedMediaState,
        config: WidgetConfig,
        copy: WidgetDisplayCopy,
        palette: WidgetPaletteArgb,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_m3_capsule)
        views.setImageViewBitmap(
            R.id.widget_bg_image,
            renderWidgetBackgroundBitmap(360, 110, config.cornerRadiusDp.toFloat(), palette),
        )

        views.setTextViewText(R.id.widget_track_title, copy.title)
        views.setTextViewText(R.id.widget_track_artist, copy.subtitle)
        views.setTextColor(R.id.widget_track_title, palette.titleArgb)
        views.setTextColor(R.id.widget_track_artist, palette.subtitleArgb)
        views.setTextColor(R.id.widget_service_badge, palette.accentArgb)

        if (config.showServiceBadge) {
            views.setViewVisibility(R.id.widget_service_badge, View.VISIBLE)
            views.setTextViewText(R.id.widget_service_badge, copy.badgeText)
        } else {
            views.setViewVisibility(R.id.widget_service_badge, View.GONE)
        }

        val artCornerPx = (config.cornerRadiusDp * 0.8f).coerceIn(12f, 28f)
        views.setImageViewBitmap(R.id.widget_album_art, prepareRoundedArtworkCached(context, media, 132, artCornerPx))

        views.setImageViewResource(
            R.id.widget_btn_play_pause,
            if (media.isPlaying) R.drawable.ic_magic_pause else R.drawable.ic_magic_play,
        )
        views.setInt(R.id.widget_btn_play_pause, "setColorFilter", palette.playBtnIconArgb)
        views.setInt(R.id.widget_btn_prev, "setColorFilter", palette.titleArgb)
        views.setInt(R.id.widget_btn_next, "setColorFilter", palette.titleArgb)
        views.setContentDescription(
            R.id.widget_btn_play_pause,
            context.getString(R.string.action_play_pause),
        )

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
        views.setOnClickPendingIntent(R.id.widget_album_art, openAppPendingIntent(context, 14))
        views.setOnClickPendingIntent(R.id.widget_track_info_container, openAppPendingIntent(context, 15))
        return views
    }

    private fun buildCardViews(
        context: Context,
        media: UnifiedMediaState,
        config: WidgetConfig,
        copy: WidgetDisplayCopy,
        palette: WidgetPaletteArgb,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_m3_card)
        views.setImageViewBitmap(
            R.id.widget_bg_image,
            renderWidgetBackgroundBitmap(360, 210, config.cornerRadiusDp.toFloat(), palette),
        )

        views.setTextViewText(R.id.widget_card_title, copy.title)
        views.setTextViewText(R.id.widget_card_artist, copy.subtitle)
        views.setTextColor(R.id.widget_card_title, palette.titleArgb)
        views.setTextColor(R.id.widget_card_artist, palette.subtitleArgb)
        views.setTextColor(R.id.widget_card_service_pill, palette.badgeTextArgb)

        if (config.showServiceBadge) {
            views.setViewVisibility(R.id.widget_card_service_pill, View.VISIBLE)
            views.setTextViewText(R.id.widget_card_service_pill, copy.badgeText)
        } else {
            views.setViewVisibility(R.id.widget_card_service_pill, View.GONE)
        }

        val artCornerPx = (config.cornerRadiusDp * 0.9f).coerceIn(14f, 32f)
        views.setImageViewBitmap(R.id.widget_card_album_art, prepareRoundedArtworkCached(context, media, 156, artCornerPx))

        if (config.showProgressBar) {
            views.setViewVisibility(R.id.widget_card_progress, View.VISIBLE)
            val progressInt = if (media.hasActiveMedia) (media.progressFraction * 1000f).toInt().coerceIn(0, 1000) else 0
            views.setProgressBar(R.id.widget_card_progress, 1000, progressInt, false)
        } else {
            views.setViewVisibility(R.id.widget_card_progress, View.GONE)
        }

        views.setImageViewResource(
            R.id.widget_card_btn_play_pause,
            if (media.isPlaying) R.drawable.ic_magic_pause else R.drawable.ic_magic_play,
        )
        views.setImageViewResource(R.id.widget_card_btn_custom_left, iconResForCustomAction(config.leftCustomAction))
        views.setImageViewResource(R.id.widget_card_btn_custom_right, iconResForCustomAction(config.rightCustomAction))

        views.setContentDescription(R.id.widget_card_btn_custom_left, WidgetRenderModel.actionAccessibilityDescription(config.leftCustomAction))
        views.setContentDescription(R.id.widget_card_btn_custom_right, WidgetRenderModel.actionAccessibilityDescription(config.rightCustomAction))
        views.setContentDescription(R.id.widget_card_btn_play_pause, context.getString(R.string.action_play_pause))

        views.setInt(R.id.widget_card_btn_play_pause, "setColorFilter", palette.playBtnIconArgb)
        views.setInt(R.id.widget_card_btn_prev, "setColorFilter", palette.titleArgb)
        views.setInt(R.id.widget_card_btn_next, "setColorFilter", palette.titleArgb)
        views.setInt(R.id.widget_card_btn_custom_left, "setColorFilter", palette.subtitleArgb)
        views.setInt(R.id.widget_card_btn_custom_right, "setColorFilter", palette.subtitleArgb)
        views.setInt(R.id.widget_card_btn_source, "setColorFilter", palette.accentArgb)

        views.setOnClickPendingIntent(R.id.widget_card_btn_play_pause, actionPendingIntent(context, MagicWidgetActionReceiver.ACTION_PLAY_PAUSE, 21))
        views.setOnClickPendingIntent(R.id.widget_card_btn_prev, actionPendingIntent(context, MagicWidgetActionReceiver.ACTION_PREV, 22))
        views.setOnClickPendingIntent(R.id.widget_card_btn_next, actionPendingIntent(context, MagicWidgetActionReceiver.ACTION_NEXT, 23))
        views.setOnClickPendingIntent(R.id.widget_card_btn_custom_left, actionPendingIntent(context, MagicWidgetActionReceiver.ACTION_CUSTOM_LEFT, 24))
        views.setOnClickPendingIntent(R.id.widget_card_btn_custom_right, actionPendingIntent(context, MagicWidgetActionReceiver.ACTION_CUSTOM_RIGHT, 25))
        views.setOnClickPendingIntent(R.id.widget_card_btn_source, actionPendingIntent(context, MagicWidgetActionReceiver.ACTION_OPEN_SOURCE, 26))
        views.setOnClickPendingIntent(R.id.widget_card_album_art, openAppPendingIntent(context, 27))
        views.setOnClickPendingIntent(R.id.widget_card_info, openAppPendingIntent(context, 28))
        return views
    }

    private fun buildSquareViews(
        context: Context,
        media: UnifiedMediaState,
        config: WidgetConfig,
        copy: WidgetDisplayCopy,
        palette: WidgetPaletteArgb,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_m3_square)
        views.setImageViewBitmap(
            R.id.widget_bg_image,
            renderWidgetBackgroundBitmap(210, 210, config.cornerRadiusDp.toFloat(), palette),
        )

        views.setTextViewText(R.id.widget_square_title, copy.title)
        views.setTextViewText(R.id.widget_square_artist, copy.subtitle)
        views.setTextColor(R.id.widget_square_title, palette.titleArgb)
        views.setTextColor(R.id.widget_square_artist, palette.subtitleArgb)
        views.setTextColor(R.id.widget_square_service_badge, palette.accentArgb)

        if (config.showServiceBadge) {
            views.setViewVisibility(R.id.widget_square_service_badge, View.VISIBLE)
            views.setTextViewText(R.id.widget_square_service_badge, media.service.displayName.uppercase())
        } else {
            views.setViewVisibility(R.id.widget_square_service_badge, View.GONE)
        }

        views.setImageViewBitmap(R.id.widget_square_album_art, prepareRoundedArtworkCached(context, media, 144, 24f))

        if (config.showProgressBar) {
            views.setViewVisibility(R.id.widget_square_progress, View.VISIBLE)
            val progressInt = if (media.hasActiveMedia) (media.progressFraction * 1000f).toInt().coerceIn(0, 1000) else 0
            views.setProgressBar(R.id.widget_square_progress, 1000, progressInt, false)
        } else {
            views.setViewVisibility(R.id.widget_square_progress, View.GONE)
        }

        views.setImageViewResource(
            R.id.widget_square_btn_play_pause,
            if (media.isPlaying) R.drawable.ic_magic_pause else R.drawable.ic_magic_play,
        )
        views.setInt(R.id.widget_square_btn_play_pause, "setColorFilter", palette.playBtnIconArgb)
        views.setInt(R.id.widget_square_btn_prev, "setColorFilter", palette.titleArgb)
        views.setInt(R.id.widget_square_btn_next, "setColorFilter", palette.titleArgb)
        views.setInt(R.id.widget_square_btn_source, "setColorFilter", palette.accentArgb)

        views.setOnClickPendingIntent(R.id.widget_square_btn_play_pause, actionPendingIntent(context, MagicWidgetActionReceiver.ACTION_PLAY_PAUSE, 31))
        views.setOnClickPendingIntent(R.id.widget_square_btn_prev, actionPendingIntent(context, MagicWidgetActionReceiver.ACTION_PREV, 32))
        views.setOnClickPendingIntent(R.id.widget_square_btn_next, actionPendingIntent(context, MagicWidgetActionReceiver.ACTION_NEXT, 33))
        views.setOnClickPendingIntent(R.id.widget_square_btn_source, actionPendingIntent(context, MagicWidgetActionReceiver.ACTION_OPEN_SOURCE, 34))
        views.setOnClickPendingIntent(R.id.widget_square_album_art, openAppPendingIntent(context, 35))
        views.setOnClickPendingIntent(R.id.widget_square_info, openAppPendingIntent(context, 36))
        return views
    }

    private fun buildMiniViews(
        context: Context,
        media: UnifiedMediaState,
        config: WidgetConfig,
        copy: WidgetDisplayCopy,
        palette: WidgetPaletteArgb,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_m3_mini)
        views.setImageViewBitmap(
            R.id.widget_bg_image,
            renderWidgetBackgroundBitmap(210, 100, config.cornerRadiusDp.toFloat(), palette),
        )
        views.setTextViewText(R.id.widget_mini_title, copy.title)
        views.setTextViewText(R.id.widget_mini_artist, copy.subtitle)
        views.setTextColor(R.id.widget_mini_title, palette.titleArgb)
        views.setTextColor(R.id.widget_mini_artist, palette.subtitleArgb)
        views.setImageViewBitmap(R.id.widget_mini_album_art, prepareRoundedArtworkCached(context, media, 112, 18f))

        views.setImageViewResource(
            R.id.widget_mini_btn_play_pause,
            if (media.isPlaying) R.drawable.ic_magic_pause else R.drawable.ic_magic_play,
        )
        views.setInt(R.id.widget_mini_btn_play_pause, "setColorFilter", palette.playBtnIconArgb)

        views.setOnClickPendingIntent(R.id.widget_mini_btn_play_pause, actionPendingIntent(context, MagicWidgetActionReceiver.ACTION_PLAY_PAUSE, 41))
        views.setOnClickPendingIntent(R.id.widget_mini_album_art, openAppPendingIntent(context, 42))
        views.setOnClickPendingIntent(R.id.widget_mini_info, openAppPendingIntent(context, 43))
        return views
    }

    private fun renderWidgetBackgroundBitmap(
        widthPx: Int,
        heightPx: Int,
        radiusDp: Float,
        palette: WidgetPaletteArgb,
    ): Bitmap {
        val key = "${widthPx}x${heightPx}_r${radiusDp}_${palette.bgStartArgb}_${palette.bgEndArgb}_${palette.borderArgb}"
        backgroundCache.get(key)?.let { cached ->
            if (!cached.isRecycled) return cached
        }
        val radiusPx = (radiusDp * 2.2f).coerceIn(18f, (heightPx / 2f) - 2f)
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rect = RectF(1.5f, 1.5f, widthPx - 1.5f, heightPx - 1.5f)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                widthPx.toFloat(),
                heightPx.toFloat(),
                palette.bgStartArgb,
                palette.bgEndArgb,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(rect, radiusPx, radiusPx, fillPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = palette.borderArgb
        }
        canvas.drawRoundRect(rect, radiusPx, radiusPx, borderPaint)
        backgroundCache.put(key, bitmap)
        return bitmap
    }

    private fun prepareRoundedArtworkCached(
        context: Context,
        media: UnifiedMediaState,
        sizePx: Int,
        radiusPx: Float,
    ): Bitmap {
        val clampedSize = sizePx.coerceIn(64, 160)
        val artHash = media.albumArt?.let { "${it.width}x${it.height}_${media.trackKey}" } ?: "fallback"
        val key = "${artHash}_${clampedSize}_$radiusPx"
        artworkCache.get(key)?.let { cached ->
            if (!cached.isRecycled) return cached
        }

        val raw = media.albumArt ?: BitmapFactory.decodeResource(context.resources, R.drawable.img_magic_album_art)
        val scaled = Bitmap.createScaledBitmap(raw, clampedSize, clampedSize, true)
        val output = Bitmap.createBitmap(clampedSize, clampedSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF(0f, 0f, clampedSize.toFloat(), clampedSize.toFloat())
        canvas.drawRoundRect(rect, radiusPx, radiusPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, null, rect, paint)
        artworkCache.put(key, output)
        return output
    }

    private fun extractDominantColorCached(media: UnifiedMediaState): Int? {
        val bmp = media.albumArt ?: return null
        val key = media.trackKey
        dominantColorCache.get(key)?.let { return it }
        val swatch = runCatching {
            val scaled = if (bmp.width > 96) Bitmap.createScaledBitmap(bmp, 72, 72, true) else bmp
            Palette.from(scaled).maximumColorCount(8).generate()
        }.getOrNull() ?: return null
        val color = swatch.dominantSwatch?.rgb ?: swatch.vibrantSwatch?.rgb ?: swatch.mutedSwatch?.rgb ?: return null
        dominantColorCache.put(key, color)
        return color
    }

    private fun buildSignatureWithoutProgress(
        media: UnifiedMediaState,
        config: WidgetConfig,
        preferredPkg: String,
    ): String {
        return "${media.trackKey}|${media.isPlaying}|${config.style}|${config.cornerRadiusDp}|" +
            "${(config.glassOpacity * 100).toInt()}|${config.showProgressBar}|${config.showServiceBadge}|" +
            "${config.showCompactHeader}|${config.leftCustomAction}|${config.rightCustomAction}|$preferredPkg"
    }

    private data class WidgetSnapshot(
        val config: WidgetConfig,
        val preferredPkg: String,
        val lastHistoryTrack: TrackHistoryEntry?,
    )

    private fun readSnapshot(context: Context): WidgetSnapshot = runBlocking {
        val repo = PreferencesRepository(context)
        val cfg = repo.getWidgetConfigSnapshot()
        val sessionPrefs = repo.getMediaSessionPreferencesSnapshot()
        val stats = repo.localStatsFlow.first()
        WidgetSnapshot(
            config = cfg,
            preferredPkg = sessionPrefs.preferredServicePackage,
            lastHistoryTrack = stats.recentTracks.firstOrNull(),
        )
    }

    private fun iconResForCustomAction(action: CustomControlAction): Int = when (action) {
        CustomControlAction.REWIND_15 -> R.drawable.ic_magic_replay15
        CustomControlAction.FORWARD_15 -> R.drawable.ic_magic_forward15
        CustomControlAction.SWITCH_SOURCE -> R.drawable.ic_magic_swap
        CustomControlAction.OPEN_PLAYER -> R.drawable.ic_magic_open
        CustomControlAction.MUTE_AUDIO -> R.drawable.ic_magic_mute
        CustomControlAction.LIKE_TRACK -> R.drawable.ic_magic_favorite
        CustomControlAction.SHUFFLE -> R.drawable.ic_magic_shuffle
        CustomControlAction.REPEAT -> R.drawable.ic_magic_repeat
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
}
