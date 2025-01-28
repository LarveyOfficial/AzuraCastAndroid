package com.larvey.azuracastplayer.ui.nowPlaying

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.larvey.azuracastplayer.classes.data.Mount
import com.larvey.azuracastplayer.state.PlayerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
  stop: () -> Unit,
  currentMount: Mount?,
  lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {

  rememberCoroutineScope()

  if (playerState?.currentMediaItem == null) hideNowPlaying()

  if (playerState?.currentMediaItem != null) {
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


    val sheetState = rememberModalBottomSheetState(
      skipPartiallyExpanded = true
    )



    ModalBottomSheet(
      modifier = Modifier.fillMaxSize(),
      sheetState = sheetState,
      shape = RoundedCornerShape(getRoundedCornerRadius()),
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
      }
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.statusBars),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
      ) {

        Spacer(Modifier.weight(0.75f))


        // Album Art
        AnimatedContent(playerState.mediaMetadata.artworkUri.toString()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp)
              .clip(RoundedCornerShape(16.dp)),
          ) {
            GlideImage(
              model = it,
              contentDescription = "${playerState.mediaMetadata.albumTitle}",
              transition = CrossFade,
              contentScale = ContentScale.FillWidth
            )
          }
        }

        Spacer(Modifier.weight(0.25f))

        SongAndArtist(playerState)

        Spacer(Modifier.weight(0.25f))

        ProgressBar(
          progressAnimation = progressAnimation,
          playerState = playerState,
          currentPosition = currentPosition,
          currentMount = currentMount
        )

        Spacer(Modifier.weight(0.15f))

        // Media Controls + Share
        MediaControls(
          sheetState = sheetState,
          stop = stop,
          pause = pause,
          play = play,
          playerState = playerState
        )

        Spacer(Modifier.weight(0.25f))


        // Favorite + Share Buttons
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
        ) {
          IconButton(
            enabled = false,
            onClick = {
            }) {
            Icon(
              imageVector = Icons.Rounded.StarBorder,
              contentDescription = "Favorite",
              modifier = Modifier.size(48.dp)
            )
          }
          Spacer(modifier = Modifier.weight(1f))
          IconButton(
            enabled = false,
            onClick = {

            }) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
              contentDescription = "Queue",
              modifier = Modifier.size(48.dp)
            )
          }
        }
        Spacer(modifier = Modifier.size(32.dp))
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

@Composable
fun getRoundedCornerRadius(): Dp {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val view = LocalView.current
    val windowInsets = view.rootWindowInsets
    val roundedCorner = windowInsets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
    val radiusPx = roundedCorner?.radius ?: 0
    val density = LocalDensity.current
    return with(density) { radiusPx.toDp() }
  }
  return 0.dp
}

@Composable
private fun SongAndArtist(playerState: PlayerState) {
  Column {
    Text(
      text = playerState.mediaMetadata.displayTitle.toString(),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      textAlign = TextAlign.Left,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.size(4.dp))

    //Artist Name
    Text(
      text = playerState.mediaMetadata.artist.toString(),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      textAlign = TextAlign.Left,
      style = MaterialTheme.typography.titleMedium
    )
  }
}


@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun ProgressBar(
  progressAnimation: Float, playerState: PlayerState, currentPosition: Long,
  currentMount: Mount?
) {
  Column {
    LinearProgressIndicator(
      progress = { progressAnimation },
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .clip(RoundedCornerShape(16.dp))
    )

    Spacer(modifier = Modifier.size(8.dp))

    // Progress Timestamps
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
    ) {
      val duration =
        playerState.mediaMetadata.durationMs!!.toDuration(DurationUnit.MILLISECONDS)

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
      Text(
        positionString,
        style = MaterialTheme.typography.labelMedium
      )
      Spacer(modifier = Modifier.weight(1f))
      SuggestionChip(
        onClick = {},
        label = {
          Text(
            "${currentMount?.format?.uppercase()} ${currentMount?.bitrate}kbps",
            style = MaterialTheme.typography.labelSmall
          )
        },
        modifier = Modifier
          .heightIn(max = 24.dp)
      )
      Spacer(modifier = Modifier.weight(1f))
      Text(
        durationString,
        style = MaterialTheme.typography.labelMedium
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaControls(
  sheetState: SheetState, stop: () -> Unit, pause: () -> Unit, play: () -> Unit,
  playerState: PlayerState
) {
  val scope = rememberCoroutineScope()
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 64.dp)
  ) {

    // Stop Button
    IconButton(onClick = {
      scope.launch {
        sheetState.hide()
        stop()
      }
    }) {
      Icon(
        imageVector = Icons.Rounded.Stop,
        contentDescription = "Stop",
        modifier = Modifier.size(48.dp)
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
            }
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
            }
        )
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    //Share Button
    IconButton(
      enabled = false,
      onClick = {
      }) {
      Icon(
        imageVector = Icons.Rounded.Share,
        contentDescription = "Share",
        modifier = Modifier.size(32.dp)
      )
    }
  }
}