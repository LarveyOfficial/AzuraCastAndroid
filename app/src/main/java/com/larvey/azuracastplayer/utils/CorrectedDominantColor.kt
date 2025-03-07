package com.larvey.azuracastplayer.utils

import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils.HSLToColor
import androidx.core.graphics.ColorUtils.colorToHSL
import androidx.palette.graphics.Palette

fun correctedVibrantColor(
  palette: MutableState<Palette?>?,
  isSystemInDarkTheme: Boolean
): Color? {
  val brightVibrant = floatArrayOf(
    0f,
    0f,
    0f
  )
  val darkVibrant = floatArrayOf(
    0f,
    0f,
    0f
  )

  if (palette?.value?.vibrantSwatch?.rgb == null) {
    return null
  }

  colorToHSL(
    palette.value?.vibrantSwatch?.rgb!!,
    brightVibrant
  )
  colorToHSL(
    palette.value?.vibrantSwatch?.rgb!!,
    darkVibrant
  )
  if (brightVibrant[2] <= 0.5f) {
    brightVibrant[2] = 0.5f
  }
  if (darkVibrant[2] >= 0.7f) {
    darkVibrant[2] = 0.7f
  }

  val vibrantColor = if (isSystemInDarkTheme) {
    Color(HSLToColor(brightVibrant))
  } else {
    Color(HSLToColor(darkVibrant))
  }

  return vibrantColor
}

fun getOnVibrantColor(
  palette: MutableState<Palette?>?
): Color? {
  if (palette?.value?.vibrantSwatch?.bodyTextColor == null) {
    return null
  }

  return Color(palette.value?.vibrantSwatch?.bodyTextColor!!)
}