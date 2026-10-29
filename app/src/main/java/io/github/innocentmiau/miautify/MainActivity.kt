package io.github.innocentmiau.miautify

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.innocentmiau.miautify.library.GroupKind
import io.github.innocentmiau.miautify.playback.nextRepeatMode
import io.github.innocentmiau.miautify.playback.rememberArtwork
import io.github.innocentmiau.miautify.playback.rememberNowPlaying
import io.github.innocentmiau.miautify.playback.rememberPlayer
import io.github.innocentmiau.miautify.playback.rememberPositionMs
import io.github.innocentmiau.miautify.ui.GroupScreen
import io.github.innocentmiau.miautify.ui.MusicFolderScreen
import io.github.innocentmiau.miautify.ui.PlayerBar
import io.github.innocentmiau.miautify.ui.SearchScreen
import io.github.innocentmiau.miautify.ui.theme.MiautifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark, so the status and navigation bar icons are always light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            MiautifyTheme {
                MiautifyApp()
            }
        }
    }
}

@Composable
fun MiautifyApp() {
    val player = rememberPlayer()
    val nowPlaying = rememberNowPlaying(player)
    // Which screen shows. Saveable, so a rotation keeps you where you were. An opened group
    // sits on top of browsing, and going back from it returns to browsing as you left it.
    var browsing by rememberSaveable { mutableStateOf(false) }
    var openKind by rememberSaveable { mutableStateOf<GroupKind?>(null) }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (player != null && nowPlaying != null) {
                // Read here and not above, so the 4 updates per second redraw only the bar.
                val positionMs = rememberPositionMs(player)
                val cover = rememberArtwork(player, nowPlaying)
                PlayerBar(
                    nowPlaying = nowPlaying,
                    cover = cover,
                    positionMs = positionMs,
                    onSeek = { player.seekTo(it) },
                    onPrevious = { player.seekToPrevious() },
                    onPlayPause = { if (player.isPlaying) player.pause() else player.play() },
                    onNext = { player.seekToNext() },
                    onShuffle = { player.shuffleModeEnabled = it },
                    onRepeat = { player.repeatMode = nextRepeatMode(player.repeatMode) }
                )
            }
        }
    ) { innerPadding ->
        // consumeWindowInsets: the padding just applied (status bar at the top, the player bar
        // at the bottom) counts as already handled, so a screen's imePadding() only adds the
        // part of the keyboard that rises above it, instead of the whole keyboard on top.
        val screenModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
        val kind = openKind
        val id = openId
        if (kind != null && id != null) {
            GroupScreen(
                kind = kind,
                id = id,
                player = player,
                playingSongId = nowPlaying?.songId,
                onBack = {
                    openKind = null
                    openId = null
                },
                modifier = screenModifier
            )
        } else if (browsing) {
            SearchScreen(
                player = player,
                playingSongId = nowPlaying?.songId,
                onOpenGroup = { groupKind, groupId ->
                    openKind = groupKind
                    openId = groupId
                },
                onClose = { browsing = false },
                modifier = screenModifier
            )
        } else {
            MusicFolderScreen(
                player = player,
                playingSongId = nowPlaying?.songId,
                onBrowse = { browsing = true },
                modifier = screenModifier
            )
        }
    }
}
