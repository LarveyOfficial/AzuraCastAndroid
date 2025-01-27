package com.larvey.azuracastplayer.ui.mainActivity.radios

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
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.larvey.azuracastplayer.classes.data.SavedStation
import com.larvey.azuracastplayer.classes.data.StationJSON

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRadios(
  savedRadioList: List<SavedStation>?,
  innerPadding: PaddingValues,
  setPlaybackSource: (String, String, String) -> Unit,
  staticDataMap: SnapshotStateMap<Pair<String, String>, StationJSON>?,
  deleteRadio: (SavedStation) -> Unit,
  editRadio: (SavedStation) -> Unit
) {

  Column(modifier = Modifier.padding(innerPadding)) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(all = 16.dp)
    ) {
      if (savedRadioList?.isNotEmpty() == true) {
        itemsIndexed(savedRadioList) { _, item ->
          StationListEntry(
            station = item,
            setPlaybackSource = setPlaybackSource,
            staticDataMap = staticDataMap,
            deleteRadio = deleteRadio,
            editRadio = editRadio
          )
          Spacer(Modifier.height(8.dp))
        }
      }
    }
  }
}

