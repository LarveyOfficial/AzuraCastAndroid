package com.larvey.azuracastplayer.ui.nowplaying.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SongAndArtist(
  modifier: Modifier = Modifier,
  songName: String,
  artistName: String,
  small: Boolean,
  color: Color = Color.White
) {
  Column(modifier = modifier.padding(horizontal = if (small) 4.dp else 16.dp)) {
    Text(
      text = if (songName == "null") " " else songName,
      modifier = Modifier
        .fillMaxWidth()
        .basicMarquee(iterations = Int.MAX_VALUE)
        .animateContentSize(),
      textAlign = TextAlign.Left,
      style = lerp(
        MaterialTheme.typography.titleMedium,
        MaterialTheme.typography.titleLarge,
        animateFloatAsState(
          if (small) 0f else 1f,
          label = "Song Name"
        ).value
      ),
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      color = color
    )

    Spacer(modifier = Modifier.size(4.dp))

    //Artist Name
    Text(
      text = if (artistName == "null") " " else artistName,
      modifier = Modifier
        .fillMaxWidth()
        .basicMarquee(iterations = Int.MAX_VALUE)
        .animateContentSize(),
      textAlign = TextAlign.Left,
      maxLines = 1,
      style = lerp(
        MaterialTheme.typography.titleSmall,
        MaterialTheme.typography.titleMedium,
        animateFloatAsState(
          if (small) 0f else 1f,
          label = "Artist Name"
        ).value
      ),
      color = color
    )
  }
}