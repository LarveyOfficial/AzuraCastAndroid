package com.larvey.azuracastplayer.ui.discovery

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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


  val autoAdvance = !pagerIsDragged && !pageIsPressed

  if (autoAdvance && pagerState.pageCount != 1) {
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


  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(innerPadding)
  ) {
    //    Text(
    //      "Featured Stations",
    //      modifier = Modifier
    //        .padding(horizontal = 16.dp)
    //        .padding(bottom = 8.dp),
    //      style = MaterialTheme.typography.titleLarge
    //    )
    HorizontalPager(
      state = pagerState,
      modifier = Modifier,
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

}