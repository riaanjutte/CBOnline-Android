# Mission Info Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show the current mission (with time left), the next mission, and weather and in-game time for both, on CB Online's roster screen.

**Architecture:** A new `MissionInfoApi` (implementing `MissionSource`) reads the Combat Box mission-info JSON. `RosterViewModel` fetches it concurrently with the roster in the existing 60-second foreground loop and keeps the last good result in its own state. Pure formatters in `roster/MissionText.kt` turn it into text. A `MissionCard` that is always the first list item renders it, with its own 30-second clock for the countdown.

**Tech Stack:** Existing only: Kotlin 2.2.10 (AGP 9.1.1 built-in), Compose BOM 2025.08.01, OkHttp 4.12.0, kotlinx.serialization 1.9.0, coroutines. Tests: JUnit 4, kotlinx-coroutines-test, MockWebServer. **No new dependencies.**

**Spec:** `docs/superpowers/specs/2026-10-03-mission-info-design.md` (read it first; §n below refers to it). v1 spec and plan: `docs/superpowers/specs/2026-10-03-cb-online-design.md`, `docs/superpowers/plans/2026-10-03-cb-online-v1.md`.

**Branch:** `feat/mission-info` (already created from `main`; the spec is committed).

## Global Constraints

**Data**
- Mission URL: `https://campaign-data.combatbox.net/mission-info-tempest.json` (release is always this). Debug builds can override it with `-Pcbonline.missionUrl=<url>`, exposed as `BuildConfig.MISSION_URL`.
- Requests send `User-Agent: CBOnline-Android/{versionName}`, the same string as the roster API. Debug builds log `fetch MissionInfo` under tag `CBOnline`; release builds don't log.

**Copy** (exact, from §4; `…` is the single character U+2026, `–` is an en dash, `·` is a middle dot)
- Time left:
  - `{h} h {mm} min left`
  - `{m} min left`
  - `Less than a minute left`
  - `Changing mission…`
  - `Mission info may be out of date`
- Weather: `{temp} °C · {cover} cloud {base}–{top} m · Wind {from}° {speed} m/s`; `Clear sky`; ` · {Precip}`; `Precipitation`.
- `In-game: %1$s` and `Next: %1$s at %2$s` (string resources).

**Behaviour**
- Countdown thresholds: whole minutes rounded down. Treat −10 min ≤ remaining ≤ 0 as "changing", and anything below −10 min as "out of date".
- Mission failures never touch roster flags (`refreshFailed`, `errorMessage`, `isLoading`, `isRefreshing`). Roster failures never clear the mission. Each source updates its state as soon as its own fetch completes.
- The mission card is always the first `LazyColumn` item (key `mission`). It is a **1 dp** spacer when there's no mission, never zero height (§4).

**Environment** (carried over from the v1 plan)
- PowerShell from the repo root. If `$env:JAVA_HOME` is empty, set it to `C:\Program Files\Android\Android Studio\jbr`.
- Run Gradle and `git commit` via the PowerShell tool or `ctx_execute` shell, with a timeout of 300 s or more.
- adb: `C:\Users\riaan\AppData\Local\Android\Sdk\platform-tools\adb.exe`. Before any on-device step, check `isKeyguardShowing`; if `true`, ask the user to unlock the phone.
- Screenshots: `screencap` to `/sdcard`, then `adb pull`. Never redirect `exec-out`.
- Unit tests: `.\gradlew.bat :app:testDebugUnitTest --tests "<fqcn>"`.

**Commits**
- Commit message trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. The repo-local `user.email` is the GitHub no-reply address; leave it.
- Do not push (not requested).

## Review Focus

