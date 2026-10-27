package io.github.innocentmiau.miautify.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class ListenTimerTest {
    @Test
    fun countsOnlyTheTimeSpentPlaying() {
        val timer = ListenTimer()

        timer.start(now = 1_000)
        timer.stop(now = 11_000)
        // Paused for a minute: not counted.
        timer.start(now = 71_000)

        assertEquals(15_000, timer.listenedMs(now = 76_000))
    }

    @Test
    fun startingTwiceDoesNotRestartTheClock() {
        val timer = ListenTimer()

        timer.start(now = 0)
        timer.start(now = 5_000)

        assertEquals(8_000, timer.listenedMs(now = 8_000))
    }

    @Test
    fun resetStartsFromZero() {
        val timer = ListenTimer()
        timer.start(now = 0)
        timer.stop(now = 20_000)

        timer.reset()

        assertEquals(0, timer.listenedMs(now = 30_000))
    }

    @Test
    fun thresholdIs30SecondsOrNinetyPercentOfAShortSong() {
        assertEquals(30_000, playThresholdMs(200_000))
        assertEquals(30_000, playThresholdMs(null))
        assertEquals(18_000, playThresholdMs(20_000))
    }
}
