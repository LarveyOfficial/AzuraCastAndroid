package com.larvey.azuracastplayer.ui.discovery

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import kotlinx.coroutines.delay
import java.net.URL
import kotlin.math.absoluteValue

@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalGlideComposeApi::class
)
@Composable
fun Discovery(innerPadding: PaddingValues) {
  val discoveryViewModel: DiscoveryViewModel = viewModel()

  val pagerState = rememberPagerState(pageCount = {
    discoveryViewModel.discoveryJSON.value?.featuredStations?.stations?.size ?: 1
  })
  val pagerIsDragged by pagerState.interactionSource.collectIsDraggedAsState()

  val pageInteractionSource = remember { MutableInteractionSource() }
  val pageIsPressed by pageInteractionSource.collectIsPressedAsState()

  val columnState = rememberLazyListState()

  val autoAdvance = !pagerIsDragged && !pageIsPressed && remember { derivedStateOf { columnState.firstVisibleItemIndex } }.value == 0

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

  val animatedPadding by animateDpAsState(
    targetValue = innerPadding.calculateBottomPadding() + 1.dp,
    animationSpec = tween(durationMillis = 100)
  )

  discoveryViewModel.discoveryJSON.value?.discoveryStations?.let { discoveryCategories ->


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
          Card(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(200.dp)
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
              discoveryViewModel.setPlaybackSource(
                url = URL(discoveryViewModel.discoveryJSON.value?.featuredStations?.stations?.get(station)?.publicPlayerUrl).host
                  ?: "",
                mountURI = discoveryViewModel.discoveryJSON.value?.featuredStations?.stations?.get(station)?.preferredMount
                  ?: "",
                shortCode = discoveryViewModel.discoveryJSON.value?.featuredStations?.stations?.get(station)?.shortCode
                  ?: ""
              )
            }

          ) {
            GlideImage(
              model = discoveryViewModel.discoveryJSON.value?.featuredStations?.stations?.get(station)?.imageMediaUrl,
              contentDescription = "Station Artwork",
              modifier = Modifier
                .requiredWidthIn(min = 1.dp)
                .requiredHeightIn(min = 1.dp)
                .fillMaxWidth(),
              contentScale = ContentScale.FillWidth
            )
          }
        }
      }
      items(
        discoveryCategories,
        key = { it.title }
      ) { category ->
        Text(
          category.title,
          modifier = Modifier.padding(horizontal = 16.dp),
          style = MaterialTheme.typography.titleLarge
        )
        Text(
          category.description,
          modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 4.dp),
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
                  discoveryViewModel.setPlaybackSource(
                    url = URL(station.publicPlayerUrl).host
                      ?: "",
                    mountURI = station.preferredMount,
                    shortCode = station.shortCode
                  )
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
                )
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

}