package com.larvey.azuracastplayer.session

/**
 * The playback source a media id resolves to. Media ids in the browse tree
 * carry a meaningful prefix (see [MusicPlayerService]'s class documentation):
 * `SAVED_STATION-<mount>` for the user's saved stations, `DISCOVERED-<mount>`
 * for discovery-catalog stations, and a bare mount URL for everything else
 * (treated as a saved station's default mount).
 *
 * In every case [mount] is the stream URL to play, with the prefix stripped.
 */
sealed interface MediaIdRoute {
  val mount: String

  /** A station from the user's saved list ([mount] = its default mount). */
  data class SavedStation(override val mount: String) : MediaIdRoute

  /** A station from the discovery catalog ([mount] = its preferred mount). */
  data class Discovered(override val mount: String) : MediaIdRoute

  /** An unprefixed media id, played as-is. */
  data class Direct(override val mount: String) : MediaIdRoute
}

internal const val SAVED_STATION_PREFIX = "SAVED_STATION-"
internal const val DISCOVERED_PREFIX = "DISCOVERED-"

/** Routes a raw media id to its playback source by prefix. */
fun resolveMediaIdRoute(mediaId: String): MediaIdRoute = when {
  mediaId.startsWith(SAVED_STATION_PREFIX) -> MediaIdRoute.SavedStation(
    mediaId.replaceFirst(
      SAVED_STATION_PREFIX,
      ""
    )
  )

  mediaId.startsWith(DISCOVERED_PREFIX) -> MediaIdRoute.Discovered(
    mediaId.replaceFirst(
      DISCOVERED_PREFIX,
      ""
    )
  )

  else -> MediaIdRoute.Direct(mediaId)
}
