package com.larvey.azuracastplayer.classes.data

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.Test

/**
 * Locks the Gson contract for [StationJSON] against realistic AzuraCast
 * "static now-playing" payloads, so schema regressions (renamed fields,
 * broken @SerializedName mappings, numeric-type drift) fail in CI instead of
 * silently producing null metadata at runtime.
 */
class StationJsonParsingTest {

  private val gson = Gson()

  private fun fixture(name: String): String =
    javaClass.classLoader!!.getResourceAsStream("fixtures/$name")!!
      .bufferedReader()
      .use { it.readText() }

  private fun parse(name: String): StationJSON =
    gson.fromJson(
      fixture(name),
      StationJSON::class.java
    )

  @Test
  fun `parses snake_case fields via SerializedName`() {
    val data = parse("nowplaying_static_full.json")

    assertThat(data.nowPlaying.playedAt).isEqualTo(1751990400L)
    assertThat(data.isOnline).isTrue()
    assertThat(data.station.hlsEnabled).isTrue()
    assertThat(data.station.hlsUrl)
      .isEqualTo("https://radio.example.com/hls/test_radio/live.m3u8")
    assertThat(data.playingNext).isNotNull()
    assertThat(data.songHistory).hasSize(2)
    assertThat(data.station.mounts[0].isDefault).isTrue()
  }

  @Test
  fun `parses station and song details`() {
    val data = parse("nowplaying_static_full.json")

    assertThat(data.station.name).isEqualTo("Test Radio")
    assertThat(data.station.mounts).hasSize(3)
    assertThat(data.station.mounts.map { it.format })
      .containsExactly(
        "mp3",
        "flac",
        "ogg"
      )
      .inOrder()
    assertThat(data.nowPlaying.song.artist).isEqualTo("Test Artist")
    assertThat(data.nowPlaying.song.album).isEqualTo("Test Album")
    assertThat(data.listeners.current).isEqualTo(13L)
  }

  @Test
  fun `duration converts to the expected milliseconds`() {
    val data = parse("nowplaying_static_full.json")

    // Gson materializes Number-typed fields as its internal
    // LazilyParsedNumber; the numeric conversions are the actual contract.
    // The metadata path relies on duration.toLong() * 1000 producing
    // milliseconds — lock that arithmetic here.
    assertThat(data.nowPlaying.duration.toLong() * 1000).isEqualTo(245_000L)
    assertThat(data.nowPlaying.duration.toDouble()).isEqualTo(245.0)
  }

  @Test
  fun `fractional duration truncates toward zero when converted`() {
    val data = parse("nowplaying_static_full.json")

    // song_history[1] has "duration": 180.5 — toLong() truncates.
    assertThat(data.songHistory[1].duration.toLong()).isEqualTo(180L)
  }

  @Test
  fun `minimal payload with nulls parses`() {
    val data = parse("nowplaying_static_minimal.json")

    assertThat(data.playingNext).isNull()
    assertThat(data.songHistory).isEmpty()
    assertThat(data.station.hlsUrl).isNull()
    assertThat(data.station.mounts[0].bitrate).isNull()
    assertThat(data.station.mounts[0].format).isNull()
    assertThat(data.nowPlaying.song.isrc).isNull()
    assertThat(data.isOnline).isFalse()
  }

  @Test
  fun `missing non-nullable fields become null despite Kotlin types`() {
    // Documents a known Kotlin+Gson quirk (not a behavior we rely on): Gson
    // uses reflection without Kotlin null-safety, so a field absent from the
    // JSON is left null even when the Kotlin type is non-nullable. Downstream
    // code null-checks defensively; this test pins the quirk so a future
    // parser swap (or Gson config change) that alters it is a visible event.
    val data = gson.fromJson(
      """{"station": {"id": 1, "name": "Ghost"}}""",
      StationJSON::class.java
    )

    assertThat(data.station.name).isEqualTo("Ghost")
    @Suppress("SENSELESS_COMPARISON")
    assertThat(data.nowPlaying == null).isTrue()
  }
}