These inputs aren't named in the spec but are likely to bite. Each has its test in the owning task.
1. **Numbers arriving as decimals:** for example `"cloud_base_m": 2800.0` or `"direction_from_deg": 210.5` (the feed is generated, and its types can drift). These must parse and round, not fail the whole file. Covered by Task 1 `decimal numbers are accepted and rounded`.
2. **`"next_mission": null`** (explicit null, not missing) and a `next_mission` without `expected_start`: no Next block, and the current mission still shows. Covered by Task 1 `null or incomplete next mission gives null`.
3. **Unknown cloud-cover words** (`Overcast`, `Light`, `Scattered`): shown verbatim, never mistaken for clear sky. Covered by Task 2 `unknown cloud cover is shown verbatim`.
4. **Rotation while the screen is open:** a later fetch returns a different mission, and the card switches to it rather than sticking to the old one. Covered by Task 3 `new mission replaces the previous one`.
5. **Long mission names** (e.g. `Operation Bagration Phase II: Breakthrough at Bobruisk (June 1944)`): the name wraps rather than being cut off, since it's the key information. Covered by the Task 4 `LongMissionName` preview; the name `Text` has no `maxLines`. Live names may be short, so the phone check doesn't prove this.

---

### Task 1: Mission model, `MissionInfoApi`, and URL hook

**Files:**
- Create: `app/src/main/java/io/github/riaanjutte/cbonline/data/MissionInfo.kt`
- Create: `app/src/main/java/io/github/riaanjutte/cbonline/data/MissionInfoApi.kt`
- Create: `app/src/test/resources/mission-info-tempest.json` (fixture below)
- Test: `app/src/test/java/io/github/riaanjutte/cbonline/data/MissionInfoApiTest.kt`
- Modify: `app/build.gradle.kts` (add `MISSION_URL` alongside `API_BASE_URL`)
- Modify: `README.md` ("Debug-only test hooks" gains `-Pcbonline.missionUrl`)

**Interfaces:**
- Produces (exactly as spec §5):
  - `data class MissionInfo(val name: String, val historicalStart: LocalDateTime?, val estimatedEnd: Instant, val weather: Weather?, val next: NextMission?)`
  - `data class NextMission(val name: String, val expectedStart: Instant, val historicalStart: LocalDateTime?, val weather: Weather?)`
  - `data class Weather(val temperatureC: Double?, val cloudCover: String?, val cloudBaseM: Int?, val cloudTopM: Int?, val precipLevel: Double, val precipType: String?, val surfaceWind: Wind?)`
  - `data class Wind(val fromDeg: Int, val speedMs: Double)`
  - `interface MissionSource { suspend fun fetch(): MissionInfo }`
  - `class MissionInfoApi(client: OkHttpClient, json: Json, url: String, userAgent: String, log: (String) -> Unit = {}) : MissionSource`
  - `BuildConfig.MISSION_URL: String`

- [ ] **Step 1: Add the fixture.** It is the real file's structure, as observed on 2026-10-03 after the 21:42Z rotation:

```json
{"generated_at":"2026-10-03T21:42:02Z","server":"tempest",
 "mission":{"name":"Crimean Resolve (Dec. 1944)","historical_start":"1944-12-02T14:00:00","started_at":"2026-10-03T21:42:00Z","estimated_end":"2026-10-04T00:42:00Z"},
 "weather":{"temperature_c":1,"pressure_mmhg":780,"pressure_hpa":1039.9,"turbulence":1,"cloud_base_m":1900,"cloud_top_m":4900,"cloud_cover":"Heavy","precip_level":0,"precip_type":null,
  "wind_layers":[{"altitude_m":0,"direction_to_deg":340,"direction_from_deg":160,"speed_ms":3},{"altitude_m":500,"direction_to_deg":330,"direction_from_deg":150,"speed_ms":4},{"altitude_m":1000,"direction_to_deg":320,"direction_from_deg":140,"speed_ms":5},{"altitude_m":2000,"direction_to_deg":300,"direction_from_deg":120,"speed_ms":6},{"altitude_m":5000,"direction_to_deg":280,"direction_from_deg":100,"speed_ms":8}]},
 "next_mission":{"name":"Mitchell's Men (Mar. 1945)","path":"Dogfight/Alonzo/Mitchells_Men/Mitchells_Men_Mar_1945","expected_start":"2026-10-04T00:42:00Z","historical_start":"1945-03-05T09:00:00","weather_state":"prepared",
  "weather":{"temperature_c":-15,"pressure_mmhg":760,"pressure_hpa":1013.2,"turbulence":0.9,"cloud_base_m":3500,"cloud_top_m":9500,"cloud_cover":"Heavy","precip_level":0,"precip_type":null,
   "wind_layers":[{"altitude_m":0,"direction_to_deg":100,"direction_from_deg":280,"speed_ms":4},{"altitude_m":500,"direction_to_deg":90,"direction_from_deg":270,"speed_ms":5},{"altitude_m":1000,"direction_to_deg":80,"direction_from_deg":260,"speed_ms":6},{"altitude_m":2000,"direction_to_deg":70,"direction_from_deg":250,"speed_ms":6},{"altitude_m":5000,"direction_to_deg":60,"direction_from_deg":240,"speed_ms":4}]},
  "artifact_sha256":"22e60aafde2e92682eaa98a0d0fbb59491714cd5b5a94ba4d802a91724816354"}}
```

