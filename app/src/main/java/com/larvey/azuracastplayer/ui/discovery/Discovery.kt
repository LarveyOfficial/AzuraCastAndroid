package com.larvey.azuracastplayer.ui.discovery

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.larvey.azuracastplayer.classes.data.DiscoveryCategory
import com.larvey.azuracastplayer.utils.fixHttps
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URL
import kotlin.math.absoluteValue

@OptIn(
  ExperimentalGlideComposeApi::class,
  ExperimentalMaterial3AdaptiveApi::class,
  ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun Discovery(
  innerPadding: PaddingValues,
  discoveryViewingStation: MutableState<Boolean>,
  isWide: Boolean,
) {

  val scope = rememberCoroutineScope()

  val discoveryViewModel: DiscoveryViewModel = viewModel()

  val pagerState = rememberPagerState(pageCount = {
    discoveryViewModel.discoveryJSON.value?.featuredStations?.stations?.size ?: 1
  })
  val pagerIsDragged by pagerState.interactionSource.collectIsDraggedAsState()

  val pageInteractionSource = remember { MutableInteractionSource() }
  val pageIsPressed by pageInteractionSource.collectIsPressedAsState()

  val columnState = rememberLazyListState()
  val systemDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())
  val customDirective = PaneScaffoldDirective(
    maxHorizontalPartitions = systemDirective.maxHorizontalPartitions,
    horizontalPartitionSpacerSize = 0.dp,
    maxVerticalPartitions = systemDirective.maxVerticalPartitions,
    verticalPartitionSpacerSize = systemDirective.verticalPartitionSpacerSize,
    defaultPanePreferredWidth = systemDirective.defaultPanePreferredWidth,
    excludedBounds = systemDirective.excludedBounds
  )
  val navigator = rememberSupportingPaneScaffoldNavigator<String>(scaffoldDirective = customDirective)

  BackHandler(navigator.canNavigateBack()) {
    scope.launch {
      navigator.navigateBack()
    }
  }

  val autoAdvance = !pagerIsDragged && !pageIsPressed && remember { derivedStateOf { columnState.firstVisibleItemIndex } }.value == 0 && !navigator.canNavigateBack()

  if (autoAdvance && discoveryViewModel.discoveryJSON.value != null) {
    LaunchedEffect(
      pagerState,
      pageInteractionSource
    ) {
      while (true) {
        delay(5000)
        val nextPage = (pagerState.currentPage + 1) % pagerState.pageCount
        pagerState.animateScrollToPage(nextPage)
      }
    }
  }

  val timedBoolean = remember { mutableStateOf(false) }
  val animatedPadding by animateDpAsState(
    targetValue = innerPadding.calculateBottomPadding() + 1.dp,
    animationSpec = tween(durationMillis = 100)
  )

  LaunchedEffect(discoveryViewingStation.value) {
    if (isWide) {
      if (discoveryViewingStation.value) {
        if (discoveryViewModel.sharedMediaController.playerState.value?.isPlaying == true) {
          delay(250)
        }
        timedBoolean.value = true
      } else {
        if (navigator.canNavigateBack() && !discoveryViewingStation.value) {
          navigator.navigateBack()
        }
        timedBoolean.value = false
      }
    } else {
      timedBoolean.value = discoveryViewingStation.value
    }
  }

  discoveryViewModel.discoveryJSON.value?.discoveryStations?.let { discoveryCategories ->


    SupportingPaneScaffold(
      directive = navigator.scaffoldDirective,
      value = navigator.scaffoldValue,
      mainPane = {
        AnimatedPane {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .padding(top = innerPadding.calculateTopPadding()),
            state = columnState
          ) {
            item {
              HorizontalPager(
                state = pagerState,
                modifier = Modifier.padding(bottom = 16.dp),
                contentPadding = PaddingValues(horizontal = 32.dp)
              ) { station ->
                val hasColor = discoveryViewModel.featuredPalettes[discoveryViewModel.discoveryJSON.value?.featuredStations?.stations?.get(station)?.imageMediaUrl]?.rgb != null
                val backgroundColor = discoveryViewModel.featuredPalettes[discoveryViewModel.discoveryJSON.value?.featuredStations?.stations?.get(station)?.imageMediaUrl]?.rgb
                val textColor = discoveryViewModel.featuredPalettes[discoveryViewModel.discoveryJSON.value?.featuredStations?.stations?.get(station)?.imageMediaUrl]?.bodyTextColor
                Card(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .wrapContentHeight()
                    .heightIn(max = if (isWide) 350.dp else 250.dp)

                    .graphicsLayer {
                      val pageOffset = (
                          (pagerState.currentPage - station) + pagerState.currentPageOffsetFraction
                          ).absoluteValue
                      alpha = lerp(
                        start = 0.5f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(
                          0f,
                          1f
                        )
                      )
                      scaleY = lerp(
                        start = 0.9f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(
                          0f,
                          1f
                        )
                      )
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                      interactionSource = pageInteractionSource,
                      indication = LocalIndication.current
                    ) {
                      scope.launch {
                        navigator.navigateTo(
                          SupportingPaneScaffoldRole.Supporting,
                          contentKey = discoveryViewModel.discoveryJSON.value?.featuredStations?.stations?.get(station)?.publicPlayerUrl
                        )
                        discoveryViewingStation.value = true
                      }
                    }
                    .background(
                      if (hasColor) {
                        Color(backgroundColor!!)
                      } else {
                        MaterialTheme.colorScheme.surfaceContainer
                      }
                    )

                ) {

                  Box {
                    Column {
                      GlideImage(
                        model = discoveryViewModel.discoveryJSON.value?.featuredStations?.stations?.get(station)?.imageMediaUrl,
                        contentDescription = "Station Artwork",
                        modifier = Modifier
                          .requiredWidthIn(min = 1.dp)
                          .requiredHeightIn(
                            min = if (isWide) 325.dp else 225.dp,
                            max = if (isWide) 325.dp else 225.dp
                          )
                          .fillMaxWidth()
                          .background(MaterialTheme.colorScheme.surfaceContainer),
                        contentScale = ContentScale.Crop,
                        failure = placeholder(
                          ColorPainter(MaterialTheme.colorScheme.surfaceContainer)
                        ),
                        loading = placeholder(
                          ColorPainter(MaterialTheme.colorScheme.surfaceContainer)
                        ),
                      ) {
                        it.diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                      }
                      Box(
                        modifier = Modifier
                          .height(25.dp)
                          .fillMaxWidth()
                          .background(
                            if (hasColor) {
                              Color(backgroundColor!!)
                            } else {
                              MaterialTheme.colorScheme.surfaceContainer
                            }
                          )
                      )
                    }
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f)
                        .align(Alignment.BottomEnd)
                        .offset(y = -(24).dp)
                        .background(
                          brush = Brush.verticalGradient(
                            startY = 25f,
                            colors = listOf(
                              Color.Transparent,
                              if (hasColor) {
                                Color(backgroundColor!!)
                              } else {
                                MaterialTheme.colorScheme.surfaceContainer
                              }
                            )
                          )
                        )

                    )
                    Column(
                      modifier = Modifier
                        .padding(
                          horizontal = 8.dp,
                          vertical = 2.dp
                        )
                        .align(Alignment.BottomStart),
                      verticalArrangement = Arrangement.Top
                    ) {
                      Text(
                        "${discoveryViewModel.discoveryJSON.value?.featuredStations?.stations?.get(station)?.friendlyName}",
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        modifier = Modifier.basicMarquee(Int.MAX_VALUE),
                        color = if (hasColor) {
                          Color(textColor!!)
                        } else {
                          MaterialTheme.colorScheme.onSurface
                        }
                      )
                      val description = if (discoveryViewModel.discoveryJSON.value?.featuredStations?.stations?.get(station)?.description == "") {
                        "No Description Provided"
                      } else {
                        discoveryViewModel.discoveryJSON.value?.featuredStations?.stations?.get(station)?.description
                      }
                      Text(
                        "$description",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        maxLines = 1,
                        color = if (hasColor) {
                          Color(textColor!!)
                        } else {
                          MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier
                          .basicMarquee(Int.MAX_VALUE)
                          .padding(bottom = 2.dp)
                      )
                    }
                  }

                }
              }
            }
            items(
              discoveryCategories,
              key = { it.title }
            ) { category ->
              Text(
                category.title,
                maxLines = 1,
                modifier = Modifier
                  .padding(horizontal = 16.dp)
                  .basicMarquee(Int.MAX_VALUE),
                style = MaterialTheme.typography.titleLarge,

                )
              Text(
                category.description,
                maxLines = 1,
                modifier = Modifier
                  .padding(horizontal = 16.dp)
                  .padding(bottom = 4.dp)
                  .basicMarquee(Int.MAX_VALUE),
                style = MaterialTheme.typography.bodyMedium
              )
              val categoryState = rememberLazyListState()
              LazyRow(
                state = categoryState,
                modifier = Modifier.padding(bottom = 16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
              ) {
                items(
                  category.stations,
                  key = { it.friendlyName }
                ) { station ->
                  Column {
                    Card(
                      modifier = Modifier.size(128.dp),
                      onClick = {

                        scope.launch {
                          navigator.navigateTo(
                            SupportingPaneScaffoldRole.Supporting,
                            contentKey = station.publicPlayerUrl
                          )
                          discoveryViewingStation.value = true
                        }
                      }
                    ) {
                      GlideImage(
                        model = station.imageMediaUrl.replaceFirst(
                          "http://",
                          "https://"
                        ),
                        contentDescription = "Station Artwork",
                        modifier = Modifier
                          .aspectRatio(1f)
                          .fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                      ) {
                        it.diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                      }
                    }
                    Text(
                      station.friendlyName,
                      maxLines = 1,
                      modifier = Modifier
                        .widthIn(max = 128.dp)
                        .basicMarquee(iterations = Int.MAX_VALUE)
                        .align(Alignment.CenterHorizontally)
                    )
                  }

                }
              }
            }
            item {
              Spacer(modifier = Modifier.size(animatedPadding))
            }
          }
        }
      },
      supportingPane = {
        AnimatedVisibility(
          timedBoolean.value && discoveryViewingStation.value,
          enter = if (isWide) slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth * 2 }
          ) else fadeIn(),
          exit = if (isWide) ExitTransition.None else fadeOut()
        ) {
          AnimatedPane(modifier = Modifier.padding(top = innerPadding.calculateTopPadding())) {
            AnimatedContent(
              navigator.currentDestination?.contentKey
            ) { stationPublicUrl ->
              val allCategories = remember { mutableListOf<DiscoveryCategory>() }

              discoveryViewModel.discoveryJSON.value?.featuredStations?.let { allCategories.add(it) }
              discoveryViewModel.discoveryJSON.value?.discoveryStations?.let { allCategories.addAll(it) }

              val station = allCategories.flatMap { it.stations }
                .find { it.publicPlayerUrl == stationPublicUrl }

              AnimatedContent(station) { animatedStation ->
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
                          GlideImage(
                            model = animatedStation?.imageMediaUrl,
                            contentDescription = "Station Artwork",
                            modifier = Modifier
                              .fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                          ) {
                            it.diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                          }
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
                      }
                    }
                    item { Spacer(Modifier.size(animatedPadding)) }
                  }
                }
              }
            }
          }
        }
      }
    )
  }

}