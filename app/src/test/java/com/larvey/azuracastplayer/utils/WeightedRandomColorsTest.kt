package com.larvey.azuracastplayer.utils

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

/**
 * Locks the inverse-frequency color sampling that feeds the mesh gradient.
 * A seeded [Random] makes the sampling deterministic for assertions.
 */
class WeightedRandomColorsTest {

  private val palette = listOf(
    Color.Red,
    Color.Green,
    Color.Blue,
    Color.Yellow,
    Color.Magenta
  )

  @Test
  fun `returns the requested number of colors`() {
    val result = weightedRandomColors(
      palette,
      9,
      Random(42)
    )

    assertThat(result).hasSize(9)
  }

  @Test
  fun `only emits colors from the input palette`() {
    val result = weightedRandomColors(
      palette,
      9,
      Random(7)
    )

    assertThat(palette).containsAtLeastElementsIn(result.distinct())
  }

  @Test
  fun `is deterministic for a fixed seed`() {
    val a = weightedRandomColors(
      palette,
      9,
      Random(1234)
    )
    val b = weightedRandomColors(
      palette,
      9,
      Random(1234)
    )

    assertThat(a).isEqualTo(b)
  }

  @Test
  fun `inverse weighting spreads picks across the palette`() {
    // With 5 colors and 9 draws, inverse-frequency weighting should never
    // let one color dominate: across several seeds, every draw uses at
    // least 3 distinct colors.
    (0L until 20L).forEach { seed ->
      val result = weightedRandomColors(
        palette,
        9,
        Random(seed)
      )
      assertThat(result.distinct().size).isAtLeast(3)
    }
  }

  @Test
  fun `single-color palette repeats that color`() {
    val result = weightedRandomColors(
      listOf(Color.Cyan),
      4,
      Random(0)
    )

    assertThat(result).containsExactly(
      Color.Cyan,
      Color.Cyan,
      Color.Cyan,
      Color.Cyan
    )
  }

  @Test
  fun `zero count returns empty`() {
    assertThat(
      weightedRandomColors(
        palette,
        0,
        Random(0)
      )
    ).isEmpty()
  }
}
