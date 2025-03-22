package com.larvey.azuracastplayer.ui.nowplaying.components


import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.larvey.azuracastplayer.ui.mainActivity.components.meshGradient
import com.larvey.azuracastplayer.utils.conditional

@Composable
fun AnimatedBackgroundColor(colorList: MutableState<List<Color>>?) {
  val transitionA = rememberInfiniteTransition(label = "X")
  val transitionB = rememberInfiniteTransition(label = "Y")
  //region Animate Gradient Movement
  val animateA by transitionA.animateFloat(
    initialValue = 0.3f,
    targetValue = 0.8f,
    animationSpec = infiniteRepeatable(
      animation = tween(22000),
      repeatMode = RepeatMode.Reverse
    ),
    label = "X"
  )
  val animateB by transitionB.animateFloat(
    initialValue = 0.4f,
    targetValue = 0.7f,
    animationSpec = infiniteRepeatable(
      animation = tween(13000),
      repeatMode = RepeatMode.Reverse
    ),
    label = "Y"
  )
  //endregion
  Box(
    modifier = Modifier
      .fillMaxSize()
      .conditional(colorList != null && Build.VERSION.SDK_INT >= 29) {
        meshGradient(
          resolutionX = 16,
          resolutionY = 16,
          points = listOf(
            // @formatter:off
            listOf(
              Offset(0f, 0f) to colorList!!.value[0], // No move
              Offset(animateA, 0f) to colorList.value[1], // Only x moves
              Offset(1f, 0f) to colorList.value[2], // No move
            ), listOf(
              Offset(0f, animateB) to colorList.value[3], // Only y moves
              Offset(animateB, 1f - animateA) to colorList.value[4],
              Offset(1f, 1f - animateA) to colorList.value[5], // Only y moves
            ), listOf(
              Offset(0f, 1f) to colorList.value[6], // No move
              Offset(1f - animateB, 1f) to colorList.value[7], //Only x moves
              Offset(1f, 1f) to colorList.value[8], // No move
            )
            // @formatter:on
          )
        )
      }
  )
}