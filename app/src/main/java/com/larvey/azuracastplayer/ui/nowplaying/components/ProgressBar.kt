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
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils.HSLToColor
import androidx.core.graphics.ColorUtils.colorToHSL
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
    var brightDominant = floatArrayOf(
      0f,
      0f,
      0f
    )
    colorToHSL(
      palette?.dominantSwatch?.rgb ?: Color.White.toArgb(),
      brightDominant
    )
    if (brightDominant[2] <= 0.7f) {
      brightDominant[2] = 0.7f
    }
    LinearProgressIndicator(
      progress = { progressAnimation },
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .clip(RoundedCornerShape(16.dp)),
      drawStopIndicator = {},
      trackColor = Color(
        palette?.lightVibrantSwatch?.bodyTextColor
          ?: Color.DarkGray.toArgb()
      ),
      color = Color(
        HSLToColor(brightDominant)
      )
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
        style = MaterialTheme.typography.labelMedium,
        color = Color.White
      )
      Spacer(modifier = Modifier.weight(1f))
      SuggestionChip(
        onClick = {},
        label = {
          if (playerState.currentMediaItem?.mediaId?.endsWith("m3u8") == true) {
            Text(
              "HLS",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White
            )
          } else {
            Text(
              "${currentMount?.format?.uppercase()} ${currentMount?.bitrate}kbps",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White
            )
          }

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
        style = MaterialTheme.typography.labelMedium,
        color = Color.White
      )
    }
  }
}