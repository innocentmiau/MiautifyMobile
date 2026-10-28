package io.github.innocentmiau.miautify.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import io.github.innocentmiau.miautify.data.BrokenFileDao
import io.github.innocentmiau.miautify.data.BrokenFileEntity
import io.github.innocentmiau.miautify.library.MusicFolder
import io.github.innocentmiau.miautify.library.shouldSkipAfterError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * When a file can't play (damaged, not really an mp3, or deleted), records it as broken and
 * moves on to the next song, so the music doesn't stop. A broken file that later plays is
 * forgotten again. Lives in the playback service, so it works with the app closed too.
 */
class SkipBrokenFiles(
    private val context: Context,
    private val player: Player,
    private val brokenFiles: BrokenFileDao,
    // Main thread, like the player. The file and database work moves off it.
    private val scope: CoroutineScope
) : Player.Listener {
    private var failuresInARow = 0

    init {
        player.addListener(this)
    }

    override fun onPlayerError(error: PlaybackException) {
        val item = player.currentMediaItem ?: return
        val uri = item.localConfiguration?.uri
        if (uri != null) record(item.mediaId, uri)

        failuresInARow++
        if (shouldSkipAfterError(failuresInARow, player.mediaItemCount, player.hasNextMediaItem())) {
            // After an error the player stops: move on, then prepare it again to play.
            player.seekToNextMediaItem()
            player.prepare()
            player.play()
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (!isPlaying) return
        failuresInARow = 0
        val songId = player.currentMediaItem?.mediaId ?: return
        scope.launch { brokenFiles.forget(songId) }
    }

    private fun record(songId: String, uri: Uri) {
        scope.launch {
            // Size and date as they are now, so the file counts as fixed once it changes.
            // A deleted file has none: nothing to record, it just disappears on the next scan.
            val stamp = withContext(Dispatchers.IO) { MusicFolder.fileStamp(context, uri) } ?: return@launch
            brokenFiles.save(BrokenFileEntity(songId, stamp.size, stamp.lastModified))
        }
    }
}
