package com.larvey.azuracastplayer.ui.mainActivity

import android.graphics.Color
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.larvey.azuracastplayer.classes.data.SavedStation
import com.larvey.azuracastplayer.session.rememberManagedMediaController
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.state.state
import com.larvey.azuracastplayer.ui.mainActivity.addStations.AddStationDialog
import com.larvey.azuracastplayer.ui.mainActivity.components.MiniPlayer
import com.larvey.azuracastplayer.ui.mainActivity.radios.MyRadios
import com.larvey.azuracastplayer.ui.nowPlaying.NowPlaying
import com.larvey.azuracastplayer.ui.theme.AzuraCastPlayerTheme
import kotlinx.coroutines.delay


class MainActivity : ComponentActivity() {

  var mainActivityViewModel: MainActivityViewModel? = null

  override fun onPause() {
    super.onPause()
    mainActivityViewModel?.pauseActivity()
  }

  override fun onResume() {
    super.onResume()
    mainActivityViewModel?.resumeActivity()
  }

  @androidx.annotation.OptIn(UnstableApi::class)
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
        mainActivityViewModel = viewModel()

        var showAddDialog by remember { mutableStateOf(false) }
        var showNowPlaying by remember { mutableStateOf(false) }

        val mediaController by rememberManagedMediaController()

        var playerState: PlayerState? by remember {
          mutableStateOf(mediaController?.state())
        }

        LaunchedEffect(Unit) {
          mainActivityViewModel?.getStationList(false)
        }

        LaunchedEffect(playerState?.mediaMetadata) {
          if (mainActivityViewModel?.nowPlayingData?.nowPlayingURL?.value != "" && playerState?.currentMediaItem?.mediaId != null) {
            mainActivityViewModel?.updateCurrentMetadata(mediaController)
          }
        }

        LaunchedEffect(key1 = mainActivityViewModel?.savedRadioList) {
          while (mainActivityViewModel?.savedRadioList != null && mainActivityViewModel?.savedRadioList != emptyList<SavedStation>()) {
            Log.d(
              "DEBUG",
              "Waiting 30 seconds to fetch data"
            )
            delay(30000)
            mainActivityViewModel?.updateRadioList()
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
            savedRadioList = mainActivityViewModel?.savedRadioList,
            innerPadding = innerPadding,
            setPlaybackSource = { url, shortCode ->
              mainActivityViewModel?.setPlaybackSource(
                url,
                shortCode,
                mediaController
              )
            },
            staticDataMap = mainActivityViewModel?.nowPlayingData?.staticDataMap,
            deleteRadio = { station ->
              mainActivityViewModel?.deleteStation(station)
            }
          )
        }
        when {
          showAddDialog -> {
            AddStationDialog(
              hideDialog = { showAddDialog = false },
              addData = { stations ->
                mainActivityViewModel?.addStation(
                  stations
                )
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
