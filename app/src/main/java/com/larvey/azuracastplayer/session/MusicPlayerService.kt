package com.larvey.azuracastplayer.session

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_RADIO_STATION
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.larvey.azuracastplayer.AppSetup
import com.larvey.azuracastplayer.R
import com.larvey.azuracastplayer.classes.models.NowPlayingData
import com.larvey.azuracastplayer.classes.models.SavedStationsDB
import com.larvey.azuracastplayer.ui.mainActivity.MainActivity


class MusicPlayerService() : MediaLibraryService() {

  lateinit var nowPlaying: NowPlayingData

  lateinit var savedStationsDB: SavedStationsDB

  private var mediaSession: MediaLibrarySession? = null

  // Create your player and media session in the onCreate lifecycle event
  @OptIn(UnstableApi::class)
  override fun onCreate() {
    super.onCreate()

    nowPlaying = (applicationContext as AppSetup).nowPlayingData

    savedStationsDB = (applicationContext as AppSetup).savedStationsDB

    val player = object : ForwardingPlayer(
      ExoPlayer.Builder(this).setAudioAttributes(
        AudioAttributes.DEFAULT,
        true
      ).build()
    ) {
      override fun play() {
        super.play()
        super.seekToDefaultPosition()
      }

      override fun stop() {
        super.stop()
        nowPlaying.nowPlayingShortCode.value = ""
        nowPlaying.nowPlayingURI.value = ""
        nowPlaying.nowPlayingURL.value = ""
      }

    }

    player.addListener(object : Player.Listener {
      override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        if (nowPlaying.nowPlayingURL.value != "" && nowPlaying.nowPlayingShortCode.value != "") {
          nowPlaying.setMediaMetadata(
            nowPlaying.nowPlayingURL.value,
            nowPlaying.nowPlayingShortCode.value,
            player
          )
        }
      }
    })

    val mediaNotificationProvider = DefaultMediaNotificationProvider(this)

    mediaNotificationProvider.setSmallIcon(R.drawable.azuracast)


    mediaSession = MediaLibrarySession.Builder(
      this,
      player,
      MyCallback()
    )
      .also { builder ->
        getSingleTopActivity()?.let { builder.setSessionActivity(it) }
      }

      .build()
    setMediaNotificationProvider(mediaNotificationProvider)

    mediaSession?.setCustomLayout(
      listOf(
        CommandButton.Builder(CommandButton.ICON_STOP).setDisplayName("Stop").setSessionCommand(
          SessionCommand(
            "STOP_RADIO",
            Bundle.EMPTY
          )
        ).build()
      )
    )
  }

  @UnstableApi
  private inner class MyCallback() : MediaLibrarySession.Callback {
    @OptIn(UnstableApi::class)
    override fun onConnect(
      session: MediaSession,
      controller: MediaSession.ControllerInfo
    ): ConnectionResult {
      val connectionResult = super.onConnect(
        session,
        controller
      )
      val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
      availableSessionCommands.add(
        SessionCommand(
          "STOP_RADIO",
          Bundle.EMPTY
        )
      )

      return ConnectionResult.accept(
        availableSessionCommands.build(),
        Player.Commands.Builder().addAllCommands().remove(Player.COMMAND_GET_TIMELINE).build()
      )

    }

    override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
      session.setCustomLayout(
        controller,
        listOf(
          CommandButton.Builder(CommandButton.ICON_STOP).setDisplayName("Stop").setSessionCommand(
            SessionCommand(
              "STOP_RADIO",
              Bundle.EMPTY
            )
          ).build()
        )
      )
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
        session.player.clearMediaItems()
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
      mediaSessiona: MediaSession,
      controller: MediaSession.ControllerInfo,
      mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> {

      val item = (applicationContext as AppSetup).savedStations.filter {
        it.url == Uri.parse(mediaItems[0].mediaId).host && it.shortcode == Uri.parse(mediaItems[0].mediaId).pathSegments[1]
      }


      /* This is the trickiest part, if you don't do this here, nothing will play */
      val updatedMediaItems =
        mediaItems.map { it.buildUpon().setUri(it.mediaId).build() }.toMutableList()
      nowPlaying.nowPlayingShortCode.value = item[0].shortcode
      nowPlaying.nowPlayingURL.value = item[0].url
      nowPlaying.nowPlayingURI.value = updatedMediaItems[0].mediaId.toString()
      return Futures.immediateFuture(updatedMediaItems)
    }

    override fun onGetLibraryRoot(
      session: MediaLibrarySession, browser: MediaSession.ControllerInfo, params: LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {
      return Futures.immediateFuture(
        LibraryResult.ofItem(
          MediaItem.Builder()
            .setMediaId("/")
            .setMediaMetadata(
              MediaMetadata.Builder()
                .setIsBrowsable(false)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .setTitle("AzuraCast Player")
                .build()
            )
            .build(),
          params
        )
      )
    }

    override fun onGetChildren(
      session: MediaLibrarySession, browser: MediaSession.ControllerInfo, parentId: String,
      page: Int, pageSize: Int, params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {

      if (parentId != "/") {

        val metaData = MediaMetadata.Builder()
          .setIsBrowsable(true)
          .setIsPlayable(false)
          .setTitle("Radio Stations")
          .build()

        return Futures.immediateFuture(
          LibraryResult.ofItemList(
            listOf(MediaItem.Builder().setMediaId("Stations").setMediaMetadata(metaData).build()),
            params
          )
        )
      }
      var stations = mutableListOf<MediaItem>()


      for (item in (applicationContext as AppSetup).savedStations) {
        val metaData = MediaMetadata.Builder()
          .setTitle(item.name)
          .setSubtitle(item.shortcode)
          .setDescription(item.url)
          .setMediaType(MEDIA_TYPE_RADIO_STATION)
          .setDurationMs(1)
          .setIsBrowsable(false)
          .setIsPlayable(true)
          .build()

        val mediaItem = MediaItem.Builder()
          .setMediaId(item.defaultMount)
          .setMediaMetadata(metaData)
          .build()

        stations.add(mediaItem)
      }

      return Futures.immediateFuture(
        LibraryResult.ofItemList(
          stations,
          params
        )
      )
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
      player.stop()
      player.release()
      release()
      mediaSession = null
    }
    super.onDestroy()
  }

  private fun getSingleTopActivity(): PendingIntent? {
    return PendingIntent.getActivity(
      applicationContext,
      0,
      Intent(
        applicationContext,
        MainActivity::class.java
      ),
      PendingIntent.FLAG_IMMUTABLE
    )
  }


  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
    mediaSession
}