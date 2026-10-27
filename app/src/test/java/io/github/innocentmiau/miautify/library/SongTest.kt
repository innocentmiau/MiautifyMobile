package io.github.innocentmiau.miautify.library

import org.junit.Assert.assertEquals
import org.junit.Test

class SongTest {
    private val file = SongFile("1", "03 Some Song.mp3", "Album")

    @Test
    fun titleComesFromTheTag() {
        val song = Song(file, SongTags(title = "Real Title", artist = null, album = null, durationMs = null))

        assertEquals("Real Title", song.title)
    }

    @Test
    fun titleFallsBackToTheFileNameWithoutExtension() {
        assertEquals("03 Some Song", Song(file, tags = null).title)
        assertEquals("03 Some Song", Song(file, SongTags(null, "Artist", null, null)).title)
    }

    @Test
    fun formatsMinutesAndSeconds() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:59", formatDuration(59_999))
        assertEquals("3:07", formatDuration(187_000))
        assertEquals("59:59", formatDuration(3_599_000))
    }

    @Test
    fun readsTheFirstNumberOfATrackOrDiscTag() {
        assertEquals(3, parseTagNumber("3/12"))
        assertEquals(3, parseTagNumber("03"))
        assertEquals(1, parseTagNumber(" 1 / 2 "))
        assertEquals(null, parseTagNumber("A1"))
        assertEquals(null, parseTagNumber(null))
    }

    @Test
    fun formatsHoursFromOneHourUp() {
        assertEquals("1:00:00", formatDuration(3_600_000))
        assertEquals("1:02:03", formatDuration(3_723_000))
    }
}
