# CB Online — Branding & UI Polish Design

- **Date:** 2026-10-03
- **Status:** Approved in conversation (with mockups); awaiting written-spec review
- **Builds on:** `2026-10-03-cb-online-design.md`, `2026-10-03-mission-info-design.md` (both merged to `main`)

## 1. Purpose

The app works but looks generic. Before the first public release it should look like Combat Box. It adopts the author's own Combat Box brand kit (`C:\Projects\Codex\CombatBoxArt`, the stats-site rebrand) in the approved **direction A, "Operations map"**:
- the wordmark banner as the header;
- the WWII operations map as the background;
- translucent dark panels, Oswald type, and brand red, amber and blue.

**This is a visual pass only.** Data, behaviour and app logic are unchanged.

## 2. Scope

**In:**
- Always-dark brand theme, typography and the Oswald font.
- Header, background and panels.
- Restyled mission card, summary, sections and rows, banners, error and loading states, and About dialog.
- Launcher icon background.
- Window background and system-bar icons.

**Out:**
- A light theme. The app is always dark, regardless of the phone's setting.
- Bundling Inter (body text uses the system font, Roboto).
- A splash-screen animation, home-screen widget, or any logic or data change.

## 3. Brand tokens

All colours live in `ui/theme/Theme.kt` as `object CbColors`.

| Token | Value | Used for |
|---|---|---|
| `Background` | `#161817` | Window background, status-bar area fallback |
| `Panel` | `#1C1F1E` | Opaque panel (sticky headers, dialogs) |
| `PanelTranslucent` | `#1C1F1E` at 94 % alpha | Cards and list panels over the map |
| `PanelBorder` | `#FFFFFF` at 11 % alpha | 1 dp panel border, dividers |
| `Text` | `#F3F5F2` | Primary text |
| `Muted` | `#AEB7B3` | Secondary text, labels |
| `Red` | `#DF392D` | Brand strip on the mission card, Retry button fill |
| `RedDeep` | `#A91F18` | "Couldn't refresh" strip background |
| `Amber` | `#D4AA5D` | Countdown, stars, Friends edge, text buttons, spinner, update-banner rule |
| `Sky` | `#7BC4EE` | Links |
| `Axis` | `#3D9ED8` | Axis fills **and** text |
| `Allied` | `#D14F43` | Allied fills only (bar, header edge) |
| `AlliedText` | `#E8695E` | Allied text. `#D14F43` is only about 3.9:1 on `Panel`. |
| `Unassigned` | `#8B949E` | Unassigned fills and text |
| `StarOff` | `#6D7471` | Outline (unstarred) star |

Side mapping is unchanged: **Axis = blue, Allied = red** (the user confirmed it).

**Material scheme.** `CbOnlineTheme` always uses one `darkColorScheme` and ignores `isSystemInDarkTheme()`:

| Role | Token |
|---|---|
| `primary` / `onPrimary` | `Red` / white |
| `secondary` / `onSecondary` | `Amber` / `Background` |
| `tertiary` | `Sky` |
| `background`, `surface` | `Background`, `Panel` |
| `surfaceContainerHigh`, `secondaryContainer` | `Panel` |
| `onBackground`, `onSurface` | `Text` |
| `onSurfaceVariant` | `Muted` |
| `outline` | `PanelBorder` |
| `error` / `onError` | `RedDeep` / white |

**Typography.**
- Oswald is a single variable font file, `res/font/oswald.ttf` (`Oswald[wght].ttf` from `github.com/google/fonts`, `ofl/oswald/`, SIL OFL 1.1). It is used through Compose `FontVariation` weights 400, 500 and 600.
- Its licence text ships as `app/src/main/assets/licenses/OFL-Oswald.txt`.
- Material `display*`, `headline*`, `title*` and `label*` styles use Oswald. `body*` styles keep the system font.

| Element | Style |
|---|---|
| Mission name | Oswald 500, 19 sp |
| Countdown | Oswald 600, 26 sp, `Amber` |
| Player count number | Oswald 600, 40 sp |
| Brand labels | Oswald 500, 11 sp, uppercase, 0.08 em tracking, `Muted` |
| Section headers | Oswald 500, 13 sp, uppercase, 0.08 em tracking |
| Next-mission name | Oswald 400, 15 sp |
| Error title | Oswald 500, 18 sp, uppercase |
| Buttons and text buttons | Oswald 500, uppercase |
| Player names | Body, 14 sp |
| Details (weather, in-game, updated) | Body, 12 sp, `Muted` |

