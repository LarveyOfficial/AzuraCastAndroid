package com.larvey.azuracastplayer.ui.nowplaying

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.palette.graphics.Palette
import com.larvey.azuracastplayer.db.settings.SettingsViewModel
import com.larvey.azuracastplayer.db.settings.SettingsViewModel.SettingsModelProvider
import com.larvey.azuracastplayer.ui.nowplaying.components.AnimatedBackgroundColor
import com.larvey.azuracastplayer.ui.nowplaying.components.BlurImageBackground
import com.larvey.azuracastplayer.ui.nowplaying.components.NowPlayingAlbumArt
import com.larvey.azuracastplayer.ui.nowplaying.components.NowPlayingBottomBar
import com.larvey.azuracastplayer.ui.nowplaying.components.NowPlayingHistory
import com.larvey.azuracastplayer.ui.nowplaying.components.NowPlayingTopBar
import com.larvey.azuracastplayer.ui.nowplaying.components.SongAndArtist
import com.larvey.azuracastplayer.ui.nowplaying.cast.CastDeviceSheet

/**
 * The full-screen Now Playing body, decoupled from any sheet/container. It is rendered as the
 * "expanded" layer of the [ExpandingNowPlayer]: the container owns positioning, growth, and corner
 * clipping, so this composable simply fills whatever bounds it is given.
 *
 * [onCollapse] shrinks the surface back to the mini bar (top-bar close button, back gesture, and
 * queue back-navigation all route through it). The tablet side-pane keeps its own parallel copy in
 * [NowPlayingPane]; this one serves the phone expanding player.
 */
