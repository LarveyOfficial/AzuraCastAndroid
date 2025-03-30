package com.larvey.azuracastplayer.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

@Composable
fun ReverseLayoutDirection(content: @Composable () -> Unit) {
  val reverseDirection = when (LocalLayoutDirection.current) {
    LayoutDirection.Rtl -> LayoutDirection.Ltr
    LayoutDirection.Ltr -> LayoutDirection.Rtl
  }
  CompositionLocalProvider(LocalLayoutDirection provides reverseDirection) {
    content()
  }
}