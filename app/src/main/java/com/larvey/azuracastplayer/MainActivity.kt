package com.larvey.azuracastplayer

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.room.Room
import com.larvey.azuracastplayer.components.AddStationDialog
import com.larvey.azuracastplayer.viewmodels.NowPlayingViewModel
import com.larvey.azuracastplayer.database.SavedStationsDatabase
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
  private val savedStationsViewModel by viewModels<SavedStationsViewModel>(
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

        LaunchedEffect (savedRadioList) {
          for (item in savedRadioList) {
            radioListViewModel.searchStationHost(item.url)
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
              Button(onClick = { TODO() }) {
                Text("Show Sheet")
              }
            }
          }
        ) { innerPadding ->

          val mediaController by rememberManagedMediaController()

          mediaController?.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
              super.onMediaMetadataChanged(mediaMetadata)
              if (mediaMetadata.title != null) {
                nowPlayingViewModel.setMediaMetadata(
                  url = nowPlayingViewModel.nowPlayingURL.value,
                  shortCode = nowPlayingViewModel.nowPlayingShortCode.value,
                  mediaPlayer = mediaController
                  )
              }
            }
          })

          var playerState: PlayerState? by remember {
            mutableStateOf(mediaController?.state())
          }

          DisposableEffect (key1 = mediaController) {
            mediaController?.run {
              playerState = state()
            }
            onDispose {
              playerState?.dispose()
            }
          }
          MyRadios(
            savedRadioList = savedRadioList,
            innerPadding = innerPadding,
            setPlaybackSource = { url, shortCode ->
              val uri = Uri.parse(nowPlayingViewModel.staticDataMap[Pair(url, shortCode)]?.station?.mounts?.get(0)?.url)
              nowPlayingViewModel.setPlaybackSource(
                uri = uri,
                url = url,
                shortCode = shortCode,
                mediaPlayer = mediaController
              )
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
        }
      }
    }
  }
}
