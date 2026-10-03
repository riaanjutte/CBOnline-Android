# CB Online — Mission Info Design

- **Date:** 2026-10-03
- **Status:** Approved in conversation; awaiting written-spec review
- **Builds on:** `2026-10-03-cb-online-design.md` (v1, merged to `main`)

## 1. Purpose

Answer "is it worth flying right now?" more completely. Alongside the player count, show:

- the current mission and how long it has left;
- the next mission and when it starts;
- the weather and in-game date/time for both.

The same server covers the roster and this file: the user confirmed `tempest` is the server `/api/OnlinePlayers` lists.

## 2. Scope

**In:** a mission card at the top of the roster screen, fed by the mission-info file and refreshed in the existing 60-second foreground loop.

**Out:**
- Upper-altitude winds, turbulence and pressure.
- Notifications (e.g. "mission ends in 10 min") and the home-screen widget.
- Other servers.
- Imperial units.

## 3. Data source

`GET https://campaign-data.combatbox.net/mission-info-tempest.json`. Public JSON served via Cloudflare (`access-control-allow-origin: *`, about 2.7 KB). This is the source the user specified; it is not scraped.

Observed shape (2026-10-03), with the fields this feature uses:

| Path | Example | Used for |
|---|---|---|
| `mission.name` | `Crimean Resolve (Dec. 1944)` | Card title |
| `mission.historical_start` | `1944-12-02T14:00:00` (no zone, in-game time) | In-game line |
| `mission.estimated_end` | `2026-10-04T00:42:00Z` | Countdown (required) |
| `weather.temperature_c`, `cloud_cover`, `cloud_base_m`, `cloud_top_m`, `precip_level`, `precip_type` | `1`, `"Heavy"`, `1900`, `4900`, `0`, `null` | Weather line |
| `weather.wind_layers[]` | 5 layers: `{altitude_m, direction_from_deg, speed_ms, …}` at 0/500/1000/2000/5000 m | Surface wind = lowest `altitude_m` |
| `next_mission.name`, `expected_start`, `historical_start`, `weather` | `Mitchell's Men (Mar. 1945)`, `2026-10-04T00:42:00Z`, … | Next block |

Also present but unused: `generated_at`, `server`, `mission.started_at`, `pressure_*`, `turbulence`, `next_mission.path`, `weather_state` and `artifact_sha256`.

**Rotation behaviour** (observed at 21:41:40Z): the mission changed about 20 s after `estimated_end`, and the file's `Last-Modified` updated at that moment. Missions run about 3 h. Cloud-cover values seen so far are `Medium` and `Heavy`; the full vocabulary is unknown.

## 4. User experience

**Placement:** the mission card is always the **first item** of the roster list (key `mission`), above the player summary. It is present even while there is no mission data, when it renders an invisible **1 dp** spacer rather than zero height. A zero-height first item doesn't count as visible, so the list would anchor to the summary card and push the card off-screen when it fills in. With the spacer, content never gets inserted above the anchored item, which avoids the off-screen insertion bug fixed in v1.

**Card layout** (exact copy):

```
{mission.name}
{time-left label}
In-game: {d MMM yyyy, HH:mm}
{weather line}
──────────
Next: {next.name} at {local start time}
In-game: {d MMM yyyy, HH:mm}
{weather line}
```

**Time-left label.** `remaining` is `estimated_end − now`, using the device clock and whole minutes rounded down.

| Condition | Label |
|---|---|
| remaining ≥ 1 h | `{h} h {mm} min left` (e.g. `2 h 47 min left`, `2 h 05 min left`) |
| 1 min ≤ remaining < 1 h | `{m} min left` (e.g. `47 min left`) |
| 0 < remaining < 1 min | `Less than a minute left` |
| −10 min ≤ remaining ≤ 0 | `Changing mission…` |
| remaining < −10 min | `Mission info may be out of date` |

**Countdown refresh:** the card recomputes the label every 30 s while it is on screen. This uses a local clock; it does not fetch.

**Weather line:**
- **Format:** `{temp} °C · {cover} cloud {base}–{top} m · Wind {from}° {speed} m/s`.
- **Clear sky:** if `cloud_cover` is null, blank or `Clear` (any case), the cloud part reads `Clear sky`, with no heights.
- **Precipitation:** if `precip_level > 0`, append ` · {Precip}`, where `{Precip}` is `precip_type` with a capital first letter, or `Precipitation` if the type is null.
- **No wind layers:** the wind part is omitted.
- **Numbers:**
  - Temperature and wind speed show no decimals when whole, otherwise one decimal (e.g. `-15 °C`, `3.5 m/s`).
  - Heights use thousands grouping that doesn't depend on the phone's locale (`1,900`).
  - The wind direction is a whole number of degrees.

**In-game line:** `In-game: 2 Dec 1944, 14:00`, using English month abbreviations. The line is omitted if `historical_start` is missing or unparseable.

**Next start:** shown in device-local time, with the same formatter as the existing `Updated` line. The whole Next block is omitted if `next_mission` is missing.

