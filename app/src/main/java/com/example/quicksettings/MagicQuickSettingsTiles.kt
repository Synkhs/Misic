package com.example.quicksettings

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.R
import com.example.data.PreferencesRepository
import com.example.data.QsTileMode
import com.example.media.MediaHubController
import kotlinx.coroutines.runBlocking

object MagicTileSync {
    fun requestTileUpdates(context: Context) {
        runCatching {
            TileService.requestListeningState(
                context,
                ComponentName(context, MagicPlayPauseTileService::class.java),
            )
            TileService.requestListeningState(
                context,
                ComponentName(context, MagicNextTrackTileService::class.java),
            )
        }
    }
}

class MagicPlayPauseTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        MediaHubController.init(applicationContext)
        syncTile()
    }

    override fun onClick() {
        super.onClick()
        MediaHubController.init(applicationContext)
        val cfg = runBlocking { PreferencesRepository(applicationContext).getLockConfigSnapshot() }
        when (cfg.qsTileMode) {
            QsTileMode.PLAY_PAUSE -> {
                MediaHubController.playPause()
                syncTile()
            }
            QsTileMode.SWITCH_SOURCE -> {
                MediaHubController.rotateActiveSource()
                syncTile()
            }
            QsTileMode.OPEN_DECK -> {
                openLockDeck()
            }
        }
    }

    private fun openLockDeck() {
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

    private fun syncTile() {
        val tile = qsTile ?: return
        val media = MediaHubController.mediaState.value
        tile.label = if (media.hasActiveMedia) media.service.displayName else getString(R.string.tile_play_pause_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = media.title
        }
        tile.state = if (media.isPlaying) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(
            this,
            if (media.isPlaying) R.drawable.ic_magic_pause else R.drawable.ic_magic_play,
        )
        tile.contentDescription = "${tile.label}: ${media.title}"
        tile.updateTile()
    }
}

class MagicNextTrackTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        MediaHubController.init(applicationContext)
        syncTile()
    }

    override fun onClick() {
        super.onClick()
        MediaHubController.init(applicationContext)
        MediaHubController.next()
        syncTile()
    }

    private fun syncTile() {
        val tile = qsTile ?: return
        val media = MediaHubController.mediaState.value
        tile.label = getString(R.string.tile_next_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (media.hasActiveMedia) media.title else "Spotify / YT / Amazon"
        }
        tile.state = if (media.hasActiveMedia) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, R.drawable.ic_magic_next)
        tile.contentDescription = "Siguiente pista: ${media.title}"
        tile.updateTile()
    }
}
