package com.larvey.azuracastplayer.ui.mainActivity.radios

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.larvey.azuracastplayer.classes.data.SavedStation
import com.larvey.azuracastplayer.classes.data.StationJSON
import com.larvey.azuracastplayer.ui.mainActivity.components.ConfirmStationDelete
import com.larvey.azuracastplayer.ui.mainActivity.components.EditStation

@OptIn(
  ExperimentalMaterial3ExpressiveApi::class,
  ExperimentalGlideComposeApi::class,
  ExperimentalComposeUiApi::class,
  ExperimentalFoundationApi::class
)
@Composable
fun StationGridEntry(
  station: SavedStation,
  setPlaybackSource: (String, String, String) -> Unit,
  staticDataMap: SnapshotStateMap<Pair<String, String>, StationJSON>?,
  deleteRadio: (SavedStation) -> Unit,
  editRadio: (SavedStation) -> Unit
) {

  val haptics = LocalHapticFeedback.current
  var offset by remember { mutableStateOf(Offset.Zero) }

  var showDropdown by remember { mutableStateOf(false) }

  var showDelete by remember { mutableStateOf(false) }

  var showEdit by remember { mutableStateOf(false) }

  val stationData = staticDataMap?.get(
    Pair(
      station.url,
      station.shortcode
    )
  )
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier.padding(top = 8.dp)
  ) {
    GlideImage(
      model = stationData?.nowPlaying?.song?.art.toString(),
      contentDescription = "${stationData?.station?.name}",
      modifier = Modifier
        .size(174.dp)
        .clip(RoundedCornerShape(8.dp))
        .pointerInteropFilter {
          offset = Offset(
            it.x,
            it.y
          )
          false
        }
        .combinedClickable(
          onClick = {
            setPlaybackSource(
              station.url,
              station.defaultMount,
              station.shortcode
            )

          },
          onLongClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            showDropdown = true
          }
        )
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = station.name,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold
    )
    Row(
      modifier = Modifier.widthIn(max = 164.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Playing: ",
        style = MaterialTheme.typography.bodySmallEmphasized
      )
      Text(
        text = "${stationData?.nowPlaying?.song?.title}",
        maxLines = 1,
        style = MaterialTheme.typography.bodySmallEmphasized,
        modifier = Modifier
          .basicMarquee(iterations = Int.MAX_VALUE)
      )
    }
    Spacer(Modifier.height(16.dp))
    Box {
      DropdownMenu(
        expanded = showDropdown,
        onDismissRequest = { showDropdown = false },
        offset = DpOffset(
          (-100 + offset.x / 3.5).dp,
          (-200 + (offset.y / 3.5)).dp
        )
      ) {
        DropdownMenuItem(
          text = { Text("Delete") },
          onClick = {
            showDelete = true
            showDropdown = false
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Rounded.Delete,
              contentDescription = "Delete Radio"
            )
          }
        )
        DropdownMenuItem(
          text = { Text("Edit") },
          onClick = {
            showEdit = true
            showDropdown = false
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Rounded.Edit,
              contentDescription = "Edit Radio"
            )
          }
        )
      }
    }
  }

  when {
    showDelete -> {
      ConfirmStationDelete(
        hideDialog = { showDelete = false },
        deleteStation = {
          deleteRadio(station)
        },
        stationName = station.name
      )
    }

    showEdit -> {
      EditStation(
        hideDialog = { showEdit = false },
        station = station,
        stationData = stationData,
        editStation = editRadio
      )
    }
  }

}