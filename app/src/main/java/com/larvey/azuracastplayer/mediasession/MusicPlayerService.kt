package com.larvey.azuracastplayer.mediasession

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
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
import com.larvey.azuracastplayer.MainActivity
import com.larvey.azuracastplayer.R


class MusicPlayerService : MediaSessionService() {

  private var _mediaSession: MediaSession? = null
  private val mediaSession get() = _mediaSession!!

  companion object {
    private const val NOTIFICATION_ID = 123
    private const val CHANNEL_ID = "session_notification_channel_id"
    private val immutableFlag = if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
  }

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
        super.play()
        super.seekToDefaultPosition()
      }
    }


    _mediaSession = MediaSession.Builder(this, player)
      .setCallback(MyCallback())
      .setCustomLayout(ImmutableList.of(stopButton))
      . also { builder ->
        getSingleTopActivity()?.let { builder.setSessionActivity(it) }
      }
      .build()

    setListener(MediaSessionServiceListener())

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
  @OptIn(UnstableApi::class)
  override fun onDestroy() {
    mediaSession?.run {
      getBackStackedActivity()?.let{setSessionActivity(it)}
      player.release()
      release()
      _mediaSession = null
    }

    clearListener()

    super.onDestroy()
  }

  private fun getSingleTopActivity(): PendingIntent? {
    return PendingIntent.getActivity(
      applicationContext, 0,
      Intent(applicationContext, MainActivity::class.java),
      PendingIntent.FLAG_IMMUTABLE)
  }

  private fun getBackStackedActivity(): PendingIntent? {
    return TaskStackBuilder.create(this).run {
      addNextIntent(Intent(this@MusicPlayerService, MainActivity::class.java))
      getPendingIntent(0, immutableFlag or PendingIntent.FLAG_UPDATE_CURRENT)
    }
  }

  @OptIn(UnstableApi::class) // MediaSessionService.Listener
  private inner class MediaSessionServiceListener : Listener {

    /**
     * This method is only required to be implemented on Android 12 or above when an attempt is made
     * by a media controller to resume playback when the {@link MediaSessionService} is in the
     * background.
     */
    override fun onForegroundServiceStartNotAllowedException() {
      if (
        Build.VERSION.SDK_INT >= 33 &&
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
      ) {
        // Notification permission is required but not granted
        return
      }
      val notificationManagerCompat = NotificationManagerCompat.from(this@MusicPlayerService)
      ensureNotificationChannel(notificationManagerCompat)
      val builder =
        NotificationCompat.Builder(this@MusicPlayerService, CHANNEL_ID)
          .setSmallIcon(androidx.media3.session.R.drawable.media3_notification_small_icon)
          .setContentTitle("Playback cannot be resumed")
          .setStyle(
            NotificationCompat.BigTextStyle().bigText("Press on the play button on the media notification if it\n" +
                "    is still present, otherwise please open the app to start the playback and re-connect the session\n" +
                "    to the controller")
          )
          .setPriority(NotificationCompat.PRIORITY_DEFAULT)
          .setAutoCancel(true)
          .also { builder -> getBackStackedActivity()?.let { builder.setContentIntent(it) } }
      notificationManagerCompat.notify(NOTIFICATION_ID, builder.build())
    }
  }

  private fun ensureNotificationChannel(notificationManagerCompat: NotificationManagerCompat) {
    if (
      Build.VERSION.SDK_INT < 26 ||
      notificationManagerCompat.getNotificationChannel(CHANNEL_ID) != null
    ) {
      return
    }

    val channel =
      NotificationChannel(
        CHANNEL_ID,
        "Playback cannot be resumed",
        NotificationManager.IMPORTANCE_DEFAULT
      )
    notificationManagerCompat.createNotificationChannel(channel)
  }


  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
    mediaSession
}