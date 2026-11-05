# FREQ Home 0.3.0 validation

Checked on 2026-11-05 against live Home Base saved version 22, source commit 774171cf8c2f02df2071ef22c5462545ca363359.

- Frontend TypeScript check and Vite production build pass. npm lockfile regenerated from registry packages, removing broken links to the old local sandbox.
- Android signed release build and full release lint pass with no errors. Remaining lint warnings cover JavaScript usage, listener preconditions, backup policy, icon formatting and untranslated prototype text.
- Full lint exposed two older compatibility issues: an API-27 theme attribute in the API-26 base resource and a persisted folder permission constant. Removed the unnecessary light-navigation attribute and now explicitly require the picker read grant before persisting it.
- Browser smoke check at 360px: live Grid/Graph/Area Tree entry points render; seven-column dock spans available width; middle Apps opens the drawer; System Lab exposes the live Home preference; 12-hour time remains.
- Native bridge stays restricted to bundled local main-frame origin. Separate live activity is non-exported and exposes no page-to-native JavaScript bridge. Remote adapter runs only on the exact Home Base origin and uses existing site navigation/layout events; it makes no data mutations itself.
- APK signature and package/version inspected separately during delivery. Notification-listener code/declarations remain absent. Keys, private dashboard snapshots and site credentials are excluded from Git.

Device-only checks remain: Samsung Play Protect installation, default-Home role, native app launch, embedded ChatGPT sign-in and actual Notion reads/writes. Cloud browser reached the live site's normal sign-in gate; no signed-in device/session was available to test the embedded flow. Browser fallback uses its own session and does not sign in the WebView. No phone-side success is claimed.

Automatic compatible APK release publishing still needs the original signing key configured privately in Actions. Without that secret the workflow emits a temporary-key CI test artifact only.