- [ ] **Step 2: Write the failing tests.**
  - Use a `MockWebServer` started in `@Before`.
  - Construct with `api = MissionInfoApi(OkHttpClient(), Json { ignoreUnknownKeys = true; coerceInputValues = true }, server.url("/mission-info-tempest.json").toString(), "CBOnline-Android/test")`.
  - Load the fixture with `javaClass.getResource("/mission-info-tempest.json")!!.readText()`.
  - Variant bodies are small inline JSON built around a minimal valid `mission`. Wrap each test in `runTest`.

```kotlin
@Test fun `parses the real file`()  // fixture → equals
    MissionInfo("Crimean Resolve (Dec. 1944)", LocalDateTime.of(1944, 12, 2, 14, 0), Instant.parse("2026-10-04T00:42:00Z"),
        Weather(1.0, "Heavy", 1900, 4900, 0.0, null, Wind(160, 3.0)),
        NextMission("Mitchell's Men (Mar. 1945)", Instant.parse("2026-10-04T00:42:00Z"), LocalDateTime.of(1945, 3, 5, 9, 0),
            Weather(-15.0, "Heavy", 3500, 9500, 0.0, null, Wind(280, 4.0))))
    // + takeRequest(): path == "/mission-info-tempest.json", User-Agent == "CBOnline-Android/test"
@Test fun `surface wind is the lowest layer even when unordered`()  // wind_layers [500:150/4, 0:210/3, 1000:140/5] → surfaceWind == Wind(210, 3.0)
@Test fun `missing weather and next mission give null`()           // only "mission" present → weather == null, next == null
@Test fun `null or incomplete next mission gives null`()           // "next_mission": null → next == null; next_mission without expected_start → next == null; mission still parsed
@Test fun `precipitation is read`()                                // weather precip_level 0.5, precip_type "snow" → precipLevel 0.5, precipType "snow"
@Test fun `empty wind layers give no surface wind`()               // "wind_layers": [] → surfaceWind == null
@Test fun `bad historical start gives null`()                      // "historical_start": "soon" → historicalStart == null, rest parsed
@Test fun `decimal numbers are accepted and rounded`()             // cloud_base_m 2800.0, cloud_top_m 4300.4, direction_from_deg 210.5 → cloudBaseM 2800, cloudTopM 4300, fromDeg 211 (Math.round)
@Test fun `missing mission throws`()                               // {"server":"tempest"} → exceptionOrNull() != null
@Test fun `bad estimated end throws`()                             // "estimated_end": "later" → exceptionOrNull() != null
@Test fun `http error throws IOException`()                        // 500 → exceptionOrNull() is IOException
@Test fun `html body throws`()                                     // 200 "<html>…</html>" → exceptionOrNull() != null
```

- [ ] **Step 3: Run the tests to confirm they fail.**
  - Run: `.\gradlew.bat :app:testDebugUnitTest --tests "io.github.riaanjutte.cbonline.data.MissionInfoApiTest"`
  - Expected: compilation failure (`MissionInfoApi` unresolved).

