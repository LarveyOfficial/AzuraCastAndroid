package com.larvey.azuracastplayer

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.larvey.azuracastplayer.classes.SavedStation
import com.larvey.azuracastplayer.components.AddStationDialog
import com.larvey.azuracastplayer.components.MiniPlayer
import com.larvey.azuracastplayer.components.NowPlaying
import com.larvey.azuracastplayer.database.SavedStationsDatabase
import com.larvey.azuracastplayer.mediasession.rememberManagedMediaController
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.state.state
import com.larvey.azuracastplayer.ui.theme.AzuraCastPlayerTheme
import com.larvey.azuracastplayer.viewmodels.NowPlayingViewModel
import com.larvey.azuracastplayer.viewmodels.SavedStationsViewModel
import com.larvey.azuracastplayer.views.MyRadios
import kotlinx.coroutines.delay


class MainActivity : ComponentActivity() {
  private val db by lazy {
    Room.databaseBuilder(
      context = applicationContext,
      klass = SavedStationsDatabase::class.java,
      name = "datamodel.db"
    ).build()
  }

  @Suppress("UNCHECKED_CAST")
  val savedStationsViewModel by viewModels<SavedStationsViewModel>(
    factoryProducer = {
      object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return SavedStationsViewModel(db.dao) as T
        }
      }
    }
  )

  private var savedRadioList: List<SavedStation> = emptyList()
  private var fetchData = mutableStateOf(true)
  private lateinit var nowPlayingViewModel: NowPlayingViewModel

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
    if (savedRadioList != emptyList<SavedStation>()) {
      for (item in savedRadioList) {
        Log.d(
          "DEBUG",
          "Fetching Data for ${item.name}"
        )
        nowPlayingViewModel.getStationInformation(
          item.url,
          item.shortcode
        )
      }
    }
    fetchData.value = true

    super.onResume()
  }

  @OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalGlideComposeApi::class
  )
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge(
      navigationBarStyle = SystemBarStyle.light(
        Color.TRANSPARENT,
        Color.TRANSPARENT
      )
    )

    setContent {
      AzuraCastPlayerTheme {
        fetchData = remember { mutableStateOf(true) }
        nowPlayingViewModel = viewModel()

        savedRadioList =
          savedStationsViewModel.getAllEntries().collectAsState(initial = emptyList()).value

        var showAddDialog by remember { mutableStateOf(false) }
        var showNowPlaying by remember { mutableStateOf(false) }

        val mediaController by rememberManagedMediaController()

        var playerState: PlayerState? by remember {
          mutableStateOf(mediaController?.state())
        }

        LaunchedEffect(playerState?.mediaMetadata) {
          if (nowPlayingViewModel.nowPlayingURL.value != "") {
            nowPlayingViewModel.setMediaMetadata(
              nowPlayingViewModel.nowPlayingURL.value,
              nowPlayingViewModel.nowPlayingShortCode.value,
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
            for (item in savedRadioList) {
              if (fetchData.value) {
                Log.d(
                  "DEBUG",
                  "Fetching Data for ${item.name}"
                )
                nowPlayingViewModel.getStationInformation(
                  item.url,
                  item.shortcode
                )
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
              visible = playerState?.currentMediaItem != null,
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
              nowPlayingViewModel.staticDataMap[Pair(
                url,
                shortCode
              )]?.station?.mounts?.get(0)?.url?.let {
                val uri = Uri.parse(it)
                nowPlayingViewModel.setPlaybackSource(
                  uri = uri,
                  url = url,
                  shortCode = shortCode,
                  mediaPlayer = mediaController
                )
              }
            },
            getStationData = { url, shortCode ->
              nowPlayingViewModel.getStationInformation(
                url,
                shortCode
              )
            },
            staticDataMap = nowPlayingViewModel.staticDataMap
          )
        }
        when {
          showAddDialog -> {
            AddStationDialog(
              hideDialog = { showAddDialog = false },
              addData = savedStationsViewModel::saveStation
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
