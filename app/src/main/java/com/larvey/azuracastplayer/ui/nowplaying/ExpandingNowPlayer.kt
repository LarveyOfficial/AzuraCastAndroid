package com.larvey.azuracastplayer.ui.nowplaying

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import androidx.palette.graphics.Palette
import com.larvey.azuracastplayer.classes.data.NowPlaying
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.ui.mainActivity.components.MiniPlayerContent
import com.larvey.azuracastplayer.ui.mainActivity.components.miniPlayerDismissHorizontalGesture
import com.larvey.azuracastplayer.ui.mainActivity.components.rememberMiniPlayerDismissGestureHandler
import com.larvey.azuracastplayer.utils.albumColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Resting height of the collapsed mini bar. */
private val MiniBarHeight: Dp = 76.dp

/** Side margin of the collapsed card; closes to 0 as it fills the screen. */
private val CollapsedSideMargin: Dp = 12.dp

/** Footprint of the floating nav bar (68dp bar + padding) plus a small gap, above the system inset. */
private val NavBarArea: Dp = 82.dp

/** Container corner radius / shadow at rest; both flatten to nothing as it fills the screen. */
private val CollapsedCorner: Dp = 24.dp
private val CollapsedShadow: Dp = 2.dp

/**
 * The collapsed mini bar's BOTTOM corners are tighter than its top so it nests against the floating
 * nav bar below (whose fused top corner is the same 14dp). They round back to [CollapsedCorner] as
 * the bar is swiped away (detaching from the nav bar), and flatten to 0 as the card fills the screen.
 */
private val CollapsedBottomCorner: Dp = 14.dp

/** Max dim of the app behind the surface, at full expansion. */
private const val ScrimMaxAlpha = 0.4f

/**
 * The mini player and the full Now Playing screen are ONE card. A single bottom-anchored surface —
 * with one solid album-colored background, one shape and one shadow — **grows upward** out of its
 * resting slot (side margins closing, corners flattening, base sliding down over the nav bar) into
 * the full screen. On that single card the layers cross-fade: the mesh gradient fades in over the
 * solid color (so the background morphs solid -> mesh), the full player fades in, and the bare mini
 * row fades out — so the mini bar itself is what expands, not a second sheet appearing behind it.
 */
