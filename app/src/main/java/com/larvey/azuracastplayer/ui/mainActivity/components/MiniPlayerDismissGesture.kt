package com.larvey.azuracastplayer.ui.mainActivity.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * Amplitude-aware haptics for the swipe gesture. On API 26+ with amplitude control we drive the
 * [Vibrator] directly so the ticks can literally get stronger as you approach the unlock point;
 * otherwise we fall back to Compose's discrete [HapticFeedback].
 */
internal class SwipeHaptics(
  private val vibrator: Vibrator?,
  private val fallback: HapticFeedback
) {
  private val hasAmplitude = vibrator?.hasAmplitudeControl() == true

  /** The satisfying "break free" when the drag crosses the unlock point. */
  fun unlock() = buzz(durationMs = 26, amplitude = 235, fallback = HapticFeedbackType.LongPress)

  /** The relock "clunk" — slightly weaker than [unlock]. */
  fun relock() = buzz(durationMs = 24, amplitude = 200, fallback = HapticFeedbackType.LongPress)

  private fun buzz(
    durationMs: Long,
    amplitude: Int,
    fallback: HapticFeedbackType
  ) {
    val v = vibrator
    if (v != null && v.hasVibrator()) {
      try {
        val amp = if (hasAmplitude) amplitude.coerceIn(1, 255) else VibrationEffect.DEFAULT_AMPLITUDE
        v.vibrate(VibrationEffect.createOneShot(durationMs, amp))
        return
      } catch (_: Exception) {
        // fall through to the Compose haptic
      }
    }
    this.fallback.performHapticFeedback(fallback)
  }
}

private fun Context.dismissVibrator(): Vibrator? = try {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
  } else {
    @Suppress("DEPRECATION")
    getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
  }
} catch (_: Exception) {
  null
}

/**
 * Horizontal swipe-to-dismiss for the mini player, with a two-state detent:
 *
 * - **Locked** (the resting state): the card resists, moving only a fraction of the finger
 *   travel, and emits a texture haptic on movement that intensifies as it nears the unlock point.
 * - Crossing the **unlock** distance ([unlockPx]) fires a strong haptic and "breaks free" — the
 *   card snaps to the finger and now dismisses on release.
 * - While unlocked, dragging back past the **relock** distance ([relockPx], < unlockPx, so there
 *   is hysteresis) re-locks with a firmer haptic and the resistance returns.
 *
 * Releasing while unlocked flies the card off and calls [onDismiss] (stops playback — the same
 * action as the Now Playing Stop button); releasing while locked springs it back.
 */
internal class MiniPlayerDismissGestureHandler(
  private val scope: CoroutineScope,
  density: Density,
  private val haptics: SwipeHaptics,
  private val offsetAnimatable: Animatable<Float, AnimationVector1D>,
  private val screenWidthPx: Float,
  private val onDismiss: () -> Unit
) {
  private val unlockPx = 120f * density.density
  private val relockPx = 60f * density.density
  private val maxTensionPx = 36f * density.density

  private var locked = true
  private var accumulatedDragX = 0f
  private var offsetJob: Job? = null

  fun onDragStart() {
    locked = true
    accumulatedDragX = 0f
    offsetJob?.cancel()
    offsetJob = scope.launch(start = CoroutineStart.UNDISPATCHED) { offsetAnimatable.stop() }
  }

  fun onHorizontalDrag(dragAmount: Float) {
    accumulatedDragX += dragAmount
    val absDrag = abs(accumulatedDragX)
    val direction = accumulatedDragX.sign

    if (locked) {
      val progress = (absDrag / unlockPx).coerceIn(0f, 1f)

      if (absDrag >= unlockPx) {
        locked = false
        haptics.unlock()
        animateOffsetTo(
          accumulatedDragX,
          spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
        )
      } else {
        // Resist: the card moves only a fraction of the finger travel.
        snapOffsetTo(maxTensionPx * progress * direction)
      }
    } else {
      if (absDrag <= relockPx) {
        locked = true
        haptics.relock()
        animateOffsetTo(
          maxTensionPx * (absDrag / unlockPx).coerceIn(0f, 1f) * direction,
          spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
      } else {
        // Free drag: follow the finger.
        animateOffsetTo(
          accumulatedDragX,
          spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
        )
      }
    }
  }

  fun onDragEnd() {
    offsetJob?.cancel()
    if (!locked) {
      // Unlocked → commit the dismiss.
      val target = if (accumulatedDragX < 0) -screenWidthPx else screenWidthPx
      offsetJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
        offsetAnimatable.animateTo(
          targetValue = target,
          animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
        )
        onDismiss()
      }
    } else {
      // Still locked → spring back.
      offsetJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
        offsetAnimatable.animateTo(
          targetValue = 0f,
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
          )
        )
      }
    }
  }

  fun onDragCancel() {
    locked = true
    accumulatedDragX = 0f
    animateOffsetTo(
      0f,
      spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    )
  }

  private fun snapOffsetTo(value: Float) {
    offsetJob?.cancel()
    offsetJob = scope.launch(start = CoroutineStart.UNDISPATCHED) { offsetAnimatable.snapTo(value) }
  }

  private fun animateOffsetTo(
    value: Float,
    spec: androidx.compose.animation.core.AnimationSpec<Float>
  ) {
    offsetJob?.cancel()
    offsetJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
      offsetAnimatable.animateTo(value, spec)
    }
  }
}

@Composable
internal fun rememberMiniPlayerDismissGestureHandler(
  scope: CoroutineScope,
  density: Density,
  hapticFeedback: HapticFeedback,
  offsetAnimatable: Animatable<Float, AnimationVector1D>,
  screenWidthPx: Float,
  onDismiss: () -> Unit
): MiniPlayerDismissGestureHandler {
  val context = LocalContext.current
  val vibrator = remember(context) { context.dismissVibrator() }
  val haptics = remember(vibrator, hapticFeedback) { SwipeHaptics(vibrator, hapticFeedback) }
  val onDismissState = rememberUpdatedState(onDismiss)
  return remember(scope, density, haptics, offsetAnimatable, screenWidthPx) {
    MiniPlayerDismissGestureHandler(
      scope = scope,
      density = density,
      haptics = haptics,
      offsetAnimatable = offsetAnimatable,
      screenWidthPx = screenWidthPx,
      onDismiss = { onDismissState.value() }
    )
  }
}

internal fun Modifier.miniPlayerDismissHorizontalGesture(
  enabled: Boolean,
  handler: MiniPlayerDismissGestureHandler
): Modifier {
  if (!enabled) return this
  return this.pointerInput(handler) {
    detectHorizontalDragGestures(
      onDragStart = { handler.onDragStart() },
      onHorizontalDrag = { change, dragAmount ->
        change.consume()
        handler.onHorizontalDrag(dragAmount)
      },
      onDragEnd = { handler.onDragEnd() },
      onDragCancel = { handler.onDragCancel() }
    )
  }
}