- [ ] **Step 4: Implement.**
  - **Pattern:** follow `OnlinePlayersApi`: `withContext(Dispatchers.IO)`, `execute()`, `IOException("HTTP $code")` on non-2xx, and `log("fetch MissionInfo")` before each request.
  - **DTOs:** private `@Serializable` classes with `@SerialName` for snake_case fields.
    - All numeric fields are `Double?`, mapped with `roundToInt()` where the model is `Int`.
    - `precip_level` defaults to `0.0`.
    - `wind_layers` defaults to `emptyList()`.
  - **Mapping:**
    - A missing `mission`, a missing or blank name, or an `estimated_end` that `Instant.parse` can't read → throw `IllegalArgumentException`.
    - `historical_start` uses `LocalDateTime.parse`, with `null` on failure.
    - `next_mission` → `null` if its name is missing or `expected_start` is missing or unparseable.
    - `surfaceWind` = the layer with the minimum `altitude_m`.

- [ ] **Step 5: Add the URL hook.** In `app/build.gradle.kts`, mirror `API_BASE_URL`:

```kotlin
val realMissionUrl = "https://campaign-data.combatbox.net/mission-info-tempest.json"
val debugMissionUrl = providers.gradleProperty("cbonline.missionUrl").getOrElse(realMissionUrl)
// debug { buildConfigField("String", "MISSION_URL", "\"$debugMissionUrl\"") }
// release { buildConfigField("String", "MISSION_URL", "\"$realMissionUrl\"") }
```

  - In the README's "Debug-only test hooks" list, add: `` `-Pcbonline.missionUrl=<url>` points a *debug* build's mission card at another file (e.g. a local fake). Release builds always use the real file.``

- [ ] **Step 6: Run the tests and build.**
  - Run: `.\gradlew.bat :app:testDebugUnitTest --tests "io.github.riaanjutte.cbonline.data.MissionInfoApiTest" :app:assembleDebug`
  - Expected: 12/12 pass, `BUILD SUCCESSFUL`, no `^w: ` lines.

- [ ] **Step 7: Commit.** Message: `feat: fetch and parse Combat Box mission info`.

---

### Task 2: Mission text formatting

**Files:**
- Create: `app/src/main/java/io/github/riaanjutte/cbonline/roster/MissionText.kt`
- Test: `app/src/test/java/io/github/riaanjutte/cbonline/roster/MissionTextTest.kt`

**Interfaces:**
- Consumes: `Weather`, `Wind` (Task 1).
- Produces:
  - `fun timeLeftLabel(estimatedEnd: Instant, now: Instant): String`
  - `fun weatherLine(weather: Weather): String`
  - `fun inGameLabel(start: LocalDateTime): String`

- [ ] **Step 1: Write the failing tests.**
  - Helper: `val end = Instant.parse("2026-10-04T00:42:00Z")`.
  - Helper: `fun left(d: Duration) = timeLeftLabel(end, end.minus(d))`, where a negative `d` means past the end.

