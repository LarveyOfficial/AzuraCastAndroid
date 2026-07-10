package com.larvey.azuracastplayer.classes.data

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.Test

/**
 * Locks the Gson contract for [DiscoveryJSON]. Unlike [StationJSON], these
 * classes carry no @SerializedName annotations — parsing relies on exact
 * property-name matching with the published catalog, so this contract is
 * worth pinning explicitly.
 */
class DiscoveryJsonParsingTest {

  private val gson = Gson()

  private fun fixture(name: String): String =
    javaClass.classLoader!!.getResourceAsStream("fixtures/$name")!!
      .bufferedReader()
      .use { it.readText() }

  @Test
  fun `parses the full catalog shape`() {
    val catalog = gson.fromJson(
      fixture("discovery_catalog.json"),
      DiscoveryJSON::class.java
    )

    assertThat(catalog.lastUpdated).isEqualTo("2026-07-01T00:00:00Z")
    assertThat(catalog.featuredStations.title).isEqualTo("Featured Stations")
    assertThat(catalog.featuredStations.stations).hasSize(1)
    assertThat(catalog.discoveryStations).hasSize(2)
    assertThat(catalog.discoveryStations.map { it.title })
      .containsExactly(
        "Chill Beats",
        "Talk & News"
      )
      .inOrder()
  }

  @Test
  fun `parses station fields including hosts with ports`() {
    val catalog = gson.fromJson(
      fixture("discovery_catalog.json"),
      DiscoveryJSON::class.java
    )

    val lofi = catalog.discoveryStations[0].stations[0]
    assertThat(lofi.friendlyName).isEqualTo("Lo-Fi Lounge")
    assertThat(lofi.shortCode).isEqualTo("lofi_lounge")
    // publicPlayerUrl is what the media-id routing derives the host from —
    // ports must survive parsing.
    assertThat(lofi.publicPlayerUrl)
      .isEqualTo("https://lofi.example.com:8080/public/lofi_lounge")
    assertThat(lofi.preferredMount)
      .isEqualTo("https://lofi.example.com:8080/listen/lofi_lounge/radio.mp3")
    assertThat(lofi.supportsHls).isFalse()
    assertThat(lofi.hlsUrl).isNull()
  }

  @Test
  fun `featured station hls fields parse`() {
    val catalog = gson.fromJson(
      fixture("discovery_catalog.json"),
      DiscoveryJSON::class.java
    )

    val featured = catalog.featuredStations.stations[0]
    assertThat(featured.supportsHls).isTrue()
    assertThat(featured.hlsUrl)
      .isEqualTo("https://featured.example.com/hls/featured_fm/live.m3u8")
  }
}
