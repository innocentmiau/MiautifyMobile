package io.github.innocentmiau.miautify.playback

import android.content.ComponentName
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import io.github.innocentmiau.miautify.library.Cover
import io.github.innocentmiau.miautify.library.Covers
import io.github.innocentmiau.miautify.library.MusicFolder
import io.github.innocentmiau.miautify.library.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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
    val durationMs: Long?,
    val shuffle: Boolean = false,
    /** One of Player.REPEAT_MODE_OFF, REPEAT_MODE_ALL or REPEAT_MODE_ONE. */
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    /** True once the player has read a cover from the file, a moment after the song starts. */
    val hasArtwork: Boolean = false
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
        durationMs = duration.takeIf { it != C.TIME_UNSET },
        shuffle = shuffleModeEnabled,
        repeatMode = repeatMode,
        hasArtwork = mediaMetadata.artworkData != null || mediaMetadata.artworkUri != null
    )
}

/**
 * The playing song's cover as a thumbnail: the picture the player already read from the file
 * for the notification, so the file isn't read twice, or else the folder's cover image.
 */
@Composable
fun rememberArtwork(player: Player?, nowPlaying: NowPlaying?): Cover {
    val context = LocalContext.current
    val artwork by produceState<Cover>(Cover.Loading, player, nowPlaying?.songId, nowPlaying?.hasArtwork) {
        // Read on the main thread like every player call, the rest on a background thread.
        val metadata = player?.mediaMetadata ?: return@produceState
        val embedded = metadata.artworkData
        val folderImage = metadata.artworkUri
        value = withContext(Dispatchers.IO) {
            val bytes = embedded ?: folderImage?.let { uri ->
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            }
            bytes?.let { Covers.thumbnail(it)?.asImageBitmap() }?.let(Cover::Loaded) ?: Cover.None
        }
    }
    return artwork
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

/**
 * Keeps the playing song going, without a pause, and makes [songs] the rest of the queue: see
 * [aroundPlaying] for where they go. Does nothing when nothing is loaded.
 */
fun Player.continueWith(folder: Uri, songs: List<Song>) {
    val playingId = currentMediaItem?.mediaId ?: return
    val (before, after) = aroundPlaying(songs, playingId)
    // Two replacements around the playing song, which itself is never touched, so it plays on.
    // After first: replacing what is before moves the playing song's index.
    val current = currentMediaItemIndex
    replaceMediaItems(current + 1, mediaItemCount, after.map { it.toMediaItem(folder) })
    replaceMediaItems(0, current, before.map { it.toMediaItem(folder) })
}

/**
 * What goes before and after the playing song when [songs] continue the queue. When the song
 * is in [songs] it keeps its place there, so the songs after it come next. When it isn't, all of
 * [songs] come after it, from the start.
 */
fun aroundPlaying(songs: List<Song>, playingId: String): Pair<List<Song>, List<Song>> {
    val at = songs.indexOfFirst { it.file.documentId == playingId }
    return if (at < 0) emptyList<Song>() to songs else songs.subList(0, at) to songs.subList(at + 1, songs.size)
}

private fun Song.toMediaItem(folder: Uri): MediaItem =
    MediaItem.Builder()
        .setMediaId(file.documentId)
        .setUri(MusicFolder.fileUri(folder, file))
        // Shown in the notification and on the lock screen. The player reads an embedded cover
        // from the file itself and prefers it; the folder's cover image is the fallback.
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(tags?.artist)
                .setAlbumTitle(tags?.album)
                .setArtworkUri(file.folderCoverId?.let { MusicFolder.documentUri(folder, it) })
                .build()
        )
        .build()
