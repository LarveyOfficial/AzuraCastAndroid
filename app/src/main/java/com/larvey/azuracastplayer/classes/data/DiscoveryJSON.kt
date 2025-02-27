package com.larvey.azuracastplayer.classes.data

data class DiscoveryJSON(
  val lastUpdated: String,
  val featuredStations: DiscoveryCategory,
  val discoveryStations: List<DiscoveryCategory>
)

data class DiscoveryCategory(
  val lastUpdated: String,
  val title: String,
  val description: String,
  val stations: List<DiscoveryStation>
)

data class DiscoveryStation(
  val friendlyName: String,
  val shortCode: String,
  val imageMediaUrl: String,
  val publicPlayerUrl: String,
  val preferredMount: String,
  val description: String,
  val supportsHls: Boolean,
  val hlsUrl: String?
)