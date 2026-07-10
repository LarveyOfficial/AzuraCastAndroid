package com.larvey.azuracastplayer.utils

/**
 * In-place HSL clamps shared by the palette color pipeline. All three mutate
 * the given `[hue, saturation, lightness]` array — matching the original
 * inlined logic, which clamped Palette swatch arrays in place.
 */

/**
 * Caps lightness at [maxLightness] and saturation at [maxSaturation]. Used to
 * keep the Now Playing mesh-gradient colors dark and muted enough for white
 * foreground text.
 */
fun clampHsl(
  hsl: FloatArray,
  maxLightness: Float,
  maxSaturation: Float
) {
  if (hsl[2] > maxLightness) {
    hsl[2] = maxLightness
  }
  if (hsl[1] > maxSaturation) {
    hsl[1] = maxSaturation
  }
}

/** Raises lightness to at least [minLightness] (legibility on dark themes). */
fun floorLightness(hsl: FloatArray, minLightness: Float) {
  if (hsl[2] <= minLightness) {
    hsl[2] = minLightness
  }
}

/** Lowers lightness to at most [maxLightness] (legibility on light themes). */
fun capLightness(hsl: FloatArray, maxLightness: Float) {
  if (hsl[2] >= maxLightness) {
    hsl[2] = maxLightness
  }
}
