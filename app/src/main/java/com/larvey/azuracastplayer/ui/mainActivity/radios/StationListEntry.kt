package com.larvey.azuracastplayer.ui.mainActivity.radios

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
  ExperimentalGlideComposeApi::class,
  ExperimentalFoundationApi::class,
  ExperimentalComposeUiApi::class
)
@Composable
fun StationListEntry(
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

  Card(
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer
    ),
    modifier = Modifier
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
  ) {
    Box {
      Row(
        modifier = Modifier
          .padding(8.dp)
          .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        GlideImage(
          model = stationData?.nowPlaying?.song?.art.toString(),
          contentDescription = "${stationData?.station?.name}",
          modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.Center) {
          Text(
            text = station.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
          if (stationData?.nowPlaying?.song?.title.toString() != "null") {
            Row {
              Text(
                text = "Now playing: "
              )
              Text(
                text = "${stationData?.nowPlaying?.song?.title}",
                maxLines = 1,
                modifier = Modifier
                  .basicMarquee(iterations = Int.MAX_VALUE)
              )
            }
          }
        }
      }
      Box {
        DropdownMenu(
          expanded = showDropdown,
          onDismissRequest = { showDropdown = false },
          offset = DpOffset(
            (0.3 * offset.x).dp,
            (0.369 * offset.y).dp
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
