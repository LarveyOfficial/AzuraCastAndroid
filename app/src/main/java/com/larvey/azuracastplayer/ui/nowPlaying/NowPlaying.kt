package com.larvey.azuracastplayer.ui.nowPlaying

import android.os.Build
import android.util.Log
import android.view.RoundedCorner
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.larvey.azuracastplayer.classes.data.Mount
import com.larvey.azuracastplayer.classes.data.PlayingNext
import com.larvey.azuracastplayer.classes.data.SongHistory
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.ui.mainActivity.components.meshGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalGlideComposeApi::class,
  ExperimentalSharedTransitionApi::class
)
@Composable
fun NowPlaying(
  hideNowPlaying: () -> Unit,
  playerState: PlayerState?,
  pause: () -> Unit,
  play: () -> Unit,
  stop: () -> Unit,
  currentMount: Mount?,
  songHistory: List<SongHistory>?,
  playingNext: PlayingNext?,
  lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {

  if (playerState?.currentMediaItem == null) hideNowPlaying()

  if (playerState?.currentMediaItem != null) {

    val appContext = LocalContext.current.applicationContext

    var currentProgress by remember { mutableFloatStateOf(0f) }
    var currentPosition by remember { mutableLongStateOf(0) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val transitionA = rememberInfiniteTransition(label = "X")
    val transitionB = rememberInfiniteTransition(label = "Y")

    val defaultColor = BottomSheetDefaults.ContainerColor

    val scope = rememberCoroutineScope()

    var colorList by remember { mutableStateOf(List(9) { defaultColor }) }

    var palette by remember { mutableStateOf<Palette?>(null) }

    var showQueue by remember { mutableStateOf(false) }


    val animateA by transitionA.animateFloat(
      initialValue = 0.3f,
      targetValue = 0.8f,
      animationSpec = infiniteRepeatable(
        animation = tween(22000),
        repeatMode = RepeatMode.Reverse
      ),
      label = "X"
    )

    val animateB by transitionB.animateFloat(
      initialValue = 0.4f,
      targetValue = 0.7f,
      animationSpec = infiniteRepeatable(
        animation = tween(13000),
        repeatMode = RepeatMode.Reverse
      ),
      label = "Y"
    )

    val progressAnimation by animateFloatAsState(
      targetValue = currentProgress,
      animationSpec = tween(
        durationMillis = 1000,
        easing = FastOutLinearInEasing
      )
    )

    LaunchedEffect(lifecycleOwner) {
      updateTime(
        isVisible = playerState.currentMediaItem != null,
        updateProgress = { progress, position ->
          currentProgress = progress
          currentPosition = position
        },
        playerState = playerState
      )
    }

    LaunchedEffect(playerState.mediaMetadata.artworkUri) {
      this.async(Dispatchers.IO) {
        Log.d(
          "DEBUG",
          "Fetching image"
        )
        Glide.with(appContext).asBitmap().load(
          playerState.mediaMetadata.artworkUri.toString()
        ).submit().get().let { bitmap ->
          palette = Palette.from(bitmap).maximumColorCount(24).generate()
          val paletteColors = listOf(
            Color(
              palette?.darkVibrantSwatch?.rgb ?: defaultColor.toArgb()
            ),
            Color(
              palette?.darkMutedSwatch?.rgb ?: defaultColor.toArgb()
            ),
            Color(
              palette?.vibrantSwatch?.rgb ?: defaultColor.toArgb()
            )
          )
          colorList = List(9) { paletteColors.random() }
        }
      }.await()
    }

    val scrollState = rememberLazyListState()

    ModalBottomSheet(
      modifier = Modifier.fillMaxSize(),
      sheetState = sheetState,
      shape = RoundedCornerShape(getRoundedCornerRadius() - 12.dp),
      onDismissRequest = {
        hideNowPlaying()
      },
      dragHandle = {},
      contentWindowInsets = {
        WindowInsets(
          0,
          0,
          0,
          0
        )
      },
      sheetGesturesEnabled = !scrollState.canScrollBackward
    ) {
      BackHandler(
        enabled = true
      ) {
        if (showQueue) {
          showQueue = false
        } else {
          scope.launch {
            sheetState.hide()
            hideNowPlaying()
          }

        }
      }

      Box(
        modifier = Modifier
          .fillMaxSize()
          .meshGradient(
            resolutionX = 5,
            resolutionY = 5,
            points = listOf(
              // @formatter:off
              listOf(
                Offset(0f, 0f) to colorList[0], // No move
                Offset(animateA, 0f) to colorList[1], // Only x moves
                Offset(1f, 0f) to colorList[2], // No move
              ), listOf(
                Offset(0f, animateB) to colorList[3], // Only y moves
                Offset(animateB, 1f - animateA) to colorList[4],
                Offset(1f, 1f - animateA) to colorList[5], // Only y moves
              ), listOf(
                Offset(0f, 1f) to colorList[6], // No move
                Offset(1f - animateB, 1f) to colorList[7], //Only x moves
                Offset(1f, 1f) to colorList[8], // No move
              )
              // @formatter:on
            )
          )
      ) {
        Scaffold(
          modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
          containerColor = Color.Transparent,
          bottomBar = {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Bottom,
              modifier = Modifier.fillMaxHeight(0.27f)
            ) {
              ProgressBar(
                progressAnimation = progressAnimation,
                playerState = playerState,
                currentPosition = currentPosition,
                currentMount = currentMount,
                palette = palette
              )
              Spacer(Modifier.weight(0.05f))
              // Media Controls + Share
              MediaControls(
                sheetState = sheetState,
                stop = stop,
                pause = pause,
                play = play,
                playerState = playerState
              )
              Spacer(Modifier.weight(0.1f))
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp)
              ) {
                IconButton(
                  enabled = false,
                  onClick = {},
                  colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color.White,
                    disabledContentColor = Color.Gray
                  )
                ) {
                  Icon(
                    imageVector = Icons.Rounded.StarBorder,
                    contentDescription = "Favorite",
                    modifier = Modifier.size(48.dp),
                  )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                  enabled = true,
                  onClick = {
                    showQueue = !showQueue
                  },
                  colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color.White,
                    disabledContentColor = Color.Gray
                  )
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = "Queue",
                    modifier = Modifier.size(48.dp)
                  )
                }
              }
            }

          }) { innerPadding ->
          SharedTransitionLayout {
            AnimatedContent(showQueue) { targetState ->
              if (!targetState) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Bottom,
                  modifier = Modifier
                    .padding(innerPadding)
                ) {
                  Spacer(Modifier.weight(0.75f))
                  NowPlayingAlbumArt(
                    playerState = playerState,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent,
                    imageSize = 0.65f
                  )
                  Spacer(Modifier.weight(0.1f))
                  SongAndArtist(
                    songName = playerState.mediaMetadata.title.toString(),
                    artistName = playerState.mediaMetadata.artist.toString(),
                    small = false
                  )
                  Spacer(Modifier.weight(0.1f))
                }
              } else {
                Column(Modifier.padding(innerPadding)) {
                  Spacer(Modifier.padding(top = 16.dp))
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                      .padding(horizontal = 16.dp)
                      .clickable(onClick = {
                        showQueue = false
                      })
                  ) {
                    NowPlayingAlbumArt(
                      playerState = playerState,
                      sharedTransitionScope = this@SharedTransitionLayout,
                      animatedVisibilityScope = this@AnimatedContent,
                      imageSize = 0.12f
                    )
                    SongAndArtist(
                      songName = playerState.mediaMetadata.title.toString(),
                      artistName = playerState.mediaMetadata.artist.toString(),
                      small = true
                    )
                  }
                  Spacer(Modifier.size(4.dp))
                  HorizontalDivider(
                    modifier = Modifier
                      .padding(
                        horizontal = 8.dp,
                        vertical = 4.dp
                      )
                      .padding(top = 4.dp)
                      .clip(RoundedCornerShape(16.dp))
                  )
                  Column(modifier = Modifier.padding(start = 6.dp)) {
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
                        modifier = Modifier.fillMaxHeight(0.723f),
                        state = scrollState
                      ) {
                        itemsIndexed(songHistory) { _, item ->
                          Row(
                            modifier = Modifier
                              .padding(horizontal = 16.dp)
                              .padding(bottom = 8.dp)
                              .fillMaxWidth(),
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
                    if (playingNext != null) {
                      HorizontalDivider(
                        modifier = Modifier
                          .padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                          )
                          .clip(RoundedCornerShape(16.dp))
                      )
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
                          .fillMaxWidth(),
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
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@androidx.annotation.OptIn(UnstableApi::class)
suspend fun updateTime(
  isVisible: Boolean, updateProgress: (Float, Long) -> Unit, playerState: PlayerState?
) {
  while (isVisible) {
    if (playerState?.isPlaying == true) {
      updateProgress(
        playerState.player.currentPosition.toFloat() / playerState.player.mediaMetadata.durationMs!!.toFloat(),
        playerState.player.currentPosition
      )
    }
    delay(1000)
  }
}

@Composable
fun getRoundedCornerRadius(): Dp {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val view = LocalView.current
    val windowInsets = view.rootWindowInsets
    val roundedCorner = windowInsets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
    val radiusPx = roundedCorner?.radius ?: 0
    val density = LocalDensity.current
    return with(density) { radiusPx.toDp() }
  }
  return 0.dp
}


@OptIn(
  ExperimentalSharedTransitionApi::class,
  ExperimentalGlideComposeApi::class
)
@Composable
fun NowPlayingAlbumArt(
  playerState: PlayerState,
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
  imageSize: Float
) {
  AnimatedContent(playerState.mediaMetadata.artworkUri.toString()) { url ->
    with(sharedTransitionScope) {
      GlideImage(
        model = url,
        modifier = Modifier
          .padding(horizontal = 16.dp)
          .fillMaxHeight(imageSize)
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
        .padding(horizontal = 16.dp)
        .fillMaxWidth(0.18f)
        .aspectRatio(1f)
        .clip(RoundedCornerShape(16.dp)),
      contentDescription = "Album Art",
      transition = CrossFade,
      contentScale = ContentScale.FillBounds
    )
  }
}

@Composable
private fun SongAndArtist(
  songName: String,
  artistName: String,
  small: Boolean
) {
  Column(modifier = Modifier.padding(if (small) 4.dp else 16.dp)) {
    Text(
      text = songName,
      modifier = Modifier
        .fillMaxWidth()
        .basicMarquee(iterations = Int.MAX_VALUE)
        .animateContentSize(),
      textAlign = TextAlign.Left,
      style = lerp(
        MaterialTheme.typography.titleMedium,
        MaterialTheme.typography.titleLarge,
        animateFloatAsState(
          if (small) 0f else 1f,
          label = "Song Name"
        ).value
      ),
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      color = Color.White
    )

    Spacer(modifier = Modifier.size(4.dp))

    //Artist Name
    Text(
      text = artistName,
      modifier = Modifier
        .fillMaxWidth()
        .basicMarquee(iterations = Int.MAX_VALUE)
        .animateContentSize(),
      textAlign = TextAlign.Left,
      maxLines = 1,
      style = lerp(
        MaterialTheme.typography.titleSmall,
        MaterialTheme.typography.titleMedium,
        animateFloatAsState(
          if (small) 0f else 1f,
          label = "Artist Name"
        ).value
      ),
      color = Color.White
    )
  }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun ProgressBar(
  progressAnimation: Float,
  playerState: PlayerState,
  currentPosition: Long,
  currentMount: Mount?,
  palette: Palette?
) {
  Column {
    LinearProgressIndicator(
      progress = { progressAnimation },
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .clip(RoundedCornerShape(16.dp)),
      drawStopIndicator = {},
      trackColor = Color(
        palette?.lightVibrantSwatch?.bodyTextColor
          ?: ProgressIndicatorDefaults.linearTrackColor.toArgb()
      ),
      color = Color(
        palette?.lightVibrantSwatch?.rgb
          ?: ProgressIndicatorDefaults.linearColor.toArgb()
      )
    )

    Spacer(modifier = Modifier.size(8.dp))

    // Progress Timestamps
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
    ) {
      val duration = playerState.mediaMetadata.durationMs!!.toDuration(DurationUnit.MILLISECONDS)

      val position = currentPosition.toDuration(DurationUnit.MILLISECONDS)

      val durationString = duration.toComponents { minutes, seconds, _ ->
        String.format(
          Locale.getDefault(),
          "%02d:%02d",
          minutes,
          seconds
        )
      }
      val positionString = position.toComponents { minutes, seconds, _ ->
        String.format(
          Locale.getDefault(),
          "%02d:%02d",
          minutes,
          seconds
        )
      }
      Text(
        positionString,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White
      )
      Spacer(modifier = Modifier.weight(1f))
      SuggestionChip(
        onClick = {},
        label = {
          Text(
            "${currentMount?.format?.uppercase()} ${currentMount?.bitrate}kbps",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
          )
        },
        modifier = Modifier.heightIn(max = 24.dp)
      )
      Spacer(modifier = Modifier.weight(1f))
      Text(
        durationString,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaControls(
  sheetState: SheetState,
  stop: () -> Unit,
  pause: () -> Unit,
  play: () -> Unit,
  playerState: PlayerState
) {
  val scope = rememberCoroutineScope()
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 64.dp)
  ) {

    // Stop Button
    IconButton(
      onClick = {
        scope.launch {
          sheetState.hide()
          stop()
        }
      },
      colors = IconButtonDefaults.iconButtonColors(
        contentColor = Color.White,
        disabledContentColor = Color.Gray
      ),
    ) {
      Icon(
        imageVector = Icons.Rounded.Stop,
        contentDescription = "Stop",
        modifier = Modifier.size(48.dp)
      )
    }

    Spacer(modifier = Modifier.weight(1f))

    // Play/Pause Button
    AnimatedContent(targetState = playerState.isPlaying) { targetState ->
      if (targetState) {
        Icon(
          imageVector = Icons.Rounded.PauseCircle,
          contentDescription = "Pause",
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable {
              pause()
            },
          tint = Color.White
        )
      } else {
        Icon(
          imageVector = Icons.Rounded.PlayCircle,
          contentDescription = "Play",
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable {
              play()
            },
          tint = Color.White
        )
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    //Share Button
    IconButton(
      enabled = false,
      colors = IconButtonDefaults.iconButtonColors(
        contentColor = Color.White,
        disabledContentColor = Color.Gray
      ),
      onClick = {}) {
      Icon(
        imageVector = Icons.Rounded.NightsStay,
        contentDescription = "Share",
        modifier = Modifier.size(32.dp),
      )
    }
  }
}