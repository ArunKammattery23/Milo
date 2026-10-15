# Milo original bilingual voice — self-recording kit

Prepared 15 October 2026. Status: voice direction and recording script ready; original recordings and model auditions pending. No model has been trained or voice match verified.

## Voice direction

Milo should sound warm, approachable and gently expressive, with conversational Kerala Malayalam and clear, natural English. Keep the same comfortable voice in both languages. Smile lightly for happy lines, soften your delivery for comfort, and use restrained emphasis for frustration. Avoid forcing a high pitch, shouting, artificial laughter or an imitation of Leda. This is an original performance.

The desired sweet female character voice is a target to audition, not a transformation guarantee. A reference model generally preserves your speaker characteristics. If your natural voice is different from the intended character, the first recordings will test the pipeline; a different consenting performer may be needed for the final voice.

## First session — two files only

Use your phone's recorder in a quiet, softly furnished room. Keep the phone about 15–20 cm away and slightly off-axis. Record without music, effects or background conversations. Keep the microphone position unchanged. Use WAV if available; otherwise send the original recorder file rather than converting through a messaging app. Preserve original quality; we can make model-specific copies later.

Read only the spoken text, not the filename. Leave a brief pause before and after. Repeat the take if you stumble, rather than editing individual words. Aim for comfortable speech, not a strict duration. Do not record private information.

**milo-ml-neutral-t01.wav**

ഹായ്, ഞാൻ മിലോ. ഇന്ന് എങ്ങനെയുണ്ടായിരുന്നു? നമുക്ക് പതുക്കെ സംസാരിക്കാം. ആദ്യം ഒരു ചെറിയ കാര്യം ചെയ്യാം. പിന്നെ അടുത്തത് തീരുമാനിക്കാം.

**milo-en-neutral-t01.wav**

Hi, I'm Milo. How was your day? We can take our time. Let's begin with one small thing, and then decide what to do next.

Send these two first. Check clarity and comfort before recording the full set. These are short reference auditions, not a sufficient fine-tuning dataset.

## Second session — matching emotions

Record each row as a separate clip, twice: t01 and t02. Keep both takes. The Malayalam lines are a draft; adjust them to your natural dialect before recording, and supply the exact final words you actually said.

| Filename stem | Delivery | Spoken text |
|---|---|---|
| milo-ml-happy | Light smile, gentle energy | നിന്നെ കണ്ടതിൽ സന്തോഷം! ഇന്ന് എന്തെങ്കിലും നല്ല കാര്യം നടന്നോ? പറയൂ, കേൾക്കാൻ ആഗ്രഹമുണ്ട്. |
| milo-en-happy | Light smile, gentle energy | It's lovely to see you! Did something good happen today? I'd love to hear about it. |
| milo-ml-comfort | Warm and steady | ഇന്ന് കുറച്ചു ബുദ്ധിമുട്ടായിരുന്നല്ലേ? സാരമില്ല. ഒരു ചെറിയ ഇടവേള എടുക്കാം. എല്ലാം ഇപ്പോൾ തന്നെ ചെയ്യേണ്ടതില്ല. |
| milo-en-comfort | Warm and steady | It's been a difficult day, hasn't it? We can pause for a moment. You don't have to do everything right now. |
| milo-ml-sad | Quiet sympathy, no crying | അങ്ങനെ സംഭവിച്ചതിൽ വിഷമമുണ്ട്. പറയാൻ തോന്നുന്നുണ്ടെങ്കിൽ പറയൂ. ഞാൻ കേൾക്കാം. |
| milo-en-sad | Quiet sympathy, no crying | I'm sorry that happened. If you feel like talking about it, I'm here to listen. |
| milo-ml-excited | Brighter energy, no shout | നീ അത് ചെയ്തു! ചെറിയൊരു ചുവടാണെങ്കിലും അത് മുന്നോട്ടുള്ള ചുവടാണ്. ഇനി എന്ത് ചെയ്യണം എന്ന് നമുക്ക് നോക്കാം. |
| milo-en-excited | Brighter energy, no shout | You did it! Even a small step is progress. Let's see what you'd like to try next. |
| milo-ml-frustrated | Mild frustration about an event | അയ്യോ, വീണ്ടും കണക്ഷൻ പോയി. ഇത് കുറച്ചു ബുദ്ധിമുട്ടാണ്. ശരി, നമുക്ക് വീണ്ടും ശ്രമിക്കാം. |
| milo-en-frustrated | Mild frustration about an event | Oh no, the connection dropped again. That's a little frustrating. All right, let's try once more. |
| milo-ml-curious | Interested question | നിനക്ക് ഏറ്റവും ഇഷ്ടമുള്ള പാട്ട് ഏതാണ്? അതിൽ എന്താണ് നിനക്ക് ഇഷ്ടം? |
| milo-en-curious | Interested question | What's your favourite song? What do you like most about it? |

