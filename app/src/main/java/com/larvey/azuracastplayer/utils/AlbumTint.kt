package com.larvey.azuracastplayer.utils

import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils.HSLToColor
import androidx.core.graphics.ColorUtils.colorToHSL
import androidx.palette.graphics.Palette

/** An album-derived container tone plus a legible on-color for text/icons over it. */
data class AlbumTint(
  val container: Color,
  val onContainer: Color
)

private fun Palette.bestSwatch(): Palette.Swatch? =
  vibrantSwatch ?: dominantSwatch ?: mutedSwatch

/**
 * A muted, theme-appropriate "container" tint pulled from the artwork — conceptually like
 * Material's *Container tonal roles, but sourced from the album art instead of the app theme
 * (this is why an album-tinted card doesn't fight Material You: the hue comes from the song,
 * not the dynamic scheme). Saturation is capped so the card reads as a tint rather than a slab
 * of pure color; lightness is pinned per theme for guaranteed text contrast.
 *
 * Returns null when there is no usable swatch (no art loaded yet) so callers fall back to a
 * neutral surface.
 */
fun albumTint(
  palette: Palette?,
  isDark: Boolean
): AlbumTint? {
  val swatch = palette?.bestSwatch() ?: return null

  val hsl = floatArrayOf(0f, 0f, 0f)
  colorToHSL(swatch.rgb, hsl)

  hsl[1] = hsl[1].coerceAtMost(0.55f)
  hsl[2] = if (isDark) 0.28f else 0.87f
  val container = Color(HSLToColor(hsl))

  val onContainer = Color(
    HSLToColor(
      floatArrayOf(
        hsl[0],
        hsl[1] * 0.6f,
        if (isDark) 0.93f else 0.15f
      )
    )
  )

  return AlbumTint(container, onContainer)
}

fun albumTint(
  palette: MutableState<Palette?>?,
  isDark: Boolean
): AlbumTint? = albumTint(palette?.value, isDark)

/**
 * A pale, album-hued "chip" tone with a deep album-hued on-color — for a light action button
 * (the Now Playing play/pause) that sits on the dark-ish palette background. Theme-independent:
 * the button stays light so it always reads as the primary affordance, and the icon carries the
 * album color. Returns null (→ caller falls back to white) when there is no usable swatch.
 */
fun albumLightChip(palette: Palette?): AlbumTint? {
  val swatch = palette?.bestSwatch() ?: return null

  val hsl = floatArrayOf(0f, 0f, 0f)
  colorToHSL(swatch.rgb, hsl)
  val sat = hsl[1].coerceAtMost(0.6f)

  val container = Color(HSLToColor(floatArrayOf(hsl[0], sat * 0.5f, 0.87f)))
  val onContainer = Color(HSLToColor(floatArrayOf(hsl[0], sat, 0.28f)))
  return AlbumTint(container, onContainer)
}
