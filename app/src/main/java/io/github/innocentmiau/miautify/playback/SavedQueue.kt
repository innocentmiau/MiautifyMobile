package io.github.innocentmiau.miautify.playback

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** The queue as saved between launches: the songs, which one was playing, and where in it. */
@Serializable
data class SavedQueue(
    val songs: List<SavedSong>,
    val index: Int,
    val positionMs: Long
)

/** One song of the saved queue, with what the notification needs before any scan. */
@Serializable
data class SavedSong(
    val id: String,
    val uri: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    /** The folder's cover image, if any. Missing in queues saved before covers, which reads as null. */
    val artworkUri: String? = null
)

// Ignores fields a newer version of the app may add, instead of failing on them.
private val json = Json { ignoreUnknownKeys = true }

fun encodeQueue(queue: SavedQueue): String = json.encodeToString(queue)

/** The saved queue, or null when [text] is empty, damaged, or has no songs. */
fun decodeQueue(text: String): SavedQueue? {
    val queue = try {
        json.decodeFromString<SavedQueue>(text)
    } catch (_: SerializationException) {
        return null
    } catch (_: IllegalArgumentException) {
        return null
    }
    if (queue.songs.isEmpty()) return null
    return queue.copy(
        index = queue.index.coerceIn(queue.songs.indices),
        positionMs = queue.positionMs.coerceAtLeast(0)
    )
}
