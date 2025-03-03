package com.larvey.azuracastplayer.ui.mainActivity.radios

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
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
import com.larvey.azuracastplayer.utils.conditional
import sh.calvin.reorderable.ReorderableCollectionItemScope

@OptIn(
  ExperimentalGlideComposeApi::class,
  ExperimentalFoundationApi::class,
  ExperimentalComposeUiApi::class
)
@Composable
fun StationListEntry(
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

  var isDragging by remember { mutableStateOf(false) }

  val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)

  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    tonalElevation = elevation,
    shadowElevation = elevation,
    shape = RoundedCornerShape(12.dp),
    modifier = with(scope) {
      Modifier
        .conditional(editingList.value) {
          longPressDraggableHandle(
            onDragStarted = {
              Log.d(
                "DEBUG",
                "Dragging"
              )
              isDragging = true
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
              isDragging = false
              ViewCompat.performHapticFeedback(
                view,
                HapticFeedbackConstantsCompat.GESTURE_END
              )
            }
          )
        }
        .conditional(!editingList.value) {
          pointerInteropFilter {
            offset = Offset(
              it.x,
              it.y
            )
            false
          }
            .clip(RoundedCornerShape(12.dp))
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
        }

    },

    ) { // Card
    Row(
      modifier = Modifier
        .padding(8.dp)
        .fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) { // Row
      GlideImage(
        model = stationData?.nowPlaying?.song?.art.toString().replace(
          "http://",
          "https://"
        ),
        contentDescription = "${stationData?.station?.name}",
        modifier = Modifier
          .size(64.dp)
          .aspectRatio(1f)
          .clip(RoundedCornerShape(8.dp)),
        failure = placeholder(
          ColorPainter(Color.DarkGray)
        ),
        loading = placeholder(
          ColorPainter(Color.DarkGray)
        ),
        contentScale = ContentScale.FillBounds
      )
      Spacer(modifier = Modifier.width(8.dp))
      Column(
        modifier = Modifier

          .weight(1f),
        verticalArrangement = Arrangement.Center
      ) { // Column
        Row(
          verticalAlignment = Alignment.CenterVertically,
        ) { // Row
          Text(
            text = station.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier
              .weight(1f)
              .padding(end = 2.dp)
              .basicMarquee(iterations = Int.MAX_VALUE)
          )
          AnimatedVisibility(
            visible = stationData?.listeners?.current != null && !editingList.value,
            enter = slideInVertically(
              initialOffsetY = { fullHeight -> -fullHeight * 2 },
              animationSpec = tween(delayMillis = 500)
            ),
            exit = slideOutVertically(
              targetOffsetY = { fullHeight -> -fullHeight * 2 }
            )
          ) {
            ElevatedAssistChip(
              onClick = {
                setPlaybackSource(
                  station.url,
                  station.defaultMount,
                  station.shortcode
                )
              },
              leadingIcon = {
                Row {
                  Spacer(Modifier.size(4.dp))
                  Icon(
                    imageVector = Icons.Rounded.Headphones,
                    contentDescription = "Current Listeners",
                    modifier = Modifier.size(16.dp)
                  )
                }

              },
              label = {
                Text(
                  "${if (stationData!!.listeners.current > 999) "999+" else stationData.listeners.current}",
                  maxLines = 1
                )
              },
              modifier = Modifier
                .widthIn(max = 80.dp)
                .height(28.dp)
                .offset(y = (-4).dp)
                .conditional(editingList.value) {
                  offset(x = (48).dp)
                }
                .padding(end = 2.dp)
            )
          }
        }
        if (stationData?.nowPlaying?.song?.title.toString() != "null") {
          Row {
            Text(
              text = "Now playing: ",
              maxLines = 1,
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
      AnimatedVisibility(
        visible = editingList.value,
        enter = slideInHorizontally(
          initialOffsetX = { fullWidth -> fullWidth * 2 },
          animationSpec = tween(delayMillis = 250)
        ),
        exit = slideOutHorizontally(
          targetOffsetX = { fullWidth -> fullWidth * 2 },
          animationSpec = tween(delayMillis = 250)
        )
      ) {
        IconButton(
          onClick = {}
        ) {
          Icon(
            imageVector = Icons.Rounded.DragHandle,
            contentDescription = "Re-Order"
          )
        }
      }
    }
  }
  if (!editingList.value) {
    Box {
      DropdownMenu(
        expanded = showDropdown,
        onDismissRequest = { showDropdown = false },
        offset = DpOffset(
          (0.3 * offset.x).dp,
          (0.369 * offset.y).dp
        ),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
