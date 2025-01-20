package com.larvey.azuracastplayer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.larvey.azuracastplayer.components.AddStationDialog
import com.larvey.azuracastplayer.components.NowPlaying
import com.larvey.azuracastplayer.viewmodels.NowPlayingViewModel
import com.larvey.azuracastplayer.database.SavedStationsDatabase
import com.larvey.azuracastplayer.mediasession.rememberManagedMediaController
import com.larvey.azuracastplayer.state.PlayerState
import com.larvey.azuracastplayer.state.state
import com.larvey.azuracastplayer.viewmodels.SavedStationsViewModel
import com.larvey.azuracastplayer.ui.theme.AzuraCastPlayerTheme
import com.larvey.azuracastplayer.viewmodels.RadioSearchViewModel
import com.larvey.azuracastplayer.views.MyRadios
import kotlinx.coroutines.delay


class MainActivity : ComponentActivity() {
  private val db by lazy {
    Room.databaseBuilder(
      context = applicationContext,
      klass = SavedStationsDatabase::class.java,
      name = "datamodel.db"
    ).build()
  }

  @Suppress("UNCHECKED_CAST")
  val savedStationsViewModel by viewModels<SavedStationsViewModel>(
    factoryProducer = {
      object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return SavedStationsViewModel(db.dao) as T
        }
      }
    }
  )

  @OptIn(ExperimentalMaterial3Api::class,
    ExperimentalGlideComposeApi::class
  )
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AzuraCastPlayerTheme {
        val nowPlayingViewModel: NowPlayingViewModel = viewModel()

        val savedRadioList by savedStationsViewModel.getAllEntries().collectAsState(initial = emptyList())

        var showAddDialog by remember { mutableStateOf(false) }
        var showNowPlaying by remember { mutableStateOf(false) }

        val mediaController by rememberManagedMediaController()

        var playerState: PlayerState? by remember {
          mutableStateOf(mediaController?.state())
        }

        LaunchedEffect(playerState?.mediaMetadata) {
          if (nowPlayingViewModel.nowPlayingURL.value != "") {
            nowPlayingViewModel.setMediaMetadata(
              nowPlayingViewModel.nowPlayingURL.value,
              nowPlayingViewModel.nowPlayingShortCode.value,
              mediaController
            )
          }
        }




        DisposableEffect (key1 = mediaController) {
          mediaController?.run {
            playerState = state()
          }
          onDispose {
            playerState?.dispose()
          }
        }

        Scaffold (
          topBar = {
            TopAppBar(
              colors = topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
              ),
              title = { Text("Radio List") }
            )
          },
          floatingActionButton = {
            FloatingActionButton(
              onClick = {
                showAddDialog = true
              }
            ) {
              Icon(Icons.Rounded.Add, contentDescription = "Add")
            }
          },
          bottomBar = {
            AnimatedVisibility(
              visible = playerState?.currentMediaItem != null,
              enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight * 2 }
              ),
              exit = slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight * 2 }
              )
            ) {
              BottomAppBar () {
                Surface (
                  onClick = {
                  showNowPlaying = true
                  },
                  modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                ) {
                  Row (
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp)
                  ) {
                    AnimatedContent(playerState?.mediaMetadata?.artworkUri.toString()) {
                      GlideImage(
                        model = it,
                        contentDescription = "${playerState?.mediaMetadata?.albumTitle}",
                        modifier = Modifier.fillMaxHeight()
                          .clip(RoundedCornerShape(8.dp)),
                        transition = CrossFade
                      )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Column {
                      Text(
                        text = playerState?.mediaMetadata?.displayTitle.toString(),
                        maxLines = 1,
                        modifier = Modifier
                          .widthIn(max = 256.dp)
                          .basicMarquee(iterations = Int.MAX_VALUE),
                        fontWeight = FontWeight.Bold
                      )
                      Text(
                        text = playerState?.mediaMetadata?.artist.toString(),
                        maxLines = 1,
                        modifier = Modifier
                          .widthIn(max = 256.dp)
                          .basicMarquee(iterations = Int.MAX_VALUE),
                        style = MaterialTheme.typography.labelLarge
                      )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    AnimatedContent(
                      targetState = playerState?.isPlaying,
                      modifier = Modifier.padding(end = 15.dp)
                    ) { targetState ->
                      if (targetState == true) {
                        IconButton (onClick = {
                          mediaController?.pause()
                        }) {
                          Icon(
                            imageVector = Icons.Rounded.Pause,
                            contentDescription = "Pause",
                            modifier = Modifier.size(64.dp)
                          ) }
                      } else {
                        IconButton (onClick = {
                          mediaController?.play()
                        }) {
                          Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier.size(64.dp)
                          ) }
                      }
                    }
                  }
                }
              }
            }
          }
        ) { innerPadding ->

          MyRadios(
            savedRadioList = savedRadioList,
            innerPadding = innerPadding,
            setPlaybackSource = { url, shortCode ->
              nowPlayingViewModel.staticDataMap[Pair(url, shortCode)]?.station?.mounts?.get(0)?.url?.let{
                val uri = Uri.parse(it)
                nowPlayingViewModel.setPlaybackSource(
                  uri = uri,
                  url = url,
                  shortCode = shortCode,
                  mediaPlayer = mediaController
                )
              }
            },
            getStationData = { url, shortCode ->
              nowPlayingViewModel.getStationInformation(url, shortCode)
            },
            staticDataMap = nowPlayingViewModel.staticDataMap
          )
        }

        when {
          showAddDialog -> {
            AddStationDialog(
              hideDialog = { showAddDialog = false },
              addData = savedStationsViewModel::saveStation
            )
          }
          showNowPlaying -> {
            NowPlaying(
              hideNowPlaying = {
                showNowPlaying = false
              },
              playerState = playerState,
              pause = {
                mediaController?.pause()
              },
              play = {
                mediaController?.play()
              }
            )
          }
        }
      }
    }
  }
}
