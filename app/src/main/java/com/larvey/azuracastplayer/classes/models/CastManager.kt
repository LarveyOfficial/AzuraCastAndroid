package com.larvey.azuracastplayer.classes.models

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.media3.common.Player
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.larvey.azuracastplayer.session.cast.CastRadioPlayer
import com.larvey.azuracastplayer.session.cast.CastRemotePlaybackState
import com.larvey.azuracastplayer.session.cast.CastStationInfo
import com.larvey.azuracastplayer.utils.fixHttps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.util.Locale

private const val TAG = "CastManager"

/** Target size for the downscaled artwork sent to the Cast receiver. */
private const val RECEIVER_ART_SIZE = 384

/**
 * App-wide singleton that owns everything about casting a radio station to a
 * Chromecast / Cast device. It is part of the same shared-state bus as
 * [NowPlayingData] / [SharedMediaController].
 *
 * ## The "muted-local shadow" model
 * The local ExoPlayer stays the single source of truth for the whole app UI and
 * is never restructured. While casting we just mute it ([Player.setVolume] 0),
 * so audio comes only from the receiver, and the app's progress bar / metadata /
 * play-pause icon keep working unchanged. Separately we drive a
 * [CastRadioPlayer] that plays the *same* stream on the receiver with **static
 * live metadata** (station name / format+bitrate / station art, no duration),
 * reloaded only when the station changes — never on a song change.
 *
 * Play/pause is kept in two-way sync between the muted local player and the
 * receiver; [stopCasting] ends the session entirely.
 *
 * Everything here runs on the main thread ([applicationScope] is main-immediate),
 * which is also where the Cast SDK requires its calls to happen.
 */
