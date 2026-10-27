package io.github.innocentmiau.miautify.library

import java.text.Collator

/** The orders the song list can be shown in. Songs play in the order shown. */
enum class SortOrder {
    /** By folder, then file name: the scan's own order. */
    FOLDER,
    TITLE,
    ARTIST,
    ALBUM,
    MOST_PLAYED
}

// Compares text the way people expect in the phone's language: "é" next to "e", case ignored.
private val collator = Collator.getInstance().apply { strength = Collator.SECONDARY }
internal val textOrder = Comparator<String> { a, b -> collator.compare(a, b) }
internal val folderOrder: Comparator<Song> = compareBy(songFileOrder) { it.file }

/**
 * [songs] in [order], or the other way around when [reversed]. Ties are always broken the
 * same way (artist then album then title, then folder order), and reversing only flips the
 * chosen order, not the ties. Songs missing the tag sorted by stay at the end either way.
 *
 * @param playCounts Play counts by document id, for [SortOrder.MOST_PLAYED]. Missing means 0.
 */
fun sortSongs(
    songs: List<Song>,
    order: SortOrder,
    reversed: Boolean,
    playCounts: Map<String, Int>
): List<Song> {
    fun Comparator<Song>.direction() = if (reversed) reversed() else this

    return when (order) {
        SortOrder.FOLDER -> songs.sortedWith(folderOrder.direction())
        SortOrder.TITLE -> songs.sortedWith(compareBy(textOrder, Song::title).direction().then(folderOrder))
        SortOrder.ARTIST -> sortByTag(songs, { it.tags?.artist }, reversed) {
            compareBy<Song, String>(textOrder) { it.tags?.album.orEmpty() }
                .thenBy(textOrder, Song::title)
                .then(folderOrder)
        }
        SortOrder.ALBUM -> sortByTag(songs, { it.tags?.album }, reversed) {
            // File names often start with the track number, so folder order keeps tracks in order.
            folderOrder
        }
        SortOrder.MOST_PLAYED -> songs.sortedWith(
            compareByDescending<Song> { playCounts[it.file.documentId] ?: 0 }.direction().then(folderOrder)
        )
    }
}

/** Songs with the tag [key] first, sorted by it then by [ties], and the songs without it after, in folder order. */
private fun sortByTag(
    songs: List<Song>,
    key: (Song) -> String?,
    reversed: Boolean,
    ties: () -> Comparator<Song>
): List<Song> {
    val (tagged, untagged) = songs.partition { key(it) != null }
    val byTag = compareBy<Song, String>(textOrder) { key(it)!! }
    val primary = if (reversed) byTag.reversed() else byTag
    return tagged.sortedWith(primary.then(ties())) + untagged.sortedWith(folderOrder)
}
