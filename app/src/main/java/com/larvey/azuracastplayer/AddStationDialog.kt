package com.larvey.azuracastplayer

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.larvey.azuracastplayer.database.SavedStationsViewModel
import java.net.URL
import kotlin.math.log

@Composable
fun AddStationDialog(showDialog: MutableState<Boolean>, savedStationsViewModel: SavedStationsViewModel, radioListViewModel: RadioListViewModel) {
  var radioURL by remember { mutableStateOf("")}
  var formatedURL by remember { mutableStateOf("") }
  var searching by remember {mutableStateOf(false)}
  Dialog(onDismissRequest = {showDialog.value = false}) {
    Card (
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      shape = RoundedCornerShape(16.dp)
    ){
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Add Radio Station",
          style = MaterialTheme.typography.titleLarge
        )
        OutlinedTextField(
          modifier = Modifier.padding(top = 8.dp),
          value = radioURL,
          onValueChange = {
            radioURL = it
          },
          singleLine = true,
          label = {
            Text("Radio URL")
          }
        )
        AnimatedVisibility(searching) {
          Column {
            HorizontalDivider( modifier = Modifier.padding(vertical = 8.dp))
            AnimatedContent(
              targetState = radioListViewModel.stationHostData[formatedURL],
            ) { hostData ->
              if (hostData != null) {
                LazyColumn {
                  itemsIndexed(hostData) { index, item ->
                    ElevatedCard (
                      modifier = Modifier.fillMaxWidth().padding(2.dp),
                      onClick = {
                        savedStationsViewModel.addData(item.station.name, item.station.shortcode, formatedURL)
                        showDialog.value = false
                      }
                    ) {
                      Text(item.station.name, modifier = Modifier.padding(8.dp))
                    }
                  }

                }

              }
            }
          }

        }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = {
            showDialog.value = false
          }) {
            Text("Cancel")
          }
          TextButton(onClick = {
            formatedURL = if (radioURL.startsWith("http://") || radioURL.startsWith("https://")) {
              radioURL
            } else {
              "https://$radioURL"
            }
            try {
              formatedURL = URL(formatedURL).host.toString()
            } catch (e: Exception) {
              return@TextButton
            }
            radioListViewModel.searchStationHost(formatedURL)
            searching = true
          }) {
            Text("Search")
          }
        }

      }
    }
  }
}