```kotlin
@Test fun `hours and minutes`()  // 2h47m30s → "2 h 47 min left"; 2h05m → "2 h 05 min left"; exactly 1h → "1 h 00 min left"
@Test fun `minutes only`()       // 59m59s → "59 min left"; 47m → "47 min left"; exactly 1m → "1 min left"
@Test fun `under a minute`()     // 59s → "Less than a minute left"; 1s → "Less than a minute left"
@Test fun `changing mission`()   // 0 → "Changing mission…"; -5m → "Changing mission…"; exactly -10m → "Changing mission…"
@Test fun `out of date`()        // -10m1s → "Mission info may be out of date"; -3h → same

// weatherLine
@Test fun `full line`()                         // Weather(1.0,"Heavy",1900,4900,0.0,null,Wind(160,3.0)) → "1 °C · Heavy cloud 1,900–4,900 m · Wind 160° 3 m/s"
@Test fun `clear sky variants`()                // cover "Clear" / "clear" / null / "  " with Wind(210,3.0), temp 14.0 → "14 °C · Clear sky · Wind 210° 3 m/s"
@Test fun `unknown cloud cover is shown verbatim`()  // "Overcast", base 800, top 2000 → "… · Overcast cloud 800–2,000 m · …"
@Test fun `cover without heights`()             // "Medium", base null, top null → "14 °C · Medium cloud · Wind 210° 3 m/s"
@Test fun `precipitation`()                     // level 0.5 type "snow" → endsWith(" · Snow"); level 0.5 type null → endsWith(" · Precipitation"); level 0.0 type "rain" → no precip part
@Test fun `missing parts are omitted`()         // surfaceWind null → "14 °C · Medium cloud 2,800–4,300 m"; temperatureC null → "Medium cloud 2,800–4,300 m · Wind 210° 3 m/s"
@Test fun `negative and fractional numbers`()   // temp -15.0 → startsWith("-15 °C"); temp -2.5 → startsWith("-2.5 °C"); Wind(210, 3.5) → contains("Wind 210° 3.5 m/s")
@Test fun `grouping ignores the phone locale`() // Locale.setDefault(Locale.GERMANY) inside try/finally (restore it) → still "1,900–4,900 m"

// inGameLabel
@Test fun `in-game label`()                     // LocalDateTime.of(1944,12,2,14,0) → "2 Dec 1944, 14:00"; LocalDateTime.of(1943,9,15,11,0) → "15 Sep 1943, 11:00"
```

- [ ] **Step 2: Run the tests to confirm they fail.**
  - Run: `.\gradlew.bat :app:testDebugUnitTest --tests "io.github.riaanjutte.cbonline.roster.MissionTextTest"`
  - Expected: compilation failure.

- [ ] **Step 3: Implement.**
  - Remaining time = `Duration.between(now, estimatedEnd)`.
  - Choose the branch by comparing **total seconds** `s`:
    - `s ≥ 3600` → hours and minutes
    - `60 ≤ s < 3600` → minutes
    - `0 < s < 60` → less than a minute
    - `-600 ≤ s ≤ 0` → changing
    - `s < -600` → out of date
  - Displayed minutes are `s / 60` (positive `s` only, so this rounds down).
  - Use `String.format(Locale.ROOT, …)` for all numbers: `%,d` for heights, and `%d` or `%.1f` for temperature and speed (whole if `x % 1.0 == 0.0`).
  - `inGameLabel` uses `DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)`.
  - Join the parts with `" · "`.

- [ ] **Step 4: Run the tests to confirm they pass.**
  - Same command. Expected: 14/14 pass.

- [ ] **Step 5: Commit.** Message: `feat: format mission countdown, weather and in-game time`.

---

### Task 3: `RosterViewModel` mission state and app wiring

**Files:**
- Modify: `app/src/main/java/io/github/riaanjutte/cbonline/ui/RosterViewModel.kt`
- Modify: `app/src/main/java/io/github/riaanjutte/cbonline/AppContainer.kt`, `app/src/main/java/io/github/riaanjutte/cbonline/MainActivity.kt` (the constructor change forces this)
- Test: `app/src/test/java/io/github/riaanjutte/cbonline/ui/RosterViewModelTest.kt`

**Interfaces:**
- Consumes: `MissionSource`, `MissionInfo` (Task 1).
- Produces:
  - `RosterViewModel(players: PlayersSource, missions: MissionSource, friends: FriendsRepository, updates: UpdateSource, currentVersion: String)`
  - `RosterUiState.mission: MissionInfo? = null`
  - `AppContainer.missionApi: MissionInfoApi`

- [ ] **Step 1: Update the test harness.**
  - Add `FakeMissions : MissionSource` with:
    - `var next: () -> MissionInfo = { MISSION_A }`
    - `var calls = 0`
    - `var gate: CompletableDeferred<Unit>? = null`
    - The same `fetch()` shape as `FakePlayers`.
  - The `vm()` helper passes `missions` second.
  - Add companion `MISSION_A` and `MISSION_B`: two distinct `MissionInfo` values with `weather = null` and `next = null`.
  - Existing tests must keep passing unchanged apart from the helper.

