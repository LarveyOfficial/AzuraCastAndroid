package com.larvey.azuracastplayer.ui.nowplaying

import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.palette.graphics.Palette
import com.larvey.azuracastplayer.db.settings.SettingsViewModel
import com.larvey.azuracastplayer.db.settings.SettingsViewModel.SettingsModelProvider
import com.larvey.azuracastplayer.ui.nowplaying.components.AnimatedBackgroundColor
import com.larvey.azuracastplayer.ui.nowplaying.components.BlurImageBackground
import com.larvey.azuracastplayer.ui.nowplaying.components.NowPlayingAlbumArt
import com.larvey.azuracastplayer.ui.nowplaying.components.NowPlayingBottomBar
import com.larvey.azuracastplayer.ui.nowplaying.components.NowPlayingHistory
import com.larvey.azuracastplayer.ui.nowplaying.components.SongAndArtist
import com.larvey.azuracastplayer.utils.getRoundedCornerRadius
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch


@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalSharedTransitionApi::class
)
@Composable
fun NowPlayingSheet(
  hideNowPlaying: () -> Unit,
  palette: MutableState<Palette?>?,
  colorList: MutableState<List<Color>>?
) {
  val settingsModel: SettingsViewModel = viewModel(factory = SettingsModelProvider.Factory)
  val legacyBackground by settingsModel.legacyMediaBackground.collectAsState()

  val nowPlayingViewModel: NowPlayingViewModel = viewModel()

  if (nowPlayingViewModel.sharedMediaController.playerState.value?.currentMediaItem == null) hideNowPlaying()
  else {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var sheetSize by remember { mutableFloatStateOf(1f) }
    var sheetOriginYOffset by remember { mutableFloatStateOf(0.25f) }


    val navController = rememberNavController()

    val scope = rememberCoroutineScope()


    val scrollState = rememberLazyListState()

    ModalBottomSheet(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          scaleX = sheetSize
          scaleY = sheetSize
          transformOrigin = TransformOrigin(
            0.5f,
            sheetOriginYOffset
          )
        },
      containerColor = Color.Transparent,
      sheetState = sheetState,
      shape = RoundedCornerShape(if (getRoundedCornerRadius() > 0.dp) 24.dp else 0.dp),
      onDismissRequest = {
        hideNowPlaying()
      },
      dragHandle = {},
      contentWindowInsets = {
        WindowInsets(
          0,
          0,
          0,
          0
        )
      },
      sheetGesturesEnabled = !scrollState.canScrollBackward,
      properties = ModalBottomSheetProperties(
        isAppearanceLightNavigationBars = false,
        isAppearanceLightStatusBars = false,
        shouldDismissOnBackPress = false
      )
    ) {
      BackHandler(
        enabled = true
      ) {
        if (navController.currentDestination?.route == "queue") {
          navController.navigateUp()
        } else {
          scope.launch {
            sheetState.hide()
            hideNowPlaying()
          }
        }
      }

      PredictiveBackHandler(navController.currentDestination?.route == "nowPlaying") { progress: Flow<BackEventCompat> ->
        try {
          progress.collect { backEvent ->
            sheetSize = 1f - (0.15f * backEvent.progress)
            sheetOriginYOffset = 0.25f + (0.75f * backEvent.progress)
          }
          sheetOriginYOffset = 1f
          scope.launch {
            sheetState.hide()
            hideNowPlaying()
          }
        } catch (e: CancellationException) {
          sheetSize = 1f
          sheetOriginYOffset = 0.25f
        }
      }



      Box(
        modifier = Modifier
          .fillMaxSize()
          .clip(RoundedCornerShape(if (getRoundedCornerRadius() > 0.dp) 24.dp else 0.dp))
      ) {
        if (Build.VERSION.SDK_INT <= 28 || legacyBackground) {
          BlurImageBackground(playerState = nowPlayingViewModel.sharedMediaController.playerState.value)
        } else {
          AnimatedBackgroundColor(colorList)
        }
        Scaffold(
          modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
          containerColor = Color.Transparent,
          bottomBar = {
            NowPlayingBottomBar(
              toggleQueueVisibility = {
                if (navController.currentDestination?.route == "nowPlaying") {
                  navController.navigate("queue")
                } else {
                  navController.navigateUp()
                }
              },
              sheetState = sheetState,
              stop = {
                nowPlayingViewModel.sharedMediaController.mediaSession.value?.player?.stop()
              },
              pause = {
                nowPlayingViewModel.sharedMediaController.mediaSession.value?.player?.pause()
              },
              play = {
                nowPlayingViewModel.sharedMediaController.mediaSession.value?.player?.play()
              },
              playerState = nowPlayingViewModel.sharedMediaController.playerState.value!!,
              currentMount = nowPlayingViewModel.nowPlayingData.staticData.value?.station?.mounts?.find { it.url == nowPlayingViewModel.sharedMediaController.playerState.value?.currentMediaItem?.mediaId },
              palette = palette?.value,
              nowPlaying = nowPlayingViewModel.nowPlayingData.staticData.value?.nowPlaying,
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
                    .padding(innerPadding)
                    .fillMaxHeight()
                    .padding(bottom = 16.dp)
                ) {
                  NowPlayingAlbumArt(
                    modifier = Modifier.fillMaxHeight(.75f),
                    playerState = nowPlayingViewModel.sharedMediaController.playerState.value!!,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                  )
                  SongAndArtist(
                    songName = nowPlayingViewModel.sharedMediaController.playerState.value?.mediaMetadata?.displayTitle.toString(),
                    artistName = nowPlayingViewModel.sharedMediaController.playerState.value?.mediaMetadata?.artist.toString(),
                    small = false
                  )
                }
              }
              composable("queue") {
                NowPlayingHistory(
                  innerPadding = innerPadding,
                  playerState = nowPlayingViewModel.sharedMediaController.playerState.value!!,
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
  }
}

