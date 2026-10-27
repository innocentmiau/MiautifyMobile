package io.github.innocentmiau.miautify.playback

import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import io.github.innocentmiau.miautify.data.PlayDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Records the play history from the player: when each song starts (for shuffle) and a play
 * once it has been listened to for [playThresholdMs]. Once per load of a song, so repeat one
 * counts each round. Lives in the playback service, so plays count with the app closed too.
 */
class PlayTracker(
    private val player: Player,
    private val plays: PlayDao,
    private val recentPlays: RecentPlays,
    // Main thread: the player may only be read there. Room moves the writes off it.
    private val scope: CoroutineScope
) : Player.Listener {
    private val timer = ListenTimer()
    private var started = false
    private var counted = false
    private var countJob: Job? = null

    init {
        player.addListener(this)
    }

    /** A new song, or the same one again with repeat one. */
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        countJob?.cancel()
        timer.reset()
        started = false
        counted = false
        if (player.isPlaying) onPlaying()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            onPlaying()
        } else {
            timer.stop(SystemClock.elapsedRealtime())
            countJob?.cancel()
        }
    }

    private fun onPlaying() {
        val songId = player.currentMediaItem?.mediaId ?: return
        if (!started) {
            started = true
            val now = System.currentTimeMillis()
            recentPlays.started(songId, now)
            scope.launch { plays.markStarted(songId, now) }
        }
        if (counted) return

        // elapsedRealtime is a clock that never jumps, unlike the wall clock the user can change.
        val now = SystemClock.elapsedRealtime()
        timer.start(now)
        val duration = player.duration.takeIf { it != C.TIME_UNSET }
        val remaining = playThresholdMs(duration) - timer.listenedMs(now)
        countJob?.cancel()
        // Wakes up when the threshold would be reached. A pause or a new song cancels it.
        countJob = scope.launch {
            delay(remaining)
            counted = true
            plays.addPlay(songId)
        }
    }
}
