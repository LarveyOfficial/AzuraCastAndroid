package com.larvey.azuracastplayer.ui.nowplaying.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.ExperimentalAnimationSpecApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.placeholder
import com.larvey.azuracastplayer.R
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.utils.fixHttps
import com.larvey.azuracastplayer.utils.isDark

@OptIn(
  ExperimentalSharedTransitionApi::class,
  ExperimentalAnimationSpecApi::class
)
@Composable
fun NowPlayingAlbumArt(
  modifier: Modifier = Modifier,
  playerState: PlayerState,
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
) {
  AnimatedContent(playerState.mediaMetadata.artworkUri.toString()) { url ->
    with(sharedTransitionScope) {
      AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
          .data(url.fixHttps())
          .crossfade(true)
          .placeholderMemoryCacheKey(url.fixHttps())
          .placeholder(
            if (MaterialTheme.colorScheme.isDark()) {
              R.drawable.loading_image_dark
            } else {
              R.drawable.loading_image
            }
          )
          .diskCacheKey(url.fixHttps())
          .build(),
        contentDescription = "${playerState.mediaMetadata.albumTitle}",
        contentScale = ContentScale.FillBounds,
        error = if (MaterialTheme.colorScheme.isDark()) {
          painterResource(R.drawable.image_loading_failed_dark)
        } else {
          painterResource(R.drawable.image_loading_failed)
        },
        modifier = modifier
          .padding(12.dp)
          .aspectRatio(1f)
          .sharedElement(
            rememberSharedContentState(key = url.fixHttps()),
            animatedVisibilityScope = animatedVisibilityScope,
            boundsTransform = BoundsTransform { initialBounds, targetBounds ->
              spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = 600f
              )
            }
          )
          .clip(RoundedCornerShape(16.dp))
      )
    }
  }
}

@Composable
fun OtherAlbumArt(
  artURL: String
) {
  AnimatedContent(artURL) { url ->
    AsyncImage(
      model = ImageRequest.Builder(LocalContext.current)
        .data(url.fixHttps())
        .crossfade(true)
        .placeholderMemoryCacheKey(url.fixHttps())
        .placeholder(
          if (MaterialTheme.colorScheme.isDark()) {
            R.drawable.loading_image_dark
          } else {
            R.drawable.loading_image
          }
        )
        .diskCacheKey(url.fixHttps())
        .build(),
      contentDescription = "Album Art",
      contentScale = ContentScale.FillBounds,
      error = if (MaterialTheme.colorScheme.isDark()) {
        painterResource(R.drawable.image_loading_failed_dark)
      } else {
        painterResource(R.drawable.image_loading_failed)
      },
      modifier = Modifier
        .padding(horizontal = 12.dp)
        .aspectRatio(1f)
        .clip(RoundedCornerShape(16.dp)),
    )
  }
}