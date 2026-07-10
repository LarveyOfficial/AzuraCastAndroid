package com.larvey.azuracastplayer.utils

import com.google.common.truth.Truth.assertThat
import com.larvey.azuracastplayer.classes.data.Mount
import org.junit.Test

/** Locks the shared flac/ogg/opus mount exclusion. */
class MountFiltersTest {

  private fun mount(format: String?) = Mount(
    id = 1L,
    name = "Test",
    url = "https://radio.example.com/listen",
    bitrate = 128L,
    format = format,
    path = "/listen",
    isDefault = true
  )

  @Test
  fun `keeps supported formats`() {
    val mounts = listOf(
      mount("mp3"),
      mount("aac")
    )

    assertThat(mounts.supportedMounts()).hasSize(2)
  }

  @Test
  fun `filters flac ogg and opus`() {
    val mounts = listOf(
      mount("mp3"),
      mount("flac"),
      mount("ogg"),
      mount("opus")
    )

    assertThat(mounts.supportedMounts().map { it.format }).containsExactly("mp3")
  }

  @Test
  fun `null format is kept`() {
    // Stations sometimes omit the format field; those mounts stay playable.
    assertThat(listOf(mount(null)).supportedMounts()).hasSize(1)
  }

  @Test
  fun `match is case-sensitive`() {
    // AzuraCast reports formats lowercase; an uppercase value would slip
    // through — documents current behavior.
    assertThat(listOf(mount("FLAC")).supportedMounts()).hasSize(1)
  }

  @Test
  fun `all-unsupported list becomes empty`() {
    val mounts = listOf(
      mount("flac"),
      mount("opus")
    )

    assertThat(mounts.supportedMounts()).isEmpty()
  }
}
