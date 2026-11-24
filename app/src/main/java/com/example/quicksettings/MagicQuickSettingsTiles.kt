package com.example.quicksettings

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.DrawableRes
import com.example.R
import com.example.core.compat.AndroidVersionCompat
import com.example.core.media.ListenerConnectionState
import com.example.core.media.MediaRepository
import com.example.core.media.SleepTimerMode
import com.example.core.media.SleepTimerState
import com.example.data.PreferencesRepository
import com.example.data.QsTileMode
import com.example.data.QuickTileType
import com.example.data.UnifiedMediaState
import kotlinx.coroutines.runBlocking

enum class TileIconKind {
    PLAY,
    PAUSE,
    MUSIC_NOTE,
    NEXT,
    PREVIOUS,
    SHUFFLE,
    REPEAT,
    FAVORITE,
    SLEEP_TIMER,
}

data class TileUiSpec(
    val label: String,
    val subtitle: String,
    val tileState: Int,
    val iconKind: TileIconKind,
    val contentDescription: String,
)

object TileStateCalculator {
    const val STATE_UNAVAILABLE = Tile.STATE_UNAVAILABLE
    const val STATE_INACTIVE = Tile.STATE_INACTIVE
    const val STATE_ACTIVE = Tile.STATE_ACTIVE

    fun compute(
        type: QuickTileType,
        media: UnifiedMediaState,
        listenerState: ListenerConnectionState,
        sleepTimer: SleepTimerState,
        qsTileMode: QsTileMode,
    ): TileUiSpec {
        val missingListener = listenerState == ListenerConnectionState.PERMISSION_MISSING && !media.hasActiveMedia

        if (missingListener && type != QuickTileType.SLEEP_TIMER) {
            return TileUiSpec(
                label = type.label,
                subtitle = "Falta permiso",
                tileState = STATE_UNAVAILABLE,
                iconKind = defaultIconFor(type),
                contentDescription = "${type.label}: Falta permiso de sesiones",
            )
        }

        if (!media.hasActiveMedia && type != QuickTileType.PLAY_PAUSE && type != QuickTileType.SLEEP_TIMER) {
            return TileUiSpec(
                label = type.label,
                subtitle = "Sin reproducción",
                tileState = STATE_UNAVAILABLE,
                iconKind = defaultIconFor(type),
                contentDescription = "${type.label}: Sin reproducción activa",
            )
        }

        return when (type) {
            QuickTileType.PLAY_PAUSE -> {
                val subtitle = when (qsTileMode) {
                    QsTileMode.OPEN_DECK -> media.title.takeIf { media.hasActiveMedia } ?: "Abrir panel"
                    QsTileMode.SWITCH_SOURCE -> "Tocar para cambiar app"
                    QsTileMode.PLAY_PAUSE -> if (media.hasActiveMedia) media.title else "Motor Local listo"
                }
                val icon = when {
                    !media.hasActiveMedia -> TileIconKind.MUSIC_NOTE
                    media.isPlaying -> TileIconKind.PAUSE
                    else -> TileIconKind.PLAY
                }
                TileUiSpec(
                    label = if (media.hasActiveMedia) media.service.displayName else type.label,
                    subtitle = subtitle,
                    tileState = if (media.isPlaying) STATE_ACTIVE else STATE_INACTIVE,
                    iconKind = icon,
                    contentDescription = "${type.label}: $subtitle",
                )
            }
            QuickTileType.NEXT -> TileUiSpec(
                label = type.label,
                subtitle = media.title,
                tileState = if (media.canSkipNext) STATE_INACTIVE else STATE_UNAVAILABLE,
                iconKind = TileIconKind.NEXT,
                contentDescription = "Siguiente pista: ${media.title}",
            )
            QuickTileType.PREVIOUS -> TileUiSpec(
                label = type.label,
                subtitle = media.title,
                tileState = if (media.canSkipPrevious) STATE_INACTIVE else STATE_UNAVAILABLE,
                iconKind = TileIconKind.PREVIOUS,
                contentDescription = "Pista anterior: ${media.title}",
            )
            QuickTileType.SHUFFLE -> TileUiSpec(
                label = type.label,
                subtitle = if (media.isShuffleEnabled) "Activado" else "Desactivado",
                tileState = when {
                    !media.canShuffle -> STATE_UNAVAILABLE
                    media.isShuffleEnabled -> STATE_ACTIVE
                    else -> STATE_INACTIVE
                },
                iconKind = TileIconKind.SHUFFLE,
                contentDescription = "Aleatorio: ${if (media.isShuffleEnabled) "Activado" else "Desactivado"}",
            )
            QuickTileType.REPEAT -> TileUiSpec(
                label = type.label,
                subtitle = if (media.isRepeatEnabled) "Repetir activo" else "Desactivado",
                tileState = when {
                    !media.canRepeat -> STATE_UNAVAILABLE
                    media.isRepeatEnabled -> STATE_ACTIVE
                    else -> STATE_INACTIVE
                },
                iconKind = TileIconKind.REPEAT,
                contentDescription = "Repetir: ${if (media.isRepeatEnabled) "Activado" else "Desactivado"}",
            )
            QuickTileType.FAVORITE -> TileUiSpec(
                label = type.label,
                subtitle = if (media.isFavorite) "En favoritas" else media.title,
                tileState = when {
                    !media.canFavorite -> STATE_UNAVAILABLE
                    media.isFavorite -> STATE_ACTIVE
                    else -> STATE_INACTIVE
                },
                iconKind = TileIconKind.FAVORITE,
                contentDescription = "Me gusta: ${if (media.isFavorite) "Marcado" else "No marcado"}",
            )
            QuickTileType.SLEEP_TIMER -> {
                val active = sleepTimer.isActive
                val sub = when (sleepTimer.mode) {
                    SleepTimerMode.COUNTDOWN -> "${(sleepTimer.remainingMs / 60_000L).coerceAtLeast(1)} min"
                    SleepTimerMode.END_OF_TRACK -> "Fin de pista"
                    SleepTimerMode.OFF -> "Apagado (15m)"
                }
                TileUiSpec(
                    label = type.label,
                    subtitle = sub,
                    tileState = if (active) STATE_ACTIVE else STATE_INACTIVE,
                    iconKind = TileIconKind.SLEEP_TIMER,
                    contentDescription = "Temporizador de apagado: $sub",
                )
            }
        }
    }

