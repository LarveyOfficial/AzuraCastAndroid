package com.larvey.azuracastplayer.ui.mainActivity.addStations

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.larvey.azuracastplayer.classes.data.SavedStation
import com.larvey.azuracastplayer.classes.data.StationJSON
import java.net.URL

data class AddableStation(
  val name: String,
  val shortcode: String,
  val defaultMount: String
)

@Composable
fun AddStationDialog(
  hideDialog: () -> Unit,
  addData: (stations: List<SavedStation>) -> Unit,
) {
  val radioSearchViewModel: RadioSearchViewModel = viewModel()

  var radioURL by remember { mutableStateOf("") }
  var formatedURL by remember { mutableStateOf("") }

  var checkedStations by remember { mutableStateOf(emptyList<AddableStation>()) }

  var context = LocalContext.current

  Dialog(onDismissRequest = {
    radioSearchViewModel.stationHostData =
      mutableStateMapOf<String, List<StationJSON>>()
    hideDialog()
  }) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
      )
    ) {
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
        AnimatedVisibility(!radioSearchViewModel.stationHostData.isEmpty()) {
          Column {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AnimatedContent(
              targetState = radioSearchViewModel.stationHostData[formatedURL],
            ) { hostData ->
              if (hostData != null) {
                LazyColumn {
                  itemsIndexed(hostData) { _, item ->

                    val mounts = item.station.mounts.filterNot {
                      listOf(
                        "flac",
                        "opus",
                        "ogg"
                      ).contains(it.format)
                    }

                    ElevatedCard(
                      colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                      ),
                      modifier = Modifier
                        .fillMaxWidth()
                        .size(46.dp)
                        .padding(
                          2.dp
                        ),
                      onClick = {
                        if (mounts.isNotEmpty()) {
                          checkedStations = if (checkedStations.contains(
                              AddableStation(
                                item.station.name,
                                item.station.shortcode,
                                mounts[0].url
                              )
                            )
                          ) {
                            checkedStations.minus(
                              AddableStation(
                                item.station.name,
                                item.station.shortcode,
                                mounts[0].url
                              )
                            )
                          } else {
                            checkedStations.plus(
                              AddableStation(
                                item.station.name,
                                item.station.shortcode,
                                mounts[0].url
                              )
                            )
                          }
                        } else {
                          Toast.makeText(
                            context,
                            "No compatible mounts available for this station",
                            Toast.LENGTH_LONG
                          ).show()
                        }
                      }
                    ) {
                      Row(
                        modifier = Modifier
                          .padding(start = 8.dp)
                          .fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                      ) {
                        Checkbox(
                          checked = checkedStations.contains(
                            AddableStation(
                              item.station.name,
                              item.station.shortcode,
                              mounts[0].url
                            )
                          ),
                          onCheckedChange = null
                        )
                        Text(
                          item.station.name,
                          modifier = Modifier.padding(8.dp)
                        )
                      }

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
            radioSearchViewModel.stationHostData =
              mutableStateMapOf<String, List<StationJSON>>()
            hideDialog()
          }) {
            Text("Cancel")
          }
          AnimatedContent(checkedStations.isEmpty()) { empty ->
            if (empty) {
              TextButton(
                onClick = {
                  formatedURL =
                    if (radioURL.startsWith("http://") || radioURL.startsWith("https://")) {
                      radioURL
                    } else {
                      "https://$radioURL"
                    }
                  try {
                    formatedURL = URL(formatedURL).host.toString()
                  } catch (e: Exception) {
                    return@TextButton
                  }
                  radioSearchViewModel.searchStationHost(formatedURL)
                },
                enabled = radioURL.isNotBlank()
              ) {
                Text("Search")
              }
            } else {
              TextButton(onClick = {
                var listOfStations = mutableListOf<SavedStation>()
                for (item in checkedStations) {
                  listOfStations.add(
                    SavedStation(
                      item.name,
                      formatedURL.lowercase(),
                      item.shortcode,
                      item.defaultMount
                    )
                  )
                }
                addData(listOfStations)
                checkedStations = emptyList()
                radioSearchViewModel.stationHostData =
                  mutableStateMapOf<String, List<StationJSON>>()
                hideDialog()
              }) {
                Text("Add")
              }
            }

          }
        }

      }
    }
  }
}