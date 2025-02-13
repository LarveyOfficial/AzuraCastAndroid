package com.larvey.azuracastplayer.ui.nowplaying

import android.util.Log
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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.larvey.azuracastplayer.classes.data.Mount
import com.larvey.azuracastplayer.classes.data.PlayingNext
import com.larvey.azuracastplayer.classes.data.SongHistory
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.ui.mainActivity.components.meshGradient
import com.larvey.azuracastplayer.ui.nowplaying.components.NowPlayingAlbumArt
import com.larvey.azuracastplayer.ui.nowplaying.components.NowPlayingBottomBar
import com.larvey.azuracastplayer.ui.nowplaying.components.NowPlayingHistory
import com.larvey.azuracastplayer.ui.nowplaying.components.SongAndArtist
import com.larvey.azuracastplayer.utils.getRoundedCornerRadius
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch


fun isBackgroundLight(colorList: List<Color>): Boolean {
  var addedLuminance = 0f
  for (color in colorList) {
    addedLuminance += color.luminance()
  }
  return (addedLuminance / colorList.size.toFloat()) > 0.5f
}

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
  playingNext: PlayingNext?
) {

  if (playerState?.currentMediaItem == null) hideNowPlaying()

  if (playerState?.currentMediaItem != null) {

    val appContext = LocalContext.current.applicationContext

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val transitionA = rememberInfiniteTransition(label = "X")
    val transitionB = rememberInfiniteTransition(label = "Y")

    val defaultColor = BottomSheetDefaults.ContainerColor

    val scope = rememberCoroutineScope()

    var colorList by remember { mutableStateOf(List(9) { defaultColor }) }

    var palette by remember { mutableStateOf<Palette?>(null) }

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

    LaunchedEffect(playerState.mediaMetadata.artworkUri) {
      this.async(Dispatchers.IO) {
        Log.d(
          "DEBUG",
          "Fetching image"
        )
        Glide.with(appContext).asBitmap().load(
          playerState.mediaMetadata.artworkUri.toString()
        ).submit().get().let { bitmap ->
          palette = Palette.from(bitmap).generate()
          val paletteColors = listOf(
            Color(
              palette?.dominantSwatch?.rgb ?: defaultColor.toArgb()
            ),
            Color(
              palette?.mutedSwatch?.rgb ?: defaultColor.toArgb()
            ),
            Color(
              palette?.vibrantSwatch?.rgb ?: defaultColor.toArgb()
            )
          )
          colorList = List(9) { paletteColors.random() }
        }
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
            resolutionX = 5,
            resolutionY = 5,
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
              palette = palette,
              isBackgroundLight = isBackgroundLight(colorList)
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
                    songName = playerState.mediaMetadata.title.toString(),
                    artistName = playerState.mediaMetadata.artist.toString(),
                    small = false,
                    isBackgroundLight = isBackgroundLight(colorList)
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
                  animatedVisibilityScope = this@AnimatedContent,
                  isBackgroundLight = isBackgroundLight(colorList)
                )
              }
            }
          }
        }
      }
    }
  }
}