class CastManager(
  private val context: Context,
  private val applicationScope: CoroutineScope,
  private val sharedMediaController: SharedMediaController,
  private val nowPlayingData: NowPlayingData
) {

  // --- Public, Compose-observable state (read by the cast button + sheet) ---

  /** False when Google Play Services / the Cast SDK is unavailable (hide the UI). */
  val isCastAvailable = mutableStateOf(false)

  /** A remote session is connected and we are casting. */
  val isCasting = mutableStateOf(false)

  /** A session is being established (or torn down). */
  val isConnecting = mutableStateOf(false)

  val castRoutes = mutableStateOf<List<MediaRouter.RouteInfo>>(emptyList())
  val selectedRoute = mutableStateOf<MediaRouter.RouteInfo?>(null)
  val routeVolume = mutableStateOf(0)
  val isRefreshingRoutes = mutableStateOf(false)

  // --- Internals ---

  private var initialized = false
  private var castContext: CastContext? = null
  private var sessionManager: SessionManager? = null
  private val mediaRouter: MediaRouter by lazy { MediaRouter.getInstance(context) }

  private var castSession: CastSession? = null
  private var castRadioPlayer: CastRadioPlayer? = null

  /**
   * When we push a receiver-driven play/pause onto the local player, suppress the
   * local→receiver mirror for a moment so the two-way sync doesn't feed back on
   * itself (Media3 listener callbacks are delivered asynchronously, so a boolean
   * flag set/reset around the call wouldn't cover them).
   */
  private var suppressLocalMirrorUntilMs = 0L

  /** Identity of what is currently loaded on the receiver, to skip song-change reloads. */
  private var lastLoadedKey: String? = null

  private val castSelector: MediaRouteSelector by lazy {
    MediaRouteSelector.Builder()
      .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
      .addControlCategory(
        CastMediaControlIntent.categoryForCast(
          CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
        )
      )
      .build()
  }

  private fun MediaRouter.RouteInfo.isCastRoute(): Boolean =
    supportsControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK) ||
      supportsControlCategory(
        CastMediaControlIntent.categoryForCast(
          CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
        )
      )

  private fun localPlayer(): Player? = sharedMediaController.mediaSession.value?.player

  // --- Lifecycle ---

  /**
   * Called once from [com.larvey.azuracastplayer.session.MusicPlayerService.onCreate]
   * after the media session is published. Registers the Cast session listener,
   * starts passive route discovery, and begins observing the now-playing station
   * so the receiver reloads when the station changes.
   */
  fun initialize() {
    if (initialized) return
    initialized = true

    val manager = try {
      CastContext.getSharedInstance(context).sessionManager
    } catch (e: Exception) {
      Log.i(TAG, "Cast unavailable (no Play Services?): ${e.message}")
      isCastAvailable.value = false
      null
    }
    if (manager == null) return

    castContext = try {
      CastContext.getSharedInstance(context)
    } catch (e: Exception) {
      null
    }
    sessionManager = manager
    isCastAvailable.value = true

    manager.addSessionManagerListener(sessionManagerListener, CastSession::class.java)
    // Reattach if a session already exists (e.g. the service restarted while casting).
    manager.currentCastSession?.let { onCastConnected(it) }

    startDiscovery()
    observeStationForReload()
  }

  private val sessionManagerListener = object : SessionManagerListener<CastSession> {
    override fun onSessionStarting(session: CastSession) {
      isConnecting.value = true
    }

    override fun onSessionStarted(session: CastSession, sessionId: String) {
      onCastConnected(session)
    }

    override fun onSessionResuming(session: CastSession, sessionId: String) {
      isConnecting.value = true
    }

    override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
      onCastConnected(session)
    }

    override fun onSessionStartFailed(session: CastSession, error: Int) {
      isConnecting.value = false
      isCasting.value = false
    }

    override fun onSessionResumeFailed(session: CastSession, error: Int) {
      isConnecting.value = false
      isCasting.value = false
    }

    override fun onSessionSuspended(session: CastSession, reason: Int) {
      isConnecting.value = true
    }

    override fun onSessionEnding(session: CastSession) {}

    override fun onSessionEnded(session: CastSession, error: Int) {
      onCastDisconnected()
    }
  }

  private fun onCastConnected(session: CastSession) {
    castSession = session
    castRadioPlayer = CastRadioPlayer(session)
    session.remoteMediaClient?.registerCallback(remoteCallback)
    localPlayer()?.addListener(localPlayerListener)

    // Silence local audio; the receiver plays the sound. The (muted) local
    // player stays the app's source of truth for the UI.
    localPlayer()?.volume = 0f

    isCasting.value = true
    isConnecting.value = false
    lastLoadedKey = null
    loadCurrentStation()
    session.remoteMediaClient?.requestStatus()
  }

  private fun onCastDisconnected() {
    castSession?.remoteMediaClient?.unregisterCallback(remoteCallback)
    localPlayer()?.removeListener(localPlayerListener)

    // Return audio to the phone.
    localPlayer()?.volume = 1f

    castRadioPlayer = null
    castSession = null
    lastLoadedKey = null
    isCasting.value = false
    isConnecting.value = false
  }

  /** Stops casting completely: ends the session (receiver stops) and stops the local player. */
  fun stopCasting() {
    localPlayer()?.stop()
    sessionManager?.endCurrentSession(true)
  }

  // --- Station load / reload ---

  // The receiver artwork is downscaled on the phone and sent as a small data: URI
  // (the Default Media Receiver spins forever on oversized/redirecting art URLs).
  // Cached per station so a resume/reload is instant.
  private var cachedArtKey: String? = null
  private var cachedArtDataUri: String? = null

  private fun observeStationForReload() {
    applicationScope.launch {
      snapshotFlow { buildStationInfoOrNull() }.collect { info ->
        if (!isCasting.value || info == null) return@collect
        val key = stationKey(info)
        if (key != lastLoadedKey) {
          lastLoadedKey = key
          launchLoad(info)
        }
      }
    }
  }

  private fun loadCurrentStation() {
    val info = buildStationInfoOrNull() ?: return
    lastLoadedKey = stationKey(info)
    launchLoad(info)
  }

  /** Downscale the art (using the pre-warmed cache if available), then load the station. */
  private fun launchLoad(info: CastStationInfo) {
    val key = stationKey(info)
    applicationScope.launch {
      val artDataUri = resolveArtDataUri(info.artUrl, key)
      if (isCasting.value) {
        castRadioPlayer?.loadStation(info.copy(artUrl = artDataUri))
      }
    }
  }

  /**
   * Kick off the (cached) art downscale early — during the connect handshake — so
   * that by the time we load the station on the receiver the small data URI is
   * ready and audio isn't delayed.
   */
  private fun prewarmArt() {
    val info = buildStationInfoOrNull() ?: return
    applicationScope.launch { resolveArtDataUri(info.artUrl, stationKey(info)) }
  }

  private suspend fun resolveArtDataUri(artUrl: String?, key: String): String? {
    val url = artUrl?.takeIf { it.isNotBlank() } ?: return null
    if (key == cachedArtKey) return cachedArtDataUri
    val dataUri = withTimeoutOrNull(4000L) { withContext(Dispatchers.IO) { encodeArt(url) } }
    cachedArtKey = key
    cachedArtDataUri = dataUri
    return dataUri
  }

  private suspend fun encodeArt(url: String): String? {
    return try {
      val request = ImageRequest.Builder(context)
        .allowHardware(false)
        .size(RECEIVER_ART_SIZE, RECEIVER_ART_SIZE)
        .data(url.fixHttps())
        .build()
      val result = ImageLoader(context).execute(request)
      if (result !is SuccessResult) return null
      val bitmap = result.image.toBitmap()
      val out = ByteArrayOutputStream()
      bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
      val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
      // Keep the load message well under the Cast channel size limit.
      if (base64.length > 90_000) null else "data:image/jpeg;base64,$base64"
    } catch (e: Exception) {
      Log.w(TAG, "Failed to prepare Cast artwork: ${e.message}")
      null
    }
  }

  private fun stationKey(info: CastStationInfo): String =
    "${info.streamUrl}|${info.title}|${info.artist}|${info.artUrl}"

  private fun buildStationInfoOrNull(): CastStationInfo? {
    val mount = nowPlayingData.nowPlayingMount.value
    val url = nowPlayingData.nowPlayingURL.value
    val shortCode = nowPlayingData.nowPlayingShortCode.value
    if (mount.isEmpty() || url.isEmpty() || shortCode.isEmpty()) return null

    val station = nowPlayingData.staticData.value?.station
    val stationName = station?.name?.takeIf { it.isNotBlank() } ?: shortCode
    val currentMount = station?.mounts?.find { it.url.fixHttps() == mount }
    val formatBitrate = when {
      mount.lowercase(Locale.ROOT).substringBefore('?').endsWith("m3u8") -> "HLS"
      currentMount?.format != null && currentMount.bitrate != null ->
        "${currentMount.format.uppercase(Locale.ROOT)} ${currentMount.bitrate}kbps"
      else -> ""
    }
    val artUrl = "https://$url/api/station/$shortCode/art/-1"

    return CastStationInfo(
      streamUrl = mount,
      title = stationName,
      artist = formatBitrate,
      artUrl = artUrl
    )
  }

  // --- Two-way play/pause sync ---

  private val localPlayerListener = object : Player.Listener {
    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
      if (!isCasting.value) return
      if (SystemClock.elapsedRealtime() < suppressLocalMirrorUntilMs) return
      if (playWhenReady) {
        // Live radio: resuming must rejoin the live edge, not resume from the
        // stale point where the receiver paused — so reload the stream rather
        // than plain play().
        loadCurrentStation()
      } else {
        castRadioPlayer?.pause()
      }
    }
  }

  private val remoteCallback = object : RemoteMediaClient.Callback() {
    override fun onStatusUpdated() {
      mirrorRemoteToLocal()
    }
  }

  private fun mirrorRemoteToLocal() {
    val client = castSession?.remoteMediaClient ?: return
    val shouldPlay = CastRemotePlaybackState.shouldLocalBePlaying(client.mediaStatus) ?: return
    val player = localPlayer() ?: return
    if (player.playWhenReady == shouldPlay) return
    // Applying a remote-driven change to the local player; don't let the local
    // listener echo it back to the receiver.
    suppressLocalMirrorUntilMs = SystemClock.elapsedRealtime() + 1000
    if (shouldPlay) player.play() else player.pause()
  }

  // --- Route discovery / selection / volume (used by the cast sheet) ---

  private val mediaRouterCallback = object : MediaRouter.Callback() {
    override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) = syncFromRouter(router)
    override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) = syncFromRouter(router)
    override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) = syncFromRouter(router)
    override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) = syncFromRouter(router)
    override fun onRouteUnselected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) = syncFromRouter(router)
    override fun onRouteVolumeChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
      if (route.id == selectedRoute.value?.id) routeVolume.value = route.volume
    }
  }

  /** Passive, low-battery discovery kept running app-wide so the button state is fresh. */
  fun startDiscovery() {
    mediaRouter.addCallback(
      castSelector,
      mediaRouterCallback,
      MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY
    )
    syncFromRouter(mediaRouter)
  }

  private var refreshJob: kotlinx.coroutines.Job? = null

  /**
   * Active discovery while the device sheet is open. This must stay on for the
   * whole time the sheet is visible: once a session connects, the Cast provider
   * stops advertising the *other* devices under passive discovery, so they'd
   * vanish from the list. Active scanning keeps them visible.
   */
  fun beginActiveDiscovery() {
    refreshJob?.cancel()
    mediaRouter.removeCallback(mediaRouterCallback)
    mediaRouter.addCallback(
      castSelector,
      mediaRouterCallback,
      MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY or MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN
    )
    syncFromRouter(mediaRouter)
  }

  /** Sheet closed: drop back to passive discovery to save battery. */
  fun endActiveDiscovery() {
    refreshJob?.cancel()
    isRefreshingRoutes.value = false
    mediaRouter.removeCallback(mediaRouterCallback)
    mediaRouter.addCallback(
      castSelector,
      mediaRouterCallback,
      MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY
    )
    syncFromRouter(mediaRouter)
  }

  /** The sheet's "refresh" affordance: re-arm the active scan and show a brief spinner. */
  fun refreshRoutes() {
    refreshJob?.cancel()
    refreshJob = applicationScope.launch {
      isRefreshingRoutes.value = true
      // Re-arm active scanning (already on while the sheet is open) to force a fresh sweep.
      mediaRouter.removeCallback(mediaRouterCallback)
      mediaRouter.addCallback(
        castSelector,
        mediaRouterCallback,
        MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY or MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN
      )
      syncFromRouter(mediaRouter)
      delay(4000)
      syncFromRouter(mediaRouter)
      isRefreshingRoutes.value = false
    }
  }

  fun selectRoute(route: MediaRouter.RouteInfo) {
    isConnecting.value = true
    // Downscale the artwork now, during the connect handshake, so it's ready by
    // the time we load the station (audio isn't delayed waiting on the image).
    prewarmArt()
    mediaRouter.selectRoute(route)
  }

  /**
   * Disconnect: stop the receiver and return audio to the phone. We end the
   * session with `stopReceiverApplication = true` so playback actually stops on
   * the Cast device (selecting the default route alone leaves the receiver
   * playing, since the framework default is to leave the receiver running).
   */
  fun disconnect() {
    val manager = sessionManager
    if (manager?.currentCastSession != null) {
      manager.endCurrentSession(true)
    } else {
      mediaRouter.selectRoute(mediaRouter.defaultRoute)
      syncFromRouter(mediaRouter)
    }
  }

  /** Ends the session (stopping the receiver) on app teardown. */
  fun endSessionForShutdown() {
    runCatching {
      if (sessionManager?.currentCastSession != null) {
        sessionManager?.endCurrentSession(true)
      }
    }
  }

  fun setRouteVolume(volume: Int) {
    routeVolume.value = volume
    selectedRoute.value?.requestSetVolume(volume)
  }

  private fun syncFromRouter(router: MediaRouter) {
    castRoutes.value = router.routes.filter { it.isCastRoute() }.distinctBy { it.id }
    val selected = router.selectedRoute
    selectedRoute.value = selected
    routeVolume.value = selected.volume
  }
}