## 4. Screens

All of these match the approved mockups (`design-states.html`, four phones).

**Frame**
- **Map background:** `cb_map.png` fills the whole window behind everything (`ContentScale.Crop`). It is fixed and does not scroll.
- **Header:** `BrandHeader` pins the wordmark banner `cb_wordmark.png` (`ContentScale.FillWidth`) to the top, below the status-bar inset. The status-bar area shows the map. The ⋮ overflow button (`Text` tint, content description `More options`) sits over the banner's top-right and opens About.
- **System bars:** edge-to-edge, with light icons forced on both bars via `SystemBarStyle.dark(TRANSPARENT)`, whatever the phone's mode.

**Pinned strips** (between the header and the list, unchanged from v1 behaviour):
- **Update banner:** a `Panel` strip with a 1 dp `Amber` bottom rule. It shows `Version %1$s available` in Oswald uppercase, with text buttons **Download** (`Amber`) and **Dismiss** (`Muted`).
- **Stale strip:** `RedDeep` background, white body text, `Couldn't refresh — showing %1$s`.

**List** (`LazyColumn`, 10 dp side padding, items in this order):
1. **Mission card.** A `PanelTranslucent` panel with a 3 dp `Red` top strip, holding:
   - the label `Current mission`;
   - the name;
   - the countdown;
   - details (`In-game: …`, weather line);
   - a divider;
   - the label `Next · {local time}`;
   - the next name;
   - next details.

   The 1 dp empty-state spacer rule from the mission spec still applies; the spacer carries no padding.
2. **Summary card.** A `PanelTranslucent` panel holding:
   - the number (Oswald 600, 40 sp) and the label `Pilots online` on one baseline;
   - the 8 dp balance bar (`Axis`, `Allied`, `Unassigned` fills);
   - `14 Axis · 9 Allied` in Oswald uppercase, with each side in its text colour (` · N unassigned` when present);
   - the line `Friends: 1 of 1 online · Updated 21:59`, or only `Updated 21:59` when nothing is starred.
3. **"Nobody's flying right now"** when the total is 0: an Oswald uppercase caption in `Muted`, centred.
4. **Sections:** Friends, Axis, Allied, then Unassigned (unchanged rules). Each section renders as **one panel**:
   - **Header (sticky):** `Panel` (opaque) background, with a 3 dp left edge in the section colour (Friends `Amber`, sides their fill colour). The title is in the section's text colour (Friends `Amber`). It has rounded top corners (8 dp).
   - **Rows:** `PanelTranslucent` background. Rows keep the v1 content: name, optional side tag in Oswald 11 sp uppercase in the side's text colour, time in `Muted`, and a star in `Amber` or `StarOff`.
   - **Last row:** rounded bottom corners (8 dp).
   - **Spacing:** 10 dp between section panels.

**No player list** (loading or failed): the mission card, when present, sits above a centred area that holds either:
- an `Amber` `CircularProgressIndicator` while loading; or
- the error: `Couldn't load players` (Oswald 500, 18 sp, uppercase), the detail (`Muted`), and a **Retry** button (`Red` fill, white Oswald uppercase).

**About dialog** (`Panel` background):
- the cube logo (68 dp);
- `CB Online` (Oswald, uppercase);
- `Version %1$s`;
- `Player data: il2statsapi.combatbox.net`;
- **`Mission data: campaign-data.combatbox.net`** (new);
- the repo link in `Sky`, underlined;
- **Close** (`Amber`).

**Launcher icon:** the background changes from `#0E1B33` to `#161817`.

**Window:** `Theme.CbOnline` in `values/themes.xml` sets `android:windowBackground` to `#161817`, so there's no white flash at launch. `values-night/themes.xml` is removed.

**Copy changes** (string resources stay in sentence case; brand labels are uppercased in code with `uppercase(Locale.ROOT)`):
- **New:**
  - `current_mission` = `Current mission`
  - `next_label` = `Next · %1$s`
  - `pilots_online` = `Pilots online`
  - `about_mission_data` = `Mission data: campaign-data.combatbox.net`
