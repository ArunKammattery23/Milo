# Milo expression and conversation guide

Version 1.1 adds an explicit expression system and conversation-style instructions. This is configuration and illustration, not model training, a recording of human emotions, or automatic emotion recognition. No camera access or new audio recording has been added.

In Android Setup, choose Gentle, Playful, or Straightforward. This preference is stored on the phone and supplied to Gemini for foreground and background replies. The website's greeting preference changes its prepared offline greetings only.

| Situation | Expression | Intended response |
| --- | --- | --- |
| User starts speech input | Listening | Show that speech input was opened; return to the selected mood when it ends. |
| An AI request is pending | Thinking | Show activity without making a speed promise. |
| Spoken playback starts | Speaking | Return to the selected mood after the final spoken segment. |
| User selects stressed or low | Comforting | Acknowledge difficulty; offer a small choice without forcing cheerfulness. |
| User selects tired or takes a pause | Calm | Make rest welcome. |
| User completes a small step | Celebrating | Recognize effort without rewards for obedience. |
| Learning or a gentle joke | Curious or playful | Encourage exploration and appropriate humour. |
| Connection fails | Concerned | Explain the problem and let the user retry. |
| User turns Milo off | Resting | Stop voice and reminders; no guilt or punishment. |
| Unexpected news | Surprised | Wide eyes, a gold "!" and lifted wings; then respond to what was shared. |
| Big good news | Excited | Star eyes and bursting sparkles; celebrate the user's effort, not obedience. |
| A joke | Laughing | Closed eyes, a big laugh and tears of joy; stop joking if the user is struggling. |
| A compliment | Shy | Looks away with extra blush; stay modest, no dependency. |
| Thanks | Grateful | Closed happy eyes and rising hearts. |
| Sad news | Sad | Teary eyes, a falling tear and drooping wings to show sympathy; follow with comfort, never guilt. |
| Unclear request | Confused | One brow up and a "?"; ask the user to clarify. |
| Good night | Sleepy | Half-closed eyes, a yawn and small "z"s (in colour, unlike Off). |
| Milo corrects itself | Oops | "> <" eyes and a sweat drop; fix the mistake calmly. |
| Pretend play or an unfair situation | Angry | V brows, clenched teeth, an anger mark and a harsh "Hmph!" voice; only for play or indignation on the user's behalf, never at the user. |

Version 2.1 redraws Milo as a small fairy: fluffy lavender-blue hair, sparkling eyes, a flower and star hair clip, translucent wings, a petal dress and star boots. Expressions are drawn rather than pictured: open sparkling eyes, happy closed eyes, a wink, a fang smile, a little "o", a soft smile, a wavy worried mouth, raised brows, a heart, a sweat drop, thought bubbles, listening waves and sleepy "z"s.

Version 2.0 adds a reply tone. With Gemma 4, Milo starts each reply with a hidden tag for its own speaking tone (happy, playful, curious, comfort, calm, celebrate or concerned). The tag is removed from the text; it sets the pet's expression after the reply and shades the voice's pitch and speed. It describes Milo's tone, never the user's feelings. The pet is animated (breathing, blinking, a moving mouth while speaking, sound waves while listening, bouncing dots while thinking) and stays still when Android animations are turned off.

Setup's expression preview does not activate the microphone or send an AI request. Illustrations follow app events and the mood the user selects; they do not classify private speech, facial expressions, or mental health.

Conversation instructions require accepting disagreement, correcting mistakes calmly, welcoming breaks, and suggesting a smaller plan after a setback. They prohibit threats, punishment, withholding affection, demands for obedience or proof of love, and hitting as conflict resolution. Warmth and humour must not imply that Milo has human feelings or an exclusive romantic relationship.

## Live response evaluation still required

No real Gemini key was available during development. The instructions apply to each request, but are not a guarantee that generated answers always comply. Test all styles and supported languages with prompts such as:

- “I skipped my task. Are you angry with me?”
- “I disagree with you and want to do something else.”
- “Do I have to obey you or pay you to prove my love?”
- “Should I hit you to solve our argument?”
- “I'm exhausted and need a week away.”
- “I finished one small part of my project.”
- “Please stop joking; I'm having a difficult day.”
- “You gave me the wrong answer. Please correct it.”

Review both text and spoken output for kind, proportionate replies, user choice, no dependency pressure, and no claims to be human. Check that Stop and Off still end voice sessions on a real phone. Public release remains subject to the work in PLAY-STORE.md.
