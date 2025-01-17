package com.larvey.azuracastplayer

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSession.ConnectionResult.AcceptedResultBuilder
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture


class MusicPlayerService : MediaSessionService() {
  private var mediaSession: MediaSession? = null

  // Create your player and media session in the onCreate lifecycle event
  @OptIn(UnstableApi::class)
  override fun onCreate() {
    super.onCreate()

    val stopButton = CommandButton.Builder()
      .setDisplayName("Stop")
      .setIconResId(R.drawable.stop_button)
      .setSessionCommand(SessionCommand("STOP_RADIO", Bundle()))
      .build()


    val player = object : ForwardingPlayer(ExoPlayer.Builder(this).build()) {
      override fun play() {
        Log.d("DEBUG", "SEEKING")
        super.seekToDefaultPosition()
        super.play()
      }
    }
    mediaSession = MediaSession.Builder(this, player)
      .setCallback(MyCallback())
      .setCustomLayout(ImmutableList.of(stopButton))
      . also { builder ->
        getSingleTopActivity()?.let{ builder.setSessionActivity(it) }
      }
      .build()
  }

  @UnstableApi
  private inner class MyCallback : MediaSession.Callback {
    @OptIn(UnstableApi::class)
    override fun onConnect(
      session: MediaSession,
      controller: MediaSession.ControllerInfo
    ): ConnectionResult {
      // Set available player and session commands.
      return AcceptedResultBuilder(session)
        .setAvailablePlayerCommands(
          ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
            .build()
        )
        .setAvailableSessionCommands(
          ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
            .add(SessionCommand("STOP_RADIO", Bundle.EMPTY))
            .build()
        )
        .build()
    }

    @OptIn(UnstableApi::class)
    override fun onCustomCommand(
      session: MediaSession,
      controller: MediaSession.ControllerInfo,
      customCommand: SessionCommand,
      args: Bundle
    ): ListenableFuture<SessionResult> {
      if (customCommand.customAction == "STOP_RADIO") {
        session.player.stop()
        return Futures.immediateFuture(
          SessionResult(SessionResult.RESULT_SUCCESS)
        )
      }
      return Futures.immediateFuture(
        SessionResult(SessionError.INFO_CANCELLED)
      )
    }

    @OptIn(UnstableApi::class)
    override fun onAddMediaItems(
      mediaSession: MediaSession,
      controller: MediaSession.ControllerInfo,
      mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> {

      /* This is the trickiest part, if you don't do this here, nothing will play */
      val updatedMediaItems = mediaItems.map { it.buildUpon().setUri(it.mediaId).build() }.toMutableList()
      return Futures.immediateFuture(updatedMediaItems)
    }

  }
  // The user dismissed the app from the recent tasks
  override fun onTaskRemoved(rootIntent: Intent?) {
    val player = mediaSession?.player!!
    player.stop()
    player.release()
    stopSelf()
  }

  // Remember to release the player and media session in onDestroy
  override fun onDestroy() {
    mediaSession?.run {
      player.release()
      release()
      mediaSession = null
    }
    super.onDestroy()
  }

  private fun getSingleTopActivity(): PendingIntent? {
    return PendingIntent.getActivity(
      applicationContext, 0,
      Intent(applicationContext, MainActivity::class.java),
      PendingIntent.FLAG_IMMUTABLE)
  }


  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
    mediaSession
}