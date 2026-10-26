package io.github.innocentmiau.miautify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.innocentmiau.miautify.playback.rememberNowPlaying
import io.github.innocentmiau.miautify.playback.rememberPlayer
import io.github.innocentmiau.miautify.ui.MusicFolderScreen
import io.github.innocentmiau.miautify.ui.PlayerBar
import io.github.innocentmiau.miautify.ui.theme.MiautifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (player != null && nowPlaying != null) {
                PlayerBar(
                    nowPlaying = nowPlaying,
                    onPrevious = { player.seekToPrevious() },
                    onPlayPause = { if (player.isPlaying) player.pause() else player.play() },
                    onNext = { player.seekToNext() }
                )
            }
        }
    ) { innerPadding ->
        MusicFolderScreen(
            player = player,
            playingSongId = nowPlaying?.songId,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}
