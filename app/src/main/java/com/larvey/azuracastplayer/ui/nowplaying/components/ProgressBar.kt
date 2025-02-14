package com.larvey.azuracastplayer.ui.nowplaying.components

import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.palette.graphics.Palette
import com.larvey.azuracastplayer.classes.data.Mount
import com.larvey.azuracastplayer.state.PlayerState
import java.util.Locale
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@OptIn(UnstableApi::class)
@Composable
fun ProgressBar(
  progressAnimation: Float,
  playerState: PlayerState,
  currentPosition: Long,
  currentMount: Mount?,
  palette: Palette?
) {
  Column {
    LinearProgressIndicator(
      progress = { progressAnimation },
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .clip(RoundedCornerShape(16.dp)),
      drawStopIndicator = {},
      trackColor = Color(
        palette?.lightVibrantSwatch?.bodyTextColor
          ?: ProgressIndicatorDefaults.linearTrackColor.toArgb()
      ),
      color = Color(
        palette?.lightVibrantSwatch?.rgb
          ?: ProgressIndicatorDefaults.linearColor.toArgb()
      )
    )

    Spacer(modifier = Modifier.size(8.dp))

    // Progress Timestamps
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
    ) {
      val duration = playerState.mediaMetadata.durationMs!!.toDuration(DurationUnit.MILLISECONDS)

      val position = currentPosition.toDuration(DurationUnit.MILLISECONDS)

      val durationString = duration.toComponents { minutes, seconds, _ ->
        String.format(
          Locale.getDefault(),
          "%02d:%02d",
          minutes,
          seconds
        )
      }
      val positionString = position.toComponents { minutes, seconds, _ ->
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
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
          )
        },
        border = BorderStroke(
          width = 1.dp,
          color = Color.White
        ),
        modifier = Modifier.heightIn(max = 24.dp)
      )
      Spacer(modifier = Modifier.weight(1f))
      Text(
        durationString,
        style = MaterialTheme.typography.labelMedium
      )
    }
  }
}