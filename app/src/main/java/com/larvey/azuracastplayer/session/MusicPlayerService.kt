package com.larvey.azuracastplayer.session

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_RADIO_STATION
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
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
import com.larvey.azuracastplayer.classes.data.DiscoveryJSON
import com.larvey.azuracastplayer.classes.data.DiscoveryStation
import com.larvey.azuracastplayer.classes.models.NowPlayingData
import com.larvey.azuracastplayer.classes.models.SavedStationsDB
import com.larvey.azuracastplayer.classes.models.SharedMediaController
import com.larvey.azuracastplayer.db.settings.AndroidAutoLayouts
import com.larvey.azuracastplayer.session.sleepTimer.AndroidAlarmScheduler
import com.larvey.azuracastplayer.session.sleepTimer.SleepItem
import com.larvey.azuracastplayer.ui.mainActivity.MainActivity
import com.larvey.azuracastplayer.utils.fixHttps
import com.larvey.azuracastplayer.utils.resourceToUri
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.URL
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class MusicPlayerService : MediaLibraryService() {

  @Inject
  lateinit var nowPlaying: NowPlayingData

  @Inject
  lateinit var savedStationsDB: SavedStationsDB

  @Inject
  lateinit var sharedMediaController: SharedMediaController

  @Inject
  lateinit var discoveryJSON: MutableState<DiscoveryJSON?>

  lateinit var savedChildrenStyle: AndroidAutoLayouts


  var mediaSession: MediaLibrarySession? = null

  private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      Log.d(
        "DEBUG",
        "Yo, I got the msg"
      )
      mediaSession?.player?.stop()
    }
  }

  @OptIn(UnstableApi::class)
  override fun onCreate() {
    super.onCreate()

    val userPreferences = (application as AppSetup).userPreferences

    val job = SupervisorJob()
    val scope = CoroutineScope(Dispatchers.IO + job)

    scope.launch { savedChildrenStyle = userPreferences.androidAutoLayoutFlow.first() }


    val isSleeping = sharedMediaController.isSleeping

    val filter = IntentFilter("com.larvey.azuracastplayer.session.MusicPlayerService.SLEEP")
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      registerReceiver(
        receiver,
        filter,
        RECEIVER_NOT_EXPORTED
      )
    } else {
      registerReceiver(
        receiver,
        filter
      )
    }

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
        nowPlaying.nowPlayingMount.value = ""
        nowPlaying.nowPlayingURL.value = ""
        val scheduler = AndroidAlarmScheduler(applicationContext)
        SleepItem(LocalDateTime.now()).let(scheduler::cancel)
        isSleeping.value = false
        super.clearMediaItems()
      }

      override fun getCurrentPosition(): Long {
        if (nowPlaying.staticData.value?.nowPlaying?.playedAt != null) {
          if ((System.currentTimeMillis() / 1000) < nowPlaying.staticData.value?.nowPlaying?.playedAt!!) {
            return (nowPlaying.staticData.value?.nowPlaying?.playedAt!!.minus(nowPlaying.staticData.value?.nowPlaying?.playedAt!!) * 1000) - if (super.isCurrentMediaItemDynamic()) super.getCurrentPosition() else 0 // System Time sync issue. Trying to prevent negative time elapsed
          }
          return ((System.currentTimeMillis() / 1000).minus(nowPlaying.staticData.value?.nowPlaying?.playedAt!!) * 1000) - if (super.isCurrentMediaItemDynamic()) super.getCurrentPosition() else 0
        }
        return super.getCurrentPosition()
      }
    }

    player.addListener(object : Player.Listener {
      override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        if (nowPlaying.nowPlayingURL.value != "" && nowPlaying.nowPlayingShortCode.value != "") {
          Log.d(
            "DEBUG-FUNKEY",
            nowPlaying.nowPlayingURL.value
          )
          nowPlaying.setMediaMetadata(
            nowPlaying.nowPlayingURL.value,
            nowPlaying.nowPlayingShortCode.value,
            player
          )
        }
      }

      override fun onPlayerError(error: PlaybackException) {
        Log.d(
          "DEBUG",
          "Player Error ${error.errorCode}"
        )

        val badConnections = listOf(
          PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
          PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
          PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW,
          PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
        )
        if (error.errorCode in badConnections) {
          Log.d(
            "DEBUG",
            "Connection issue, going to keep trying to play"
          )
          player.seekToDefaultPosition()
          player.prepare()
        }
      }
    })

    val mediaNotificationProvider = DefaultMediaNotificationProvider(this)

    mediaNotificationProvider.setSmallIcon(R.drawable.azuracast)


    mediaSession = MediaLibrarySession.Builder(
      this,
      player,
      MyCallback()
    ).also { builder ->
      getSingleTopActivity()?.let { builder.setSessionActivity(it) }
    }

      .build()
    setMediaNotificationProvider(mediaNotificationProvider)

    mediaSession?.setCustomLayout(
      listOf(
        CommandButton.Builder(CommandButton.ICON_STOP)
          .setDisplayName("Stop")
          .setSessionCommand(
            SessionCommand(
              "STOP_RADIO",
              Bundle.EMPTY
            )
          )
          .build()
      )
    )

    mediaSession?.let {
      sharedMediaController.mediaSession.value = mediaSession
    }
  }

  @UnstableApi
  private inner class MyCallback : MediaLibrarySession.Callback {
    @OptIn(UnstableApi::class)
    override fun onConnect(
      session: MediaSession, controller: MediaSession.ControllerInfo
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
        .add(SessionCommand.COMMAND_CODE_LIBRARY_SUBSCRIBE)
        .add(SessionCommand.COMMAND_CODE_LIBRARY_GET_CHILDREN)
        .add(SessionCommand.COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT)
        .add(SessionCommand.COMMAND_CODE_LIBRARY_GET_ITEM)

      return ConnectionResult.accept(
        availableSessionCommands.build(),
        Player.Commands.Builder()
          .addAllCommands()
          .remove(Player.COMMAND_GET_TIMELINE)
          .remove(Player.COMMAND_SEEK_BACK)
          .remove(Player.COMMAND_SEEK_FORWARD)
          .remove(Player.COMMAND_SEEK_TO_NEXT)
          .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
          .remove(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
          .build()
      )
    }

    override fun onPostConnect(
      session: MediaSession,
      controller: MediaSession.ControllerInfo
    ) {
      session.setCustomLayout(
        controller,
        listOf(
          CommandButton.Builder(CommandButton.ICON_STOP)
            .setDisplayName("Stop")
            .setSessionCommand(
              SessionCommand(
                "STOP_RADIO",
                Bundle.EMPTY
              )
            )
            .build()
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
        return Futures.immediateFuture(
          SessionResult(SessionResult.RESULT_SUCCESS)
        )
      }
      return Futures.immediateFuture(
        SessionResult(SessionError.INFO_CANCELLED)
      )
    }

    override fun onSubscribe(
      session: MediaLibrarySession,
      browser: MediaSession.ControllerInfo,
      parentId: String,
      params: LibraryParams?
    ): ListenableFuture<LibraryResult<Void>> {
      return Futures.immediateFuture(LibraryResult.ofVoid(params))
    }

    @OptIn(UnstableApi::class)
    override fun onAddMediaItems(
      mediaSession: MediaSession,
      controller: MediaSession.ControllerInfo,
      mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> {

      Log.d(
        "DEBUG-SEARCH2PLAY",
        mediaItems[0].requestMetadata.searchQuery.toString()
      )

      if (mediaItems[0].requestMetadata.searchQuery != null) {
        val foundStations = mutableListOf<MediaItem>()
        var url = ""
        var shortCode = ""
        //This is stupid but Google Assistant is stupider
        val searchQuery = mediaItems[0].requestMetadata.searchQuery!!.lowercase()
          .replace(
            " ",
            ""
          )
          .replace(
            "onazuracastradio",
            ""
          )
          .replace(
            "onazurecastradio",
            ""
          )
          .replace(
            "onazuracastplayer",
            ""
          )
          .replace(
            "onazurecastplayer",
            ""
          )
          .replace(
            "onazuracast",
            ""
          )
          .replace(
            "onazurecast",
            ""
          )
        savedStationsDB.savedStations.value?.let { stations ->
          stations.find { station ->
            station.name.lowercase()
              .replace(
                " ",
                ""
              )
              .contains(
                searchQuery
              )
          }?.let { item ->
            val metaData = MediaMetadata.Builder()
              .setTitle(item.name)
              .setArtist(item.url)
              .setMediaType(MEDIA_TYPE_RADIO_STATION)
              .setArtworkUri("https://${item.url}/api/station/${item.shortcode}/art/-1".toUri())
              .setDurationMs(1)
              .setIsBrowsable(false)
              .setIsPlayable(true)
              .build()
            val mediaItem = MediaItem.Builder()
              .setMediaId(item.defaultMount)
              .setMediaMetadata(metaData)
              .build()
            foundStations.add(mediaItem)
            url = item.url
            shortCode = item.shortcode
          }
        }

        if (foundStations.isEmpty()) {
          val item = mutableStateOf<DiscoveryStation?>(null)
          discoveryJSON.value?.let { json ->
            json.featuredStations.stations.find { stationData ->
              stationData.friendlyName.lowercase()
                .replace(
                  " ",
                  ""
                )
                .contains(searchQuery)
            }?.let {
              item.value = it
            }
            if (item.value == null) {
              json.discoveryStations.flatMap { it.stations }.find { stationData ->
                stationData.friendlyName.lowercase()
                  .replace(
                    " ",
                    ""
                  )
                  .contains(searchQuery)
              }?.let {
                item.value = it
              }
            }
          }
          item.value?.let {
            Log.d(
              "DEBUG",
              "I founded it"
            )
            val metaData = MediaMetadata.Builder()
              .setTitle(it.friendlyName)
              .setArtist(it.description)
              .setMediaType(MEDIA_TYPE_RADIO_STATION)
              .setArtworkUri(it.imageMediaUrl.toUri())
              .setDurationMs(1)
              .setIsBrowsable(false)
              .setIsPlayable(true)
              .build()
            val mediaItem = MediaItem.Builder()
              .setMediaId(it.preferredMount)
              .setMediaMetadata(metaData)
              .build()
            foundStations.add(mediaItem)
            shortCode = it.shortCode
            url = URL(it.publicPlayerUrl).host.toString()
          }
        }
        if (foundStations.isEmpty()) {
          return Futures.immediateFuture(
            mutableListOf()
          )
        }
        val updatedMediaItems = foundStations.map {
          it.buildUpon()
            .setUri(it.mediaId)
            .build()
        }
          .toMutableList()
        nowPlaying.nowPlayingShortCode.value = shortCode
        nowPlaying.nowPlayingURL.value = url
        nowPlaying.nowPlayingMount.value = updatedMediaItems[0].mediaId.fixHttps()
        return Futures.immediateFuture(updatedMediaItems)
      }

      var shortCode = ""
      var url = ""

      var updatedMediaItems = mutableListOf<MediaItem>()

      when {
        mediaItems[0].mediaId.startsWith("SAVED_STATION-") -> {
          val item = savedStationsDB.savedStations.value?.find { savedStation ->
            savedStation.defaultMount == mediaItems[0].mediaId.replaceFirst(
              "SAVED_STATION-",
              ""
            )
          }
          shortCode = item?.shortcode ?: ""
          url = item?.url ?: ""
          updatedMediaItems = mediaItems.map {
            it.buildUpon()
              .setUri(
                it.mediaId.replaceFirst(
                  "SAVED_STATION-",
                  ""
                )
              ).setMediaId(
                it.mediaId.replaceFirst(
                  "SAVED_STATION-",
                  ""
                )
              )
              .build()
          }
            .toMutableList()
        }

        mediaItems[0].mediaId.startsWith("DISCOVERED-") -> {
          val item = mutableStateOf<DiscoveryStation?>(null)
          discoveryJSON.value?.let { json ->
            json.featuredStations.stations.find { stationData ->
              stationData.preferredMount == mediaItems[0].mediaId.replaceFirst(
                "DISCOVERED-",
                ""
              )
            }?.let {
              item.value = it
            }
            if (item.value == null) {
              json.discoveryStations.flatMap { it.stations }.find { stationData ->
                stationData.preferredMount == mediaItems[0].mediaId.replaceFirst(
                  "DISCOVERED-",
                  ""
                )
              }?.let {
                item.value = it
              }
            }
          }
          shortCode = item.value?.shortCode ?: ""
          url = if (item.value != null) {
            URL(item.value?.publicPlayerUrl).host.toString()
          } else {
            ""
          }
          updatedMediaItems = mediaItems.map {
            it.buildUpon()
              .setUri(
                it.mediaId.replaceFirst(
                  "DISCOVERED-",
                  ""
                )
              ).setMediaId(
                it.mediaId.replaceFirst(
                  "DISCOVERED-",
                  ""
                )
              )
              .build()
          }
            .toMutableList()

        }

        else -> {
          val item = savedStationsDB.savedStations.value?.find { savedStation ->
            savedStation.defaultMount == mediaItems[0].mediaId
          }
          shortCode = item?.shortcode ?: ""
          url = item?.url ?: ""
          updatedMediaItems = mediaItems.map { it.buildUpon().setUri(it.mediaId).build() }
            .toMutableList()
        }
      }

      nowPlaying.nowPlayingShortCode.value = shortCode
      nowPlaying.nowPlayingURL.value = url
      nowPlaying.nowPlayingMount.value = updatedMediaItems[0].mediaId.fixHttps()

      return Futures.immediateFuture(updatedMediaItems)
    }

    override fun onGetLibraryRoot(
      session: MediaLibrarySession,
      browser: MediaSession.ControllerInfo,
      params: LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {
      return Futures.immediateFuture(
        LibraryResult.ofItem(
          MediaItem.Builder()
            .setMediaId("/")
            .setMediaMetadata(
              MediaMetadata.Builder()
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build()
            )
            .build(),
          params
        )
      )
    }

    override fun onGetChildren(
      session: MediaLibrarySession,
      browser: MediaSession.ControllerInfo,
      parentId: String,
      page: Int,
      pageSize: Int,
      params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {

      val userPreferences = (application as AppSetup).userPreferences

      val job = SupervisorJob()
      val scope = CoroutineScope(Dispatchers.IO + job)

      scope.launch { savedChildrenStyle = userPreferences.androidAutoLayoutFlow.first() }

      val gridStyleChildren = Bundle()
      val listStyleChildren = Bundle()

      gridStyleChildren.putInt(
        MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
        MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
      )

      listStyleChildren.putInt(
        MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
        MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
      )

      val childrenStyle = if (savedChildrenStyle == AndroidAutoLayouts.GRID) {
        gridStyleChildren
      } else {
        listStyleChildren
      }

      when (parentId) {
        "/" -> {
          val metaDataStations = MediaMetadata.Builder()
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS)
            .setExtras(childrenStyle)
            .setTitle("My Stations")
            .setArtworkUri(
              resourceToUri(
                applicationContext,
                R.drawable.stations_icon
              )
            )
            .build()
          val metaDataDiscover = MediaMetadata.Builder()
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS)
            .setExtras(childrenStyle)
            .setTitle("Discover")
            .setArtworkUri(
              resourceToUri(
                applicationContext,
                R.drawable.discover_icon
              )
            )
            .build()



          return Futures.immediateFuture(
            LibraryResult.ofItemList(
              listOf(
                MediaItem.Builder()
                  .setMediaId("Stations")
                  .setMediaMetadata(metaDataStations)
                  .build(),
                MediaItem.Builder()
                  .setMediaId("Discover")
                  .setMediaMetadata(metaDataDiscover)
                  .build()
              ),
              params
            )
          )
        }

        "Stations" -> {
          val stations = mutableListOf<MediaItem>()
          savedStationsDB.savedStations.value?.let {
            for (item in savedStationsDB.savedStations.value!!) {
              val metaData = MediaMetadata.Builder()
                .setTitle(item.name)
                .setArtist(item.url)
                .setMediaType(MEDIA_TYPE_RADIO_STATION)
                .setArtworkUri("https://${item.url}/api/station/${item.shortcode}/art/-1".toUri())
                .setDurationMs(1)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build()

              val mediaItem = MediaItem.Builder()
                .setMediaId("SAVED_STATION-${item.defaultMount}")
                .setMediaMetadata(metaData)
                .build()

              stations.add(mediaItem)
            }
          }
          return Futures.immediateFuture(
            LibraryResult.ofItemList(
              stations,
              params
            )
          )
        }

        "Discover" -> {
          val featuredStations = MediaItem.Builder()
            .setMediaId("Discover/Featured_Stations")
            .setMediaMetadata(
              MediaMetadata.Builder()
                .setTitle("Featured Stations")
                .setArtist("Stations hand-picked by listeners for listeners")
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS)
                .setExtras(childrenStyle)
                .build()
            ).build()

          val categories = mutableListOf(featuredStations)

          discoveryJSON.value?.let { json ->
            json.discoveryStations.forEach {
              val category = MediaItem.Builder()
                .setMediaId(
                  "Discover/${
                    it.title.replace(
                      " ",
                      "_"
                    )
                  }"
                )
                .setMediaMetadata(
                  MediaMetadata.Builder()
                    .setTitle(it.title)
                    .setArtist(it.description)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS)
                    .setExtras(childrenStyle)
                    .build()
                )
                .build()
              categories.add(category)
            }
          }

          return Futures.immediateFuture(
            LibraryResult.ofItemList(
              categories,
              params
            )
          )
        }

        else -> {

          if (parentId.startsWith("Discover/")) {
            val category = parentId.replaceFirst(
              "Discover/",
              ""
            )
            val stations = mutableListOf<MediaItem>()

            if (parentId == "Discover/Featured_Stations") {
              discoveryJSON.value?.let { discoveryData ->
                discoveryData.featuredStations.stations.forEach { stationData ->
                  val station = MediaItem.Builder()
                    .setMediaId("DISCOVERED-${stationData.preferredMount}")
                    .setMediaMetadata(
                      MediaMetadata.Builder()
                        .setTitle(stationData.friendlyName)
                        .setArtist(stationData.description)
                        .setMediaType(MEDIA_TYPE_RADIO_STATION)
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .setArtworkUri(stationData.imageMediaUrl.toUri())
                        .setDurationMs(1)
                        .build()
                    )
                    .build()
                  stations.add(station)
                }
              }
              return Futures.immediateFuture(
                LibraryResult.ofItemList(
                  stations,
                  params
                )
              )
            } else {
              discoveryJSON.value?.let { discoveryData ->
                val discoveryCategory = discoveryData.discoveryStations.first { discoveryCategory ->
                  discoveryCategory.title.replace(
                    " ",
                    "_"
                  ) == category
                }

                discoveryCategory.stations.forEach { stationData ->
                  val station = MediaItem.Builder()
                    .setMediaId("DISCOVERED-${stationData.preferredMount}")
                    .setMediaMetadata(
                      MediaMetadata.Builder()
                        .setTitle(stationData.friendlyName)
                        .setArtist(stationData.description)
                        .setMediaType(MEDIA_TYPE_RADIO_STATION)
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .setArtworkUri(stationData.imageMediaUrl.toUri())
                        .setDurationMs(1)
                        .build()
                    )
                    .build()
                  stations.add(station)
                }
              }
              return Futures.immediateFuture(
                LibraryResult.ofItemList(
                  stations,
                  params
                )
              )
            }
          }

          return Futures.immediateFuture(
            LibraryResult.ofItemList(
              ImmutableList.of(),
              params
            )
          )
        }
      }
    }

    override fun onSearch(
      session: MediaLibrarySession,
      browser: MediaSession.ControllerInfo,
      query: String,
      params: LibraryParams?
    ): ListenableFuture<LibraryResult<Void>> {
      mediaSession?.notifySearchResultChanged(
        browser,
        query,
        query.length,
        params
      )
      return super.onSearch(
        session,
        browser,
        query,
        params
      )
    }

    override fun onGetSearchResult(
      session: MediaLibrarySession,
      browser: MediaSession.ControllerInfo,
      query: String,
      page: Int,
      pageSize: Int,
      params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {

      val foundStations = mutableListOf<MediaItem>()
      savedStationsDB.savedStations.value?.let { stations ->
        stations.filter { station ->
          "${station.name}${station.url}".lowercase().contains(query.lowercase())
        }.forEach { item ->
          val metaData = MediaMetadata.Builder()
            .setTitle(item.name)
            .setArtist(item.url)
            .setMediaType(MEDIA_TYPE_RADIO_STATION)
            .setArtworkUri("https://${item.url}/api/station/${item.shortcode}/art/-1".toUri())
            .setDurationMs(1)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()


          val mediaItem = MediaItem.Builder()
            .setMediaId(item.defaultMount)
            .setMediaMetadata(metaData)
            .build()
          foundStations.add(mediaItem)
        }
      }

      return Futures.immediateFuture(
        LibraryResult.ofItemList(
          foundStations,
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
      sharedMediaController.mediaSession.value = null
      mediaSession = null
    }
    Log.d(
      "DEBUG-MEDIA",
      "Good-Bye! \uD83D\uDC4B\uD83C\uDFFB"
    )
    nowPlaying.nowPlayingShortCode.value = ""
    nowPlaying.nowPlayingURL.value = ""
    nowPlaying.nowPlayingMount.value = ""
    unregisterReceiver(receiver)
    super.onDestroy()
    android.os.Process.killProcess(android.os.Process.myPid())
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