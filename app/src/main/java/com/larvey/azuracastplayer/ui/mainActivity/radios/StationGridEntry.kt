package com.larvey.azuracastplayer.ui.mainActivity.radios

import android.util.Log
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.larvey.azuracastplayer.classes.data.SavedStation
import com.larvey.azuracastplayer.classes.data.StationJSON
import com.larvey.azuracastplayer.ui.mainActivity.components.ConfirmStationDelete
import com.larvey.azuracastplayer.ui.mainActivity.components.EditStation
import sh.calvin.reorderable.ReorderableCollectionItemScope

@OptIn(
  ExperimentalMaterial3ExpressiveApi::class,
  ExperimentalGlideComposeApi::class,
  ExperimentalComposeUiApi::class,
  ExperimentalFoundationApi::class
)
@Composable
fun StationGridEntry(
  scope: ReorderableCollectionItemScope,
  station: SavedStation,
  setPlaybackSource: (String, String, String) -> Unit,
  staticDataMap: SnapshotStateMap<Pair<String, String>, StationJSON>?,
  deleteRadio: (SavedStation) -> Unit,
  editRadio: (SavedStation) -> Unit,
  editingList: MutableState<Boolean>
) {
  val view = LocalView.current
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
  if (editingList.value) {

    val infiniteShake = rememberInfiniteTransition(label = "infiniteShake")

    var shakeItemRotation by remember { mutableFloatStateOf(0f) }

    shakeItemRotation = infiniteShake.animateFloat(
      initialValue = 0.5f,
      targetValue = -0.5f,
      animationSpec = infiniteRepeatable(
        animation = tween(
          90
        ),
        repeatMode = RepeatMode.Reverse
      ),
      label = "rotation"
    ).value

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .padding(top = 8.dp)
        .fillMaxWidth()
        .rotate(shakeItemRotation)
    ) {
      GlideImage(
        model = stationData?.nowPlaying?.song?.art.toString(),
        contentDescription = "${stationData?.station?.name}",
        failure = placeholder(
          ColorPainter(Color.DarkGray)
        ),
        loading = placeholder(
          ColorPainter(Color.DarkGray)
        ),
        modifier = with(scope) {
          Modifier
            .size(174.dp)
            .clip(RoundedCornerShape(8.dp))
            .longPressDraggableHandle(
              onDragStarted = {
                Log.d(
                  "DEBUG",
                  "Dragging"
                )
                ViewCompat.performHapticFeedback(
                  view,
                  HapticFeedbackConstantsCompat.GESTURE_START
                )
              },
              onDragStopped = {
                Log.d(
                  "DEBUG",
                  "Stopped dragging"
                )
                ViewCompat.performHapticFeedback(
                  view,
                  HapticFeedbackConstantsCompat.GESTURE_END
                )
              }
            )
        }
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
    }
  } else {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .padding(top = 8.dp)
        .fillMaxWidth()
    ) {
      GlideImage(
        model = stationData?.nowPlaying?.song?.art.toString(),
        contentDescription = "${stationData?.station?.name}",
        failure = placeholder(
          ColorPainter(Color.DarkGray)
        ),
        loading = placeholder(
          ColorPainter(Color.DarkGray)
        ),
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
          DropdownMenuItem(
            text = { Text("Change Order") },
            onClick = {
              editingList.value = true
              showDropdown = false
            },
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.FormatListNumbered,
                contentDescription = "Change Order"
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