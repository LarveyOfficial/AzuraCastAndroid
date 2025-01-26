package com.larvey.azuracastplayer.ui.nowPlaying

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.larvey.azuracastplayer.state.PlayerState
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalGlideComposeApi::class
)
@Composable
fun NowPlaying(
  hideNowPlaying: () -> Unit,
  playerState: PlayerState?,
  pause: () -> Unit,
  play: () -> Unit,
  lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {

  if (playerState?.currentMediaItem == null) hideNowPlaying()


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
      isVisible = playerState?.currentMediaItem != null,
      updateProgress = { progress, position ->
        currentProgress = progress
        currentPosition = position
      },
      playerState = playerState
    )
  }

  val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = true
  )

  ModalBottomSheet(
    modifier = Modifier.fillMaxSize(),
    sheetState = sheetState,
    onDismissRequest = {
      hideNowPlaying()
    },
    windowInsets = WindowInsets(
      0,
      0,
      0,
      0
    ),
    dragHandle = {}
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.size(96.dp))
      AnimatedContent(playerState?.mediaMetadata?.artworkUri.toString()) {
        GlideImage(
          model = it,
          contentDescription = "${playerState?.mediaMetadata?.albumTitle}",
          modifier = Modifier
            .size(384.dp)
            .clip(RoundedCornerShape(16.dp)),
          transition = CrossFade
        )
      }
      Spacer(modifier = Modifier.size(32.dp))
      Text(
        text = playerState?.mediaMetadata?.displayTitle.toString(),
        modifier = Modifier.width(384.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.size(4.dp))
      Text(
        text = playerState?.mediaMetadata?.artist.toString(),
        modifier = Modifier.width(384.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleMedium
      )
      Spacer(modifier = Modifier.size(32.dp))
      Row(modifier = Modifier.width(384.dp)) {
        val duration =
          playerState?.mediaMetadata?.durationMs!!.toDuration(DurationUnit.MILLISECONDS)

        val position = currentPosition.toDuration(DurationUnit.MILLISECONDS)

        val durationString =
          duration.toComponents { minutes, seconds, _ ->
            String.format(
              Locale.getDefault(),
              "%02d:%02d",
              minutes,
              seconds
            )
          }
        val positionString =
          position.toComponents { minutes, seconds, _ ->
            String.format(
              Locale.getDefault(),
              "%02d:%02d",
              minutes,
              seconds
            )
          }

        Text(positionString)
        Spacer(modifier = Modifier.weight(1f))
        Text(durationString)
      }
      LinearProgressIndicator(
        progress = { progressAnimation },
        modifier = Modifier
          .width(384.dp)
          .clip(RoundedCornerShape(16.dp))
      )
      Spacer(modifier = Modifier.size(32.dp))
      AnimatedContent(targetState = playerState?.isPlaying) { targetState ->
        if (targetState == true) {
          IconButton(onClick = {
            pause()
          }) {
            Icon(
              imageVector = Icons.Rounded.Pause,
              contentDescription = "Pause",
              modifier = Modifier.size(64.dp)
            )
          }
        } else {
          IconButton(onClick = {
            play()
          }) {
            Icon(
              imageVector = Icons.Rounded.PlayArrow,
              contentDescription = "Play",
              modifier = Modifier.size(64.dp)
            )
          }
        }
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

