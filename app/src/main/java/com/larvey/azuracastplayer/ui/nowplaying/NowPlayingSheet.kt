package com.larvey.azuracastplayer.ui.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.palette.graphics.Palette
import com.larvey.azuracastplayer.ui.mainActivity.components.meshGradient
import com.larvey.azuracastplayer.ui.nowplaying.components.NowPlayingAlbumArt
import com.larvey.azuracastplayer.ui.nowplaying.components.NowPlayingBottomBar
import com.larvey.azuracastplayer.ui.nowplaying.components.NowPlayingHistory
import com.larvey.azuracastplayer.ui.nowplaying.components.SongAndArtist
import com.larvey.azuracastplayer.utils.conditional
import com.larvey.azuracastplayer.utils.getRoundedCornerRadius
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

  val nowPlayingViewModel: NowPlayingViewModel = viewModel()

  if (nowPlayingViewModel.sharedMediaController.playerState.value?.currentMediaItem == null) hideNowPlaying()
  else {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val transitionA = rememberInfiniteTransition(label = "X")
    val transitionB = rememberInfiniteTransition(label = "Y")

    val scope = rememberCoroutineScope()

    val showQueue = remember { mutableStateOf(false) }

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

    ModalBottomSheet(
      modifier = Modifier.fillMaxSize(),
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
      sheetGesturesEnabled = !scrollState.canScrollBackward
    ) {
      BackHandler(
        enabled = true
      ) {
        if (showQueue.value) {
          showQueue.value = false
        } else {
          scope.launch {
            sheetState.hide()
            hideNowPlaying()
          }

        }
      }
      Box(
        modifier = Modifier
          .fillMaxSize()
          .conditional(colorList != null) {
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
        Scaffold(
          modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
          containerColor = Color.Transparent,
          bottomBar = {

            NowPlayingBottomBar(
              showQueue = showQueue,
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
            AnimatedContent(showQueue.value) { targetState ->
              if (!targetState) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Bottom,
                  modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxHeight()
                    .padding(bottom = 32.dp)
                ) {
                  NowPlayingAlbumArt(
                    modifier = Modifier.fillMaxHeight(.75f),
                    playerState = nowPlayingViewModel.sharedMediaController.playerState.value!!,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent,
                  )
                  SongAndArtist(
                    songName = nowPlayingViewModel.sharedMediaController.playerState.value?.mediaMetadata?.displayTitle.toString(),
                    artistName = nowPlayingViewModel.sharedMediaController.playerState.value?.mediaMetadata?.artist.toString(),
                    small = false
                  )
                }
              } else {
                NowPlayingHistory(
                  innerPadding = innerPadding,
                  playerState = nowPlayingViewModel.sharedMediaController.playerState.value!!,
                  songHistory = nowPlayingViewModel.nowPlayingData.staticData.value?.songHistory,
                  playingNext = nowPlayingViewModel.nowPlayingData.staticData.value?.playingNext,
                  scrollState = scrollState,
                  showQueue = showQueue,
                  sharedTransitionScope = this@SharedTransitionLayout,
                  animatedVisibilityScope = this@AnimatedContent
                )
              }
            }
          }
        }
      }
    }
  }
}

