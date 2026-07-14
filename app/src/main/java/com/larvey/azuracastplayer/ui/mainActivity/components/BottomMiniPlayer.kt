package com.larvey.azuracastplayer.ui.mainActivity.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.placeholder
import com.larvey.azuracastplayer.R
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.ui.theme.AppMotion
import com.larvey.azuracastplayer.ui.theme.expressiveShape
import com.larvey.azuracastplayer.utils.albumColors
import com.larvey.azuracastplayer.utils.fixHttps
import com.larvey.azuracastplayer.utils.isDark

@OptIn(
  ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun MiniPlayer(
  playerState: PlayerState?,
  showNowPlaying: () -> Unit,
  pause: () -> Unit,
  play: () -> Unit,
  // Swiped-away → stop playback entirely (same action as the Now Playing Stop button).
  stop: () -> Unit,
  // Reports swipe-away progress (0 = at rest, 1 = at the dismiss threshold) so the nav bar
  // below can round its top corners back in sync — making them feel like they detach.
  onDismissProgress: (Float) -> Unit = {},
  palette: MutableState<Palette?>?
) {
  val colors = albumColors(palette?.value)
  val containerColor by animateColorAsState(
    targetValue = colors.container,
    animationSpec = tween(durationMillis = 450),
    label = "miniContainer"
  )
  val onContainerColor by animateColorAsState(
    targetValue = colors.onContainer,
    animationSpec = tween(durationMillis = 450),
    label = "miniOnContainer"
  )
  val accent = colors.accent
  val onAccent = colors.onAccent

  // Swipe-left/right-to-dismiss (stop playback).
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current
  val dismissHaptics = LocalHapticFeedback.current
  val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
  val dismissOffset = remember { Animatable(0f) }
  val dismissHandler = rememberMiniPlayerDismissGestureHandler(
    scope = scope,
    density = density,
    hapticFeedback = dismissHaptics,
    offsetAnimatable = dismissOffset,
    screenWidthPx = screenWidthPx,
    onDismiss = stop
  )
  // 0 at rest → 1 at the dismiss threshold; still reported so callers can react to the swipe.
  val dragProgress = (abs(dismissOffset.value) / (screenWidthPx * 0.4f)).coerceIn(0f, 1f)
  SideEffect { onDismissProgress(dragProgress) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(
        start = 12.dp,
        end = 12.dp,
        top = 6.dp,
        bottom = 4.dp
      )
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer { translationX = dismissOffset.value }
        .miniPlayerDismissHorizontalGesture(
          enabled = true,
          handler = dismissHandler
        ),
      // Fully rounded card — this bar is used only on wide/tablet layouts (in the pane's bottom
      // bar), where there is no navigation bar beneath it to nest against.
      shape = RoundedCornerShape(24.dp),
      color = containerColor,
      shadowElevation = 4.dp
    ) {
      MiniPlayerContent(
        playerState = playerState,
        showNowPlaying = showNowPlaying,
        play = play,
        pause = pause,
        containerColor = containerColor,
        onContainerColor = onContainerColor,
        accent = accent,
        onAccent = onAccent
      )
    }
  }
}

/**
 * The bare mini-player content row — album art, title/artist, play chip — with **no background of
 * its own** (the caller provides it). This lets the expanding player use the mini content directly
 * on top of the one growing card background, so the mini bar reads as the card itself expanding
 * rather than a separate sheet appearing behind it. [MiniPlayer] wraps this in its own [Surface];
 * the expanding surface draws its own shared background behind it.
 */
@Composable
fun MiniPlayerContent(
  playerState: PlayerState?,
  showNowPlaying: () -> Unit,
  play: () -> Unit,
  pause: () -> Unit,
  containerColor: Color,
  onContainerColor: Color,
  accent: Color,
  onAccent: Color,
  modifier: Modifier = Modifier
) {
  val dark = MaterialTheme.colorScheme.isDark()
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxSize()
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple()
      ) { showNowPlaying() }
      .padding(start = 8.dp)
  ) {
    AnimatedContent(
      targetState = playerState?.mediaMetadata?.artworkUri.toString(),
      label = "miniArtwork"
    ) {
      AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
          .data(it.fixHttps())
          .crossfade(true)
          .placeholderMemoryCacheKey(it.fixHttps())
          .placeholder(
            if (dark) {
              R.drawable.loading_image_dark
            } else {
              R.drawable.loading_image
            }
          )
          .diskCacheKey(it.fixHttps())
          .build(),
        contentDescription = "${playerState?.mediaMetadata?.albumTitle}",
        error = if (dark) {
          painterResource(R.drawable.image_loading_failed_dark)
        } else {
          painterResource(R.drawable.image_loading_failed)
        },
        modifier = Modifier
          .fillMaxHeight()
          .padding(vertical = 10.dp)
          .clip(expressiveShape(12.dp)),
      )
    }
    Spacer(modifier = Modifier.size(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      EdgeFadeMarquee(
        text = playerState?.mediaMetadata?.displayTitle?.toString() ?: " ",
        style = MaterialTheme.typography.titleSmall.copy(
          fontSize = 15.sp,
          lineHeight = 20.sp,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = (-0.2).sp,
          color = onContainerColor
        ),
        gradientEdgeColor = containerColor,
        modifier = Modifier.fillMaxWidth()
      )
      EdgeFadeMarquee(
        text = playerState?.mediaMetadata?.artist?.toString() ?: " ",
        style = MaterialTheme.typography.bodySmall.copy(
          fontSize = 13.sp,
          lineHeight = 17.sp,
          color = onContainerColor.copy(alpha = 0.7f)
        ),
        gradientEdgeColor = containerColor,
        modifier = Modifier.fillMaxWidth()
      )
    }
    Spacer(modifier = Modifier.size(8.dp))
    MiniPlayPauseChip(
      playbackState = playerState?.playbackState,
      isPlaying = playerState?.isPlaying == true,
      accent = accent,
      onAccent = onAccent,
      play = play,
      pause = pause,
      modifier = Modifier.padding(end = 10.dp)
    )
  }
}

