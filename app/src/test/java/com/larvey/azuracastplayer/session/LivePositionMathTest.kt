package com.larvey.azuracastplayer.session

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Locks the live-stream position arithmetic extracted from the
 * ForwardingPlayer.getCurrentPosition override — including its known
 * clock-skew quirk, which is documented rather than fixed (fixing it would be
 * a behavior change outside this refactor's scope).
 */
class LivePositionMathTest {

  @Test
  fun `elapsed time derives from wall clock vs played_at for dynamic streams`() {
    // Song started 100 s ago; raw stream position 4 s.
    val result = computeLiveElapsedMs(
      nowMs = 1_000_100_000L,
      playedAtSec = 1_000_000L,
      isDynamic = true,
      rawPositionMs = 4_000L
    )

    assertThat(result).isEqualTo(96_000L) // (100 s * 1000) - 4000
  }

  @Test
  fun `non-dynamic items do not subtract the raw position`() {
    val result = computeLiveElapsedMs(
      nowMs = 1_000_100_000L,
      playedAtSec = 1_000_000L,
      isDynamic = false,
      rawPositionMs = 4_000L
    )

    assertThat(result).isEqualTo(100_000L)
  }

  @Test
  fun `zero elapsed when the song just started`() {
    val result = computeLiveElapsedMs(
      nowMs = 1_000_000_000L,
      playedAtSec = 1_000_000L,
      isDynamic = false,
      rawPositionMs = 0L
    )

    assertThat(result).isEqualTo(0L)
  }

  @Test
  fun `clock-skew guard quirk - dynamic items return negated raw position`() {
    // Device clock behind played_at: the guard computes
    // (playedAtSec - playedAtSec) * 1000 = 0, so the result is -rawPositionMs
    // rather than a plausible elapsed time. Deliberately preserved.
    val result = computeLiveElapsedMs(
      nowMs = 999_000_000L, // 999_000 s < playedAt 1_000_000 s
      playedAtSec = 1_000_000L,
      isDynamic = true,
      rawPositionMs = 4_000L
    )

    assertThat(result).isEqualTo(-4_000L)
  }

  @Test
  fun `clock-skew guard quirk - non-dynamic items return zero`() {
    val result = computeLiveElapsedMs(
      nowMs = 999_000_000L,
      playedAtSec = 1_000_000L,
      isDynamic = false,
      rawPositionMs = 4_000L
    )

    assertThat(result).isEqualTo(0L)
  }

  @Test
  fun `sub-second wall clock truncates to whole seconds`() {
    // nowMs / 1000 uses integer division — 999 ms of drift vanishes.
    val result = computeLiveElapsedMs(
      nowMs = 1_000_100_999L,
      playedAtSec = 1_000_000L,
      isDynamic = false,
      rawPositionMs = 0L
    )

    assertThat(result).isEqualTo(100_000L)
  }
}
