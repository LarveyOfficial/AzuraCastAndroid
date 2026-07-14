package com.larvey.azuracastplayer.ui.nowplaying.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Total height of [NowPlayingTopBar] (44dp button + 6dp vertical padding each side). */
val NowPlayingTopBarHeight: Dp = 56.dp

/**
 * The Now Playing top bar: an optional collapse button on the left and a song-history button on
 * the right. Styled as neutral translucent "glass" so it reads on the dark backdrop without
 * competing with the album-tinted controls. The history button is shaped like the right half of a
 * split button — squared-off inner (left) edge, fully rounded outer (right) edge.
 *
 * [onClose] is nullable so the side-pane layout (which has nothing to collapse) can omit it.
 */
@Composable
fun NowPlayingTopBar(
  onToggleHistory: () -> Unit,
  modifier: Modifier = Modifier,
  onClose: (() -> Unit)? = null
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        horizontal = 12.dp,
        vertical = 6.dp
      ),
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (onClose != null) {
      GlassButton(
        onClick = onClose,
        shape = CircleShape
      ) {
        Icon(
          imageVector = Icons.Rounded.KeyboardArrowDown,
          contentDescription = "Close",
          modifier = Modifier.size(26.dp),
          tint = Color.White
        )
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    GlassButton(
      onClick = onToggleHistory,
      shape = RoundedCornerShape(
        topStart = 6.dp,
        bottomStart = 6.dp,
        topEnd = 22.dp,
        bottomEnd = 22.dp
      ),
      width = 42.dp
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
        contentDescription = "Song history",
        modifier = Modifier.size(22.dp),
        tint = Color.White
      )
    }
  }
}

@Composable
private fun GlassButton(
  onClick: () -> Unit,
  shape: Shape,
  modifier: Modifier = Modifier,
  width: Dp = 44.dp,
  content: @Composable () -> Unit
) {
  Box(
    modifier = modifier
      .height(44.dp)
      .width(width)
      .clip(shape)
      .background(Color.White.copy(alpha = 0.15f))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple()
      ) { onClick() },
    contentAlignment = Alignment.Center
  ) {
    content()
  }
}
