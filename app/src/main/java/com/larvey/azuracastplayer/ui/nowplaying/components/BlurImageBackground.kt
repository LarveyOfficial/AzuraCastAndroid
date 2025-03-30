package com.larvey.azuracastplayer.ui.nowplaying.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.utils.BlurTransformation
import com.larvey.azuracastplayer.utils.fixHttps
import com.larvey.azuracastplayer.utils.isDark


//Backgrounds for android SDK 28 and older
@Composable
fun BlurImageBackground(playerState: PlayerState?) {

  val contrast = 0.85f // 0f..10f (1 should be default)
  val brightness = -40f // -255f..255f (0 should be default)
  val colorMatrix = floatArrayOf(
    contrast,
    0f,
    0f,
    0f,
    brightness,
    0f,
    contrast,
    0f,
    0f,
    brightness,
    0f,
    0f,
    contrast,
    0f,
    brightness,
    0f,
    0f,
    0f,
    1f,
    0f
  )
  AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
      .data(playerState?.mediaMetadata?.artworkUri.toString().fixHttps())
      .crossfade(false)
      .placeholderMemoryCacheKey(
        playerState?.mediaMetadata?.artworkUri.toString()
          .fixHttps()
      ).transformations(
        BlurTransformation(
          radius = 25
        )
      )
      .diskCacheKey(playerState?.mediaMetadata?.artworkUri.toString().fixHttps())
      .build(),
    contentDescription = "Album Art",
    contentScale = ContentScale.Crop,
    colorFilter = ColorFilter.colorMatrix(ColorMatrix(colorMatrix)),
    error = if (MaterialTheme.colorScheme.isDark()) {
      ColorPainter(MaterialTheme.colorScheme.surfaceContainer)
    } else {
      ColorPainter(Color.DarkGray)
    },
    modifier = Modifier
      .fillMaxSize()
      .background(if (MaterialTheme.colorScheme.isDark()) MaterialTheme.colorScheme.surfaceContainer else Color.DarkGray),
  )

}