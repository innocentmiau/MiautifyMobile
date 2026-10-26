package io.github.innocentmiau.miautify.library

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

/** Reads tags with Android's built-in MediaMetadataRetriever. */
object TagReader {
    /** The tags of [file], or null when it can't be opened (not a real mp3, or deleted meanwhile). Slow: call off the main thread. */
    fun read(context: Context, file: Uri): SongTags? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, file)
            SongTags(
                title = retriever.text(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = retriever.text(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = retriever.text(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                durationMs = retriever.text(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            )
        } catch (_: RuntimeException) {
            // setDataSource throws IllegalArgumentException or RuntimeException for files it can't read.
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun MediaMetadataRetriever.text(key: Int): String? =
        extractMetadata(key)?.trim()?.takeIf { it.isNotEmpty() }
}
