# Updates

0.3.0 adds the current live Home Base workspace inside a separate WebView, Graph / Area Tree shortcuts, the center installed app drawer, and an optional live Home landing screen. The offline workspace remains accessible. UI changes to the live site appear on reload without rebuilding the native app.

The main-branch Actions workflow builds Android on each push. It never publishes a differently signed update: without the original FREQ_KEYSTORE_B64 Actions secret it uploads a temporary-key CI test artifact only. That artifact cannot update the existing phone installation. The key is excluded from public source and has not been uploaded to GitHub.

Once the original signer is configured as an Actions secret, the workflow publishes versioned APK and update.json release assets. The in-app updater reads only retr0sdq/freq-home releases, verifies SHA256, package identity, increasing version code and the installed signing certificate, then uses the Android installer confirmation screen. Native app changes need APK installation; site changes do not.

First-time signing configuration remains a separate account setup step. Device installation, sign-in, launcher-role switching and Notion writes require phone validation.

## 0.3.1 login handoff hotfix

External Google/Apple/Microsoft provider navigation restarts Home Base from its root in the browser, retaining one browser session for the entire login. Partially completed WebView transactions are not forwarded to the browser. Adds a visible BROWSER SIGN-IN action and identifies the auth error page. This is a browser fallback fix, not a native authenticated-session exchange; browser mode cannot expose the native app drawer inside the site.
