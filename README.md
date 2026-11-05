# FREQ Home

Android Home launcher for the Galaxy M31 (SM-M315F/DS), Android 12 / One UI Core 4.1. Version 0.3.1, Android version code 5.

## What changed

Live Home Base runs inside the launcher, with direct Knowledge Graph and pinned Area tree shortcuts. It loads the current published site rather than bundling a stale copy of the connected dashboard. The seven-column, full-width native dock keeps installed Apps in the center and Settings above. The offline launcher, app drawer, local notes/tasks, 12-hour clock and FREQ activity panel remain available.

System Lab → “Open live workspace on Home” makes the live Grid the landing screen. The live view’s LOCAL button returns to the offline launcher even when that preference is enabled. The native APPS button always returns to the installed app drawer.

## Live workspace

URL: https://farhan-home-base.farhaansadeeq.chatgpt.site

Integration inspected against saved site version 22, source commit `774171cf8c2f02df2071ef22c5462545ca363359`.

The live view uses the site's ChatGPT sign-in and its existing server-side Notion/Spotify/PC connections. The WebView has a separate cookie session from the browser; sign in there when asked. A browser fallback is available from the live view menu. Browser sign-in does not transfer cookies to the embedded view. Authentication and Notion edits still require device validation.

Graph supports the site's current canvas, filtering and touch hierarchy controls. Area Tree pins the real Area → projects → tasks view to the live Grid. Those preferences belong to the live site's origin and persist on this device; local launcher tasks do not become Notion records.

Refreshing the live view loads current site updates without rebuilding the APK. Native Android changes still require an APK update. The site remains the source of truth for connected data.

## Build

JDK 17, Android SDK platform 35, Node.js 22.13+ and the included Gradle 8.7 wrapper are required.

```sh
cd web
npm ci
npm run check
npm run build
cd ../android
bash ./gradlew assembleRelease
```

Set ANDROID_HOME or untracked android/local.properties with sdk.dir. Supply the original signing keystore privately at android/debug.keystore. The source repository excludes keys, generated UI, build output and local paths. The regenerated npm lockfile uses registry packages, with no references to the previous local sandbox.

## Security boundaries

The bundled launcher uses WebViewAssetLoader and an origin-scoped, main-frame-only native message listener. Its WebView blocks network navigation. Native app launches are checked against Android's launcher activity list.

The live site runs in a separate, non-exported Activity without a JavaScript-to-native bridge. Its small bundled adapter uses the site's own navigation and layout events only on the exact Home Base HTTPS origin. It does not make direct Notion writes or fabricate connected data. It preserves the site's layout-change guards. File/content URL access and mixed content are disabled; TLS errors are not overridden. External HTTP(S) links open in the browser; arbitrary intent URLs are rejected.

No notification listener, accessibility service, SMS, contacts, root or location access is included. FREQ's activity panel shows only its own alerts and update state.

## Signing and updates

This prototype retains the original test signing identity so compatible APKs can update the installed launcher without uninstalling. The private key is not in the repository or source archive. This is not a production signing identity.

The GitHub workflow validates and builds each main-branch commit. Without the original FREQ_KEYSTORE_B64 Actions secret it creates a temporary CI key and labels the artifact test-only; that artifact cannot update an existing installation. Publishing compatible automatic releases requires separately configuring the original key as a GitHub Actions secret. No key has been uploaded by this change.

The existing in-app updater checks release checksum, package name, strictly higher version code and the installed signing certificate before invoking Android's installer. Play Protect acceptance on the phone has not been verified.

## Login handoff hotfix (0.3.1)

External login-provider redirects now restart the complete flow at the Home Base root in the browser rather than forwarding a partial WebView transaction. BROWSER SIGN-IN is visible above the workspace. Continue using the workspace in that browser; its session does not authenticate the embedded view. The reported session error is consistent with the previous cross-client handoff, but device-side authentication remains unverified.
