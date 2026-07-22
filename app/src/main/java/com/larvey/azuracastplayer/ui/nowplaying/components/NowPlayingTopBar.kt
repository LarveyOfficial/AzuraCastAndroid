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
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.palette.graphics.Palette
import com.larvey.azuracastplayer.utils.albumColors

/**
 * The Now Playing top bar: an optional collapse button on the left, and (right-aligned) an
 * optional Cast button followed by the song-history button. The Cast/history pair reads as a split
 * button — the history button is the right half (squared inner/left edge, fully-rounded outer/right
 * edge) and the Cast button mirrors it (rounded outer/left edge, squared inner/right edge). The
 * collapse button is a plain circle.
 *
 * All three buttons are tinted from the current album art: a pale [AlbumColors.lightChip] fill with
 * a deep album-hued [AlbumColors.onLightChip] icon — the same pair the play/pause chip uses — so the
 * bar coheres with the rest of the album-tinted controls instead of reading as neutral glass.
 *
 * [onClose] is nullable so the side-pane layout (which has nothing to collapse) can omit it.
 * [onCast] is nullable so the button is omitted when casting is unavailable.
 */
@Composable
fun NowPlayingTopBar(
  onToggleHistory: () -> Unit,
  modifier: Modifier = Modifier,
  palette: Palette? = null,
  onClose: (() -> Unit)? = null,
  onCast: (() -> Unit)? = null,
  isCasting: Boolean = false,
  isConnecting: Boolean = false
) {
  val colors = albumColors(palette)
  val containerColor = colors.lightChip
  val contentColor = colors.onLightChip

  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        horizontal = 14.dp,
        vertical = 6.dp
      ),
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (onClose != null) {
      GlassButton(
        onClick = onClose,
        shape = CircleShape,
        containerColor = containerColor,
        width = 42.dp
      ) {
        Icon(
          imageVector = Icons.Rounded.KeyboardArrowDown,
          contentDescription = "Close",
          tint = contentColor
        )
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    if (onCast != null) {
      GlassButton(
        onClick = onCast,
        // Mirror of the history button: rounded outer (left) edge, squared inner (right) edge.
        shape = RoundedCornerShape(
          topStart = 50.dp,
          bottomStart = 50.dp,
          topEnd = 6.dp,
          bottomEnd = 6.dp
        ),
        containerColor = containerColor
      ) {
        when {
          isConnecting -> CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = contentColor
          )
          else -> Icon(
            imageVector = if (isCasting) Icons.Rounded.CastConnected else Icons.Rounded.Cast,
            contentDescription = "Cast",
            tint = contentColor
          )
        }
      }

      Spacer(modifier = Modifier.width(6.dp))
    }

    GlassButton(
      onClick = onToggleHistory,
      shape = RoundedCornerShape(
        topStart = 6.dp,
        bottomStart = 6.dp,
        topEnd = 50.dp,
        bottomEnd = 50.dp
      ),
      containerColor = containerColor
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
        contentDescription = "Song history",
        tint = contentColor
      )
    }
  }
}

@Composable
private fun GlassButton(
  onClick: () -> Unit,
  shape: Shape,
  containerColor: Color,
  modifier: Modifier = Modifier,
  width: Dp = 50.dp,
  content: @Composable () -> Unit
) {
  Box(
    modifier = modifier
      .height(42.dp)
      .width(width)
      .clip(shape)
      .background(containerColor)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple()
      ) { onClick() },
    contentAlignment = Alignment.Center
  ) {
    content()
  }
}
