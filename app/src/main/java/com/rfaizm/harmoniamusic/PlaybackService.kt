package com.rfaizm.harmoniamusic

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioManager.STREAM_MUSIC
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.rfaizm.harmoniamusic.data.Library
import com.rfaizm.harmoniamusic.data.Settings
import com.rfaizm.harmoniamusic.data.Settings.Key
import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.data.countPlay
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Owns the player so music keeps going with the app closed. Media3 draws the notification and lock-screen
 * controls and routes Bluetooth buttons; the UI talks to it through a MediaController.
 */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private lateinit var player: ExoPlayer
    private var errorsInARow = 0
    private var pausedByUnplug = false
    private val handler = Handler(Looper.getMainLooper())
    private val audio by lazy { getSystemService(AudioManager::class.java) } // no context before onCreate
    private val settings by lazy { getSharedPreferences(Settings.FILE, MODE_PRIVATE) }
    private val settingsWatcher = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> applySettings() }

    // Earphones back in (wired, USB-C, Bluetooth, anything): the sound no longer goes to the bare speaker,
    // so drop the 30% cap. Matching on device types missed USB-C sets and headsets that reconnect late.
    private val outputWatcher = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            if (addedDevices.any { it.isSink }) pausedByUnplug = false
        }
    }

    @OptIn(UnstableApi::class) // DefaultExtractorsFactory, setMaxSeekToPreviousPositionMs, setDeviceVolumeControlEnabled
    override fun onCreate() {
        super.onCreate()
        // Voice recorders often write ADTS .aac or .amr, which have no seek index; without this they can't be seeked.
        val extractors = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
        Settings.init(this)
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this, extractors))
            // PRD phase 4: pause for calls and other music apps; notifications duck or pause per Settings.
            .setAudioAttributes(audioAttributesFor(Settings[Key.Ducking]), true)
            .setHandleAudioBecomingNoisy(Settings[Key.PauseOnUnplug])
            .setMaxSeekToPreviousPositionMs(5_000) // PRD phase 7: previous after 5 s restarts the song
            // Lets the controller read the phone's media volume, so hardware keys move the slider.
            .setDeviceVolumeControlEnabled(true)
            .build()
        audio.registerAudioDeviceCallback(outputWatcher, handler)
        settings.registerOnSharedPreferenceChangeListener(settingsWatcher)
        player.addListener(object : Player.Listener {
            // Counted here, not in the UI, so tracks that advance in the background count too.
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                item?.mediaId?.toIntOrNull()?.let {
                    Library.songs.countPlay(it)
                    Library.save(this@PlaybackService)
                }
            }

            // PRD phase 2: a corrupt or missing file skips ahead instead of stalling the queue.
            override fun onPlayerError(error: PlaybackException) {
                val title = player.currentMediaItem?.mediaMetadata?.title
                Toast.makeText(this@PlaybackService, title?.let { "Couldn't play “$it”" } ?: "Couldn't play this song", Toast.LENGTH_SHORT).show()
                errorsInARow++
                if (shouldSkipAfterError(errorsInARow, player.mediaItemCount, player.hasNextMediaItem())) {
                    player.seekToNextMediaItem()
                    player.prepare() // an error leaves the player idle; playWhenReady is still set, so it plays
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) {
                    pausedByUnplug = reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY
                    return
                }
                if (reason != Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) return
                // PRD phase 4: play pressed after the earphones came out shouldn't blast the speaker. Android keeps a
                // separate volume per output, so this lowers only the speaker's; headphones keep theirs.
                if (pausedByUnplug) {
                    audio.setStreamVolume(STREAM_MUSIC, speakerSafeVolume(audio.getStreamVolume(STREAM_MUSIC), audio.getStreamMaxVolume(STREAM_MUSIC)), 0)
                }
                pausedByUnplug = false
                // Resuming mid-song fades in over 1 s; a song started from 0 plays at full volume straight away.
                if (player.currentPosition > 0) fadeIn()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) errorsInARow = 0
            }

            // A new queue from the user starts a fresh count, even if the last one ended all-broken.
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) errorsInARow = 0
            }
        })
        // Same intent as the launcher icon, so tapping the notification brings back the running task
        // instead of stacking a second activity on it.
        val launch = Intent(this, MainActivity::class.java).setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        session = MediaSession.Builder(this, player)
            .setSessionActivity(PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE))
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    /** Settings can change mid-playback, so both audio switches are re-applied to the live player. */
    @OptIn(UnstableApi::class) // setAudioAttributes
    private fun applySettings() {
        player.setAudioAttributes(audioAttributesFor(Settings[Key.Ducking]), true)
        player.setHandleAudioBecomingNoisy(Settings[Key.PauseOnUnplug])
    }

    /** Ramps the volume up over 1 s. A Handler, not ValueAnimator: animators tick on display frames, which aren't guaranteed with the screen off. */
    private fun fadeIn() {
        handler.removeCallbacksAndMessages(null)
        val start = SystemClock.uptimeMillis()
        object : Runnable {
            override fun run() {
                val t = ((SystemClock.uptimeMillis() - start) / 1_000f).coerceAtMost(1f)
                player.volume = t * t // squared: the ear hears loudness logarithmically, so a linear ramp jumps early
                if (t < 1f) handler.postDelayed(this, 50)
            }
        }.run() // first step now, so the volume is 0 before the resumed audio is heard
    }

    override fun onDestroy() {
        settings.unregisterOnSharedPreferenceChangeListener(settingsWatcher)
        audio.unregisterAudioDeviceCallback(outputWatcher)
        handler.removeCallbacksAndMessages(null)
        session?.release()
        player.release()
        session = null
        super.onDestroy()
    }
}

/**
 * Skip past a broken song unless it was the last one, or every song in the queue has now failed in a row
 * (repeat-all always has a next song, so the count is what stops an all-broken queue from looping forever).
 */
internal fun shouldSkipAfterError(errorsInARow: Int, queueSize: Int, hasNext: Boolean) = hasNext && errorsInARow < queueSize

/**
 * Settings › "Lower volume for notifications". Media3 ducks for every content type except speech, where it
 * pauses instead, so the content type is the switch (tests pin this; it isn't obvious from the API).
 */
internal fun audioAttributesFor(ducking: Boolean): AudioAttributes = AudioAttributes.Builder()
    .setUsage(C.USAGE_MEDIA)
    .setContentType(if (ducking) C.AUDIO_CONTENT_TYPE_MUSIC else C.AUDIO_CONTENT_TYPE_SPEECH)
    .build()

/** PRD phase 4: after an unplug the speaker plays at no more than 30%; a quieter setting is left alone. */
internal fun speakerSafeVolume(current: Int, max: Int) = min(current, (max * 0.3).roundToInt())

fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toLong()))
    // Cleaned tags, so the notification and lock screen match the app.
    .setMediaMetadata(MediaMetadata.Builder().setTitle(displayTitle).setArtist(displayArtist).setAlbumTitle(album).build())
    .build()
