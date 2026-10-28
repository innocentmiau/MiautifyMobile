package io.github.innocentmiau.miautify.library

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrokenFilesTest {
    private val song = Song(SongFile("a", "a.mp3", "", size = 100, lastModified = 5), tags = null)

    @Test
    fun aFileThatFailedAndDidNotChangeIsBroken() {
        assertTrue(song.isBroken(mapOf("a" to BrokenFile(100, 5))))
    }

    @Test
    fun aFileThatChangedSinceIsPlayableAgain() {
        assertFalse(song.isBroken(mapOf("a" to BrokenFile(90, 5))))
        assertFalse(song.isBroken(mapOf("a" to BrokenFile(100, 6))))
        assertFalse(song.isBroken(emptyMap()))
    }

    @Test
    fun skipsToTheNextSongUntilEverySongFailed() {
        assertTrue(shouldSkipAfterError(failuresInARow = 1, songCount = 3, hasNext = true))
        assertTrue(shouldSkipAfterError(failuresInARow = 2, songCount = 3, hasNext = true))
        // Every song failed once: with repeat all there is always a "next", so stop here.
        assertFalse(shouldSkipAfterError(failuresInARow = 3, songCount = 3, hasNext = true))
        assertFalse(shouldSkipAfterError(failuresInARow = 1, songCount = 3, hasNext = false))
    }
}
