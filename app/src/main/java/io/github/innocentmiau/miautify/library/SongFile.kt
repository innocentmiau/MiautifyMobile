package io.github.innocentmiau.miautify.library

/**
 * An mp3 file found in the music folder.
 *
 * @property documentId The file's id in the Storage Access Framework, used to open it.
 * @property name The file name, with its extension.
 * @property folder The path of its folder from the music folder, like "Album/Disc 1", or "" at the top.
 * @property size The file size in bytes, 0 when unknown.
 * @property lastModified When the file last changed, in milliseconds since 1970, 0 when unknown.
 */
data class SongFile(
    val documentId: String,
    val name: String,
    val folder: String,
    val size: Long = 0,
    val lastModified: Long = 0
)
