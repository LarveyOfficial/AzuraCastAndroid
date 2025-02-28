package com.larvey.azuracastplayer.ui.mainActivity.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.palette.graphics.Palette
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.larvey.azuracastplayer.classes.data.NowPlaying
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.utils.correctedDominantColor
import com.larvey.azuracastplayer.utils.updateTime

@OptIn(
  ExperimentalGlideComposeApi::class,
  ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun MiniPlayer(
  playerState: PlayerState?,
  showNowPlaying: () -> Unit,
  nowPlaying: NowPlaying?,
  pause: () -> Unit,
  play: () -> Unit,
  palette: MutableState<Palette?>?,
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

  LaunchedEffect(lifecycleOwner) {
    updateTime(
      isVisible = playerState?.currentMediaItem != null,
      updateProgress = { progress, position ->
        currentProgress = progress
        currentPosition = position
      },
      playerState = playerState,
      nowPlaying = nowPlaying
    )
  }

  val dominantColor = correctedDominantColor(
    palette,
    isSystemInDarkTheme()
  ) ?: MaterialTheme.colorScheme.primary

  Surface(
    modifier = Modifier
      .fillMaxSize(),
    color = MaterialTheme.colorScheme.surfaceContainer
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable {
            showNowPlaying()
          }
          .padding(start = 16.dp)
          .weight(1f)
      ) {
        AnimatedContent(playerState?.mediaMetadata?.artworkUri.toString()) {
          GlideImage(
            model = it,
            contentDescription = "${playerState?.mediaMetadata?.albumTitle}",
            modifier = Modifier
              .fillMaxHeight()
              .padding(vertical = 6.dp)
              .clip(RoundedCornerShape(8.dp)),
            transition = CrossFade
          )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = (playerState?.mediaMetadata?.displayTitle?.toString() ?: " "),
            maxLines = 1,
            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
            fontWeight = FontWeight.Bold
          )
          Text(
            text = playerState?.mediaMetadata?.artist?.toString() ?: " ",
            maxLines = 1,
            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
            style = MaterialTheme.typography.labelLarge
          )
        }
        AnimatedContent(
          targetState = playerState?.playbackState,
          modifier = Modifier.padding(end = 15.dp)
        ) { loading ->
          if (loading != 2) {
            AnimatedContent(
              targetState = playerState?.isPlaying,
            ) { targetState ->
              if (targetState == true) {
                IconButton(onClick = {
                  pause()
                }) {
                  Icon(
                    imageVector = Icons.Rounded.Pause,
                    contentDescription = "Pause",
                    modifier = Modifier.size(48.dp)
                  )
                }
              } else {
                IconButton(onClick = {
                  play()
                }) {
                  Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    modifier = Modifier.size(48.dp)
                  )
                }
              }
            }
          } else {

            LoadingIndicator(
              modifier = Modifier.size(48.dp),
              color = MaterialTheme.colorScheme.onBackground
            )
          }
        }
      }
      LinearProgressIndicator(
        modifier = Modifier
          .fillMaxWidth(),
        progress = { progressAnimation },
        trackColor = MaterialTheme.colorScheme.surfaceContainer,
        color = dominantColor,
        drawStopIndicator = {}
      )
    }
  }
}