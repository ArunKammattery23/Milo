# Milo

Android companion for conversation, learning, goals and everyday wellbeing. Existing Milo artwork is retained. Version 1.5 adds System / Light / Dark themes and simplifies Talk with Speak and Send, a conversation options menu, and Home / Talk / Goals / More navigation.

- `android/`: native app, Gradle wrapper, setup instructions and device smoke tests.
- `web/`: companion web UI and Android download guide.
- Read `android/README.md` for setup and `android/VERIFICATION.md` for tested behavior and limitations.

Build from `android/` with Java 17 and Android SDK 36: `./gradlew assembleDebug lintDebug`. Configure your own SDK location in untracked `local.properties`. Local AI is the default on supported Gemini Nano phones; cloud providers are optional and may charge. No provider keys or release signing material are included.

Current personal test download: https://milo-pocket-companion.arun-techlogica.chatgpt.site/android.html

This is a debug prototype, not yet verified for production or clinical use. Real-phone inference and voice still require verification.
