package com.rfaizm.harmoniamusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.withFrameMillis
import com.rfaizm.harmoniamusic.data.LyricLine
import com.rfaizm.harmoniamusic.data.currentLine
import com.rfaizm.harmoniamusic.data.parseLrc
import kotlinx.coroutines.delay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.rfaizm.harmoniamusic.SLEEP_END_OF_TRACK
import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.data.LyricsCache
import com.rfaizm.harmoniamusic.data.LyricsResult
import com.rfaizm.harmoniamusic.data.Settings
import com.rfaizm.harmoniamusic.data.Settings.Key
import com.rfaizm.harmoniamusic.data.albumArtOf
import com.rfaizm.harmoniamusic.data.fetchLyrics
import com.rfaizm.harmoniamusic.data.lyricsQueries
import com.rfaizm.harmoniamusic.data.readLrc
import kotlinx.coroutines.launch
import com.rfaizm.harmoniamusic.data.formatDuration
import com.rfaizm.harmoniamusic.ui.theme.PlayerBottom
import com.rfaizm.harmoniamusic.ui.theme.card

@Composable
fun MiniPlayer(song: Song, isPlaying: Boolean, progress: Float, onToggle: () -> Unit, onNext: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val (g0, g1) = song.gradient
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp)
            .shadow(16.dp, shape, ambientColor = g1, spotColor = g1)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(g0.copy(alpha = 0.94f), g1.copy(alpha = 0.94f))))
            .clickable(onClick = onOpen)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AlbumArt(song.gradient, 42.dp, Modifier.shadow(2.dp, RoundedCornerShape(12.dp)), songId = song.id)
            Column(Modifier.weight(1f)) {
                Text(song.displayTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.displayArtist, fontSize = 12.sp, color = Color.White.copy(alpha = 0.65f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            CircleIcon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (isPlaying) "Pause" else "Play", onToggle,
                size = 36.dp, iconSize = 19.dp, tint = Color.White, background = Color.White.copy(alpha = 0.15f)
            )
            CircleIcon(Icons.Rounded.SkipNext, "Next", onNext, size = 36.dp, iconSize = 19.dp, tint = Color.White, background = Color.White.copy(alpha = 0.10f))
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.15f))) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(Color.White.copy(alpha = 0.6f)))
        }
    }
}

