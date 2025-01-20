package com.larvey.azuracastplayer.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.larvey.azuracastplayer.classes.SavedStation
import com.larvey.azuracastplayer.classes.StationJSON
import kotlinx.coroutines.delay

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun StationListEntry(
  station: SavedStation,
  setPlaybackSource: (url: String, shortCode: String) -> Unit,
  getStationData: (url: String, shortCode: String) -> Unit,
  staticDataMap: MutableMap<Pair<String, String>, StationJSON>
) {

  LaunchedEffect(Unit) {
    while (true) {
      getStationData(station.url, station.shortcode)
      delay(30000)
    }
  }

  val stationData = staticDataMap[Pair(station.url, station.shortcode)]

  Card(
    onClick = {
      setPlaybackSource(station.url, station.shortcode)
    }

    ) {
    Row(modifier = Modifier
      .padding(8.dp)
      .fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ){
      GlideImage(
        model = stationData?.nowPlaying?.song?.art.toString(),
        contentDescription = "${stationData?.station?.name}",
        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
      )
      Spacer(modifier = Modifier.width(8.dp))
      Column (verticalArrangement = Arrangement.Center) {
        Text(
          text = station.name,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        if (stationData?.nowPlaying?.song?.title.toString() != "null") {
          Text(
            text = "Now playing: ${stationData?.nowPlaying?.song?.title}",
            maxLines = 1,
            modifier = Modifier
              .basicMarquee(iterations = Int.MAX_VALUE),
          )
        }
      }

    }
  }
}
