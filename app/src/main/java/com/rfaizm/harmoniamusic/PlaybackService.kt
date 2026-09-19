package com.rfaizm.harmoniamusic

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Intent
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.OptIn
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
import com.rfaizm.harmoniamusic.data.Song
import com.rfaizm.harmoniamusic.data.countPlay

/**
 * Owns the player so music keeps going with the app closed. Media3 draws the notification and lock-screen
 * controls and routes Bluetooth buttons; the UI talks to it through a MediaController.
 */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private var errorsInARow = 0

    @OptIn(UnstableApi::class) // DefaultExtractorsFactory, setDeviceVolumeControlEnabled
    override fun onCreate() {
        super.onCreate()
        // Voice recorders often write ADTS .aac or .amr, which have no seek index; without this they can't be seeked.
        val extractors = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this, extractors))
            // Lets the controller read the phone's media volume, so hardware keys move the slider.
            .setDeviceVolumeControlEnabled(true)
            .build()
        player.addListener(object : Player.Listener {
            // Counted here, not in the UI, so tracks that advance in the background count too.
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                item?.mediaId?.toIntOrNull()?.let(Library.songs::countPlay)
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

    override fun onDestroy() {
        session?.run { player.release(); release() }
        session = null
        super.onDestroy()
    }
}

/**
 * Skip past a broken song unless it was the last one, or every song in the queue has now failed in a row
 * (repeat-all always has a next song, so the count is what stops an all-broken queue from looping forever).
 */
internal fun shouldSkipAfterError(errorsInARow: Int, queueSize: Int, hasNext: Boolean) = hasNext && errorsInARow < queueSize

fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toLong()))
    // Cleaned tags, so the notification and lock screen match the app.
    .setMediaMetadata(MediaMetadata.Builder().setTitle(displayTitle).setArtist(displayArtist).setAlbumTitle(album).build())
    .build()
