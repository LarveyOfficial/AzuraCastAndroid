package com.larvey.azuracastplayer.components

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.larvey.azuracastplayer.R
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.viewmodels.NowPlayingViewModel

@OptIn(ExperimentalMaterial3Api::class,
  ExperimentalGlideComposeApi::class
)
@Composable
fun NowPlaying(
  hideNowPlaying: () -> Unit,
  playerState: PlayerState?,
  pause: () -> Unit,
  play: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = true
  )
  ModalBottomSheet(
    modifier = Modifier.fillMaxHeight(),
    sheetState = sheetState,
    onDismissRequest = {
      hideNowPlaying()
    },
    shape = RoundedCornerShape(0)
  ) {
    Column (
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      AnimatedContent(playerState?.mediaMetadata?.artworkUri.toString()) {
        GlideImage(
          model = it,
          contentDescription = "${playerState?.mediaMetadata?.albumTitle}",
          modifier = Modifier.size(384.dp)
            .clip(RoundedCornerShape(16.dp)),
          transition = CrossFade
        )
      }
      Spacer(modifier = Modifier.size(16.dp))
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
      AnimatedContent(targetState = playerState?.isPlaying) { targetState ->
        if (targetState == true) {
          IconButton (onClick = {
            pause()
          }) {
            Icon(
              imageVector = Icons.Rounded.Pause,
              contentDescription = "Pause",
              modifier = Modifier.size(64.dp)
            ) }
        } else {
          IconButton (onClick = {
            play()
          }) {
            Icon(
              imageVector = Icons.Rounded.PlayArrow,
              contentDescription = "Play",
              modifier = Modifier.size(64.dp)
            ) }
        }
      }
    }
  }
}