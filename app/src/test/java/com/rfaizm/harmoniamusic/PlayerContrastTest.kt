package com.rfaizm.harmoniamusic

import androidx.compose.ui.graphics.Color
import com.rfaizm.harmoniamusic.ui.GRADIENTS
import com.rfaizm.harmoniamusic.ui.LIKED_GRADIENT
import com.rfaizm.harmoniamusic.ui.ON_PLAYER_LARGE
import com.rfaizm.harmoniamusic.ui.ON_PLAYER_SMALL
import com.rfaizm.harmoniamusic.ui.PLAYER_SCRIM
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * M11 (T50): white text on the full player stays readable over every song gradient. The text can sit anywhere from
 * the top (the first colour) to the middle (the second); further down the backdrop only gets darker.
 * WCAG 2.1: 4.5:1 for small text, 3:1 for large (18 sp+, or 14 sp+ bold).
 */
class PlayerContrastTest {
    private val backdrops = (GRADIENTS + LIKED_GRADIENT).flatMap { (top, middle) -> listOf(top, middle) }.map { it.darkened(PLAYER_SCRIM) }

    @Test
    fun smallTextReachesFourAndAHalfToOneOnEveryGradient() = assertReadable(ON_PLAYER_SMALL, 4.5)

    @Test
    fun theDimLyricLinesReachThreeToOne() = assertReadable(ON_PLAYER_LARGE, 3.0)

    private fun assertReadable(alpha: Float, needed: Double) = backdrops.forEach { bg ->
        val ratio = contrast(Color.White.copy(alpha = alpha).over(bg), bg)
        assertTrue("white at $alpha on $bg is ${"%.2f".format(ratio)}:1, needs $needed", ratio >= needed)
    }

    private fun Color.darkened(by: Float) = Color(red * (1 - by), green * (1 - by), blue * (1 - by))
    private fun Color.over(bg: Color) = Color(red * alpha + bg.red * (1 - alpha), green * alpha + bg.green * (1 - alpha), blue * alpha + bg.blue * (1 - alpha))

    private fun luminance(c: Color): Double {
        fun channel(v: Float) = if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        return 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val (la, lb) = luminance(a) to luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }
}