@Composable
fun FullPlayer(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    shuffle: Boolean,
    repeatMode: Int, // Player.REPEAT_MODE_*
    volume: Float,   // device media volume, 0..1
    muted: Boolean,
    sleep: SleepOption?,
    lyrics: String?,
    playerArtist: String?,
    position: () -> Long, // the player's clock in ms, read only while synced lyrics are on screen
    onClose: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Float) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onVolume: (Float) -> Unit,
    onMute: () -> Unit,
    onSleep: (SleepOption?) -> Unit,
    onLike: () -> Unit,
) {
    val (g0, g1) = song.gradient
    // Dragging only moves the thumb; the seek happens on release, not on every frame.
    var drag by remember(song.id) { mutableStateOf<Float?>(null) }
    val shown = drag ?: progress
    val seek by rememberUpdatedState(onSeek) // the gesture detectors below outlive a single onSeek lambda
    // The real volume arrives a moment later via the player, so the thumb follows the finger meanwhile.
    var volumeDrag by remember { mutableStateOf<Float?>(null) }
    // Stays on across songs, like the art it replaces; closing the player brings the art back.
    var showLyrics by remember { mutableStateOf(false) }
    BackHandler(onBack = onClose)

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to g0, 0.45f to g1, 1f to PlayerBottom))
            // swallow taps so they don't reach the screen underneath
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .systemBarsPadding()
    ) {
        // 1. Top bar
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleIcon(Icons.Rounded.KeyboardArrowDown, "Close player", onClose, size = 36.dp, iconSize = 22.dp, tint = Color.White, background = Color.White.copy(alpha = 0.10f))
            Text("NOW PLAYING", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            HeartButton(song.liked, onLike, iconSize = 18.dp, unlikedTint = Color.White)
        }

        // 2. Album art, or the lyrics in its place (T39), so the controls below keep working while they're read
        Crossfade(showLyrics, Modifier.weight(1f).fillMaxWidth(), label = "artOrLyrics") { lyricsShown ->
            if (lyricsShown) LyricsView(song, lyrics, playerArtist, position, Modifier.fillMaxSize().padding(vertical = 16.dp))
            else PlayerArt(song, isPlaying)
        }

        // 3. Song info
        Row(Modifier.padding(horizontal = 28.dp).padding(bottom = 20.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(song.displayTitle, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${song.displayArtist} · ${song.album}", fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
            Row(Modifier.padding(top = 6.dp, start = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.Star, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(12.dp))
                Text("${song.playCount}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f))
            }
        }

        // 4. Scrubber
        Column(Modifier.padding(horizontal = 28.dp).padding(bottom = 20.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .pointerInput(Unit) { detectTapGestures { seek((it.x / size.width).coerceIn(0f, 1f)) } }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(onDragEnd = { drag?.let(seek); drag = null }, onDragCancel = { drag = null }) { change, _ ->
                            drag = (change.position.x / size.width).coerceIn(0f, 1f)
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))) {
                    Box(Modifier.fillMaxWidth(shown).fillMaxHeight().clip(CircleShape).background(Color.White))
                }
                Row(Modifier.fillMaxWidth(shown), horizontalArrangement = Arrangement.End) {
                    Box(Modifier.size(12.dp).shadow(2.dp, CircleShape).background(Color.White, CircleShape))
                }
            }
            Row(Modifier.fillMaxWidth()) {
                Text(formatDuration((song.duration * shown).toInt()), fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.weight(1f))
                Text(formatDuration(song.duration), fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
            }
        }

        // 5. Controls — wide spacing between play/skip (PRD phase 7)
        Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(bottom = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            ModeButton(Icons.Rounded.Shuffle, "Shuffle", shuffle, onShuffle)
            CircleIcon(Icons.Rounded.SkipPrevious, "Previous", onPrev, size = 48.dp, iconSize = 34.dp, tint = Color.White.copy(alpha = 0.85f))
            val playScale by animateFloatAsState(if (isPlaying) 1f else 0.96f, label = "play")
            Box(
                Modifier
                    .size(64.dp)
                    .graphicsLayer { scaleX = playScale; scaleY = playScale }
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (isPlaying) "Pause" else "Play", tint = g0, modifier = Modifier.size(32.dp))
            }
            CircleIcon(Icons.Rounded.SkipNext, "Next", onNext, size = 48.dp, iconSize = 34.dp, tint = Color.White.copy(alpha = 0.85f))
            ModeButton(if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat", repeatMode != Player.REPEAT_MODE_OFF, onRepeat)
        }

        // 6. Volume
        Row(Modifier.padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircleIcon(
                if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp, if (muted) "Unmute" else "Mute",
                onMute, size = 32.dp, iconSize = 18.dp, tint = Color.White.copy(alpha = 0.6f)
            )
            Slider(
                value = volumeDrag ?: if (muted) 0f else volume,
                onValueChange = { volumeDrag = it; onVolume(it) },
                onValueChangeFinished = { volumeDrag = null },
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.2f))
            )
        }

        // 7. Extras — lyrics & sleep timer (PRD phases 7 & 9)
        Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            ExtraChip(Icons.Rounded.FormatQuote, "Lyrics", active = showLyrics) { showLyrics = !showLyrics }
            SleepTimerChip(sleep, onSleep)
        }
    }
}

/** The big album art: remounts per song, breathes with play state. */
@Composable
private fun PlayerArt(song: Song, isPlaying: Boolean) {
    val (_, g1) = song.gradient
    Box(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 16.dp), contentAlignment = Alignment.Center) {
        key(song.id) {
            var appeared by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { appeared = true }
            val artSpring = spring<Float>(dampingRatio = 0.71f, stiffness = 240f)
            val scale by animateFloatAsState(if (!appeared) 0.82f else if (isPlaying) 1f else 0.88f, artSpring, label = "artScale")
            val alpha by animateFloatAsState(if (appeared) 1f else 0f, artSpring, label = "artAlpha")
            val art = albumArtOf(song.id)
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                    .shadow(32.dp, RoundedCornerShape(24.dp), ambientColor = g1, spotColor = g1)
                    .clip(RoundedCornerShape(24.dp))
                    .background(gradientBrush(song.gradient)),
                contentAlignment = Alignment.Center
            ) {
                if (art != null) Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = 0.45f), modifier = Modifier.size(80.dp))
            }
        }
    }
}

