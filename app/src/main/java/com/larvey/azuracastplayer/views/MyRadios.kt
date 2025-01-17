package com.larvey.azuracastplayer.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.larvey.azuracastplayer.viewmodels.SavedStationsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.larvey.azuracastplayer.classes.SavedStation
import com.larvey.azuracastplayer.components.AddStationDialog
import com.larvey.azuracastplayer.components.NowPlaying
import com.larvey.azuracastplayer.components.StationEntry
import com.larvey.azuracastplayer.rememberManagedMediaController
import com.larvey.azuracastplayer.viewmodels.NowPlayingViewModel
import com.larvey.azuracastplayer.viewmodels.RadioListViewModel

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