- **Removed:** `next_mission` (`Next: %1$s at %2$s`) and `online_count` (`%1$d online`). They are replaced by the label-and-name and number-and-label layouts above.
- **Unchanged:** all other copy, and all accessibility labels.

## 5. Architecture

| Unit | New or changed | Responsibility |
|---|---|---|
| `ui/theme/Theme.kt` | changed | `CbColors`, `CbTypography` (Oswald), always-dark `CbOnlineTheme`. Keeps `StarColor` and `Coalition.color()` as aliases onto `CbColors`, so existing callers compile. Adds `Coalition.textColor()`. |
| `ui/brand/BrandHeader.kt` | new | Wordmark banner, overflow menu and About dialog trigger. |
| `ui/brand/MapBackground.kt` | new | Fixed full-screen map image. |
| `ui/brand/Panel.kt` | new | `Modifier.panel()` for card panels, plus `Modifier.panelSegment(position)` (`First`, `Middle`, `Last`, `Only`) for section headers and rows, with the right corners, translucency and border. |
| `ui/brand/BrandLabel.kt` | new | Uppercase Oswald label composable. |
| `ui/RosterScreen.kt` | changed | Map background, brand header, sections as segmented panels, restyled no-roster area. |
| `ui/MissionCard.kt`, `SummaryCard.kt`, `PlayerRow.kt`, `UpdateBanner.kt`, `AboutDialog.kt` | changed | Restyled per §4. Content and logic are unchanged. |
| `MainActivity.kt` | changed | `enableEdgeToEdge(SystemBarStyle.dark(…), SystemBarStyle.dark(…))`. |
| `res/` | changed | `drawable-nodpi/cb_wordmark.png`, `drawable-nodpi/cb_map.png`, `font/oswald.ttf`, strings, `colors.xml` (launcher background), `themes.xml`; `values-night/themes.xml` deleted. |
| `assets/licenses/OFL-Oswald.txt` | new | Font licence. |

## 6. Testing

**JVM, test-first:** `ThemeContrastTest` is rewritten for the brand palette.
- **Text colours on `Panel`, each at least 4.5:1:** `Text`, `Muted`, `Amber`, `Axis`, `AlliedText`, `Unassigned`, `Sky`.
- **On-fill text, at least 4.5:1:**
  - white on `Red` (Retry);
  - white on `RedDeep` (stale strip);
  - `Text` on `Panel` (update banner).
- **Material scheme** colours used as text: `onSurface`/`surface`, `onSurfaceVariant`/`surface`, `secondary`/`surface` (amber text buttons) and `tertiary`/`surface` (links), each at least 4.5:1.

The existing 80 tests remain unchanged and must pass. Compose previews are updated for every state.

**On device** (Pixel, via adb; screenshots compared with the mockups):
1. **Main screen, live data:** header, map, panels, Oswald, colours.
2. **Mid-scroll:** a sticky section header is opaque, with no rows visible through it.
3. **Error state** (`-Pcbonline.apiBaseUrl=http://127.0.0.1:9`): mission card above the Oswald error and red Retry.
4. **Stale strip:** fake roster through `adb reverse`, first request OK then 500.
5. **About dialog:** opened through ⋮.
6. **Launcher icon** in the app drawer.
7. **Release build** (`-Pcbonline.debugSignRelease=true`): installs, shows the font, images and live mission and roster data under R8 and resource shrinking. Report the APK size (expected about 2.5 MB).
8. **Always dark:** with the user's consent, `adb shell cmd uimode night no`, then screenshot. Expected: still the dark brand look, with light status-bar icons. Restore with `cmd uimode night yes`.

**Not on device:** the update banner, because no GitHub release exists yet. It is covered by previews.

## 7. Risks

- **APK size:** about 1.4 MB → 2.5 MB (images plus font). Acceptable for a sideloaded APK.
- **Readability over the map:** text sits on 94 %-opaque panels, so the map never sits directly behind small text. The contrast test uses the opaque panel colour, and the map is darker than the panel, so real contrast is equal or better.
- **Resource shrinking:** the images and font are referenced from code, so `shrinkResources` keeps them. Device check 7 confirms this.
