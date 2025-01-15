package com.larvey.azuracastplayer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.larvey.azuracastplayer.database.DataModel

@Composable
fun NowPlaying(stationURL: String?) {
  if (stationURL != null) {
    Text(text = "Now Playing: $stationURL")
  } else {
    Text(text = "No station selected")

  }
}