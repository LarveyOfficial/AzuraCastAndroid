package com.larvey.azuracastplayer.utils

import com.larvey.azuracastplayer.classes.data.Mount

/**
 * Mount formats the app cannot (or chooses not to) play; filtered out of
 * every mount picker. Shared by the add-station grid and the edit-station
 * mount dropdown.
 */
val UNSUPPORTED_MOUNT_FORMATS = listOf(
  "flac",
  "ogg",
  "opus"
)

/** Returns only the mounts the app supports playing. */
fun List<Mount>.supportedMounts(): List<Mount> =
  filterNot { UNSUPPORTED_MOUNT_FORMATS.contains(it.format) }
