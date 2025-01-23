package com.larvey.azuracastplayer

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_RADIO_STATION
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.larvey.azuracastplayer.classes.NowPlayingData
import com.larvey.azuracastplayer.classes.SavedStation
import com.larvey.azuracastplayer.components.AddStationDialog
import com.larvey.azuracastplayer.components.MiniPlayer
import com.larvey.azuracastplayer.components.NowPlaying
import com.larvey.azuracastplayer.mediasession.rememberManagedMediaController
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.state.state
import com.larvey.azuracastplayer.ui.theme.AzuraCastPlayerTheme
import com.larvey.azuracastplayer.views.MyRadios
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

  lateinit var nowPlayingData: NowPlayingData


  private var savedRadioList: List<SavedStation>? = emptyList()
  private var fetchData = mutableStateOf(true)

  override fun onPause() {
    Log.d(
      "DEBUG",
      "Pausing"
    )
    fetchData.value = false
    super.onPause()
  }

  override fun onResume() {
    Log.d(
      "DEBUG",
      "Resuming"
    )
    if (savedRadioList != null && savedRadioList != emptyList<SavedStation>()) {
      for (item in savedRadioList) {
        Log.d(
          "DEBUG",
          "Fetching Data for ${item.name}"
        )
        nowPlayingData?.getStationInformation(
          item.url,
          item.shortcode
        )
      }
    }
    fetchData.value = true

    super.onResume()
  }

  @androidx.annotation.OptIn(UnstableApi::class)
  @OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalGlideComposeApi::class
  )
  override fun onCreate(savedInstanceState: Bundle?) {
    val app = (application as AppSetup)
    nowPlayingData = app.nowPlayingData
    val savedStationsDB = app.savedStationsDB

    super.onCreate(savedInstanceState)

    enableEdgeToEdge(
      navigationBarStyle = SystemBarStyle.light(
        Color.TRANSPARENT,
        Color.TRANSPARENT
      )
    )

    setContent {
      AzuraCastPlayerTheme {

        savedStationsDB?.let {
          Log.d(
            "DEBUG",
            "${it.getAllEntries()}"
          )
        }

        val scope = rememberCoroutineScope()
        fetchData = remember { mutableStateOf(true) }

        savedRadioList =
          savedStationsDB?.getAllEntries()?.collectAsState(initial = emptyList())?.value

        var showAddDialog by remember { mutableStateOf(false) }
        var showNowPlaying by remember { mutableStateOf(false) }

        val mediaController by rememberManagedMediaController({
          val mediaList = mutableListOf<MediaItem>()

          if (savedRadioList != null) {
            for (item in savedRadioList) {
              val metaData = MediaMetadata.Builder()
                .setTitle(item.name)
                .setDisplayTitle("Bruh")
                .setStation(item.shortcode)
                .setSubtitle(item.url.toString())
                .setMediaType(MEDIA_TYPE_RADIO_STATION)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setArtworkUri(
                  Uri.parse(
                    nowPlayingData?.staticDataMap[Pair(
                      item.url,
                      item.shortcode
                    )]?.nowPlaying?.song?.art
                  )
                )
                .setDurationMs(1)
                .build()
              var bundle = Bundle()
              Bundle().putString(
                "Station",
                item.shortcode
              )
              val media = MediaItem.Builder()
                .setMediaId(
                  nowPlayingData?.staticDataMap[Pair(
                    item.url,
                    item.shortcode
                  )]?.station?.mounts[0]?.url.toString()
                )
                .setUri("fuck")
                .setRequestMetadata(MediaItem.RequestMetadata.Builder().setExtras(bundle).build())
                .setMediaMetadata(metaData)
                .build()
              mediaList.add(media)
            }
          }

          mediaList.toList()
        })

        var playerState: PlayerState? by remember {
          mutableStateOf(mediaController?.state())
        }

        LaunchedEffect(playerState?.mediaMetadata) {
          if (playerState?.currentMediaItem?.mediaId != null) {
            Log.d(
              "DEBUG",
              "Now Playing: ${playerState?.currentMediaItem?.requestMetadata?.extras.toString()}"
            )
            Log.d(
              "DEBUG",
              "Now Playing: ${playerState?.mediaMetadata?.title.toString()}"
            )
            Log.d(
              "DEBUG",
              "Now Playing: ${playerState?.mediaMetadata?.displayTitle.toString()}"
            )

            nowPlayingData?.setMediaMetadata(
              playerState?.mediaMetadata?.subtitle.toString(),
              playerState?.mediaMetadata?.station.toString(),
              mediaController
            )
          }
        }

        LaunchedEffect(
          key1 = fetchData.value,
          key2 = savedRadioList
        ) {
          while (savedRadioList != emptyList<SavedStation>() && fetchData.value == true) {
            Log.d(
              "DEBUG",
              "Waiting 30 seconds to fetch data"
            )
            delay(30000)
            if (savedRadioList != null) {
              for (item in savedRadioList) {
                if (fetchData.value) {
                  Log.d(
                    "DEBUG",
                    "Fetching Data for ${item.name}"
                  )
                  nowPlayingData?.getStationInformation(
                    item.url,
                    item.shortcode
                  )
                }
              }
            }
          }
        }


        DisposableEffect(key1 = mediaController) {
          mediaController?.run {
            playerState = state()
          }
          onDispose {
            playerState?.dispose()
          }
        }

        Scaffold(
          topBar = {
            TopAppBar(
              colors = topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
              ),
              title = { Text("Radio List") }
            )
          },
          floatingActionButton = {
            FloatingActionButton(
              onClick = {
                showAddDialog = true
              }
            ) {
              Icon(
                Icons.Rounded.Add,
                contentDescription = "Add"
              )
            }
          },
          bottomBar = {
            AnimatedVisibility(
              visible = playerState?.currentMediaItem?.mediaId != null,
              enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight * 2 }
              ),
              exit = slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight * 2 }
              )
            ) {
              BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
              ) {
                MiniPlayer(
                  playerState = playerState,
                  showNowPlaying = {
                    showNowPlaying = true
                  },
                  pause = {
                    mediaController?.pause()
                  },
                  play = {
                    mediaController?.play()
                  }
                )
              }
            }
          }
        ) { innerPadding ->

          MyRadios(
            savedRadioList = savedRadioList,
            innerPadding = innerPadding,
            setPlaybackSource = { url, shortCode ->
              nowPlayingData?.staticDataMap[Pair(
                url,
                shortCode
              )]?.station?.mounts?.get(0)?.url?.let {
                val uri = Uri.parse(it)
                nowPlayingData.setPlaybackSource(
                  uri = uri,
                  url = url,
                  shortCode = shortCode,
                  mediaPlayer = mediaController
                )
              }
            },
            getStationData = { url, shortCode ->
              nowPlayingData?.getStationInformation(
                url,
                shortCode
              )
            },
            staticDataMap = nowPlayingData?.staticDataMap,
            deleteRadio = { station ->
              scope.launch {
                savedStationsDB?.removeStation(station)
              }
            }
          )
        }
        when {
          showAddDialog -> {
            AddStationDialog(
              hideDialog = { showAddDialog = false },
              addData = { name, shortcode, url ->
                scope.launch {
                  savedStationsDB?.saveStation(
                    name,
                    shortcode,
                    url
                  )
                }
              }
            )
          }

          showNowPlaying -> {
            NowPlaying(
              hideNowPlaying = {
                showNowPlaying = false
              },
              playerState = playerState,
              pause = {
                mediaController?.pause()
              },
              play = {
                mediaController?.play()
              }
            )
          }
        }
      }
    }
  }
}
