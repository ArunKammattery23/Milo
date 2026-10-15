# Milo Android personal prototype 2.0

Local AI is the default; cloud APIs are optional. Android 8 or newer can install the app. The recommended on-device model is Google's Gemma 4 E2B (2.6 GB, Apache-2.0), run on the phone's GPU through LiteRT-LM with a CPU fallback; Setup downloads it on Wi-Fi straight into Milo's app folder. Smaller imported `.task` models (Qwen 2.5 0.5B, Gemma 3 1B) through MediaPipe and system Gemini Nano remain as alternatives. Local inference has no per-request API fee after the model download, but model availability, storage, battery use and performance vary by phone. The imported model needs substantial free storage and can take time to respond.

## Start on your phone

1. Install Milo-Android-Test.apk. This is a debug-signed personal prototype.
2. Open Setup → Free on-phone AI → Download Gemma 4 (Wi-Fi, 2.6 GB; about 2.9 GB free storage needed), then Use Gemma 4. Smaller `.task` models can still be imported with Android's file picker, or check whether Gemini Nano is available. The Setup screen links to the official model source and explains its license. Once ready, use Talk. No API key is needed for local text generation.
3. If local AI is unavailable, offline Comfort, Goals, Memories and prepared lessons still work. Cloud AI is optional: choose Gemini, OpenAI API, Claude or a compatible HTTPS API, get a developer key from that provider, review the data/price notes and Save connection. Gemini uses automatic model selection by default; manual model choice is under Advanced.
4. Test saved connection sends only a short hello with no private mood/goal/history context. It can use quota or incur provider charges. A saved encrypted key alone does not prove live access.
5. For sound, enable Milo, choose a supported spoken language, use Setup → Test speaker and raise media volume. Android voice settings can install/select a speech engine and voice. Failed playback is shown explicitly.
6. Speak opens phone speech recognition. Review the transcript before Send, or explicitly enable Direct voice. Speech recognition/TTS may use remote phone services even when AI text generation is local.

The local mode never silently switches to a cloud provider. Local inference is limited to short tasks in the foreground; it has no web research or background AI. AICore has per-app quotas and its model availability varies by phone/configuration. Importing Gemma on a real phone and its response speed still need device testing.

Cloud chat streams text. Google Search is available only for Gemini in this prototype. OpenAI and Claude model IDs are editable for account availability and deeper learning. Other Chat Completions services can use the Custom API option over HTTPS. Choosing a more capable model may cost more. No automatic provider/model upgrade occurs. ChatGPT/Claude app subscriptions do not grant this app API access.

## Floating pet

Open Setup → Show floating Milo pet, allow notifications, then allow Display over other apps in Android settings. Return to Milo to start the pet. Drag it anywhere; it stays where you drop it. Tap it to talk hands-free: it listens, thinks and answers aloud, then listens again. Tap while it listens to finish speaking, or while it speaks to interrupt. Hold it to open Milo. Drop it on the ✕ that appears while dragging, use Hide pet in its notification, or turn Milo Off to remove it. A bubble shows what it heard and its reply. It blinks and breathes, moves its mouth while speaking, shows sound waves while listening and dots while thinking, then takes the tone of its reply. The first tap asks for microphone permission; the microphone is used only during a conversation you start, with a Stop talking notification, and three silent turns end it. It never reads screen contents.

With Gemma 4 the model is preloaded when the pet appears (about 6–12 s on the GPU), and Milo's instructions are processed while you speak. On the Galaxy S23 Ultra, Milo starts answering aloud about 2.7 s after you stop speaking a Malayalam question; follow-up text starts in about 0.2 s.

## Malayalam and voices

Choose Malayalam as the spoken language in Setup. Gemma 4 then replies in everyday spoken Malayalam in Malayalam script. Speech uses Google's voices (offline when the voice is installed, otherwise online). Setup → Milo's voice lists the voices for your language; Voice style offers Friendly, Cute pet, Natural, Calm and Deep. Milo shades each reply by its tone: brighter when playful or celebrating, slower and softer when comforting. Malayalam speech recognition on this phone needs internet; Google offers no offline Malayalam recognizer here, while the AI reply itself stays on the phone. It starts only at your request, never at reboot. Android may stop it for battery/system reasons. This independent Android pet uses simple illustrated expressions; it is not connected to ChatGPT Work Pets.

