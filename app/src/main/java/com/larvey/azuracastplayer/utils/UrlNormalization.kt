package com.larvey.azuracastplayer.utils

/**
 * Prefixes `https://` onto user-typed station input that has no scheme, so it
 * can be parsed as a URI. Input that already carries an explicit scheme —
 * including `http://` — is returned unchanged (the add-station flow keeps the
 * user's choice at this stage; stream URLs are upgraded later via [fixHttps]).
 */
fun normalizeToHttpsScheme(input: String): String {
  return if (input.startsWith("http://") || input.startsWith("https://")) {
    input
  } else {
    "https://$input"
  }
}
