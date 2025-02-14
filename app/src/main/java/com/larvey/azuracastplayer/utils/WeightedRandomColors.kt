package com.larvey.azuracastplayer.utils

import androidx.compose.ui.graphics.Color
import kotlin.collections.forEach
import kotlin.random.Random

fun weightedRandomColors(colors: List<Color>, count: Int): List<Color> {
  val selectionCounts = mutableMapOf<Color, Int>()
  colors.forEach { selectionCounts[it] = 0 }

  val result = mutableListOf<Color>()
  repeat(count) {
    val weights = selectionCounts.mapValues { 1.0 / (it.value + 1) } // Inverse weight
    val totalWeight = weights.values.sum()
    val randomValue = Random.nextDouble(totalWeight)

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