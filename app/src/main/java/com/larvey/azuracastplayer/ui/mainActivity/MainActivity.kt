package com.larvey.azuracastplayer.ui.mainActivity

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneExpansionAnchor
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.session.MediaController
import androidx.window.core.layout.WindowSizeClass
import com.larvey.azuracastplayer.db.settings.SettingsViewModel
import com.larvey.azuracastplayer.db.settings.SettingsViewModel.SettingsModelProvider
import com.larvey.azuracastplayer.session.rememberManagedMediaController
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.state.state
import com.larvey.azuracastplayer.ui.mainActivity.addStations.AddStationDialog
import com.larvey.azuracastplayer.ui.mainActivity.components.MiniPlayer
import com.larvey.azuracastplayer.ui.mainActivity.radios.MyRadios
import com.larvey.azuracastplayer.ui.nowplaying.NowPlayingPane
import com.larvey.azuracastplayer.ui.nowplaying.NowPlayingSheet
import com.larvey.azuracastplayer.ui.theme.AzuraCastPlayerTheme
import dagger.hilt.android.AndroidEntryPoint

enum class AppDestinations(
  val label: String,
  val icon: ImageVector
) {
  STATIONS(
    "Stations",
    Icons.Rounded.Radio
  ),
  DISCOVER(
    "Discover",
    Icons.Rounded.Public
  ),
  FAVORITES(
    "Favorites",
    Icons.Rounded.Favorite
  )
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  private var mainActivityViewModel: MainActivityViewModel? = null

  private var mediaController: MediaController? = null

  override fun onPause() {
    super.onPause()
    mainActivityViewModel?.pauseActivity()
  }

  override fun onResume() {
    super.onResume()
    mainActivityViewModel?.resumeActivity()
  }

  override fun onDestroy() {
    super.onDestroy()
    mediaController?.run {
      pause()
      stop()
      release()
    }
  }


  @OptIn(
    ExperimentalMaterial3AdaptiveApi::class
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
        val showAddDialog = remember { mutableStateOf(false) }
        val showNowPlayingSheet = remember { mutableStateOf(false) }
        val settingsModel: SettingsViewModel = viewModel(factory = SettingsModelProvider.Factory)
        val radioListMode by settingsModel.gridView.collectAsState() // false = list, true = grid
        mediaController = rememberManagedMediaController().value

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
        }

        //        LaunchedEffect(radioListMode) {
        //          mainActivityViewModel?.notifySessionStationsUpdated()
        //        }

        DisposableEffect(mediaController) {
          mediaController?.run {
            playerState = state()
          }
          onDispose {
            playerState?.dispose()
          }
        }
        val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
        val systemDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())
        val customDirective = PaneScaffoldDirective(
          maxHorizontalPartitions = systemDirective.maxHorizontalPartitions,
          horizontalPartitionSpacerSize = 0.dp,
          maxVerticalPartitions = systemDirective.maxVerticalPartitions,
          verticalPartitionSpacerSize = systemDirective.verticalPartitionSpacerSize,
          defaultPanePreferredWidth = systemDirective.defaultPanePreferredWidth,
          excludedBounds = systemDirective.excludedBounds
        )
        val navigator = rememberSupportingPaneScaffoldNavigator(scaffoldDirective = customDirective)

        var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.STATIONS) }

        SupportingPaneScaffold(
          modifier = Modifier.background(MaterialTheme.colorScheme.background),
          directive = navigator.scaffoldDirective,
          value = navigator.scaffoldValue,
          mainPane = {
            AnimatedPane(modifier = Modifier.fillMaxSize()) {
              NavigationSuiteScaffold(
                navigationSuiteItems = {
                  AppDestinations.entries.forEach {
                    item(
                      icon = {
                        Icon(
                          it.icon,
                          contentDescription = it.label
                        )
                      },
                      label = { Text(it.label) },
                      selected = it == currentDestination,
                      onClick = {
                        currentDestination = it
                      }
                    )
                  }
                },
                navigationSuiteColors = NavigationSuiteDefaults.colors(
                  navigationRailContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                )
              ) {
                AnimatedContent(
                  currentDestination,
                  label = "Current Location",
                  transitionSpec = {
                    if (windowSizeClass.minWidthDp != 0 && windowSizeClass.minHeightDp != 0) {
                      if (targetState.ordinal > initialState.ordinal) {
                        slideInVertically(initialOffsetY = { fullHeight -> fullHeight * 2 }) togetherWith slideOutVertically(
                          targetOffsetY = { fullHeight -> -fullHeight * 2 }
                        )
                      } else {
                        slideInVertically(initialOffsetY = { fullHeight -> -fullHeight * 2 }) togetherWith slideOutVertically(
                          targetOffsetY = { fullHeight -> fullHeight * 2 }
                        )
                      }
                    } else {
                      if (targetState.ordinal > initialState.ordinal) {
                        slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth * 2 }) togetherWith slideOutHorizontally(
                          targetOffsetX = { fullWidth -> -fullWidth * 2 }
                        )
                      } else {
                        slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth * 2 }) togetherWith slideOutHorizontally(
                          targetOffsetX = { fullWidth -> fullWidth * 2 }
                        )
                      }
                    }
                  }
                ) { destination ->
                  when (destination) {
                    AppDestinations.STATIONS -> {
                      MyStationsPage(
                        lazyGridState,
                        radioListMode,
                        lazyListState,
                        editingList,
                        settingsModel,
                        showAddDialog,
                        animatedFabColor,
                        animatedFabIconTint,
                        confirmEdit,
                        playerState,
                        navigator,
                        showNowPlayingSheet,
                        windowSizeClass
                      )
                    }

                    AppDestinations.FAVORITES -> {
                      Box(
                        modifier = Modifier
                          .fillMaxSize()
                      )
                    }

                    AppDestinations.DISCOVER -> {
                      Box(
                        modifier = Modifier
                          .fillMaxSize()
                      )
                    }
                  }
                }

              }
            }
          },
          supportingPane = {
            AnimatedVisibility(
              visible = playerState?.currentMediaItem != null,
              enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth * 2 })
            ) {
              AnimatedPane(modifier = Modifier.fillMaxSize()) {
                NowPlayingPane(
                  playerState = playerState,
                  pause = {
                    mediaController?.pause()
                  },
                  play = {
                    mediaController?.play()
                  },
                  stop = {
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
          },
          paneExpansionState = rememberPaneExpansionState(
            navigator.scaffoldValue,
            anchors = listOf(
              PaneExpansionAnchor.Proportion(0f),
              PaneExpansionAnchor.Proportion(0.50f),
              PaneExpansionAnchor.Proportion(0.60f),
              PaneExpansionAnchor.Proportion(0.65f)
            )
          ),
          paneExpansionDragHandle = { state ->
            val interactionSource =
              remember { MutableInteractionSource() }
            VerticalDragHandle(
              modifier =
              Modifier.paneExpansionDraggable(
                state,
                LocalMinimumInteractiveComponentSize.current,
                interactionSource,
                semanticsProperties = {}
              ),
              interactionSource = interactionSource
            )
          }
        )
        when {
          showAddDialog.value -> {
            AddStationDialog(
              hideDialog = { showAddDialog.value = false },
              addData = { stations ->
                mainActivityViewModel?.addStation(
                  stations
                )
              },
              currentStationCount = mainActivityViewModel?.savedRadioList?.size ?: 0
            )
          }

          showNowPlayingSheet.value -> {
            NowPlayingSheet(
              hideNowPlaying = {
                showNowPlayingSheet.value = false
              },
              playerState = playerState,
              pause = {
                mediaController?.pause()
              },
              play = {
                mediaController?.play()
              },
              stop = {
                showNowPlayingSheet.value = false
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

  @Composable
  @OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3AdaptiveApi::class
  )
  private fun MyStationsPage(
    lazyGridState: LazyGridState,
    radioListMode: Boolean?,
    lazyListState: LazyListState,
    editingList: MutableState<Boolean>,
    settingsModel: SettingsViewModel,
    showAddDialog: MutableState<Boolean>,
    animatedFabColor: State<androidx.compose.ui.graphics.Color>,
    animatedFabIconTint: State<androidx.compose.ui.graphics.Color>,
    confirmEdit: MutableState<Boolean>,
    playerState: PlayerState?,
    navigator: ThreePaneScaffoldNavigator<Any>,
    showNowPlayingSheet: MutableState<Boolean>,
    windowSizeClass: WindowSizeClass
  ) {
    val isWide = (windowSizeClass.minWidthDp != 0 && windowSizeClass.minHeightDp != 0)

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
          exit = slideOutHorizontally(targetOffsetX = { fullWidth ->
            fullWidth * 2 * if (isWide) {
              -1
            } else {
              1
            }
          }),
          enter = slideInHorizontally(initialOffsetX = { fullWidth ->
            fullWidth * 2 * if (isWide) {
              -1
            } else {
              1
            }
          }),
        ) {
          FloatingActionButton(
            onClick = {
              showAddDialog.value = true
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
          exit = slideOutHorizontally(targetOffsetX = { fullWidth ->
            fullWidth * 2 * if (isWide) {
              -1
            } else {
              1
            }
          }),
          enter = slideInHorizontally(initialOffsetX = { fullWidth ->
            fullWidth * 2 * if (isWide) {
              -1
            } else {
              1
            }
          }),
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
      floatingActionButtonPosition =
      if (isWide) {
        FabPosition.Start
      } else {
        FabPosition.End
      },
      bottomBar = {
        AnimatedVisibility(
          visible = playerState?.currentMediaItem?.mediaId != null && navigator.scaffoldState.currentState.secondary != PaneAdaptedValue.Expanded,
          enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight * 2 }),
          exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight * 2 })
        ) {
          BottomAppBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
          ) {
            MiniPlayer(
              playerState = playerState,
              showNowPlaying = {
                showNowPlayingSheet.value = true
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
        enter = slideInVertically(initialOffsetY = { fullHeight -> -fullHeight * 2 }),
        exit = ExitTransition.None
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
  }
}
