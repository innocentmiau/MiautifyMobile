package io.github.innocentmiau.miautify.library

/** A file that failed to play, with its size and date at the time, by document id. */
data class BrokenFile(val size: Long, val lastModified: Long)

/**
 * True when this song's file failed to play and hasn't changed since. A file that was fixed
 * (replaced or re-encoded) has another size or date, so it counts as playable again.
 */
fun Song.isBroken(broken: Map<String, BrokenFile>): Boolean {
    val failed = broken[file.documentId] ?: return false
    return failed.size == file.size && failed.lastModified == file.lastModified
}

/**
 * Whether to move on to the next song after a playback error. Not when there is none, and not
 * after as many failures in a row as there are songs: then every song failed, and with repeat
 * on, skipping would go round forever.
 */
fun shouldSkipAfterError(failuresInARow: Int, songCount: Int, hasNext: Boolean): Boolean =
    hasNext && failuresInARow < songCount