    private fun defaultIconFor(type: QuickTileType): TileIconKind = when (type) {
        QuickTileType.PLAY_PAUSE -> TileIconKind.MUSIC_NOTE
        QuickTileType.NEXT -> TileIconKind.NEXT
        QuickTileType.PREVIOUS -> TileIconKind.PREVIOUS
        QuickTileType.SHUFFLE -> TileIconKind.SHUFFLE
        QuickTileType.REPEAT -> TileIconKind.REPEAT
        QuickTileType.FAVORITE -> TileIconKind.FAVORITE
        QuickTileType.SLEEP_TIMER -> TileIconKind.SLEEP_TIMER
    }
}

object MagicTileSync {
    private val allServiceClasses = listOf(
        MagicPlayPauseTileService::class.java,
        MagicNextTrackTileService::class.java,
        MagicPreviousTrackTileService::class.java,
        MagicShuffleTileService::class.java,
        MagicRepeatTileService::class.java,
        MagicFavoriteTileService::class.java,
        MagicSleepTimerTileService::class.java,
    )

    fun componentFor(context: Context, type: QuickTileType): ComponentName {
        val cls = when (type) {
            QuickTileType.PLAY_PAUSE -> MagicPlayPauseTileService::class.java
            QuickTileType.NEXT -> MagicNextTrackTileService::class.java
            QuickTileType.PREVIOUS -> MagicPreviousTrackTileService::class.java
            QuickTileType.SHUFFLE -> MagicShuffleTileService::class.java
            QuickTileType.REPEAT -> MagicRepeatTileService::class.java
            QuickTileType.FAVORITE -> MagicFavoriteTileService::class.java
            QuickTileType.SLEEP_TIMER -> MagicSleepTimerTileService::class.java
        }
        return ComponentName(context, cls)
    }

