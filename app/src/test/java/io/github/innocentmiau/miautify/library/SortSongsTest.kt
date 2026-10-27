package io.github.innocentmiau.miautify.library

import org.junit.Assert.assertEquals
import org.junit.Test

class SortSongsTest {
    private fun song(id: String, title: String? = null, artist: String? = null, album: String? = null, folder: String = "") =
        Song(SongFile(id, "$id.mp3", folder), SongTags(title, artist, album, null))

    private fun ids(songs: List<Song>) = songs.map { it.file.documentId }

    @Test
    fun folderOrderIsByFolderThenFileName() {
        val songs = listOf(song("b", folder = "Z"), song("a", folder = "Z"), song("c", folder = "A"))

        assertEquals(listOf("c", "a", "b"), ids(sortSongs(songs, SortOrder.FOLDER, false, emptyMap())))
        assertEquals(listOf("b", "a", "c"), ids(sortSongs(songs, SortOrder.FOLDER, true, emptyMap())))
    }

    @Test
    fun titlesSortIgnoringCaseAndAccents() {
        val songs = listOf(song("1", "beta"), song("2", "Éclair"), song("3", "alpha"), song("4", "echo"))

        // "Éclair" sorts among the e's (after "echo": h comes before l), not after z.
        assertEquals(listOf("3", "1", "4", "2"), ids(sortSongs(songs, SortOrder.TITLE, false, emptyMap())))
    }

    @Test
    fun artistTiesAreBrokenByAlbumThenTitle() {
        val songs = listOf(
            song("1", "B", "Same", "Second"),
            song("2", "A", "Same", "Second"),
            song("3", "Z", "Same", "First"),
            song("4", "A", "Another", "Any")
        )

        assertEquals(listOf("4", "3", "2", "1"), ids(sortSongs(songs, SortOrder.ARTIST, false, emptyMap())))
    }

    @Test
    fun songsWithoutTheTagStayAtTheEndEitherWay() {
        val songs = listOf(song("none"), song("b", artist = "B"), song("a", artist = "A"))

        assertEquals(listOf("a", "b", "none"), ids(sortSongs(songs, SortOrder.ARTIST, false, emptyMap())))
        assertEquals(listOf("b", "a", "none"), ids(sortSongs(songs, SortOrder.ARTIST, true, emptyMap())))
    }

    @Test
    fun reversingFlipsOnlyTheChosenOrderNotTheTies() {
        val songs = listOf(song("2", album = "Same"), song("1", album = "Same"), song("3", album = "Other"))

        assertEquals(listOf("1", "2", "3"), ids(sortSongs(songs, SortOrder.ALBUM, true, emptyMap())))
    }

    @Test
    fun mostPlayedComesFirstAndUnplayedCountAsZero() {
        val songs = listOf(song("never"), song("often"), song("once"))
        val counts = mapOf("often" to 12, "once" to 1)

        assertEquals(listOf("often", "once", "never"), ids(sortSongs(songs, SortOrder.MOST_PLAYED, false, counts)))
        assertEquals(listOf("never", "once", "often"), ids(sortSongs(songs, SortOrder.MOST_PLAYED, true, counts)))
    }
}
