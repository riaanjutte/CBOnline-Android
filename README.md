# CB Online

An Android companion app for [Combat Box](https://combatbox.net) (IL-2 Great Battles) players. It shows who's online right now, the player count and side balance, and your starred friends pinned at the top, so you can decide whether it's worth starting the sim. Player data comes from the public `il2statsapi.combatbox.net` API. The app refreshes every 60 seconds while it's on screen and never in the background.

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

3. Publish it on GitHub:

   ```powershell
   gh release create v<version> app\build\outputs\apk\release\app-release.apk --repo riaanjutte/CBOnline-Android --title "CB Online <version>"
   ```

The repository must be **public** for the in-app update check to work. The app reads `releases/latest`, so drafts and pre-releases are ignored.

## Debug-only test hooks

These are for local testing. **Never publish an APK built with them.**

- `-Pcbonline.apiBaseUrl=<url>` points a *debug* build at another host. For example, `http://127.0.0.1:9` shows the error screen. Release builds always use the real API.
- `-Pcbonline.debugSignRelease=true` signs a *release* build with the debug key, so the shrunk (R8) build can be installed and checked on a device.
