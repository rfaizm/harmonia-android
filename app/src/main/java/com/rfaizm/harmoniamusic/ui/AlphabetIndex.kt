package com.rfaizm.harmoniamusic.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rfaizm.harmoniamusic.ui.theme.HarmoniaTheme
import com.rfaizm.harmoniamusic.ui.theme.muted
import com.rfaizm.harmoniamusic.ui.theme.mutedForeground
import kotlin.math.roundToInt

private val LETTER_HEIGHT = 18.dp
private val TRACK_PADDING = 6.dp
private val TOUCH_WIDTH = 28.dp // wider than the capsule it shows, so a thumb finds it easily
private val BUBBLE_SIZE = 56.dp
private val BUBBLE_GAP = 8.dp

/**
 * T41/T42: the A–Z index beside the Songs list, in Harmonia's own shapes.
 * - At rest it is a slim muted capsule, like an inactive sort pill (GUIDELINE §5.4), holding the letters the list
 *   has in the letter headings' extra-bold (§5.8).
 * - The group at the top of the list is lit the way the nav bar lights its tab: sage on a primary/15 dot (§5.2).
 * - Touching it tints the capsule sage, pops the letter under the finger, and shows that letter in a sage bubble
 *   beside it, since the finger covers the bar. The bubble glides with the mini player's spring (§10).
 * [currentSection] is read here rather than passed as a value, so a scroll redraws the bar, not the whole screen.
 */
@Composable
fun AlphabetIndex(letters: List<String>, currentSection: () -> Int, onPick: (index: Int) -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val pick by rememberUpdatedState(onPick)
    var touched by remember { mutableIntStateOf(-1) } // the letter under the finger, or -1
    var bubble by remember { mutableIntStateOf(0) } // the bubble's letter, kept while it fades out
    var heightPx by remember { mutableIntStateOf(0) }
    val padPx = with(LocalDensity.current) { TRACK_PADDING.toPx() }
    val track by animateColorAsState(
        if (touched >= 0) colors.primary.copy(alpha = 0.15f) else colors.muted.copy(alpha = 0.7f),
        label = "indexTrack",
    )
    // A new touch puts the bubble straight on its letter; while the finger slides, it glides from letter to letter.
    val bubbleY = remember { Animatable(0f) }
    var gliding by remember { mutableStateOf(false) }
    LaunchedEffect(bubble, touched >= 0, heightPx) {
        if (touched < 0) {
            gliding = false
            return@LaunchedEffect
        }
        val target = padPx + (bubble + 0.5f) * (heightPx - 2 * padPx) / letters.size.coerceAtLeast(1)
        if (gliding) bubbleY.animateTo(target, spring(dampingRatio = 0.81f, stiffness = 340f)) else bubbleY.snapTo(target)
        gliding = true
    }

    Box(
        modifier
            .width(TOUCH_WIDTH)
            .heightIn(max = LETTER_HEIGHT * letters.size + TRACK_PADDING * 2)
            .fillMaxHeight()
            .onSizeChanged { heightPx = it.height }
            .pointerInput(letters) {
                awaitEachGesture {
                    fun touch(y: Float) {
                        val i = letterAt(y - padPx, (size.height - 2 * padPx).roundToInt(), letters.size)
                        if (i == touched) return
                        touched = i
                        bubble = i
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        pick(i)
                    }
                    try {
                        val down = awaitFirstDown()
                        down.consume()
                        touch(down.position.y)
                        drag(down.id) { change ->
                            change.consume()
                            touch(change.position.y)
                        }
                    } finally {
                        touched = -1
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .width(20.dp)
                .clip(CircleShape)
                .background(track)
                .padding(vertical = TRACK_PADDING),
        ) {
            val current = currentSection()
            letters.forEachIndexed { i, letter ->
                IndexLetter(letter, lit = i == current, pressed = i == touched, onActivate = { pick(i) }, Modifier.weight(1f))
            }
        }
        AnimatedVisibility(
            touched >= 0,
            Modifier
                .align(Alignment.TopCenter)
                .offset {
                    // Centred on the letter, to the bar's left, clear of the thumb.
                    val left = (TOUCH_WIDTH / 2 + BUBBLE_GAP + BUBBLE_SIZE / 2).roundToPx()
                    IntOffset(-left, (bubbleY.value - BUBBLE_SIZE.toPx() / 2).roundToInt())
                },
            enter = scaleIn(initialScale = 0.7f) + fadeIn(),
            exit = scaleOut(targetScale = 0.7f) + fadeOut(),
        ) {
            Box(
                Modifier
                    .requiredSize(BUBBLE_SIZE)
                    .shadow(4.dp, CircleShape)
                    .background(colors.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(letters.getOrElse(bubble) { "" }, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = colors.onPrimary)
            }
        }
    }
}

@Composable
private fun IndexLetter(letter: String, lit: Boolean, pressed: Boolean, onActivate: () -> Unit, modifier: Modifier) {
    val scale by animateFloatAsState(if (pressed) 1.25f else 1f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "indexLetter")
    Box(
        modifier
            .fillMaxWidth()
            // One button per letter for TalkBack, which can't slide along the bar.
            .clearAndSetSemantics {
                contentDescription = if (letter == "#") "Jump to other titles" else "Jump to $letter"
                role = Role.Button
                onClick { onActivate(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(16.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .background(if (lit) colors.primary.copy(alpha = 0.15f) else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                letter, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                color = if (lit || pressed) colors.primary else colors.mutedForeground,
            )
        }
    }
}

/** Which of [count] evenly spread letters a finger at [y] is on; past either end it keeps the end letter. */
internal fun letterAt(y: Float, height: Int, count: Int): Int =
    if (height <= 0 || count <= 0) 0 else (y / height * count).toInt().coerceIn(0, count - 1)

/** The letter group at the top of the list: the last heading at or above the first row showing, or -1 for none. */
internal fun sectionAt(headingRows: List<Int>, firstVisibleRow: Int): Int = headingRows.indexOfLast { it <= firstVisibleRow }

@Preview(showBackground = true, backgroundColor = 0xFFF5F4F0, heightDp = 420)
@Composable
private fun AlphabetIndexPreview() = HarmoniaTheme(darkTheme = false) {
    AlphabetIndex(PREVIEW_LETTERS, currentSection = { 4 }, onPick = {}, Modifier.padding(vertical = 8.dp))
}

@Preview(showBackground = true, backgroundColor = 0xFF1E2022, heightDp = 420)
@Composable
private fun AlphabetIndexDarkPreview() = HarmoniaTheme(darkTheme = true) {
    AlphabetIndex(PREVIEW_LETTERS, currentSection = { 4 }, onPick = {}, Modifier.padding(vertical = 8.dp))
}

private val PREVIEW_LETTERS = "ABCDEFGHJKLMNOPRSTVWY#".map { it.toString() }
