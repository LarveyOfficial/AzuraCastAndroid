package com.larvey.azuracastplayer.ui.mainActivity.components.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(drawerState: DrawerState) {

  val scope = rememberCoroutineScope()

  val maxWidth = LocalConfiguration.current.smallestScreenWidthDp * 0.75f
  Box(modifier = Modifier.width(maxWidth.dp))
  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        navigationIcon = {
          IconButton(onClick = {
            scope.launch {
              drawerState.close()
            }
          }) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = "Back",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        title = {
          Text(
            text = "Settings",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium
          )
        }
      )
    }
  ) { innerPadding ->
    Column(modifier = Modifier.padding(innerPadding)) {
      ThemePicker()
    }
  }
}