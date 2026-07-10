package com.larvey.azuracastplayer.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Locks the add-station input scheme normalization. */
class UrlNormalizationTest {

  @Test
  fun `prefixes https onto bare host`() {
    assertThat(normalizeToHttpsScheme("radio.example.com"))
      .isEqualTo("https://radio.example.com")
  }

  @Test
  fun `keeps an explicit https scheme`() {
    assertThat(normalizeToHttpsScheme("https://radio.example.com"))
      .isEqualTo("https://radio.example.com")
  }

  @Test
  fun `keeps an explicit http scheme`() {
    // The user's explicit choice is preserved at this stage; stream URLs are
    // upgraded later via fixHttps.
    assertThat(normalizeToHttpsScheme("http://radio.example.com"))
      .isEqualTo("http://radio.example.com")
  }

  @Test
  fun `keeps host with port`() {
    assertThat(normalizeToHttpsScheme("radio.example.com:8080"))
      .isEqualTo("https://radio.example.com:8080")
  }

  @Test
  fun `scheme match is case-sensitive`() {
    // "HTTP://…" is not recognized as a scheme and gets prefixed — documents
    // current behavior (the URI parse downstream then fails validation).
    assertThat(normalizeToHttpsScheme("HTTP://radio.example.com"))
      .isEqualTo("https://HTTP://radio.example.com")
  }

  @Test
  fun `empty input gets the prefix`() {
    assertThat(normalizeToHttpsScheme("")).isEqualTo("https://")
  }
}
