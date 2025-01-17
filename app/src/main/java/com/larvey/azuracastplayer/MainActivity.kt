package com.larvey.azuracastplayer

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.room.Room
import com.larvey.azuracastplayer.viewmodels.NowPlayingViewModel
import com.larvey.azuracastplayer.database.SavedStationsDatabase
import com.larvey.azuracastplayer.viewmodels.SavedStationsViewModel
import com.larvey.azuracastplayer.ui.theme.AzuraCastPlayerTheme
import com.larvey.azuracastplayer.views.MyRadios

@Suppress("UNCHECKED_CAST")
class MainActivity : ComponentActivity() {
  private val db by lazy {
    Room.databaseBuilder(
      context = applicationContext,
      klass = SavedStationsDatabase::class.java,
      name = "datamodel.db"
    ).build()
  }
  private val savedStationsViewModel by viewModels<SavedStationsViewModel>(
    factoryProducer = {
      object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return SavedStationsViewModel(db.dao) as T
        }
      }
    }
  )

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val sessionToken = SessionToken(applicationContext, ComponentName(applicationContext, MusicPlayerService::class.java))
      val mediaControllerFuture = MediaController.Builder(applicationContext, sessionToken).buildAsync()

      val nowPlayingViewModel by viewModels<NowPlayingViewModel>(
        factoryProducer = {
          object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
              return NowPlayingViewModel(mediaControllerFuture) as T
            }
          }
        }
      )
      DisposableEffect(Unit) {
        onDispose {
          nowPlayingViewModel.mediaPlayer.release()
          nowPlayingViewModel.staticData.value = null
          nowPlayingViewModel.nowPlayingURL.value = ""
          nowPlayingViewModel.nowPlayingURI.value = ""
          nowPlayingViewModel.nowPlayingShortCode.value = ""
          android.os.Process.killProcess(android.os.Process.myPid())
        }
      }

      AzuraCastPlayerTheme {
        MyRadios(
          savedStationsViewModel,
          nowPlayingViewModel
        )
      }
    }
  }
}
