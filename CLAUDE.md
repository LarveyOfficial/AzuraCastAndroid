# CLAUDE.md

Guidance for working in this repository. This is a **native Android radio player for [AzuraCast](https://www.azuracast.com/) stations**, written in Kotlin with Jetpack Compose. It lets users collect AzuraCast stations, stream them with live metadata, discover new stations, and listen through Android Auto.

> Unofficial app, not affiliated with the AzuraCast project. Package `com.larvey.azuracastplayer`. Play Store listing exists; there is a sister iOS app.

> **This document is living.** You (Claude) are allowed and expected to update or change this CLAUDE.md as the project evolves — whenever a change makes something here inaccurate, add a new subsystem, remove code, or alter a convention/gotcha, edit the relevant section in the same change so the doc stays true to the code. Keep it accurate over comprehensive; prune stale guidance rather than letting it rot.

---

## 1. Build / tooling quick facts

- **Language / UI**: Kotlin, 100% Jetpack Compose (no XML layouts; only resource XML for drawables/themes/manifest).
- **Build**: Gradle Kotlin DSL. Single module `:app`. Version catalog in [gradle/libs.versions.toml](gradle/libs.versions.toml).
- **SDK**: `compileSdk`/`targetSdk = 36`, `minSdk = 26` (Android 8.0). JVM target 11.
- **Release build**: R8 minify + resource shrink enabled. Keep rules in [app/proguard-rules.pro](app/proguard-rules.pro) (protects Retrofit interfaces, Gson `@SerializedName` fields, and the `classes.data`/`classes.models` packages — **if you rename or move data classes, update ProGuard rules or release JSON parsing breaks**).
- **Namespace / appId**: `com.larvey.azuracastplayer`. Version tracked as `versionCode`/`versionName` in [app/build.gradle.kts](app/build.gradle.kts).
- Common commands: `./gradlew assembleDebug`, `./gradlew installDebug`, `./gradlew assembleRelease`, `./gradlew lint`, `./gradlew testDebugUnitTest`. JVM unit tests live in `app/src/test/` (pure logic, Gson fixture contracts in `app/src/test/resources/fixtures/`, MockWebServer coverage of the repository); run them before committing.

### Key libraries
- **Media3 (ExoPlayer + `MediaLibraryService` + `MediaSession`)** — playback + Android Auto/system media browser. HLS support (`media3-exoplayer-hls`).
- **Hilt (Dagger)** — dependency injection; app-wide singletons.
- **Room** — persistence of saved stations (destructive migration).
- **DataStore (Preferences)** — user settings (`"settings"` store).
- **Retrofit + Gson** — AzuraCast REST/static JSON APIs (suspend functions behind `AzuraCastRepository`).
- **Coil 3** — all async image loading + bitmap extraction for palette.
- **AndroidX Palette** — dominant/vibrant/muted color extraction from album art.
- **RenderScript Toolkit** (`libs/renderscript-toolkit-release.aar`, `com.google.android.renderscript.Toolkit`) — Gaussian blur for the legacy background.
- **Material3 + Material3 Adaptive + Material3 Expressive (alpha)** — `SupportingPaneScaffold`, `NavigationSuiteScaffold`, window size classes, `LinearWavyProgressIndicator`, `LoadingIndicator`. Note: pinned to alpha/rc versions (`material3:1.4.0-alpha11`, adaptive `1.1.0`).
- **Haze** (`dev.chrisbanes.haze`) — glassmorphism blur for the grid listener badge only.
- **sh.calvin.reorderable** — drag-and-drop reordering of the station list/grid.
- **Compose Navigation** — used only *inside* the Now Playing surface (nowPlaying ↔ queue). Top-level app navigation is a hand-rolled enum + `AnimatedContent`, **not** a NavHost.

---

## 2. High-level architecture

MVVM with a **service-owned player**, **Hilt singletons acting as a shared in-memory state bus**, and a single **`AzuraCastRepository`** behind the singletons/ViewModels for all network access.

```
AppSetup (Application, @HiltAndroidApp)
 ├─ Hilt singletons (hiltModules/singletons/):
 │    NowPlayingData          — live station metadata cache + playback-source setter
 │    SavedStationsDB         — Room-backed saved stations + in-memory mirror
 │    SharedMediaController   — holds MediaLibrarySession, isSleeping, PlayerState
 │    DiscoveryJSON (MutableState) — discovery catalog, fetched at provision time
 │    UserPreferences         — DataStore settings (held on AppSetup, not a Hilt module)
 │
 ├─ MusicPlayerService (MediaLibraryService, @AndroidEntryPoint)
 │    owns ExoPlayer (wrapped in ForwardingPlayer) + MediaLibrarySession
 │    serves the media browser tree (Stations / Discover) → Android Auto
 │
 └─ MainActivity (single activity, @AndroidEntryPoint, Compose)
      binds a Media3 MediaController to the service
      renders all UI: My Radios, Discovery, Now Playing, Settings
```

### The "shared state" pattern (important)
Several singletons expose Compose `mutableStateOf`/`mutableStateMapOf` fields that are **read and written from both the Activity and the Service**. This is the app's core wiring; understand it before changing playback:

- `SharedMediaController.mediaSession` — set by the service in `onCreate`, cleared in `onDestroy`. The Activity/ViewModels call `sharedMediaController.mediaSession.value?.player` to control playback directly.
- `SharedMediaController.playerState` — a `PlayerState` (see [state/PlayerState.kt](app/src/main/java/com/larvey/azuracastplayer/state/PlayerState.kt)) created in `MainActivity` from `mediaController.state()` and pushed into the singleton so any ViewModel can observe playback reactively.
- `SharedMediaController.isSleeping` — sleep-timer active flag, toggled from the sleep-timer UI and the service's `stop()`.
- `NowPlayingData.nowPlayingURL / nowPlayingShortCode / nowPlayingMount` — the "what is currently playing" tuple; the service reads these to know which station's metadata to refresh.
- `NowPlayingData.staticDataMap` — `Map<Pair<url, shortCode>, StationJSON>` cache of the AzuraCast now-playing static JSON for every known station; `staticData` is the currently-playing station's entry.
- `SavedStationsDB.savedStations` — in-memory `mutableStateOf<List<SavedStation>?>` mirror of the Room table, kept in sync manually.

Because state is shared this way, **UI updates as a side effect of the service (or a background poll) mutating these singleton fields.**

---

## 3. Directory / package map

`app/src/main/java/com/larvey/azuracastplayer/`

| Package | Contents |
|---|---|
| `AppSetup.kt` | `Application` class. Boots Hilt, cancels stray sleep alarms, warms the station metadata cache, holds `UserPreferences`. Declares Hilt `@EntryPoint`s for accessing singletons from non-injected code. Also defines the `Context.dataStore` extension. |
| `api/` | The network layer: `ApiResult` (sealed result + `safeApiCall` mapper), `AzuraCastApi` (the single Retrofit interface), `AzuraCastRepository` (suspend functions returning `ApiResult`; pure URL builders). |
| `classes/data/` | Plain data/model classes. `StationJSON` (the AzuraCast now-playing schema), `DiscoveryJSON`, `SavedStation` (Room `@Entity`). |
| `classes/models/` | The shared-state singletons' backing classes: `NowPlayingData`, `SavedStationsDB`, `SharedMediaController`. |
| `db/` | Room: `SavedStationDao`, `SavedStationsDatabase`. `db/settings/` holds `UserPreferences` (DataStore), `SettingsViewModel`, `AppViewModelProvider`. |
| `hiltModules/` | `NetworkModule` (OkHttp/Retrofit/`AzuraCastApi`), `CoroutinesModule` (`@ApplicationScope` main-immediate scope); `singletons/` holds the state-bus singleton providers (`NowPlayingData`, `SavedStationsDB`, `SharedMediaController`, `DiscoveryJSON`). |
| `session/` | Playback: `MusicPlayerService`, `MediaController` (client-side `rememberManagedMediaController`), `sleepTimer/` (AlarmManager-based). |
| `state/` | `PlayerState` — a Compose-observable snapshot of a Media3 `Player` (mirrors every `Player.Listener` callback into snapshot state). |
| `ui/mainActivity/` | The activity, its ViewModel, and the main-screen sub-features: `radios/` (My Radios list/grid), `addStations/`, `settings/`, `components/` (mini player, mesh gradient, edit/delete dialogs). |
| `ui/nowplaying/` | The Now Playing sheet/pane and its `components/`. |
| `ui/discovery/` | The Discovery page and its `components/`. |
| `ui/theme/` | Material3 color tokens, theme wiring, typography. |
| `utils/` | Small helpers (see §11). |

---

## 4. Data models & AzuraCast APIs

All network access flows through **`AzuraCastRepository`** ([api/AzuraCastRepository.kt](app/src/main/java/com/larvey/azuracastplayer/api/AzuraCastRepository.kt)) — a Hilt `@Singleton` exposing suspend functions that return a typed **`ApiResult`** (`Success` / `Failure.Http` / `Failure.Network` / `Failure.Unexpected`). One shared default-configured `OkHttpClient` + `Retrofit` (from `hiltModules/NetworkModule`), one Retrofit interface (`AzuraCastApi`): the fixed-host discovery endpoint is relative to the base URL, the per-station-host endpoints take `@Url` absolute URLs built by pure, test-covered builders. **The repository never logs (callers log with context) and never switches dispatchers** — Retrofit suspend calls are non-blocking, and callers rely on resuming on the main thread to mutate the Media3 player. Do not add `withContext(Dispatchers.IO)` there. Callers launch work on the `@ApplicationScope` `CoroutineScope` (`Dispatchers.Main.immediate`, from `hiltModules/CoroutinesModule`). URLs are always forced to HTTPS via `String.fixHttps()`; the manifest also allows cleartext.

**Endpoints used** (base URL is `https://<station-host>`):
- `GET /api/nowplaying_static/{shortCode}.json` → `StationJSON`. The primary metadata source (["static nowplaying" JSON](https://www.azuracast.com/docs/developers/now-playing-data/#static-now-playing-json-file)). Fetched via `AzuraCastRepository.getNowPlayingStatic` (seeds the cache and refreshes the playing item's metadata). Preferred because it's a cached static file, cheap to poll.
- `GET /api/nowplaying` → `List<StationJSON>`. Used only by the add-station flow (`AzuraCastRepository.listHostStations`) to enumerate all stations on a host the user typed in.
- Album art URLs: `https://<url>/api/station/<shortcode>/art/-1` and per-song `art` fields.

**Discovery catalog**: fetched from a static GitHub-hosted JSON, **not** from AzuraCast:
`https://owcramer.github.io/AzuraCast-Discovery-API/azuracastDiscovery.json` → `DiscoveryJSON` (`featuredStations` + `discoveryStations` categories, each with `DiscoveryStation`s). Fetched once when the Hilt `DiscoveryJSONModule` provides the singleton.

**`StationJSON`** ([classes/data/StationJSON.kt](app/src/main/java/com/larvey/azuracastplayer/classes/data/StationJSON.kt)) mirrors AzuraCast's schema: `station` (with `mounts`, `hls_enabled`, `hls_url`), `now_playing` (`played_at`, `duration`, `song`, `elapsed`, `remaining`), `playing_next`, `song_history`, `listeners`, `is_online`. Gson `@SerializedName` maps snake_case.

**`SavedStation`** (Room `@Entity`, composite PK `[shortcode, url]`, indexed on `position`): `name`, `url` (host), `shortcode`, `defaultMount` (the stream URL to play), `position` (user ordering).

### Persistence
- Room DB `datamodel.db`, version 2, **destructive migration** (`fallbackToDestructiveMigration`). Bumping the schema wipes saved stations — acceptable here but be aware.
- DAO: `@Upsert insertStation`, `getAllEntries()` (ordered by `position`), `deleteStation`, `updateStation`.
- `SavedStationsDB` wraps the DAO and maintains the `savedStations` in-memory mirror; callers must refresh it (`getStationList`) after writes.

### Settings (DataStore) — [db/settings/UserPreferences.kt](app/src/main/java/com/larvey/azuracastplayer/db/settings/UserPreferences.kt)
Keys: `is_grid_view` (My Radios grid vs list, default grid), `theme_type` (`ThemeTypes.SYSTEM/LIGHT/DARK`), `is_dynamic_theme` (Material You, default on), `android_auto_layout` (`AndroidAutoLayouts.GRID/LIST`), `legacy_media_background` (bool). `SettingsViewModel` exposes these as `StateFlow`s and provides toggles.

---

## 5. Playback & the media service

[session/MusicPlayerService.kt](app/src/main/java/com/larvey/azuracastplayer/session/MusicPlayerService.kt) is a `MediaLibraryService` (`@AndroidEntryPoint`, `foregroundServiceType=mediaPlayback`). It is the single source of playback truth.

### Player setup (`onCreate`)
- ExoPlayer wrapped in a custom `ForwardingPlayer` that overrides:
  - `play()` → also `seekToDefaultPosition()` (jump to live edge on resume).
  - `stop()` → clears the now-playing tuple, cancels any sleep alarm, `isSleeping=false`, `clearMediaItems()`.
  - `getCurrentPosition()` → for live radio, derives elapsed time from `now_playing.played_at` vs wall clock (with a guard against negative time due to clock skew) instead of the raw stream position.
- A `Player.Listener`:
  - `onMediaMetadataChanged` → re-fetches station metadata (`NowPlayingData.setMediaMetadata`) so the notification/UI stay current.
  - `onPlayerError` → for network/live-window/bad-HTTP errors, **auto-retries** (`seekToDefaultPosition()` + `prepare()`). This is how the app survives radio stream hiccups.
- `DefaultMediaNotificationProvider` with a custom small icon; a custom session command layout adds a **Stop** button (`"STOP_RADIO"`).
- A `BroadcastReceiver` listens for the sleep-timer action (`...MusicPlayerService.SLEEP`) and stops the player.

### Lifecycle / teardown
- `onTaskRemoved` (app swiped away) and `onDestroy` both stop + release the player and session. `onDestroy` even calls `android.os.Process.killProcess(myPid())` — a hard kill (historically to resolve an Android Auto deadlock; see commits mentioning "deadlock"). Be cautious changing teardown.

### Client side — [session/MediaController.kt](app/src/main/java/com/larvey/azuracastplayer/session/MediaController.kt)
`rememberManagedMediaController()` is a Composable returning a `State<MediaController?>`. A singleton `MediaControllerManager` builds a Media3 `MediaController` bound to `MusicPlayerService` via `SessionToken`, tied to the lifecycle (initialize on `ON_START`; release on abandon/forget). `MainActivity` wraps this controller in a `PlayerState` and publishes it to `SharedMediaController`.

### How a station starts playing
`NowPlayingData.setPlaybackSource(mountURI, url, shortCode, mediaPlayer)`:
1. stops the current player, sets the now-playing tuple,
2. calls `setMediaMetadata(..., reset=true)` → fetches the static JSON via the repository, then `applyNowPlayingMetadata` builds a `MediaItem` with full `MediaMetadata` (title/artist/album/art/duration), `replaceMediaItem(0, ...)`, then `prepare()` + `play()` — all on the main thread via the `@ApplicationScope` scope.

Note the metadata quirks in `NowPlayingData.applyNowPlayingMetadata` ([classes/models/NowPlayingData.kt](app/src/main/java/com/larvey/azuracastplayer/classes/models/NowPlayingData.kt)): Android Auto reads `subtitle` as artist and `description` as album, so those fields are set redundantly alongside the "correct" ones — keep both when editing. Also `staticData.value = staticDataMap.put(...)` intentionally stores the *previous* map value (a `.put()` return), so `staticData` lags one refresh; live-position math and the progress UI were built against this — do not "fix" it casually.

### Metadata polling
Two independent pollers keep metadata fresh (radio has no client-side "track changed" signal):
- `MainActivityViewModel.init` loops every **30 s** refreshing every saved station's metadata (while the app is foregrounded — gated by `fetchData`, toggled in `onPause`/`onResume`).
- The Now Playing UI runs a **1 s** `updateTime` loop ([utils/UpdatePlayerTime.kt](app/src/main/java/com/larvey/azuracastplayer/utils/UpdatePlayerTime.kt)) to advance the progress bar, computing live-stream elapsed from `played_at`.

### Sleep timer — [session/sleepTimer/](app/src/main/java/com/larvey/azuracastplayer/session/sleepTimer/)
`AndroidAlarmScheduler` uses `AlarmManager.setAndAllowWhileIdle(RTC_WAKEUP, ...)` to broadcast the SLEEP action at the chosen time; the service receiver stops playback. UI lives in `MediaControls` (§7). Requires exact-alarm capability. Cancel uses `cancelAll()` on API ≥ 34.

---

## 6. Android Auto (read this in full before touching the media browser)

Android Auto integration is delivered entirely through the **`MediaLibraryService` + `MediaLibrarySession.Callback`** in [MusicPlayerService.kt](app/src/main/java/com/larvey/azuracastplayer/session/MusicPlayerService.kt). There is no separate `CarAppService` / car-app-library UI — it's the standard Media3 "browsable media" model that Android Auto (and Google Assistant, and the system media browser) consume.

### Manifest declarations
- `<meta-data android:name="com.google.android.gms.car.application" android:resource="@xml/automotive_app_desc"/>` where [automotive_app_desc.xml](app/src/main/res/xml/automotive_app_desc.xml) declares `<uses name="media"/>` — this is what flags the app as an Auto media app.
- The service is `exported="true"` with intent filters for `MediaLibraryService`, `MediaSessionService`, `MediaBrowserService`, and `MEDIA_PLAY_FROM_SEARCH`.
- The activity also handles `MEDIA_PLAY_FROM_SEARCH`.

### The browsable tree (`onGetLibraryRoot` / `onGetChildren`)
Root id is `"/"`. The hierarchy served to Auto:

```
/                                 (root, browsable)
├─ Stations                       "My Stations" folder (icon: stations_icon)
│   └─ SAVED_STATION-<mount>      one playable item per saved station
└─ Discover                       "Discover" folder (icon: discover_icon)
    ├─ Discover/Featured_Stations "Featured Stations"
    │   └─ DISCOVERED-<mount>     playable
    └─ Discover/<Category_Title>  one folder per discovery category (spaces → "_")
        └─ DISCOVERED-<mount>     playable
```

- **Media id prefixes are meaningful** and are how `onAddMediaItems` routes playback:
  - `SAVED_STATION-<defaultMount>` → look up in `savedStationsDB.savedStations`, strip prefix, play the mount.
  - `DISCOVERED-<preferredMount>` → look up in `discoveryJSON` (featured then all categories), resolve host from `publicPlayerUrl`, play.
  - Anything else → treated as a saved-station default mount.
  - In all cases `onAddMediaItems` sets `nowPlayingShortCode/URL/Mount` so metadata polling knows what's playing, then returns items with `.setUri(mediaId)`.
- **Grid vs List layout**: `onGetChildren` reads `UserPreferences.androidAutoLayoutFlow` and stamps folder items' extras with `MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE` = GRID_ITEM or LIST_ITEM. This is the setting toggled by the Android Auto settings card (§9). Playable station items are the ones styled.
- Folder items use `MEDIA_TYPE_FOLDER_RADIO_STATIONS`; playable items use `MEDIA_TYPE_RADIO_STATION` + artwork URIs + `durationMs=1` (radio has no real duration).

### Live refresh of the Auto tree
When the saved-station list or the Auto layout changes, the app calls `mediaSession.notifyChildrenChanged(parentId, count, params)` so Auto re-queries:
- `MainActivityViewModel.notifySessionStationsUpdated()` notifies `"Stations"` and `"/"` after any add/edit/delete/reorder.
- `AndroidAutoDropdownViewModel.updateAndroidAutoLayout()` notifies `"Stations"`, `"/"`, and `"Discover"` (after a 500 ms delay to let DataStore persist) so the grid/list change appears immediately.

### Voice / "play X on AzuraCast" (`onAddMediaItems` with a search query, `onSearch`, `onGetSearchResult`)
- Google Assistant voice playback arrives as `onAddMediaItems` where `requestMetadata.searchQuery != null`. The code normalizes the query (lowercases, strips spaces and a long list of "on azuracast/azurecast radio/player" variants — see the `.replace(...)` chain) then fuzzy-matches against saved station names, then against discovery stations, builds a playable `MediaItem`, and plays it. Empty match → empty list.
- `onSearch` notifies the browser of result count; `onGetSearchResult` returns saved stations whose `name+url` contains the query.

### Connection / commands (`onConnect`, `onPostConnect`, `onCustomCommand`)
- `onConnect` grants the custom `STOP_RADIO` command + the library subscribe/get-children/get-root/get-item commands, and **removes all seek commands** (`SEEK_BACK/FORWARD/TO_NEXT/TO_PREVIOUS/IN_CURRENT_MEDIA_ITEM` and `GET_TIMELINE`) because live radio isn't seekable. If playback controls misbehave in Auto, this command set is why.
- `onPostConnect` re-applies the custom Stop button layout for the connected controller.
- `onCustomCommand` handles `STOP_RADIO` → `player.stop()`.

### Practical notes for changing Auto behavior
- Any new browsable node must be handled in **both** `onGetChildren` (to list it) and `onAddMediaItems` (to make its children playable) with a consistent media-id scheme.
- After mutating the station set, you **must** call `notifyChildrenChanged` or Auto shows stale content.
- The `discoveryJSON` singleton may still be null (network not yet returned) when Auto queries — the code null-checks it; keep that.
- The hard `killProcess` in `onDestroy` exists to fix an Auto teardown deadlock; test Auto reconnection if you touch service lifecycle.

---

## 7. UI — Main activity & Now Playing

Single `MainActivity` ([ui/mainActivity/MainActivity.kt](app/src/main/java/com/larvey/azuracastplayer/ui/mainActivity/MainActivity.kt)), everything Compose. Adaptive/responsive design driven by Material3 window size classes.

### Shell
- **Adaptive layout**: a `SupportingPaneScaffold` — main pane = app content, supporting pane = Now Playing (shown side-by-side on wide/tablet, as a modal sheet on phones). A custom `PaneScaffoldDirective` zeroes the inter-pane spacer; a resizable `paneExpansionState` with proportional anchors + a `VerticalDragHandle`.
- `isWide` (from window size class) gates phone-vs-tablet behavior throughout; on narrow devices orientation is locked to portrait.
- **Top-level navigation** is a hand-rolled `AppDestinations` enum (`STATIONS`, `DISCOVER`; a `FAVORITES` entry is commented out) swapped via `AnimatedContent`, hosted in a `NavigationSuiteScaffold` (adaptive nav bar/rail). Not a NavHost.
- `Scaffold` provides: a `TopAppBar` with scroll-elevation color changes + grid/list toggle + settings button; animated FABs (Add station / confirm-reorder); a `bottomBar` hosting the `MiniPlayer` (only when something is playing and the Now Playing pane isn't expanded).
- **Settings** is a right-edge `ModalNavigationDrawer` (positioned right by wrapping in the `ReverseLayoutDirection` util).
- Custom pre-draw splash gate holds the first frame briefly.

### My Radios — [ui/mainActivity/radios/](app/src/main/java/com/larvey/azuracastplayer/ui/mainActivity/radios/)
- `MyRadios` renders either a `LazyColumn` (`StationListEntry`) or an adaptive `LazyVerticalGrid` (`StationGridEntry`) based on the grid/list setting, with pull-to-refresh.
- **Reordering** via `sh.calvin.reorderable`: long-press "Change Order" enters edit mode; drag reorders a local copy; confirm persists new `position`s via `editAllStations`. Haptics throughout.
- Each entry: Coil album art (themed loading/error drawables), live title/now-playing (marquee), listener count. **Tap = play** (`MyRadiosViewModel.setPlaybackSource`); **long-press = dropdown** (Delete / Edit / Change Order). Grid entry shakes in edit mode and shows a Haze-glass listener badge.
- Delete → `ConfirmStationDelete` (AlertDialog). Edit → `EditStation` (dialog to rename + pick default mount, filters flac/ogg/opus, offers experimental HLS if enabled).

### Add station — [ui/mainActivity/addStations/](app/src/main/java/com/larvey/azuracastplayer/ui/mainActivity/addStations/)
Full-screen `ModalBottomSheet`. User types a host URL → `AddStationViewModel.parseURL` normalizes/validates (adds `https://`, extracts host+port, validates with `Patterns.WEB_URL`) → `findHostsStations` fetches `/api/nowplaying` → shows a grid of that host's stations (filtering out flac/opus/ogg mounts). Multi-select → confirm FAB saves each as a `SavedStation`. `AddStationViewModel` is a plain `ViewModel` (not Hilt).

### Now Playing — [ui/nowplaying/](app/src/main/java/com/larvey/azuracastplayer/ui/nowplaying/)
Two containers sharing one inner composition: `NowPlayingSheet` (modal, phones) and `NowPlayingPane` (side pane, tablets). Inside: a `SharedTransitionLayout` + nested `NavHost` with `"nowPlaying"` (big album art + song/artist) and `"queue"` (`NowPlayingHistory`: up-next + song history). Album art is a **shared element** animating between the two.
- **Background** (two paths, chosen by SDK + `legacyMediaBackground`): modern = animated `meshGradient` from the palette's 9 colors (slow drifting via infinite transitions); legacy (API ≤ 28 or opt-in) = blurred, dimmed album art via RenderScript Toolkit.
- **Controls** (`MediaControls`): Stop / Play-Pause (shows a buffering `LoadingIndicator` when `playbackState == STATE_BUFFERING`) / Sleep-timer. Controls call `sharedMediaController.mediaSession.value?.player` directly. The MiniPlayer instead calls the `MediaController` — both drive the same session.
- **Progress** (`ProgressBar` + `NowPlayingBottomBar`): a Material3 `LinearWavyProgressIndicator` whose wave amplitude tracks `isPlaying`, colored from the palette; timestamps derived from `played_at`; a chip shows `HLS` or `<FORMAT> <bitrate>kbps` from the current mount.
- **Sleep timer UI**: time-picker dialog → schedules `AndroidAlarmScheduler`; toggling icon reflects `isSleeping`.

### Palette / color system
`MainActivityViewModel.updatePalette` runs whenever the artwork URI changes: Coil loads the bitmap (`allowHardware(false)`), AndroidX `Palette` extracts swatches, and `updateColorList` clamps luminance ≤ 0.45 / saturation ≤ 0.75 and expands 5 swatch colors into a 9-color list (`weightedRandomColors`) that feeds the mesh gradient. The raw `Palette` also colors the progress bar and mini player.

---

## 8. UI — Discovery

[ui/discovery/](app/src/main/java/com/larvey/azuracastplayer/ui/discovery/). Consumes the injected `DiscoveryJSON` singleton (does **not** fetch it itself). Also a `SupportingPaneScaffold`:
- A **featured carousel** (`HorizontalPager`, auto-advances every 5 s when idle) whose card colors come from each featured station's vibrant swatch (extracted in `DiscoveryViewModel.init` into `featuredPalettes`), with parallax alpha/scale.
- **Category rows**: horizontal `LazyRow`s of station cards per `DiscoveryCategory`.
- Tapping a station opens the supporting pane → `DiscoveryDetails` (hero image, name/description, **Play Station** and **Save Station** buttons; Save disabled if already saved) → `DiscoveryNowPlaying` (now-playing / up-next / song-history, reusing now-playing components). Play routes through `DiscoveryViewModel.setPlaybackSource`; Save persists a `SavedStation` and refreshes the Auto tree.

---

## 9. UI — Settings & Theme

Settings drawer ([ui/mainActivity/settings/](app/src/main/java/com/larvey/azuracastplayer/ui/mainActivity/settings/)), each section an expandable card backed by `SettingsViewModel`:
- **ThemePicker**: radio choice of System / Light / Dark (**no AMOLED option**), plus a **Dynamic Color** (Material You) switch on Android 12+.
- **AndroidAutoDropdown**: Grid vs List radio → persists `androidAutoLayout` and (after 500 ms) notifies the media session to rebuild the Auto tree.
- **LegacyMediaBackground**: switch (only shown on API > 28) toggling the blurred-art Now Playing background.
- **AboutDropdown**: version (opens Play Store), source code (GitHub), privacy policy.
- **ContactMeDropdown**: email (`mailto:luis@mtu.lol`), report a bug (GitHub issues), tip (Ko-fi).

Theme ([ui/theme/](app/src/main/java/com/larvey/azuracastplayer/ui/theme/)): Material Theme Builder tokens (blue seed) in `Color.kt` (medium/high-contrast token sets defined but **unused**). `AzuraCastPlayerTheme` resolves dark/light from the `theme_type` setting, uses `dynamic{Dark,Light}ColorScheme` when dynamic + API ≥ 31, else the static schemes; sets a transparent status bar. `Type.kt` only customizes `bodyLarge`.

---

## 10. Cross-cutting utilities — [utils/](app/src/main/java/com/larvey/azuracastplayer/utils/)

- `StringFixHttps.fixHttps()` — force first `http://`→`https://`. Used on essentially every station URL. 
- `UpdatePlayerTime.updateTime()` — 1 s progress-poll loop; live-stream elapsed from `played_at`.
- `CorrectedDominantColor` — vibrant swatch with lightness clamped for legibility per theme; `getOnVibrantColor` for text.
- `GetCurrentTheme.ColorScheme.isDark()` — app-preference-aware dark check (used to pick light/dark drawables).
- `BlurTransformation` — Coil transformation, double RenderScript Gaussian blur.
- `WeightedRandomColors` — inverse-frequency color sampling for the mesh gradient.
- `MeshGradient` (`ui/mainActivity/components/`) — custom `Modifier.meshGradient` rendering a Bézier-interpolated color mesh via `Canvas.drawVertices`.
- `RoundedCornerRadius` — reads the device's physical display corner radius (API ≥ 31) to nest sheet corners.
- `ResourceToUri`, `ConditionalModifier`, `ReverseLayoutDirection` — small helpers (resource→URI, conditional modifier chaining, LTR/RTL flip for the right-edge drawer).

---

## 11. Conventions & gotchas

- **2-space indentation**, `kotlin.code.style=official`. Match the surrounding heavy line-wrapping style (each builder arg on its own line).
- **No error UX / thin error handling**: API failures just log and no-op. If you add features, follow the existing null-check-and-bail pattern but consider surfacing errors.
- **API layer**: add new endpoints to `AzuraCastApi` + `AzuraCastRepository` (suspend → `ApiResult`), and handle both result branches at the call site (log failures with context — the repository itself never logs). Never make the repository switch dispatchers; player-mutating callers depend on main-thread resumption.
- **Shared mutable singletons** are load-bearing (see §2). Prefer wiring new cross-component state through `SharedMediaController` / `NowPlayingData` rather than inventing a parallel path.
- **`fixHttps()` everywhere**: keep applying it to any station/art/mount URL you introduce, or cache keys and playback break for `http` inputs. Port-in-URL handling was a real bug fixed historically ("Fix URLs w/ port numbers").
- **ProGuard**: any new Retrofit interface / Gson model must survive minification — extend the keep rules if you add data classes outside `classes.data`/`classes.models`.
- **Android Auto is easy to break**: changes to the media-id scheme, the browse tree, the granted command set, or service teardown all have Auto implications (§6). Test in the Desktop Head Unit / a car after such changes.
- **16 KB page size**: the project already added support (commit "Support 16KB page size") — keep native/aar deps 16KB-compatible.
- Alpha/rc Compose Material3 versions are intentional (Expressive APIs); expect occasional API churn on dependency bumps.

---

## 12. Versioning & releases (release-please)

Releases are automated with [release-please](https://github.com/googleapis/release-please) driven by [Conventional Commits](https://www.conventionalcommits.org/). **`main` is protected (merge via PR only)**, which fits release-please — it works entirely through PRs and never pushes to `main` directly.

Every PR into `main` runs [PR Checks](.github/workflows/pr-check.yml): `assembleDebug` **and** `assembleRelease` (release is unsigned on PRs — no signing env — but still runs R8/minify + resource shrinking so missing ProGuard keep rules fail the PR, not the release), plus `lintDebug` and unit tests. Set the **`build`** job as a required status check in branch protection.

### How it flows
1. You merge PRs into `main` using Conventional Commit messages (`feat:`, `fix:`, `feat!:`/`BREAKING CHANGE:`, plus `docs:`/`chore:`/`refactor:`… which don't trigger releases).
2. The [Release Please workflow](.github/workflows/release-please.yml) keeps an open **release PR** that accumulates a `CHANGELOG.md` and the next SemVer bump (`fix`→patch, `feat`→minor, breaking→major).
3. Merging that release PR creates a git tag (`vX.Y.Z`) and a **GitHub Release**, then a second CI job (`release-artifacts`) builds the signed **APK + AAB** and:
   - attaches the **APK** to the public GitHub Release, and
   - uploads the **AAB** privately to the **Play Console internal testing track** (via `r0adkll/upload-google-play`). The AAB is never attached to the public Release.

### Where the version lives
- **`versionName`** in [app/build.gradle.kts](app/build.gradle.kts) is owned by release-please — it's the line tagged `// x-release-please-version`. **Never edit the version string by hand** and keep that trailing comment or the updater won't find it.
- Source of truth is [.release-please-manifest.json](.release-please-manifest.json) (mirrored in [version.txt](version.txt)). Config is [release-please-config.json](release-please-config.json). `bootstrap-sha` there pins the history start (commit `fce73a6`) so the first changelog doesn't ingest the old non-conventional history — **remove `bootstrap-sha` after the first release cuts.**
- **`versionCode`** is NOT managed by release-please (it only does SemVer strings). It's computed in `build.gradle.kts` as `GITHUB_RUN_NUMBER + 75` so it always increases and never drops below the last published `75`; local builds fall back to `75`.
- The current line before release-please's first bump is `1.1.0`. The first release-please Release will be the next bump from there (e.g. `1.1.1` for a `fix`, `1.2.0` for a `feat`). If you want `1.1.0` itself cut as a Release, add `Release-As: 1.1.0` to a commit footer once.

### One-time setup the maintainer must do (outside code)
- **Repo setting**: Settings → Actions → General → "Allow GitHub Actions to create and approve pull requests" must be enabled, or release-please can't open its PR.
- **Signing secrets** (Settings → Secrets and variables → Actions) for the artifact build job. The keystore is a **PKCS12** file (`storeType = "PKCS12"` in `build.gradle.kts`); it is the Play **upload key** (the matching public cert is `upload_certificate.pem`, used only for Play App Signing enrolment — not a secret):
  - `SIGNING_KEYSTORE` — base64 of the release keystore (`base64 -w0 <keystore>`).
  - `SIGNING_KEYSTORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD` (for PKCS12 the key password is usually the same as the store password).
  Without these the build job fails (the app-level signing config no-ops locally, leaving unsigned release builds — that's expected off-CI).
- **Play upload secret**: `PLAY_SERVICE_ACCOUNT_JSON` — full JSON of a Google Play Developer API service account with "Release to testing tracks" permission, used to push the AAB to the internal track. (Google may require the app's very first release be made manually in the console before API uploads are accepted.)
- **Note on required checks**: PRs opened by the default `GITHUB_TOKEN` don't trigger *other* workflows. If you later add required status checks that must run on the release PR, switch the action's `token:` to a PAT/App token.

