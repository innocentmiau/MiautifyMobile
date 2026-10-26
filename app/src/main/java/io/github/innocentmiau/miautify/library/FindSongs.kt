package io.github.innocentmiau.miautify.library

/** One file or folder inside a folder, as the Storage Access Framework lists it. */
data class DocumentEntry(
    val id: String,
    val name: String,
    val isFolder: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0
)

/** The order of the song list: by folder, then by name, ignoring case. */
val songFileOrder: Comparator<SongFile> =
    compareBy(String.CASE_INSENSITIVE_ORDER, SongFile::folder)
        .thenBy(String.CASE_INSENSITIVE_ORDER, SongFile::name)

/** Names of a folder's cover image, the first found winning, like on the desktop. */
private val folderImageNames = listOf("cover", "folder", "front", "album")
private val imageExtensions = setOf("jpg", "jpeg", "png")

/** How good [name] is as a folder's cover image (0 is best), or null if it isn't one. */
fun folderImageRank(name: String): Int? {
    val extension = name.substringAfterLast('.', "").lowercase()
    if (extension !in imageExtensions) return null
    val base = name.substringBeforeLast('.').lowercase()
    return folderImageNames.indexOf(base).takeIf { it >= 0 }
}

/**
 * Walks every folder under [rootId] and returns the mp3 files in [songFileOrder], each with the
 * cover image of its folder if there is one.
 *
 * [children] lists what is directly inside one folder. It is a parameter so the walk can be
 * tested without a phone.
 */
fun findSongs(rootId: String, children: (folderId: String) -> List<DocumentEntry>): List<SongFile> {
    val songs = mutableListOf<SongFile>()
    // Folders still to read, with their path from the root. A list instead of recursion,
    // so a very deep folder tree can't overflow the stack.
    val pending = ArrayDeque(listOf(rootId to ""))

    while (pending.isNotEmpty()) {
        val (folderId, path) = pending.removeLast()
        val firstSongHere = songs.size
        var cover: DocumentEntry? = null
        var coverRank = Int.MAX_VALUE

        for (entry in children(folderId)) {
            when {
                entry.isFolder -> pending.addLast(entry.id to joinPath(path, entry.name))
                entry.name.endsWith(".mp3", ignoreCase = true) ->
                    songs += SongFile(entry.id, entry.name, path, entry.size, entry.lastModified)
                else -> folderImageRank(entry.name)?.takeIf { it < coverRank }?.let {
                    cover = entry
                    coverRank = it
                }
            }
        }

        // The image can come before or after the songs in the listing, so it is set at the end.
        cover?.let { image ->
            for (i in firstSongHere until songs.size) {
                songs[i] = songs[i].copy(folderCoverId = image.id)
            }
        }
    }

    return songs.sortedWith(songFileOrder)
}

private fun joinPath(parent: String, name: String): String =
    if (parent.isEmpty()) name else "$parent/$name"
