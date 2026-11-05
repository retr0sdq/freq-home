# Install FREQ Home 0.3.1

Install over the existing launcher without uninstalling to preserve local records.

This hotfix prevents a partly completed external-provider login from being handed from WebView to a browser with a different session. Google/Apple/Microsoft handoffs now restart at the Home Base root in the browser. A visible BROWSER SIGN-IN button also starts there. An auth.openai.com/error page is identified as a session error rather than a workspace loading state.

Complete the sign-in and use the connected workspace in that browser. Browser login does not sign in the embedded WebView. The embedded view can still be used where its authentication completes within the same session; phone validation remains required. Browser mode uses the site's own Graph and Area Tree controls and does not provide the native Apps dock inside that browser page.

If an old auth error tab is open, open a new Chrome tab and navigate to https://farhan-home-base.farhaansadeeq.chatgpt.site directly. Start Continue with ChatGPT there using the usual account method. Do not resume an auth callback copied from the launcher.

No root or Play Protect changes are required. Device installation and authentication acceptance remain unverified.
