# How PixelPlayer implements Google Cast (Chromecast)

Research document. This describes, in detail, **how the open-source [PixelPlayer](https://github.com/PixelPlayerHQ/PixelPlayer) Android app (`com.theveloper.pixelplay`) implements casting** — both its **backend** (session handling, device delivery, volume) and its **UI** (the cast button, the device sheet, every style and animation). It was produced by reading PixelPlayer's actual source directly (a fresh clone), cross-checked against Google's official Cast Android-sender documentation.

Every file path below is under `app/src/main/java/com/theveloper/pixelplay/` in the PixelPlayer repo unless stated otherwise. Line numbers are from the clone used for this research and are approximate.

> One framing fact that explains a large part of the design: **PixelPlayer plays local device files.** A Chromecast can only fetch media over HTTP(S), so PixelPlayer runs an **HTTP server on the phone** to expose each local file to the receiver. Much of the backend exists to serve, secure, and transcode those local files. This is called out where relevant.

---

## Table of contents

**Backend**
1. [Architecture at a glance](#1-architecture-at-a-glance)
2. [Dependencies, manifest, permissions](#2-dependencies-manifest-permissions)
3. [Bootstrapping the Cast SDK — CastOptionsProvider](#3-bootstrapping-the-cast-sdk--castoptionsprovider)
4. [Device discovery & routes — CastStateHolder](#4-device-discovery--routes--caststateholder)
5. [Selecting a device & starting a session](#5-selecting-a-device--starting-a-session)
6. [How it casts to a device, part 1: the LAN HTTP server](#6-how-it-casts-to-a-device-part-1-the-lan-http-server)
7. [How it casts to a device, part 2: the CastPlayer wrapper](#7-how-it-casts-to-a-device-part-2-the-castplayer-wrapper)
8. [Local → remote transfer (transferPlayback)](#8-local--remote-transfer-transferplayback)
9. [Remote status → app state (the reconciler)](#9-remote-status--app-state-the-reconciler)
10. [Remote → local transfer (disconnect)](#10-remote--local-transfer-disconnect)
11. [Volume — end to end](#11-volume--end-to-end)
12. [Command dispatch while casting](#12-command-dispatch-while-casting)
13. [Notification, foreground, and the service-side mirror](#13-notification-foreground-and-the-service-side-mirror)
14. [Error handling & watchdogs](#14-error-handling--watchdogs)

**UI**
15. [The cast button](#15-the-cast-button)
16. [Opening the sheet & album-color theming](#16-opening-the-sheet--album-color-theming)
17. [The sheet container](#17-the-sheet-container)
18. [Permission gate](#18-permission-gate)
19. [Layout — the two-tab pager](#19-layout--the-two-tab-pager)
20. [Controls tab — hero card + volume slider](#20-controls-tab--hero-card--volume-slider)
21. [Devices tab — rows, states, the scallop animation](#21-devices-tab--rows-states-the-scallop-animation)
22. [Complete animation reference](#22-complete-animation-reference)

**Appendix**
- [A. Google Cast platform facts used above](#a-google-cast-platform-facts-used-above)

---

# BACKEND

## 1. Architecture at a glance

PixelPlayer does **not** use Media3's `androidx.media3:media3-cast` `CastPlayer`. It talks to the Google Cast SDK's `RemoteMediaClient` directly, through a **hand-written class also named `CastPlayer`** (`data/service/player/CastPlayer.kt`), and it treats casting as a **parallel remote-control plane at the ViewModel layer**. The Media3 `MediaSession` in `MusicService` stays bound to the **local** ExoPlayer for the whole cast session (the local player is *paused*, not swapped out); every transport command re-checks "am I casting?" and branches local-vs-remote.

The moving parts:

| Class | Scope | Role |
|---|---|---|
| `CastOptionsProvider` (`data/service/cast/`) | manifest | Cast SDK bootstrap: receiver app id + notification actions |
| `CastStateHolder` (`presentation/viewmodel/`) | `@Singleton` | State bus (`castSession`, `castPlayer`, `isRemotePlaybackActive`, `isCastConnecting`, `remotePosition`) + owns all `MediaRouter` discovery, `castRoutes`, `selectedRoute`, `routeVolume` |
| `CastRouteStateHolder` (`presentation/viewmodel/`) | `@ViewModelScoped` | Thin route select / disconnect / volume façade |
| `CastTransferStateHolder` (`presentation/viewmodel/`) | `@Singleton` (~1,600 lines) | The brain: `SessionManagerListener`, local→remote transfer, remote-status reconciler, remote→local transfer, watchdogs, HTTP-server lifecycle |
| `CastPlayer` (`data/service/player/`) | plain class, per session | Wraps `CastSession.remoteMediaClient`: `loadQueue`, `play/pause/seek/next/previous/jumpToItem/setRepeatMode`, serialized command queue |
| `CastRemotePlaybackState` (`data/service/cast/`) | object | Pure projection `MediaStatus` → `{isPlaying, playWhenReady, isBuffering}` |
| `CastSyncCoordinator` (`data/service/`) | manual, in `MusicService` | **Service-side** observer: mirrors remote playback into listening-stats + widget/Wear (does not control playback) |
| `MediaFileHttpServerService` (`data/service/http/`) | foreground `Service` (~2,900 lines) | The Ktor HTTP server that serves local files to the receiver |
| `CastSessionSecurity`, `CastAudioMimeUtils`, `IsoBmffAudioCodecDetector` (`data/service/…`) | — | Per-session auth tokens; MIME normalization; MP4 codec sniffing |
| `CastBottomSheet` (`presentation/components/`) | Composable (~2,000 lines) | The device picker + controls sheet |

`CastContext` is fetched lazily, wrapped in `try/catch`, at three independent sites (`CastStateHolder:39`, `CastTransferStateHolder:86`, `CastSyncCoordinator:75`) — each uses the deprecated synchronous `CastContext.getSharedInstance(context)` overload. If Play Services is unavailable it resolves to `null` and cast silently does nothing; there is **no** `GoogleApiAvailability` check.

## 2. Dependencies, manifest, permissions

**Gradle** (`gradle/libs.versions.toml`, `app/build.gradle.kts:295,323`):

```kotlin
implementation("androidx.mediarouter:mediarouter:1.8.1")
implementation("com.google.android.gms:play-services-cast-framework:22.3.1")
// there is NO androidx.media3:media3-cast dependency
```

**Manifest** (`AndroidManifest.xml`):

```xml
<!-- the only Cast-SDK-mandated manifest entry -->
<meta-data
    android:name="com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME"
    android:value="com.theveloper.pixelplay.data.service.cast.CastOptionsProvider" />

<!-- the local-file HTTP server -->
<service
    android:name=".data.service.http.MediaFileHttpServerService"
    android:foregroundServiceType="mediaPlayback"
    android:exported="false" />
```

Cast-relevant permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `NEARBY_WIFI_DEVICES` (Android 13+), `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION` (`maxSdkVersion=30`, legacy Wi-Fi/BT discovery), `WAKE_LOCK`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`. **`CHANGE_WIFI_MULTICAST_STATE` is *not* declared** — Cast discovery goes through the Play Services route provider, which holds its own multicast lock. **`MediaTransferReceiver` is *not* declared**, so the Android-13 system Output Switcher is not integrated.

The app sets `android:usesCleartextTraffic="false"` but overrides it with `android:networkSecurityConfig="@xml/network_security_config"`, whose `<base-config cleartextTrafficPermitted="true">` re-enables cleartext app-wide (plus explicit `127.0.0.1`/`localhost` domain configs) — needed because the phone talks to its own `http://<lan-ip>:<port>` server.

**ProGuard** (`proguard-rules.pro:66-68`) — needed because the OptionsProvider is instantiated reflectively from the manifest string:

```proguard
-keep class com.theveloper.pixelplay.data.service.cast.CastOptionsProvider { *; }
-keep class * implements com.google.android.gms.cast.framework.OptionsProvider
```

There is **no Hilt module** for casting; the cast singletons are plain constructor-injected classes.

## 3. Bootstrapping the Cast SDK — CastOptionsProvider

`data/service/cast/CastOptionsProvider.kt` (the entire file):

```kotlin
class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        val notificationOptions = NotificationOptions.Builder()
            .setActions(
                listOf(
                    MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK,
                    MediaIntentReceiver.ACTION_STOP_CASTING
                ), intArrayOf(0, 1)               // both shown in compact view
            )
            .build()
        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .build()
        return CastOptions.Builder()
            .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
            .setCastMediaOptions(mediaOptions)
            .build()
    }
    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}
```

- **Receiver:** the **Default Media Receiver** (`DEFAULT_MEDIA_RECEIVER_APPLICATION_ID`, Google-hosted, no console registration, no UI styling). There is no custom receiver anywhere.
- **Notification:** because `CastMediaOptions` is set, the **Cast SDK posts its own media notification** during a session (a play/pause toggle + a stop-casting action). This is separate from the Media3 notification — see [§13](#13-notification-foreground-and-the-service-side-mirror).
- Every other `CastOptions` flag keeps its framework default (`resumeSavedSession=true`, `enableReconnectionService=true`, `stopReceiverApplicationWhenEndingSession=false`, `remoteToLocalEnabled` unset).

## 4. Device discovery & routes — CastStateHolder

Discovery uses **androidx `MediaRouter` directly** (not `CastButtonFactory`). In `CastStateHolder.kt`:

```kotlin
val mediaRouter = MediaRouter.getInstance(context)           // main-thread only

private val castControlCategory =
    CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)

fun MediaRouter.RouteInfo.isCastRoute() =
    supportsControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK) ||
    supportsControlCategory(castControlCategory)

fun buildCastRouteSelector() = MediaRouteSelector.Builder()
    .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
    .addControlCategory(castControlCategory)
    .build()
```

Two scan modes:

- **Passive** (`startDiscovery()`, `CastStateHolder:257`): `addCallback(selector, cb, CALLBACK_FLAG_REQUEST_DISCOVERY)`.
- **Active burst** (`refreshRoutes()`, `CastStateHolder:233`): swap to `CALLBACK_FLAG_REQUEST_DISCOVERY or CALLBACK_FLAG_PERFORM_ACTIVE_SCAN`, `delay(1800)`, then drop back to passive. Toggles `isRefreshingRoutes` around it. The `init` block deliberately does nothing ("battery drain").

A single `MediaRouter.Callback` funnels `onRouteAdded/Removed/Changed/Selected/Unselected` into `updateRoutes()` (filter `isCastRoute()`, `distinctBy { it.id }`, **no sorting**) + `syncSelectedRouteFromRouter()`. `onRouteVolumeChanged` updates `routeVolume` only for the selected route (`CastStateHolder:207`).

Exposed as `StateFlow`s: `castRoutes`, `selectedRoute`, `routeVolume`, `isRefreshingRoutes`, plus session state (`castSession`, `isRemotePlaybackActive`, `isCastConnecting`, `remotePosition`, `isRemotelySeeking`).

`disconnect()` selects `mediaRouter.defaultRoute` — it does **not** call `endCurrentSession`; the framework tears the session down in response, which comes back as `onSessionEnded`.

## 5. Selecting a device & starting a session

`CastRouteStateHolder.selectRoute()` (`CastRouteStateHolder:20`):

1. If it's a cast route but `sessionManager == null` → surface "Cast is unavailable right now…" and bail.
2. Detect **switching between two remotes** (already active/connecting, different route) and **retrying a failed same route**; in those cases set `pendingCastRouteId = route.id`, `castConnecting = true`, and `sessionManager.endCurrentSession(true)` (or `disconnect()` for retry) first.
3. If it's a cast route, `castTransferStateHolder.primeHttpServerStart()` — pre-warm the HTTP server in parallel with the handshake.
4. `castStateHolder.selectRoute(route)` → `mediaRouter.selectRoute(route)` → the Cast framework starts a `CastSession`.

Session detection is entirely a `SessionManagerListener<CastSession>` registered in `CastTransferStateHolder.setupListeners()` (`:201`). There is **no** `CastStateListener` and **no** `MediaRouteButton`. The callbacks:

| Callback | Action |
|---|---|
| `onSessionStarting` / `onSessionResuming` | `castConnecting = true` |
| `onSessionStarted` / `onSessionResumed` | `transferPlayback(session)` ([§8](#8-local--remote-transfer-transferplayback)) |
| `onSessionEnded` | `stopServerAndTransferBack()` ([§10](#10-remote--local-transfer-disconnect)) |
| `onSessionSuspended(reason)` | `castConnecting = true` + 12 s recovery timer; if still no `remoteMediaClient`, transfer back |
| `onSessionStartFailed` / `onSessionResumeFailed` | clear pending route, `castConnecting=false`, toast |

On `setupListeners`, if a `currentCastSession` already exists (process re-created while casting) it registers the callbacks and requests status immediately.

## 6. How it casts to a device, part 1: the LAN HTTP server

Because the songs are local files, PixelPlayer streams them to the receiver from a phone-hosted HTTP server. **This whole section is specific to local-file playback.**

`data/service/http/MediaFileHttpServerService.kt` — a foreground `Service` running a **Ktor CIO** server:

```kotlin
server = embeddedServer(CIO, port = serverPort, host = "0.0.0.0") { routing { … } }
```

**Lifecycle** (`onCreate`/`onStartCommand`, `:264`): started via `ACTION_START_SERVER` (with an optional `EXTRA_CAST_DEVICE_IP`), returns `START_STICKY`, posts a foreground notification on channel `"pixelplay_cast_server"`. `onDestroy` (`:2902`) resets state, deletes transcode temp files, and stops Ktor on a background thread (`stop(100, 2000)`). `onBind` returns `null`.

**Published state** — `@Volatile` companion fields the ViewModel layer reads directly (`:155`): `isServerRunning`, `isServerStarting`, `serverAddress` (`"http://<ip>:<port>"`), `serverHostAddress`, `serverPrefixLength`, `lastFailureReason`/`lastFailureMessage` (`FailureReason { NO_NETWORK_ADDRESS, FOREGROUND_START_EXCEPTION, START_EXCEPTION }`).

**Port** (`resolveServerPort`, `:843`): prefers **8080**, scans 8080..8100, else an ephemeral `ServerSocket(0)`.

**LAN IP** (`selectIpAddress`, `:882`): walks `ConnectivityManager.allNetworks`, keeps non-loopback non-link-local IPv4 addresses on Wi-Fi/Ethernet, and **prefers an address on the Cast device's subnet** when a hint is given (active > validated > has-internet > longest prefix). IPv6 is never used.

**Endpoints** (`:385`): `GET/HEAD /health` (loopback-only), `GET/HEAD /song/{songId}`, `GET/HEAD /art/{songId}`.

**Security** (`CastSessionSecurity.kt` + `ensureAuthorizedCastMediaRequest`, `:995`): a per-session **auth token** (16 `SecureRandom` bytes, hex, in the `?auth=` query param), a **song-ID allowlist**, and a soft **client-IP allowlist** (cast device IP + loopback + the server's own LAN IP). The real boundary is token + allowlisted songId — a valid pair is accepted even from a non-allowlisted IP (only logged); an invalid token → `401`, a non-allowlisted song → `404`. The policy is rebuilt on every queue load via `configureCastSessionAccess(allowedSongIds, castDeviceIpHint)`; the token is reused for the server's lifetime. Everything is plain HTTP (no TLS, no CORS headers — the Default Receiver's `<audio>`/`<img>` fetches don't need CORS).

**Transcoding** (`:394` onward): the receiver can't play ALAC and mis-reports FLAC duration, so `/song` probes the codec (`MediaExtractor` + a hand-rolled `IsoBmffAudioCodecDetector` MP4-box parser, because extractors lie) and **transcodes ALAC/FLAC → AAC-ADTS** into a per-song temp-file cache (with `CountDownLatch` coordination) so the receiver can issue byte-range seeks against a known length. `CastAudioMimeUtils` normalizes any raw MIME into the Cast-supported set (`audio/mpeg`, `audio/flac`, `audio/aac`, `audio/mp4`, `audio/wav`, `audio/ogg` + explicit Opus/Vorbis, `audio/webm`). The `contentType` announced to the receiver **must match** what the server actually serves, which is why the client side ([§7](#7-how-it-casts-to-a-device-part-2-the-castplayer-wrapper)) re-runs the same decoder-support checks.

The server lifecycle from the transfer holder: `primeHttpServerStart()` (`CastTransferStateHolder:1203`) fire-and-forgets a start; `ensureHttpServerRunning()` (`:1231`, under a `Mutex`) is authoritative — if the running server's IP isn't on the cast device's subnet it **restarts the service**, then polls up to **~20 s** (`for (i in 0..200) { … delay(100) }`) for `isServerRunning && serverAddress != null`, mapping each `FailureReason` to a user-facing toast.

## 7. How it casts to a device, part 2: the CastPlayer wrapper

`data/service/player/CastPlayer.kt` — a bespoke wrapper over `CastSession.remoteMediaClient` (it does **not** implement Media3's `Player`). Constructor grabs the client once (`:48`). Public API: `loadQueue`, `play`, `pause`, `seek`, `next`, `previous`, `jumpToItem`, `setRepeatMode`, `canSeekCurrentItem`, `release`.

**Serialized command pipeline** (`enqueueRemoteCommand`, `:69`) — all transport commands go through one FIFO `ArrayDeque` drained on a main-looper `Handler`, so they never overlap:

```kotlin
private val queueLoadTimeoutMs = 25000L    // whole queueLoad watchdog
private val commandTimeoutMs   = 3500L     // per-command timeout → retry
private val commandRetryDelayMs = 220L
private val minCommandSpacingMs = 120L     // ≥120 ms between commands
private val seekDebounceMs      = 140L
```

Commands are spaced ≥120 ms apart; each has a 3.5 s timeout that retries up to `retryAttempts` times; `"Invalid Request"` statuses are not retried; after every completion the client calls `requestStatus()` to re-sync. `seek(position)` is debounced 140 ms, and refused entirely for Ogg/Opus/Vorbis items (`canSeekCurrentItem()` false — default-receiver Ogg seeking is unstable). `play`/`pause`/`next`/`previous` use `retryAttempts = 1`; `seek`/`jumpToItem`/`setRepeatMode` use `0`. All `RemoteMediaClient` calls are **main-thread only** (they throw `IllegalStateException` otherwise), which is why the pipeline runs on the main looper.

**Building the load** (`loadQueue`, `:228` → `Song.toMediaQueueItem`, `:478`). Note there is no single-item load — even one song is a 1-item queue. A background `"cast-queue-probe"` thread resolves per-song forced MIME types (the transcode decision), then it posts to Main and calls `client.queueLoad(items, startIndex, repeatMode, startPosition, null)`:

```kotlin
val mediaInfo = MediaInfo.Builder(mediaUrl)                       // mediaUrl = http://<lan-ip>:<port>/song/<id>?v=…&auth=…
    .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)               // local files → BUFFERED
    .setContentType(contentType)                                 // forced MIME or resolveCastContentType()
    .setStreamDuration(streamDuration)                           // song.duration, or MediaInfo.UNKNOWN_DURATION
    .setMetadata(MediaMetadata(MEDIA_TYPE_MUSIC_TRACK).apply {
        putString(KEY_TITLE, title); putString(KEY_ARTIST, displayArtist)
        addImage(WebImage(Uri.parse(artUrl)))                    // artUrl = …/art/<id>?v=…&auth=…
    })
    .build()
return MediaQueueItem.Builder(mediaInfo)
    .setCustomData(JSONObject()
        .put("songId", id)                                       // ← the join key back to the library
        .put("mimeType", contentType)
        .put("durationHintMs", durationHintMs)
        .put("streamRevision", streamRevision))                  // cache-buster (hash + per-load nonce)
    .build()
```

- **`contentId` is the phone's HTTP URL** for that song.
- **`customData.songId`** is the linchpin — every remote-status reader maps queue items back to library `Song`s through it.
- `streamRevision` (`buildCastStreamRevision`, `:526`) is a hash of the song identity + announced MIME + a per-`loadQueue` nonce, put in the `?v=` param so a reload never replays a stale cached representation (the server ignores `v`).
- On success, if `autoPlay == false`, it explicitly calls `client.pause()` because `queueLoad` autostarts by default (`:426`). A 25 s watchdog + a `callbackFired` flag guard against a stuck load and against the late SDK callback firing twice.

`next()`/`previous()` read `mediaStatus.queueItems`, prefer `client.queueJumpToItem(neighbourItemId, 0L, null)`, and fall back to `queueNext/queuePrev`.

## 8. Local → remote transfer (transferPlayback)

`CastTransferStateHolder.transferPlayback(session)` (`:526`), run on the main scope. Exact order:

1. Reset the buffering watchdog; `castConnecting = true`.
2. Resolve the cast device IP (`session.castDevice.inetAddress`) and `ensureHttpServerRunning(hint)`; on failure → `castConnecting=false`, `onDisconnect()`.
3. `dualPlayerEngine.cancelNext()` (kill any local crossfade), read `serverAddress` and the local `masterPlayer`, read the current queue.
4. **Capture local state before pausing** (`:555`): `wasPlaying = isPlaying || playWhenReady`, `currentMediaItemIndex`, `currentPosition`, and map local repeat+shuffle → a Cast queue repeat mode (shuffle → `REPEAT_MODE_REPEAT_ALL_AND_SHUFFLE`).
5. `onSheetVisible()`, then **`localPlayer.pause()`** — the local ExoPlayer is *paused, not stopped*; its timeline stays loaded.
6. Build `CastPlayer(session, contentResolver)`, publish `castSession`, keep `isRemotePlaybackActive = false` (UI stays "connecting").
7. `configureCastSessionAccess(allowedSongIds = queue ids, hint)` → the HTTP-server access policy + token; preflight-HEAD the start song's loopback URL (best-effort — a failure only logs).
8. `loadInitialQueueAttempt()` → `castPlayer.loadQueue(…, startPosition = currentPosition, autoPlay = wasPlaying)`. On failure: **retry once after 450 ms**; on second failure: toast + `sessionManager.endCurrentSession(true)` (avoid a stuck route). On success: snapshot the last-remote fields, register the `RemoteMediaClient.Callback` + a **1000 ms** `ProgressListener`, start the pollers, `launchAlignToTarget(startSongId)`, `isRemotePlaybackActive = true`, `castConnecting = false`.

**Resume semantics:** `autoPlay = wasPlaying` and `startPosition = currentPosition` → the receiver resumes exactly where local playback was.

Post-load **alignment** (`alignRemotePlaybackToSong`, `:1465`): up to 2 attempts (350 ms apart) verifying the receiver's current `songId` matches the target; if not, `jumpToItem`.

Two pollers keep remote state fresh (`startRemoteProgressObserver`, `:685`): the 1000 ms `ProgressListener` feeds `remotePosition`; a status keep-alive loop calls `requestStatus()` every **1500 ms** (playing) / **2500 ms** (active-idle) / **1500 ms** (connecting) / **4000 ms** (else).

## 9. Remote status → app state (the reconciler)

Every `RemoteMediaClient.Callback` (`onStatusUpdated`/`onMetadataUpdated`/`onQueueStatusUpdated`/`onPreloadStatusUpdated`) calls `handleRemoteStatusUpdate()` (`:276`). What it does:

- **Projects playback** via `CastRemotePlaybackState.project(mediaStatus, previousPlayIntent)` (`data/service/cast/CastRemotePlaybackState.kt`, full logic):

  ```kotlin
  val isRecoverableError = playerState == PLAYER_STATE_IDLE &&
      idleReason == IDLE_REASON_ERROR && previousPlayIntent
  val isPlaying = when (playerState) {
      PLAYER_STATE_PLAYING, PLAYER_STATE_BUFFERING -> true
      PLAYER_STATE_IDLE -> isRecoverableError
      else -> previousPlayIntent && playerState == PLAYER_STATE_UNKNOWN
  }
  ```

  `BUFFERING` counts as playing (keeps the pause icon); `IDLE + IDLE_REASON_ERROR` while the user intended playback is treated as **recoverable buffering** so the UI doesn't flicker to paused; `UNKNOWN` preserves the previous intent.

- **Rebuilds the queue** from `mediaStatus.queueItems[].customData.songId` → library map. **Shrunk-subset guard** (`:434`): Cast status updates sometimes report only a window of the queue (current + next), so if the new queue is a strict subset of the last known one it keeps the last full queue.
- **Seek unlock:** while `isRemotelySeeking`, unlocks once `|streamPosition − expected| ≤ 1500 ms` (`:302`).
- **Pending-song reconciliation** (`:307`): after a user jump/load the target `songId` is "pending" for **4000 ms**; a receiver report of a *different* song during that window is ignored (with one forced `jumpToItem` retry after ~700 ms), accepting remote truth only after ~3.5 s or ≥12 mismatched polls. Stops the UI flip-flopping to the previous song.
- **Idle / error logging** (`:358`, `:380`): detects a `prematureFinish` (`IDLE_REASON_FINISHED` reported >3 s early — a transcode/duration symptom); on `IDLE_REASON_ERROR` it schedules a transfer-back-to-local ([§14](#14-error-handling--watchdogs)).
- **Mirrors into the UI player state** (`:490`): `currentSong`, `isPlaying`/`playWhenReady`/`isBuffering` from the projection, `totalDuration` (remote `streamDuration` → song → previous), repeat mapping (`REPEAT_SINGLE→ONE`, `REPEAT_ALL|ALL_AND_SHUFFLE→ALL`), `isShuffleEnabled = queueRepeatMode == REPEAT_ALL_AND_SHUFFLE`. On song change it fires `onSongChanged(albumArtUri)` (theme/lyrics refresh). **While casting, the app's "local" stable player state that the whole UI renders is overwritten from the remote `MediaStatus`.**

## 10. Remote → local transfer (disconnect)

`stopServerAndTransferBack()` (`:1059`), on `onSessionEnded` (any cause: user disconnect, error recovery, suspend timeout):

1. Cancel every job (recovery/align/progress/status/buffering/error); unregister the progress listener + callback.
2. Snapshot final state, preferring the live `mediaStatus` over the cached one: `lastPosition`, `wasPlaying`, `lastItemId`, repeat/shuffle. Store in a `TransferSnapshot`.
3. Null out `castPlayer`/`castSession`, `isRemotePlaybackActive = false`.
4. If `pendingCastRouteId == null` (not a device switch): `stopService(MediaFileHttpServerService)` + `onDisconnect()`. Otherwise keep the server up and stay "connecting."
5. Unless a one-shot `skipTransferBackOnNextSessionEnd` flag is set (mini-player swipe-dismiss shouldn't resurrect playback locally): on `Dispatchers.Default` rebuild the local queue from the remote queue's `songId`s (fallback `lastRemoteQueue`), find the target song by `lastItemId`, then on a **freshly re-fetched** `masterPlayer` (dodging a released-reference race): `shuffleModeEnabled`, `repeatMode`, `setMediaItems(items, startIndex, lastPosition)`, `prepare()`, `play()`/`pause()` per `wasPlaying`, and sync the UI.

**Resume semantics:** if the receiver was playing, the local player resumes the same content at the last remote position; else it sits paused there.

## 11. Volume — end to end

This is how PixelPlayer "interfaces with the phone when increasing/decreasing volume" while casting. There are exactly two paths, and one of them is *no app code at all*.

**Hardware volume buttons → handled automatically by the Cast framework.** PixelPlayer has **no** `VolumeProvider`, **no** `dispatchKeyEvent`/`onKeyDown` override, **no** `setVolumeControlStream`, and **no** `KEYCODE_VOLUME` handling anywhere (verified across the whole source). Google's docs confirm this is correct: *"the physical buttons on the sender device can be used to change the volume of the Cast session on the Web Receiver by default for any device using Jelly Bean or newer"* and *"The framework automatically manages the volume for the sender app … so that the sender UI always reports the volume specified by the Web Receiver."* So while a session is active, VOLUME_UP/DOWN go to the **receiver's** volume with zero code; the phone's own media stream is not what the keys act on.

**The in-app slider → MediaRouter route volume.** The only explicit volume code is the slider in `CastBottomSheet`, which routes through `PlayerViewModel.setRouteVolume` → `CastRouteStateHolder.setRouteVolume` → `CastStateHolder.setRouteVolume` (`:278`):

```kotlin
fun setRouteVolume(volume: Int) {
    _routeVolume.value = volume                    // optimistic echo
    _selectedRoute.value?.requestSetVolume(volume) // androidx MediaRouter.RouteInfo.requestSetVolume(int)
}
```

i.e. `RouteInfo.requestSetVolume(int)` in integer units up to `route.volumeMax` (Cast routes typically report a max of ~20–25). This is *not* `RemoteMediaClient.setStreamVolume` and *not* `CastSession.setVolume` — those are never called.

**Feedback loop:** volume changes from the receiver (or another sender / the physical remote) come back through `MediaRouter.Callback.onRouteVolumeChanged` (`CastStateHolder:207`), which updates `routeVolume` for the selected route, so the slider stays in sync. When the session is selected, `syncSelectedRouteFromRouter` seeds `routeVolume` from `route.volume`.

**Local (not casting):** the same slider instead calls `PlayerViewModel.setTrackVolume` (`:924`) → `mediaController.volume = clamped(0f..1f)` — the ExoPlayer track volume, unrelated to Cast.

The slider is continuous (every drag frame calls `onVolumeChange`); the only quantization is haptic — see [§20](#20-controls-tab--hero-card--volume-slider).

## 12. Command dispatch while casting

There is no polymorphic player. Every transport command in `PlaybackStateHolder.kt` re-checks the same predicate and branches (`castStateHolder.castSession.value?.remoteMediaClient != null`):

- **`playPause()`** (`:396`): remote → project state; if playing → `castPlayer.pause()`; else if `mediaQueue.itemCount > 0` → `castPlayer.play()`; optimistic UI update either way. Local → `MediaController.pause()`/`play()` (with `prepare()` if idle).
- **`seekTo()`** (`:442`): remote → `castPlayer.canSeekCurrentItem()` first; if blocked (Ogg family) → a rate-limited toast (`CAST_SEEK_BLOCKED_TOAST_COOLDOWN_MS = 2500L`) + `requestStatus`. Otherwise set `isRemotelySeeking=true`, optimistic position, `castPlayer.seek()`, and arm an **1800 ms** fail-safe that clears the seek lock.
- **`nextSong()`/`previousSong()`** (`:503`,`:489`): remote → `castPlayer.next()`/`.previous()`. (Local `previous` restarts if position > 10 s; there's no such rule on cast.)
- **`cycleRepeatMode()`/`setRepeatMode()`** (`:512`,`:549`): remote → `castPlayer.setRepeatMode()`.
- **`toggleShuffle()`** (`:921`): remote shuffle is expressed purely as `REPEAT_MODE_REPEAT_ALL_AND_SHUFFLE` toggled against `REPEAT_ALL` (Cast has no separate shuffle flag).
- **Playing a new song** while casting is handled by `CastTransferStateHolder`: `markPendingRemoteSong(song)` + `castPlayer.jumpToItem` when the target is already in the remote queue, else `playRemoteQueue(...)` — a fresh `loadQueue` at position 0 (`:1316`). `markPendingRemoteSong` optimistically flips the UI to the new song and shields it for 4 s (see [§9](#9-remote-status--app-state-the-reconciler)).

Meanwhile local ExoPlayer events are **suppressed** so the paused local player can't clobber the remote-driven UI: `MediaControllerSyncStateHolder.isRemoteSessionControllingPlayback()` (`:385`) early-returns from `onIsPlayingChanged`, `onPlayWhenReadyChanged`, `onPlaybackStateChanged`, `onMediaItemTransition`, `onTimelineChanged`, `onTracksChanged`.

## 13. Notification, foreground, and the service-side mirror

The Media3 `MediaSession` in `MusicService` **keeps the local player** for the whole cast session; nothing swaps a cast player in. Because `transferPlayback` paused the local player, `Player.hasForegroundPlaybackIntent()` (`MusicService:1067`) is false, so `onUpdateNotification` (`:2401`) lets Media3 drop the foreground/notification. The remote playback is represented by the **Cast SDK's own notification** (from the `CastMediaOptions` in [§3](#3-bootstrapping-the-cast-sdk--castoptionsprovider)).

To keep that path from crashing, `MusicService` installs a main-thread `UncaughtExceptionHandler` (`:407`) that swallows `ForegroundServiceStartNotAllowedException` thrown from the Media3/Cast internal notification path (which calls `startForeground()` directly), and wraps `onUpdateNotification` in try/catch.

`CastSyncCoordinator` (`data/service/CastSyncCoordinator.kt`) is a **second, independent** observer, constructed manually in `MusicService` and started 1 s after service creation. It registers its own `SessionManagerListener` + `RemoteMediaClient.Callback` and **only reads** remote status to:

- mirror playback into `ListeningStatsTracker` (play counts), keyed by the queue `itemId`/`songId`, and
- project a `RemotePlaybackSnapshot` (`resolveRemoteSnapshot()`, `:211`) for the **home-screen widget + Wear** surfaces — so those show *cast* playback (title/artist/art/position from `MediaStatus`) even though the local ExoPlayer is paused.

It shares nothing with the ViewModel-layer observer except the `customData.songId` convention and the `CastRemotePlaybackState` projection.

## 14. Error handling & watchdogs

`CastTransferStateHolder` runs a **buffering watchdog** (`updateRemoteBufferingWatchdog`, `:756`) while the remote is `PLAYER_STATE_BUFFERING`:

```kotlin
remoteBufferingSoftRecoveryMs = 6_000L
remoteBufferingReloadMs       = 14_000L
remoteBufferingTransferBackMs = 28_000L
```

If position hasn't moved ≥750 ms: at **6 s** soft-recover (`requestStatus` + `castPlayer.play()`); at **14 s** (once) reload the current item via a fresh `loadQueue` at the stalled position; at **28 s** give up → `scheduleCastSessionTransferBack` ("Cast stayed stuck loading. Resuming on this device.") which ends the session → falls back to the phone. Separately: `IDLE_REASON_ERROR` schedules the same transfer-back; `onSessionSuspended` gives a 12 s recovery window; the initial `loadQueue` failure retries once then ends the session; a `playRemoteQueue` failure keeps the session alive and rolls back the optimistic UI song.

---

# UI

All UI paths below are under `presentation/`. The cast UI is two pieces: the **cast button** in the full player, and the **`CastBottomSheet`** (device picker + controls).

## 15. The cast button

Lives in the full player's `TopAppBar` actions slot (`components/player/FullPlayerContent.kt:758`). There is **no** cast button in the mini player. Its colours come from the album-art-derived scheme (`playerAccentColor = primary`, `playerOnAccentColor = onPrimary`).

**Three icon states** (`:758`):

```kotlin
val showCastLabel = isCastConnecting || (isRemotePlaybackActive && selectedRouteName != null)
val castIconPainter = when {
    isCastConnecting || isRemotePlaybackActive -> R.drawable.rounded_cast_24       // connecting / connected
    isBluetoothActive                          -> R.drawable.rounded_bluetooth_24  // BT audio active locally
    else                                       -> R.drawable.rounded_mobile_speaker_24 // idle = phone speaker
}
```

So the idle glyph is a **phone-speaker**, not a cast icon; the cast glyph appears only while connecting/connected; a Bluetooth glyph takes precedence when BT audio is active locally.

**Morphing pill** (`:779`):

- `Box` `height(42.dp)`, `.animateContentSize(spring(dampingRatio = MediumBouncy, stiffness = StiffnessLow))`, `.widthIn(min = 50.dp, max = if (showCastLabel) 190.dp else 58.dp)`, background `playerOnAccentColor.copy(alpha = 0.7f)`.
- Corner morph: left corners fixed at `50.dp`; right corners `animateDpAsState(if (showCastLabel) 50.dp else 6.dp, spring(MediumBouncy, StiffnessLow))` — idle it's an asymmetric pill (round left, 6 dp right), connected it becomes a full stadium that grows to fit the label.
- Label (`:820`): `AnimatedVisibility(showCastLabel)` → `AnimatedContent` (`fadeIn(tween(150)) togetherWith fadeOut(tween(120))`) showing "Connecting…" (+ a 14 dp `CircularProgressIndicator`, 2 dp stroke) or the route name (+ an 8 dp `onTertiaryContainer` "live" dot), `typography.labelMedium`, tinted `playerAccentColor`.

While casting, the top-bar title "Now Playing" is hidden (`if (!isCastConnecting) AnimatedVisibility(visible = !isRemotePlaybackActive)`, `:701`).

## 16. Opening the sheet & album-color theming

`components/scoped/CastSheetState.kt` — a `showCastSheet` boolean plus a derived open fraction:

```kotlin
val castSheetOpenFraction by animateFloatAsState(
    targetValue = if (showCastSheet) 1f else 0f,
    animationSpec = tween(220, easing = FastOutSlowInEasing)
)
```

`openCastSheet`/`dismissCastSheet` just flip the boolean; `onCastExpansionChanged` is a remembered **no-op** (the fraction is driven by the boolean, not by the sheet callback). The fraction dims/scales the player behind it and suspends its realtime updates.

**The whole sheet is themed with the album-art colors.** `UnifiedPlayerCastLayer` (`components/UnifiedPlayerOverlaysLayer.kt:478`) wraps `CastBottomSheet` in `MaterialTheme(colorScheme = albumColorScheme)`, so every `MaterialTheme.colorScheme.*` inside the sheet resolves to album-derived tones (and is unmounted while the keyboard is visible).

## 17. The sheet container

`CastBottomSheet.kt:346` — a **stock Material3 `ModalBottomSheet`**:

```kotlin
val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { true })
ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    dragHandle = { BottomSheetDefaults.DragHandle() },
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    tonalElevation = 12.dp
) {
    CompositionLocalProvider(LocalOverscrollFactory provides null) {  // kills overscroll stretch inside
        Box(Modifier.padding(bottom = 18.dp)) { … }
    }
}
```

The content is either `CastPermissionStep` (permissions missing) or `CastSheetContent`.

> The same file also contains ~350 lines of **dead** legacy code (`CastSheetContainer`, `CollapsibleCastTopBar`, `ScanningIndicator`) — a hand-rolled drag/nested-scroll sheet superseded by this stock `ModalBottomSheet`. It's not wired up.

## 18. Permission gate

Required permissions are built per SDK (`:185`): API ≥ 31 → `BLUETOOTH_CONNECT` + `BLUETOOTH_SCAN`; API ≥ 33 → `NEARBY_WIFI_DEVICES`; API ≤ 30 → `ACCESS_FINE_LOCATION`. If missing, `CastPermissionStep` (`:458`) shows an explainer: a title (`titleLarge` Bold), a `Card(shape = RoundedCornerShape(26.dp), containerColor = surfaceContainerHigh)` with two `PermissionHighlight` rows (a 42 dp circle icon in `primary.copy(alpha = 0.12f)`, `titleSmall`/`bodySmall`), and a full-width "Allow access" `Button(shape = RoundedCornerShape(50))` that launches `RequestMultiplePermissions`.

## 19. Layout — the two-tab pager

`CastSheetContent` (`:568`):

- A title chip "Connect device" (`headlineMedium` SemiBold, inside a `CircleShape` `surfaceContainerLow` box), and an `AnimatedVisibility` "Scanning nearby" `BadgeChip` (`fadeIn tween(180)` / `fadeOut tween(160)`, primary-tinted).
- A `HorizontalPager` of 2 pages — **CONTROLS** (page 0) and **DEVICES** (page 1) — inside a `Box` capped at `maxPagerHeight = screenHeight − insets − 212.dp` (min 280 dp) with `animateContentSize(tween(280, FastOutSlowInEasing))`, suppressed for the first two frames (`withFrameNanos` ×2) to avoid an opening jump. Initial page is Controls when a remote session exists, else Devices.
- A `PrimaryTabRow` styled as a pill: `clip(CircleShape).background(surfaceContainerHigh).padding(5.dp)`, transparent container, empty divider/indicator. Each tab is a `TabAnimation` with an icon (`Icons.Rounded.Speaker` / `Icons.Filled.Devices`) + a hardcoded label ("CONTROLS"/"DEVICES", `GoogleSansRounded` Bold); tapping animates the pager to that page.

## 20. Controls tab — hero card + volume slider

`ActiveDeviceHero` (`:1274`):

- **Card:** `shape = AbsoluteSmoothCornerShape(42/20/20/42 dp, smoothness 70%)` (an asymmetric squircle from the `smooth_corner_rect_library`), `containerColor = tertiaryContainer`, `elevation = 6.dp`, inner `padding(20.dp)`, `spacedBy(16.dp)`.
- **Top row:** a 62 dp leading icon area — a `matchParentSize` circle in `onTertiaryContainer.copy(alpha = if (isConnecting) 0.18f else 0.12f)`; while connecting a 34 dp `CircularProgressIndicator` (stroke 4 dp, `strokeCap = Round`), else the device `Icon` tinted `onTertiaryContainer`. A weight-1 text column: title (`titleLarge` Bold, max 2 lines), a status line `"$subtitle • $connectionLabel"` (or "Connecting..." while a remote connect is in flight), and — when remote — a **Disconnect** `Button(shape = RoundedCornerShape(50), containerColor = surfaceContainerLow, contentColor = onErrorContainer, height 46.dp)` with the `rounded_mimo_disconnect_24` icon.
- Device-type → icon map (also used in rows): `DEVICE_TYPE_TV → Rounded.Tv`, `DEVICE_TYPE_REMOTE_SPEAKER`/`BUILTIN_SPEAKER → Rounded.Speaker`, `DEVICE_TYPE_BLUETOOTH_A2DP → Rounded.Bluetooth`, else `Filled.Cast`.

- **Volume section** (`:1407`): a header row with a label ("Device volume" remote / "Phone volume" local) and a live value (`"${(v*100).toInt()}%"` for the 0..1 local range, else `"${v.toInt()} / ${max.toInt()}"`). The `Slider`:
  - value range `0f..volumeMax` (remote) or `0f..1f` (local); local echo state snaps to external changes via `LaunchedEffect(device.volume)`.
  - **custom track** `SliderDefaults.Track(height(30.dp).clip(RoundedCornerShape(12.dp)), activeTrackColor = onTertiaryContainer)`, **custom thumb** `SliderDefaults.Thumb(height(36.dp), thumbColor = onTertiaryContainer)`.
  - `onValueChange` forwards the raw float **every drag frame** (continuous, no debounce); it also quantizes to 20 steps (0..1) or per-integer and fires `HapticFeedbackType.TextHandleMove` on each step change.

## 21. Devices tab — rows, states, the scallop animation

`CastDevicesTabContent` is a `LazyColumn` (16 dp spacing) with a section header + refresh, a fade `LinearProgressIndicator` while refreshing, then one of:

- **Scanning + no devices:** `ScanningPlaceholderList` — 3 `ShimmerBox` (68 dp tall, `RoundedCornerShape(topStart 28, topEnd 4, bottomEnd 28, bottomStart 4)`), infinite gradient sweep `tween(1000, delay 200)`.
- **Empty:** `EmptyDeviceState` card (`RoundedCornerShape(22.dp)`, "Searching for devices…").
- **List:** `CastDeviceRow`s.

**Device model** (`:235`): Wi-Fi routes first (when Wi-Fi is on), then Bluetooth A2DP devices appended (BT rows are synthetic ids `"bluetooth_…"` that just deep-link to `Settings.ACTION_BLUETOOTH_SETTINGS`). Route connection state is **normalized** (`:240`): the active route while connecting → `CONNECTING`; active + remote session → `CONNECTED`; a route reporting `CONNECTED` that isn't the app's active one is downgraded to `DISCONNECTED`.

**`CastDeviceRow`** (`:1519`):

- A full stadium pill: `Surface(clip(CircleShape), tonalElevation = 2.dp)`, colours by state — `primaryContainer`/`onPrimaryContainer` (selected), `secondaryContainer` (bluetooth), else `surfaceVariant`/`onSurface`.
- **The active-device flourish:** when `isSelected && CONNECTION_STATE_CONNECTED`, the 52 dp leading icon gets a backdrop that is an **8-lobed scallop** (`RoundedStarShape(sides = 8, curve = 0.10)`) instead of a circle, painted in `onContainer.copy(alpha = 0.12f)`, that **rotates 0→360° over 9000 ms `LinearEasing` infinitely** and scales to `1.16f` (`animateFloatAsState(tween(450, FastOutSlowInEasing))`). (Non-active rows use a plain circle; the rotation transition is composed for every row but only applied to the active one.)
- Name (`titleMedium` SemiBold, 1 line), a `BadgeChip` status ("Connected"/"Connecting"/"Available"/"Available to connect", with a wifi/bluetooth icon), and a trailing metric (battery% or `"volume/volumeMax"`) for selected variable-volume devices.
- **Click routing** (`:1564`): bluetooth → open BT settings; selected + connected → `onDisconnect`; selected + connecting → no-op `({})`; else `onSelect` → `playerViewModel.selectRoute(route)`. `onDisconnect` also dismisses the sheet.

## 22. Complete animation reference

| Element | Spec |
|---|---|
| Sheet open fraction (dims/scales player) | `animateFloatAsState` tween 220 ms FastOutSlowIn |
| Sheet enter/exit | stock M3 `ModalBottomSheet` spring + scrim |
| Pager height | `animateContentSize` tween 280 ms FastOutSlowIn (snap first 2 frames) |
| "Scanning nearby" badge | `AnimatedVisibility` fadeIn 180 ms / fadeOut 160 ms |
| Refresh linear bar | fadeIn 200 ms FastOutSlowIn / fadeOut 180 ms |
| Shimmer placeholder | infinite tween 1000 ms + 200 ms delay |
| Active-device scallop | rotate 0→360° tween 9000 ms Linear infinite; scale →1.16f tween 450 ms FastOutSlowIn |
| Tab pill | scale pulse 1→1.05→1 tween 250 ms; color tween 200 ms |
| Cast-button corners | `animateDpAsState` spring(MediumBouncy, StiffnessLow), 6 ↔ 50 dp |
| Cast-button width | `animateContentSize` spring(MediumBouncy, StiffnessLow), 58 ↔ 190 dp |
| Cast-button label swap | `AnimatedContent` fadeIn 150 ms / fadeOut 120 ms |
| Connecting spinner (button / hero) | `CircularProgressIndicator` 14 dp·2 dp / 34 dp·4 dp Round |

Haptics: volume-slider step + tab switch only (`TextHandleMove`); none on device select, disconnect, or sheet open/close.

---

# APPENDIX

## A. Google Cast platform facts used above

These are the platform behaviors (from Google's official Android-sender docs) that PixelPlayer's code above relies on.

- **The Cast SDK ships inside Google Play services** — `play-services-cast-framework` is a thin client; the real module loads on-device. On a device without GMS, cast can't work.
- **`OptionsProvider` + manifest meta-data** (`OPTIONS_PROVIDER_CLASS_NAME`) is the required bootstrap; `CastContext.getSharedInstance(context)` is main-thread only (the synchronous overload PixelPlayer uses is deprecated in favor of an async `getSharedInstance(context, executor)` returning a `Task`).
- **Receiver types:** the **Default Media Receiver** (`CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID`) is Google-hosted and *"does not require you to register with the Google Cast SDK Developer Console"* and cannot be styled. The **Styled** and **Custom** receivers require registering in the Cast Developer Console. (PixelPlayer uses the Default receiver.)
- **`SessionManager` / `SessionManagerListener`** own the session lifecycle (`onSessionStarting/Started/StartFailed/Suspended/Resuming/Resumed/Ending/Ended`). `castSession.remoteMediaClient` is the media-control surface and is nullable.
- **Media loading:** `MediaInfo.Builder(contentId)` + `setStreamType` + `setContentType` + `setMetadata`, loaded via `remoteMediaClient.load(MediaLoadRequestData…)` or `queueLoad(...)`; all methods return `PendingResult` and are main-thread only. For the Default receiver, `contentId` is the media URL.
- **`STREAM_TYPE_LIVE` vs `STREAM_TYPE_BUFFERED`:** for a live stream *"the SDK automatically displays a play/stop button in place of the play/pause button"* in the framework controllers/notification; `BUFFERED` is for on-demand content (what PixelPlayer's local files use).
- **Volume:** *"The framework automatically manages the volume for the sender app"* and *"the physical buttons on the sender device can be used to change the volume of the Cast session on the Web Receiver by default for any device using Jelly Bean or newer"* — no app code required. The androidx path PixelPlayer uses for its slider is `MediaRouter.RouteInfo.requestSetVolume(int)` (integer units up to `volumeMax`, only on the selected route), which the Cast route provider maps to the receiver device volume.
- **Supported audio** on the Default receiver: MP3, LC-AAC, HE-AAC, FLAC, Opus, Vorbis, WAV (LPCM), WebM; containers MP4/OGG/WAV/etc; protocols HLS, MPEG-DASH, SmoothStreaming, and progressive download. Artwork display caps at 720p. *"With adaptive bitrate streaming protocols, you must implement CORS"* — progressive downloads are not called out as needing CORS. (This is why PixelPlayer's progressive-file server sends no CORS headers.)

**Docs referenced:** [Integrate Cast (Android)](https://developers.google.com/cast/docs/android_sender/integrate), [Supported media](https://developers.google.com/cast/docs/media), [Web Receiver types](https://developers.google.com/cast/docs/web_receiver).

---

*Reference source: the `PixelPlayerHQ/PixelPlayer` repository, cloned locally at `../PixelPlayer` for this research. All code facts above were read directly from that source.*
