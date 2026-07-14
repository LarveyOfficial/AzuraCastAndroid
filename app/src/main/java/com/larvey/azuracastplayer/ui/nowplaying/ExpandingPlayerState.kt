package com.larvey.azuracastplayer.ui.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Drives the continuous mini <-> full-screen player transition.
 *
 * [translationY] is the single source of truth for how open the player is: `0f` = fully expanded,
 * [collapsedY] = collapsed. A drag moves it 1:1 with the finger; releasing settles it to one end.
 * [expansion] (0..1) is *derived* from it so the two can never disagree. The player content itself
 * is drawn in a bottom-anchored window that grows upward, so the mini bar stays put while the sheet
 * expands over it.
 */
class ExpandingPlayerState(
  private val scope: CoroutineScope
) {
  val translationY = Animatable(0f)

  var collapsedY: Float = 0f
    private set

  /** True while the user is actively dragging — used to show placeholders instead of live content. */
  var isDragging by mutableStateOf(false)
    private set

  /** Set once by the layout so drag thresholds are in real px. */
  var dragThresholdPx: Float = 24f

  private var initialized = false
  private var accumulated = 0f
  private var expandedState = false

  /** 0f = mini bar, 1f = full screen. Derived from [translationY] and [collapsedY]. */
  fun expansion(): Float {
    val c = collapsedY
    return if (c <= 0f) 0f else ((c - translationY.value) / c).coerceIn(0f, 1f)
  }

  /** Collapsed offset = screen height - mini bar - nav-bar area. */
  fun setCollapsedBounds(px: Float) {
    val y = px.coerceAtLeast(1f)
    val fractionBefore = expansion()
    collapsedY = y
    if (!initialized) {
      initialized = true
      // UNDISPATCHED so the value is set during this composition, before layout reads it — the
      // surface starts collapsed instead of flashing open on the first frame.
      scope.launch(start = CoroutineStart.UNDISPATCHED) { translationY.snapTo(y) }
      return
    }
    if (!isDragging && !translationY.isRunning) {
      scope.launch(start = CoroutineStart.UNDISPATCHED) { translationY.snapTo(y * (1f - fractionBefore)) }
    }
  }

  fun onDragStart() {
    isDragging = true
    accumulated = 0f
    scope.launch(start = CoroutineStart.UNDISPATCHED) { translationY.stop() }
  }

  /** [dy] = raw vertical drag delta in px (down = positive). Clamped to [0, collapsedY] (no overshoot). */
  fun onDrag(dy: Float) {
    accumulated += dy
    val newY = (translationY.value + dy).coerceIn(0f, collapsedY)
    scope.launch(start = CoroutineStart.UNDISPATCHED) { translationY.snapTo(newY) }
  }

  /** [velocityY] px/s (down = positive). Settles toward wherever the gesture is heading. */
  fun onDragEnd(velocityY: Float) {
    isDragging = false
    val target = when {
      expandedState && accumulated <= 0f -> true
      abs(accumulated) > dragThresholdPx -> accumulated < 0f
      abs(velocityY) > VELOCITY_THRESHOLD -> velocityY < 0f
      else -> expansion() > 0.5f
    }
    settle(target, velocityY)
  }

  /** Predictive-back preview: [progress] 0..1 slides the sheet from expanded toward collapsed. */
  fun onPredictiveBack(progress: Float) {
    val target = lerp(0f, collapsedY, progress.coerceIn(0f, 1f))
    scope.launch(start = CoroutineStart.UNDISPATCHED) { translationY.snapTo(target) }
  }

  fun expand() = settle(expand = true, velocityY = 0f)

  fun collapse() = settle(expand = false, velocityY = 0f)

  /** Collapse to the mini bar and suspend until it finishes (used to sequence the animated stop). */
  suspend fun animateCollapse() {
    expandedState = false
    isDragging = false
    translationY.animateTo(
      targetValue = collapsedY,
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
      )
    )
  }

  /** Instantly reset to collapsed — called when playback ends so the next song opens as the mini bar. */
  fun snapToCollapsed() {
    expandedState = false
    scope.launch(start = CoroutineStart.UNDISPATCHED) { translationY.snapTo(collapsedY) }
  }

  private fun settle(expand: Boolean, velocityY: Float) {
    expandedState = expand
    isDragging = false
    val target = if (expand) 0f else collapsedY
    scope.launch {
      translationY.animateTo(
        targetValue = target,
        initialVelocity = velocityY,
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioNoBouncy,
          stiffness = Spring.StiffnessMediumLow
        )
      )
    }
  }

  companion object {
    private const val VELOCITY_THRESHOLD = 150f
  }
}

@Composable
fun rememberExpandingPlayerState(): ExpandingPlayerState {
  val scope = rememberCoroutineScope()
  return remember { ExpandingPlayerState(scope) }
}
