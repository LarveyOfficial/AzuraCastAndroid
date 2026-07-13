package com.larvey.azuracastplayer.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme

/**
 * Central facade over Material 3 Expressive motion so the whole app shares one springy
 * motion vocabulary, and any future alpha API churn (or an opt-out) is a single-file edit.
 *
 * - **Spatial** specs animate size / position / shape — springy, with a little overshoot.
 * - **Effects** specs animate color / alpha — no overshoot.
 * - The `Fast` variants are shorter/snappier (use for taps, icon crossfades).
 *
 * If [ExperimentalMaterial3ExpressiveApi] is ever pulled, replace the four bodies with the
 * commented plain-Compose fallbacks below — no call site changes required.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object AppMotion {
  private val scheme = MotionScheme.expressive()

  fun <T> spatial(): FiniteAnimationSpec<T> = scheme.defaultSpatialSpec()
  fun <T> spatialFast(): FiniteAnimationSpec<T> = scheme.fastSpatialSpec()
  fun <T> effects(): FiniteAnimationSpec<T> = scheme.defaultEffectsSpec()
  fun <T> effectsFast(): FiniteAnimationSpec<T> = scheme.fastEffectsSpec()

  // Plain-Compose fallback (drop the @OptIn above and swap these in if the API disappears):
  // fun <T> spatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.75f, stiffness = 380f)
  // fun <T> spatialFast(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.70f, stiffness = 800f)
  // fun <T> effects(): FiniteAnimationSpec<T> = tween(durationMillis = 300)
  // fun <T> effectsFast(): FiniteAnimationSpec<T> = tween(durationMillis = 150)
}
