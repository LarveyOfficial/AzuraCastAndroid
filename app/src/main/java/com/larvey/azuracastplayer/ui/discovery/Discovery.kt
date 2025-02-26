package com.larvey.azuracastplayer.ui.discovery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Discovery(innerPadding: PaddingValues) {
  val discoveryViewModel: DiscoveryViewModel = viewModel()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(top = innerPadding.calculateTopPadding())
  ) {
    Text(discoveryViewModel.discoveryJSON.value.toString())
  }

}