package io.github.innocentmiau.miautify.library

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchTest {
    private fun song(id: String, title: String? = null, artist: String? = null, album: String? = null, albumArtist: String? = null) =
        Song(SongFile(id, "$id.mp3", ""), SongTags(title, artist, album, null, albumArtist))

    private fun ids(songs: List<Song>) = songs.map { it.file.documentId }

    @Test
    fun foldingRemovesAccentsCaseAndPunctuation() {
        assertEquals("beyonce", foldForSearch("Beyoncé"))
        assertEquals("ac dc", foldForSearch("AC/DC"))
        assertEquals("m fi", foldForSearch("Ｍ ﬁ"))
        assertEquals("ラーメン", foldForSearch("ラーメン!"))
    }

    @Test
    fun everyWordMustMatchInAnyOrderAndAnyField() {
        val search = SongSearch(listOf(song("rc", "Ratchet & Clank", "Insomniac"), song("other", "Clank Theme")))

        assertEquals(listOf("rc"), ids(search.search("clank ratchet")))
        assertEquals(listOf("rc"), ids(search.search("insomniac clank")))
    }

    @Test
    fun accentsAndPunctuationInTheQueryDoNotMatter() {
        val search = SongSearch(listOf(song("b", "Halo", "Beyoncé"), song("a", "Thunderstruck", "AC/DC")))

        assertEquals(listOf("b"), ids(search.search("beyonce")))
        assertEquals(listOf("a"), ids(search.search("ac-dc")))
    }

    @Test
    fun albumAndAlbumArtistAreSearchedToo() {
        val search = SongSearch(listOf(song("ost", "Track 1", "Someone", "Game OST", albumArtist = "Composer")))

        assertEquals(listOf("ost"), ids(search.search("composer ost")))
    }

    @Test
    fun theFileNameStandsInOnlyWithoutATitle() {
        val search = SongSearch(listOf(song("untagged"), song("07 tagged", title = "Real Name")))

        assertEquals(listOf("untagged"), ids(search.search("untagged")))
        assertEquals(emptyList<String>(), ids(search.search("07")))
    }

    @Test
    fun anEmptyQueryFindsNothingAndResultsKeepTheListOrder() {
        val search = SongSearch(listOf(song("2", "Same B"), song("1", "Same A")))

        assertEquals(emptyList<String>(), ids(search.search("  ")))
        assertEquals(listOf("2", "1"), ids(search.search("same")))
    }
}
