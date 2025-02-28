package com.larvey.azuracastplayer.utils

import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils.HSLToColor
import androidx.core.graphics.ColorUtils.colorToHSL
import androidx.palette.graphics.Palette

fun correctedDominantColor(
  palette: MutableState<Palette?>?,
  isSystemInDarkTheme: Boolean
): Color? {
  val brightDominant = floatArrayOf(
    0f,
    0f,
    0f
  )
  val darkDominant = floatArrayOf(
    0f,
    0f,
    0f
  )

  if (palette?.value?.dominantSwatch?.rgb == null) {
    return null
  }

  colorToHSL(
    palette.value?.dominantSwatch?.rgb!!,
    brightDominant
  )
  colorToHSL(
    palette.value?.dominantSwatch?.rgb!!,
    darkDominant
  )
  if (brightDominant[2] <= 0.7f) {
    brightDominant[2] = 0.7f
  }
  if (darkDominant[2] >= 0.5f) {
    darkDominant[2] = 0.5f
  }

  val dominantColor = if (isSystemInDarkTheme) {
    Color(HSLToColor(brightDominant))
  } else {
    Color(HSLToColor(darkDominant))
  }

  return dominantColor
}

// Basically corrupts instead of corrects the bodyTextColor to be black and white (was done on accident but if it works it works)
fun correctedOnDominantColor(
  palette: MutableState<Palette?>?
): Color? {

  val onDominant = floatArrayOf(
    0f,
    0f,
    0f
  )

  if (palette?.value?.dominantSwatch?.bodyTextColor == null) {
    return null
  }

  colorToHSL(
    palette.value?.dominantSwatch?.bodyTextColor!!,
    onDominant
  )


  return Color(HSLToColor(onDominant))
}