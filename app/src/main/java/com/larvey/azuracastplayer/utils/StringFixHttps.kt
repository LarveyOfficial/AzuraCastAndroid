package com.larvey.azuracastplayer.utils

fun String.fixHttps(): String {
  return this.replaceFirst(
    "http://",
    "https://"
  )
}