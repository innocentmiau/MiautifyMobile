package io.github.innocentmiau.miautify.playback

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import kotlin.random.Random

/**
 * When each song last started playing, by document id, kept in memory for [SmartShuffle].
 * Filled from the plays table when the service starts and updated by [PlayTracker], so a deck
 * never waits for the database. Main thread only, like the player.
 */
class RecentPlays {
    private val lastPlayed = HashMap<String, Long>()

    fun lastPlayedAt(songId: String): Long? = lastPlayed[songId]

    fun started(songId: String, at: Long) {
        lastPlayed[songId] = at
    }

    /** Adds times read from the database, keeping any newer one recorded meanwhile. */
    fun load(times: Map<String, Long>) {
        for ((songId, at) in times) {
            lastPlayed.merge(songId, at, ::maxOf)
        }
    }
}

/**
 * Replaces ExoPlayer's plain random shuffle with [shuffleDeck]: the song playing first, the
 * songs heard most recently last. Dealt again when shuffle is turned on or a new list starts
 * playing. Next, previous, the notification and auto-advance all follow it.
 *
 * Custom shuffle orders are marked unstable in Media3 (their API may still change), hence the
 * opt-in.
 */
@OptIn(UnstableApi::class)
class SmartShuffle(
    private val player: ExoPlayer,
    private val recentPlays: RecentPlays
) : Player.Listener {
    // Setting the shuffle order tells the listeners that the playlist changed, which would
    // deal again, forever. Listeners are told during the call, so a flag can stop that.
    private var dealing = false

    init {
        player.addListener(this)
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        if (shuffleModeEnabled) deal()
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        if (!dealing && reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED && player.shuffleModeEnabled) {
            deal()
        }
    }

    /** Deals a new deck for the current list, starting with the song that is current. */
    fun deal() {
        val count = player.mediaItemCount
        if (count == 0) return
        val deck = shuffleDeck(count, player.currentMediaItemIndex, { index ->
            recentPlays.lastPlayedAt(player.getMediaItemAt(index).mediaId)
        })
        dealing = true
        try {
            // The seed is only used to place songs added to the list later.
            player.setShuffleOrder(DefaultShuffleOrder(deck, Random.nextLong()))
        } finally {
            dealing = false
        }
    }
}
