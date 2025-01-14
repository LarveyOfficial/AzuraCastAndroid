package com.larvey.azuracastplayer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.larvey.azuracastplayer.database.DataModelViewModel

@Composable
fun RadioList(viewModel: DataModelViewModel) {
  val radioList = viewModel.getAllEntries().collectAsState(initial = emptyList())

  Column {
    Button(onClick = {
      viewModel.addData("Test", "Test")
    }) {
      Text("Add Items")
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(all = 16.dp)
    ) {
      itemsIndexed(radioList.value) { index, item ->
        Row {
          Text(item.nickname)
          Spacer(Modifier.weight(1f))
          Text(item.url)
        }
      }
    }
  }

}