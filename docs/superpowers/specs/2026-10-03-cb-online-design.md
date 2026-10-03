# CB Online — v1 Design

- **Date:** 2026-10-03
- **Status:** Approved in conversation; awaiting written-spec review
- **Project:** `C:\Projects\Claude\Android\CombatBoxOnline`

## 1. Purpose

An Android companion app that answers two questions for Combat Box (IL-2 Great Battles) players, equally:

1. **Is it worth flying right now?** How many players are online and how the sides are balanced.
2. **Are my mates on?** Which starred friends are online.

Players fly in VR, so the app is used *before* and *between* sessions, not in the cockpit.

**Audience:** the author plus other Combat Box players. Distributed as a signed APK on GitHub releases (not the Play Store).

**Success for v1:** a player installs the APK, opens it, and within a few seconds sees an accurate count, side balance and roster; can star friends and find them pinned at the top on later launches; and is told when a newer release exists.

## 2. Scope

**In v1**
- Single roster screen (summary, friends, per-side lists)
- Starring friends (persisted on device)
- Foreground refresh: on open/resume, pull-to-refresh, every 60 s while visible
- In-app update notice from GitHub releases
- About dialog
- Release signing and R8-minified release build

**Out of v1 (roadmap, not designed here)**
- v2: home-screen widget (Glance)
- v3: notifications when friends come online or player count passes a threshold (WorkManager, ≥15 min interval; alerts lag accordingly)
- Later: tours/stats/leaderboard; a relay server with push (approach B) if 15-minute alert lag proves too slow
- Starring a player who is not currently online (by typing a name)
- Persisting the roster across launches
- Other servers

## 3. Data source

`GET https://il2statsapi.combatbox.net/api/OnlinePlayers` — public, no auth. Observed 2026-10-03: HTTP 200, `application/json`, ~1.3 KB for 13 players, ~0.3 s.

Response: JSON array of objects:

| Field | Type | Meaning | Used in v1 |
|---|---|---|---|
| `nickname` | string | Player name | yes |
| `coalition` | int | `1` Allied, `2` Axis, `0`/other = unassigned | yes |
| `date` | ISO-8601 UTC string | Time the player joined the mission | no |
| `timeOnMission` | string `"H…H:MM"` | Time since joining | yes |

The API exposes players only: no mission name, map, or server status.

This API is not operated by this project. The app polls politely: never more than once per 60 s per device in the foreground, never in the background, and with an identifying `User-Agent`.

## 4. User experience

**Identity**
- Launcher label: **CB Online**
- Application ID: `io.github.riaanjutte.cbonline`
- Source/release repo: `riaanjutte/CBOnline-Android` (public)
- Launcher icon: Combat Box cube logo (`C:\Projects\Codex\CombatBoxArt\assets\combat-box-logo.png`, 512×512, author has permission to use it) as an adaptive icon. The logo is the foreground, inset to sit inside the 66 dp safe zone. The background is solid navy `#0E1B33`.

**Roster screen, top to bottom**
1. **Top app bar:** title "CB Online"; overflow menu with **About**.
2. **Update banner** (only when a newer release exists): "Version X.Y.Z available", with **Download** (opens the release page in the browser) and **Dismiss** (hides it for that version).
3. **Summary card:**
   - Large total, e.g. "13 online"
   - Balance bar split proportionally: Axis `#2F80ED`, Allied `#E5484D`, Unassigned `#8B949E`
   - Text line "4 Axis · 9 Allied", with "· N unassigned" appended when N > 0
   - "Friends: 1 of 3 online", shown only when at least one friend is starred
   - "Updated 20:23" in the device's local short time format
4. **Friends section** (only when at least one friend is starred):
   - Online friends first, A–Z. Each row has a side tag (text label in side colour) and time on mission.
   - Then offline friends, A–Z, greyed out and labelled "offline". They keep the star toggle, so stale names can be unstarred.
5. **Side sections:** Axis, Allied, then Unassigned (Unassigned only when non-empty).
   - Sticky headers with the count, e.g. "Allied (9)".
   - Rows sorted A–Z, ignoring case.
   - Starred players also stay in their side section, so header counts always equal row counts.
