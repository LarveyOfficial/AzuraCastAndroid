package com.larvey.azuracastplayer.ui.mainActivity

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.PaneExpansionAnchor
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.session.MediaController
import com.larvey.azuracastplayer.db.settings.SettingsViewModel
import com.larvey.azuracastplayer.db.settings.SettingsViewModel.SettingsModelProvider
import com.larvey.azuracastplayer.session.rememberManagedMediaController
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.state.state
import com.larvey.azuracastplayer.ui.discovery.Discovery
import com.larvey.azuracastplayer.ui.mainActivity.addStations.AddStationSheet
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import com.larvey.azuracastplayer.ui.mainActivity.components.FloatingExpressiveNavBar
import com.larvey.azuracastplayer.ui.mainActivity.components.MiniPlayer
import com.larvey.azuracastplayer.ui.mainActivity.radios.MyRadios
import com.larvey.azuracastplayer.ui.mainActivity.settings.SettingsSheet
import com.larvey.azuracastplayer.ui.nowplaying.ExpandingNowPlayer
import com.larvey.azuracastplayer.ui.nowplaying.NowPlayingPane
import com.larvey.azuracastplayer.ui.nowplaying.rememberExpandingPlayerState
import com.larvey.azuracastplayer.ui.theme.AzuraCastPlayerTheme
import com.larvey.azuracastplayer.classes.models.CastManager
import com.larvey.azuracastplayer.utils.ReverseLayoutDirection
import com.larvey.azuracastplayer.utils.albumColors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.view.KeyEvent
import javax.inject.Inject

/**
 * Width of the side navigation rail shown on tablets (Material3 `NavigationRail` container width).
 * The expanding Now Playing overlay is inset by this on portrait tablets so it never draws over the
 * rail (see the `startInset` passed to [ExpandingNowPlayer]).
 */
private val NavigationRailWidth = 80.dp

