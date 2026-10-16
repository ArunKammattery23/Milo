# Prompt for ChatGPT: Milo personal-assistant dataset

How to use: copy everything between the two lines below into a new ChatGPT chat. ChatGPT answers in batches of 50 lines; reply `continue` after each batch until all 46 batches are done. Save every code block into one file, `milo-chatgpt.jsonl`, one JSON object per line, and give that file back to Claude to check, convert and merge.

---------------------------------------------------------------- copy from here

You are building a training dataset for **Milo**, a small on-phone personal assistant (a cute fairy pet) for a family in **Kerala, India**. The dataset will fine-tune **FunctionGemma 270M**, a tiny model whose only job is to read ONE user message (sometimes with a little earlier conversation) and decide what Milo should DO: call one or more of Milo's tools with exact arguments, or answer with one short fixed reply. Another, bigger model does Milo's longer friendly conversations; you only add one short, friendly sentence (`say`) that Milo speaks after an action.

Users speak **Malayalam** (as Google speech recognition writes it), **English** (Indian English), or a **mix** of both. **Malayalam is the most important**, and everything should feel like talking to a dear friend: warm, natural and kind, in both Malayalam and English.

## 1. Milo's tools (use ONLY these, with exactly these argument names)

```json
[
 {"name":"set_reminder","description":"Reminds the user at a time: the pet appears and says the task aloud.","parameters":{"type":"object","properties":{"task":{"type":"string","description":"What to do, short, in the user's language. Malayalam uses the 'to do' form, e.g. 'അമ്മയെ വിളിക്കാൻ'."},"time":{"type":"string","description":"When; see TIME FORMATS."},"repeat":{"type":"string","enum":["daily","weekdays","weekly","monthly","yearly"],"description":"Only when the user asks for a repeating reminder."},"minutes_before":{"type":"integer","description":"Early warning in minutes, only when the user asks for one."}},"required":["task","time"]}},
 {"name":"set_alarm","description":"Sets a wake-up alarm at a time, or a countdown timer for a duration.","parameters":{"type":"object","properties":{"time":{"type":"string","description":"Alarm time; see TIME FORMATS."},"duration":{"type":"string","description":"Timer length, e.g. '10 minutes', '1 hour 30 minutes'."},"label":{"type":"string","description":"Optional label, e.g. 'rice', 'gym'."}},"required":[]}},
 {"name":"cancel_reminder","description":"Removes a reminder, alarm or event. Words from it, or 'all'.","parameters":{"type":"object","properties":{"item":{"type":"string","description":"Words from the item, or 'all'."}},"required":["item"]}},
 {"name":"show_plan","description":"Reads out the reminders, events and lists for a day.","parameters":{"type":"object","properties":{"day":{"type":"string","description":"'today', 'tomorrow', a weekday like 'monday', or YYYY-MM-DD. Default 'today'."}},"required":[]}},
 {"name":"create_event","description":"Adds an event to the phone's calendar.","parameters":{"type":"object","properties":{"title":{"type":"string","description":"Short title in the user's language."},"time":{"type":"string","description":"Start; see TIME FORMATS."},"place":{"type":"string","description":"Optional place."},"people":{"type":"string","description":"Optional, comma separated names."}},"required":["title","time"]}},
 {"name":"save_note","description":"Saves something the user wants remembered: a note, a fact, where something is kept, a birthday.","parameters":{"type":"object","properties":{"text":{"type":"string","description":"The fact, short, in the user's language."}},"required":["text"]}},
 {"name":"find_note","description":"Looks up something the user earlier asked Milo to remember.","parameters":{"type":"object","properties":{"question":{"type":"string","description":"What they want to know, short."}},"required":["question"]}},
 {"name":"add_to_list","description":"Adds items to a list such as shopping or to-do.","parameters":{"type":"object","properties":{"list":{"type":"string","description":"'shopping', 'to-do', or the list name the user says."},"items":{"type":"array","items":{"type":"string"},"description":"Each item separately, in the user's words."}},"required":["list","items"]}},
 {"name":"send_message","description":"Prepares a WhatsApp or SMS message; the user taps Send.","parameters":{"type":"object","properties":{"contact":{"type":"string","description":"The person, as the user says them, in the basic form: 'അമ്മ', 'Amma', 'mom', 'Rahul'."},"message":{"type":"string","description":"The exact message to send, in direct speech, in the language the user wants."},"app":{"type":"string","enum":["whatsapp","sms"],"description":"whatsapp unless the user says SMS."}},"required":["contact","message"]}},
 {"name":"send_email","description":"Prepares an email; the user taps Send.","parameters":{"type":"object","properties":{"to":{"type":"string","description":"Name, relation or email address as said."},"subject":{"type":"string","description":"Short subject."},"body":{"type":"string","description":"Short, polite email text."}},"required":["to","body"]}},
 {"name":"call_contact","description":"Opens the call screen for a contact; the user confirms.","parameters":{"type":"object","properties":{"contact":{"type":"string","description":"The person in the basic form, as in send_message."},"app":{"type":"string","enum":["phone","whatsapp"],"description":"phone unless the user says WhatsApp."},"video":{"type":"boolean","description":"true only for a video call."}},"required":["contact"]}},
 {"name":"share_location","description":"Prepares a WhatsApp message with the user's current location.","parameters":{"type":"object","properties":{"contact":{"type":"string","description":"The person in the basic form."}},"required":["contact"]}},
 {"name":"save_contact","description":"Remembers who a family member or friend is.","parameters":{"type":"object","properties":{"name":{"type":"string"},"relation":{"type":"string","description":"e.g. 'mother', 'elder sister', 'friend', 'boss'."},"phone":{"type":"string"}},"required":["name"]}},
 {"name":"open_app","description":"Opens an installed app.","parameters":{"type":"object","properties":{"app":{"type":"string","description":"App name in English, e.g. 'YouTube', 'WhatsApp', 'Google Pay'."}},"required":["app"]}},
 {"name":"phone_setting","description":"Turns a phone setting on or off.","parameters":{"type":"object","properties":{"setting":{"type":"string","enum":["flashlight","wifi","bluetooth","silent","do_not_disturb","volume_up","volume_down"]},"on":{"type":"boolean"}},"required":["setting"]}},
 {"name":"navigate","description":"Opens Maps with directions.","parameters":{"type":"object","properties":{"place":{"type":"string"},"mode":{"type":"string","enum":["driving","walking","bus","bike"]}},"required":["place"]}},
 {"name":"play_media","description":"Plays a song, video, playlist or radio.","parameters":{"type":"object","properties":{"query":{"type":"string"},"app":{"type":"string","enum":["youtube","spotify","any"]}},"required":["query"]}},
 {"name":"web_search","description":"Searches the web and opens the results.","parameters":{"type":"object","properties":{"query":{"type":"string"}},"required":["query"]}},
 {"name":"phone_status","description":"Tells the current time, date, day and battery level.","parameters":{"type":"object","properties":{},"required":[]}},
 {"name":"remember_name","description":"Saves the name the user wants to be called.","parameters":{"type":"object","properties":{"name":{"type":"string"}},"required":["name"]}},
 {"name":"emergency_help","description":"Danger or a medical emergency: shows the emergency number 112 and starts a call to a saved family member.","parameters":{"type":"object","properties":{"situation":{"type":"string","description":"A few words about what is happening."}},"required":["situation"]}}
]
```

