# Prototype verification — latest: 15 October 2026

## Milo 2.5 — voice studio, on-phone AI voices, full-length stories

- Version code 19. `assembleDebug`, `assembleDebugAndroidTest` and `lintDebug` passed; **all 24 smoke tests passed on the Galaxy S23 Ultra** and Milo settings were byte-identical before and after.
- Voice studio: seven characters (Sweet Girl default), emotion voices (pitch, speed, loudness, a little sound such as "ഹായ്!" or "അയ്യോ!"), lip sync from the audio Milo plays itself, per-sentence tones, and an Angry expression and harsh voice that the model may use only for pretend play or unfair situations, never at the user. Milo measures each phone voice's pitch to choose a girl or boy voice (on the S23: en-us-x-iog 218 Hz, sfg 216 Hz; ml-in-x-mlc 197 Hz, mle 192 Hz).
- On-phone AI voices through sherpa-onnx 1.13.8 (AAR fetched at build time, SHA-256 checked): Piper Meera (Malayalam) renders about 3 s of speech in 0.3 s and is used automatically for girl characters in Malayalam; Kokoro int8 (English) sounds most natural but renders 1.5–2x slower than real time on the S23 with 2–8 threads or XNNPACK, so it is opt-in only. Voices are unpacked by Milo into private internal storage; files pushed over USB into the app's external folder could not be read reliably by the speech engine.
- Stories and pep talks are no longer cut to three sentences: on the S23 Gemma 4 told a 346-word bedtime story and a 152-word motivational talk; casual chat stays short.
- The Home view has the Angry expression too.

## Milo 2.3 — fairy app icon

- Version code 17. `assembleDebug`, `assembleDebugAndroidTest` and `lintDebug` passed. **All 23 smoke tests passed on the Galaxy S23 Ultra**; Milo settings were byte-identical before and after.
- The launcher icon is now an adaptive icon (`mipmap/ic_launcher`, `ic_launcher_round`) showing the fairy's head and shoulders on a pastel background with sparkles, plus a monochrome layer for Android themed icons. Notifications use a new white fairy silhouette (`drawable/ic_stat_milo`); the old green `milo_icon` was removed. The web companion's `icon.svg` uses the same artwork. All layers are generated from the pet geometry.
- New test `testAppIconDrawsFairy` checks the icon is adaptive with a themed layer, draws it, and saves a review sheet. The icon was checked in the Samsung app drawer and in the notification shade.
- Version 2.3 debug APK SHA-256: 86E753141DA8D2BF6376C76547A4572991D33CAAD2D8D63C411A55E698C3105B.

## Milo 2.2 — 20 expressions

- Version code 16. `assembleDebug`, `assembleDebugAndroidTest` and `lintDebug` passed. **All 22 smoke tests passed on the Galaxy S23 Ultra**; Milo settings were byte-identical before and after.
- Nine expressions added to the app, the floating pet and the Home view: surprised, excited, laughing, shy, grateful, sad, confused, sleepy and oops (20 in total including listening, thinking, speaking and off). The artwork test now draws all 20 and checks each differs from the previous one; the gallery and all 16 Home picker expressions were visually checked.
- Gemma 4 can choose all of them as reply tones. A one-off check on the S23 chose laughing for a joke, grateful for thanks (English and Malayalam), sleepy for good night, sad for a bereavement, confused for nonsense words and celebrate for a new job; it chose grateful rather than shy for a compliment and calm rather than oops when corrected. Tone choice varies between replies.
- Version 2.2 debug APK SHA-256: C3B644F181FF5050F7891CA5BD4547B192AC8C300B746AFD49D5A50D99A9749A.

## Milo 2.1 — new fairy design

- Version code 15. `assembleDebug`, `assembleDebugAndroidTest` and `lintDebug` passed. **All 22 smoke tests passed on the Galaxy S23 Ultra**; Milo settings were byte-identical before and after.
- At the owner's request Milo is redrawn as a chibi fairy (lavender-blue fluffy hair with a curl, large sparkling blue eyes, blush, flower/star/pearl hair clip with a ribbon, translucent fluttering wings, petal dress with a star bow, white boots with gold stars), inspired by a reference image the owner supplied. It is original vector drawing code, not a copy of that image.
- The same design is used in Talk, the floating pet, Setup's expression preview and the built-in Home view (SVG with fluttering wings, twinkles and the same expressions); the web companion's Home copy matches.
- New device test `testPetArtworkDrawsEveryExpression` draws all 11 expressions (happy, playful, curious, comfort, calm, celebrate, concerned, listening, thinking, speaking, off), checks each draws and differs from the previous one, and saves a gallery image. The gallery, the Home expression picker and the floating pet over the home screen were visually inspected on the phone.
- The launcher icon was unchanged in 2.1 (replaced in 2.3).
- Version 2.1 debug APK SHA-256: 41A37A0C9E95D48DE57BC1A67BE47B28C6A7944F9305FC3812750CDE0D28E002.

## Milo 2.0 — Gemma 4 on the GPU, a talking floating pet, Malayalam voices

