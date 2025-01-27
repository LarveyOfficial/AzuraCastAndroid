package com.larvey.azuracastplayer.ui.mainActivity.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.larvey.azuracastplayer.classes.data.SavedStation
import com.larvey.azuracastplayer.classes.data.StationJSON

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStation(
  hideDialog: () -> Unit, station: SavedStation, stationData: StationJSON?,
  editStation: (SavedStation) -> Unit
) {
  var stationFieldRename by remember { mutableStateOf(station.name) }
  var expandedDropdown by remember { mutableStateOf(false) }
  val mounts = stationData?.station?.mounts?.map { it.name }!!
  var setMount by remember { mutableStateOf(stationData.station.mounts.find { it.url == station.defaultMount }!!.url) }
  val textFieldState =
    rememberTextFieldState(mounts.find { it == stationData.station.mounts.find { it.url == station.defaultMount }!!.name }!!)

  Dialog(onDismissRequest = { hideDialog() }) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Edit Station",
          style = MaterialTheme.typography.titleLarge
        )
        OutlinedTextField(
          modifier = Modifier.padding(top = 16.dp),
          value = stationFieldRename,
          onValueChange = {
            stationFieldRename = it
          },
          singleLine = true,
          label = { Text("Station Name") },
          placeholder = { Text(stationData.station.name) }
        )
        ExposedDropdownMenuBox(
          modifier = Modifier.padding(top = 8.dp),
          expanded = expandedDropdown,
          onExpandedChange = { expandedDropdown = it }
        ) {
          TextField(
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            state = textFieldState,
            readOnly = true,
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text("Default Mount") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
          )
          ExposedDropdownMenu(
            expanded = expandedDropdown,
            onDismissRequest = { expandedDropdown = false }
          ) {
            mounts.forEach { mount ->
              DropdownMenuItem(
                text = {
                  Text(
                    mount,
                    style = MaterialTheme.typography.bodyLarge
                  )
                },
                onClick = {
                  textFieldState.setTextAndPlaceCursorAtEnd(mount)
                  setMount = stationData.station.mounts.find { it.name == mount }?.url!!
                  expandedDropdown = false
                },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
              )
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
            hideDialog()
          }) {
            Text("Cancel")
          }
          TextButton(
            onClick = {
              val newStation = SavedStation(
                name = stationFieldRename,
                url = station.url,
                shortcode = station.shortcode,
                defaultMount = setMount
              )
              editStation(
                newStation
              )
              hideDialog()
            },
            enabled = stationFieldRename.isNotEmpty()
          ) {
            Text("Save")
          }
        }
      }
    }
  }

}