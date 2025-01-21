package com.larvey.azuracastplayer.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ConfirmStationDelete(
  hideDialog: () -> Unit,
  deleteStation: () -> Unit,
  stationName: String
) {
  AlertDialog(
    title = {
      Text("Delete Radio?")
    },
    text = {
      Text("Are you sure you want to delete $stationName?")
    },
    onDismissRequest = {
      hideDialog()
    },
    confirmButton = {
      TextButton(onClick = {
        deleteStation()
        hideDialog()
      }) {
        Text("Yes")
      }
    },
    dismissButton = {
      TextButton(onClick = { hideDialog() }) {
        Text("No")
      }
    }
  )
}