6. **Row:** nickname, formatted time on mission, and a star toggle.
   - The toggle's accessibility label is "Star {name}" / "Unstar {name}".
   - Starred players show a filled amber star (`#F2B705`).

**Time on mission formatting:** `"00:27"` → "27 min"; `"00:02"` → "2 min"; `"01:05"` → "1 h 05 min"; `"12:00"` → "12 h 00 min". Hours may have more than two digits. Anything that doesn't match `^\d+:\d{2}$` is shown verbatim.

**States**
- **First load:** centred progress indicator.
- **Loaded, zero players:** summary card with "0 online" and the message "Nobody's flying right now."
- **Refresh failed, but previous data exists:** keep showing the previous data, plus a strip "Couldn't refresh — showing 20:15".
- **No data and the load failed:** full-screen error message with a **Retry** button.

**Refresh behaviour**
- Fetch immediately when the screen becomes visible (launch or return from background).
- Fetch again every 60 s while visible.
- Pull-to-refresh fetches immediately and restarts the 60 s timer.
- A manual refresh while a fetch is already in flight is ignored.
- No fetches while the app is not visible. Polling stops within 5 s of leaving the screen.

**Starring**
- Tapping the star toggles the friend immediately; no network request is made.
- Friends are matched by nickname after trimming and lowercasing. The display name kept is the one first starred.
- **Known limitation:** stars are keyed by nickname (the API has no player ID). If a friend renames, re-star them and unstar the old name from the Friends section.

**Theme**
- Fixed brand colours, not wallpaper-based dynamic colour, so red and blue always mean the same sides.
- Follows system light/dark.
- Primary accent: indigo `#4F5BD5`, used sparingly (progress indicators, buttons).
- Side colours are used only for side tags, the balance bar and section header accents.
- Side is always conveyed by a text label as well as colour.

**About dialog:** app version, logo, "Player data: il2statsapi.combatbox.net", and a link to the GitHub repo.

## 5. Architecture

Package root: `io.github.riaanjutte.cbonline`. No DI framework.

| Unit | Responsibility | Interface (shape) | Depends on |
|---|---|---|---|
| `data/OnlinePlayer.kt` | Model | `data class OnlinePlayer(nickname: String, coalition: Coalition, timeOnMission: String)`; `enum Coalition { Axis, Allied, Unassigned }` | — |
| `data/OnlinePlayersApi.kt` | Fetch and parse the roster | `suspend fun fetch(): List<OnlinePlayer>` (throws on network, HTTP or parse failure) | OkHttp, kotlinx.serialization |
| `data/FriendsStore.kt` | Persist starred nicknames | `val friends: Flow<Set<String>>`; `suspend fun toggle(nickname: String)` | DataStore Preferences |
| `data/UpdateChecker.kt` | Find a newer GitHub release; remember dismissals | `suspend fun check(currentVersion: String): UpdateInfo?` (null on no update or any failure); `suspend fun dismiss(version: String)` | OkHttp, kotlinx.serialization, DataStore |
| `data/Versions.kt` | Compare version strings | `fun isNewer(candidate: String, current: String): Boolean` | — |
| `roster/RosterBuilder.kt` | Pure transform to screen model | `fun build(players: List<OnlinePlayer>, friends: Set<String>): Roster` | — |
| `roster/TimeOnMission.kt` | Pure formatter | `fun formatTimeOnMission(raw: String): String` | — |
| `ui/RosterViewModel.kt` | Orchestrates fetch → build → state; refresh loop; error retention | `val state: StateFlow<RosterUiState>`; `fun refresh()`; `fun toggleFriend(name)`; `fun dismissUpdate()` | API, FriendsStore, UpdateChecker, RosterBuilder |
| `ui/RosterScreen.kt` (+ small composables) | Render `RosterUiState`; stateless apart from UI concerns | `@Composable fun RosterScreen(state, onRefresh, onToggleFriend, onDismissUpdate, onOpenUrl)` | Compose, Material 3 |
| `ui/theme/Theme.kt` | Brand colours, light/dark | `CbOnlineTheme { }` | Material 3 |
| `AppContainer.kt`, `CbOnlineApp.kt`, `MainActivity.kt` | Create singletons once (OkHttp client, Json, DataStore, API, stores) and wire the ViewModel | — | all |

