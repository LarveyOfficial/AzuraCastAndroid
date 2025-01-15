package com.larvey.azuracastplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import com.larvey.azuracastplayer.database.DataModelViewModel

@Composable
fun AddRadioDialog(showDialog: MutableState<Boolean>, viewModel: DataModelViewModel) {
  var radioName by remember { mutableStateOf("")}
  var radioURL by remember { mutableStateOf("")}
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
          modifier = Modifier.padding(top = 12.dp),
          value = radioName,
          onValueChange = {
            radioName = it
          },
          singleLine = true,
          label = {
            Text("Radio Name")
          }
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
            showDialog.value = false
            viewModel.addData(radioName, radioURL)
          }) {
            Text("Add")
          }
        }
      }
    }
  }
}