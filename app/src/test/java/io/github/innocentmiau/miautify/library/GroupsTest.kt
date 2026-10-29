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
        folder: String = "",
        lastModified: Long = 0,
        durationMs: Long? = null
    ) = Song(SongFile(id, "$id.mp3", folder, lastModified = lastModified), SongTags(id, artist, album, durationMs, albumArtist, track, disc))

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
    fun mostPlayedAddsUpTheSongsAndTiesStayByName() {
        val groups = albums(
            listOf(
                song("a1", album = "A"), song("a2", album = "A"),
                song("b1", album = "B"),
                song("c1", album = "C"),
                song("d1", album = "D")
            )
        )
        // A: 2 + 2 = 4 beats B's 3. C and D were never played and stay by name.
        val counts = mapOf("a1" to 2, "a2" to 2, "b1" to 3)

        assertEquals(listOf("A", "B", "C", "D"), sortGroups(groups, GroupOrder.MOST_PLAYED, counts, emptyMap()).map { it.name })
        assertEquals(listOf("B", "A", "C", "D"), sortGroups(groups, GroupOrder.MOST_PLAYED, counts + ("b1" to 5), emptyMap()).map { it.name })
    }

    @Test
    fun recentlyPlayedGoesByTheLastSongStartedAndNeverPlayedStayByNameAtTheEnd() {
        val groups = albums(
            listOf(
                song("a1", album = "A"),
                song("b1", album = "B"), song("b2", album = "B"),
                song("c1", album = "C"),
                song("d1", album = "D")
            )
        )
        // B's second song is the latest one, even though its first was played long ago.
        val lastPlayed = mapOf("a1" to 200L, "b1" to 10L, "b2" to 300L)

        assertEquals(listOf("B", "A", "C", "D"), sortGroups(groups, GroupOrder.RECENTLY_PLAYED, emptyMap(), lastPlayed).map { it.name })
    }

    @Test
    fun newestGoesByTheGroupsNewestSong() {
        val groups = albums(
            listOf(
                song("old", album = "Old", lastModified = 100),
                song("mixed1", album = "Mixed", lastModified = 50),
                song("mixed2", album = "Mixed", lastModified = 300),
                song("new", album = "New", lastModified = 200)
            )
        )

        // One song added to Mixed last makes it the newest album.
        assertEquals(listOf("Mixed", "New", "Old"), sortGroups(groups, GroupOrder.NEWEST, emptyMap(), emptyMap()).map { it.name })
        assertEquals(listOf("Mixed", "New", "Old"), sortGroups(groups, GroupOrder.NAME, emptyMap(), emptyMap()).map { it.name })
    }

    @Test
    fun anEmptySearchKeepsEveryGroupAndWordsMatchNameOrDetail() {
        val groups = albums(listOf(song("1", album = "Night Songs", albumArtist = "Cinderella"), song("2", album = "Day")))

        assertEquals(2, searchGroups(groups, "").size)
        assertEquals(listOf("Night Songs"), searchGroups(groups, "cinderella night").map { it.name })
    }

    @Test
    fun aGroupAddsUpItsLengthAndPlaysAndSkipsWhatIsUnknown() {
        val album = albums(
            listOf(
                song("1", album = "A", durationMs = 180_000),
                song("2", album = "A", durationMs = 240_000),
                song("3", album = "A")
            )
        ).single()

        assertEquals(420_000L, album.durationMs)
        assertEquals(7, album.playCount(mapOf("1" to 5, "3" to 2, "elsewhere" to 9)))
    }
}
