package io.github.innocentmiau.miautify.playback

import io.github.innocentmiau.miautify.library.Song
import io.github.innocentmiau.miautify.library.SongFile
import org.junit.Assert.assertEquals
import org.junit.Test

class ContinueQueueTest {
    private val songs = listOf("a", "b", "c", "d").map { Song(SongFile(it, "$it.mp3", ""), tags = null) }

    private fun ids(list: List<Song>) = list.map { it.file.documentId }

    @Test
    fun aPlayingSongInTheListKeepsItsPlaceSoTheSongsAfterItComeNext() {
        val (before, after) = aroundPlaying(songs, "b")

        assertEquals(listOf("a"), ids(before))
        assertEquals(listOf("c", "d"), ids(after))
    }

    @Test
    fun aPlayingSongFromElsewhereIsFollowedByTheWholeList() {
        val (before, after) = aroundPlaying(songs, "other")

        assertEquals(emptyList<String>(), ids(before))
        assertEquals(listOf("a", "b", "c", "d"), ids(after))
    }
}
