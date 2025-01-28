package com.larvey.azuracastplayer.ui.mainActivity.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.larvey.azuracastplayer.state.PlayerState

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MiniPlayer(
  playerState: PlayerState?,
  showNowPlaying: () -> Unit,
  pause: () -> Unit,
  play: () -> Unit
) {
  Surface(
    onClick = {
      showNowPlaying()
    },
    modifier = Modifier
      .fillMaxSize()
      .clip(RoundedCornerShape(8.dp)),
    color = MaterialTheme.colorScheme.surfaceContainer
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(start = 8.dp)
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
          text = playerState?.mediaMetadata?.displayTitle.toString(),
          maxLines = 1,
          modifier = Modifier
            .basicMarquee(iterations = Int.MAX_VALUE),
          fontWeight = FontWeight.Bold
        )
        Text(
          text = playerState?.mediaMetadata?.artist.toString(),
          maxLines = 1,
          modifier = Modifier
            .basicMarquee(iterations = Int.MAX_VALUE),
          style = MaterialTheme.typography.labelLarge
        )
      }
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
    }
  }
}