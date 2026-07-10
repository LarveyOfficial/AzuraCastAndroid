package com.larvey.azuracastplayer.session

/**
 * Normalizes a Google Assistant voice query for station matching.
 *
 * Assistant delivers the raw utterance (e.g. "play Chill FM on AzuraCast
 * radio"), so the app strips spaces and every spoken variation of the app
 * name — including the common "azurecast" mis-transcription — before
 * substring-matching against station names. Replacement order matters: the
 * longer "…radio"/"…player" suffixed forms must be stripped before the bare
 * "onazuracast" forms, or a partial strip would leave "radio"/"player"
 * fragments behind.
 */
fun normalizeVoiceQuery(raw: String): String {
  return raw.lowercase()
    .replace(
      " ",
      ""
    )
    .replace(
      "onazuracastradio",
      ""
    )
    .replace(
      "onazurecastradio",
      ""
    )
    .replace(
      "onazuracastplayer",
      ""
    )
    .replace(
      "onazurecastplayer",
      ""
    )
    .replace(
      "onazuracast",
      ""
    )
    .replace(
      "onazurecast",
      ""
    )
}
