package io.github.innocentmiau.miautify.library

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryListsTest {
    private fun song(id: String, title: String, album: String) =
        Song(SongFile(id, "$id.mp3", ""), SongTags(title, "Artist", album, null))

    @Test
    fun songsAndGroupsFollowTheirOwnOrdersAndTheSearchUsesTheSortedSongs() {
        val songs = listOf(song("1", "Banana", "Zebra"), song("2", "Apple", "Ant"))

        val lists = buildLists(
            songs,
            sortOrder = SortOrder.TITLE,
            sortReversed = false,
            groupOrder = GroupOrder.MOST_PLAYED,
            playCounts = mapOf("1" to 3),
            lastPlayed = emptyMap()
        )

        assertEquals(listOf("Apple", "Banana"), lists.songs.map { it.title })
        // Zebra was played, so it leads even though Ant comes first by name.
        assertEquals(listOf("Zebra", "Ant"), lists.groups(GroupKind.ALBUM).map { it.name })
        assertEquals(listOf("Apple", "Banana"), lists.search.search("artist").map { it.title })
    }
}
