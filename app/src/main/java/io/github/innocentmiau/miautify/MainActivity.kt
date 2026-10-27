package io.github.innocentmiau.miautify

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.innocentmiau.miautify.playback.nextRepeatMode
import io.github.innocentmiau.miautify.playback.rememberArtwork
import io.github.innocentmiau.miautify.playback.rememberNowPlaying
import io.github.innocentmiau.miautify.playback.rememberPlayer
import io.github.innocentmiau.miautify.playback.rememberPositionMs
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
    var searching by rememberSaveable { mutableStateOf(false) }

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
        val screenModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        // Two screens for now, so a flag is enough. Saveable: a rotation keeps you in search.
        if (searching) {
            SearchScreen(
                player = player,
                playingSongId = nowPlaying?.songId,
                onClose = { searching = false },
                modifier = screenModifier
            )
        } else {
            MusicFolderScreen(
                player = player,
                playingSongId = nowPlaying?.songId,
                onSearch = { searching = true },
                modifier = screenModifier
            )
        }
    }
}
