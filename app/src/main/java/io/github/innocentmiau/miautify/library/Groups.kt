package io.github.innocentmiau.miautify.library

/** The ways songs are grouped for browsing. */
enum class GroupKind { ALBUM, ARTIST, FOLDER }

/**
 * Songs that belong together: an album, an artist or a folder.
 *
 * @property id Unique among the groups of its kind, to open it again after a rescan.
 * @property name The album title, the artist's name, or the folder's own name ("" for the
 *   music folder itself).
 * @property detail The album's artist (null when its songs have several: "Various artists"),
 *   or a folder's parent path. Null for artists.
 * @property songs In playing order: by disc and track for an album.
 */
data class SongGroup(
    val id: String,
    val name: String,
    val detail: String?,
    val songs: List<Song>
)

/**
 * By disc (none means disc 1, the usual convention), then track number (songs without one at
 * the end of their disc), then folder order.
 */
private val trackOrder: Comparator<Song> =
    compareBy<Song>({ it.tags?.discNumber ?: 1 }, { it.tags?.trackNumber ?: Int.MAX_VALUE })
        .then(folderOrder)

/** By name, then by detail, so two albums with the same title go by artist. */
private val byName: Comparator<SongGroup> =
    compareBy(textOrder, SongGroup::name).thenBy(textOrder) { it.detail.orEmpty() }

/** The groups of [kind] made from [songs]. */
fun groupSongs(songs: List<Song>, kind: GroupKind): List<SongGroup> = when (kind) {
    GroupKind.ALBUM -> albums(songs)
    GroupKind.ARTIST -> artists(songs)
    GroupKind.FOLDER -> folders(songs)
}

/**
 * Albums by title and album artist, so two different "Greatest Hits" stay apart. Without an
 * album artist tag, the same title in the same folder is one album, which keeps a soundtrack
 * with a different artist on every track together. Songs without an album tag are left out.
 */
fun albums(songs: List<Song>): List<SongGroup> =
    songs.filter { it.tags?.album != null }
        .groupBy(::albumKey)
        .map { (key, inAlbum) ->
            val tags = inAlbum.first().tags!!
            val artists = inAlbum.mapNotNull { it.tags?.artist }.distinctBy(::foldForSearch)
            SongGroup(key, tags.album!!, tags.albumArtist ?: artists.singleOrNull(), inAlbum.sortedWith(trackOrder))
        }
        .sortedWith(byName)

private fun albumKey(song: Song): String {
    val tags = song.tags!!
    val owner = tags.albumArtist?.let(::foldForSearch) ?: "folder:${song.file.folder}"
    return "${foldForSearch(tags.album!!)}|$owner"
}

/** Artists by the songs' artist tag, ignoring case and accents. Each one's songs by album, then track. */
fun artists(songs: List<Song>): List<SongGroup> =
    songs.filter { it.tags?.artist != null }
        .groupBy { foldForSearch(it.tags!!.artist!!) }
        .map { (key, byArtist) ->
            val inOrder = byArtist.sortedWith(compareBy<Song, String>(textOrder) { it.tags?.album.orEmpty() }.then(trackOrder))
            SongGroup(key, byArtist.first().tags!!.artist!!, null, inOrder)
        }
        .sortedWith(byName)

/** Every folder that has songs directly in it, by path, each with its songs in folder order. */
fun folders(songs: List<Song>): List<SongGroup> =
    songs.groupBy { it.file.folder }
        .map { (path, inFolder) ->
            SongGroup(
                id = path,
                name = path.substringAfterLast('/'),
                detail = path.substringBeforeLast('/', "").ifEmpty { null },
                songs = inFolder.sortedWith(folderOrder)
            )
        }
        .sortedWith(compareBy(textOrder, SongGroup::id))

/** How long all the group's songs last together. Songs whose length isn't known count as 0. */
val SongGroup.durationMs: Long
    get() = songs.sumOf { it.tags?.durationMs ?: 0L }

/** The play counts of the group's songs added up. [playCounts] is by document id; missing means 0. */
fun SongGroup.playCount(playCounts: Map<String, Int>): Int =
    songs.sumOf { playCounts[it.file.documentId] ?: 0 }

/** The orders albums, artists and folders can be browsed in. */
enum class GroupOrder {
    /** The most listened to first: the play counts of their songs added up. */
    MOST_PLAYED,
    /** The one with the song that started playing last first. Never played ones at the end. */
    RECENTLY_PLAYED,
    /** By name, or by path for folders. */
    NAME,
    /** The newest file first: the group whose most recently added or changed song is newest. */
    NEWEST
}

/**
 * [groups] in [order]. They come in from [groupSongs] by name, and the sorts here keep that
 * order for ties, so groups with the same play count, or never played, stay by name.
 *
 * @param playCounts Play counts by document id, for [GroupOrder.MOST_PLAYED]. Missing means 0.
 * @param lastPlayed When each song last started playing, by document id, for
 *   [GroupOrder.RECENTLY_PLAYED]. Missing means never.
 */
fun sortGroups(
    groups: List<SongGroup>,
    order: GroupOrder,
    playCounts: Map<String, Int>,
    lastPlayed: Map<String, Long>
): List<SongGroup> = when (order) {
    GroupOrder.MOST_PLAYED -> groups.sortedByDescending { it.playCount(playCounts) }
    // 0 for never played: before 1970, so after every real time.
    GroupOrder.RECENTLY_PLAYED -> groups.sortedByDescending { group -> group.songs.maxOf { lastPlayed[it.file.documentId] ?: 0L } }
    GroupOrder.NAME -> groups
    GroupOrder.NEWEST -> groups.sortedByDescending { group -> group.songs.maxOf { it.file.lastModified } }
}

/** [groups] whose name or detail has every word of [query]. An empty query keeps them all: browsing. */
fun searchGroups(groups: List<SongGroup>, query: String): List<SongGroup> {
    val words = searchWords(query)
    if (words.isEmpty()) return groups
    return groups.filter { group ->
        val haystack = foldForSearch(listOfNotNull(group.name, group.detail).joinToString(" "))
        words.all { it in haystack }
    }
}
