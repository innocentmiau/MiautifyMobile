package io.github.innocentmiau.miautify.library

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

/** Reads tags with Android's built-in MediaMetadataRetriever. */
object TagReader {
    /**
     * Raised whenever [read] starts reading more tags. Cached songs read by an older version are
     * read again in the background, so they get the new tags too.
     * 1: title, artist, album, duration. 2: album artist, track and disc numbers.
     */
    const val VERSION = 2

    /** The tags of [file], or null when it can't be opened (not a real mp3, or deleted meanwhile). Slow: call off the main thread. */
    fun read(context: Context, file: Uri): SongTags? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, file)
            SongTags(
                title = retriever.text(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = retriever.text(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = retriever.text(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                durationMs = retriever.text(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                albumArtist = retriever.text(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                trackNumber = parseTagNumber(retriever.text(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)),
                discNumber = parseTagNumber(retriever.text(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER))
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
