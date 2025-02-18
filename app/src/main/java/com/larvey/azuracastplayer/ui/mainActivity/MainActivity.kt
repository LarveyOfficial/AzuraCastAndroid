package com.larvey.azuracastplayer.ui.mainActivity

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.larvey.azuracastplayer.classes.data.SavedStation
import com.larvey.azuracastplayer.db.settings.SettingsViewModel
import com.larvey.azuracastplayer.db.settings.SettingsViewModel.SettingsModelProvider
import com.larvey.azuracastplayer.session.rememberManagedMediaController
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.state.state
import com.larvey.azuracastplayer.ui.mainActivity.addStations.AddStationDialog
import com.larvey.azuracastplayer.ui.mainActivity.components.MiniPlayer
import com.larvey.azuracastplayer.ui.mainActivity.radios.MyRadios
import com.larvey.azuracastplayer.ui.nowplaying.NowPlaying
import com.larvey.azuracastplayer.ui.theme.AzuraCastPlayerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay


class MainActivity : ComponentActivity() {

  private var mainActivityViewModel: MainActivityViewModel? = null

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
    ExperimentalMaterial3Api::class
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
        val settingsModel: SettingsViewModel = viewModel(factory = SettingsModelProvider.Factory)
        val radioListMode by settingsModel.gridView.collectAsState() // false = list, true = grid
        val mediaController by rememberManagedMediaController()
        val lazyListState = rememberLazyListState()
        val lazyGridState = rememberLazyGridState()

        val editingList = remember { mutableStateOf(false) }
        val confirmEdit = remember { mutableStateOf(false) }

        var palette = remember { mutableStateOf<Palette?>(null) }


        var playerState: PlayerState? by remember {
          mutableStateOf(mediaController?.state())
        }

        val appContext = LocalContext.current.applicationContext

        LaunchedEffect(playerState?.mediaMetadata?.artworkUri) {
          if (playerState?.mediaMetadata?.artworkUri != null) {
            this.async(Dispatchers.IO) {
              Glide.with(appContext).asBitmap().load(
                playerState?.mediaMetadata?.artworkUri.toString()
              ).submit().get().let { bitmap ->
                palette.value = Palette.from(bitmap).maximumColorCount(32).generate()
              }
            }
          } else {
            palette.value = null
          }
        }

        var animatedFabColor = animateColorAsState(
          targetValue =
          if (palette.value?.vibrantSwatch?.rgb != null
            || palette.value?.dominantSwatch?.rgb != null
          ) androidx.compose.ui.graphics.Color(
            palette.value?.vibrantSwatch?.rgb ?: palette.value?.dominantSwatch?.rgb!!
          )
          else MaterialTheme.colorScheme.primaryContainer,
          label = "Fab Color"
        )
        var animatedFabIconTint = animateColorAsState(
          targetValue =
          if (palette.value?.vibrantSwatch?.bodyTextColor != null
            || palette.value?.dominantSwatch?.bodyTextColor != null
          ) androidx.compose.ui.graphics.Color(
            palette.value?.vibrantSwatch?.bodyTextColor
              ?: palette.value?.dominantSwatch?.bodyTextColor!!
          ) else MaterialTheme.colorScheme.onPrimaryContainer,
          label = "Fab Icon Color"
        )

        LaunchedEffect(Unit) {
          mainActivityViewModel?.getStationList(false)
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

        DisposableEffect(key1 = this) {
          onDispose {
            mediaController?.run {
              pause()
              stop()
              release()
            }
          }
        }

        Scaffold(
          topBar = {
            TopAppBar(colors = topAppBarColors(
              containerColor = MaterialTheme.colorScheme.background,
              titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
              title = { Text("Radio List") },
              actions = {
                AnimatedVisibility(radioListMode != null && !editingList.value) {
                  IconButton(
                    onClick = {
                      settingsModel.toggleGridView()
                    }
                  ) {
                    Icon(
                      imageVector = if (radioListMode == true) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                      contentDescription = "Add"
                    )
                  }
                }
              }
            )
          },
          floatingActionButton = {
            AnimatedVisibility(
              visible = ((!lazyGridState.canScrollBackward && radioListMode == true) || (!lazyListState.canScrollBackward) && radioListMode == false) && !editingList.value,
              exit = slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth * 2 }),
              enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth * 2 }),
            ) {
              FloatingActionButton(
                onClick = {
                  showAddDialog = true
                },
                containerColor = animatedFabColor.value
              ) {
                Icon(
                  Icons.Rounded.Add,
                  contentDescription = "Add",
                  tint = animatedFabIconTint.value
                )
              }
            }
            AnimatedVisibility(
              visible = editingList.value,
              exit = slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth * 2 }),
              enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth * 2 }),
            ) {
              FloatingActionButton(
                onClick = {
                  confirmEdit.value = true
                },
                containerColor = MaterialTheme.colorScheme.tertiary
              ) {
                Icon(
                  Icons.Rounded.Check,
                  contentDescription = "Add",
                  tint = MaterialTheme.colorScheme.onTertiary
                )
              }
            }
          },
          bottomBar = {
            AnimatedVisibility(
              visible = playerState?.currentMediaItem?.mediaId != null,
              enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight * 2 }),
              exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight * 2 })
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
                  })
              }
            }
          }) { innerPadding ->
          AnimatedVisibility(
            radioListMode != null,
            enter = slideInVertically(initialOffsetY = { fullHeight -> -fullHeight * 2 })
          ) {
            MyRadios(
              savedRadioList = mainActivityViewModel?.savedRadioList,
              innerPadding = innerPadding,
              setPlaybackSource = { url, uri, shortCode ->
                mainActivityViewModel?.setPlaybackSource(
                  url,
                  uri,
                  shortCode,
                  mediaController
                )
              },
              staticDataMap = mainActivityViewModel?.nowPlayingData?.staticDataMap,
              deleteRadio = { station ->
                mainActivityViewModel?.deleteStation(station)
              },
              editRadio = { newStation ->
                mainActivityViewModel?.editStation(newStation)
              },
              radioListMode = radioListMode!!,
              editingList = editingList,
              confirmEdit = confirmEdit,
              editAllStations = { stations ->
                mainActivityViewModel?.editAllStations(stations)
              },
              lazyListState = lazyListState,
              lazyGridState = lazyGridState
            )
          }
        }
        when {
          showAddDialog -> {
            AddStationDialog(
              hideDialog = { showAddDialog = false },
              addData = { stations ->
                mainActivityViewModel?.addStation(
                  stations
                )
              },
              currentStationCount = mainActivityViewModel?.savedRadioList?.size ?: 0
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
              },
              stop = {
                showNowPlaying = false
                mediaController?.stop()
                mediaController?.clearMediaItems()
              },
              currentMount = mainActivityViewModel?.nowPlayingData?.staticData?.value?.station?.mounts?.find { it.url == playerState?.currentMediaItem?.mediaId },
              songHistory = mainActivityViewModel?.nowPlayingData?.staticData?.value?.songHistory,
              playingNext = mainActivityViewModel?.nowPlayingData?.staticData?.value?.playingNext,
              nowPlayingData = mainActivityViewModel?.nowPlayingData,
              palette = palette
            )
          }
        }
      }
    }
  }
}
