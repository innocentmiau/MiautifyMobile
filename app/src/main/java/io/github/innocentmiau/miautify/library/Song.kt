package io.github.innocentmiau.miautify.library

/** What the file's tags say. Each field is null when the tag is missing or empty. */
data class SongTags(
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationMs: Long?
)

/** A song in the list: the file, and its tags once they are read (null before, or if unreadable). */
data class Song(
    val file: SongFile,
    val tags: SongTags?
) {
    /** The title tag, or the file name without ".mp3" when there is none. */
    val title: String
        get() = tags?.title ?: file.name.substringBeforeLast('.')
}

/** Formats a duration like a player does: "3:07", or "1:02:03" from one hour up. */
fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = totalSeconds % 3600 / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
