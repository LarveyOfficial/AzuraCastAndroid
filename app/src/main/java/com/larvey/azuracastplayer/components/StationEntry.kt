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
import com.larvey.azuracastplayer.viewmodels.NowPlayingViewModel
import com.larvey.azuracastplayer.classes.SavedStation

@Composable
fun StationEntry(nowPlayingViewModel: NowPlayingViewModel, entry: SavedStation, showBottomSheet: MutableState<Boolean>) {
  LaunchedEffect(Unit) {
    //TODO: Make it so it goes into a map again... Make this a separate call, the things they do are way to different at this point...
    nowPlayingViewModel.setMediaMetadata(entry.url, entry.shortcode)
  }
  Card(
    onClick = {
      val uri = Uri.parse(nowPlayingViewModel.staticDataMap[Pair(entry.url, entry.shortcode)]?.station?.mounts?.get(0)?.url)
      nowPlayingViewModel.setPlaybackSource(uri, entry.url, entry.shortcode)
      showBottomSheet.value = true }

    ) {
    Row(modifier = Modifier.padding(8.dp)){
      Text(entry.name)
      Spacer(Modifier.weight(1f))
      Text(entry.url)
      Spacer(Modifier.weight(1f))
      Text(entry.shortcode)
    }
  }

}