package com.larvey.azuracastplayer.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Locks the current behavior of [fixHttps], which is applied to essentially every
 * station, mount, and artwork URL in the app before use. These tests document the
 * exact semantics (a literal, case-sensitive, first-occurrence replacement) so any
 * future change to URL handling is a deliberate decision rather than an accident.
 */
class FixHttpsTest {

  @Test
  fun `upgrades plain http url to https`() {
    assertThat("http://example.com".fixHttps()).isEqualTo("https://example.com")
  }

  @Test
  fun `leaves https url untouched`() {
    assertThat("https://example.com".fixHttps()).isEqualTo("https://example.com")
  }

  @Test
  fun `preserves path query and port`() {
    assertThat("http://radio.example.com:8080/api/nowplaying?x=1".fixHttps())
      .isEqualTo("https://radio.example.com:8080/api/nowplaying?x=1")
  }

  @Test
  fun `replaces only the first occurrence`() {
    // Documents replaceFirst semantics: a second http:// (e.g. inside a query
    // parameter) is intentionally left alone.
    assertThat("http://a.com/redirect?to=http://b.com".fixHttps())
      .isEqualTo("https://a.com/redirect?to=http://b.com")
  }

  @Test
  fun `replacement is not anchored to the start of the string`() {
    // Documents current behavior: the match may occur anywhere in the string,
    // not just as a prefix.
    assertThat("url=http://example.com".fixHttps()).isEqualTo("url=https://example.com")
  }

  @Test
  fun `is case sensitive`() {
    // Uppercase scheme is not matched — documents current (strict) behavior.
    assertThat("HTTP://example.com".fixHttps()).isEqualTo("HTTP://example.com")
  }

  @Test
  fun `leaves scheme-less input untouched`() {
    assertThat("example.com".fixHttps()).isEqualTo("example.com")
  }

  @Test
  fun `empty string stays empty`() {
    assertThat("".fixHttps()).isEqualTo("")
  }
}
