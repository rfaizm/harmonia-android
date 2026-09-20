package com.rfaizm.harmoniamusic

import com.rfaizm.harmoniamusic.data.sampleSizeFor
import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumArtTest {
    @Test
    fun artworkAlreadySmallEnoughIsDecodedWhole() {
        assertEquals(1, sampleSizeFor(width = 512, height = 512, target = 512))
        assertEquals(1, sampleSizeFor(width = 100, height = 100, target = 512)) // never upscale
    }

    @Test
    fun largeArtworkIsDecodedSmaller() {
        // Halving stops while the shorter side still covers the target, so nothing is decoded blurry.
        assertEquals(4, sampleSizeFor(width = 2048, height = 2048, target = 512))
        assertEquals(2, sampleSizeFor(width = 3000, height = 2000, target = 512))
    }
}