enum class AppDestinations(
  val label: String,
  val icon: ImageVector,
  val title: String,
) {
  STATIONS(
    "Stations",
    Icons.Rounded.Radio,
    "My Stations"
  ),
  DISCOVER(
    "Discover",
    Icons.Rounded.Public,
    "Discover"
  ),
  // Planned feature (favorites) — intentionally kept:
  //  FAVORITES(
  //    "Songs",
  //    Icons.Rounded.Favorite,
  //    "My Songs"
  //  )
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject
  lateinit var castManager: CastManager

  private var mainActivityViewModel: MainActivityViewModel? = null

  private var mediaController: MediaController? = null

  /**
   * While casting, the phone's hardware volume keys control the Cast receiver's
   * volume (consuming the event so the local media stream isn't changed instead).
   * When not casting, keys behave normally.
   */
  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    if (::castManager.isInitialized && castManager.isCasting.value) {
      when (event.keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP -> {
          if (event.action == KeyEvent.ACTION_DOWN) castManager.adjustRouteVolume(1)
          return true
        }
        KeyEvent.KEYCODE_VOLUME_DOWN -> {
          if (event.action == KeyEvent.ACTION_DOWN) castManager.adjustRouteVolume(-1)
          return true
        }
      }
    }
    return super.dispatchKeyEvent(event)
  }

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
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalMaterial3Api::class
  )
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      navigationBarStyle = SystemBarStyle.light(
        Color.TRANSPARENT,
        Color.TRANSPARENT
      ),
      statusBarStyle = SystemBarStyle.light(
        Color.TRANSPARENT,
        Color.TRANSPARENT
      )
    )
    setContent {
      var isLoading by remember { mutableStateOf(false) }

      LaunchedEffect(Unit) {
        delay(50)
        isLoading = true
      }

      val content: View = findViewById(android.R.id.content)
      content.viewTreeObserver.addOnPreDrawListener(
        object : ViewTreeObserver.OnPreDrawListener {
          override fun onPreDraw(): Boolean {
            return if (isLoading) {
              content.viewTreeObserver.removeOnPreDrawListener(this)
              true
            } else {
              false
            }
          }
        }
      )

      AzuraCastPlayerTheme {
        mainActivityViewModel = viewModel()
        mediaController = rememberManagedMediaController().value
        var playerState: PlayerState? by remember {
          mutableStateOf(mediaController?.state())
        }
        DisposableEffect(mediaController) {
          mediaController?.run {
            mainActivityViewModel?.sharedMediaController?.playerState?.value = state()
            playerState = state()
          }
          onDispose {
            playerState?.dispose()
            mainActivityViewModel?.sharedMediaController?.playerState?.value?.dispose()
          }
        }
        val defaultColor = MaterialTheme.colorScheme.outline
        LaunchedEffect(playerState?.mediaMetadata?.artworkUri) {
          mainActivityViewModel?.updatePalette(
            defaultColor
          )
        }

        val scope = rememberCoroutineScope()

        val showAddDialog = remember { mutableStateOf(false) }

        // Phone Now Playing is one continuous surface that grows from the mini bar into the full
        // screen; this drives its 0..1 expansion. (Tablets keep the side pane and the modal sheet.)
        val expandingPlayerState = rememberExpandingPlayerState()
        // Mini-player swipe-away progress, shared so the floating nav bar rounds its top corners
        // back in sync as the bar detaches. Written by the mini bar, read by the nav bar.
        var miniDismissProgress by remember { mutableFloatStateOf(0f) }
        // Measured nav-bar height, so it can slide straight down by its own height as the player opens.
        var navBarHeightPx by remember { mutableIntStateOf(0) }
        val settingsModel: SettingsViewModel = viewModel(factory = SettingsModelProvider.Factory)
        val radioListMode by settingsModel.gridView.collectAsState() // false = list, true = grid

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
        val discoveryViewingStation = remember { mutableStateOf(false) }
        // Briefly true while the discover detail pane is animating away, so the Now Playing pane
        // waits for it to finish instead of both moving at once (which jitters).
        var hidingDiscoverDetail by remember { mutableStateOf(false) }
        // Hides the wide mini bar first (before the discover detail closes), so it disappears at its
        // resting width instead of snapping to full width when its pane-reserved padding drops.
        var hideMiniForDock by remember { mutableStateOf(false) }

        //region List States for MyRadios
        val lazyListState = rememberLazyListState()
        val lazyGridState = rememberLazyGridState()
        //endregion

        //region List editing variables
        val editingList = remember { mutableStateOf(false) }
        val confirmEdit = remember { mutableStateOf(false) }
        //endregion

        //region Animated Add Button Colors
        val fabColors = albumColors(mainActivityViewModel?.palette?.value)
        val animatedFabColor = animateColorAsState(
          targetValue = fabColors.accent,
          label = "Fab Color"
        )

        val animatedFabIconTint = animateColorAsState(
          targetValue = fabColors.onAccent,
          label = "Fab Icon Color"
        )
        //endregion

        val settingsDrawer = rememberDrawerState(DrawerValue.Closed)

        val isWide = (windowSizeClass.minWidthDp != 0 && windowSizeClass.minHeightDp != 0)

        // Whether the Now Playing side pane can actually dock beside the content (two horizontal
        // partitions — expanded width, i.e. a landscape tablet). A portrait tablet is `isWide` but
        // has only ONE partition, so it can't split: it falls through to the expanding overlay player
        // like a phone. Derived from the directive (fresh window metrics), NOT the flaky navigator
        // pane value. This — not `isWide` — is what gates which Now Playing surface is used.
        val splitViewActive = customDirective.maxHorizontalPartitions > 1

        // Lift the FAB above the mini player when it's showing. The mini bar is now an overlay
        // (not in the Scaffold's bottomBar), so the Scaffold no longer reserves space for it.
        val fabMiniInset by animateDpAsState(
          targetValue = if (!splitViewActive && playerState?.currentMediaItem?.mediaId != null) 84.dp else 0.dp,
          label = "Fab Mini Inset"
        )

        // Phones get the custom FloatingExpressiveNavBar (rendered in the bottomBar), so suppress
        // the NavigationSuiteScaffold's own bar; tablets keep the stock NavigationRail.
        val navLayoutType = if (isWide) {
          NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
        } else {
          NavigationSuiteType.None
        }

        if (!isWide) {
          LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        }

        val topBarContainerColor = (
            if (currentDestination == AppDestinations.STATIONS) {
              if ((!lazyGridState.canScrollBackward && radioListMode == true)
                || (!lazyListState.canScrollBackward) && radioListMode == false
              )
                MaterialTheme.colorScheme.background
              else
                MaterialTheme.colorScheme.surfaceContainer
            } else {
              MaterialTheme.colorScheme.background
            })
        val topBarTitleColor = (
            if (currentDestination == AppDestinations.STATIONS) {
              if ((!lazyGridState.canScrollBackward && radioListMode == true)
                || (!lazyListState.canScrollBackward) && radioListMode == false
              )
                MaterialTheme.colorScheme.onBackground
              else
                MaterialTheme.colorScheme.onSurface
            } else {
              MaterialTheme.colorScheme.onBackground
            })

        BackHandler(currentDestination != AppDestinations.STATIONS) {
          if (isWide && currentDestination == AppDestinations.DISCOVER && discoveryViewingStation.value) {
            scope.launch {
              delay(1000)
              discoveryViewingStation.value = false
            }
          }
          currentDestination = AppDestinations.STATIONS
        }

        BackHandler(settingsDrawer.isOpen) {
          scope.launch {
            settingsDrawer.close()
          }
        }

        ReverseLayoutDirection {
          ModalNavigationDrawer(
            drawerState = settingsDrawer,
            gesturesEnabled = settingsDrawer.isOpen,
            drawerContent = {
              ReverseLayoutDirection {
                ModalDrawerSheet(
                  drawerState = settingsDrawer,
                  drawerShape = RoundedCornerShape(16.dp),
                  windowInsets = WindowInsets(0.dp)
                ) {
                  SettingsSheet(settingsDrawer)
                }
              }
            }
          ) {
            ReverseLayoutDirection {
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
                                contentDescription = it.label,
                              )
                            },
                            label = {
                              Text(
                                it.label,
                                maxLines = 1
                              )
                            },
                            selected = it == currentDestination,
                            onClick = {
                              if (!isWide) {
                                currentDestination = it
                                discoveryViewingStation.value = false
                              } else {
                                if (currentDestination == AppDestinations.DISCOVER && discoveryViewingStation.value) {
                                  currentDestination = it
                                  scope.launch {
                                    delay(1000)
                                    discoveryViewingStation.value = false
                                  }
                                } else {
                                  currentDestination = it
                                }
                              }
                            }
                          )
                        }
                      },
                      layoutType = navLayoutType,
                      navigationSuiteColors = NavigationSuiteDefaults.colors(
                        navigationRailContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                      )
                    ) {
                      Scaffold(
                        topBar = {
                          TopAppBar(
                            colors = topAppBarColors(
                              containerColor = topBarContainerColor,
                              titleContentColor = topBarTitleColor,
                            ),
                            title = {
                              Text(
                                currentDestination.title,
                                maxLines = 1
                              )
                            },
                            actions = {
                              AnimatedVisibility(
                                radioListMode != null && !editingList.value && currentDestination == AppDestinations.STATIONS,
                                exit = slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth * 2 }),
                                enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth * 2 })
                              ) {
                                IconButton(
                                  onClick = {
                                    settingsModel.toggleGridView()
                                  }
                                ) {
                                  Icon(
                                    imageVector = if (radioListMode == true) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                                    contentDescription = "Switch View"
                                  )
                                }
                              }
                              AnimatedVisibility(
                                currentDestination == AppDestinations.STATIONS && !editingList.value,
                                exit = slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth * 2 }),
                                enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth * 2 })
                              ) {
                                IconButton(
                                  onClick = {
                                    scope.launch {
                                      settingsDrawer.open()
                                    }
                                  }
                                ) {
                                  Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = "Settings"
                                  )
                                }
                              }
                            },
                            navigationIcon = {
                              if (!isWide && currentDestination == AppDestinations.DISCOVER && discoveryViewingStation.value) {
                                IconButton(
                                  onClick = {
                                    scope.launch {
                                      discoveryViewingStation.value = false
                                    }
                                  }
                                ) {
                                  Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Arrow Back"
                                  )
                                }
                              }
                            }
                          )
                        },
                        floatingActionButton = {
                          AnimatedVisibility(
                            visible = ((!lazyGridState.canScrollBackward && radioListMode == true) || (!lazyListState.canScrollBackward) && radioListMode == false) && !editingList.value && currentDestination == AppDestinations.STATIONS,
                            exit = slideOutHorizontally(targetOffsetX = { fullWidth ->
                              fullWidth * 2 * if (isWide) {
                                -1
                              } else {
                                1
                              }
                            }),
                            enter = slideInHorizontally(
                              initialOffsetX = { fullWidth ->
                                fullWidth * 2 * if (isWide) {
                                  -1
                                } else {
                                  1
                                }
                              },
                              animationSpec = tween(delayMillis = 320)
                            )
                          ) {
                            FloatingActionButton(
                              onClick = {
                                showAddDialog.value = true
                              },
                              modifier = Modifier.padding(bottom = fabMiniInset),
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
                              modifier = Modifier.padding(bottom = fabMiniInset),
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
                        floatingActionButtonPosition = if (isWide) {
                          FabPosition.Start
                        } else {
                          FabPosition.End
                        },
                        bottomBar = {
                          // On wide layouts the Now Playing surface lives in the supporting pane
                          // whenever something is playing and no Discover station's detail is up, so
                          // the bottom mini bar must be its exact complement — visible only when the
                          // pane is suppressed (a Discover station is being viewed / while it docks).
                          // Derived from the same flags as the pane rather than the scaffold
                          // navigator's pane-adapted value, which stops reporting "Expanded" across a
                          // fold/unfold and would let the mini and the pane both show at once.
                          // On phones the mini is the expanding overlay and this flag only drives the
                          // nav-bar fuse, so it's simply "is something playing".
                          val miniPlayerVisible = if (splitViewActive) {
                            playerState?.currentMediaItem?.mediaId != null &&
                              (discoveryViewingStation.value || hidingDiscoverDetail)
                          } else {
                            playerState?.currentMediaItem?.mediaId != null
                          }

                          val miniPlayer = @Composable {
                            MiniPlayer(
                              playerState = playerState,
                              showNowPlaying = {
                                // Wide layouts: instead of a full-screen sheet, sequence it so nothing
                                // collides: hide the mini bar first (at its resting width), then close
                                // the discover detail, then — once it's gone — dock the resizable Now
                                // Playing pane.
                                scope.launch {
                                  hideMiniForDock = true
                                  delay(120)
                                  hidingDiscoverDetail = true
                                  discoveryViewingStation.value = false
                                  delay(350)
                                  hidingDiscoverDetail = false
                                  hideMiniForDock = false
                                }
                              },
                              pause = {
                                mediaController?.pause()
                              },
                              play = {
                                mediaController?.play()
                              },
                              stop = {
                                mainActivityViewModel?.sharedMediaController?.mediaSession?.value?.player?.stop()
                              },
                              onDismissProgress = { miniDismissProgress = it },
                              palette = mainActivityViewModel?.palette,
                            )
                          }

                          if (splitViewActive) {
                            // Landscape tablet: the Now Playing side pane docks, so keep the stock
                            // NavigationRail and let this bottom mini bar re-dock the pane.
                            AnimatedVisibility(
                              visible = miniPlayerVisible && !hideMiniForDock,
                              enter = slideInVertically(
                                initialOffsetY = { fullHeight -> fullHeight * 2 },
                                animationSpec = tween(delayMillis = if (currentDestination == AppDestinations.DISCOVER) 200 else 0)
                              ),
                              exit = ExitTransition.None
                            ) {
                              BottomAppBar(
                                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                contentPadding = PaddingValues(0.dp)
                              ) {
                                // Keep the bar over the left (main/browse) column. On the Discover
                                // detail split the station details occupy the right pane (its width is
                                // the directive's preferred pane width), so reserve that on the right;
                                // otherwise cap the bar to a card width instead of stretching a wide
                                // layout edge-to-edge.
                                Box(
                                  modifier = Modifier
                                    .fillMaxSize()
                                    .padding(
                                      end = if (currentDestination == AppDestinations.DISCOVER && discoveryViewingStation.value) {
                                        customDirective.defaultPanePreferredWidth
                                      } else {
                                        0.dp
                                      }
                                    )
                                ) {
                                  Box(
                                    modifier = Modifier
                                      .fillMaxHeight()
                                      .widthIn(max = 560.dp)
                                      .align(Alignment.CenterStart)
                                  ) {
                                    miniPlayer()
                                  }
                                }
                              }
                            }
                          } else if (!isWide) {
                            // Phones: only the floating nav bar lives in the bar. The mini player is
                            // now the collapsed state of the expanding Now Playing surface, rendered
                            // as an overlay above everything (see ExpandingNowPlayer below), so it can
                            // grow out of this slot into the full screen. As that surface expands the
                            // nav bar slides straight down (by its own height) to make room.
                            Column(
                              modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { navBarHeightPx = it.height }
                                .graphicsLayer {
                                  translationY = navBarHeightPx.toFloat() * expandingPlayerState.expansion()
                                }
                                .navigationBarsPadding()
                            ) {
                              FloatingExpressiveNavBar(
                                destinations = AppDestinations.entries,
                                current = currentDestination,
                                fusedTop = miniPlayerVisible,
                                detachProgress = { miniDismissProgress },
                                onSelect = { destination ->
                                  currentDestination = destination
                                  discoveryViewingStation.value = false
                                }
                              )
                            }
                          }
                          // else: portrait tablet — the rail owns navigation and the expanding overlay
                          // owns the player, so the bottom bar stays empty.
                        }) { innerPadding ->
                        // The mini / expanding player is an OVERLAY (not in the Scaffold's bottomBar),
                        // so the Scaffold reserves no space for it. Add its footprint to the scroll
                        // content's bottom inset so the last list/grid items clear it instead of
                        // scrolling behind it (matches the FAB's 84dp mini inset).
                        val layoutDirection = LocalLayoutDirection.current
                        val contentInnerPadding = PaddingValues(
                          start = innerPadding.calculateStartPadding(layoutDirection),
                          top = innerPadding.calculateTopPadding(),
                          end = innerPadding.calculateEndPadding(layoutDirection),
                          bottom = innerPadding.calculateBottomPadding() +
                            (if (!splitViewActive && playerState?.currentMediaItem?.mediaId != null) 84.dp else 0.dp)
                        )
                        AnimatedVisibility(
                          radioListMode != null,
                          enter = slideInVertically(initialOffsetY = { fullHeight -> -fullHeight * 2 }),
                          exit = ExitTransition.None
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
                                MyRadios(
                                  innerPadding = contentInnerPadding,
                                  deleteRadio = { station ->
                                    mainActivityViewModel?.deleteStation(station)
                                  },
                                  editRadio = { newStation ->
                                    mainActivityViewModel?.editStation(newStation)
                                  },
                                  editingList = editingList,
                                  confirmEdit = confirmEdit,
                                  editAllStations = { stations ->
                                    scope.launch {
                                      mainActivityViewModel?.editAllStations(stations)
                                    }
                                  },
                                  lazyListState = lazyListState,
                                  lazyGridState = lazyGridState
                                )
                              }

                              AppDestinations.DISCOVER -> {
                                Discovery(
                                  contentInnerPadding,
                                  discoveryViewingStation,
                                  isWide
                                )
                              }

                              // Planned feature (favorites) — intentionally kept:
                              //                        AppDestinations.FAVORITES -> {
                              //                          Box(
                              //                            modifier = Modifier
                              //                              .fillMaxSize()
                              //                          )
                              //                        }
                            }
                          }
                        }
                      }
                    }
                  }
                },
                supportingPane = {
                  AnimatedVisibility(
                    visible = playerState?.currentMediaItem != null && !discoveryViewingStation.value && !hidingDiscoverDetail,
                    enter = slideInHorizontally(
                      initialOffsetX = { fullWidth -> fullWidth * 2 }
                    ),
                    exit = slideOutHorizontally(
                      targetOffsetX = { fullWidth -> fullWidth * 2 },
                      animationSpec = tween(durationMillis = 250)
                    )
                  ) {
                    AnimatedPane(modifier = Modifier.fillMaxSize()) {
                      NowPlayingPane(
                        palette = mainActivityViewModel?.palette,
                        colorList = mainActivityViewModel?.colorList
                      )
                    }
                  }
                },
                paneExpansionState = rememberPaneExpansionState(
                  navigator.scaffoldValue,
                  anchors = listOf(
                    PaneExpansionAnchor.Proportion(0f),
                    PaneExpansionAnchor.Proportion(0.40f),
                    PaneExpansionAnchor.Proportion(0.50f),
                    PaneExpansionAnchor.Proportion(0.60f),
                    PaneExpansionAnchor.Proportion(0.70f)
                  ),
                  initialAnchoredIndex = 4 // This doesn't work :3
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
            }

            // The expanding Now Playing surface lives here — a SECOND child of the drawer content,
            // stacked over the scaffold, so the settings drawer sheet draws OVER it. It used to be a
            // sibling of the whole drawer and painted on top of the opened settings sheet. Re-wrapped
            // in ReverseLayoutDirection because the entire drawer is flipped to sit on the right edge.
            // Shown whenever the side pane can't dock (phones AND portrait tablets) — the docking side
            // pane replaces it only when the horizontal split is actually available.
            if (!splitViewActive) {
              // When playback ends, snap back to the mini bar so the next song opens collapsed.
              LaunchedEffect(playerState?.currentMediaItem?.mediaId) {
                if (playerState?.currentMediaItem?.mediaId == null) {
                  expandingPlayerState.snapToCollapsed()
                }
              }
              ReverseLayoutDirection {
                AnimatedVisibility(
                  visible = playerState?.currentMediaItem?.mediaId != null,
                  // Slide the collapsed mini bar in from the side; exit is instant (the animated stop
                  // already slides it away before it disappears).
                  enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }),
                  exit = ExitTransition.None
                ) {
                  ExpandingNowPlayer(
                    state = expandingPlayerState,
                    playerState = playerState,
                    palette = mainActivityViewModel?.palette,
                    colorList = mainActivityViewModel?.colorList,
                    nowPlaying = { mainActivityViewModel?.nowPlayingData?.staticData?.value?.nowPlaying },
                    play = { mediaController?.play() },
                    pause = { mediaController?.pause() },
                    stop = {
                      // While casting, Stop must end the cast session entirely; otherwise
                      // just stop the local player.
                      if (mainActivityViewModel?.castManager?.isCasting?.value == true) {
                        mainActivityViewModel?.castManager?.stopCasting()
                      } else {
                        mainActivityViewModel?.sharedMediaController?.mediaSession?.value?.player?.stop()
                      }
                    },
                    onDismissProgress = { miniDismissProgress = it },
                    // Portrait tablet keeps the side rail (no floating bottom nav bar), so the mini bar
                    // rests just above the system nav inset instead of reserving the nav bar footprint,
                    // and is inset past the rail so it never draws over it. Phones: neither applies.
                    reserveNavBarArea = !isWide,
                    startInset = if (isWide) NavigationRailWidth else 0.dp
                  )
                }
              }
            }

          }
        }

        when {
          showAddDialog.value -> {
            AddStationSheet(
              hideSheet = {
                showAddDialog.value = false
              },
              addData = { stations ->
                mainActivityViewModel?.addStation(
                  stations
                )
              },
              currentStationCount = mainActivityViewModel?.savedStationsDB?.savedStations?.value?.size
                ?: 0
            )
          }
        }

      }
    }
  }

}

@Composable
fun LockScreenOrientation(orientation: Int) {
  val context = LocalContext.current
  DisposableEffect(Unit) {
    val activity = context.findActivity() ?: return@DisposableEffect onDispose {}
    val originalOrientation = activity.requestedOrientation
    activity.requestedOrientation = orientation
    onDispose {
      // restore original orientation when view disappears
      activity.requestedOrientation = originalOrientation
    }
  }
}

fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}
