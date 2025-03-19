package com.larvey.azuracastplayer.ui.nowplaying.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.ExperimentalAnimationSpecApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getDrawable
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.larvey.azuracastplayer.R
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.utils.fixHttps

@OptIn(
  ExperimentalSharedTransitionApi::class,
  ExperimentalGlideComposeApi::class,
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
      GlideImage(
        model = url,
        modifier = modifier
          .padding(12.dp)
          .aspectRatio(1f)
          .sharedElement(
            rememberSharedContentState(key = "album art"),
            animatedVisibilityScope = animatedVisibilityScope,
            boundsTransform = BoundsTransform { initialBounds, targetBounds ->
              spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = 600f
              )
            }
          )
          .clip(RoundedCornerShape(16.dp)),
        contentDescription = "${playerState.mediaMetadata.albumTitle}",
        transition = CrossFade,
        failure =
          if (isSystemInDarkTheme()) {
            placeholder(
              drawable = getDrawable(
                LocalContext.current,
                R.drawable.image_loading_failed_dark
              )
            )
          } else {
            placeholder(
              drawable = getDrawable(
                LocalContext.current,
                R.drawable.image_loading_failed
              )
            )
          },
        loading = if (isSystemInDarkTheme()) {
          placeholder(
            drawable = getDrawable(
              LocalContext.current,
              R.drawable.loading_image_dark
            )
          )
        } else {
          placeholder(
            drawable = getDrawable(
              LocalContext.current,
              R.drawable.loading_image
            )
          )
        },
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
      model = url.fixHttps(),
      modifier = Modifier
        .padding(horizontal = 12.dp)
        .aspectRatio(1f)
        .clip(RoundedCornerShape(16.dp)),
      contentDescription = "Album Art",
      transition = CrossFade,
      failure =
        if (isSystemInDarkTheme()) {
          placeholder(
            drawable = getDrawable(
              LocalContext.current,
              R.drawable.image_loading_failed_dark
            )
          )
        } else {
          placeholder(
            drawable = getDrawable(
              LocalContext.current,
              R.drawable.image_loading_failed
            )
          )
        },
      loading = if (isSystemInDarkTheme()) {
        placeholder(
          drawable = getDrawable(
            LocalContext.current,
            R.drawable.loading_image_dark
          )
        )
      } else {
        placeholder(
          drawable = getDrawable(
            LocalContext.current,
            R.drawable.loading_image
          )
        )
      },
      contentScale = ContentScale.FillBounds
    )
  }
}