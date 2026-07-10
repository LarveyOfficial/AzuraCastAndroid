package com.larvey.azuracastplayer.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Locks the in-place HSL clamps used by the palette color pipeline. */
class HslClampTest {

  @Test
  fun `clampHsl caps lightness and saturation`() {
    val hsl = floatArrayOf(
      200f,
      0.9f,
      0.8f
    )

    clampHsl(
      hsl,
      maxLightness = 0.45f,
      maxSaturation = 0.75f
    )

    assertThat(hsl[1]).isEqualTo(0.75f)
    assertThat(hsl[2]).isEqualTo(0.45f)
    assertThat(hsl[0]).isEqualTo(200f) // hue untouched
  }

  @Test
  fun `clampHsl leaves in-range values alone`() {
    val hsl = floatArrayOf(
      120f,
      0.5f,
      0.3f
    )

    clampHsl(
      hsl,
      maxLightness = 0.45f,
      maxSaturation = 0.75f
    )

    assertThat(hsl[1]).isEqualTo(0.5f)
    assertThat(hsl[2]).isEqualTo(0.3f)
  }

  @Test
  fun `clampHsl uses strict comparison so boundary values pass through`() {
    val hsl = floatArrayOf(
      0f,
      0.75f,
      0.45f
    )

    clampHsl(
      hsl,
      maxLightness = 0.45f,
      maxSaturation = 0.75f
    )

    assertThat(hsl[1]).isEqualTo(0.75f)
    assertThat(hsl[2]).isEqualTo(0.45f)
  }

  @Test
  fun `floorLightness raises dark values`() {
    val hsl = floatArrayOf(
      0f,
      1f,
      0.2f
    )

    floorLightness(
      hsl,
      0.5f
    )

    assertThat(hsl[2]).isEqualTo(0.5f)
  }

  @Test
  fun `floorLightness keeps bright values`() {
    val hsl = floatArrayOf(
      0f,
      1f,
      0.8f
    )

    floorLightness(
      hsl,
      0.5f
    )

    assertThat(hsl[2]).isEqualTo(0.8f)
  }

  @Test
  fun `capLightness lowers bright values`() {
    val hsl = floatArrayOf(
      0f,
      1f,
      0.9f
    )

    capLightness(
      hsl,
      0.7f
    )

    assertThat(hsl[2]).isEqualTo(0.7f)
  }

  @Test
  fun `capLightness keeps dark values`() {
    val hsl = floatArrayOf(
      0f,
      1f,
      0.3f
    )

    capLightness(
      hsl,
      0.7f
    )

    assertThat(hsl[2]).isEqualTo(0.3f)
  }
}
