# FREQ Home 0.3.1 validation

Source diagnosis: 0.3.0 allowed OpenAI login inside a WebView but opened external provider redirects in the browser. That transfers a partial login transaction without its originating cookie jar. User screenshot shows auth.openai.com/error in Chrome, client_id_not_found_in_session. The broken handoff is confirmed in source; causality on the phone remains inferred.

0.3.1 routes known external login providers to a fresh Home Base root in the browser, adds BROWSER SIGN-IN and recognizes the auth error page. It does not claim that browser sign-in authenticates the embedded view. No tokens, cookies or login codes are copied across clients.

Frontend check/build, Android release build/lint and original signer verification run before delivery. Phone-only validation remains required for actual login and installation. Prior 360px UI and native drawer smoke checks are unchanged.
