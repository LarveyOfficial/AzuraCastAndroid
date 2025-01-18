package com.larvey.azuracastplayer

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.larvey.azuracastplayer.components.AddStationDialog
import com.larvey.azuracastplayer.components.NowPlaying
import com.larvey.azuracastplayer.viewmodels.NowPlayingViewModel
import com.larvey.azuracastplayer.database.SavedStationsDatabase
import com.larvey.azuracastplayer.mediasession.rememberManagedMediaController
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.state.state
import com.larvey.azuracastplayer.viewmodels.SavedStationsViewModel
import com.larvey.azuracastplayer.ui.theme.AzuraCastPlayerTheme
import com.larvey.azuracastplayer.viewmodels.RadioListViewModel
import com.larvey.azuracastplayer.views.MyRadios

@Suppress("UNCHECKED_CAST")
class MainActivity : ComponentActivity() {
  private val db by lazy {
    Room.databaseBuilder(
      context = applicationContext,
      klass = SavedStationsDatabase::class.java,
      name = "datamodel.db"
    ).build()
  }
  val savedStationsViewModel by viewModels<SavedStationsViewModel>(
    factoryProducer = {
      object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return SavedStationsViewModel(db.dao) as T
        }
      }
    }
  )

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AzuraCastPlayerTheme {

        val radioListViewModel: RadioListViewModel = viewModel()
        val nowPlayingViewModel: NowPlayingViewModel = viewModel()

        val savedRadioList by savedStationsViewModel.getAllEntries().collectAsState(initial = emptyList())

        var showAddDialog by remember { mutableStateOf(false) }
        var showNowPlaying by remember { mutableStateOf(false) }

        LaunchedEffect (savedRadioList) {
          for (item in savedRadioList) {
            radioListViewModel.searchStationHost(item.url)
          }
        }
        val mediaController by rememberManagedMediaController()

        var playerState: PlayerState? by remember {
          mutableStateOf(mediaController?.state())
        }

        LaunchedEffect(playerState?.mediaMetadata?.title) {
          if (nowPlayingViewModel.nowPlayingURL.value != "") {
            nowPlayingViewModel.setMediaMetadata(
              nowPlayingViewModel.nowPlayingURL.value,
              nowPlayingViewModel.nowPlayingShortCode.value,
              mediaController
            )
          }
        }

        DisposableEffect (key1 = mediaController) {
          mediaController?.run {
            playerState = state()
          }

          onDispose {
            playerState?.dispose()
          }
        }

        Scaffold (
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
              Icon(Icons.Rounded.Add, contentDescription = "Add")
            }
          },
          bottomBar = {
            BottomAppBar {
              Button(onClick = { showNowPlaying = true }) {
                Text("Show Sheet")
              }
            }
          }
        ) { innerPadding ->

          MyRadios(
            savedRadioList = savedRadioList,
            innerPadding = innerPadding,
            setPlaybackSource = { url, shortCode ->
              nowPlayingViewModel.staticDataMap[Pair(url, shortCode)]?.station?.mounts?.get(0)?.url?.let{
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
              nowPlayingViewModel.getStationInformation(url, shortCode)
            }
          )
        }

        when {
          showAddDialog -> {
            AddStationDialog(
              hideDialog = { showAddDialog = false },
              addData = savedStationsViewModel::addData,
              stationHostData = radioListViewModel.stationHostData,
              searchStationHost = radioListViewModel::searchStationHost
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
