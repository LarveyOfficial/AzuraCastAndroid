package com.larvey.azuracastplayer.utils

import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A soft scalloped/"flower" shape used behind the active Cast device's icon. The
 * outline is a smooth polar curve `r = base * (1 + curve * cos(sides * t))`, so
 * with e.g. `sides = 8` it reads as an 8-lobed rounded star.
 */
class RoundedStarShape(
  private val sides: Int = 8,
  private val curve: Double = 0.10,
  private val rotation: Float = 0f
) : Shape {
  override fun createOutline(
    size: androidx.compose.ui.geometry.Size,
    layoutDirection: LayoutDirection,
    density: Density
  ): Outline {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val baseRadius = min(size.width, size.height) * 0.5f / (1f + curve.toFloat())
    val steps = 360
    val rotationRad = Math.toRadians(rotation.toDouble())

    val path = Path()
    for (i in 0..steps) {
      val t = (i.toDouble() / steps) * 2.0 * PI
      val r = baseRadius * (1.0 + curve * cos(sides * t))
      val x = cx + (r * cos(t + rotationRad)).toFloat()
      val y = cy + (r * sin(t + rotationRad)).toFloat()
      if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return Outline.Generic(path)
  }
}