## Other tools

Milo offers explicit mood check-ins, breathing/grounding, small goal steps, language practice, interests, business brainstorming through AI, expressions, rename and Gentle/Playful/Straightforward tone preferences. Users stay free to disagree, rest, skip activities or turn it off. See EMOTIONS.md.

Cloud background voice is a separate opt-in foreground-service session with microphone/notification permissions, an ongoing Stop microphone notification and no wake word. It pauses input while replying and can stop after silence/platform restrictions. It never starts after reboot. Off stops Milo floating pet, voice and reminders; external Clock alarms remain managed in the Clock app.

Memories accepts only selected photos; resized private copies stay local and are never sent to an AI. Up to 50 moments can be saved with optional dates. Daily reminders are around 9 am local time and Android may delay them. Calendar, email and Clock actions open the phone's apps for user review. Automatic email delivery, Google OAuth/account sign-in, cloud sync and a native iPhone app remain future work; see GOOGLE-SETUP.md and PLAY-STORE.md.

## Privacy and limits

No developer AI key is included. Provider keys are encrypted separately using Android Keystore. Saving an unchanged connection with a blank key field retains the already saved key. Selected photos, mood, interests, goals and settings use private phone storage; chats stay in memory. Backup is disabled. Firebase stores only opted-in People accounts, invitations and messages, subject to its rules. Setup can disconnect providers and erase local data. Device compromise can still expose data; local erasure does not erase provider-retained data.

Cloud Send includes the prompt, recent chat, selected mood/goals/preferences to the chosen provider over HTTPS. Google unpaid-service terms may allow submitted data to improve products. Custom API keys and prompts go to the HTTPS server the user chooses. Keep confidential details out. Local text generation stays on the phone; model downloads/status checks and speech services have separate connectivity needs.

There is a 30-attempt daily cloud limit per installation, not a billing guarantee. Provider quotas, regions, eligibility and prices can change. Milo cannot detect whether a key has billing enabled. No health, happiness, business outcome, immediate-response or worldwide-scale guarantee is made. Milo supports reflection and everyday wellbeing; it cannot diagnose or treat illness.

## Build and verification

Java 21, Gradle 8.14, Android SDK 36, AGP 8.13.2, LiteRT-LM 0.18.0, ML Kit genai-prompt 1.0.0-beta4 and MediaPipe tasks-genai 0.10.27. Build: assembleDebug lintDebug assembleDebugAndroidTest. Use -PmiloBuildDir pointing to a writable external build directory if the checkout is on OneDrive.

See VERIFICATION.md for exactly what passed and what still needs real-phone/credential testing.

Official references: https://developers.google.com/ml-kit/genai/prompt/android/get-started ; https://ai.google.dev/gemini-api/docs/api-key ; https://developers.openai.com/api/docs/models/gpt-4.1-mini ; https://platform.claude.com/docs/en/build-with-claude/streaming .


## Appearance

Open Setup → Appearance to choose System, Light or Dark. System follows your Android appearance setting. Milo remembers your choice on this phone and applies it to native screens and the built-in Home view. The web companion offers the same options in Settings and remembers the choice in this browser.

## Simple conversation layout

Talk keeps the existing Milo artwork and uses a spacious conversation area with Speak and Send. Open the ••• menu for web search, spoken replies, hands-free turns, Pause voice, privacy and New conversation. Need a starting point opens suggested topics. Bottom navigation shows Home, Talk, Goals and More; More contains Comfort, Memories and Setup.

Canonical source repository: https://github.com/ArunKammattery23/Milo.git. Use this repository for future Milo changes. Sites publication continues through the existing Sites source workflow.

## People · family and friends

Open More → People for family/friend messages. Firestore rules are deployed on the free Spark plan. Both people need verified email/password accounts and must accept an invitation. Review exact messages before sending, reply, or tap Read aloud. Ask where they are requests their reply; no GPS permission is used. Messages update while People is open, without background push or automatic speech. Firebase Authentication still needs to be activated in the owner project before cross-phone sign-in can work; see [cloud/README.md](../cloud/README.md). AI keys are unrelated to messaging. Paid anonymous messaging and Google account sign-in are not included.