@Composable
fun ExpandingNowPlayer(
  state: ExpandingPlayerState,
  playerState: PlayerState?,
  palette: MutableState<Palette?>?,
  colorList: MutableState<List<Color>>?,
  nowPlaying: () -> NowPlaying?,
  play: () -> Unit,
  pause: () -> Unit,
  stop: () -> Unit,
  onDismissProgress: (Float) -> Unit,
  modifier: Modifier = Modifier
) {
  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val density = LocalDensity.current
    val fullHeightPx = with(density) { maxHeight.toPx() }
    val miniBarPx = with(density) { MiniBarHeight.toPx() }
    val sideMarginPx = with(density) { CollapsedSideMargin.toPx() }
    val shadowPx = with(density) { CollapsedShadow.toPx() }
    val navAreaPx = WindowInsets.navigationBars.getBottom(density) +
      with(density) { NavBarArea.toPx() }
    val collapsedY = (fullHeightPx - miniBarPx - navAreaPx).coerceAtLeast(0f)

    val thresholdPx = with(density) { 5.dp.toPx() }
    remember(collapsedY, thresholdPx) {
      state.dragThresholdPx = thresholdPx
      state.overDragPx = miniBarPx * 0.2f
      state.setCollapsedBounds(collapsedY)
      collapsedY
    }

    // The card's single background = the mini player's album card color (animated across songs).
    val albumCardColors = albumColors(palette?.value)
    val cardColor by animateColorAsState(
      targetValue = albumCardColors.container,
      animationSpec = tween(durationMillis = 450),
      label = "expandingCardColor"
    )

    // Swipe the collapsed mini bar sideways to stop playback (slides the whole card off, same as the
    // standalone mini player). Only wired while the mini row is composed (collapsed).
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val dismissOffset = remember { Animatable(0f) }
    val dismissHandler = rememberMiniPlayerDismissGestureHandler(
      scope = scope,
      density = density,
      hapticFeedback = haptics,
      offsetAnimatable = dismissOffset,
      screenWidthPx = screenWidthPx,
      onDismiss = stop
    )
    val dismissProgress = (abs(dismissOffset.value) / (screenWidthPx * 0.4f)).coerceIn(0f, 1f)
    SideEffect { onDismissProgress(dismissProgress) }

    // Stop from the full player collapses back to the mini bar, then slides it away, then actually
    // stops — instead of the surface vanishing mid-screen.
    val animatedStop: () -> Unit = {
      scope.launch {
        state.animateCollapse()
        dismissOffset.animateTo(
          targetValue = -screenWidthPx,
          animationSpec = tween(durationMillis = 220)
        )
        stop()
      }
      Unit
    }

    val isOpen by remember { derivedStateOf { state.expansion() > 0.001f } }
    // Mini row is only present (and interactive) while collapsed-ish, so it never blocks the
    // expanded controls; the card drag lives on the card, so un-composing it never cancels a drag.
    val miniPresent by remember { derivedStateOf { state.expansion() < 0.5f } }

    var onNowPlayingRoute by remember { mutableStateOf(true) }

    PredictiveBackHandler(enabled = isOpen && onNowPlayingRoute) { progress: Flow<BackEventCompat> ->
      try {
        progress.collect { event -> state.onPredictiveBack(event.progress) }
        // The back gesture is drag-like, so finish with the momentum spring (bounce), not a glide.
        state.collapseWithMomentum()
      } catch (_: CancellationException) {
        state.expand()
      }
    }

    // Dimming scrim behind the surface (dim only — collapsing is via drag-down / back / close).
    Box(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer { alpha = state.expansion() * ScrimMaxAlpha }
        .background(Color.Black)
    )

    // THE CARD.
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .offset { IntOffset(0, -lerp(navAreaPx, 0f, state.expansion()).roundToInt()) }
        // OUTER: the card's visible width (side margins close) and height (mini -> full) grow.
        .layout { measurable, constraints ->
          val f = state.expansion()
          val hInset = lerp(sideMarginPx, 0f, f)
          val cardW = (constraints.maxWidth - 2f * hInset).roundToInt()
            .coerceIn(0, constraints.maxWidth)
          val cardH = lerp(miniBarPx, fullHeightPx, f).roundToInt()
            .coerceIn(0, constraints.maxHeight)
          val placeable = measurable.measure(Constraints.fixed(cardW, cardH))
          layout(constraints.maxWidth, cardH) { placeable.place(hInset.roundToInt(), 0) }
        }
        // One shape + shadow for the whole card; both flatten as it opens. translationX slides the
        // whole card when the collapsed mini bar is swiped away to dismiss.
        .graphicsLayer {
          val f = state.expansion()
          // Bottom corners nest against the floating nav bar at rest (14dp), round back to the top
          // radius as the mini bar is swiped away, then flatten with the top corners as it opens.
          val dismiss = (abs(dismissOffset.value) / (screenWidthPx * 0.4f)).coerceIn(0f, 1f)
          val topC = lerp(CollapsedCorner, 0.dp, f)
          val bottomC = lerp(lerp(CollapsedBottomCorner, CollapsedCorner, dismiss), 0.dp, f)
          shape = RoundedCornerShape(
            topStart = topC,
            topEnd = topC,
            bottomStart = bottomC,
            bottomEnd = bottomC
          )
          clip = true
          shadowElevation = lerp(shadowPx, 0f, f)
          translationX = dismissOffset.value
          // Momentum overshoot: render the part of the settle/drag position that goes PAST the
          // [0, collapsedY] end-stops as a real vertical offset, so a fast collapse physically dips
          // past the mini-bar rest and springs back while geometry stays saturated at the end.
          val ty = state.translationY.value
          translationY = ty - ty.coerceIn(0f, state.collapsedY)
          // Small cosmetic squash accent on collapse, anchored to the bottom edge.
          scaleY = state.squashScaleY.value
          transformOrigin = TransformOrigin(0.5f, 1f)
        }
        // INNER: content laid out at full height, bottom-aligned, revealed by the clip (not stretched).
        .layout { measurable, constraints ->
          val fullH = fullHeightPx.roundToInt()
          val placeable = measurable.measure(constraints.copy(minHeight = fullH, maxHeight = fullH))
          layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(0, constraints.maxHeight - fullH)
          }
        }
        .then(if (onNowPlayingRoute) Modifier.playerVerticalDrag(state) else Modifier)
    ) {
      // Layer 1 — the single solid card background (the mini player's color). Always opaque.
      Box(modifier = Modifier.fillMaxSize().background(cardColor))

      // Layer 2 — the mesh gradient fades in over the solid, so the background morphs solid -> mesh.
      Box(
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer { alpha = (state.expansion() / 0.3f).coerceIn(0f, 1f) }
      ) {
        playerState?.let {
          NowPlayingBackground(playerState = it, colorList = colorList)
        }
      }

      // Layer 3 — the full player, fading in over the second half.
      Box(
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer { alpha = ((state.expansion() - 0.25f) / 0.75f).coerceIn(0f, 1f) }
      ) {
        NowPlayingContent(
          palette = palette,
          colorList = colorList,
          onCollapse = { state.collapse() },
          onNowPlayingRouteChange = { onNowPlayingRoute = it },
          onStop = animatedStop,
          drawBackground = false
        )
      }

      // Layer 4 — the bare mini row at the bottom of the card (no background of its own), fading out.
      if (miniPresent) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(MiniBarHeight)
            .graphicsLayer { alpha = (1f - state.expansion() * 2f).coerceIn(0f, 1f) }
            .miniPlayerDismissHorizontalGesture(
              enabled = true,
              handler = dismissHandler
            )
        ) {
          MiniPlayerContent(
            playerState = playerState,
            showNowPlaying = { state.expand() },
            play = play,
            pause = pause,
            containerColor = cardColor,
            onContainerColor = albumCardColors.onContainer,
            accent = albumCardColors.accent,
            onAccent = albumCardColors.onAccent
          )
        }
      }
    }
  }
}

/**
 * Card vertical drag: the finger moves [ExpandingPlayerState.translationY] 1:1 and settles to
 * mini/full on release. A vertical swipe wins over the child mini-row clickable; a tap still clicks.
 */
private fun Modifier.playerVerticalDrag(state: ExpandingPlayerState): Modifier =
  this.pointerInput(state) {
    val tracker = VelocityTracker()
    detectVerticalDragGestures(
      onDragStart = {
        tracker.resetTracking()
        state.onDragStart()
      },
      onVerticalDrag = { change, dragAmount ->
        change.consume()
        tracker.addPosition(change.uptimeMillis, change.position)
        state.onDrag(dragAmount)
      },
      onDragEnd = { state.onDragEnd(tracker.calculateVelocity().y) },
      onDragCancel = { state.onDragEnd(0f) }
    )
  }
