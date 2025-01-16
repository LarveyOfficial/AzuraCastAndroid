package com.larvey.azuracastplayer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.larvey.azuracastplayer.database.SavedStation
import java.net.URL

@Composable
fun StationEntry(radioListViewModel: RadioListViewModel, entry: SavedStation) {
  val url = "https://${entry.url}"
//  LaunchedEffect(Unit) {
//    radioListViewModel.searchStationHost(url)
//  }
  Card(onClick = {
  }) {
    Row {
      Text(entry.name)
      Spacer(Modifier.weight(1f))
      Text(entry.url)
      Spacer(Modifier.weight(1f))
      Text(entry.shortcode)
    }
  }

}