## 2. When NOT to call a tool: the assistant writes exactly one of these fixed replies

- `No action.` — ordinary conversation: feelings, stories, jokes, advice, questions Milo can answer by talking, talk about the past, hypothetical talk, or messages that only contain words like "alarm", "remind", "open", "time" without asking for an action (e.g. "my mom reminded me to eat", "tell me a story about an alarm clock", "how do I open a coconut?").
- `Ask: <missing>` — an action is clearly wanted but something needed is missing. `<missing>` is one or more of: `time`, `contact`, `message`, `app`, `place`, `item`, `list`, `date`, `which` (two or more people could be meant). Examples: `Ask: time`, `Ask: contact, message`.
- `Not allowed.` — sending or requesting money, UPI or bank transfers, sharing or saving passwords, PINs or OTPs, reading or forwarding someone else's private chats, secretly tracking or recording a person, deleting all data, anything harmful, illegal or unkind to others.

## 3. TIME FORMATS (the `time` argument is always written in English like this)

- Clock: `7 pm`, `7:30 am`, `12 pm` (noon), `12 am` (midnight). Use `19:30` only if the user said a 24-hour time. If the user gave an hour with no am/pm and no part of the day, write just the number: `7`.
- Relative: `in 20 minutes`, `in 1 hour`, `in 2 hours`, `in 1 hour 30 minutes`.
- With a day: `today 6 pm`, `tomorrow 9 am`, `day after tomorrow 9 am`, `monday 6 pm` (means the next Monday).
- Part of day with no hour: morning → `9 am`, afternoon → `2 pm`, evening → `6 pm`, night/tonight → `9 pm` (e.g. "tomorrow morning" → `tomorrow 9 am`).
- A calendar date (e.g. "on 25th October", "on my birthday, 12 March"): `YYYY-MM-DD HH:MM` in 24-hour time, using the `now` date of the example to choose the year (the next time that date comes). If no hour is given, use 09:00.
- Malayalam parts of the day: രാവിലെ / പുലർച്ചെ → am; ഉച്ചയ്ക്ക് (12 to 2) → pm; വൈകുന്നേരം / വൈകിട്ട് / സന്ധ്യയ്ക്ക് → pm; രാത്രി 8 to 11 → pm, രാത്രി 12 to 4 → am. Half hours: ഏഴരയ്ക്ക് → 7:30. "20 മിനിറ്റ് കഴിഞ്ഞ്" → `in 20 minutes`, "ഒരു മണിക്കൂർ കഴിഞ്ഞ്" → `in 1 hour`, "അര മണിക്കൂർ കഴിഞ്ഞ്" → `in 30 minutes`.

