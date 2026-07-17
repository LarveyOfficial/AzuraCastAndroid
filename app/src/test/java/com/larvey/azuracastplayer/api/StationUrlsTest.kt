package com.larvey.azuracastplayer.api

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Locks the URL construction for the per-station endpoints. These must match
 * the URLs the pre-repository code built (`"https://$url"` + literal paths),
 * including hosts that carry an explicit port.
 */
class StationUrlsTest {

  @Test
  fun `builds station now-playing url`() {
    assertThat(
      AzuraCastRepository.nowPlayingUrl(
        "radio.example.com",
        "my_station"
      )
    ).isEqualTo("https://radio.example.com/api/nowplaying/my_station")
  }

  @Test
  fun `builds station now-playing url for host with port`() {
    assertThat(
      AzuraCastRepository.nowPlayingUrl(
        "radio.example.com:8080",
        "my_station"
      )
    ).isEqualTo("https://radio.example.com:8080/api/nowplaying/my_station")
  }

  @Test
  fun `builds host station-list url`() {
    assertThat(AzuraCastRepository.nowPlayingUrl("radio.example.com"))
      .isEqualTo("https://radio.example.com/api/nowplaying")
  }

  @Test
  fun `builds host station-list url for host with port`() {
    assertThat(AzuraCastRepository.nowPlayingUrl("radio.example.com:8000"))
      .isEqualTo("https://radio.example.com:8000/api/nowplaying")
  }
}
