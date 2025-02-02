package com.larvey.azuracastplayer.ui.nowplaying.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.larvey.azuracastplayer.state.PlayerState

@OptIn(
  ExperimentalSharedTransitionApi::class,
  ExperimentalGlideComposeApi::class
)
@Composable
fun NowPlayingAlbumArt(
  playerState: PlayerState,
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
) {
  AnimatedContent(playerState.mediaMetadata.artworkUri.toString()) { url ->
    with(sharedTransitionScope) {
      GlideImage(
        model = url,
        modifier = Modifier
          .padding(12.dp)
          .aspectRatio(1f)
          .sharedElement(
            rememberSharedContentState(key = "album art"),
            animatedVisibilityScope = animatedVisibilityScope
          )
          .clip(RoundedCornerShape(16.dp)),
        contentDescription = "${playerState.mediaMetadata.albumTitle}",
        transition = CrossFade,
        contentScale = ContentScale.FillBounds
      )
    }
  }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun OtherAlbumArt(
  artURL: String
) {
  AnimatedContent(artURL) { url ->
    GlideImage(
      model = url,
      modifier = Modifier
        .padding(horizontal = 12.dp)
        .aspectRatio(1f)
        .clip(RoundedCornerShape(16.dp)),
      contentDescription = "Album Art",
      transition = CrossFade,
      contentScale = ContentScale.FillBounds
    )
  }
}