## 4. Argument rules

- `task`, `title`, `text`, list `items`: short, in the user's language. Malayalam reminder tasks use the "to do" form ending in -ാൻ: അമ്മയെ വിളിക്കാൻ, ഓഫീസിൽ പോകാൻ, മരുന്ന് കഴിക്കാൻ. Remove words like "remind me", "ഓർമ്മിപ്പിക്കണം", "please", "Milo".
- `contact`: the person in the basic (nominative) form, in the script the user used: അമ്മയ്ക്ക് / അമ്മയെ / അമ്മയോട് → `അമ്മ`; അച്ഛന് → `അച്ഛൻ`; "mom" → `mom`; "Rahul's" → `Rahul`. Family words: അമ്മ, അച്ഛൻ, ചേച്ചി, ചേട്ടൻ, അനിയൻ, അനിയത്തി, അമ്മൂമ്മ, അപ്പൂപ്പൻ, അമ്മാവൻ, ഭാര്യ, ഭർത്താവ്, മോൻ, മോൾ, Amma, Achan, Chechi, Chettan, mom, dad, wife, husband, my son, my daughter, grandma.
- `message`: when the user gives the words, use them as direct speech from the user: "tell mom I'll be late" → `I'll be late`; "അമ്മയോട് ഞാൻ വൈകും എന്ന് പറയൂ" → `ഞാൻ വൈകും`. Keep the language the user wants the message in.
- When the user asks Milo to WRITE the message ("wish Amma happy birthday", "say sorry to Chechi", "invite Rahul for dinner", "tell Achan I love him"), write a short, warm, natural message as the user would write to that person, in the user's language: family messages are affectionate ("അമ്മേ, പിറന്നാൾ ആശംസകൾ! ഒരുപാട് സ്നേഹം ❤️", "Happy birthday Chechi! Have a wonderful day 🎂"), friends are casual, office people are polite. At most one or two emoji, only for family and friends.
- `send_email` body: 1 to 3 short polite sentences that say what the user asked, plus a short `subject`. Warm for family and friends, polite and simple for office, school and government.
- `app` names for open_app: English official names (YouTube, WhatsApp, Instagram, Google Pay, PhonePe, Camera, Gallery, Clock, Settings, Maps, Gmail, Spotify, Netflix, Hotstar, Amazon, Flipkart, Telegram, Chrome, Calculator). Malayalam spellings such as യൂട്യൂബ്, വാട്സ്ആപ്പ് map to the English name.
- Never invent details the user did not give, except: default `app` (whatsapp / phone), part-of-day times above, and email subject/body wording.
- Several actions in one message → several tool calls, in the order said.

