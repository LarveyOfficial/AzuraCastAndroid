package com.larvey.azuracastplayer.utils

/**
 * Upgrades the first `http://` in the string to `https://`. Applied to
 * essentially every station, mount, and artwork URL before use so Coil cache
 * keys and playback URLs stay consistent for stations that report plain-http
 * URLs. Literal, case-sensitive, first occurrence only (see FixHttpsTest).
 */
fun String.fixHttps(): String {
  return this.replaceFirst(
    "http://",
    "https://"
  )
}