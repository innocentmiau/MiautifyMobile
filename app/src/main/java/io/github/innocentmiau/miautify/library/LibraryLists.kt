package io.github.innocentmiau.miautify.library

/**
 * Everything the screens list, made from one scan in one go: the sorted song list with its
 * search index, and the sorted albums, artists and folders. Slow for a big library (sorting,
 * grouping and folding the text of thousands of songs), so made off the main thread and kept,
 * so opening a screen again is instant.
 */
class LibraryLists(
    /** In the song list's order, which is also the order they play in. */
    val songs: List<Song>,
    val search: SongSearch,
    private val albums: List<SongGroup>,
    private val artists: List<SongGroup>,
    private val folders: List<SongGroup>
) {
    fun groups(kind: GroupKind): List<SongGroup> = when (kind) {
        GroupKind.ALBUM -> albums
        GroupKind.ARTIST -> artists
        GroupKind.FOLDER -> folders
    }
}

/**
 * The lists for [songs]: the songs by [sortOrder] (or reversed), the groups by [groupOrder].
 *
 * @param playCounts Play counts by document id, for the "most played" orders. Missing means 0.
 * @param lastPlayed When each song last started playing, for "recently played". Missing means never.
 */
fun buildLists(
    songs: List<Song>,
    sortOrder: SortOrder,
    sortReversed: Boolean,
    groupOrder: GroupOrder,
    playCounts: Map<String, Int>,
    lastPlayed: Map<String, Long>
): LibraryLists {
    val sorted = sortSongs(songs, sortOrder, sortReversed, playCounts)
    fun groups(kind: GroupKind) = sortGroups(groupSongs(songs, kind), groupOrder, playCounts, lastPlayed)
    return LibraryLists(
        songs = sorted,
        search = SongSearch(sorted),
        albums = groups(GroupKind.ALBUM),
        artists = groups(GroupKind.ARTIST),
        folders = groups(GroupKind.FOLDER)
    )
}
