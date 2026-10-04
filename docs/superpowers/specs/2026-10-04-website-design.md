# CB Online website: design

Date: 2026-10-04 · Status: approved in conversation, awaiting written-spec review

## Purpose

A branded, shareable landing page for CB Online, published with GitHub Pages at
`https://riaanjutte.github.io/CBOnline-Android/`. It's the link the maintainer posts in Discord and on the
Combat Box forums instead of the GitHub repo page.

**Audience:** Combat Box players, most of them opening the link on an Android phone.

**Success:** a player who opens the link on their phone understands what the app is, downloads it in one or
two taps, and finds all the install help they need without visiting GitHub.

**Decisions made in conversation**

| Question | Decision |
|---|---|
| What is the site for? | A landing page only. No live data on the site (the player API blocks browsers; the mission feed would allow it, but it isn't wanted). |
| How much content? | Everything a player needs: hero, download, screenshots, features, install (Play Protect and developer verification), updating, FAQ, credits. Install help therefore lives in both the README and the site. |
| How is it built? | Hand-written static files in `site/`, published by a GitHub Actions workflow. No Jekyll, no build tools. |
| Layout | Mockup **A**, "Hero with the app beside it" (headline + download button next to a real screenshot; app-style panels below). |

## Page

One page, `site/index.html`, `lang="en"`, always dark. Sections in order:

1. **Top bar.** The wordmark (links to the top of the page, alt text "Combat Box"). From 600 px wide, the links
   **Features · Install · FAQ** on the right, jumping to `#features`, `#install` and `#faq`. Narrower screens
   show the wordmark only. The bar stays at the top while scrolling (`position: sticky`).
2. **Hero** (`#top`), over the map background.
   - Amber label **For Android**.
   - Headline **Who's flying on Combat Box?**
   - Intro: *See who's online, the current mission and how long it has left, and your friends at the top of
     the list. Before you start the sim.*
   - Red button **Download for Android** (see "Download button").
   - The release line under it (see "Download button"), followed by an **Install help** link to `#install`.
   - The main screenshot (`screenshots/main.png`). From 760 px wide it sits to the right of the text; narrower,
     it sits below the button.
3. **Features** (`#features`). Six app-style panels (panel colour, thin border, red strip on top), each an
   Oswald label and one or two lines. Same facts as the README's "What it shows":
   - **Who's online:** the pilot count, the Axis / Allied split, and both side lists with time in mission.
   - **The current mission:** its name, a countdown to the end, the in-game date and time, and the weather.
   - **What's next:** when the next mission starts, in your phone's time, with its date and weather.
   - **Your friends:** star a name to pin that pilot at the top; shown as offline when they aren't flying.
   - **Fresh data:** refreshes every minute while open; pull down to refresh straight away.
   - **Update notices:** the app tells you when a new version is out.
   Panels sit in a grid: three columns from 900 px, two from 600 px, one below.
4. **Screenshots.** `main.png`, `lists.png`, `about.png` side by side (wrapping on narrow screens), each with
   a short caption ("Mission and pilots", "Friends at the top", "About"), then the line *The pilot names in
   these screenshots are made up.*
5. **Install** (`#install`).
   1. On your phone, tap **Download for Android** above. If GitHub's release page opens instead, tap
      **CBOnline-&lt;version&gt;.apk** under **Assets**.
   2. Open the downloaded file from the notification or your Downloads folder.
   3. If Android says your browser isn't allowed to install apps, tap **Settings**, turn on **Allow from this
      source**, then go back.
   4. Tap **Install**.

   Then the Play Protect paragraph and *Only download CB Online from this site or its GitHub releases page.*,
   with the same wording as the README apart from that sentence. Then a collapsed `<details>` block, **If
   Android says the developer is unverified**, holding the README's developer verification text and steps
   (same facts, same links to Google's pages).
6. **Updating.** The README's "Updating" text: the *Version x.y.z available* notice, **Download** and
   **Dismiss**.
7. **FAQ** (`#faq`). The README's seven questions, each a `<details>` element (question in `<summary>`), all
   collapsed by default.
8. **Footer.** Data sources (il2statsapi.combatbox.net, campaign-data.combatbox.net), "Combat Box logo,
   wordmark and map artwork used with permission", the Oswald credit linking to `assets/OFL-Oswald.txt`, and
   links to the GitHub repo, the issues page and `DEVELOPMENT.md` on GitHub.

**Copy rule:** the site makes no claim the README doesn't. The wording is the README's, tightened for a web
page. When install help or the FAQ changes in one, the other is updated in the same change (DEVELOPMENT.md
says so).

### Look

- Colours from the app (`CbColors`): background `#161817`, panel `#1C1F1E` at 94 %, panel border white at
  11 %, text `#F3F5F2`, muted `#AEB7B3`, red `#D2352A` (button, strips), amber `#D4AA5D` (labels, focus),
  sky `#7BC4EE` (links).
- Oswald (self-hosted) for the headline, labels, section headings and buttons; the system UI font stack for body
  text (Roboto on Android, as in the app).
- The map sits behind the hero only, under a dark gradient. The rest of the page is the plain background.
- No animations. Anchor links scroll smoothly only when `prefers-reduced-motion` isn't set.

### Head

- `<title>`: **CB Online: who's flying on Combat Box**
- `meta description`: the intro sentence.
- Favicons: `favicon.ico`, `favicon-16x16.png`, `favicon-32x32.png`, `apple-touch-icon.png` (the cube, from the
  maintainer's CombatBoxArt set).
- `theme-color` `#161817`, viewport meta.
- Link preview: `og:type` website, `og:title`, `og:description`, `og:url`
  (`https://riaanjutte.github.io/CBOnline-Android/`), `og:image` (absolute URL of `assets/social-card.jpg`),
  `og:image:width` 1200, `og:image:height` 630, `og:image:alt`, and `twitter:card` `summary_large_image`.

### Accessibility

- Alt text on every image. The screenshots' alt text says what each shows.
- Body text reaches 4.5:1 contrast on what it sits on. For text over the map, that's checked against the
  brightest part of the map under the hero gradient, not just the panel colour.
- Visible keyboard focus (amber outline) on links, the button and FAQ summaries.
- Headings in order: one `h1` (the headline), `h2` per section.

### Privacy

No analytics, cookies, trackers or third-party fonts. The only request beyond the site's own files is the
visitor's browser asking `api.github.com` for the latest release.

## Download button

The site never needs editing for a new release.

- **Static fallback**, in the HTML: the button links to
  `https://github.com/riaanjutte/CBOnline-Android/releases/latest`, and the release line reads
  **Latest version on GitHub · Android 8.0 or newer**. This is what visitors without JavaScript get, and what
  stays if anything below fails.
- **Normal case:** on load, `main.js` fetches
  `https://api.github.com/repos/riaanjutte/CBOnline-Android/releases/latest` (header
  `Accept: application/vnd.github+json`, 5-second timeout). If `release.js` returns a result, the button's
  `href` becomes the APK's download URL and the release line becomes
  **Version {version} · {size} MB · Android 8.0 or newer**, e.g. *Version 1.0.0 · 2.3 MB · Android 8.0 or
  newer*.
- **Any failure** (network error, timeout, non-OK status including GitHub's 60-requests-an-hour limit, bad
  JSON, no APK) leaves the fallback in place. No error is shown, and nothing is logged to the console.

`release.js` is an ES module with no DOM access:

- `pickApk(release)` returns `{ version, url, sizeLabel }` or `null`.
  - `version`: `tag_name` with one leading `v`/`V` removed. Missing or empty → `null`.
  - The asset: the first entry of `assets` whose `name` ends in `.apk` (case-insensitive) and has a
    `browser_download_url`. None → `null`.
  - `sizeLabel`: `size` in decimal megabytes with one decimal place (`2321394` → `"2.3"`), as Android shows
    sizes; at least `"0.1"`. A missing, zero or non-numeric size → `null` for the whole result.
  - Anything that isn't an object, or has no `assets` array → `null`. It never throws.
- `releaseLine(result)` returns the release line text for a result, or the fallback text for `null`.

`main.js` is the only file that touches the page: it finds the button and the release line, calls the API,
and applies `pickApk` / `releaseLine`.

## Files

```
site/
  index.html
  style.css
  release.js
  main.js                    loaded with <script type="module">
  screenshots/               main.png, lists.png, about.png (moved here from docs/screenshots/)
  assets/
    wordmark.webp            from cb_wordmark.png, 960 px wide
    map.webp                 from cb_map.png, 1600 px wide
    social-card.jpg          1200 × 630 link preview
    favicon.ico, favicon-16x16.png, favicon-32x32.png, apple-touch-icon.png
    oswald.ttf               copied from app/src/main/res/font/
    OFL-Oswald.txt           copied from app/src/main/assets/licenses/
tests/site/release.test.mjs  Node test runner tests for release.js (not published)
.github/workflows/pages.yml
```

- **Screenshots move** from `docs/screenshots/` to `site/screenshots/`, and the README's image paths change to
  match, so each screenshot exists once.
- **Social card:** made with ImageMagick from the map (darkened), the wordmark, the headline in Oswald and the
  main screenshot. JPEG, under 300 KB.
- **All paths relative**, so everything works under `/CBOnline-Android/`.
- The font ships as TTF (172 KB). WOFF2 would need a Python package installed, so it's left out.

## Publishing

**Workflow `.github/workflows/pages.yml`, "Publish website":**

- Triggers: pushes to `main` that touch `site/**`, `tests/site/**` or the workflow file, plus `workflow_dispatch`.
- Permissions: `contents: read`, `pages: write`, `id-token: write`. Concurrency group `pages`, not cancelling a
  run in progress.
- Job `test`: checkout, set up Node 24, `node --test "tests/site/*.test.mjs"`.
- Job `deploy` (needs `test`, environment `github-pages`): checkout, configure Pages, upload `site/` as the Pages
  artifact, deploy. Uses the current major versions of `actions/checkout`, `actions/setup-node`,
  `actions/configure-pages`, `actions/upload-pages-artifact` and `actions/deploy-pages`, checked at
  implementation time.
- Only `site/` is published. `docs/` (specs and plans) and everything else stays off the site.

**One-time GitHub settings.** Each needs the maintainer's OK before it's run:

1. Before the first push: turn on Pages with GitHub Actions as the source
   (`gh api -X POST repos/riaanjutte/CBOnline-Android/pages -f build_type=workflow`).
2. Once the site is live: set the repo's Website field to the site URL (`gh repo edit --homepage ...`).

**Repo docs on the same branch**

- README: a link to the website near the top, and the new screenshot paths.
- DEVELOPMENT.md: a "Website" section covering previewing locally (`python -m http.server -d site 8000`),
  running the tests (`node --test "tests/site/*.test.mjs"`), how publishing works, and the rule that the README and the
  site change together.

## Testing and verification

**Automated:** `tests/site/release.test.mjs`, written before `release.js` and run with `node --test "tests/site/*.test.mjs"`:

- A normal release → version `1.0.0`, the APK URL, size label `2.3`.
- `tag_name` without a `v`, and with `V`.
- No `.apk` asset, an empty `assets` array, and no `assets` at all → `null`.
- Several assets with the APK not first → that APK. `.APK` in capitals is accepted.
- Sizes: 2,321,394 → `2.3`; 1,000,000 → `1.0`; 40,000 → `0.1`; 0, missing or a string → `null`.
- `null`, a string, an array, or an object missing `tag_name` → `null`.
- `releaseLine` for a result and for `null`.

The workflow runs the same tests before every publish.

**In a browser, served locally** (`python -m http.server`, checked in the in-app browser at 375 × 812 and at
desktop width):

- Layout A as approved. No horizontal scrolling at 375 px. All images and the Oswald font load. No console
  errors.
- The top-bar links jump to their sections. FAQ answers and the developer verification block open and close.
  Keyboard focus is visible.
- With the real API, the button links to `CBOnline-1.0.0.apk` and shows *Version 1.0.0 · 2.3 MB · Android 8.0
  or newer*.
- With the API call made to fail, the button keeps the release-page link and the fallback line, with no error.
- Contrast of the hero text against the brightest part of the map behind it.

**After publishing:**

- The site URL and every file it loads return 200 (HTML, CSS, both scripts, images, font, social card).
- The button's APK link resolves (a redirect check, without downloading the file).
- The live site opened on the maintainer's phone via adb and screenshotted. Download isn't tapped.
- A fresh whole-branch review before merge.

**Not checked:** how the link preview looks in Discord, which would need a message posted.

## Out of scope

- Live data on the site (who's online, the mission).
- A custom domain.
- Changing the app's About link, which still points to GitHub.
- WOFF2 font conversion.
- Translations.
