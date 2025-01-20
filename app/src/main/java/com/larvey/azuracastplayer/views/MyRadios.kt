package com.larvey.azuracastplayer.views

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.larvey.azuracastplayer.classes.SavedStation
import com.larvey.azuracastplayer.classes.StationJSON
import com.larvey.azuracastplayer.components.StationListEntry
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRadios(
  savedRadioList: List<SavedStation>,
  innerPadding: PaddingValues,
  setPlaybackSource: (url: String, shortCode: String) -> Unit,
  getStationData: (url: String, shortCode: String) -> Unit,
  staticDataMap: MutableMap<Pair<String, String>, StationJSON>
) {
  LaunchedEffect(savedRadioList) {
    while (savedRadioList != emptyList<SavedStation>()) {
      Log.d("DEBUG", "Waiting 30 seconds to fetch data")
      delay(30000)
      for (item in savedRadioList) {
        Log.d("DEBUG", "Fetching Data for ${item.name}")
        getStationData(item.url, item.shortcode)
      }
    }
  }

  Column (modifier = Modifier.padding(innerPadding))  {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(all = 16.dp)
    ) {
      itemsIndexed(savedRadioList) { _, item ->
        StationListEntry(
          station = item,
          setPlaybackSource = setPlaybackSource,
          getStationData = getStationData,
          staticDataMap = staticDataMap
        )
        Spacer(Modifier.height(8.dp))
      }
    }
  }
}

