package io.github.innocentmiau.miautify.playback

import kotlin.random.Random

/** The share of the list kept for the end of a shuffle: the songs heard most recently. */
private const val RECENT_SHARE = 0.3

/**
 * A shuffled order of a list of [count] songs, like the desktop app's deck: [first] (the song
 * tapped, or the one playing) leads, the rest is shuffled, and the songs heard most recently
 * (up to 30% of the list, never all of it) go at the end, the most recent one last. So a
 * shuffle doesn't open with what you just heard. Returns the song indexes in play order.
 *
 * @param first The index to play first, or any index outside the list for none.
 * @param lastPlayedAt When the song at an index last started playing, or null if never.
 */
fun shuffleDeck(
    count: Int,
    first: Int,
    lastPlayedAt: (index: Int) -> Long?,
    random: Random = Random.Default
): IntArray {
    val hasFirst = first in 0 until count
    val others = (0 until count).filter { it != first }

    // At least one when any song was played, but always leave something to shuffle.
    val maxRecent = minOf((count * RECENT_SHARE).toInt().coerceAtLeast(1), others.size - 1)
    val recent = others
        .mapNotNull { index -> lastPlayedAt(index)?.let { index to it } }
        .sortedByDescending { (_, at) -> at }
        .take(maxRecent.coerceAtLeast(0))
        .sortedBy { (_, at) -> at }
        .map { (index, _) -> index }
    val recentSet = recent.toSet()
    val fresh = others.filter { it !in recentSet }.shuffled(random)

    return (listOfNotNull(first.takeIf { hasFirst }) + fresh + recent).toIntArray()
}
