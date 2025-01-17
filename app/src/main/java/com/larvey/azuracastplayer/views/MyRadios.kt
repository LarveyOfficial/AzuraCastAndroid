package com.larvey.azuracastplayer.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.larvey.azuracastplayer.classes.SavedStation
import com.larvey.azuracastplayer.components.StationEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRadios(
  savedRadioList: List<SavedStation>,
  innerPadding: PaddingValues,
  setPlaybackSource: (url: String, shortCode: String) -> Unit,
  getStationData: (url: String, shortCode: String) -> Unit,
) {

    Column (modifier = Modifier.padding(innerPadding))  {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(all = 16.dp)
      ) {
        itemsIndexed(savedRadioList) { _, item ->
          StationEntry(
            station =item,
            setPlaybackSource = setPlaybackSource,
            getStationData = getStationData
          )
        }
      }
    }
//  NowPlaying(
//    showBottomSheet,
//    nowPlayingViewModel,
//    mediaController!!
//  )
//  when {
//    showDialog.value -> AddStationDialog(
//      showDialog = showDialog,
//      addData = savedStationsViewModel::addData,
//      radioListViewModel = radioListViewModel
//    )
//  }
}

