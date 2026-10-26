package io.github.innocentmiau.miautify.library

/**
 * An mp3 file found in the music folder.
 *
 * @property documentId The file's id in the Storage Access Framework, used to open it.
 * @property name The file name, with its extension.
 * @property folder The path of its folder from the music folder, like "Album/Disc 1", or "" at the top.
 */
data class SongFile(
    val documentId: String,
    val name: String,
    val folder: String
)
