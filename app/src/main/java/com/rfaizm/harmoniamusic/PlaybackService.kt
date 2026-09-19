package com.rfaizm.harmoniamusic

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Intent
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        player.addListener(object : Player.Listener {
            // Counted here, not in the UI, so tracks that advance in the background count too.
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                item?.mediaId?.toIntOrNull()?.let(Library.songs::countPlay)
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

fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toLong()))
    // Cleaned tags, so the notification and lock screen match the app.
    .setMediaMetadata(MediaMetadata.Builder().setTitle(displayTitle).setArtist(displayArtist).setAlbumTitle(album).build())
    .build()
