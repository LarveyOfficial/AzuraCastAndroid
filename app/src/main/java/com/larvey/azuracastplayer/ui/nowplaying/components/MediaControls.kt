package com.larvey.azuracastplayer.ui.nowplaying.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.larvey.azuracastplayer.state.PlayerState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaControls(
  sheetState: SheetState,
  stop: () -> Unit,
  pause: () -> Unit,
  play: () -> Unit,
  playerState: PlayerState,
  isBackgroundLight: Boolean
) {
  val scope = rememberCoroutineScope()
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 64.dp)
  ) {

    // Stop Button
    IconButton(
      onClick = {
        scope.launch {
          sheetState.hide()
          stop()
        }
      }
    ) {
      Icon(
        imageVector = Icons.Rounded.Stop,
        contentDescription = "Stop",
        modifier = Modifier.size(48.dp),
        tint = if (isBackgroundLight) Color.Black else Color.White
      )
    }

    Spacer(modifier = Modifier.weight(1f))

    // Play/Pause Button
    AnimatedContent(targetState = playerState.isPlaying) { targetState ->
      if (targetState) {
        Icon(
          imageVector = Icons.Rounded.PauseCircle,
          contentDescription = "Pause",
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable {
              pause()
            },
          tint = if (isBackgroundLight) Color.Black else Color.White
        )
      } else {
        Icon(
          imageVector = Icons.Rounded.PlayCircle,
          contentDescription = "Play",
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable {
              play()
            },
          tint = if (isBackgroundLight) Color.Black else Color.White
        )
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    //Share Button
    IconButton(
      enabled = false,
      onClick = {}) {
      Icon(
        imageVector = Icons.Rounded.NightsStay,
        contentDescription = "Share",
        modifier = Modifier.size(32.dp),
        tint = if (isBackgroundLight) Color.Black else Color.White
      )
    }
  }
}