package io.github.innocentmiau.miautify.library

/** One file or folder inside a folder, as the Storage Access Framework lists it. */
data class DocumentEntry(
    val id: String,
    val name: String,
    val isFolder: Boolean
)

/**
 * Walks every folder under [rootId] and returns the mp3 files, sorted by folder and then by name.
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
        for (entry in children(folderId)) {
            when {
                entry.isFolder -> pending.addLast(entry.id to joinPath(path, entry.name))
                entry.name.endsWith(".mp3", ignoreCase = true) -> songs += SongFile(entry.id, entry.name, path)
            }
        }
    }

    return songs.sortedWith(
        compareBy(String.CASE_INSENSITIVE_ORDER, SongFile::folder)
            .thenBy(String.CASE_INSENSITIVE_ORDER, SongFile::name)
    )
}

private fun joinPath(parent: String, name: String): String =
    if (parent.isEmpty()) name else "$parent/$name"
