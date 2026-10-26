package io.github.innocentmiau.miautify.library

/** Tags remembered from an earlier scan, with the size and date the file had then. */
data class CachedSong(
    val size: Long,
    val lastModified: Long,
    val tags: SongTags
)

/**
 * The scan's songs with the tags the cache already has, and which of them still need reading.
 *
 * @property songs Every file found, in the same order, with tags for those the cache knew.
 * @property toRead Indexes in [songs] of new or changed files, whose tags must be read.
 * @property removed Ids of cached files that are no longer in the folder.
 */
data class CacheMatch(
    val songs: List<Song>,
    val toRead: List<Int>,
    val removed: List<String>
)

/**
 * Matches the files found by a scan with the cache, keyed by document id. A cached file is
 * reused only if its size and date are unchanged, so an edited file has its tags read again.
 */
fun matchCache(files: List<SongFile>, cached: Map<String, CachedSong>): CacheMatch {
    val toRead = mutableListOf<Int>()
    val songs = files.mapIndexed { index, file ->
        val hit = cached[file.documentId]
            ?.takeIf { it.size == file.size && it.lastModified == file.lastModified }
        if (hit == null) toRead += index
        Song(file, hit?.tags)
    }
    val found = files.mapTo(HashSet()) { it.documentId }
    val removed = cached.keys.filter { it !in found }
    return CacheMatch(songs, toRead, removed)
}
