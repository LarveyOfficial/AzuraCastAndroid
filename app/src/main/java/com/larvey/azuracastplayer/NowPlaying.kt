package com.larvey.azuracastplayer

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlaying(showBottomSheet: MutableState<Boolean>) {
  val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = true
  )
  if (showBottomSheet.value) {
    ModalBottomSheet(
      modifier = Modifier.fillMaxHeight(),
      sheetState = sheetState,
      onDismissRequest = {
        showBottomSheet.value = false
      }
    ) {
      Text("This is a Sheet")
    }
  }
}