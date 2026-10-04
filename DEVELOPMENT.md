# CB Online: development

How to build, test and release the app. Player-facing docs are in the [README](README.md).

An Android companion app for [Combat Box](https://combatbox.net) (IL-2 Great Battles) players. It shows who's online right now, the player count and side balance, and your starred friends pinned at the top, so you can decide whether it's worth starting the sim. Player data comes from the public `il2statsapi.combatbox.net` API. The app refreshes every 60 seconds while it's on screen. In the background it only runs what the user switches on: the mission reminder (an alarm) and friend alerts (a WorkManager check about every 15 minutes).

Requires Android 8.0 (API 26) or newer.

## Building

Install Android Studio (it provides the SDK and a JDK), then:

1. Point `JAVA_HOME` at Android Studio's bundled JDK:

   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   ```

2. Create `local.properties` (git-ignored) with your SDK path, for example:

   ```properties
   sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
   ```

3. Build and install on a connected device:

   ```powershell
   .\gradlew.bat :app:installDebug
   ```

   Debug builds install as a separate app, **CB Online (dev)** (`io.github.riaanjutte.cbonline.debug`), next to the released app. Testing never touches your real install or its starred friends.

## Tests

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

## Creating the release key (once, by the maintainer)

1. Generate the key. Pick the passwords yourself and keep them safe:

   ```powershell
   New-Item -ItemType Directory -Force "$env:USERPROFILE\keys" | Out-Null
   & "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -v -keystore "$env:USERPROFILE\keys\cbonline-release.jks" -alias cbonline -keyalg RSA -keysize 4096 -validity 36500
   ```

2. Create `keystore.properties` in the project root. It's git-ignored; never commit it.

   ```properties
   storeFile=C\:\\Users\\<you>\\keys\\cbonline-release.jks
   storePassword=<store password>
   keyAlias=cbonline
   keyPassword=<key password>
   ```

3. **Back up the `.jks` file and both passwords.** Every update must be signed with the same key. If it's lost, players have to uninstall and reinstall to upgrade.

Without `keystore.properties`, release builds come out unsigned and can't be installed. That's deliberate, so a wrongly signed APK can't be published by accident.

## Releasing

1. Bump `appVersion` in `app/build.gradle.kts`. The `versionCode` is derived from it.
2. Build the signed release:

   ```powershell
   .\gradlew.bat :app:assembleRelease
   ```

3. Publish it on GitHub as `CBOnline-<version>.apk`, the name the README tells players to download:

   ```powershell
   Copy-Item app\build\outputs\apk\release\app-release.apk app\build\outputs\apk\release\CBOnline-<version>.apk
   gh release create v<version> app\build\outputs\apk\release\CBOnline-<version>.apk --repo riaanjutte/CBOnline-Android --title "CB Online <version>"
   ```

The repository must be **public** for the in-app update check to work. The app reads `releases/latest`, so drafts and pre-releases are ignored.

## Debug-only test hooks

These are for local testing. **Never publish an APK built with them.**

- `-Pcbonline.apiBaseUrl=<url>` points a *debug* build at another host. For example, `http://127.0.0.1:9` shows the error screen. Release builds always use the real API.
- `-Pcbonline.missionUrl=<url>` points a *debug* build's mission card at another file (e.g. a local fake). Release builds always use the real file.
- `-Pcbonline.debugSignRelease=true` signs a *release* build with the debug key, so the shrunk (R8) build can be installed and checked on a device.

## Website

The website at <https://riaanjutte.github.io/CBOnline-Android/> is the static files in `site/`, with no build step. The download button asks GitHub's API for the latest release when the page loads, so publishing a release needs no site change.

- Preview it locally, then open <http://localhost:8000/>:

  ```powershell
  python -m http.server 8000 -d site
  ```

- Run its tests (Node 24 or newer):

  ```powershell
  node --test "tests/site/*.test.mjs"
  ```

- Publishing is automatic. A push to `main` that touches `site/`, `tests/site/` or `.github/workflows/pages.yml` runs the **Publish website** workflow, which runs the tests and then publishes `site/` to GitHub Pages. You can also start it by hand from the Actions tab.

The README and the website carry the same install help and FAQ. When you change one, change the other in the same commit.
