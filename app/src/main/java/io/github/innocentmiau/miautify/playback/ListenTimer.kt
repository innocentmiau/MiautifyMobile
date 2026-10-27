package io.github.innocentmiau.miautify.playback

import kotlin.math.min

/** How long a song must really be listened to before it counts as played. */
private const val PLAY_THRESHOLD_MS = 30_000L

/**
 * Listening time needed for a play to count: 30 seconds, or 90% of a song shorter than that
 * (so it can count before its last moment). 30 seconds when the length isn't known yet.
 */
fun playThresholdMs(durationMs: Long?): Long =
    if (durationMs == null || durationMs <= 0) PLAY_THRESHOLD_MS else min(PLAY_THRESHOLD_MS, durationMs * 9 / 10)

/**
 * Adds up how long the current song has really been playing. Pauses don't count, and since
 * it measures time and not the song's position, seeking ahead doesn't count either.
 * Times come from the caller, so tests can use a fake clock.
 */
class ListenTimer {
    private var listenedMs = 0L
    private var playingSince: Long? = null

    fun start(now: Long) {
        if (playingSince == null) playingSince = now
    }

    fun stop(now: Long) {
        playingSince?.let { listenedMs += now - it }
        playingSince = null
    }

    fun listenedMs(now: Long): Long = listenedMs + (playingSince?.let { now - it } ?: 0L)

    /** For the next song, or the same one starting again. */
    fun reset() {
        listenedMs = 0L
        playingSince = null
    }
}
