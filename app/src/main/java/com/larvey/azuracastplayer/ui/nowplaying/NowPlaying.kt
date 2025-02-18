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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils.HSLToColor
import androidx.core.graphics.ColorUtils.colorToHSL
import androidx.palette.graphics.Palette
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.larvey.azuracastplayer.classes.data.Mount
import com.larvey.azuracastplayer.classes.data.PlayingNext
import com.larvey.azuracastplayer.classes.data.SongHistory
import com.larvey.azuracastplayer.classes.models.NowPlayingData
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.ui.mainActivity.components.meshGradient
import com.larvey.azuracastplayer.ui.nowplaying.components.NowPlayingAlbumArt
import com.larvey.azuracastplayer.ui.nowplaying.components.NowPlayingBottomBar
import com.larvey.azuracastplayer.ui.nowplaying.components.NowPlayingHistory
import com.larvey.azuracastplayer.ui.nowplaying.components.SongAndArtist
import com.larvey.azuracastplayer.utils.getRoundedCornerRadius
import com.larvey.azuracastplayer.utils.weightedRandomColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch


@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalGlideComposeApi::class,
  ExperimentalSharedTransitionApi::class
)
@Composable
fun NowPlaying(
  hideNowPlaying: () -> Unit,
  playerState: PlayerState?,
  pause: () -> Unit,
  play: () -> Unit,
  stop: () -> Unit,
  currentMount: Mount?,
  songHistory: List<SongHistory>?,
  playingNext: PlayingNext?,
  nowPlayingData: NowPlayingData?,
  palette: MutableState<Palette?>
) {

  if (playerState?.currentMediaItem == null) hideNowPlaying()

  if (playerState?.currentMediaItem != null) {

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val transitionA = rememberInfiniteTransition(label = "X")
    val transitionB = rememberInfiniteTransition(label = "Y")

    val defaultColor = MaterialTheme.colorScheme.outline

    val scope = rememberCoroutineScope()

    var colorList by remember { mutableStateOf(List(9) { defaultColor }) }

    val showQueue = remember { mutableStateOf(false) }

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

    LaunchedEffect(palette.value) {
      this.async(Dispatchers.IO) {
        var defaultHSL = floatArrayOf(
          0f,
          0f,
          0f
        )
        colorToHSL(
          defaultColor.toArgb(),
          defaultHSL
        )
        val maxLuminance = 0.45f

        var vibrantSwatch = palette.value?.vibrantSwatch?.hsl
          ?: palette.value?.dominantSwatch?.hsl ?: defaultHSL

        var mutedSwatch = palette.value?.mutedSwatch?.hsl
          ?: palette.value?.dominantSwatch?.hsl
          ?: defaultHSL

        var lightMutedSwatch = palette.value?.lightMutedSwatch?.hsl
          ?: defaultHSL

        if (vibrantSwatch[2] > maxLuminance) {
          vibrantSwatch[2] = maxLuminance
        }

        if (mutedSwatch[2] > maxLuminance) {
          mutedSwatch[2] = maxLuminance
        }

        if (lightMutedSwatch[2] > maxLuminance) {
          lightMutedSwatch[2] = maxLuminance
        }

        var lightVibrantSwatch = palette.value?.lightVibrantSwatch?.hsl
          ?: defaultHSL

        var darkVibrantSwatch = palette.value?.darkVibrantSwatch?.hsl ?: defaultHSL

        if (darkVibrantSwatch[2] > maxLuminance) {
          darkVibrantSwatch[2] = maxLuminance
        }
        if (lightVibrantSwatch[2] > maxLuminance) {
          lightVibrantSwatch[2] = maxLuminance
        }

        val paletteColors = listOf(
          Color(
            HSLToColor(vibrantSwatch)
          ),
          Color(
            HSLToColor(mutedSwatch)
          ),
          Color(
            HSLToColor(lightMutedSwatch)
          ),
          Color(
            HSLToColor(darkVibrantSwatch)
          ),
          Color(
            HSLToColor(lightVibrantSwatch)
          )
        )

        colorList = weightedRandomColors(
          paletteColors,
          9
        )
      }.await()
    }

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
          .meshGradient(
            resolutionX = 16,
            resolutionY = 16,
            points = listOf(
              // @formatter:off
              listOf(
                Offset(0f, 0f) to colorList[0], // No move
                Offset(animateA, 0f) to colorList[1], // Only x moves
                Offset(1f, 0f) to colorList[2], // No move
              ), listOf(
                Offset(0f, animateB) to colorList[3], // Only y moves
                Offset(animateB, 1f - animateA) to colorList[4],
                Offset(1f, 1f - animateA) to colorList[5], // Only y moves
              ), listOf(
                Offset(0f, 1f) to colorList[6], // No move
                Offset(1f - animateB, 1f) to colorList[7], //Only x moves
                Offset(1f, 1f) to colorList[8], // No move
              )
              // @formatter:on
            )
          )
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
              stop = stop,
              pause = pause,
              play = play,
              playerState = playerState,
              currentMount = currentMount,
              palette = palette.value,
              nowPlayingData = nowPlayingData
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
                ) {
                  Spacer(Modifier.weight(0.75f))
                  NowPlayingAlbumArt(
                    playerState = playerState,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent,
                  )
                  Spacer(Modifier.weight(0.1f))
                  SongAndArtist(
                    songName = playerState.mediaMetadata.displayTitle.toString(),
                    artistName = playerState.mediaMetadata.artist.toString(),
                    small = false
                  )
                  Spacer(Modifier.weight(0.1f))
                }
              } else {
                NowPlayingHistory(
                  innerPadding = innerPadding,
                  playerState = playerState,
                  songHistory = songHistory,
                  playingNext = playingNext,
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