@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalSharedTransitionApi::class
)
@Composable
fun NowPlayingContent(
  palette: MutableState<Palette?>?,
  colorList: MutableState<List<Color>>?,
  onCollapse: () -> Unit,
  onNowPlayingRouteChange: (Boolean) -> Unit = {},
  // Overrides the Stop button action; the expanding surface passes an animated collapse-then-dismiss
  // so stopping doesn't just make the surface vanish. Null = stop the player directly.
  onStop: (() -> Unit)? = null,
  // The expanding surface draws the album background itself (so it also shows behind the drag
  // placeholder), so it renders this content with drawBackground = false.
  drawBackground: Boolean = true
) {
  val nowPlayingViewModel: NowPlayingViewModel = viewModel()

  val playerState = nowPlayingViewModel.sharedMediaController.playerState.value ?: return

  val castManager = nowPlayingViewModel.castManager
  var showCastSheet by remember { mutableStateOf(false) }

  val navController = rememberNavController()
  val scrollState = rememberLazyListState()

  // Report whether we're on the (scrollable) queue route so the surrounding expanding surface can
  // yield its vertical drag to the list there.
  val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
  LaunchedEffect(currentRoute) {
    onNowPlayingRouteChange(currentRoute != "queue")
  }

  // Only the queue route intercepts back here (to return to the now-playing view). Collapsing the
  // whole surface is owned by the expanding player's predictive-back handler.
  BackHandler(enabled = currentRoute == "queue") {
    navController.navigateUp()
  }

  if (showCastSheet) {
    CastDeviceSheet(
      castManager = castManager,
      castConnectivity = nowPlayingViewModel.castConnectivity,
      palette = palette?.value,
      onDismiss = { showCastSheet = false }
    )
  }

  Box(modifier = Modifier.fillMaxSize()) {
    if (drawBackground) {
      NowPlayingBackground(
        playerState = playerState,
        colorList = colorList
      )
    }
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = Color.Transparent,
      topBar = {
        // Content is laid out at full screen height (behind the status bar), so the top bar carries
        // its own status-bar inset.
        Box(modifier = Modifier.statusBarsPadding()) {
          NowPlayingTopBar(
            onClose = onCollapse,
            onToggleHistory = {
              if (navController.currentDestination?.route == "nowPlaying") {
                navController.navigate("queue")
              } else {
                navController.popBackStack(
                  route = "nowPlaying",
                  inclusive = false
                )
              }
            },
            onCast = if (castManager.isCastAvailable.value) {
              { showCastSheet = true }
            } else null,
            isCasting = castManager.isCasting.value,
            isConnecting = castManager.isConnecting.value
          )
        }
      },
      bottomBar = {
        NowPlayingBottomBar(
          stop = onStop ?: {
            if (castManager.isCasting.value) {
              castManager.stopCasting()
            } else {
              nowPlayingViewModel.sharedMediaController.mediaSession.value?.player?.stop()
            }
            Unit
          },
          pause = {
            nowPlayingViewModel.sharedMediaController.mediaSession.value?.player?.pause()
          },
          play = {
            nowPlayingViewModel.sharedMediaController.mediaSession.value?.player?.play()
          },
          playerState = playerState,
          currentMount = nowPlayingViewModel.nowPlayingData.staticData.value?.station?.mounts?.find { it.url == playerState.currentMediaItem?.mediaId },
          palette = palette?.value,
          nowPlaying = { nowPlayingViewModel.nowPlayingData.staticData.value?.nowPlaying },
          isSleeping = nowPlayingViewModel.sharedMediaController.isSleeping
        )
      }
    ) { innerPadding ->
      SharedTransitionLayout {
        NavHost(
          navController = navController,
          startDestination = "nowPlaying",
          popExitTransition = {
            scaleOut(
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = 600f
              ),
              transformOrigin = TransformOrigin(
                0.25f,
                0.25f
              )
            ) + fadeOut()
          },
          exitTransition = {
            slideOutHorizontally(
              targetOffsetX = { width -> -width },
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = 600f
              )
            ) + fadeOut()
          },
          enterTransition = {
            slideInHorizontally(
              initialOffsetX = { width -> -width },
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = 600f
              )
            ) + fadeIn()
          }
        ) {
          composable("nowPlaying") {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Bottom,
              modifier = Modifier
                .padding(bottom = innerPadding.calculateBottomPadding())
                .fillMaxHeight()
                .fillMaxWidth()
//                .padding(bottom = 32.dp)
            ) {
              NowPlayingAlbumArt(
                modifier = Modifier.fillMaxHeight(.75f),
                playerState = playerState,
                sharedTransitionScope = this@SharedTransitionLayout,
                animatedVisibilityScope = this@composable,
              )
              SongAndArtist(
                songName = playerState.mediaMetadata?.displayTitle.toString(),
                artistName = playerState.mediaMetadata?.artist.toString(),
                small = false
              )
            }
          }
          composable("queue") {
            NowPlayingHistory(
              innerPadding = innerPadding,
              playerState = playerState,
              songHistory = nowPlayingViewModel.nowPlayingData.staticData.value?.songHistory,
              playingNext = nowPlayingViewModel.nowPlayingData.staticData.value?.playingNext,
              scrollState = scrollState,
              toggleQueueVisibility = {
                if (navController.currentDestination?.route == "nowPlaying") {
                  navController.navigate("queue")
                } else {
                  navController.navigateUp()
                }
              },
              sharedTransitionScope = this@SharedTransitionLayout,
              animatedVisibilityScope = this@composable
            )
          }
        }
      }
    }
  }
}

/**
 * The album-derived Now Playing backdrop (animated mesh gradient, or blurred art on old devices /
 * when the legacy option is on). Shared so the expanding surface can draw it behind the drag
 * placeholder as well as behind the live content.
 */
@Composable
fun NowPlayingBackground(
  playerState: com.larvey.azuracastplayer.state.PlayerState,
  colorList: MutableState<List<Color>>?
) {
  val settingsModel: SettingsViewModel = viewModel(factory = SettingsModelProvider.Factory)
  val legacyBackground by settingsModel.legacyMediaBackground.collectAsState()
  if (Build.VERSION.SDK_INT <= 28 || legacyBackground) {
    BlurImageBackground(playerState = playerState)
  } else {
    AnimatedBackgroundColor(colorList)
  }
}
