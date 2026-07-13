package com.larvey.azuracastplayer.ui.nowplaying.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.larvey.azuracastplayer.ui.theme.expressiveShape
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.palette.graphics.Palette
import com.larvey.azuracastplayer.classes.data.Mount
import com.larvey.azuracastplayer.classes.data.NowPlaying
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.ui.theme.AppMotion
import com.larvey.azuracastplayer.utils.updateTime

@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun NowPlayingBottomBar(
  toggleQueueVisibility: () -> Unit,
  sheetState: SheetState? = null,
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
    // Media Controls + Share
    Column(
      verticalArrangement = Arrangement.spacedBy(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      MediaControls(
        sheetState = sheetState,
        stop = stop,
        pause = pause,
        play = play,
        playerState = playerState,
        isSleeping = isSleeping,
        palette = palette
      )
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Press-to-expand, matching the Stop / Sleep control buttons above.
        val queueInteraction = remember { MutableInteractionSource() }
        val queuePressed by queueInteraction.collectIsPressedAsState()
        val queueWeight by animateFloatAsState(
          targetValue = if (queuePressed) 1.3f else 1f,
          animationSpec = AppMotion.spatialFast(),
          label = "queuePillWeight"
        )

        // The two spacers mirror the Stop + Play/Pause slots in MediaControls above, so the
        // queue pill lines up under — and matches the width of — the Sleep button.
        // Planned feature (favorites) — intentionally kept; would replace the first spacer:
        //        Box(
        //          modifier = Modifier
        //            .weight(1f)
        //            .height(50.dp)
        //            .clip(expressiveShape(20.dp))
        //            .background(Color.White.copy(alpha = 0.15f))
        //            .clickable { },
        //          contentAlignment = Alignment.Center
        //        ) {
        //          Icon(
        //            imageVector = Icons.Rounded.FavoriteBorder,
        //            contentDescription = "Favorite",
        //            modifier = Modifier.size(22.dp),
        //            tint = Color.White
        //          )
        //        }
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.weight(1.5f))
        // Song history / queue — same width as the Sleep button, a bit over half its height.
        Box(
          modifier = Modifier
            .weight(queueWeight)
            .height(50.dp)
            .clip(expressiveShape(20.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .clickable(
              interactionSource = queueInteraction,
              indication = ripple()
            ) {
              toggleQueueVisibility()
            },
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
            contentDescription = "Song history",
            modifier = Modifier.size(22.dp),
            tint = Color.White
          )
        }
      }
    }

  }
}

