package com.larvey.azuracastplayer.ui.mainActivity

import android.graphics.Color
import android.os.Bundle
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
import androidx.lifecycle.viewmodel.compose.viewModel
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

        //region List States for MyRadios
        val lazyListState = rememberLazyListState()
        val lazyGridState = rememberLazyGridState()
        //endregion

        //region List editing variables
        val editingList = remember { mutableStateOf(false) }
        val confirmEdit = remember { mutableStateOf(false) }
        //endregion

        var playerState: PlayerState? by remember {
          mutableStateOf(mediaController?.state())
        }

        //region NowPlaying gradient colors
        val defaultColor = MaterialTheme.colorScheme.outline
        LaunchedEffect(playerState?.mediaMetadata?.artworkUri) {
          mainActivityViewModel?.updatePalette(playerState)
        }
        LaunchedEffect(mainActivityViewModel?.palette?.value) {
          mainActivityViewModel?.updateColorList(defaultColor)
        }
        //endregion

        //region Animated Add Button Colors
        val animatedFabColor = animateColorAsState(
          targetValue =
          if (mainActivityViewModel?.palette?.value?.vibrantSwatch?.rgb != null
            || mainActivityViewModel?.palette?.value?.dominantSwatch?.rgb != null
          ) androidx.compose.ui.graphics.Color(
            mainActivityViewModel?.palette?.value?.vibrantSwatch?.rgb
              ?: mainActivityViewModel?.palette?.value?.dominantSwatch?.rgb!!
          )
          else MaterialTheme.colorScheme.primaryContainer,
          label = "Fab Color"
        )
        val animatedFabIconTint = animateColorAsState(
          targetValue =
          if (mainActivityViewModel?.palette?.value?.vibrantSwatch?.bodyTextColor != null
            || mainActivityViewModel?.palette?.value?.dominantSwatch?.bodyTextColor != null
          ) androidx.compose.ui.graphics.Color(
            mainActivityViewModel?.palette?.value?.vibrantSwatch?.bodyTextColor
              ?: mainActivityViewModel?.palette?.value?.dominantSwatch?.bodyTextColor!!
          ) else MaterialTheme.colorScheme.onPrimaryContainer,
          label = "Fab Icon Color"
        )
        //endregion

        LaunchedEffect(Unit) {
          mainActivityViewModel?.getStationList(false)
          mainActivityViewModel?.periodicUpdate(30)
        }

        DisposableEffect(mediaController) {
          mediaController?.run {
            playerState = state()
          }
          onDispose {
            playerState?.dispose()
          }
        }

        DisposableEffect(this) {
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
              containerColor = if ((!lazyGridState.canScrollBackward && radioListMode == true) || (!lazyListState.canScrollBackward) && radioListMode == false) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceContainer,
              titleContentColor = if ((!lazyGridState.canScrollBackward && radioListMode == true) || (!lazyListState.canScrollBackward) && radioListMode == false) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface,
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
              },
              currentMount = mainActivityViewModel?.nowPlayingData?.staticData?.value?.station?.mounts?.find { it.url == playerState?.currentMediaItem?.mediaId },
              songHistory = mainActivityViewModel?.nowPlayingData?.staticData?.value?.songHistory,
              playingNext = mainActivityViewModel?.nowPlayingData?.staticData?.value?.playingNext,
              nowPlaying = mainActivityViewModel?.nowPlayingData?.staticData?.value?.nowPlaying,
              palette = mainActivityViewModel?.palette,
              colorList = mainActivityViewModel?.colorList,
              isSleeping = mainActivityViewModel?.isSleeping
            )
          }
        }
      }
    }
  }
}
