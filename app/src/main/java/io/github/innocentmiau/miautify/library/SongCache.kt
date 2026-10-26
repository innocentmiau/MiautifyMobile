package io.github.innocentmiau.miautify.library

/** What an earlier scan remembered about a file: its size, date and folder cover then, and its tags. */
data class CachedSong(
    val size: Long,
    val lastModified: Long,
    val tags: SongTags,
    val folderCoverId: String? = null
)

/**
 * The scan's songs with the tags the cache already has, and what to do with the cache.
 *
 * @property songs Every file found, in the same order, with tags for those the cache knew.
 * @property toRead Indexes in [songs] of new or changed files, whose tags must be read.
 * @property toUpdate Indexes in [songs] of unchanged files whose cached row is out of date
 *   (their folder got or lost a cover image), to save again without reading their tags.
 * @property removed Ids of cached files that are no longer in the folder.
 */
data class CacheMatch(
    val songs: List<Song>,
    val toRead: List<Int>,
    val toUpdate: List<Int>,
    val removed: List<String>
)

/**
 * Matches the files found by a scan with the cache, keyed by document id. A cached file is
 * reused only if its size and date are unchanged, so an edited file has its tags read again.
 */
fun matchCache(files: List<SongFile>, cached: Map<String, CachedSong>): CacheMatch {
    val toRead = mutableListOf<Int>()
    val toUpdate = mutableListOf<Int>()
    val songs = files.mapIndexed { index, file ->
        val hit = cached[file.documentId]
            ?.takeIf { it.size == file.size && it.lastModified == file.lastModified }
        when {
            hit == null -> toRead += index
            hit.folderCoverId != file.folderCoverId -> toUpdate += index
        }
        Song(file, hit?.tags)
    }
    val found = files.mapTo(HashSet()) { it.documentId }
    val removed = cached.keys.filter { it !in found }
    return CacheMatch(songs, toRead, toUpdate, removed)
}
