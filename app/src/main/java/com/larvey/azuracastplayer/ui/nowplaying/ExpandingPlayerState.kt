package com.larvey.azuracastplayer.ui.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
 * [expansion] (0..1) is *derived* from it so the two can never disagree — geometry, the layer
 * cross-fade, and the scrim all read it.
 *
 * When a settle spring overshoots an end-stop (or a drag pulls past it), [translationY] leaves the
 * `[0, collapsedY]` range. [expansion] stays clamped so geometry saturates at the ends; the surface
 * separately renders that out-of-range excess as a real vertical offset, so the momentum bounce is
 * physically visible (see ExpandingNowPlayer's card `graphicsLayer`).
 */
class ExpandingPlayerState(
  private val scope: CoroutineScope
) {
  val translationY = Animatable(0f)

  /**
   * Cosmetic vertical squash on collapse (1f = none), riding on top of the real momentum overshoot.
   * Velocity-independent — its depth is set only by how open the sheet was at release.
   */
  val squashScaleY = Animatable(1f)

  var collapsedY: Float = 0f
    private set

  /** Set once by the layout so drag thresholds are in real px. */
  var dragThresholdPx: Float = 24f

  /** How far (px) a drag may pull past either end-stop before hard-stopping (elastic over-drag). */
  var overDragPx: Float = 0f

  private var initialized = false
  private var isDragging = false
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

  /**
   * [dy] = raw vertical drag delta in px (down = positive). Tracks the finger 1:1, allowing a small
   * elastic over-drag ([overDragPx]) past either end-stop before it hard-stops.
   */
  fun onDrag(dy: Float) {
    accumulated += dy
    // Hard stop at the fully-expanded end (0): once open, swiping up must NOT lift the player off the
    // top and reveal content beneath it. The elastic over-drag stays only past the collapsed end.
    val newY = (translationY.value + dy).coerceIn(0f, collapsedY + overDragPx)
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
    settle(target, velocityY, fromDrag = true)
  }

  /** Predictive-back preview: [progress] 0..1 slides the sheet from expanded toward collapsed. */
  fun onPredictiveBack(progress: Float) {
    val target = lerp(0f, collapsedY, progress.coerceIn(0f, 1f))
    scope.launch(start = CoroutineStart.UNDISPATCHED) { translationY.snapTo(target) }
  }

  fun expand() = settle(expand = true, velocityY = 0f, fromDrag = false)

  fun collapse() = settle(expand = false, velocityY = 0f, fromDrag = false)

  /**
   * Collapse with the momentum spring (overshoot + bounce), as a drag release would. The back
   * gesture routes here — it's a drag-like action, so it should physically bounce, not glide like the
   * close button. [velocityY] px/s can be forwarded if the gesture carries one (0 = pure spring
   * overshoot from the release point).
   */
  fun collapseWithMomentum(velocityY: Float = 0f) =
    settle(expand = false, velocityY = velocityY, fromDrag = true)

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
    scope.launch(start = CoroutineStart.UNDISPATCHED) {
      translationY.snapTo(collapsedY)
      squashScaleY.snapTo(1f)
    }
  }

  /**
   * Settles the sheet to one end.
   *
   * Opening always glides on a fixed tween — the fling velocity is intentionally not carried, so a
   * flick up simply glides open. A **drag-released collapse** instead pours the actual fling velocity
   * into an underdamped, low-stiffness spring, so the sheet overshoots the mini-bar rest and springs
   * back: the bounce IS the gesture's momentum, not a scripted effect. Its damping softens (bounces
   * more) the more open the sheet was at release. A programmatic collapse (back / close button) has
   * no momentum, so it glides like opening.
   */
  private fun settle(expand: Boolean, velocityY: Float, fromDrag: Boolean) {
    expandedState = expand
    isDragging = false
    val fractionBefore = expansion()
    val target = if (expand) 0f else collapsedY
    scope.launch {
      if (!expand && fromDrag) {
        translationY.animateTo(
          targetValue = target,
          initialVelocity = velocityY,
          animationSpec = spring(
            dampingRatio = lerp(
              Spring.DampingRatioNoBouncy,
              COLLAPSE_DAMPING_OPEN,
              fractionBefore
            ),
            stiffness = Spring.StiffnessLow
          )
        )
      } else {
        translationY.animateTo(
          targetValue = target,
          animationSpec = tween(durationMillis = 255, easing = FastOutSlowInEasing)
        )
      }
    }
    // Cosmetic squash accent layered on the collapse (always from rest, velocity-independent).
    scope.launch {
      if (expand) {
        squashScaleY.snapTo(1f)
      } else {
        squashScaleY.snapTo(lerp(1f, COLLAPSE_SQUASH, fractionBefore))
        squashScaleY.animateTo(
          targetValue = 1f,
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessVeryLow
          )
        )
      }
    }
  }

  companion object {
    private const val VELOCITY_THRESHOLD = 150f

    /** Deepest squash at the start of a collapse-from-fully-open (1f = none). */
    private const val COLLAPSE_SQUASH = 0.97f

    /**
     * Spring damping when collapsing from fully open (lerps up to critically-damped 1.0 as the
     * release point nears the mini bar). Higher = LESS overshoot travel, WITHOUT speeding the motion
     * up — stiffness sets speed, damping sets throw. PixelPlayer uses 0.75; nudged up for less throw.
     */
    private const val COLLAPSE_DAMPING_OPEN = 0.80f
  }
}

@Composable
fun rememberExpandingPlayerState(): ExpandingPlayerState {
  val scope = rememberCoroutineScope()
  return remember { ExpandingPlayerState(scope) }
}