/** Repeat button order: off → all → one. Media3 numbers them OFF=0, ONE=1, ALL=2, so this isn't just +1. */
internal fun nextRepeatMode(mode: Int) = when (mode) {
    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
    else -> Player.REPEAT_MODE_OFF
}

/** MediaStore rounds durations, so the position can run a little past the end; clamp for fillMaxWidth. */
internal fun progressOf(positionMs: Long, durationSec: Int) = (positionMs / (durationSec * 1000f)).coerceIn(0f, 1f)

@Composable
private fun ModeButton(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clip(CircleShape).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = Color.White.copy(alpha = if (active) 1f else 0.35f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(2.dp))
        Box(Modifier.size(4.dp).clip(CircleShape).background(if (active) Color.White else Color.Transparent))
    }
}

@Composable
private fun ExtraChip(icon: ImageVector, text: String, active: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(Color.White.copy(alpha = if (active) 0.25f else 0.10f)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(15.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.8f))
    }
}

/**
 * PRD phase 9 and T39: the lyrics, in the album art's place. Synced ones follow the song, plain ones just scroll,
 * and with none there is an honest note and, when the setting is on, the online lookup.
 */
@Composable
private fun LyricsView(song: Song, lyrics: String?, playerArtist: String?, position: () -> Long, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Anything found online before, so the same song needs no network a second time.
    var found by remember(song.id) { mutableStateOf<String?>(null) }
    var looking by remember(song.id) { mutableStateOf(false) }
    var miss by remember(song.id) { mutableStateOf<LyricsResult?>(null) }
    val shown = lyrics ?: found
    val lines = remember(shown) { shown?.let(::parseLrc).orEmpty() }
    // The best guess at who and what this is; for a tagless file that means the player's tag or the file name.
    val asked = remember(song, playerArtist) { lyricsQueries(song, playerArtist).first() }
    // Offline first: a .lrc beside the song, then anything found online before. Neither touches the network.
    LaunchedEffect(song.id) {
        if (lyrics == null) found = readLrc(context, song) ?: LyricsCache.get(context, song.id)
    }

    Box(modifier.padding(horizontal = 28.dp), contentAlignment = Alignment.CenterStart) {
        when {
            lines.isNotEmpty() -> SyncedLyrics(lines, song.id, position)
            shown != null -> Column(Modifier.fillMaxSize()) {
                Text(
                    "NOT SYNCED TO THE SONG", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                    color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp),
                )
                Text(
                    shown, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp, color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            }
            looking -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            else -> Column {
                Text(
                    when (miss) {
                        LyricsResult.Offline -> "Couldn't reach the lyrics service. Check your connection and try again."
                        // Names what was searched for, so a bad tag is visible rather than a silent miss. A file name
                        // was searched both ways round, so it's shown as it reads, not as a guessed artist and title.
                        LyricsResult.NotFound -> when {
                            asked.fromFileName -> "No lyrics found for “${asked.artist} - ${asked.title}”."
                            asked.artist != null -> "No lyrics found for “${asked.title}” by “${asked.artist}”."
                            else -> "No lyrics found for “${asked.title}”."
                        }
                        else -> "This file has no lyrics saved in it."
                    },
                    fontSize = 15.sp, color = Color.White.copy(alpha = 0.75f), lineHeight = 22.sp,
                )
                if (Settings[Key.OnlineLyrics]) {
                    TextButton({
                        looking = true
                        scope.launch {
                            // The only moment this app uses the network, and only because it was tapped.
                            val result = fetchLyrics(song, playerArtist)
                            looking = false
                            miss = result
                            if (result is LyricsResult.Found) {
                                found = result.text
                                LyricsCache.put(context, song.id, result.text)
                            }
                        }
                    }) { Text(if (miss == null) "Find lyrics online" else "Try again", fontWeight = FontWeight.Bold, color = Color.White) }
                } else {
                    Text(
                        "Harmonia only reads lyrics saved inside a file. You can switch on “Look up lyrics online” in Settings.",
                        fontSize = 13.sp, color = Color.White.copy(alpha = 0.55f), lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }
    }
}

/**
 * Lyrics that follow the song: the line being sung is lit and kept a third of the way down. Earlier and Later move
 * this song's timing in half seconds, and the app remembers it (see [Settings.lyricsShift]).
 */
@Composable
private fun SyncedLyrics(lines: List<LyricLine>, songId: Int, position: () -> Long) {
    val readPosition by rememberUpdatedState(position)
    var positionMs by remember { mutableLongStateOf(readPosition()) }
    LaunchedEffect(Unit) {
        while (true) {
            positionMs = readPosition()
            delay(50)
            withFrameMillis { } // no frames come while the app is in the background, so this loop rests too
        }
    }
    var shift by remember(songId) { mutableIntStateOf(Settings.lyricsShift(songId)) }
    fun moveBy(ms: Int) {
        shift += ms
        Settings.setLyricsShift(songId, shift)
    }
    // The clock ticks twenty times a second, but only a change of line gets past this and redraws anything.
    val current by remember(lines) { derivedStateOf { currentLine(lines, positionMs - shift) } }
    // Opens on the line being sung rather than scrolling down to it from the top.
    val listState = remember(lines) { LazyListState(current.coerceAtLeast(0)) }
    LaunchedEffect(current, listState) { listState.animateScrollToItem(current.coerceAtLeast(0)) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (shift != 0) Text("%+.1f s".format(shift / 1000f), fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            TimingButton("Earlier") { moveBy(-SHIFT_STEP_MS) }
            TimingButton("Later") { moveBy(SHIFT_STEP_MS) }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                Modifier.fillMaxSize(),
                state = listState,
                // Scrolling a line to the top puts it a third of the way down, with what's coming next below it.
                contentPadding = PaddingValues(top = maxHeight / 3, bottom = maxHeight * 2 / 3),
            ) {
                itemsIndexed(lines) { i, line ->
                    val alpha by animateFloatAsState(if (i == current) 1f else 0.4f, label = "lyricLine")
                    Text(
                        line.text.ifEmpty { "♪" }, // a pause in the singing
                        fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp, color = Color.White,
                        modifier = Modifier.padding(vertical = 8.dp).graphicsLayer { this.alpha = alpha },
                    )
                }
            }
        }
    }
}

private const val SHIFT_STEP_MS = 500

@Composable
private fun TimingButton(text: String, onClick: () -> Unit) {
    Text(
        text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.8f),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** PRD phase 7. [SLEEP_END_OF_TRACK] finishes the current song instead of counting minutes. */
enum class SleepOption(val label: String, val minutes: Int) {
    Min15("15 min", 15),
    Min30("30 min", 30),
    Min45("45 min", 45),
    Min60("60 min", 60),
    EndOfTrack("End of track", SLEEP_END_OF_TRACK),
}

/** The option the service reports it is running, or null for "Off" (0 means none, including "just fired"). */
internal fun sleepOptionOf(minutes: Int) = SleepOption.entries.firstOrNull { it.minutes == minutes }

@Composable
private fun SleepTimerChip(selected: SleepOption?, onSelect: (SleepOption?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        ExtraChip(Icons.Rounded.Bedtime, selected?.let { "Sleep · ${it.label}" } ?: "Sleep timer") { open = true }
        DropdownMenu(open, { open = false }, shape = RoundedCornerShape(16.dp), containerColor = colors.card) {
            SleepOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label, fontSize = 14.sp, color = colors.onSurface) },
                    onClick = { onSelect(option); open = false }
                )
            }
            DropdownMenuItem(
                text = { Text("Off", fontSize = 14.sp, color = colors.onSurfaceVariant) },
                onClick = { onSelect(null); open = false }
            )
        }
    }
}

