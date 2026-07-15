package com.larvey.azuracastplayer.ui.nowplaying.components


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.larvey.azuracastplayer.session.sleepTimer.AndroidAlarmScheduler
import com.larvey.azuracastplayer.session.sleepTimer.SleepItem
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.ui.theme.AppMotion
import com.larvey.azuracastplayer.ui.theme.expressiveShape
import com.larvey.azuracastplayer.utils.albumColors
import kotlinx.coroutines.delay
import java.time.LocalDateTime

private enum class ControlSlot { NONE, STOP, PLAY_PAUSE, SLEEP }

@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun MediaControls(
  modifier: Modifier = Modifier,
  stop: () -> Unit,
  pause: () -> Unit,
  play: () -> Unit,
  playerState: PlayerState,
  isSleeping: MutableState<Boolean>?,
  palette: Palette?
) {
  val context = LocalContext.current
  val scheduler = AndroidAlarmScheduler(context)
  val sleepTimer: MutableState<SleepItem> =
    remember { mutableStateOf(SleepItem(LocalDateTime.now())) }
  val haptics = LocalHapticFeedback.current

  var showTimePicker by remember { mutableStateOf(false) }
  val timePickerState = rememberTimePickerState(is24Hour = true)

  val isPlaying = playerState.isPlaying
  val buffering = playerState.playbackState == 2

  // Album-tinted light play button (icon carries the album hue); translucent glass side buttons.
  val colors = albumColors(palette)
  val playColor = colors.lightChip
  val playIconColor = colors.onLightChip
  val sideColor = Color.White.copy(alpha = 0.15f)

  // Press-to-expand: the tapped button grows and its neighbours compress (play/pause never
  // compresses — it stays the dominant control).
  var lastClicked by remember { mutableStateOf(ControlSlot.NONE) }
  LaunchedEffect(lastClicked) {
    if (lastClicked != ControlSlot.NONE) {
      delay(220)
      lastClicked = ControlSlot.NONE
    }
  }
  fun weightFor(slot: ControlSlot): Float = when (slot) {
    ControlSlot.PLAY_PAUSE -> if (lastClicked == ControlSlot.PLAY_PAUSE) 1.78f else 1.5f
    else -> when (lastClicked) {
      slot -> 1.24f
      ControlSlot.NONE -> 1f
      else -> 0.78f
    }
  }

  val stopWeight by animateFloatAsState(
    weightFor(ControlSlot.STOP),
    AppMotion.spatialFast(),
    label = "stopWeight"
  )
  val playWeight by animateFloatAsState(
    weightFor(ControlSlot.PLAY_PAUSE),
    AppMotion.spatialFast(),
    label = "playWeight"
  )
  val sleepWeight by animateFloatAsState(
    weightFor(ControlSlot.SLEEP),
    AppMotion.spatialFast(),
    label = "sleepWeight"
  )

  // paused → full pill (half of the 80dp height), playing → squircle
  val playCorner by animateDpAsState(
    targetValue = if (isPlaying) 24.dp else 40.dp,
    animationSpec = AppMotion.spatial(),
    label = "playCorner"
  )

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
      .padding(bottom = 32.dp)
  ) {
    // Stop
    ControlPill(
      weight = stopWeight,
      shape = expressiveShape(28.dp),
      color = sideColor,
      onClick = {
        lastClicked = ControlSlot.STOP
        stop()
      }
    ) {
      Icon(
        imageVector = Icons.Rounded.Stop,
        contentDescription = "Stop",
        modifier = Modifier.size(30.dp),
        tint = Color.White
      )
    }

    // Play / Pause (morphing squircle)
    Box(
      modifier = Modifier
        .weight(playWeight)
        .height(80.dp)
        .clip(RoundedCornerShape(playCorner))
        .background(playColor)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = ripple()
        ) {
          lastClicked = ControlSlot.PLAY_PAUSE
          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          if (isPlaying) pause() else play()
        },
      contentAlignment = Alignment.Center
    ) {
      AnimatedContent(targetState = buffering, label = "playBuffering") { isBuffering ->
        if (isBuffering) {
          LoadingIndicator(
            modifier = Modifier.size(46.dp),
            color = playIconColor
          )
        } else {
          Crossfade(
            targetState = isPlaying,
            animationSpec = AppMotion.effectsFast(),
            label = "playPauseIcon"
          ) { playing ->
            Icon(
              imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
              contentDescription = if (playing) "Pause" else "Play",
              modifier = Modifier.size(40.dp),
              tint = playIconColor
            )
          }
        }
      }
    }

    // Sleep timer
    ControlPill(
      weight = sleepWeight,
      shape = expressiveShape(28.dp),
      color = sideColor,
      onClick = {
        lastClicked = ControlSlot.SLEEP
        showTimePicker = true
      }
    ) {
      Icon(
        imageVector = if (isSleeping?.value != true) Icons.Rounded.NightsStay else Icons.Rounded.Timer,
        contentDescription = "Sleep timer",
        modifier = Modifier.size(28.dp),
        tint = if (isSleeping?.value != true) Color.White else MaterialTheme.colorScheme.primary
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
                    .plusHours(timePickerState.hour.toLong())
                    .plusMinutes(timePickerState.minute.toLong())
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
          TimePicker(timePickerState)
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

/** A filled, weighted side control (Stop / Sleep) with a ripple. */
@Composable
private fun RowScope.ControlPill(
  weight: Float,
  shape: androidx.compose.ui.graphics.Shape,
  color: Color,
  onClick: () -> Unit,
  content: @Composable () -> Unit
) {
  val haptics = LocalHapticFeedback.current
  Box(
    modifier = Modifier
      .weight(weight)
      .height(80.dp)
      .clip(shape)
      .background(color)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple()
      ) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
      },
    contentAlignment = Alignment.Center
  ) {
    content()
  }
}
