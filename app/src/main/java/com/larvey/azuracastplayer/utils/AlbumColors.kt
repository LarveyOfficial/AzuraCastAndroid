package com.larvey.azuracastplayer.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.core.graphics.ColorUtils.HSLToColor
import androidx.core.graphics.ColorUtils.colorToHSL
import androidx.palette.graphics.Palette

/**
 * A coherent set of album-derived color roles, all computed from ONE seed swatch so they are
 * always harmonious (same hue family) and always present.
 *
 * This replaces the old per-consumer helpers (correctedVibrantColor / getOnVibrantColor /
 * albumTint / albumLightChip) that each reached for a *different* swatch — usually
 * `vibrantSwatch`, which is the swatch most likely to be null (grayscale / monochrome / pastel
 * art) — and fell back to unrelated colors. That mismatch is why the play/pause chip and the
 * mini-player card sometimes stopped matching. Here every role derives from a single resilient
 * seed, so a missing swatch degrades gracefully instead of splitting into clashing colors.
 */
data class AlbumColors(
  val accent: Color,       // legible mid-tone: mini play chip bg, Add-station FAB bg
  val onAccent: Color,     // legible icon/text on [accent]
  val container: Color,    // muted card tint: mini-player background
  val onContainer: Color,  // legible text on [container]
  val lightChip: Color,    // pale button on the dark Now Playing backdrop (play/pause)
  val onLightChip: Color,  // deep album-hued icon on [lightChip]
  val bright: Color,       // bright fill that pops on the dark Now Playing background (progress)
  val track: Color         // faint unfilled track (progress)
)

/**
 * The resilient seed chain. Vibrant is preferred (most saturated / "album-y") but it is also the
 * swatch most likely to be null, so we fall through every other swatch. [Palette.dominantSwatch]
 * exists for essentially any colored image, so this returns null only when the palette has no
 * swatches at all (no art loaded, or a fully flat image).
 */
private fun Palette.seedSwatch(): Palette.Swatch? =
  vibrantSwatch
    ?: lightVibrantSwatch
    ?: darkVibrantSwatch
    ?: dominantSwatch
    ?: mutedSwatch
    ?: lightMutedSwatch
    ?: darkMutedSwatch

private fun hsl(
  hue: Float,
  sat: Float,
  light: Float
): Color = Color(HSLToColor(floatArrayOf(hue, sat, light)))

/**
 * Resolves [AlbumColors] from a [palette]. When no usable swatch exists, returns a coherent
 * fallback built from the current Material theme (the roles still relate to each other).
 */
@Composable
fun albumColors(palette: Palette?): AlbumColors {
  val scheme = MaterialTheme.colorScheme
  val isDark = scheme.isDark()

  val seed = palette?.seedSwatch()
    ?: return AlbumColors(
      accent = scheme.primary,
      onAccent = scheme.onPrimary,
      container = scheme.surfaceContainerHigh,
      onContainer = scheme.onSurface,
      lightChip = scheme.primaryContainer,
      onLightChip = scheme.onPrimaryContainer,
      bright = scheme.primary,
      track = scheme.onSurface.copy(alpha = 0.22f)
    )

  val base = floatArrayOf(0f, 0f, 0f)
  colorToHSL(seed.rgb, base)
  val hue = base[0]
  val sat = base[1]

  // accent: the seed at a legible lightness for the current theme (bright on dark, darker on
  // light) — same clamps the old correctedVibrantColor used, but from the resilient seed.
  val accentHsl = floatArrayOf(hue, sat, base[2])
  if (isDark) floorLightness(accentHsl, 0.5f) else capLightness(accentHsl, 0.7f)
  val accent = Color(HSLToColor(accentHsl))
  val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White

  val container = hsl(hue, sat.coerceAtMost(0.55f), if (isDark) 0.28f else 0.87f)
  val onContainer = hsl(hue, (sat * 0.6f).coerceAtMost(0.4f), if (isDark) 0.93f else 0.15f)

  val lightChip = hsl(hue, (sat * 0.5f).coerceAtMost(0.4f), 0.88f)
  val onLightChip = hsl(hue, sat.coerceAtMost(0.6f), 0.28f)

  val bright = hsl(hue, sat.coerceAtMost(0.8f), 0.72f)
  val track = Color.White.copy(alpha = 0.22f)

  return AlbumColors(
    accent = accent,
    onAccent = onAccent,
    container = container,
    onContainer = onContainer,
    lightChip = lightChip,
    onLightChip = onLightChip,
    bright = bright,
    track = track
  )
}
