package com.larvey.azuracastplayer

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import com.larvey.azuracastplayer.database.DataModelViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioList(viewModel: DataModelViewModel, innerPadding: PaddingValues) {
  val radioList = viewModel.getAllEntries().collectAsState(initial = emptyList())

  var showDialog = remember { mutableStateOf(false)}
  val radioListViewModel: RadioListViewModel = viewModel()
  LaunchedEffect(radioList.value) {
    for (item in radioList.value) {
      Log.d("DEBUG", "yo")
      val url = "https://" + URL(item.url).host
      radioListViewModel.getData(url)
    }
  }

  Scaffold(
    modifier = Modifier.padding(innerPadding),
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
          val url = "https://" + URL(item.url).host
          Row {
            Text(item.nickname)
            Spacer(Modifier.weight(1f))
            Text(item.url)
            Spacer(Modifier.weight(1f))
            Text(text = radioListViewModel.stationData[url]?.nowPlaying?.song?.title.toString())
          }
        }
      }
    }
  }
  when {
    showDialog.value -> AddRadioDialog(
      showDialog = showDialog,
      viewModel = viewModel
    )
  }
}

