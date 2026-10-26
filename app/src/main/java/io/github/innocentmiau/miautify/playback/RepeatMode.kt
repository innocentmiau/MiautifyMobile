package io.github.innocentmiau.miautify.playback

import androidx.media3.common.Player

/** The mode after [mode] when the repeat button is pressed: off, then all, then one, then off again. */
fun nextRepeatMode(mode: Int): Int = when (mode) {
    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
    else -> Player.REPEAT_MODE_OFF
}
