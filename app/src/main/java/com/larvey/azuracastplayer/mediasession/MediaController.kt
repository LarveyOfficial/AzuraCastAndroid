package com.larvey.azuracastplayer.mediasession

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

/**
 * A Composable function that provides a managed MediaController instance.
 *
 * @param lifecycle The lifecycle of the owner of this MediaController. Defaults to the lifecycle of the LocalLifecycleOwner.
 * @return A State object containing the MediaController instance. The Composable will automatically re-compose whenever the state changes.
 */
@Composable
fun rememberManagedMediaController(
  lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
  setMediaMetadata: ((Player?) -> Unit)? = null
): State<MediaController?> {
  // Application context is used to prevent memory leaks
  val appContext = LocalContext.current.applicationContext
  val controllerManager = remember { MediaControllerManager.getInstance(appContext) }

  // Observe the lifecycle to initialize and release the MediaController at the appropriate times.
  DisposableEffect(lifecycle) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_START -> controllerManager.initialize(setMediaMetadata)
//        Lifecycle.Event.ON_STOP -> controllerManager.release()
        else -> {}
      }
    }
    lifecycle.addObserver(observer)
    onDispose { lifecycle.removeObserver(observer) }
  }

  return controllerManager.controller
}

/**
 * A Singleton class that manages a MediaController instance.
 *
 * This class observes the Remember lifecycle to release the MediaController when it's no longer needed.
 */
@Stable
internal class MediaControllerManager private constructor(context: Context) : RememberObserver {
  private val appContext = context.applicationContext
  private var factory: ListenableFuture<MediaController>? = null
  var controller = mutableStateOf<MediaController?>(null)
    private set

  init { initialize() }

  /**
   * Initializes the MediaController.
   *
   * If the MediaController has not been built or has been released, this method will build a new one.
   */
  @OptIn(UnstableApi::class)
  internal fun initialize(setMediaMetadata: ((Player?) -> Unit)? = null) {
    if (factory == null || factory?.isDone == true) {
      factory = MediaController.Builder(
        appContext,
        SessionToken(appContext, ComponentName(appContext, MusicPlayerService::class.java))
      ).buildAsync()

    }
    factory?.addListener(
      {
        controller.value = factory?.let { if (it.isDone) it.get() else null }

        controller.value?.addListener(object : Player.Listener {
          override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            super.onMediaMetadataChanged(mediaMetadata)
            if (controller.value?.isPlaying == true) {
              setMediaMetadata?.invoke(
                controller.value
              )
            }
          }
        })
      },
      MoreExecutors.directExecutor()
    )
  }



  /**
   * Releases the MediaController.
   *
   * This method will release the MediaController and set the controller state to null.
   */
  internal fun release() {
    factory?.let {
      MediaController.releaseFuture(it)
      controller.value = null
    }
    factory = null
  }

  // Lifecycle methods for the RememberObserver interface.
  override fun onAbandoned() { release() }
  override fun onForgotten() { release() }
  override fun onRemembered() {}

  companion object {
    @Volatile
    private var instance: MediaControllerManager? = null

    /**
     * Returns the Singleton instance of the MediaControllerManager.
     *
     * @param context The context to use when creating the MediaControllerManager.
     * @return The Singleton instance of the MediaControllerManager.
     */
    fun getInstance(context: Context): MediaControllerManager {
      return instance ?: synchronized(this) {
        instance ?: MediaControllerManager(context).also { instance = it }
      }
    }
  }
}