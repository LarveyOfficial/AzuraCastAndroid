package com.larvey.azuracastplayer

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class MusicPlayerService: MediaLibraryService() {

  /* This is the service side player, the media controller in the activity will control this one, so don't worry about it */
  lateinit var player: Player

  /* This is the session which will delegate everything you need about audio playback such as notifications, pausing player, resuming player, listening to states, etc */
  lateinit var session: MediaLibrarySession

  override fun onCreate() {
    super.onCreate()

    /* Step 1 out of 2: Instantiate the player (ExoPlayer) */
    player = ExoPlayer.Builder(applicationContext)
      .setRenderersFactory(
        DefaultRenderersFactory(this).setExtensionRendererMode(
          DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER /* We prefer extensions, such as FFmpeg */
        )
      ).build()


    /* Step 2 out of 2: Instantiate the session (most important part) */
    session = MediaLibrarySession.Builder(this, player,
      object: MediaLibrarySession.Callback {
        override fun onAddMediaItems(
          mediaSession: MediaSession,
          controller: MediaSession.ControllerInfo,
          mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {

          /* This is the trickiest part, if you don't do this here, nothing will play */
          val updatedMediaItems = mediaItems.map { it.buildUpon().setUri(it.mediaId).build() }.toMutableList()
          return Futures.immediateFuture(updatedMediaItems)
        }
      }).build()
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
    return session
  }


}