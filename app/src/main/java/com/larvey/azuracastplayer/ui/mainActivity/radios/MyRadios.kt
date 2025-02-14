package com.larvey.azuracastplayer.ui.mainActivity.radios

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.larvey.azuracastplayer.classes.data.SavedStation
import com.larvey.azuracastplayer.classes.data.StationJSON

@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalGlideComposeApi::class,
  ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun MyRadios(
  savedRadioList: List<SavedStation>?,
  innerPadding: PaddingValues,
  setPlaybackSource: (String, String, String) -> Unit,
  staticDataMap: SnapshotStateMap<Pair<String, String>, StationJSON>?,
  deleteRadio: (SavedStation) -> Unit,
  editRadio: (SavedStation) -> Unit,
  radioListMode: Boolean
) {
  if (savedRadioList?.isNotEmpty() == true) {
    Column(modifier = Modifier.padding(innerPadding)) {
      AnimatedContent(radioListMode) { targetState ->
        if (!targetState) {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .padding(all = 16.dp)
          ) {
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
        } else {
          LazyVerticalGrid(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 16.dp),
            columns = GridCells.Adaptive(minSize = 164.dp)
          ) {
            itemsIndexed(savedRadioList) { _, item ->
              StationGridEntry(
                station = item,
                setPlaybackSource = setPlaybackSource,
                staticDataMap = staticDataMap,
                deleteRadio = deleteRadio,
                editRadio = editRadio
              )
            }
          }
        }
      }
    }
  }
}

