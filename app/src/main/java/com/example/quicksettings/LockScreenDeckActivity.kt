package com.example.quicksettings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CustomControlAction
import com.example.data.LockAndQsConfig
import com.example.data.PreferencesRepository
import com.example.data.UnifiedMediaState
import com.example.media.MediaHubController
import com.example.ui.theme.HonorCobalt
import com.example.ui.theme.HonorCyan
import com.example.ui.theme.MagicObsidian
import com.example.ui.theme.MyApplicationTheme

class LockScreenDeckActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MediaHubController.init(applicationContext)
        val prefs = PreferencesRepository(applicationContext)

        setContent {
            val media by MediaHubController.mediaState.collectAsState()
            val lockConfig by prefs.lockAndQsConfigFlow.collectAsState(initial = LockAndQsConfig())

            MyApplicationTheme(darkTheme = true) {
                LockScreenDeckContent(
                    media = media,
                    lockConfig = lockConfig,
                    onPlayPause = { MediaHubController.playPause() },
                    onNext = { MediaHubController.next() },
                    onPrevious = { MediaHubController.previous() },
                    onSeek = { MediaHubController.seekTo(it) },
                    onCustomAction = { MediaHubController.executeCustomAction(it) },
                    onOpenSource = { MediaHubController.openActiveOrPreferredApp(this) },
                    onClose = { finish() },
                )
            }
        }
    }
}

@Composable
private fun LockScreenDeckContent(
    media: UnifiedMediaState,
    lockConfig: LockAndQsConfig,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onCustomAction: (CustomControlAction) -> Unit,
    onOpenSource: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MagicObsidian),
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_magicos_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.5f,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x99060A14),
                            Color(0xE6060A14),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .widthIn(max = 540.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xCC0D1628),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x5500D2FF)),
                ) {
                    Text(
                        text = "MAGIC CAPSULE • ${media.service.displayName.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = HonorCyan,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("lock_deck_close"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cerrar panel de bloqueo",
                        tint = Color.White,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .border(1.5.dp, Color(0x44FFFFFF), RoundedCornerShape(32.dp))
                        .clickable { onOpenSource() },
                ) {
                    if (media.albumArt != null) {
                        Image(
                            bitmap = media.albumArt.asImageBitmap(),
                            contentDescription = "Carátula del álbum",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_magic_album_art),
                            contentDescription = "Carátula MagicOS",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = media.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${media.artist} • ${media.album}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Slider(
                    value = media.progressFraction,
                    onValueChange = { frac ->
                        if (media.durationMs > 0L) {
                            onSeek((frac * media.durationMs).toLong())
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = HonorCyan,
                        activeTrackColor = HonorCyan,
                        inactiveTrackColor = Color(0x33FFFFFF),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Surface(
                shape = RoundedCornerShape(30.dp),
                color = Color(0xCC111B30),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { onCustomAction(lockConfig.lockScreenAction1) },
                        modifier = Modifier.testTag("lock_deck_custom_1"),
                    ) {
                        Icon(
                            imageVector = iconForCustomAction(lockConfig.lockScreenAction1),
                            contentDescription = lockConfig.lockScreenAction1.label,
                            tint = Color(0xFF94A3B8),
                        )
                    }

                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier.testTag("lock_deck_prev"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Pista anterior",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(HonorCyan, HonorCobalt)))
                            .clickable(onClick = onPlayPause)
                            .testTag("lock_deck_play_pause"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Reproducir o pausar",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp),
                        )
                    }

                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.testTag("lock_deck_next"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Siguiente pista",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp),
                        )
                    }

                    IconButton(
                        onClick = { onCustomAction(lockConfig.lockScreenAction2) },
                        modifier = Modifier.testTag("lock_deck_custom_2"),
                    ) {
                        Icon(
                            imageVector = iconForCustomAction(lockConfig.lockScreenAction2),
                            contentDescription = lockConfig.lockScreenAction2.label,
                            tint = Color(0xFF94A3B8),
                        )
                    }
                }
            }
        }
    }
}

private fun iconForCustomAction(action: CustomControlAction): ImageVector = when (action) {
    CustomControlAction.REWIND_15 -> Icons.Rounded.Replay10
    CustomControlAction.FORWARD_15 -> Icons.Rounded.Forward10
    CustomControlAction.SWITCH_SOURCE -> Icons.Rounded.SwapHoriz
    CustomControlAction.OPEN_PLAYER -> Icons.AutoMirrored.Rounded.OpenInNew
    CustomControlAction.MUTE_AUDIO -> Icons.AutoMirrored.Rounded.VolumeOff
}