**Screen model**
- `Roster` holds:
  - `total`, `axisCount`, `alliedCount`, `unassignedCount`
  - `friendsOnline: List<RosterRow>`, `friendsOffline: List<String>`, `starredCount`
  - `axis`, `allied`, `unassigned: List<RosterRow>`
- `RosterRow` is `(nickname, coalition, timeLabel, isFriend)`.
- `RosterUiState` holds:
  - `roster: Roster?`, `lastUpdated: Instant?`
  - `isLoading`, `isRefreshing`, `refreshFailed: Boolean`, `errorMessage: String?`
  - `update: UpdateInfo?`

**Data flow**
1. `RosterViewModel` combines three inputs: the latest successful player list (from the 60 s polling loop and manual refreshes), the `FriendsStore.friends` flow, and the update check result.
2. It runs `RosterBuilder.build` and emits `RosterUiState`.
3. Polling runs inside a flow shared with `SharingStarted.WhileSubscribed(5_000)` and collected by the UI with `collectAsStateWithLifecycle`. Leaving the screen stops polling; returning restarts it with an immediate fetch.
4. Toggling a star writes to DataStore. The friends flow re-emits and the UI rebuilds from the cached player list, with no refetch.

**Future reuse:** the v2 widget and v3 worker reuse `OnlinePlayersApi`, `FriendsStore` and `RosterBuilder` unchanged. A relay (approach B) changes only `OnlinePlayersApi` internals.

## 6. Error handling

- **Roster fetch:**
  - OkHttp call timeout is 10 s.
  - A non-2xx status, IO failure, or JSON parse failure of the whole payload throws, and the ViewModel catches it.
  - If previous data exists, set `refreshFailed = true` and keep the data.
  - If there is no data, show the full-screen error with Retry.
  - The next successful fetch clears the flags.
- **Individual malformed entries:** an entry with a blank or missing nickname is dropped. A missing or unknown `coalition` maps to Unassigned. Unknown fields are ignored (`ignoreUnknownKeys = true`, `coerceInputValues = true`).
- **Update check:** every failure (404 when no repo or release exists, rate limit, network, parse) yields `null`, so no banner. It is never surfaced to the user.
- **DataStore:** an IO failure when reading friends is treated as an empty set and logged.
- **Debug builds:** each roster request is logged with `Log.d("CBOnline", ...)` so polling behaviour can be verified with logcat. Release builds don't log it.

## 7. Update check

- **Request:** `GET https://api.github.com/repos/riaanjutte/CBOnline-Android/releases/latest`
  - Headers: `Accept: application/vnd.github+json`; `User-Agent: CBOnline-Android/{versionName}`
  - This endpoint excludes drafts and pre-releases.
- **When:** once per process launch, after the first roster load.
- **Response handling:**
  - Read `tag_name` and `html_url`.
  - Strip a leading `v`/`V` from the tag.
  - If `isNewer(tag, BuildConfig.VERSION_NAME)` and the tag isn't the stored dismissed version, return `UpdateInfo(version, url)`.
- **Version comparison:**
  - Split on `.` and compare segments numerically; a missing segment counts as 0.
  - If either side has a non-numeric segment, the result is "not newer".
  - Examples: `1.0.10` > `1.0.9`; `1.1` > `1.0.9`; `1.0` == `1.0.0`.
- **Rate limit:** unauthenticated GitHub allows 60 requests per hour per IP. One check per launch is well within it, and exceeding it fails silently.

## 8. Build and distribution

- **Toolchain:** same versions as the proven Demo project:
  - Gradle 9.3.1
  - AGP 9.1.1 with built-in Kotlin 2.2.10
  - Compose BOM 2025.08.01 (Material 3 `PullToRefreshBox`)
  - `org.jetbrains.kotlin.plugin.compose` and `org.jetbrains.kotlin.plugin.serialization` 2.2.10
  - compileSdk 35, targetSdk 35, minSdk 26
  - JDK 21 via `JAVA_HOME`
