# Milo

Android companion for conversation, learning, goals and everyday wellbeing. Existing Milo artwork and System/Light/Dark themes are retained.

Version 1.7 adds free open-app People messaging, clearer encrypted API-key save/test feedback and an importable local Gemma model option for phones such as the Galaxy S23.

- `android/`: native app, Gradle wrapper, setup and device tests.
- `web/`: companion UI and Android download guide.
- `cloud/`: Spark access rules, security tests and [owner setup](cloud/README.md). Authentication initialization still needs verification for milo-aea0c; live messaging is not yet confirmed.

Build from `android/` with Java 21 and SDK 36: `./gradlew assembleDebug lintDebug`. SDK configuration, private keys, signing material and APKs are not committed.

Local AI is the default. Import `gemma3-1b-it-int4.task` via Setup for local Gemma, or choose system Gemini Nano on supported devices. The model publisher may require a free account and acceptance of terms. Optional cloud providers can charge. No personal AI key is included.

[Android test download](https://milo-pocket-companion.arun-techlogica.chatgpt.site/android.html) · [Verification and limits](android/VERIFICATION.md)

This is a debug prototype. Local-model inference, live provider access and two-phone messaging still need physical-device verification.
