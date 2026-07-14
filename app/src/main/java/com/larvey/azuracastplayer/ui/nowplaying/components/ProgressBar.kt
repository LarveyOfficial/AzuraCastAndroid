package com.larvey.azuracastplayer.ui.nowplaying.components

import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.palette.graphics.Palette
import com.larvey.azuracastplayer.classes.data.Mount
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.utils.albumColors
import java.util.Locale
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import com.larvey.azuracastplayer.utils.formatPlaybackTimestamp


@OptIn(UnstableApi::class)
@ExperimentalMaterial3ExpressiveApi
@Composable
fun ProgressBar(
  progressAnimation: Float,
  playerState: PlayerState,
  currentPosition: Long,
  currentMount: Mount?,
  palette: Palette?
) {
  Column {
    val colors = albumColors(palette)

    val isPlaying = animateFloatAsState(
      targetValue = if (playerState.isPlaying) 1f else 0f,
    )

    LinearWavyProgressIndicator(
      progress = { progressAnimation },
      trackColor = colors.track,
      amplitude = { isPlaying.value },
      stopSize = 0.dp,
      waveSpeed = 15.dp,
      color = colors.bright,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .clip(RoundedCornerShape(16.dp)),
    )

    Spacer(modifier = Modifier.size(8.dp))

    // Progress Timestamps
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
    ) {
      val duration = playerState.mediaMetadata.durationMs?.toDuration(DurationUnit.MILLISECONDS)
        ?: 0.toDuration(DurationUnit.MILLISECONDS)

      val position = currentPosition.toDuration(DurationUnit.MILLISECONDS)

      val durationString = formatPlaybackTimestamp(
        value = duration,
        forceHours = position.inWholeHours > 0
      )
      val positionString = formatPlaybackTimestamp(
        value = position,
        forceHours = duration.inWholeHours > 0
      )
      Text(
        positionString,
        maxLines = 1,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White
      )
      Spacer(modifier = Modifier.weight(1f))
      if ((currentMount?.bitrate != null && currentMount.format != null) || playerState.currentMediaItem?.mediaId?.endsWith("m3u8") == true) {
        val qualityLabel = if (playerState.currentMediaItem?.mediaId?.endsWith("m3u8") == true) {
          "HLS"
        } else {
          "${currentMount?.format?.uppercase()} ${currentMount?.bitrate}kbps"
        }
        Text(
          text = qualityLabel,
          style = MaterialTheme.typography.labelSmall,
          maxLines = 1,
          color = colors.onLightChip,
          modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(colors.lightChip)
            .padding(
              horizontal = 12.dp,
              vertical = 4.dp
            )
        )
      }
      Spacer(modifier = Modifier.weight(1f))
      Text(
        durationString,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        color = Color.White
      )
    }
  }
}