    @DrawableRes
    fun iconResFor(type: QuickTileType): Int = when (type) {
        QuickTileType.PLAY_PAUSE -> R.drawable.ic_magic_music_note
        QuickTileType.NEXT -> R.drawable.ic_magic_next
        QuickTileType.PREVIOUS -> R.drawable.ic_magic_prev
        QuickTileType.SHUFFLE -> R.drawable.ic_magic_shuffle
        QuickTileType.REPEAT -> R.drawable.ic_magic_repeat
        QuickTileType.FAVORITE -> R.drawable.ic_magic_favorite
        QuickTileType.SLEEP_TIMER -> R.drawable.ic_magic_sleep_timer
    }

    fun requestTileUpdates(context: Context) {
        allServiceClasses.forEach { cls ->
            runCatching {
                TileService.requestListeningState(context, ComponentName(context, cls))
            }
        }
    }
}

abstract class BaseMagicTileService(
    protected val tileType: QuickTileType,
) : TileService() {

    override fun onTileAdded() {
        super.onTileAdded()
        MediaRepository.init(applicationContext)
        MediaRepository.setQuickTileAdded(tileType.key, true)
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        MediaRepository.init(applicationContext)
        MediaRepository.setQuickTileAdded(tileType.key, false)
    }

    override fun onStartListening() {
        super.onStartListening()
        MediaRepository.init(applicationContext)
        MediaRepository.setQuickTileAdded(tileType.key, true)
        syncTile()
    }

    protected fun syncTile() {
        val tile = qsTile ?: return
        val cfg = runBlocking { PreferencesRepository(applicationContext).getLockConfigSnapshot() }
        val media = MediaRepository.mediaState.value
        val player = MediaRepository.playerState.value
        val sleep = MediaRepository.sleepTimerState.value

        val spec = TileStateCalculator.compute(
            type = tileType,
            media = media,
            listenerState = player.listenerState,
            sleepTimer = sleep,
            qsTileMode = cfg.qsTileMode,
        )

        tile.label = spec.label
        if (AndroidVersionCompat.supportsTileSubtitle()) {
            tile.subtitle = spec.subtitle
        }
        tile.state = spec.tileState
        tile.icon = Icon.createWithResource(this, mapIconKind(spec.iconKind))
        tile.contentDescription = spec.contentDescription
        tile.updateTile()
    }

    protected fun runWithOptionalUnlock(requiresUnlock: Boolean, block: () -> Unit) {
        if (requiresUnlock && isLocked) {
            unlockAndRun {
                block()
                syncTile()
            }
        } else {
            block()
            syncTile()
        }
    }

    protected fun openLockDeck() {
        val intent = Intent(this, LockScreenDeckActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pi = PendingIntent.getActivity(
                this,
                301,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pi)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    protected fun showSessionSwitchDialog() {
        val sessions = MediaRepository.playerState.value.sessions
        val entries = mutableListOf<Pair<String, () -> Unit>>()
        sessions.forEach { s ->
            val mark = if (s.isSelected) " (Activa)" else ""
            entries.add(
                "${s.appName}$mark — ${s.title}" to {
                    MediaRepository.selectSession(s.packageName)
                    syncTile()
                },
            )
        }
        entries.add("Abrir panel en pantalla completa" to { openLockDeck() })

        val labels = entries.map { it.first }.toTypedArray()
        val dialog = AlertDialog.Builder(this)
            .setTitle("MagicMusic • Cambiar sesión")
            .setItems(labels) { _, which ->
                entries.getOrNull(which)?.second?.invoke()
            }
            .setNegativeButton("Cerrar", null)
            .create()
        runCatching { showDialog(dialog) }
    }

    @DrawableRes
    private fun mapIconKind(kind: TileIconKind): Int = when (kind) {
        TileIconKind.PLAY -> R.drawable.ic_magic_play
        TileIconKind.PAUSE -> R.drawable.ic_magic_pause
        TileIconKind.MUSIC_NOTE -> R.drawable.ic_magic_music_note
        TileIconKind.NEXT -> R.drawable.ic_magic_next
        TileIconKind.PREVIOUS -> R.drawable.ic_magic_prev
        TileIconKind.SHUFFLE -> R.drawable.ic_magic_shuffle
        TileIconKind.REPEAT -> R.drawable.ic_magic_repeat
        TileIconKind.FAVORITE -> R.drawable.ic_magic_favorite
        TileIconKind.SLEEP_TIMER -> R.drawable.ic_magic_sleep_timer
    }
}

class MagicPlayPauseTileService : BaseMagicTileService(QuickTileType.PLAY_PAUSE) {
    override fun onClick() {
        super.onClick()
        MediaRepository.init(applicationContext)
        val cfg = runBlocking { PreferencesRepository(applicationContext).getLockConfigSnapshot() }
        when (cfg.qsTileMode) {
            QsTileMode.PLAY_PAUSE -> {
                MediaRepository.playPause()
                syncTile()
            }
            QsTileMode.SWITCH_SOURCE -> {
                runWithOptionalUnlock(cfg.requireUnlockToSwitchSource) {
                    if (MediaRepository.playerState.value.sessions.size > 1) {
                        showSessionSwitchDialog()
                    } else {
                        MediaRepository.rotateActiveSource()
                    }
                }
            }
            QsTileMode.OPEN_DECK -> openLockDeck()
        }
    }
}

class MagicNextTrackTileService : BaseMagicTileService(QuickTileType.NEXT) {
    override fun onClick() {
        super.onClick()
        MediaRepository.init(applicationContext)
        MediaRepository.next()
        syncTile()
    }
}

class MagicPreviousTrackTileService : BaseMagicTileService(QuickTileType.PREVIOUS) {
    override fun onClick() {
        super.onClick()
        MediaRepository.init(applicationContext)
        MediaRepository.previous()
        syncTile()
    }
}

class MagicShuffleTileService : BaseMagicTileService(QuickTileType.SHUFFLE) {
    override fun onClick() {
        super.onClick()
        MediaRepository.init(applicationContext)
        val cfg = runBlocking { PreferencesRepository(applicationContext).getLockConfigSnapshot() }
        runWithOptionalUnlock(cfg.requireUnlockForCustomActions) {
            MediaRepository.toggleShuffle()
        }
    }
}

class MagicRepeatTileService : BaseMagicTileService(QuickTileType.REPEAT) {
    override fun onClick() {
        super.onClick()
        MediaRepository.init(applicationContext)
        val cfg = runBlocking { PreferencesRepository(applicationContext).getLockConfigSnapshot() }
        runWithOptionalUnlock(cfg.requireUnlockForCustomActions) {
            MediaRepository.toggleRepeat()
        }
    }
}

class MagicFavoriteTileService : BaseMagicTileService(QuickTileType.FAVORITE) {
    override fun onClick() {
        super.onClick()
        MediaRepository.init(applicationContext)
        val cfg = runBlocking { PreferencesRepository(applicationContext).getLockConfigSnapshot() }
        runWithOptionalUnlock(cfg.requireUnlockForCustomActions) {
            MediaRepository.toggleFavorite()
        }
    }
}

class MagicSleepTimerTileService : BaseMagicTileService(QuickTileType.SLEEP_TIMER) {
    override fun onClick() {
        super.onClick()
        MediaRepository.init(applicationContext)
        val sleep = MediaRepository.sleepTimerState.value
        when {
            !sleep.isActive -> MediaRepository.startSleepTimerCountdown(15, fadeOutEnabled = true)
            sleep.mode == SleepTimerMode.COUNTDOWN && sleep.selectedMinutes == 15 ->
                MediaRepository.startSleepTimerCountdown(30, sleep.fadeOutEnabled)
            else -> MediaRepository.cancelSleepTimer()
        }
        syncTile()
    }
}
