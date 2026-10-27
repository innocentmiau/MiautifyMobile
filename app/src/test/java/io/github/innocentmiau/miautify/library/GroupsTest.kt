package io.github.innocentmiau.miautify.library

import org.junit.Assert.assertEquals
import org.junit.Test

class GroupsTest {
    private fun song(
        id: String,
        artist: String? = null,
        album: String? = null,
        albumArtist: String? = null,
        track: Int? = null,
        disc: Int? = null,
        folder: String = ""
    ) = Song(SongFile(id, "$id.mp3", folder), SongTags(id, artist, album, null, albumArtist, track, disc))

    private fun ids(group: SongGroup) = group.songs.map { it.file.documentId }

    @Test
    fun albumsPlayByDiscThenTrackAndNoDiscMeansDiscOne() {
        val songs = listOf(
            song("d2t1", album = "A", track = 1, disc = 2),
            song("d1t2", album = "A", track = 2, disc = 1),
            song("d1t1", album = "A", track = 1, disc = 1),
            song("none", album = "A")
        )

        // "none" has no disc (so disc 1) and no track (so the end of disc 1).
        assertEquals(listOf("d1t1", "d1t2", "none", "d2t1"), ids(albums(songs).single()))
    }

    @Test
    fun sameTitleWithDifferentAlbumArtistsIsTwoAlbums() {
        val songs = listOf(
            song("1", album = "Greatest Hits", albumArtist = "Queen"),
            song("2", album = "Greatest Hits", albumArtist = "ABBA")
        )

        assertEquals(listOf("ABBA", "Queen"), albums(songs).map { it.detail })
    }

    @Test
    fun withoutAlbumArtistOneFolderIsOneAlbumWithVariousArtists() {
        val songs = listOf(
            song("1", artist = "Composer A", album = "Game OST", folder = "OST"),
            song("2", artist = "Composer B", album = "Game OST", folder = "OST"),
            song("3", artist = "Someone", album = "Game OST", folder = "Elsewhere")
        )

        val albums = albums(songs)

        assertEquals(2, albums.size)
        val ost = albums.single { it.songs.size == 2 }
        assertEquals(null, ost.detail)
    }

    @Test
    fun anAlbumWithOneArtistShowsThatArtist() {
        val songs = listOf(song("1", artist = "Band", album = "X"), song("2", artist = "band", album = "X"))

        assertEquals("Band", albums(songs).single().detail)
    }

    @Test
    fun artistsIgnoreCaseAndAccentsAndSkipUntaggedSongs() {
        val songs = listOf(song("1", artist = "Beyoncé"), song("2", artist = "beyonce"), song("3"))

        val artists = artists(songs)

        assertEquals(1, artists.size)
        assertEquals(listOf("1", "2"), ids(artists.single()))
    }

    @Test
    fun foldersHaveTheirNameAndParentPath() {
        val songs = listOf(song("1", folder = "Rock/Album"), song("2", folder = ""))

        val folders = folders(songs)

        assertEquals(listOf("", "Rock/Album"), folders.map { it.id })
        assertEquals("Album", folders[1].name)
        assertEquals("Rock", folders[1].detail)
    }

    @Test
    fun anEmptySearchKeepsEveryGroupAndWordsMatchNameOrDetail() {
        val groups = albums(listOf(song("1", album = "Night Songs", albumArtist = "Cinderella"), song("2", album = "Day")))

        assertEquals(2, searchGroups(groups, "").size)
        assertEquals(listOf("Night Songs"), searchGroups(groups, "cinderella night").map { it.name })
    }
}