- **Libraries:** OkHttp 4.x, kotlinx-serialization-json, DataStore Preferences, lifecycle viewmodel/runtime compose. Test-only: JUnit 4, kotlinx-coroutines-test, OkHttp MockWebServer. Exact versions are pinned in the implementation plan, chosen from the same 2025 era to stay compatible with Kotlin 2.2.10 and compileSdk 35.
- **Permissions:** `INTERNET` only.
- **Versioning:**
  - `versionName` follows `MAJOR.MINOR.PATCH`, starting at `1.0.0`.
  - `versionCode = MAJOR*10000 + MINOR*100 + PATCH` (1.0.0 → 10000).
  - Git tag is `v{versionName}`.
- **Release build:** `isMinifyEnabled = true`, `isShrinkResources = true`. The kotlinx.serialization keep rules come from the library. The minified build is verified on a device before any release.
- **Signing:**
  - The author creates the release keystore with `keytool` and keeps it and its passwords outside the repo. Back it up: losing it makes future updates uninstallable over existing installs.
  - Gradle reads a git-ignored `keystore.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`).
  - If the file is absent, release builds are unsigned and can't be installed.
  - Passing `-Pcbonline.debugSignRelease=true` signs the release build with the debug key. This is for local verification only and is never published.
- **Release procedure** (documented in the README): bump the version, run `gradlew assembleRelease`, then `gh release create v{version} app-release.apk`. Creating the GitHub repo and publishing are separate, explicitly approved steps, not part of implementation.

## 9. Testing

**JVM unit tests (written test-first)**
- `OnlinePlayersApi` against MockWebServer:
  - The real-shape payload parses.
  - `coalition` 0/1/2/99/missing map correctly.
  - Extra fields are ignored; blank nicknames are dropped.
  - An empty array gives an empty list.
  - HTTP 500 throws; invalid JSON throws.
  - The `User-Agent` header is sent.
- `RosterBuilder`:
  - Counts and sections are right; sorting is A–Z ignoring case.
  - Friend matching ignores case and whitespace.
  - Online and offline friends are split correctly.
  - A friend stays in their side section.
  - Unassigned is empty when no unassigned players exist.
  - Zero players works.
- `formatTimeOnMission`: all examples in §4, plus `"123:05"`, `""` and `"abc"`.
- `Versions.isNewer`: all examples in §7, plus non-numeric segments.
- `UpdateChecker` against MockWebServer: newer, same, older, dismissed, 404, malformed.
- `FriendsStore` with a file-backed DataStore in a temp dir: toggling adds then removes; case-insensitive toggle-off; persists across store instances.
- `RosterViewModel` with a fake API and `runTest`:
  - First load success and failure.
  - A failed refresh keeps the previous data and sets `refreshFailed`.
  - Toggling a star doesn't call the API.
  - A manual refresh during an in-flight fetch is ignored.

**Compose previews:** loading, loaded (with friends and the update banner), empty, refresh-failed, and error.

**On-device verification (Pixel 9 Pro XL, Android 17, via adb)**
1. Install the debug build, launch it, and screenshot. Total and per-side counts match an API response fetched at the same time.
2. Star a player with adb input. They appear in Friends with the correct side tag.
3. Force-stop and relaunch. The star persists.
4. Press Home and wait more than 60 s. Logcat shows no roster requests while the app is in the background. Return to the app: an immediate request is logged.
5. Build the release with `-Pcbonline.debugSignRelease=true`, install it, and confirm it launches and shows live data (confirms R8 and serialization rules).

**Not verifiable in v1 without extra setup:**
- The update banner against a real release (needs the public repo and a release). It is covered by the MockWebServer tests.
- The error screen on device (would need changing the phone's network settings). It is covered by the ViewModel tests and previews.

## 10. Risks and limitations

- **The API is third-party to this project:** schema or availability changes break the app. Failures surface as the error/stale state, not crashes.
- **Nickname-keyed stars** break on renames (see §4).
- **Shared networks:** many devices behind one IP could exhaust the GitHub unauthenticated rate limit. The effect is only a missing update banner.
- **Signing key:** losing the release keystore forces users to uninstall and reinstall to upgrade.
