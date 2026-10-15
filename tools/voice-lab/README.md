# Milo voice auditions

Research and prototype: 15 October 2026. This is an audition tool, not an Android integration or a trained model. No voice was generated during development. The supplied reference stayed local.

## What Milo currently does

The canonical checkout is `milo-repo/android`, a newer version than the sibling `milo-android` folder supplied initially. `NeuralVoices.java` uses Kokoro for English and Piper Meera for Malayalam. `MiloVoice.java` maps emotions to pitch, rate, volume and interjections. `MiloSpeaker.java` applies these controls and character effects; optional `GeminiVoice.java` asks a cloud model to perform the emotion and falls back to local speech on failure. These paths do not clone a reference voice. Choosing a better chat model alone will not replace the voice.

One possible source of inconsistent quality is cloud-to-local fallback. Another is that character effects are still applied to cloud-rendered speech. These are code observations, not confirmed causes on the user's phone.

## Recommendation

Keep Milo's conversation model and audition a dedicated expressive speech engine first. Compare the same lines with one consistent voice at neutral, happy, comforting and excited delivery. Judge likeness, pronunciation, natural emotion, pauses and response delay separately. Do not promise an exact match from a short clip.

- Hosted: ElevenLabs documents v4 / v4 Turbo for expressive speech and cloning. v4 uses the Text to Dialogue API and Turbo its WebSocket. The simple prototype deliberately uses the separately documented v3 Text to Speech endpoint; it is an audition baseline, not the newest realtime integration.
- Local experiment: original Chatterbox supports reference audio and an exaggeration control. The included adapter is English-only. Its multilingual model lists Hindi but not Malayalam or Tamil; do not route those languages through this adapter.
- Malayalam: compare Milo's existing Gemini speech with ElevenLabs using native Malayalam sentences before choosing. Their presence in a supported-language list is not evidence of good pronunciation for this reference.
- Training: do not start by training from scratch. Consider fine-tuning only after an audition shows a specific gap and enough suitable recordings are available.

The reference WAV is 5.6 seconds, mono, 24 kHz, 16-bit PCM. Peak is -2.41 dBFS, RMS is -16.02 dBFS, with no full-scale samples. These measurements do not establish speech clarity, speaker identity, background noise, language or emotional tone. The recording was not listened to in this environment. A longer clean single-speaker recording would give more material for comparison. Use a voice you own or have permission to reproduce; alternatively choose/design an original Milo voice with similar broad qualities.

## Run without credentials

Requires Python 3.10+. Inspection and request previews use only the standard library.

```powershell
python voice_lab.py inspect 'C:\Users\aruns\Downloads\lp8GVekznVkvnx6a2Pv_eg.wav'
python voice_lab.py eleven --text "You did it! Let's celebrate that little step." --emotion excited --preview
python -m unittest discover -v
```

## Hosted audition

Choose a library voice or create an authorized clone in ElevenLabs, then set `ELEVENLABS_API_KEY` and `ELEVENLABS_VOICE_ID` in your local environment. Do not put the key in source control or chat. This tool sends only the supplied text to ElevenLabs; it does not upload or clone the reference WAV. Live generation uses provider quota and may cost money.

```powershell
python voice_lab.py eleven --text "I'm here. We can take this one step at a time." --emotion comfort --output comfort.mp3
python voice_lab.py eleven --text "I'm here. We can take this one step at a time." --emotion neutral --output neutral.mp3
```

Tags steer performance but are not guaranteed. Listen to each result. The tool measures whole-request time, not time to first audio, and refuses to overwrite an existing output.

## Local reference audition (English)

Use a separate Python 3.11 environment, matching Chatterbox's documented development version. Install `chatterbox-tts`; follow upstream PyTorch instructions for your GPU. This dependency was not installed or tested here. The first run downloads model weights; CPU speed and Windows compatibility require measurement. Reference inference runs locally after downloads.

```powershell
python -m pip install chatterbox-tts
python voice_lab.py local --reference 'C:\Users\aruns\Downloads\lp8GVekznVkvnx6a2Pv_eg.wav' --text "Hello. I'm Milo. How was your day?" --exaggeration 0.5 --output milo-local.wav
```

Exaggeration changes intensity; it does not select a particular emotion. Compare 0.3, 0.5 and 0.7 with different output names. Keep Chatterbox's built-in watermark.

## Android integration after audition

Add the winning provider behind `MiloSpeaker`, keeping Stop/Off cancellation and utterance ordering. For hosted service, use an authenticated backend to keep an application-owned API key off the phone. Keep cloud speech opt-in and label fallback explicitly. Preserve voice identity across tones; avoid layering pitch shifts, synthetic laughter and sparkle effects over expressive generated speech. Test interruptions, network loss, quota errors, language switches and background sessions on a phone before release.

## Sources

- https://elevenlabs.io/docs/overview/models
- https://elevenlabs.io/docs/api-reference/text-to-speech/convert
- https://elevenlabs.io/docs/help-center/product/core-capabilities/text-to-speech/how-do-audio-tags-work-with-eleven-v3-and-v4
- https://github.com/resemble-ai/chatterbox

Verified: reference metadata, local validation and mocked hosted request. Not verified: live provider access, generated audio quality, matching this voice, local model runtime, Android integration or phone latency.
