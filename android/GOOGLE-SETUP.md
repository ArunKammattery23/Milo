# Google connections — setup for the next version

The current app opens calendar and email drafts in the user’s own apps. It does not have Google sign-in or API access. A Gemini API key authorizes model calls; it does not authorize access to Gmail or Google Calendar.

## Personal AI key

Open [Google AI Studio](https://aistudio.google.com/api-keys), sign in with an eligible Google account, and create a key in a project without billing enabled. Enter it only in Milo Setup. Do not distribute a developer key in the app, paste keys into source, or send them through chat. [Google API-key guidance](https://ai.google.dev/gemini-api/docs/api-key).

## OAuth preparation for account integrations

1. Create/select an owner-controlled Google Cloud project. Configure the OAuth consent screen, intended audience, developer contact, and accurate publicly accessible privacy policy. Add test users while in testing.
2. Enable only the APIs required: Calendar API for calendar actions; Gmail API if Gmail access is actually needed.
3. Register an Android OAuth client for package `life.milo.companion` and the SHA-1 of the **release** signing certificate. Debug keys are unsuitable for a production client. Google Sign-in authenticates a user; requesting access to Calendar/Gmail is a separate authorization flow.
4. Implement [Android AuthorizationClient](https://developer.android.com/identity/authorization) with incremental consent for the smallest scopes. Store tokens securely, handle expiration and revoked permissions, and provide a clear Disconnect action. Do not embed client secrets in an Android APK.
5. Build a reviewed-action flow: display the exact recipient/event, date, time zone, and content before execution. Sign-in is not blanket consent to send messages or share sensitive data.
6. If scheduling email while the app is closed is required, add a server scheduler, secure token storage, cancellation, idempotent jobs, and error reporting. This creates operational/security responsibilities and may cost money. Phone-only background work cannot guarantee exact unattended delivery across device power restrictions.
7. Before public distribution, complete Google’s verification requirements for the requested scopes. Some Gmail scopes are restricted and can impose extra requirements. Verify the actual selected scopes against current Google policies.

Keep account credentials and secrets out of chat. The public OAuth client ID can be supplied later; authentication remains disabled until configured and tested. OAuth verification and Play Store review are distinct processes.

References: [Google authorization](https://developer.android.com/identity/authorization), [Calendar event API](https://developers.google.com/calendar/api/v3/reference/events/insert), [Gmail scopes](https://developers.google.com/workspace/gmail/api/auth/scopes).
