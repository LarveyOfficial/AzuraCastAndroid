package com.larvey.azuracastplayer.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.larvey.azuracastplayer.viewmodels.SavedStationsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.larvey.azuracastplayer.components.AddStationDialog
import com.larvey.azuracastplayer.components.NowPlaying
import com.larvey.azuracastplayer.components.StationEntry
import com.larvey.azuracastplayer.viewmodels.NowPlayingViewModel
import com.larvey.azuracastplayer.viewmodels.RadioListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRadios(savedStationsViewModel: SavedStationsViewModel, nowPlayingViewModel: NowPlayingViewModel) {
  val radioList = savedStationsViewModel.getAllEntries().collectAsState(initial = emptyList())
  var showDialog = remember { mutableStateOf(false)}
  var showBottomSheet = remember { mutableStateOf(false)}
  val radioListViewModel: RadioListViewModel = viewModel()

  LaunchedEffect(radioList.value) {
    for (item in radioList.value) {
      radioListViewModel.searchStationHost(item.url)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        colors = topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        title = { Text("Radio List") }
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          showDialog.value = true
        }
      ) {
        Icon(Icons.Rounded.Add, contentDescription = "Add")
      }
    },
    bottomBar = {
      BottomAppBar {
        Button(onClick = { showBottomSheet.value = true }) {
          Text("Show Sheet")
        }
      }
    }
    ) {
    innerPadding ->
    Column (Modifier.padding(innerPadding)) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(all = 16.dp)
      ) {
        itemsIndexed(radioList.value) { index, item ->
          StationEntry(
            nowPlayingViewModel,
            item,
            showBottomSheet
          )
        }
      }
    }
  }
  NowPlaying(
    showBottomSheet,
    nowPlayingViewModel
  )
  when {
    showDialog.value -> AddStationDialog(
      showDialog = showDialog,
      addData = savedStationsViewModel::addData,
      radioListViewModel = radioListViewModel
    )
  }
}

