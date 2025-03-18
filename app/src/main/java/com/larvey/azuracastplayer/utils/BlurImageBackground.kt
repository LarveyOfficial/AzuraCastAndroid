package com.larvey.azuracastplayer.utils

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.request.RequestOptions
import com.larvey.azuracastplayer.state.PlayerState
import jp.wasabeef.glide.transformations.BlurTransformation


//Backgrounds for android SDK 28 and older
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun BlurImageBackground(playerState: PlayerState?) {

  val contrast = 1f // 0f..10f (1 should be default)
  val brightness = -80f // -255f..255f (0 should be default)
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
  GlideImage(
    model = playerState?.mediaMetadata?.artworkUri,
    modifier = Modifier
      .fillMaxSize(),
    contentDescription = "${playerState?.mediaMetadata?.albumTitle}",
    transition = CrossFade,
    contentScale = ContentScale.Crop,
    colorFilter = ColorFilter.colorMatrix(ColorMatrix(colorMatrix))
  ) {
    it.apply(
      RequestOptions.bitmapTransform(
        BlurTransformation(
          30,
          3
        )
      )
    )
  }

}