package com.rfaizm.harmoniamusic.ui

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.AudioManager.STREAM_MUSIC
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.rfaizm.harmoniamusic.LYRICS
import com.rfaizm.harmoniamusic.PlaybackService
import com.rfaizm.harmoniamusic.SLEEP_ADJUST_MS
import com.rfaizm.harmoniamusic.SLEEP_AT
import com.rfaizm.harmoniamusic.SLEEP_COMMAND
import com.rfaizm.harmoniamusic.SLEEP_MINUTES
import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.toMediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

/** What the mini and full player show, mirrored from the playback service through its MediaController. */
data class PlaybackState(
    val activeId: Int? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    /** The phone's media volume, 0..1. */
    val volume: Float = 0f,
    val muted: Boolean = false,
    /** The service's timer, so the chip goes back to "Sleep timer" by itself once it fires. */
    val sleep: SleepTimer? = null,
    /** Read from the song's tags by the service; track metadata never reaches a controller on its own. */
    val lyrics: String? = null,
    /** The artist the player read from the file, which MediaStore sometimes misses (SPEC.md S2). */
    val playerArtist: String? = null,
)

/**
 * The player's side of the screen (M6, T33): the connection to [PlaybackService], the state it reports, and every
 * command the mini and full player send. A ViewModel outlives rotation, so the mini player doesn't flash away while
 * the controller reconnects. Playback itself lives in the service, which keeps playing after [disconnect].
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(@ApplicationContext private val context: Context) : ViewModel() {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var connecting: ListenableFuture<MediaController>? = null
    private var ticking: Job? = null
    // Media3 controllers drop device-volume commands during local playback, so volume is set through AudioManager;
    // the new level comes back through sync(), same as a hardware key press.
    private val audio = context.getSystemService(AudioManager::class.java)

    /** Called while the app is visible. */
    fun connect() {
        if (connecting != null) return
        val future = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java)))
            .setListener(object : MediaController.Listener {
                override fun onExtrasChanged(controller: MediaController, extras: Bundle) = readExtras(extras)
            })
            .buildAsync()
        connecting = future
        future.addListener({
            // Cancelled or already released if the app was stopped before the connection finished.
            val c = runCatching { future.get() }.getOrNull()?.takeIf { it.isConnected } ?: return@addListener
            c.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) = sync(c)
            })
            controller = c
            sync(c)
            readExtras(c.sessionExtras) // a timer may already be running
        }, ContextCompat.getMainExecutor(context))
    }

    /** Called when the app is no longer visible; the last state stays on show until [connect]. */
    fun disconnect() {
        controller = null
        ticking?.cancel()
        connecting?.let(MediaController::releaseFuture)
        connecting = null
    }

    override fun onCleared() = disconnect()

    private fun sync(c: MediaController) {
        _state.update {
            it.copy(
                activeId = c.currentMediaItem?.mediaId?.toIntOrNull(),
                isPlaying = !Util.shouldShowPlayButton(c),
                positionMs = c.currentPosition,
                shuffle = c.shuffleModeEnabled,
                repeatMode = c.repeatMode,
                volume = c.deviceVolume / c.deviceInfo.maxVolume.coerceAtLeast(1).toFloat(),
                muted = c.isDeviceMuted,
                playerArtist = c.mediaMetadata.artist?.toString(),
            )
        }
        if (_state.value.isPlaying) tick()
    }

    private fun readExtras(extras: Bundle) = _state.update {
        it.copy(sleep = sleepTimerOf(extras.getInt(SLEEP_MINUTES), extras.getLong(SLEEP_AT)), lyrics = extras.getString(LYRICS))
    }

    // Seeks, track changes and pauses arrive through sync(); only the steady progress has to be polled.
    private fun tick() {
        if (ticking?.isActive == true) return
        ticking = viewModelScope.launch {
            while (true) {
                val c = controller ?: break
                if (Util.shouldShowPlayButton(c)) break
                _state.update { it.copy(positionMs = c.currentPosition) }
                delay(500)
            }
        }
    }

    /** The player's clock right now, for synced lyrics, which need it more often than the half-second tick. */
    fun position(): Long = controller?.currentPosition ?: 0L

    /** Next and previous walk the list the song was tapped from: the sorted Songs tab, a playlist, or Liked. */
    fun playFrom(song: Song, list: List<Song>) {
        val c = controller ?: return
        c.setMediaItems(list.map { it.toMediaItem() }, list.indexOfFirst { it.id == song.id }, 0)
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        Util.handlePlayPauseButtonAction(controller)
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    fun seekTo(fraction: Float, durationSec: Int) {
        controller?.seekTo((fraction * durationSec * 1000).toLong())
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        controller?.let { it.repeatMode = nextRepeatMode(it.repeatMode) }
    }

    fun setVolume(fraction: Float) {
        if (audio.isStreamMute(STREAM_MUSIC)) audio.adjustStreamVolume(STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
        audio.setStreamVolume(STREAM_MUSIC, (fraction * audio.getStreamMaxVolume(STREAM_MUSIC)).roundToInt(), 0)
    }

    fun toggleMute() = audio.adjustStreamVolume(STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, 0)

    // No local echo for the sleep timer: the chip changes when the service confirms, and only it knows when it ends.
    fun setSleep(option: SleepOption?) = sendSleep(bundleOf(SLEEP_MINUTES to (option?.minutes ?: 0)))

    fun adjustSleep(deltaMs: Long) = sendSleep(bundleOf(SLEEP_ADJUST_MS to deltaMs))

    private fun sendSleep(args: Bundle) {
        controller?.sendCustomCommand(SessionCommand(SLEEP_COMMAND, Bundle.EMPTY), args)
    }

    /** T16: the song's file is gone, so take it out of the queue; Media3 moves on to the next song by itself. */
    fun dropFromQueue(songId: Int) {
        val c = controller ?: return
        (c.mediaItemCount - 1 downTo 0)
            .filter { c.getMediaItemAt(it).mediaId == songId.toString() }
            .forEach(c::removeMediaItem)
    }
}
