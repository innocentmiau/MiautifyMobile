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
    // The list the current deck was dealt for, as the ids the player gave its songs. Setting
    // the shuffle order tells the listeners that the playlist changed, so a deal must not lead
    // to another one. These ids stay the same when only the order changes, while every new
    // list (setMediaItems) gets new ones, even for the same songs. (A flag set during the call
    // didn't work: called from a listener, the player tells the others only after it returns.)
    private var dealtFor: List<Any> = emptyList()

    init {
        player.addListener(this)
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        if (shuffleModeEnabled) deal()
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED &&
            player.shuffleModeEnabled &&
            timeline.windowUids() != dealtFor
        ) {
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
        dealtFor = player.currentTimeline.windowUids()
        // The seed is only used to place songs added to the list later.
        player.setShuffleOrder(DefaultShuffleOrder(deck, Random.nextLong()))
    }
}

/** The id of each song in the player's list, in list order. */
private fun Timeline.windowUids(): List<Any> {
    val window = Timeline.Window()
    return List(windowCount) { getWindow(it, window).uid }
}