/**
 * Filled squircle play/pause chip whose corner subtly morphs (rounder when paused, tighter
 * when playing) — a compact preview of the full player's morphing control. Shows a wavy
 * loading spinner while buffering.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MiniPlayPauseChip(
  playbackState: Int?,
  isPlaying: Boolean,
  accent: Color,
  onAccent: Color,
  play: () -> Unit,
  pause: () -> Unit,
  modifier: Modifier = Modifier
) {
  val haptics = LocalHapticFeedback.current
  Box(
    modifier = modifier.size(46.dp),
    contentAlignment = Alignment.Center
  ) {
    // playbackState == Player.STATE_BUFFERING (2) → wavy loading spinner
    AnimatedContent(
      targetState = playbackState,
      label = "miniBuffering"
    ) { state ->
      if (state == 2) {
        LoadingIndicator(
          modifier = Modifier.size(46.dp),
          color = accent
        )
      } else {
        // paused → full circle (half the 46dp box), playing → tighter squircle
        val corner by animateDpAsState(
          targetValue = if (isPlaying) 14.dp else 23.dp,
          animationSpec = AppMotion.spatial(),
          label = "miniChipCorner"
        )
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(corner))
            .background(accent)
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = ripple(bounded = false)
            ) {
              haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              if (isPlaying) pause() else play()
            },
          contentAlignment = Alignment.Center
        ) {
          Crossfade(
            targetState = isPlaying,
            animationSpec = AppMotion.effectsFast(),
            label = "miniPlayPauseIcon"
          ) { playing ->
            Icon(
              imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
              contentDescription = if (playing) "Pause" else "Play",
              tint = onAccent,
              modifier = Modifier.size(24.dp)
            )
          }
        }
      }
    }
  }
}

/**
 * Single-line text that scrolls (marquee) only when it overflows, with a soft gradient
 * fade at whichever edge is clipped. Ported from PixelPlayer's AutoScrollingText — the fade
 * is a horizontal-gradient rect drawn with [BlendMode.DstIn] over an offscreen layer.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EdgeFadeMarquee(
  text: String,
  style: TextStyle,
  gradientEdgeColor: Color,
  modifier: Modifier = Modifier,
  gradientWidth: Dp = 20.dp
) {
  SubcomposeLayout(modifier = modifier.clipToBounds()) { constraints ->
    val measured = subcompose("measure") {
      Text(text = text, style = style, maxLines = 1, softWrap = false)
    }[0].measure(constraints.copy(maxWidth = Constraints.Infinity))

    val overflowing = measured.width > constraints.maxWidth

    val content = @Composable {
      if (overflowing) {
        var scrolling by remember(text) { mutableStateOf(false) }
        val canFadeLeft by remember(text) { derivedStateOf { scrolling } }
        LaunchedEffect(text) {
          scrolling = false
          kotlinx.coroutines.delay(2000)
          scrolling = true
        }
        val leftEdge by animateColorAsState(
          targetValue = if (canFadeLeft) Color.Transparent else gradientEdgeColor,
          animationSpec = tween(durationMillis = 500),
          label = "leftEdge"
        )
        Box(
          modifier = Modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
              drawContent()
              val w = gradientWidth.toPx()
              drawRect(
                brush = Brush.horizontalGradient(
                  colors = listOf(leftEdge, gradientEdgeColor),
                  startX = 0f,
                  endX = w
                ),
                blendMode = BlendMode.DstIn
              )
              drawRect(
                brush = Brush.horizontalGradient(
                  colors = listOf(gradientEdgeColor, Color.Transparent),
                  startX = size.width - w,
                  endX = size.width
                ),
                blendMode = BlendMode.DstIn
              )
            }
        ) {
          Text(
            text = text,
            style = style,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Start,
            modifier = Modifier.basicMarquee(
              iterations = Int.MAX_VALUE,
              spacing = MarqueeSpacing(gradientWidth + 6.dp),
              velocity = 24.dp,
              initialDelayMillis = 2000
            )
          )
        }
      } else {
        Text(text = text, style = style, maxLines = 1, softWrap = false)
      }
    }

    val placeable = subcompose("content", content)[0].measure(constraints)
    val width = constraints.maxWidth.takeIf { it != Constraints.Infinity } ?: placeable.width
    layout(width, placeable.height) { placeable.place(0, 0) }
  }
}
