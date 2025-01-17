package com.larvey.azuracastplayer.components

import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import com.larvey.azuracastplayer.viewmodels.NowPlayingViewModel
import com.larvey.azuracastplayer.classes.SavedStation

@Composable
fun StationEntry(
  station: SavedStation,
  setPlaybackSource: (url: String, shortCode: String) -> Unit,
  getStationData: (url: String, shortCode: String) -> Unit,
) {
  LaunchedEffect(Unit) {
    getStationData(station.url, station.shortcode)
  }

  Card(
    onClick = {
      setPlaybackSource(station.url, station.shortcode)
    }

    ) {
    Row(modifier = Modifier.padding(8.dp)){
      Text(station.name)
      Spacer(Modifier.weight(1f))
      Text(station.url)
      Spacer(Modifier.weight(1f))
      Text(station.shortcode)
    }
  }

}