**States:**
- Mission never loaded (loading or failed): the card renders nothing.
- A later mission fetch fails: the last good mission stays on screen and its countdown keeps running.
- Mission failures never show the roster's `Couldn't refresh` strip and never block the roster. Roster failures never hide the card.
- Pull-to-refresh refreshes both the roster and the mission file.

## 5. Architecture

| Unit | New or changed | Interface |
|---|---|---|
| `data/MissionInfo.kt` | new | `MissionInfo(name: String, historicalStart: LocalDateTime?, estimatedEnd: Instant, weather: Weather?, next: NextMission?)`; `NextMission(name: String, expectedStart: Instant, historicalStart: LocalDateTime?, weather: Weather?)`; `Weather(temperatureC: Double?, cloudCover: String?, cloudBaseM: Int?, cloudTopM: Int?, precipLevel: Double, precipType: String?, surfaceWind: Wind?)`; `Wind(fromDeg: Int, speedMs: Double)`; `interface MissionSource { suspend fun fetch(): MissionInfo }` |
| `data/MissionInfoApi.kt` | new | `class MissionInfoApi(client, json, url, userAgent, log = {}) : MissionSource`. Throws on non-2xx, unparseable JSON, a missing `mission`, or a missing or unparseable `estimated_end`. Logs `fetch MissionInfo`. |
| `roster/MissionText.kt` | new | `fun timeLeftLabel(estimatedEnd: Instant, now: Instant): String`, `fun weatherLine(weather: Weather): String`, `fun inGameLabel(start: LocalDateTime): String` |
| `ui/RosterViewModel.kt` | changed | Constructor gains `missions: MissionSource` (after `players`). `RosterUiState` gains `mission: MissionInfo? = null`. Each fetch runs both sources concurrently, each with its own try/catch that rethrows cancellation. The last good mission is kept in its own `MutableStateFlow`. Roster flags (`refreshFailed`, `errorMessage`, `isLoading`, `isRefreshing`) stay roster-only. |
| `ui/MissionCard.kt` | new | `@Composable fun MissionCard(mission: MissionInfo?)`, with an internal 30-second clock via `produceState`. |
| `ui/RosterScreen.kt` | changed | First `LazyColumn` item is `item(key = "mission") { MissionCard(state.mission) }`. |
| `AppContainer.kt`, `app/build.gradle.kts` | changed | Wire `MissionInfoApi`. Add `BuildConfig.MISSION_URL`: the real URL in release, and in debug overridable with `-Pcbonline.missionUrl`, mirroring `cbonline.apiBaseUrl`. |
| `ui/RosterPreviews.kt` | changed | Add mission data to Loaded; add a `ChangingMission` preview. |

**Data flow:**
1. The poll loop (unchanged timing) calls `fetchOnce`, which runs `players.fetch()` and `missions.fetch()` concurrently. `inFlight` covers both; `fetchOnce` returns when both have finished.
2. Each source updates its own state **as soon as its fetch completes**, so a slow mission file never delays the roster. Roster results update `fetchState` as today. Mission success updates `missionState`; mission failure leaves it unchanged.
3. `combine(fetchState, friends, updateState, missionState)` produces `RosterUiState`.
4. `MissionCard` renders the state and recomputes the time-left label from `estimatedEnd` and its local clock.

## 6. Error handling

- Mission fetch failures (network, HTTP, JSON, missing required fields) are swallowed in the ViewModel, apart from cancellation, and the last good mission is kept. There is no user-facing error.
- Optional fields degrade gracefully, as described in §4.
- Device clock skew is ignored: precision is whole minutes, and phones are network-time synced.

## 7. Testing

**JVM unit tests, written test-first:**
- `MissionInfoApi` against MockWebServer, using the full real-shape payload with all 5 wind layers:
  - Parses to the expected model.
  - Picks the lowest-altitude layer as surface wind even when layers are out of order.
  - A missing `weather` or `next_mission` gives null.
  - Reads `precip_level` and `precip_type`.
  - Missing `mission` throws; HTTP 500 throws; an HTML body throws.
  - Sends the User-Agent.
- `MissionText`:
  - Every time-left condition in §4, including the boundaries at exactly 1 h, 1 min, 0 and −10 min.
  - Weather lines: full, clear sky, null cover, precipitation with and without a type, no wind, negative and fractional values, and thousands grouping.
  - `inGameLabel`: `2 Dec 1944, 14:00`.
- `RosterViewModel`:
  - The mission is present after the first load.
  - Each poll and each manual refresh fetches each source exactly once.
  - A mission failure keeps the roster and the previous mission.
  - A roster failure keeps the mission.
  - A first-load mission failure still shows the roster with `mission == null`.

**On device (Pixel, via adb):**
- The card's mission name, next-mission name and next start time match a simultaneous fetch of the live file. The countdown is consistent with `estimated_end`.
- Using the debug `cbonline.missionUrl` hook pointed at a local fake server through `adb reverse`:
  - a past `estimated_end` shows `Changing mission…`, and an end more than 10 minutes past shows `Mission info may be out of date`;
  - a first mission fetch that fails, followed by one that succeeds, makes the card appear at the top **without scrolling**.

## 8. Risks and limitations

- The file name is fixed to `tempest`. If Combat Box renames or splits servers, the card stops updating; it shows the last data with the "may be out of date" label.
- The schema is owned by Combat Box. A breaking change hides the card (on first load) or freezes it on the last good data.
- `cloud_cover` vocabulary beyond `Medium`/`Heavy` is unknown and is shown verbatim.
