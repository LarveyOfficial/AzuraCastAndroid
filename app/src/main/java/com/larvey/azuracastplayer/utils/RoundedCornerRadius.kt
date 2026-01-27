package com.larvey.azuracastplayer.utils

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun getRoundedCornerRadius(): Dp {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val view = LocalView.current
    val windowInsets = view.rootWindowInsets
    val roundedCorner = windowInsets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
    val radiusPx = roundedCorner?.radius ?: 0
    val density = LocalDensity.current
    val rounding = with(density) {
      radiusPx.toDp().value.toInt().dp
    }
    val adjustedValue = rounding - (rounding.value * 0.1f).dp
    return adjustedValue
  }
  return 0.dp
}