package com.larvey.azuracastplayer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * Centralized "expressive" rounded shape so the squircle-vs-rounded decision is a single
 * toggle for the whole app (mini-player card, morphing play/pause, floating nav bar).
 *
 * Currently a plain [RoundedCornerShape]: at the radii we use (14–60dp) it is visually
 * indistinguishable from a superellipse squircle and carries zero dependency / R8 / 16KB
 * risk. To adopt a true squircle later, change ONLY this function — either:
 *   - derive shapes from `androidx.compose.material3.MaterialShapes` (`.toShape()`), or
 *   - add `com.github.racra:smooth-corner-rect-android-compose` (JitPack is already
 *     configured in settings.gradle.kts) and return an `AbsoluteSmoothCornerShape`.
 */
fun expressiveShape(radius: Dp): Shape = RoundedCornerShape(radius)
