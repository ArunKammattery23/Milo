# Prototype verification — 11 October 2026

- Final personal Android APK: version 1.5 (version code 6), SDK 36, Java 17, Gradle 8.14, AGP 8.13.2, ML Kit Prompt API 1.0.0-beta4.
- assembleDebug, lintDebug and assembleDebugAndroidTest passed. Lint warnings for legacy API usage, localization and style remain.
- Android API-35 emulator: **10 tests passed together** with an explicitly selected test class. Checks cover encrypted key setup/save/disconnect for Gemini, OpenAI and Claude, consent gating, invalid key headers, cloud SSE completion and incomplete-stream rejection, Gemini stream/source assembly, local default without automatic cloud fallback, disabled local web search, actual SDK readiness callback, expressions, Off preventing speech/sends, offline screens, renaming Talk layout, floating window creation, continued visibility with the activity in the background, expression persistence and removal when Off is set.
- The legacy InstrumentationTestRunner's dependency-wide discovery timed out at process startup after the SDK addition. Selecting the app test class avoids that timeout: adb shell am instrument -w -e class life.milo.companion.MiloSmokeTest life.milo.companion.test/android.test.InstrumentationTestRunner.
- Final APK signature verified with apksigner. SHA-256: 98660189E9F2499EF33AD783AE5554CDD620F41590FAB0D52B3B25F30F881C28.
- No real provider key was used. Fake test keys were used only for local encryption and removed. Cloud generation, account billing/quota/region access and real web research are not end-to-end verified.
- The emulator does not establish real-phone Gemini Nano support or inference quality. The actual SDK status path is tested; generation and model download still need a supported physical phone.
- Real microphone recognition, audible TTS, background voice, gallery selection, reminder delivery and external calendar/email/Clock handlers still need physical-phone verification. Test speaker, Android voice settings and Test saved connection now expose useful diagnostics without requiring private data.
- This remains a debug-signed personal prototype. No production signing, Google OAuth, clinical validation or worldwide-scale verification is claimed.

- Floating overlay screenshot inspected on the emulator home screen (milo-floating.png). Drag/tap/long press gestures, notification Hide, permission denial/revocation, rotation, lock screen and OEM battery behavior still need real-phone verification.


- Simplified Talk layout built and lint checked; all 10 existing emulator tests passed after redesign. Updated Talk screenshot visually inspected. Milo artwork is unchanged. No live AI or audible voice verification was added in this design update.

- Milo 1.5 adds persistent System, Light and Dark appearance selection for native screens and the built-in Home view; the web companion also has appearance preferences. `assembleDebug` and `lintDebug` passed. No emulator tests were run for this theme change.