Filename example: `milo-ml-happy-t01.wav`. Keep the same speaker across every row. Save exact transcripts beside the audio. If you change a word, update its transcript.

## Unseen evaluation lines — do not use as model reference prompts

After selecting reference clips, generate these NEW sentences to test generalisation. You may later record a separate human comparison, but never feed that comparison back as the reference for the same test.

1. Malayalam neutral: ഇന്ന് വൈകുന്നേരം ഒരു ചെറിയ നടത്തത്തിന് പോയാലോ?
2. English neutral: Would you like to take a short walk this evening?
3. Malayalam comfort: ഇപ്പോൾ ഉത്തരം പറയേണ്ടതില്ല. നിനക്ക് സമയം എടുക്കാം.
4. English comfort: You don't need to answer right away. Take your time.
5. Malayalam happy: ഇന്നത്തെ ചെറിയ വിജയം നമുക്ക് ഓർത്തുവയ്ക്കാം!
6. English happy: Let's remember this little win from today!
7. Mixed: ഇന്നത്തെ plan വളരെ simple ആണ്. Let's start with one small step.
8. Malayalam pronunciation: മഴ കഴിഞ്ഞപ്പോൾ വഴിയരികിലെ പുഴ നിറഞ്ഞൊഴുകി.
9. English pronunciation: Please check the schedule before choosing your next activity.
10. Number handling: നാളെ രാവിലെ ഒമ്പത് മണിക്ക് പത്ത് മിനിറ്റ് വായിക്കാം.

## Reference-model audition plan

For Malayalam, begin by evaluating AI4Bharat IndicF5. Its official model card lists Malayalam and accepts reference audio plus an exact transcript. It does not list English, so do not claim it solves the bilingual requirement alone. Access is gated; the account holder must review its access conditions.

For English, compare the same person's English recordings through the existing Chatterbox adapter in `voice_lab.py`. Two separate engines may sound different even with the same performer. Cross-language speaker consistency is an explicit test, not an assumption. Mixed-language lines remain unsupported by this two-engine baseline unless testing establishes a reliable route.

IndicF5 now points to Indic-Speak as its successor. Indic-Speak lists Malayalam and English, but its documented interface uses trained speaker names rather than arbitrary reference WAVs. Its custom licence also places conditions on hosting for others. Treat it as a separate stock-voice candidate, not a drop-in reference-cloning solution for paid Milo users.

Test neutral references first, then matching happy and comforting references. Generate three outputs for each evaluation line to check consistency. Keep model version, settings, reference filename, exact transcript, generated text, duration, loading time and generation time with each result. Do not upload the generated reference samples as training data. Their origin and reuse permissions have not been established.

## Decision gate before fine-tuning

Have a fluent Malayalam listener and an English listener score each output 1–5 for intelligibility, pronunciation, naturalness, speaker similarity and appropriate emotion. Listen to consecutive Malayalam/English lines for voice changes. Note skipped/repeated words and unwanted extra speech separately. Suggested project target: average at least 4/5 for pronunciation and naturalness in EACH language, no critical word errors, and an acceptable same-speaker impression across languages. These are proposed acceptance targets, not measured results.

Measure response latency on the intended hardware before estimating hosting cost. File generation time is not streaming time-to-first-audio. Fine-tune only if a reference model misses a clearly identified requirement and its licence, architecture and data needs fit that requirement. Choose the model before commissioning hours of recordings. Keep evaluation sentences out of training.

## References checked 15 October 2026

- https://huggingface.co/ai4bharat/IndicF5
- https://github.com/AI4Bharat/IndicF5
- https://github.com/resemble-ai/chatterbox
- https://huggingface.co/bodhan-ai/indic-speak

No actor has been hired, no weights downloaded, no recording uploaded, no speech generated and no training started as part of preparing this kit. The immediate next step is the two original neutral recordings above.
