package com.larvey.azuracastplayer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.larvey.azuracastplayer.R

/**
 * App-wide rounded typeface: Nunito, a rounded humanist variable font (SIL OFL 1.1 — license
 * bundled at assets/Nunito-OFL.txt). All weights come from the single variable font via its
 * `wght` axis.
 */
@OptIn(ExperimentalTextApi::class)
val Rounded = FontFamily(
  Font(
    R.font.nunito_variable,
    FontWeight.Normal,
    // Nunito reads a touch light at 400; nudge the base weight up slightly.
    variationSettings = FontVariation.Settings(FontVariation.weight(470))
  ),
  Font(
    R.font.nunito_variable,
    FontWeight.Medium,
    variationSettings = FontVariation.Settings(FontVariation.weight(500))
  ),
  Font(
    R.font.nunito_variable,
    FontWeight.SemiBold,
    variationSettings = FontVariation.Settings(FontVariation.weight(600))
  ),
  Font(
    R.font.nunito_variable,
    FontWeight.Bold,
    variationSettings = FontVariation.Settings(FontVariation.weight(700))
  ),
  Font(
    R.font.nunito_variable,
    FontWeight.ExtraBold,
    variationSettings = FontVariation.Settings(FontVariation.weight(800))
  )
)

private val default = Typography()

// Apply the rounded family to every text style so the whole app uses it.
val Typography = Typography(
  displayLarge = default.displayLarge.copy(fontFamily = Rounded),
  displayMedium = default.displayMedium.copy(fontFamily = Rounded),
  displaySmall = default.displaySmall.copy(fontFamily = Rounded),
  headlineLarge = default.headlineLarge.copy(fontFamily = Rounded),
  headlineMedium = default.headlineMedium.copy(fontFamily = Rounded),
  headlineSmall = default.headlineSmall.copy(fontFamily = Rounded),
  titleLarge = default.titleLarge.copy(fontFamily = Rounded),
  titleMedium = default.titleMedium.copy(fontFamily = Rounded),
  titleSmall = default.titleSmall.copy(fontFamily = Rounded),
  bodyLarge = default.bodyLarge.copy(fontFamily = Rounded),
  bodyMedium = default.bodyMedium.copy(fontFamily = Rounded),
  bodySmall = default.bodySmall.copy(fontFamily = Rounded),
  labelLarge = default.labelLarge.copy(fontFamily = Rounded),
  labelMedium = default.labelMedium.copy(fontFamily = Rounded),
  labelSmall = default.labelSmall.copy(fontFamily = Rounded)
)
