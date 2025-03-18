package com.larvey.azuracastplayer.ui.nowplaying

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.palette.graphics.Palette
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.larvey.azuracastplayer.ui.mainActivity.components.meshGradient
import com.larvey.azuracastplayer.ui.nowplaying.components.NowPlayingAlbumArt
import com.larvey.azuracastplayer.ui.nowplaying.components.NowPlayingBottomBar
import com.larvey.azuracastplayer.ui.nowplaying.components.NowPlayingHistory
import com.larvey.azuracastplayer.ui.nowplaying.components.SongAndArtist
import com.larvey.azuracastplayer.utils.BlurImageBackground
import com.larvey.azuracastplayer.utils.conditional

@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalSharedTransitionApi::class,
  ExperimentalGlideComposeApi::class
)
@Composable
fun NowPlayingPane(
  palette: MutableState<Palette?>?,
  colorList: MutableState<List<Color>>?
) {

  val nowPlayingViewModel: NowPlayingViewModel = viewModel()


  AnimatedVisibility(
    visible = nowPlayingViewModel.sharedMediaController.playerState.value?.currentMediaItem != null,
    enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth * 2 }),
    exit = slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth * 2 })
  ) {
    nowPlayingViewModel.sharedMediaController.playerState.value!!
    val transitionA = rememberInfiniteTransition(label = "X")
    val transitionB = rememberInfiniteTransition(label = "Y")

    val navController = rememberNavController()

    //region Animate Gradient Movement
    val animateA by transitionA.animateFloat(
      initialValue = 0.3f,
      targetValue = 0.8f,
      animationSpec = infiniteRepeatable(
        animation = tween(22000),
        repeatMode = RepeatMode.Reverse
      ),
      label = "X"
    )
    val animateB by transitionB.animateFloat(
      initialValue = 0.4f,
      targetValue = 0.7f,
      animationSpec = infiniteRepeatable(
        animation = tween(13000),
        repeatMode = RepeatMode.Reverse
      ),
      label = "Y"
    )
    //endregion

    val scrollState = rememberLazyListState()

    BackHandler(
      enabled = navController.currentDestination?.route == "queue"
    ) {
      navController.popBackStack(
        route = "nowPlaying",
        inclusive = false
      )
    }
    Box(
      modifier = Modifier
        .fillMaxSize()
        .conditional(colorList != null && Build.VERSION.SDK_INT >= 29) {
          meshGradient(
            resolutionX = 16,
            resolutionY = 16,
            points = listOf(
              // @formatter:off
                listOf(
                  Offset(0f, 0f) to colorList!!.value[0], // No move
                  Offset(animateA, 0f) to colorList.value[1], // Only x moves
                  Offset(1f, 0f) to colorList.value[2], // No move
                ), listOf(
                  Offset(0f, animateB) to colorList.value[3], // Only y moves
                  Offset(animateB, 1f - animateA) to colorList.value[4],
                  Offset(1f, 1f - animateA) to colorList.value[5], // Only y moves
                ), listOf(
                  Offset(0f, 1f) to colorList.value[6], // No move
                  Offset(1f - animateB, 1f) to colorList.value[7], //Only x moves
                  Offset(1f, 1f) to colorList.value[8], // No move
                )
                // @formatter:on
            )
          )
        }

    ) {
      if (Build.VERSION.SDK_INT <= 28) {
        BlurImageBackground(playerState = nowPlayingViewModel.sharedMediaController.playerState.value)
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
                navController.popBackStack(
                  route = "nowPlaying",
                  inclusive = false
                )
              }
            },
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
                  stiffness = Spring.StiffnessLow
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
                  stiffness = Spring.StiffnessLow
                )
              )
            },
            enterTransition = {
              slideInHorizontally(
                initialOffsetX = { width -> -width },
                animationSpec = spring(
                  dampingRatio = Spring.DampingRatioLowBouncy,
                  stiffness = Spring.StiffnessLow
                )
              )
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
                  .padding(bottom = 32.dp)
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
                    navController.popBackStack(
                      route = "nowPlaying",
                      inclusive = false
                    )
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
