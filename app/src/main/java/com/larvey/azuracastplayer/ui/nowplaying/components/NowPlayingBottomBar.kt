package com.larvey.azuracastplayer.ui.nowplaying.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.palette.graphics.Palette
import com.larvey.azuracastplayer.classes.data.Mount
import com.larvey.azuracastplayer.classes.data.NowPlaying
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.utils.updateTime

@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun NowPlayingBottomBar(
  stop: () -> Unit,
  pause: () -> Unit,
  play: () -> Unit,
  playerState: PlayerState,
  isSleeping: MutableState<Boolean>?,
  currentMount: Mount?,
  palette: Palette?,
  nowPlaying: () -> NowPlaying?,
  lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {

  var currentProgress by remember { mutableFloatStateOf(0f) }
  var currentPosition by remember { mutableLongStateOf(0) }

  val progressAnimation by animateFloatAsState(
    targetValue = currentProgress,
    animationSpec = tween(
      durationMillis = 1000,
      easing = LinearEasing
    )
  )

  val hasMedia = playerState.currentMediaItem != null
  LaunchedEffect(lifecycleOwner, hasMedia) {
    updateTime(
      isVisible = hasMedia,
      updateProgress = { progress, position ->
        currentProgress = progress
        currentPosition = position
      },
      playerState = playerState,
      nowPlaying = nowPlaying
    )
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Bottom,
    modifier = Modifier.fillMaxHeight(0.27f)
  ) {
    ProgressBar(
      progressAnimation = progressAnimation,
      playerState = playerState,
      currentPosition = currentPosition,
      currentMount = currentMount,
      palette = palette
    )
    Spacer(Modifier.weight(0.05f))
    MediaControls(
      stop = stop,
      pause = pause,
      play = play,
      playerState = playerState,
      isSleeping = isSleeping,
      palette = palette
    )
    Spacer(Modifier.weight(0.05f))
  }
}
