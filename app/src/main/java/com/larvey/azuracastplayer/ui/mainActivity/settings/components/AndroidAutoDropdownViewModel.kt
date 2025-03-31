package com.larvey.azuracastplayer.ui.mainActivity.settings.components

import androidx.lifecycle.ViewModel
import androidx.media3.session.MediaLibraryService
import com.larvey.azuracastplayer.classes.models.SharedMediaController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AndroidAutoDropdownViewModel @Inject constructor(
  val sharedMediaController: SharedMediaController
) : ViewModel() {

  fun updateAndroidAutoLayout() {
    sharedMediaController.mediaSession.value.let { session ->
      session?.notifyChildrenChanged(
        "Stations",
        Int.MAX_VALUE,
        MediaLibraryService.LibraryParams.Builder().build()
      )
      session?.notifyChildrenChanged(
        "/",
        Int.MAX_VALUE,
        MediaLibraryService.LibraryParams.Builder().build()
      )
      session?.notifyChildrenChanged(
        "Discover",
        Int.MAX_VALUE,
        MediaLibraryService.LibraryParams.Builder().build()
      )
    }
  }

}