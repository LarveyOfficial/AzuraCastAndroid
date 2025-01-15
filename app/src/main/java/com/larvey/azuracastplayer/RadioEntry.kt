package com.larvey.azuracastplayer

import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.larvey.azuracastplayer.database.DataModel
import java.net.URL

@Composable
fun RadioEntry(radioListViewModel: RadioListViewModel, entry: DataModel, navController: NavHostController) {
  val url = "https://" + URL(entry.url).host
  LaunchedEffect(Unit) {
    radioListViewModel.getData(url)
  }
  Card(onClick = {
    navController.navigate("nowPlaying/${URL(entry.url).host.toString()}") {

    popUpTo(navController.graph.findStartDestination().id) {
      saveState = true
    }
    // Restore state when reelecting a previously selected item
    restoreState = true
    }
  }) {
    Row {
      Text(entry.nickname)
      Spacer(Modifier.weight(1f))
      Text(entry.url)
      Spacer(Modifier.weight(1f))
      Text(text = radioListViewModel.stationData[url]?.nowPlaying?.song?.title.toString())
    }
  }

}