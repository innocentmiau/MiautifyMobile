package io.github.innocentmiau.miautify.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SavedQueueTest {
    private val songs = listOf(
        SavedSong("1", "content://a/1", "First", artist = "Artist", album = "Album"),
        SavedSong("2", "content://a/2", "Second")
    )

    @Test
    fun aQueueSurvivesSavingAndLoading() {
        val queue = SavedQueue(songs, index = 1, positionMs = 42_000)

        assertEquals(queue, decodeQueue(encodeQueue(queue)))
    }

    @Test
    fun damagedOrEmptyTextGivesNoQueue() {
        assertNull(decodeQueue(""))
        assertNull(decodeQueue("not json"))
        assertNull(decodeQueue("""{"songs": [{"id": "1"}"""))
    }

    @Test
    fun aQueueWithoutSongsIsNoQueue() {
        assertNull(decodeQueue(encodeQueue(SavedQueue(emptyList(), index = 0, positionMs = 0))))
    }

    @Test
    fun anIndexOrPositionOutOfRangeIsCorrected() {
        val loaded = decodeQueue(encodeQueue(SavedQueue(songs, index = 7, positionMs = -5)))

        assertEquals(1, loaded?.index)
        assertEquals(0L, loaded?.positionMs)
    }

    @Test
    fun unknownFieldsFromANewerVersionAreIgnored() {
        val text = """{"songs": [{"id": "1", "uri": "content://a/1", "title": "First", "rating": 5}], "index": 0, "positionMs": 0, "shuffle": true}"""

        assertEquals("First", decodeQueue(text)?.songs?.single()?.title)
    }
}
