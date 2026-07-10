package com.larvey.azuracastplayer.session

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Locks the media-id prefix routing that Android Auto playback depends on.
 * The id scheme (SAVED_STATION- / DISCOVERED- / bare mount) is produced by
 * MusicPlayerService's browse tree and consumed by onAddMediaItems.
 */
class MediaIdRoutingTest {

  @Test
  fun `saved station prefix routes with the prefix stripped`() {
    val route = resolveMediaIdRoute("SAVED_STATION-https://radio.example.com/listen/a/radio.mp3")

    assertThat(route).isInstanceOf(MediaIdRoute.SavedStation::class.java)
    assertThat(route.mount).isEqualTo("https://radio.example.com/listen/a/radio.mp3")
  }

  @Test
  fun `discovered prefix routes with the prefix stripped`() {
    val route = resolveMediaIdRoute("DISCOVERED-https://lofi.example.com:8080/listen/l/radio.mp3")

    assertThat(route).isInstanceOf(MediaIdRoute.Discovered::class.java)
    assertThat(route.mount).isEqualTo("https://lofi.example.com:8080/listen/l/radio.mp3")
  }

  @Test
  fun `unprefixed id routes direct and unchanged`() {
    val route = resolveMediaIdRoute("https://radio.example.com/listen/a/radio.mp3")

    assertThat(route).isInstanceOf(MediaIdRoute.Direct::class.java)
    assertThat(route.mount).isEqualTo("https://radio.example.com/listen/a/radio.mp3")
  }

  @Test
  fun `only the first prefix occurrence is stripped`() {
    // replaceFirst semantics: a pathological mount containing the prefix
    // string keeps its inner occurrence.
    val route = resolveMediaIdRoute("SAVED_STATION-SAVED_STATION-mount")

    assertThat(route).isInstanceOf(MediaIdRoute.SavedStation::class.java)
    assertThat(route.mount).isEqualTo("SAVED_STATION-mount")
  }

  @Test
  fun `prefix match is exact and case-sensitive`() {
    assertThat(resolveMediaIdRoute("saved_station-mount"))
      .isInstanceOf(MediaIdRoute.Direct::class.java)
    assertThat(resolveMediaIdRoute("DISCOVERED_mount"))
      .isInstanceOf(MediaIdRoute.Direct::class.java)
  }

  @Test
  fun `empty id routes direct`() {
    val route = resolveMediaIdRoute("")

    assertThat(route).isInstanceOf(MediaIdRoute.Direct::class.java)
    assertThat(route.mount).isEmpty()
  }
}
