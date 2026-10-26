package io.github.innocentmiau.miautify.playback

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class RepeatModeTest {
    @Test
    fun goesFromOffToAllToOneAndBackToOff() {
        assertEquals(Player.REPEAT_MODE_ALL, nextRepeatMode(Player.REPEAT_MODE_OFF))
        assertEquals(Player.REPEAT_MODE_ONE, nextRepeatMode(Player.REPEAT_MODE_ALL))
        assertEquals(Player.REPEAT_MODE_OFF, nextRepeatMode(Player.REPEAT_MODE_ONE))
    }

    @Test
    fun anUnknownModeGoesBackToOff() {
        assertEquals(Player.REPEAT_MODE_OFF, nextRepeatMode(42))
    }
}
