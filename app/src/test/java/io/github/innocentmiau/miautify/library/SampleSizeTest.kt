package io.github.innocentmiau.miautify.library

import org.junit.Assert.assertEquals
import org.junit.Test

class SampleSizeTest {
    @Test
    fun smallImagesAreDecodedWhole() {
        assertEquals(1, sampleSize(160, 160, 160))
        assertEquals(1, sampleSize(100, 300, 160))
    }

    @Test
    fun bigImagesAreDecodedAtAFractionThatStaysAboveTheTarget() {
        assertEquals(2, sampleSize(320, 320, 160))
        assertEquals(2, sampleSize(500, 500, 160))
        assertEquals(4, sampleSize(1000, 1000, 160))
        assertEquals(8, sampleSize(3000, 1400, 160))
    }
}