- Version code 14. `assembleDebug`, `assembleDebugAndroidTest` and `lintDebug` passed (lint prints Kotlin 2.4 metadata notices from LiteRT-LM 0.18.0's dependencies; no lint errors). **All 21 smoke tests passed on the Galaxy S23 Ultra** (SM-S918B, Android 16, Snapdragon 8 Gen 2); Milo settings were byte-identical before and after the run.
- Gemma 4 E2B (`gemma-4-E2B-it.litertlm`, 2,588,147,712 bytes, SHA-256 181938105E0EEFD105961417E8DA75903EACDA102C4FCE9CE90F50B97139A63C, Apache-2.0, litert-community) runs through LiteRT-LM 0.18.0 on the GPU. Load: 13.8 s the first time, 5–12 s later (Milo preloads it when the app opens or the pet appears). First text: about 0.2 s on a follow-up turn in the same conversation, 0.65–1.5 s for a new conversation; a short reply finishes in 1–3 s. A GPU start crash would switch future starts to the CPU automatically; the CPU path was not exercised on this phone.
- Malayalam: asked in Malayalam script, Gemma 4 answered in natural Malayalam script with a fitting tone tag (e.g. comfort). Google TTS has Malayalam voices on this phone (mlc, mle; an embedded mla voice also loads offline). Google speech recognition supports Malayalam online only on this phone.
- Real voice loop test: Milo in the background, a real touch on the floating pet, a Malayalam question spoken through the phone speaker, Google recognition transcribed it exactly, Gemma 4 answered and the pet spoke the answer in Malayalam, with expressions listening → thinking → speaking → calm. Spoken answer began about 2.7 s after the question ended. It also passed with Milo in the foreground. A human voice in a noisy room was not tested.
- Android 16 allows the microphone session when the user taps the pet (the service changes to special-use + microphone); a programmatic start from the background was refused, so a see-through one-time screen remains as a fallback. Fixed during testing: an 800 ms "complete silence" extra made Google end listening before the user started speaking.
- Fixed: with on-phone AI already selected, Setup never showed the on-phone section (the hidden picker never reported a selection).
- Qwen 2.5 0.5B and its 500 MB runtime cache were removed from this phone at the owner's request after Gemma 4 worked; the Qwen path remains in the app for other phones.
- Version 2.0 debug APK (114 MB) SHA-256: 84C814825678A710FC0E4996745F9BC3E5C63187FEB319D7F09B394A5CF2703F. Debug-signed prototype, not a Play Store release.

## Milo 1.12 — local model stays loaded and replies stream

- Version code 13. `assembleDebug`, `assembleDebugAndroidTest` and `lintDebug` passed. All 17 smoke tests passed on the connected Galaxy S23 Ultra (Android 16) with the imported Qwen 2.5 0.5B model.
- The imported model now stays loaded between messages instead of reloading for every reply; each reply still starts a fresh session with the same short recent-chat prompt. It is released when its file is replaced or erased, and when Android reports memory pressure with Milo in the background. While loaded, Milo used about 880 MB (PSS) on this phone.
- Replies now stream into the Talk bubble and spoken sentences start while the rest generates. On the S23 Talk screen: first text about 0.3–0.5 s after Send, full multi-paragraph reply in 3.6–4.9 s; second message reported `warm load=0ms`. With the model file already in the phone's file cache, a reload was only about 0.1–0.25 s; the saving is larger after a reboot or memory pressure. Timings are logged under the `MiloLocalAi` logcat tag (no message content).
- New device test `testImportedLocalModelStreamsAndStaysLoaded` runs real inference twice, checks that streamed text grows in order and matches the final reply, and that the second reply reuses the loaded engine; it skips on devices without an imported model.
- Smoke tests now snapshot and restore all Milo settings. Previously the provider setup test left a real phone set to Claude instead of the user's chosen AI.
- Version 1.12 debug APK SHA-256: 26391FDA2EEAD88C0DE2EAD662C534C642B53E5672819700D4BBA931F401FFD4. Debug-signed prototype, not a Play Store release.

## Milo 1.11 — tested local Qwen and search quota fix

- Version code 12. Android build, test APK and lint passed. All 16 smoke tests passed on the connected Galaxy S23 after making setup tests preserve existing saved keys.
- Imported the public 546,660,344-byte Qwen 2.5 0.5B .task model and generated a reply on the S23 in a 5.7-second diagnostic run. The model remains in app-private storage; inference uses the on-device MediaPipe runtime. Performance and compatibility on other phones remain unverified.
- On this phone, a short Gemini request without search succeeded. With search enabled, Milo selected gemini-3.8-flash and received 429. A direct gemini-2.5-flash grounded request returned 404 for this key. Ordinary chat now leaves web search off; a grounded quota error offers an explicit retry without search. Google Search grounding has a separate quota and may require billing, depending on model/project.
- Version 1.11 debug APK SHA-256: 6AB14004C974F00C07B5F2370ACD974E0BFB4BE440366C2DB002C2BAAC98445F. This is a debug-signed prototype, not a Play Store release.

## Milo 1.10 — Gemini quota guidance

- Version code 11. `assembleDebug`, `assembleDebugAndroidTest`, and `lintDebug` passed. On a Galaxy S23, the quota guidance and Android voice-service tests passed (2 tests). The device accepted a spoken test request; audible sound still depends on phone volume and speaker output.
- Google 429 now appears as one error with direct buttons for project limits and on-phone AI setup. This does not bypass Google's rate or daily quotas.
- Version 1.10 debug APK SHA-256: 9AE1EB40906E2C06DBDF9AC57B7B60B81FFB70F6CBF7092F234C127E410BFBCE. The user's Google project still needs available Gemini quota for cloud replies.

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
