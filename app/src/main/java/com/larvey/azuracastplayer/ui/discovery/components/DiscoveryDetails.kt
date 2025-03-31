package com.larvey.azuracastplayer.ui.discovery.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.placeholder
import com.larvey.azuracastplayer.R
import com.larvey.azuracastplayer.classes.data.DiscoveryStation
import com.larvey.azuracastplayer.ui.discovery.DiscoveryViewModel
import com.larvey.azuracastplayer.ui.nowplaying.components.OtherAlbumArt
import com.larvey.azuracastplayer.ui.nowplaying.components.SongAndArtist
import com.larvey.azuracastplayer.utils.fixHttps
import com.larvey.azuracastplayer.utils.isDark
import java.net.URL

@Composable
fun DiscoveryDetails(animatedStation: DiscoveryStation?, innerPadding: PaddingValues, discoveryViewModel: DiscoveryViewModel) {

  val animatedPadding by animateDpAsState(
    targetValue = innerPadding.calculateBottomPadding() + 1.dp,
    animationSpec = tween(durationMillis = 100)
  )

  Box(
    Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    LazyColumn {
      item {
        Column(
          Modifier
            .padding(horizontal = 16.dp)
        ) {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 256.dp)
              .padding(bottom = 16.dp)
          ) {
            AsyncImage(
              model = ImageRequest.Builder(LocalContext.current)
                .data(animatedStation?.imageMediaUrl?.fixHttps())
                .crossfade(true)
                .placeholderMemoryCacheKey(animatedStation?.imageMediaUrl?.fixHttps())
                .placeholder(
                  if (MaterialTheme.colorScheme.isDark()) {
                    R.drawable.loading_image_dark
                  } else {
                    R.drawable.loading_image
                  }
                )
                .diskCacheKey(animatedStation?.imageMediaUrl?.fixHttps())
                .build(),
              contentDescription = animatedStation?.friendlyName,
              modifier = Modifier
                .fillMaxWidth(),
              contentScale = ContentScale.FillWidth,
              error = if (MaterialTheme.colorScheme.isDark()) {
                painterResource(R.drawable.image_loading_failed_dark)
              } else {
                painterResource(R.drawable.image_loading_failed)
              },
            )
          }
          Text(
            animatedStation?.friendlyName ?: " ",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
          )
          if (animatedStation?.description != "") {
            Text(
              animatedStation?.description ?: " ",
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(bottom = 8.dp)
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
          ) {
            Button(
              onClick = {
                discoveryViewModel.setPlaybackSource(
                  url = URL(animatedStation?.publicPlayerUrl).host
                    ?: "",
                  mountURI = animatedStation?.preferredMount?.fixHttps()
                    ?: "",
                  shortCode = animatedStation?.shortCode ?: ""
                )
              }
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Rounded.PlayArrow,
                  contentDescription = "Play"
                )
                Text("Play Station")
              }
            }
            Button(
              onClick = {
                if (animatedStation != null) {
                  discoveryViewModel.addStation(animatedStation)
                }
              },
              enabled = discoveryViewModel.savedStationsDB.savedStations.value?.none { it.shortcode == animatedStation?.shortCode } != false
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Rounded.Add,
                  contentDescription = "Add"
                )
                Text("Save Station")
              }
            }
          }
          Spacer(modifier = Modifier.size(16.dp))

          val data = discoveryViewModel.nowPlayingData.staticDataMap[Pair(
            animatedStation?.publicPlayerUrl?.toUri()?.host.toString(),
            animatedStation?.shortCode
          )]

          AnimatedVisibility(data != null) {
            DiscoveryNowPlaying(data)
          }
        }
      }
      item { Spacer(Modifier.size(animatedPadding)) }
    }
  }
}