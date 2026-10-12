# Prototype verification — latest: 12 October 2026

## Milo 1.9 — power control and voice access

- Version code 10. `assembleDebug`, `assembleDebugAndroidTest`, and `lintDebug` passed. The focused Android emulator test for saved Milo on/off state passed.
- The header now confirms before turning Milo off and shows **Turn on** when off. Setup saves the main switch immediately and labels daily reminders as a separate choice. This prevents accidental one-tap shutdown from silently disabling Talk.
- Version 1.9 debug APK SHA-256: F21CDAD371412AEC12D31B0A7003F6E6E09B58F828B18E87BB30B182177775C2. Physical-phone voice, local Gemma performance, and two-phone messaging still need real-device verification.

## Milo 1.8 — simpler AI connection

- Version code 9. `assembleDebug`, `assembleDebugAndroidTest` and `lintDebug` passed with Java 21 and Android SDK 36. The focused emulator tests for encrypted key storage and automatic Gemini model selection passed (2 tests). A full instrumentation run stopped because the emulator reported an app ANR; it is not counted as passing.
- Setup now offers **Free on-phone AI** and **Connect cloud AI**. Gemini selects a compatible chat model from Google's model list; advanced model choice remains available. Connect saves the encrypted key and sends a small test request in one action.
- A user-authorized temporary Gemini key was tested through the v1.7 emulator's in-app test button with a supported model. The response succeeded and the key was removed from the emulator. This confirms the provider path, not the new 1.8 interface on a physical phone.
- Version 1.8 debug APK SHA-256: EA3ADF336C83BCDFC7924386B33F84B1500BDB6AEF5A14701E9C7BF5EBFD6AF1. Google account billing, local Gemma speed and two-phone messaging still need real-device checks.

## Milo 1.7 — Spark People, API setup and local Gemma

- Java 21 build: assembleDebug, assembleDebugAndroidTest and lintDebug passed. MediaPipe tasks-genai 0.10.27 requires Java 21 to compile; Java 17 is insufficient. Existing lint warnings remain; no lint errors.
- **11 Android API-35 emulator tests passed**. Provider setup tests now additionally verify that an empty key field after saving still retains the encrypted key and that saving again with a blank field preserves it. Tests use fixture keys only; no real provider credentials were read or sent.
- **7 Spark rules tests passed**: verified/private identities, email-bound atomic invitation acceptance, exact two-way messages, recipient-only read receipts, outsider/forgery/oversize/quota rejection, blocking across conversations, reconnection after unblock, hiding/deletion ownership, expiry and disabled-profile enforcement. **8 legacy backend tests also passed**.
- Spark rules deployed successfully to milo-aea0c. No billing upgrade or Cloud Functions deployment. Authentication still returns CONFIGURATION_NOT_FOUND; live signup, verification email and two-phone messaging are not verified.
- Version 1.7 / version code 8. APK SHA-256: A2C38C932D5E4994BE1C0760B7AFAA5374249EE5B8544C2EB6A577EB52C7EFB7.
- Added actual local Gemma inference through MediaPipe and Android file-picker import, separate from system Nano. Publisher model download/terms are handled by the user. No model weights are bundled. Native inference, import of the full model and performance on the owner's S23 remain unverified; the emulator is not proof of support. MediaPipe's API is in maintenance mode; this Java-compatible prototype uses its documented 0.10.27 API, with a future LiteRT-LM migration left open.
- API error classifications distinguish invalid key, access restrictions, model availability, quota/credits and service failures without showing raw provider responses. Test requests retain saved keys and use a captured provider/key. Live provider access remains unverified without a user-owned valid key.
- People requires the conversation open. It does not automatically parse/send a general AI-chat request, speak in the background, or track location. See cloud/README.md for retention and account-deletion limits.

## Earlier verification

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


## Milo 1.6 — People, 11 October 2026

- `assembleDebug`, `lintDebug`, and `assembleDebugAndroidTest` passed with Java 17, SDK 36 and Firebase BoM 34.16.0. BoM 35.0.0 produced an incompatible Kotlin metadata warning in this project's lint tools; the compatible version removes that warning.
- **11 Android API-35 emulator tests passed** on the final Firebase-configured build. The additional check covers saved local People entries, inactive server status, closing People listeners when navigating away, and absence of location permissions. Prior AI/encryption/voice controls and floating-pet checks remain passing.
- **8 backend tests passed** using Node 22.23.3, Java 21 and the demo Firestore emulator. Checks cover verified authentication/App Check guards, email-bound/expired/single-use invitations, sender attribution, duplicate concurrent sends, read receipts, blocks/reconnection, quotas, message length, device ownership, deletion/stale tokens and client/cross-account access denials. These tests do not call production, FCM or verification email services.
- Final APK signature verified; version 1.6 / version code 7. SHA-256: 07333D7DAB2563AC3090BF127744AF9E19EB9B8CD21EBD724AE4FD95CCDD001C.
- Owner project `milo-aea0c` verified through the authorized Firebase CLI. Android app `life.milo.companion` registered; SDK config saved locally and ignored by Git. Default Firestore database created in us-central1 with deletion protection. Tested private rules successfully deployed.
- Authentication still returns CONFIGURATION_NOT_FOUND after the owner reported setup; email/password activation is not independently verified. Billing reports false. No Cloud Functions deployment, App Check registration, FCM delivery or two-phone messaging is claimed. `PEOPLE_RELAY_ENABLED` defaults to false and requires `-PmiloPeopleEnabled=true` after server activation.
- People activation screen visually inspected; Talk design and pet artwork retained. Web appearance controls and dark settings dialog visually checked in local preview. The Android download route is packaged from HTML source.
- The APK remains a debug-signed private prototype. Real-phone voice, supported-phone local inference, cloud AI and configured account/push flows still need end-to-end testing. No paid anonymous communication, live tracking, iPhone messaging client or public-scale validation is implemented.
