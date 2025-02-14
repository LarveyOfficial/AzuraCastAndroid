package com.larvey.azuracastplayer.ui.nowplaying.components

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.palette.graphics.Palette
import com.larvey.azuracastplayer.classes.data.Mount
import com.larvey.azuracastplayer.state.PlayerState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingBottomBar(
  showQueue: MutableState<Boolean>,
  sheetState: SheetState,
  stop: () -> Unit,
  pause: () -> Unit,
  play: () -> Unit,
  playerState: PlayerState,
  currentMount: Mount?,
  palette: Palette?,
  lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {

  var currentProgress by remember { mutableFloatStateOf(0f) }
  var currentPosition by remember { mutableLongStateOf(0) }

  val progressAnimation by animateFloatAsState(
    targetValue = currentProgress,
    animationSpec = tween(
      durationMillis = 1000,
      easing = FastOutLinearInEasing
    )
  )

  LaunchedEffect(lifecycleOwner) {
    updateTime(
      isVisible = playerState.currentMediaItem != null,
      updateProgress = { progress, position ->
        currentProgress = progress
        currentPosition = position
      },
      playerState = playerState
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
    // Media Controls + Share
    MediaControls(
      sheetState = sheetState,
      stop = stop,
      pause = pause,
      play = play,
      playerState = playerState
    )
    Spacer(Modifier.weight(0.1f))
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
    ) {
      IconButton(
        enabled = false,
        onClick = {}
      ) {
        Icon(
          imageVector = Icons.Rounded.StarBorder,
          contentDescription = "Favorite",
          modifier = Modifier.size(48.dp),
          tint = Color.White
        )
      }
      Spacer(modifier = Modifier.weight(1f))
      IconButton(
        enabled = true,
        onClick = {
          showQueue.value = !showQueue.value
        }
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
          contentDescription = "Queue",
          modifier = Modifier.size(48.dp),
          tint = Color.White
        )
      }
    }
  }
}

@androidx.annotation.OptIn(UnstableApi::class)
suspend fun updateTime(
  isVisible: Boolean, updateProgress: (Float, Long) -> Unit, playerState: PlayerState?
) {
  while (isVisible) {
    if (playerState?.isPlaying == true) {
      updateProgress(
        playerState.player.currentPosition.toFloat() / playerState.player.mediaMetadata.durationMs!!.toFloat(),
        playerState.player.currentPosition
      )
    }
    delay(1000)
  }
}