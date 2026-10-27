package io.github.innocentmiau.miautify.playback

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class ShuffleDeckTest {
    private fun deck(count: Int, first: Int, played: Map<Int, Long> = emptyMap(), seed: Int = 1) =
        shuffleDeck(count, first, { played[it] }, Random(seed)).toList()

    @Test
    fun everySongComesOnceAndTheTappedOneFirst() {
        val deck = deck(10, first = 4)

        assertEquals(4, deck.first())
        assertEquals((0 until 10).toList(), deck.sorted())
    }

    @Test
    fun withoutAFirstSongEverySongIsShuffled() {
        assertEquals((0 until 5).toList(), deck(5, first = -1).sorted())
    }

    @Test
    fun recentlyPlayedSongsGoLastWithTheMostRecentAtTheVeryEnd() {
        // 10 songs: up to 3 (30%) are kept for the end.
        val played = mapOf(1 to 100L, 2 to 300L, 3 to 200L)

        val deck = deck(10, first = 0, played = played)

        assertEquals(listOf(1, 3, 2), deck.takeLast(3))
    }

    @Test
    fun onlyThe30PercentHeardMostRecentlyAreKeptForTheEnd() {
        val played = (1 until 10).associateWith { it.toLong() }

        val deck = deck(10, first = 0, played = played)

        assertEquals(listOf(7, 8, 9), deck.takeLast(3))
        assertEquals((1..6).toSet(), deck.subList(1, 7).toSet())
    }

    @Test
    fun aSmallListStillKeepsSomethingToShuffle() {
        // 2 songs, both played: the other one can't be "kept for last" and also be all there is.
        assertEquals(listOf(0, 1), deck(2, first = 0, played = mapOf(0 to 1L, 1 to 2L)))
        assertEquals(listOf(2, 1, 0), deck(3, first = 2, played = mapOf(0 to 5L)))
    }

    @Test
    fun theSameSeedDealsTheSameDeck() {
        assertEquals(deck(20, first = 3, seed = 7), deck(20, first = 3, seed = 7))
    }
}
