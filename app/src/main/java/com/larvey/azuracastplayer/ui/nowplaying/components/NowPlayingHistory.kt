package com.larvey.azuracastplayer.ui.nowplaying.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.larvey.azuracastplayer.classes.data.PlayingNext
import com.larvey.azuracastplayer.classes.data.SongHistory
import com.larvey.azuracastplayer.state.PlayerState

@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalSharedTransitionApi::class
)
@Composable
fun NowPlayingHistory(
  innerPadding: PaddingValues,
  playerState: PlayerState,
  songHistory: List<SongHistory>?,
  playingNext: PlayingNext?,
  scrollState: LazyListState,
  showQueue: MutableState<Boolean>,
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope
) {
  Column(Modifier.padding(innerPadding)) {
    Spacer(Modifier.padding(top = 16.dp))
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier
        .padding(horizontal = 22.dp)
        .clickable(onClick = {
          showQueue.value = false
        })
        .height(93.5.dp)
    ) {
      NowPlayingAlbumArt(
        playerState = playerState,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
      )
      SongAndArtist(
        songName = playerState.mediaMetadata.title.toString(),
        artistName = playerState.mediaMetadata.artist.toString(),
        small = true
      )
    }
    HorizontalDivider(
      modifier = Modifier
        .padding(
          horizontal = 8.dp,
          vertical = 4.dp
        )
        .padding(top = 4.dp)
        .clip(RoundedCornerShape(16.dp))
    )
    Column(
      modifier = Modifier
        .padding(start = 6.dp)
    ) {
      if (playingNext != null) {
        Text(
          "Up Next",
          modifier = Modifier.padding(
            start = 20.dp,
            top = 4.dp,
            bottom = 4.dp
          ),
          style = MaterialTheme.typography.labelMedium,
          color = Color.White
        )
        Row(
          modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .height(67.5.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OtherAlbumArt(playingNext.song.art)
          SongAndArtist(
            songName = playingNext.song.title,
            artistName = playingNext.song.artist,
            small = true
          )
        }
      }
      if (!songHistory.isNullOrEmpty()) {
        Text(
          "Song History",
          modifier = Modifier.padding(
            start = 20.dp,
            top = 4.dp,
            bottom = 4.dp
          ),
          style = MaterialTheme.typography.labelMedium,
          color = Color.White
        )
        LazyColumn(
          state = scrollState,
          modifier = Modifier.padding(bottom = 4.dp)
        ) {
          itemsIndexed(songHistory) { _, item ->
            Row(
              modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
                .fillMaxWidth()
                .height(67.5.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              OtherAlbumArt(item.song.art)
              SongAndArtist(
                songName = item.song.title,
                artistName = item.song.artist,
                small = true
              )
            }
          }
        }
      }
    }
  }
}