- [ ] **Step 2: Write the failing tests.**

```kotlin
@Test fun `mission present after first load`()                   // subscribe → state.mission == MISSION_A, missions.calls == 1
@Test fun `each poll and manual refresh fetches each source once`() // advanceTimeBy(120_001) → players.calls == 3 && missions.calls == 3; refresh(); runCurrent() → 4 && 4
@Test fun `mission failure keeps roster and previous mission`()  // load; missions.next throws IOException; advanceTimeBy(60_001) → mission == MISSION_A, roster.total == 2, refreshFailed false, errorMessage null
@Test fun `roster failure keeps mission`()                       // load; players.next throws; advanceTimeBy(60_001) → refreshFailed true, mission == MISSION_A
@Test fun `first-load mission failure still shows roster`()      // missions.next throws from start → roster.total == 2, mission == null, errorMessage == null, isLoading false
@Test fun `first-load roster failure still shows mission`()      // players.next throws from start → errorMessage == "HTTP 503", mission == MISSION_A
@Test fun `slow mission does not delay roster`()                 // missions.gate = CompletableDeferred() (never completed) → after subscribe: roster.total == 2, isLoading false, mission == null
@Test fun `new mission replaces the previous one`()              // load (A); missions.next = { MISSION_B }; advanceTimeBy(60_001) → mission == MISSION_B
```

- [ ] **Step 3: Run the tests to confirm they fail.**
  - Run: `.\gradlew.bat :app:testDebugUnitTest --tests "io.github.riaanjutte.cbonline.ui.RosterViewModelTest"`
  - Expected: compilation failure (constructor and `mission` unresolved).

- [ ] **Step 4: Implement.**
  - Add `private val missionState = MutableStateFlow<MissionInfo?>(null)` and include it in the `combine` (4 flows → `toUiState(fetch, friends, update, mission)`).
  - Split the existing roster body into `private suspend fun fetchRoster(manual: Boolean)`, keeping its behaviour.
  - Add `private suspend fun fetchMission()`, which sets `missionState` on success, rethrows `CancellationException`, and otherwise ignores the failure.
  - `fetchOnce` becomes:

```kotlin
private suspend fun fetchOnce(manual: Boolean) {
    fetchState.update { it.copy(inFlight = true, isLoading = it.players == null, isRefreshing = manual && it.players != null) }
    try {
        coroutineScope {             // both run concurrently; each updates its own state when it finishes
            launch { fetchMission() }
            fetchRoster()            // catches its own failures, so it never cancels the mission fetch
        }
    } finally {
        fetchState.update { it.copy(inFlight = false, isRefreshing = false) }
    }
}
```

  - `AppContainer`: `val missionApi = MissionInfoApi(client, json, BuildConfig.MISSION_URL, userAgent, log = { if (BuildConfig.DEBUG) Log.d("CBOnline", it) })`.
  - `MainActivity`: pass `container.missionApi` second.

- [ ] **Step 5: Run the tests.**
  - Same command. Expected: 21/21 pass (13 existing + 8 new).
  - Then run the full suite: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`. Expected: green and 0 warnings.

- [ ] **Step 6: Commit.** Message: `feat: fetch mission info alongside the roster`.

---

### Task 4: Mission card UI and on-device verification

**Files:**
- Create: `app/src/main/java/io/github/riaanjutte/cbonline/ui/MissionCard.kt`
- Modify: `app/src/main/java/io/github/riaanjutte/cbonline/ui/RosterScreen.kt`, `app/src/main/java/io/github/riaanjutte/cbonline/ui/RosterPreviews.kt`, `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `RosterUiState.mission` (Task 3), `timeLeftLabel` / `weatherLine` / `inGameLabel` (Task 2).
- Produces: `@Composable fun MissionCard(mission: MissionInfo?, modifier: Modifier = Modifier)`.

- [ ] **Step 1: Add the strings.** `in_game` = `In-game: %1$s`, `next_mission` = `Next: %1$s at %2$s`.

