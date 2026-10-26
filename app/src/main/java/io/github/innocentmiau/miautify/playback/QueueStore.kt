package io.github.innocentmiau.miautify.playback

import android.content.Context
import android.util.AtomicFile
import androidx.core.net.toUri
import androidx.core.util.readText
import androidx.core.util.writeText
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import io.github.innocentmiau.miautify.library.MusicFolder
import java.io.File

/** Saves the player's queue to a file in the app's private storage, and loads it back. */
class QueueStore(private val context: Context) {
    // AtomicFile writes a new copy and swaps it in, so being killed mid-write can't leave half a file.
    private val file = AtomicFile(File(context.filesDir, "queue.json"))

    /** What to save from [player]. Call on the main thread, like every player call. */
    fun snapshot(player: Player): SavedQueue? {
        if (player.mediaItemCount == 0) return null
        val songs = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).toSavedSong() }
        return SavedQueue(songs, player.currentMediaItemIndex, player.currentPosition)
    }

    /** Writes [queue], or deletes the file when it is null. Slow: call off the main thread. */
    fun write(queue: SavedQueue?) {
        if (queue == null) {
            file.delete()
        } else {
            file.writeText(encodeQueue(queue))
        }
    }

    /** Loads the saved queue into [player], paused, unless the app lost access to its folder. */
    fun restore(player: Player) {
        val queue = runCatching { decodeQueue(file.readText()) }.getOrNull() ?: return
        val firstUri = queue.songs.first().uri.toUri()
        if (!MusicFolder.hasAccess(context, MusicFolder.folderOf(firstUri))) return

        player.setMediaItems(queue.songs.map { it.toMediaItem() }, queue.index, queue.positionMs)
        player.prepare()
    }
}

private fun MediaItem.toSavedSong() = SavedSong(
    id = mediaId,
    uri = localConfiguration?.uri?.toString().orEmpty(),
    title = mediaMetadata.title?.toString().orEmpty(),
    artist = mediaMetadata.artist?.toString(),
    album = mediaMetadata.albumTitle?.toString()
)

private fun SavedSong.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .build()
        )
        .build()
