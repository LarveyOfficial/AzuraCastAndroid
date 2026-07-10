package com.larvey.azuracastplayer.session

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Locks the Google Assistant voice-query normalization, including the
 * "azurecast" mis-transcription variants and the replacement ordering
 * (longer "…radio"/"…player" forms strip before the bare forms).
 */
class VoiceSearchQueryTest {

  @Test
  fun `lowercases and strips spaces`() {
    assertThat(normalizeVoiceQuery("Chill FM")).isEqualTo("chillfm")
  }

  @Test
  fun `strips on azuracast suffix`() {
    assertThat(normalizeVoiceQuery("Chill FM on AzuraCast")).isEqualTo("chillfm")
  }

  @Test
  fun `strips on azuracast radio suffix`() {
    assertThat(normalizeVoiceQuery("Chill FM on AzuraCast Radio")).isEqualTo("chillfm")
  }

  @Test
  fun `strips on azuracast player suffix`() {
    assertThat(normalizeVoiceQuery("Chill FM on AzuraCast Player")).isEqualTo("chillfm")
  }

  @Test
  fun `strips the azurecast mis-transcription variants`() {
    assertThat(normalizeVoiceQuery("Chill FM on AzureCast")).isEqualTo("chillfm")
    assertThat(normalizeVoiceQuery("Chill FM on AzureCast Radio")).isEqualTo("chillfm")
    assertThat(normalizeVoiceQuery("Chill FM on AzureCast Player")).isEqualTo("chillfm")
  }

  @Test
  fun `longer suffix forms strip cleanly rather than leaving fragments`() {
    // If the bare "onazuracast" were replaced first, "radio" would remain.
    assertThat(normalizeVoiceQuery("Jazz on AzuraCast Radio")).isEqualTo("jazz")
  }

  @Test
  fun `query without any app-name suffix passes through`() {
    assertThat(normalizeVoiceQuery("Lo-Fi Lounge")).isEqualTo("lo-filounge")
  }

  @Test
  fun `strips every occurrence, not just a trailing one`() {
    // replace() is global — documents current behavior.
    assertThat(normalizeVoiceQuery("on AzuraCast on AzuraCast")).isEqualTo("")
  }

  @Test
  fun `empty input stays empty`() {
    assertThat(normalizeVoiceQuery("")).isEqualTo("")
  }
}