- [ ] **Step 2: Write `MissionCard`.**
  - **No mission:** `mission == null` → `Spacer(modifier.height(1.dp))`.
  - **Card:** otherwise an `ElevatedCard(modifier.fillMaxWidth())` whose `Column` (16 dp padding, 4 dp spacing) holds:
    1. Name, in `titleMedium`, wrapping with no `maxLines`.
    2. `timeLeftLabel(estimatedEnd, now)` in `titleLarge`, `colorScheme.primary`.
    3. `in_game`, if `historicalStart` is present.
    4. `weatherLine`, if `weather` is present (both in `bodySmall`, `onSurfaceVariant`).
    5. If `next` is present: a `HorizontalDivider`, then `next_mission` (`bodyMedium`) with the start formatted by `DateFormat.getTimeFormat(context).format(Date.from(expectedStart))`, then its in-game and weather lines.
  - **Clock:** `val now by produceState(Instant.now()) { while (true) { delay(30_000); value = Instant.now() } }`.

- [ ] **Step 3: Integrate into `RosterScreen`.**
  - **List:** in `RosterItems`, the **first** item is `item(key = "mission") { MissionCard(state.mission, Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) }`, placed before `"summary"`. `RosterItems` takes `mission: MissionInfo?` as a new parameter.
  - **Error screen:** in the no-roster error branch, show `MissionCard(state.mission, Modifier.padding(16.dp))` above the `LoadError` content in a `Column`. Roster failures must not hide the card (§4).

- [ ] **Step 4: Update the previews.**
  - `Loaded`: add a sample `mission`, the fixture's values with `estimatedEnd = Instant.now().plus(Duration.ofMinutes(167))`.
  - New previews:
    - `ChangingMission` (`estimatedEnd = Instant.now().minusSeconds(120)`)
    - `LongMissionName` (`Operation Bagration Phase II: Breakthrough at Bobruisk (June 1944)`)
    - `ErrorWithMission`

- [ ] **Step 5: Build.**
  - Run: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`
  - Expected: green, 0 `^w: ` lines.

- [ ] **Step 6: Check the live card on the device** (§7).
  - `installDebug`, launch, screenshot. Fetch the live JSON at the same moment.
  - Expected:
    - The card's mission name and `Next:` name equal the file's.
    - The `Next` time equals `expected_start` in the phone's local time zone.
    - The countdown is within 1 min of `estimated_end − now`.
    - The weather line matches the file's surface layer.
    - The card is above `{N} online`.

- [ ] **Step 7: Check the fallback states on the device with a fake server.**
  - **Fake server** (test-only Node script in the scratchpad, not the repo): serves `GET /mission-info.json` on `127.0.0.1:8765` with a mode switch `GET /mode/<name>`:
    - `past5`: `estimated_end` = now − 5 min
    - `past11`: now − 11 min
    - `failthenok`: the first request returns 500, later ones return the fixture with `estimated_end` = now + 2 h
  - **Setup:** `adb reverse tcp:8765 tcp:8765`, then `installDebug -Pcbonline.missionUrl=http://127.0.0.1:8765/mission-info.json`. The roster stays live.
  - **Expected:**
    - `past5`, after launch: `Changing mission…` is visible.
    - `past11`, after relaunch: `Mission info may be out of date`.
    - `failthenok`, after relaunch: no card at first. After ≤ 70 s the mission name appears **above** `{N} online` **without scrolling** (uiautomator bounds, top of the mission name < top of the summary).
  - **Teardown:** `adb reverse --remove tcp:8765`, stop the server, reinstall the normal debug build.

- [ ] **Step 8: Check the error screen keeps the card.**
  - Run `installDebug -Pcbonline.apiBaseUrl=http://127.0.0.1:9`, which leaves the mission URL real, then launch.
  - Expected: `Couldn't load players`, Retry, **and** the live mission name visible.
  - Then reinstall the normal debug build.

- [ ] **Step 9: Commit.** Message: `feat: mission card with countdown, weather and next mission`.
