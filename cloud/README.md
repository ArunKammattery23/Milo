# Milo People: owner setup

Milo 1.6 adds a private family/friends list, email-bound invitations, exact messages, replies, read receipts, blocking, deletion and optional Android notifications. “Where is my son?” sends **“Where are you? Please reply when you can.”** The son chooses what to say. Milo does not request GPS permission or track people. Senders review messages before sending; received messages are read aloud only when the recipient taps **Read aloud**.

The personal APK is connected to the owner project **milo-aea0c**, but **cross-phone messaging remains inactive** until its backend and App Check are activated. It can save names locally now. Android registration and private Firestore rules are set up. Authentication initialization is not yet verified, and the project is still on the non-billing Spark plan. The owner must finish these steps, deploy the service and rebuild with the messaging flag enabled. Customers do not need an AI API key for messaging. This first version uses verified **email/password** accounts; Google sign-in and anonymous paid communication are not implemented.

## 1. Create the owner project

1. Create a project in the [Firebase console](https://console.firebase.google.com/). Disable Analytics if you do not need it; this app does not include the Analytics SDK.
2. Add an Android app with package name **life.milo.companion**. Download `google-services.json` to `android/app/google-services.json`. It is intentionally ignored by Git. Never put service-account credentials or AI provider secrets in the APK or repository.
3. Enable Authentication → Email/Password. Configure password policy, email verification and password recovery templates. Review authentication quotas and abuse protection before inviting users.
4. Create a Cloud Firestore database in production mode. Choose its region deliberately. This implementation's functions run in `us-central1`; changing region requires updating `functions/index.js` and `PeopleClient.java` together.
5. Cloud Functions requires a billing-enabled **Blaze** project. Set budget alerts and monitor usage. Alerts are not hard spending caps. FCM itself has no charge, but Firestore, functions, storage, email delivery limits and operational costs mean this is not an unlimited free service. See [Firebase pricing](https://firebase.google.com/pricing).

## 2. Activate app verification

Register the Android app with [App Check / Play Integrity](https://firebase.google.com/docs/app-check/android/play-integrity-provider). Add the SHA-256 fingerprint of your release signing certificate (including Play App Signing when using Google Play). Configure attestation requirements for your distribution channel. Release builds use Play Integrity; callable functions require App Check and verified-email authentication.

Debug builds use the Firebase debug provider. For a private configured debug build, obtain its debug token locally from Android logcat and register it in App Check → Manage debug tokens. Treat that token as a secret; do not commit it, paste it into chat, or publish a debug-token-enabled build to customers. The APK currently offered on the private Site keeps messaging inactive and does not need a registered debug token until cloud testing is enabled.

Enable App Check enforcement for Firestore after checking valid-device metrics. Do not relax the deny-by-default Firestore rules to fix a setup error. Test token refresh and verification on real phones before production enforcement.

## 3. Install and deploy the backend

Use Node.js **22**, Java **21** for local Firebase emulators, and the Firebase CLI installed below. From the repository:

```sh
cd cloud
npm ci
npm --prefix functions ci
npx firebase login
npx firebase deploy --only functions,firestore:rules --project YOUR_PROJECT_ID
```

The owner identified project **milo-aea0c**, saved as the CLI default in `.firebaserc`. Replace `YOUR_PROJECT_ID` with `milo-aea0c` after confirming access to that project. Deployment creates billable resources; these instructions do not deploy anything automatically. Follow the CLI's prompts for required APIs, IAM and artifact cleanup. Functions use Application Default Credentials on the server, never a service-account file in the Android app.

In Firestore → Time-to-live, enable the `expiresAt` field for collection groups **messages**, **invites**, and **limits**. Message retention is 30 days, invitations 24 hours and rate-limit documents 3 days. TTL removal is asynchronous and can incur delete charges. The app hides expired messages; TTL physically removes stored copies later. Until TTL deletes them, authorized server operators and the owning account can still access expired copies. Tombstones in `deletedUsers` retain only the removed account UID and timestamp to prevent stale tokens from recreating a deleted account. Do not enable TTL on tombstones while old authentication tokens could still be accepted.

## 4. Build and try on two phones

With Java 17 and Android SDK 36:

```sh
cd android
./gradlew -PmiloPeopleEnabled=true assembleDebug lintDebug
```

For release distribution, configure owner signing outside Git and build the release bundle. The debug prototype is not a Play Store artifact. The Google Services Gradle plugin is applied only when `app/google-services.json` exists, so the default checkout still builds without owner configuration. `-PmiloPeopleEnabled=true` activates the client only after you have deployed and verified the server. Without that flag, People shows an owner activation notice even if the SDK configuration is present.

On each phone:

1. Open **More → People**, create an account, open its verification email, then tap **I have verified · refresh**.
2. Save your People name. This name and verified email identify your messages to connected people; a relationship label is not proof of a real family relationship.
3. Mom taps **Invite family or a friend**, enters the son's verified email and label “Son”, creates a code and shares it herself. Milo does not send this code automatically.
4. Son taps **Accept an invitation**, enters the code and label “Mom”, and accepts the stated sharing permissions. The code is email-bound and single-use.
5. Mom opens Son → **Ask where they are** or **Ask them to come home**, reviews the exact message and sends it. Son opens his inbox and replies. No location is collected automatically.
6. Each person may enable notifications independently. Notifications contain no sender or message body; opening Milo loads the inbox. Offline delivery can be delayed. **Off** suppresses Milo notifications and speech; it does not revoke an accepted connection. Disable notifications or block/remove someone in People when desired.

Test verified/unverified accounts, rejected/expired invitations, retries after lost connectivity, two-way replies, receipt synchronization, blocked connections, logout/account switching, notification denial, Off, token refresh, reinstall, real voice and account deletion. Push/voice delivery and configured Android authentication still need real-device end-to-end verification; emulator backend tests alone do not establish these.

## Privacy and boundaries

- Cloud data: account UID, verified email, chosen name/labels, connections, exact message text, timestamps/read receipts, rate counters and opt-in device tokens. Saved local names stay on the phone until you invite someone. Mood, AI history, gallery, goals and location are not sent to this messaging backend.
- Client writes are denied. Server functions derive identity from authenticated claims; invitations are bound to verified email and mutual acceptance. Blocks stop new messages. Maximum 50 active connections, 10 invitations/day, 20 accepts/day, 100 sends/day, 2,000 characters/message and 5 devices/account. These limits and App Check mitigate abuse; they are not a complete public-service moderation system.
- Message transport does not call any AI model. Dictation may use your phone's remote speech service. Messages are not end-to-end encrypted; the project's authorized server operators can access stored content. Firebase handles transport encryption and storage controls.
- **Delete my copy** removes only that user's copy. **Delete my People account** requires recent sign-in and removes their profile, tokens, private message copies and connections. Another person's message copies remain until they delete them or TTL removes them. Erasing local Milo data signs out; it does not delete server copies. If a deletion is interrupted, retry or have the owner complete cleanup before treating it as finished.
- No paid anonymous feature, live location, emergency dispatch, autonomous message sending or automatic background reading is included. The native Android app is the messaging client; the web companion and iPhone do not yet have this flow.

## Local verification

```sh
cd cloud
npm test
```

Tests use the **demo-milo-people** Firestore emulator, never production. They exercise invitations, identity/App Check guards, exact sender attribution, transaction retries/idempotence, read receipts, blocks, quotas, device ownership, account deletion and Firestore access rules. They do not send FCM pushes, verification emails or real messages. See [Android verification](../android/VERIFICATION.md) for recorded results.

Primary references: [Android setup](https://firebase.google.com/docs/android/setup), [callable functions](https://firebase.google.com/docs/functions/callable), [FCM receiving](https://firebase.google.com/docs/cloud-messaging/android/receive-messages), [email/password authentication](https://firebase.google.com/docs/auth/android/password-auth), [App Check](https://firebase.google.com/docs/app-check/android/play-integrity-provider), [TTL](https://firebase.google.com/docs/firestore/ttl).
