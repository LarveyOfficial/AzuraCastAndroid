package com.larvey.azuracastplayer.ui.discovery.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.larvey.azuracastplayer.classes.data.StationJSON
import com.larvey.azuracastplayer.ui.nowplaying.components.OtherAlbumArt
import com.larvey.azuracastplayer.ui.nowplaying.components.SongAndArtist

@Composable
fun DiscoveryNowPlaying(data: StationJSON?) {
  Column {
    data?.let {
      HorizontalDivider()
      Text(
        "Now Playing",
        modifier = Modifier.padding(
          start = 8.dp,
          top = 8.dp,
          bottom = 4.dp
        ),
        style = MaterialTheme.typography.labelMedium
      )
      Row(
        modifier = Modifier
          .padding(bottom = 8.dp)
          .fillMaxWidth()
          .height(67.5.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OtherAlbumArt(data.nowPlaying.song.art)
        SongAndArtist(
          songName = data.nowPlaying.song.title,
          artistName = data.nowPlaying.song.artist,
          small = true,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
      data.playingNext?.let {
        Text(
          "Up Next",
          modifier = Modifier.padding(
            start = 8.dp,
            top = 8.dp,
            bottom = 4.dp
          ),
          style = MaterialTheme.typography.labelMedium
        )
        Row(
          modifier = Modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .height(67.5.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OtherAlbumArt(data.playingNext.song.art)
          SongAndArtist(
            songName = data.playingNext.song.title,
            artistName = data.playingNext.song.artist,
            small = true,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
      Text(
        "Song History",
        modifier = Modifier.padding(
          start = 8.dp,
          top = 8.dp,
          bottom = 4.dp
        ),
        style = MaterialTheme.typography.labelMedium
      )
      Column(
        modifier = Modifier.padding(bottom = 4.dp)
      ) {
        data.songHistory.forEach { item ->
          Row(
            modifier = Modifier
              .padding(bottom = 8.dp)
              .fillMaxWidth()
              .height(67.5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OtherAlbumArt(item.song.art)
            SongAndArtist(
              songName = item.song.title,
              artistName = item.song.artist,
              small = true,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }
  }
}