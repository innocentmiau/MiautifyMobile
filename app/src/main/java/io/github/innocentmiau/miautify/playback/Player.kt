package io.github.innocentmiau.miautify.playback

import android.content.ComponentName
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import io.github.innocentmiau.miautify.library.MusicFolder
import io.github.innocentmiau.miautify.library.Song
import kotlinx.coroutines.delay

/**
 * Connects to [PlaybackService] while the screen is shown. Null until connected (a moment
 * after start). The service keeps playing when this disconnects.
 */
@Composable
fun rememberPlayer(): Player? {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }

    DisposableEffect(Unit) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            { controller = runCatching { future.get() }.getOrNull() },
            ContextCompat.getMainExecutor(context)
        )
        onDispose {
            controller = null
            MediaController.releaseFuture(future)
        }
    }
    return controller
}

/**
 * What the screen shows about the player. [songId] is the playing file's document id.
 * [durationMs] is null until the player knows it, a moment after the song starts.
 */
data class NowPlaying(
    val songId: String,
    val title: String,
    val artist: String?,
    val isPlaying: Boolean,
    val durationMs: Long?
)

/** The player's current song, updated on every change; null when nothing is loaded. */
@Composable
fun rememberNowPlaying(player: Player?): NowPlaying? {
    var nowPlaying by remember(player) { mutableStateOf(player?.nowPlaying()) }

    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                nowPlaying = player.nowPlaying()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    return nowPlaying
}

private fun Player.nowPlaying(): NowPlaying? {
    val item = currentMediaItem ?: return null
    return NowPlaying(
        songId = item.mediaId,
        title = item.mediaMetadata.title?.toString().orEmpty(),
        artist = item.mediaMetadata.artist?.toString(),
        isPlaying = isPlaying,
        durationMs = duration.takeIf { it != C.TIME_UNSET }
    )
}

/**
 * The position in the current song in milliseconds. Read right away on seeks, song changes,
 * play and pause, and a few times per second while playing, since playing moves it without
 * any event. Read it only where it is shown, because it changes often.
 */
@Composable
fun rememberPositionMs(player: Player?): Long {
    var position by remember(player) { mutableLongStateOf(player?.currentPosition ?: 0L) }
    var playing by remember(player) { mutableStateOf(player?.isPlaying == true) }

    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                position = player.currentPosition
                playing = player.isPlaying
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Restarts when playing starts or stops, so it only loops while playing.
    LaunchedEffect(player, playing) {
        while (player != null && playing) {
            position = player.currentPosition
            delay(250)
        }
    }
    return position
}

/** Replaces the queue with [songs] and plays from [startIndex]. */
fun Player.playSongs(folder: Uri, songs: List<Song>, startIndex: Int) {
    setMediaItems(songs.map { it.toMediaItem(folder) }, startIndex, 0L)
    prepare()
    play()
}

private fun Song.toMediaItem(folder: Uri): MediaItem =
    MediaItem.Builder()
        .setMediaId(file.documentId)
        .setUri(MusicFolder.fileUri(folder, file))
        // Shown in the notification and on the lock screen. The cover is read from the file by the player.
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(tags?.artist)
                .setAlbumTitle(tags?.album)
                .build()
        )
        .build()