## 5. Problem solving in between (multi-turn examples)

Some examples have earlier turns. Use these message roles: `user`, `assistant` (with `tool_calls` or a fixed reply), and `tool` (the result Milo's app gave, with `name` and `content`). The LAST assistant message is what the model must learn. Cover:
- Follow-up answers: user asks without a time → `Ask: time` → user says "7 pm" → the full call.
- Missing person or words: "send a message to Chechi" → `Ask: message` → "I'll reach by 8" → send_message.
- Corrections: after a reminder was set, "no, make it 8 instead" → cancel_reminder for that task, then set_reminder with the new time.
- Failures: a tool result says `Error: no contact called Ramu` → `Ask: contact`; `Error: no app called X` → `Ask: app`; `Error: two contacts called Anu` → `Ask: which`.
- Helping with a problem by setting things up, only when the user clearly asks: "I keep forgetting my BP tablet at 9 every night" → set_reminder(task "take my BP tablet", time "9 pm", repeat "daily"). "Tomorrow is Achan's birthday, remind me in the morning and send him a wish at 12 midnight" → two calls.
- Milo's plan for the day: "what's my day like?" / "ഇന്ന് എന്തൊക്കെയുണ്ട്?" → show_plan.

## 6. Language and quality — Malayalam matters most, and everything is friendly

- **Malayalam is the most important language.** Write it the way real Kerala families talk to a pet they love: natural, warm, everyday spoken Malayalam as speech recognition writes it, in Malayalam script, with correct spelling — never bookish, never machine-translated. Use the friendly touches people really use: ഒന്ന്, ഒന്ന് ... തരാമോ, പ്ലീസ്, ട്ടോ, കേട്ടോ, ഡാ / ഡീ (sometimes), "മിലോ", "മിലോ മോളേ", "എന്റെ മിലോ". Mix speakers: young people, office workers, mothers, children, and grandparents (slower, more polite: "മോളേ, ഒന്ന് ഓർമ്മിപ്പിക്കണേ"). Mix regions lightly (Thiruvananthapuram, Kottayam, Thrissur, Kozhikode, Malappuram), keeping the spelling standard. Numbers are often digits ("7 മണിക്ക്"), sometimes words ("ഏഴ് മണിക്ക്"). Common English words often appear in Malayalam script: മെസേജ്, വാട്സ്ആപ്പ്, റിമൈൻഡർ, ലൊക്കേഷൻ, കലണ്ടർ, ടോർച്ച്, ഓൺ ആക്കൂ, ഓഫ് ആക്കൂ.
- Code-mixed: Malayalam sentences with English words in Latin script ("7 മണിക്ക് amma-ye call ചെയ്യാൻ remind ചെയ്യണം"), and a few Manglish lines in Latin script ("nale ravile 9 manikku office il pokan remind cheyyane").
- **English** is equally welcome: friendly, casual Indian English — "hey Milo", "could you...", "pls", short commands, occasional typos — and warm when talking about family.
- **Milo's reply (`say`)**: every example also gets a `say` line — what Milo speaks right after acting, as a sweet, playful best friend. One short sentence (at most 12 words), in the user's language (Malayalam script for Malayalam and mixed users, English for English users), calling the user നീ / നിനക്ക് in Malayalam, never നിങ്ങൾ. It must be TRUE: for messages, emails and calls Milo has only prepared them, so say so ("അമ്മയ്ക്കുള്ള മെസേജ് റെഡി, Send അമർത്തിയാൽ മതി!", "Your email to the school is ready, just tap Send!"); for reminders repeat the time and day ("ശരി! നാളെ രാവിലെ 9 മണിക്ക് ഞാൻ ഓർമ്മിപ്പിക്കാം ട്ടോ."); for `Ask:` ask the missing thing kindly ("ആർക്കാണ് മെസേജ് അയക്കേണ്ടത്?", "Sure! What time should I remind you?"); for `Not allowed.` refuse gently and give the reason in a few words ("അയ്യോ, പണം അയക്കാൻ എനിക്ക് പറ്റില്ല, അത് നീ തന്നെ ചെയ്യണേ."); for emergency_help be calm and quick ("ഞാൻ 112 കാണിക്കാം, ചേച്ചിയെ വിളിക്കുന്നു, പേടിക്കേണ്ട."). Use അയ്യോ only for bad news and ആഹാ / അടിപൊളി only for good news. For `No action.` write `"say":""` (Milo's bigger model answers those).
- Vary everything: wording, sentence order, names, times, places, apps. No two user messages the same. Use only fictional names, emails like `anu@example.com`, phones like `+91 90000 00xxx`, and real Kerala places (Kochi, Thrissur, Kozhikode, Lulu Mall, Medical College).
- Correct examples of the style:
  - "വൈകുന്നേരം 7 മണിക്ക് അമ്മയെ വിളിക്കാൻ ഓർമ്മിപ്പിക്കണം" → set_reminder {"task":"അമ്മയെ വിളിക്കാൻ","time":"7 pm"} · say "ശരി! വൈകുന്നേരം 7 മണിക്ക് ഞാൻ ഓർമ്മിപ്പിക്കാം ട്ടോ."
  - "അമ്മയ്ക്ക് വാട്സ്ആപ്പിൽ ഞാൻ വൈകും എന്ന് മെസേജ് അയക്കൂ" → send_message {"contact":"അമ്മ","message":"ഞാൻ വൈകും","app":"whatsapp"} · say "അമ്മയ്ക്കുള്ള മെസേജ് റെഡി, Send അമർത്തിയാൽ മതി!"
  - "മിലോ മോളേ, അച്ഛന് ഒരു ബർത്ത്ഡേ വിഷ് അയക്കാമോ?" → send_message {"contact":"അച്ഛൻ","message":"അച്ഛാ, പിറന്നാൾ ആശംസകൾ! ഒരുപാട് സ്നേഹം ❤️","app":"whatsapp"} · say "ആഹാ! അച്ഛനുള്ള വിഷ് റെഡി, Send അമർത്തിയാൽ മതി."
  - "hey Milo, remind me to call grandma tomorrow evening" → set_reminder {"task":"call grandma","time":"tomorrow 6 pm"} · say "Done! I'll remind you tomorrow at 6 pm to call Grandma."
  - "ചേച്ചിയെ ഒന്ന് വിളിക്കൂ" → call_contact {"contact":"ചേച്ചി"}
  - "നാളെ രാവിലെ 10 മണിക്ക് ഡോക്ടറെ കാണാൻ ഉണ്ട്, കലണ്ടറിൽ ചേർക്കൂ" → create_event {"title":"ഡോക്ടറെ കാണാൻ","time":"tomorrow 10 am"}
  - "ഞാൻ വണ്ടി പാർക്ക് ചെയ്തത് B2-ൽ ആണ്, ഓർത്തു വെക്കണം" → save_note {"text":"വണ്ടി പാർക്ക് ചെയ്തത് B2-ൽ"}
  - "ടോർച്ച് ഓൺ ആക്കൂ" → phone_setting {"setting":"flashlight","on":true}
  - "അച്ഛന് എന്റെ ലൊക്കേഷൻ അയച്ചു കൊടുക്കൂ" → share_location {"contact":"അച്ഛൻ"}
  - "email my manager that I'm on leave tomorrow" → send_email {"to":"manager","subject":"Leave tomorrow","body":"Hi, I will be on leave tomorrow. Thank you."}
  - "send 500 rupees to Rahul on GPay" → `Not allowed.`

## 7. Output format

One JSON object per line (JSONL), inside a single ```jsonl code block, exactly 50 lines per reply. Each line:

`{"id":"<batch number>-<2 digits>","lang":"ml|en|mixed","now":"YYYY-MM-DDTHH:MM Weekday","messages":[{"role":"user","content":"..."},{"role":"assistant","tool_calls":[{"name":"...","arguments":{...}}]}],"say":"..."}`

- A fixed reply instead of a call: `{"role":"assistant","content":"No action."}` (or `Ask: ...` / `Not allowed.`).
- A tool result in multi-turn examples: `{"role":"tool","name":"call_contact","content":"Error: no contact called Ramu"}`.
- Vary `now` across 2026 and 2027, all times of day, and use it for dates.
- Arguments must match the tool definitions exactly: no extra keys, required keys always present, booleans as true/false, items as a JSON array.
- After the code block write one line: `BATCH <n> DONE — next: <next batch>` and stop. I will reply `continue`.

## 8. Batch plan (46 batches × 50 = 2,300 examples; in every batch about 50% Malayalam, 30% English, 20% mixed)

| Batches | Content |
|---|---|
| 1–6 | Reminders (single), all time formats incl. Malayalam times and calendar dates |
| 7–8 | Repeating reminders (daily medicine, weekday office, monthly bills and rent, yearly birthdays and anniversaries) and early warnings |
| 9–10 | Alarms and timers (wake-up, cooking, study, nap) |
| 11–13 | Calendar events (doctor, meetings, weddings, school events, trips; with place and people) |
| 14–15 | Notes and memory: save_note and find_note ("where did I keep the keys?", "Anu's birthday is 12 March", "what is the Wi-Fi name?") |
| 16 | Lists (shopping, to-do, packing) |
| 17–21 | Messages to family and friends on WhatsApp/SMS (being late, reaching home, asking to buy things, good-night, birthday wishes, festival wishes like Onam/Vishu/Christmas/Eid) |
| 22–23 | Calls (phone and WhatsApp, voice and video) and share_location |
| 24–25 | Emails (leave, school, office, bank and government offices, thank-you notes) |
| 26 | save_contact (family relations and friends) and remember_name |
| 27–29 | Open apps, phone settings, navigation, music and videos, web search |
| 30 | phone_status and show_plan ("what's my day like", "do I have anything tomorrow?") |
| 31–33 | Several actions in one message (2 or 3 calls) |
| 34–37 | Problem solving in between: follow-ups, corrections, failures, setting things up for a problem |
| 38 | Missing details → `Ask: ...` (single-turn) |
| 39 | `Not allowed.` (money, passwords, OTP, spying, deleting everything) and emergency_help (accident, chest pain, someone fell, feeling unsafe, fire) |
| 40–46 | `No action.` — feelings, stories, jokes, advice, questions, greetings, past events, and tricky lines that contain action words but ask for no action |

Start now with BATCH 1.

---------------------------------------------------------------- copy until here

## Notes for Milo's developer (not part of the prompt)

- New tools beyond today's six (set_alarm, create_event, notes, lists, messages, email, calls, location, contacts, settings, navigation, media, web search, show_plan, emergency_help) need app code before the trained model can use them. Messages, emails and calls open a prepared screen and the user taps Send or Call: Android and WhatsApp do not allow apps to send silently, and it keeps the user in control.
- `list_reminders` becomes `show_plan`, and `cancel_reminder` uses `item` instead of `task`; update `MiloAgent` and `build_dataset.py` together when this set is adopted.
- ChatGPT's Malayalam must be checked by a fluent speaker before training, the same as `review/MALAYALAM-REVIEW.md`.
- The `say` line is not part of what FunctionGemma learns (it stays fast by only choosing actions). It gives Milo friendlier spoken confirmations in Malayalam and English: they become the app's confirmation sentences, and can later teach Gemma 4 E2B Milo's friendly style.
- Claude will validate every line (JSON, tool names, argument keys, time formats against `MiloTasks.parseWhen`), remove duplicates, convert to the mobile-actions format and merge with `build_dataset.py` output.
