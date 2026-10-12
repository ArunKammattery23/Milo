# Milo People — free Spark version (1.7)

Milo 1.7 uses Firebase Authentication and direct Cloud Firestore client transactions. Cloud Functions and AI provider keys are **not required**. Keep the project on Spark. Free storage/read/write/authentication quotas still apply; this is not an unlimited service.

## Current owner configuration

Project: `milo-aea0c`. Android package: `life.milo.companion`. The configured personal APK includes the ignored owner SDK configuration, and the tested Spark rules are deployed. **Authentication still returns CONFIGURATION_NOT_FOUND**, including through the same sign-in endpoint used by the Android SDK. This is a service setup blocker, not an AI key problem.

Open [Authentication → Sign-in method](https://console.firebase.google.com/project/milo-aea0c/authentication/providers), click **Get started** if present, then enable Email/Password and Save. The Android app-settings page does not enable sign-in. Once authentication is initialized, this build needs no messaging server activation flag. Google sign-in is not implemented.

## Use on two phones

1. Install the same configured APK. Open **More → People**, create an email/password account, open its verification email and tap **I have verified · refresh**.
2. Save your People name. The other person does the same.
3. Add a saved name/relationship, or tap **Invite family or a friend**. Enter the other person's exact verified account email and your label, such as Son.
4. Share the 32-character invitation code yourself. It expires after approximately 23 hours. The recipient chooses **Accept an invitation**, supplies the code and their own relationship label, such as Mom, and explicitly accepts.
5. Open the connection. Choose **Ask where they are**, **Ask them to come home**, or **Write or speak a message**. Review the actual text and confirm Send.
6. The recipient opens the conversation, taps a message, and chooses **Read aloud** or **Reply**. Replies go back through the same private conversation.

Milo relays the exact reviewed text. It never invents a person's whereabouts or collects GPS. Speech dictation uses Android's speech service and may be online. General AI chat does not autonomously choose contacts or send messages. This is a conversation relay, not automatic conflict resolution.

## Free-version boundaries

- Updates run only while the People view is visible. No background push, automatic playback, wake word or guaranteed immediate delivery.
- The newest 50 conversations and 50 messages in an opened conversation are shown. No older-message pagination in this prototype.
- A server-enforced per-account rolling allowance permits 120 combined message/invitation creations per 24 hours. Firebase project quotas may be reached sooner. Reads and account creation are subject to Firebase quotas, not a complete public-abuse prevention system.
- Invites are email-bound, single-use and expire for acceptance. They are not automatically deleted. Messages remain until their sender deletes them; no paid TTL cleanup is configured.
- **Hide for me** changes only the current user's view; it is not cloud deletion. **Delete for both people** removes a message authored by the caller. Previously read/copied text cannot be recalled.
- Blocking stops messages in both directions across old conversations. Unblocking still requires a new invitation/conversation. Closing a conversation cannot be reversed.
- **Delete my People account** requires recent sign-in and first disables/redacts the private profile, then deletes the Firebase login. Existing conversation identity snapshots and messages remain for the other participant; delete sent messages before deleting the account if desired. A minimal disabled profile prevents stale credentials from recreating the profile. Local drafts are erased separately in Setup. A failed login-deletion step can be retried.
- Messages are encrypted in transit and access-controlled but **not end-to-end encrypted**. Authorized project operators can access stored data.
- Paid anonymous communication is deferred. Native iPhone messaging and Google sign-in are not included.

## Owner build and deployment

Use Java **21**, Android SDK 36, Gradle 8.14 and the repo's AGP version. Configure the SDK path outside Git. Place the owner-downloaded `google-services.json` in `android/app/`; it is ignored. Never put service-account files or personal AI keys in the APK.

```sh
cd android
./gradlew assembleDebug assembleDebugAndroidTest lintDebug
cd ../cloud
npm ci
npm --prefix functions ci
npm test
npx firebase deploy --only firestore:rules --project milo-aea0c
```

Tests use demo-milo-people emulators, never production records. Deployment above changes only Firestore rules; it does not enable billing or deploy functions. Release signing and production distribution still need separate work. App Check providers exist in the app, but production Firestore App Check enforcement must be configured and tested by the owner before public launch. Do not distribute debug attestation tokens.

The old v1.6 callable-functions implementation remains in `functions/` for possible future migration. Its Android push delivery is disabled by `PEOPLE_RELAY_ENABLED=false`. Do not deploy it for this Spark version.

Primary references: [Firestore realtime listeners](https://firebase.google.com/docs/firestore/query-data/listen), [atomic writes and access-rule validation](https://firebase.google.com/docs/firestore/manage-data/transactions), [Firebase pricing plans](https://firebase.google.com/docs/projects/billing/firebase-pricing-plans), [email/password authentication](https://firebase.google.com/docs/auth/android/password-auth).
