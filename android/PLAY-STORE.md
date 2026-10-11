# Play Store release blockers and next steps

Current artifact: debug-signed APK for personal testing. Do not upload it as a production release or represent the current prototype as ready for lakhs of users.

Before publication:

- Create/verify an owner-controlled Play Console account. Check its production-access status. Personal accounts created after 13 November 2023 generally require at least 12 testers continuously opted in for 14 days before applying for production access. [Official testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465).
- Choose the final application ID, app name, developer identity, support contact, target audience, supported countries, and branding. Confirm Google’s model API age/region restrictions; the prototype is not assessed for children.
- Create a release upload key, enroll in Play App Signing, and build a properly signed Android App Bundle. Secure signing material outside source control. The project targets API 36, matching the current new-app requirement. [Target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878).
- Test real Android phones, unsupported speech engines, denied/cancelled speech actions, text-to-speech languages, screen rotations, offline startup, weak networks, revoked/invalid keys, quota limits, voice interruptions, long conversations, and deletion. Perform accessibility and localization reviews. Test the Gemini API end to end using an eligible test project.
- Provide an accurate public privacy-policy URL and an in-app privacy-policy link. Add the actual operator and support contact; do not submit a placeholder policy. Complete Data safety based on the final implementation and provider behavior. Mood, chat, and voice interactions can include sensitive information even if they are sent directly to a third party.
- Complete the Health apps declaration accurately for wellbeing/stress features. Avoid medical claims or guaranteed outcomes; retain appropriate wellbeing boundaries. [Health requirements](https://support.google.com/googleplay/android-developer/answer/16679511).
- Review Google Play’s AI-generated-content requirements, including a usable in-app reporting mechanism and response process. This prototype has no operational report endpoint or developer support address yet.
- Declare and justify both the microphone and special-use floating-pet foreground services in Play Console with the required demonstration material. Test microphone permission denial/revocation, notification access, background sessions, display-over-other-apps permission, floating-pet drag/tap/hide, lock-screen behavior, stopping, battery limits, and OEM speech-service restrictions. Sessions never auto-start at boot. Review the final permissions and disclosures before release.
- Give reviewers a working way to exercise AI features without asking them to configure paid billing or exposing an unrestricted production key.
- Prepare store description, screenshots, content rating, app-access instructions, and release notes. Submit through the proper testing track first. Review/approval time cannot be promised for today.

The app’s independent per-user API requests reduce a central server bottleneck but do not establish scalability, provider eligibility for every user, or reliable worldwide availability. Additional provider integrations, cloud sync, Google OAuth, automatic email scheduling, and multi-user operations still require additional engineering.

