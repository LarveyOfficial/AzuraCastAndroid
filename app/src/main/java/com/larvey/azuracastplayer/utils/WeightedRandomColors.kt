package com.larvey.azuracastplayer.utils

import androidx.compose.ui.graphics.Color
import kotlin.collections.forEach
import kotlin.random.Random

/**
 * Samples [count] colors from [colors] with inverse-frequency weighting: each
 * time a color is picked its weight drops (`1 / (timesPicked + 1)`), so the
 * result spreads across the input instead of repeating one color. Feeds the
 * Now Playing mesh gradient's 9 control points from 5 palette swatches.
 *
 * [random] is injectable so tests can pass a seeded generator; production
 * callers use the default.
 */
fun weightedRandomColors(
  colors: List<Color>,
  count: Int,
  random: Random = Random.Default
): List<Color> {
  val selectionCounts = mutableMapOf<Color, Int>()
  colors.forEach { selectionCounts[it] = 0 }

  val result = mutableListOf<Color>()
  repeat(count) {
    val weights = selectionCounts.mapValues { 1.0 / (it.value + 1) } // Inverse weight
    val totalWeight = weights.values.sum()
    val randomValue = random.nextDouble(totalWeight)

    var cumulativeWeight = 0.0
    var selectedColor: Color? = null
    for ((color, weight) in weights) {
      cumulativeWeight += weight
      if (randomValue <= cumulativeWeight) {
        selectedColor = color
        break
      }
    }

    selectedColor?.let {
      result.add(it)
      selectionCounts[it] = selectionCounts[it]!! + 1
    }
  }
  return result
}
