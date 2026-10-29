package io.github.innocentmiau.miautify.library

/** What the file's tags say. Each field is null when the tag is missing or empty. */
data class SongTags(
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationMs: Long?,
    /** The album's own artist (ID3 TPE2): one name for an OST whose tracks have many artists. */
    val albumArtist: String? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null
)

/**
 * The first number of a track or disc tag: "3/12" (track 3 of 12) or "3" give 3. Null when
 * the tag is missing or doesn't start with a number.
 */
fun parseTagNumber(text: String?): Int? =
    text?.substringBefore('/')?.trim()?.toIntOrNull()

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

/**
 * A total length, like an album's, rounded to the nearest minute and split into hours and
 * minutes, for "1 h 12 min". Any length under half a minute still shows as 1 minute.
 */
fun hoursAndMinutes(ms: Long): Pair<Long, Long> {
    val minutes = ((ms + 30_000) / 60_000).coerceAtLeast(if (ms > 0) 1 else 0)
    return minutes / 60 to minutes % 60
}
