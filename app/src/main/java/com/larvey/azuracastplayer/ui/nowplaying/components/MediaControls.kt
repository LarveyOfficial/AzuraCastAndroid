package com.larvey.azuracastplayer.ui.nowplaying.components


import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.larvey.azuracastplayer.session.sleepTimer.AndroidAlarmScheduler
import com.larvey.azuracastplayer.session.sleepTimer.SleepItem
import com.larvey.azuracastplayer.state.PlayerState
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun MediaControls(
  modifier: Modifier = Modifier,
  sheetState: SheetState?,
  stop: () -> Unit,
  pause: () -> Unit,
  play: () -> Unit,
  playerState: PlayerState,
  isSleeping: MutableState<Boolean>?
) {
  val context = LocalContext.current
  val scheduler = AndroidAlarmScheduler(context)
  val sleepTimer: MutableState<SleepItem> = remember { mutableStateOf(SleepItem(LocalDateTime.now())) }
  val scope = rememberCoroutineScope()

  var showTimePicker by remember { mutableStateOf(false) }
  val state = rememberTimePickerState(is24Hour = true)

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceEvenly,
    modifier = modifier
      .fillMaxWidth()
  ) {

    // Stop Button
    IconButton(
      onClick = {
        scope.launch {
          sheetState?.hide()
          stop()
        }
      }
    ) {
      Icon(
        imageVector = Icons.Rounded.Stop,
        contentDescription = "Stop",
        modifier = Modifier.size(48.dp),
        tint = Color.White
      )
    }
    // Play/Pause Button
    AnimatedContent(targetState = playerState.playbackState) { loading ->
      if (loading != 2) {
        AnimatedContent(targetState = playerState.isPlaying) { targetState ->
          if (targetState) {
            Icon(
              imageVector = Icons.Rounded.PauseCircle,
              contentDescription = "Pause",
              modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .clickable {
                  pause()
                },
              tint = Color.White
            )
          } else {
            Icon(
              imageVector = Icons.Rounded.PlayCircle,
              contentDescription = "Play",
              modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .clickable {
                  play()
                },
              tint = Color.White
            )
          }
        }
      } else {
        LoadingIndicator(
          modifier = Modifier.size(72.dp),
          color = Color.White
        )
      }
    }

    //Sleep Button
    IconButton(
      onClick = {

        showTimePicker = true
      }) {
      Icon(
        imageVector = if (!isSleeping?.value!!) Icons.Rounded.NightsStay else Icons.Rounded.Timer,
        contentDescription = "Share",
        modifier = Modifier.size(32.dp),
        tint = if (!isSleeping.value) Color.White else MaterialTheme.colorScheme.primary
      )
    }
  }
  when {
    showTimePicker -> {
      if (!isSleeping?.value!!) {
        TimePickerDialog(
          title = {
            Text(
              "Sleep timer duration",
              style = MaterialTheme.typography.titleLarge,
              modifier = Modifier.padding(bottom = 8.dp)
            )
          },
          onDismissRequest = { showTimePicker = false },
          confirmButton = {
            TextButton(
              onClick = {

                sleepTimer.value = SleepItem(
                  LocalDateTime.now()
                    .plusHours(state.hour.toLong())
                    .plusMinutes(state.minute.toLong())
                )
                sleepTimer.value.let(scheduler::schedule)
                isSleeping.value = true
                showTimePicker = false
              }
            ) {
              Text("Ok")
            }
          },
          dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
          modeToggleButton = {},
        ) {
          TimePicker(state = state)
        }
      } else {
        BasicAlertDialog(
          onDismissRequest = {
            // Dismiss the dialog when the user clicks outside the dialog or on the back
            // button. If you want to disable that functionality, simply use an empty
            // onDismissRequest.
            showTimePicker = false
          }
        ) {
          Surface(
            modifier = Modifier
              .wrapContentWidth()
              .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = TimePickerDialogDefaults.containerColor
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Text(
                "Cancel Sleep Timer?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                  .padding(horizontal = 16.dp)
                  .padding(top = 24.dp)
              )
              Spacer(modifier = Modifier.height(24.dp))
              Text(
                "Would you like to cancel your current sleep timer?",
                modifier = Modifier.padding(horizontal = 16.dp)
              )
              Spacer(modifier = Modifier.height(24.dp))
              Row(modifier = Modifier.align(Alignment.End)) {
                TextButton(
                  onClick = {
                    isSleeping.value = false
                    sleepTimer.value.let(scheduler::cancel)
                    showTimePicker = false
                  }
                ) {
                  Text("Yes")
                }
                TextButton(
                  onClick = { showTimePicker = false }
                ) {
                  Text("No")
                }
              }

            }
          }
        }
      }

    }
  }

}

