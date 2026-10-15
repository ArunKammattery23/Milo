# Milo

Android companion for conversation, learning, goals and everyday wellbeing. Existing Milo artwork and System/Light/Dark themes are retained.

Version 2.0 runs Google's Gemma 4 E2B on the phone's GPU through LiteRT-LM (offline, Malayalam and 140+ languages) and turns the floating pet into a hands-free talking companion with live expressions and tone-matched voices. Version 2.1 redraws Milo as a little fairy with fluttering wings; 2.2 gives her 20 expressions; 2.3 makes her the app icon; 2.5 adds a voice studio with sweet character voices, emotion voices and an offline Malayalam AI voice. 2.6 listens quietly with animated dots, can hide the reply text in the pet's bubble, and offers an optional pay-as-you-go human voice from Google Gemini (off by default: it adds about 4–5 s before Milo speaks and needs your Gemini key and billing).

- `android/`: native app, Gradle wrapper, setup and device tests.
- `web/`: companion UI and Android download guide.
- `cloud/`: Spark access rules, security tests and [owner setup](cloud/README.md). Authentication initialization still needs verification for milo-aea0c; live messaging is not yet confirmed.

Build from `android/` with Java 21 and SDK 36: `./gradlew assembleDebug lintDebug`. SDK configuration, private keys, signing material and APKs are not committed.

Local AI is the default. Import `gemma3-1b-it-int4.task` via Setup for local Gemma, or choose system Gemini Nano on supported devices. The model publisher may require a free account and acceptance of terms. Optional cloud providers can charge. No personal AI key is included.

[Android test download](https://milo-pocket-companion.arun-techlogica.chatgpt.site/android.html) · [Verification and limits](android/VERIFICATION.md)

This is a debug prototype. Local-model inference, live provider access and two-phone messaging still need physical-device verification.
