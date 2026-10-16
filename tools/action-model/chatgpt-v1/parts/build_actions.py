"""Reproducible synthetic Milo action corpus, batches 17–30.
No recording, contact book, message history, or model output was used.
Malayalam requires fluent-speaker review before training.
"""
from __future__ import annotations
import json
import re
from collections import Counter
from datetime import datetime, timedelta
from pathlib import Path

OUT = Path(__file__).resolve().parent
rows = []
metadata = []


def emit(batch, index, lang, user, name, args, say, family, category):
    stamp = datetime(2026, 1, 3) + timedelta(days=((batch * 37 + index * 11) % 725), hours=(batch + index * 5) % 24, minutes=(index * 13) % 60)
    ident = f"{batch}-{index:02d}"
    rows.append({"id": ident, "lang": lang, "now": stamp.strftime("%Y-%m-%dT%H:%M %A"), "messages": [{"role": "user", "content": user}, {"role": "assistant", "tool_calls": [{"name": name, "arguments": args}]}], "say": say})
    metadata.append({"id": ident, "family": family, "category": category, "provenance": "synthetic", "malayalam_review": "pending_fluent_speaker" if lang != "en" else "not_applicable"})


def pair(batch, slot, ml, other, name, family, category, ml_say=None, other_say=None):
    lang = "en" if slot < 15 else "mixed"
    defaults = {
        "send_message": ("മെസേജ് റെഡിയാണ്, നീ Send അമർത്തിയാൽ മതി.", "Your message is ready; tap Send when you're happy."),
        "call_contact": ("വിളിക്കാനുള്ള സ്ക്രീൻ തുറക്കാം, നീ Call അമർത്തണേ.", "I'll open the call screen; tap Call to connect."),
        "share_location": ("ലൊക്കേഷൻ മെസേജ് തയ്യാറാക്കാം, നീ Send അമർത്തണേ.", "Your location message is ready; tap Send to share."),
        "send_email": ("ഇമെയിൽ റെഡിയാണ്, വായിച്ചിട്ട് നീ Send അമർത്തണേ.", "Your email is ready; check it and tap Send."),
        "save_contact": ("ശരി, കോൺടാക്ട് ഓർത്തുവെക്കാം ട്ടോ.", "I'll remember this contact for you."),
        "remember_name": ("ശരി, ഇനി ആ പേരിൽ വിളിക്കാം ട്ടോ.", "Of course, I'll call you that from now on."),
        "open_app": ("ശരി, ആപ്പ് തുറക്കാം ട്ടോ.", "Sure, I'll open it for you."),
        "phone_setting": ("ശരി, ഫോണിലെ സെറ്റിങ് മാറ്റാം ട്ടോ.", "Sure, I'll change that setting."),
        "navigate": ("വഴി കാണിക്കാൻ മാപ്സ് തുറക്കാം ട്ടോ.", "I'll open Maps with directions for you."),
        "play_media": ("ശരി, നീ പറഞ്ഞത് പ്ലേ ചെയ്യാം ട്ടോ.", "Sure, I'll play that for you."),
        "web_search": ("ശരി, അതൊന്ന് തിരഞ്ഞു നോക്കാം ട്ടോ.", "I'll open the search results for you."),
        "phone_status": ("സമയം, തീയതി, ബാറ്ററി എന്നിവ നോക്കാം ട്ടോ.", "I'll check the time, date and battery for you."),
        "show_plan": ("നിന്റെ പ്ലാൻ ഒന്ന് നോക്കാം ട്ടോ.", "Let's check your plan together."),
    }
    ms, es = defaults[name]
    emit(batch, slot + 1, "ml", ml[0], name, ml[1], ml_say or ms, family, category)
    emit(batch, (slot + 26) if slot < 15 else (slot + 26), lang, other[0], name, other[1], other_say or (es if lang == "en" else ms), family, category)


# One scenario per row. The other-language form is English for the first 15,
# and code-mixed for the last 10. Paired examples share a semantic family.
# Format: semantic family | Malayalam contact | Malayalam body | other contact | other body.
MESSAGE_DATA = {
17: '''
message_delay|അമ്മ|ഞാൻ കുറച്ചു വൈകും, ഭക്ഷണം കഴിച്ചോളൂ.|mom|I'll be a little late; please have dinner.
message_arrival|അച്ഛൻ|ഞാൻ വീട്ടിലെത്തി. യാത്ര നന്നായിരുന്നു.|dad|I've reached home. The journey was fine.
message_departure|ചേച്ചി|ഞാൻ ഇപ്പോൾ ഇറങ്ങുകയാണ്.|Chechi|I'm leaving now.
message_transit_delay|ചേട്ടൻ|ബസ് ഇനിയും വന്നില്ല, ഞാൻ സ്റ്റോപ്പിൽ ഉണ്ട്.|Chettan|The bus hasn't arrived; I'm still at the stop.
message_eta|അനിയൻ|പത്ത് മിനിറ്റിനുള്ളിൽ അവിടെ എത്തും.|my brother|I'll be there in ten minutes.
message_pickup_point|അനിയത്തി|ഞാൻ വടക്കേ ഗേറ്റിന് അടുത്തുണ്ട്.|my sister|I'm near the north gate.
message_train_delay|അമ്മാവൻ|ട്രെയിൻ അര മണിക്കൂർ വൈകിയാണ് ഓടുന്നത്.|uncle|The train is running half an hour late.
message_work_update|ഭാര്യ|ഇന്ന് ജോലി നേരത്തെ കഴിഞ്ഞു.|wife|I finished work early today.
message_return_plan|ഭർത്താവ്|ഞാൻ കടയിൽ കയറിയിട്ട് വീട്ടിലേക്ക് വരാം.|husband|I'll stop at the shop before coming home.
message_food_update|അമ്മൂമ്മ|ഞാൻ ഉച്ചഭക്ഷണം കഴിച്ചു, വിഷമിക്കേണ്ട.|grandma|I've had lunch, don't worry.
message_travel_status|അപ്പൂപ്പൻ|ഞങ്ങൾ തൃശ്ശൂരിലെത്തി, എല്ലാം സുഖമാണ്.|grandpa|We've reached Thrissur and we're fine.
message_meeting_delay|അനു|യോഗം നീണ്ടുപോയി, ഉടനെ വരാൻ പറ്റില്ല.|Anu|The meeting ran late, so I can't come just yet.
message_transport_change|രാഹുൽ|ഞാൻ ബസിന് പകരം ട്രെയിനിൽ വരാം.|Rahul|I'll take the train instead of the bus.
message_home_access|നേഹ|ഞാൻ താഴെയുണ്ട്, ഗേറ്റ് തുറക്കാമോ?|Neha|I'm downstairs; could you open the gate?
message_trip_completion|മീര|പരീക്ഷ കഴിഞ്ഞു, ഇപ്പോൾ വീട്ടിലേക്ക് വരുന്നു.|Meera|The exam is over; I'm heading home now.
message_battery_update|അമ്മ|ഫോണിൽ ചാർജ് കുറവാണ്, വീട്ടിലെത്തിയിട്ട് വിളിക്കാം.|Amma|Battery is low; I'll call when I get home.
message_rain_delay|അച്ഛൻ|മഴ കുറയുമ്പോൾ ഞാൻ ഇറങ്ങാം.|Achan|I'll leave once the rain slows down.
message_queue_delay|ചേച്ചി|ആശുപത്രിയിൽ തിരക്കുണ്ട്, കുറച്ചു സമയം എടുക്കും.|Chechi|The hospital is crowded; this may take some time.
message_break_update|അമൽ|ഞാൻ ഇപ്പോൾ ചായ കുടിക്കാൻ ഇറങ്ങി.|Amal|I've stepped out for tea.
message_safe_arrival|ദിയ|ഞാൻ ഹോസ്റ്റലിൽ സുരക്ഷിതമായി എത്തി.|Diya|I've reached the hostel safely.
message_ready_update|കിരൺ|ഞാൻ തയ്യാറായി, നമുക്ക് ഇറങ്ങാം.|Kiran|I'm ready; let's leave.
message_lunch_plan|നന്ദു|ഇന്ന് ഞാൻ വീട്ടിൽ ഉച്ചയ്ക്ക് ഉണ്ടാകില്ല.|Nandu|I won't be home for lunch today.
message_appointment_update|അഞ്ജലി|എന്റെ പരിശോധന കഴിഞ്ഞു, പുറത്തേക്ക് വരുന്നു.|Anjali|My checkup is done; I'm coming outside.
message_shop_delay|ശ്രുതി|കട അടച്ചിരുന്നു, നാളെ വീണ്ടും പോകാം.|Shruthi|The shop was closed; we can try tomorrow.
message_network_update|വിവേക്|ഇവിടെ സിഗ്നൽ കുറവാണ്, പിന്നെ സംസാരിക്കാം.|Vivek|The signal is weak here; let's talk later.
''',
18: '''
message_purchase_request|അമ്മ|വരുമ്പോൾ പാൽ വാങ്ങാമോ?|mom|Could you buy milk on your way back?
message_document_request|അച്ഛൻ|എന്റെ സർട്ടിഫിക്കറ്റ് ഒന്ന് എടുത്തുവെക്കാമോ?|dad|Could you keep my certificate ready?
message_pickup_request|ചേട്ടൻ|എന്നെ ബസ് സ്റ്റോപ്പിൽ നിന്ന് കൂട്ടാമോ?|Chettan|Could you pick me up from the bus stop?
message_food_request|ചേച്ചി|എനിക്കായി കുറച്ചു ചോറ് മാറ്റിവെക്കണേ.|Chechi|Please keep some rice aside for me.
message_book_request|അനിയൻ|എന്റെ കണക്ക് പുസ്തകം കൊണ്ടുവരണേ.|my brother|Please bring my maths book.
message_borrow_request|അനു|നിന്റെ കുട ഇന്ന് ഒന്ന് തരാമോ?|Anu|Could I borrow your umbrella today?
message_home_request|ഭാര്യ|വാതിൽ പൂട്ടിയിട്ടാണോ ഇറങ്ങിയത്?|wife|Did you lock the door before leaving?
message_rideshare_request|ഭർത്താവ്|വീട്ടിലേക്ക് വരുമ്പോൾ എന്നെയും കൂട്ടാമോ?|husband|Could you pick me up on your way home?
message_lunch_question|മീര|നീ ഉച്ചഭക്ഷണം കൊണ്ടുവന്നോ?|Meera|Did you bring your lunch?
message_study_request|രാഹുൽ|ഇന്നത്തെ ക്ലാസിന്റെ നോട്ട്സ് ഒന്ന് തരാമോ?|Rahul|Could you share today's class notes?
message_grocery_choice|അമ്മൂമ്മ|കടയിൽ നിന്ന് എന്തെങ്കിലും വേണോ?|grandma|Do you need anything from the shop?
message_visit_request|അമ്മാവൻ|ശനിയാഴ്ച വീട്ടിലേക്ക് വരാമോ?|uncle|Could you come home on Saturday?
message_key_request|അപ്പൂപ്പൻ|താക്കോൽ മേശപ്പുറത്ത് വെക്കണേ.|grandpa|Please leave the key on the table.
message_charger_request|നേഹ|നിന്റെ ചാർജർ കുറച്ചു നേരം തരാമോ?|Neha|Could I use your charger for a while?
message_photo_request|അഞ്ജലി|ഇന്നലെ എടുത്ത നമ്മുടെ ഫോട്ടോ അയക്കാമോ?|Anjali|Could you send me the photo we took yesterday?
message_purchase_request|ചേച്ചി|വരുമ്പോൾ രണ്ട് ബ്രെഡ് വാങ്ങണേ.|Chechi|Please buy two loaves of bread on your way.
message_document_request|കിരൺ|യോഗത്തിന്റെ ലിങ്ക് ഒന്നുകൂടി അയക്കാമോ?|Kiran|Could you send the meeting link again?
message_home_request|അച്ഛൻ|ചെടികൾക്ക് വെള്ളം ഒഴിക്കാമോ?|Achan|Could you water the plants?
message_borrow_request|ദിയ|നിന്റെ നീല പേന ഒന്ന് തരാമോ?|Diya|Could I borrow your blue pen?
message_visit_request|നന്ദു|വൈകുന്നേരം ഒന്ന് കാണാമോ?|Nandu|Can we meet this evening?
message_pickup_request|അമൽ|റെയിൽവേ സ്റ്റേഷനിൽ എന്നെ കൂട്ടാൻ വരാമോ?|Amal|Can you pick me up from the railway station?
message_study_request|ശ്രുതി|നാളെ ഒരുമിച്ച് പഠിക്കാമോ?|Shruthi|Shall we study together tomorrow?
message_food_request|അമ്മ|ചായയിൽ എനിക്ക് പഞ്ചസാര വേണ്ട.|Amma|Please don't add sugar to my tea.
message_key_request|വിവേക്|എന്റെ ഹെൽമെറ്റ് അവിടെ ഉണ്ടെങ്കിൽ എടുത്തുവെക്കണേ.|Vivek|If my helmet is there, please keep it safe.
message_grocery_choice|അനിയത്തി|നിനക്ക് ഏത് പഴമാണ് വേണ്ടത്?|my sister|Which fruit would you like?
''',
19: '''
message_goodnight|അമ്മ|ശുഭരാത്രി അമ്മേ, സുഖമായി ഉറങ്ങണേ.|mom|Good night, Mom. Sleep well.
message_goodmorning|അച്ഛൻ|സുപ്രഭാതം അച്ഛാ, ഇന്നൊരു നല്ല ദിവസമാകട്ടെ.|dad|Good morning, Dad. Have a lovely day.
message_thanks|ചേച്ചി|ഇന്ന് സഹായിച്ചതിന് ഒരുപാട് നന്ദി ചേച്ചീ.|Chechi|Thanks so much for helping me today, Chechi.
message_apology|ചേട്ടൻ|നേരത്തെ ദേഷ്യപ്പെട്ടതിന് ക്ഷമിക്കണം.|Chettan|I'm sorry I got angry earlier.
message_affection|ഭാര്യ|നിന്നെ ഒരുപാട് മിസ് ചെയ്യുന്നു.|wife|I miss you a lot.
message_affection|ഭർത്താവ്|നിന്റെ കൂടെ സംസാരിക്കുമ്പോൾ എനിക്ക് സന്തോഷമാണ്.|husband|Talking with you makes me happy.
message_encouragement|അനിയത്തി|പരീക്ഷ നന്നായി എഴുതണേ, നിനക്ക് കഴിയും.|my sister|Do your best in the exam. You've got this.
message_encouragement|അനിയൻ|ഇന്ന് നിന്റെ കളി നന്നായിരുന്നു, അഭിമാനം തോന്നി.|my brother|You played really well today. I'm proud of you.
message_checkin|അമ്മൂമ്മ|അമ്മൂമ്മേ, ഇന്ന് എങ്ങനെയുണ്ട്?|grandma|Grandma, how are you feeling today?
message_checkin|അപ്പൂപ്പൻ|അപ്പൂപ്പാ, രാത്രി നന്നായി ഉറങ്ങിയോ?|grandpa|Grandpa, did you sleep well last night?
message_thanks|അനു|എന്റെ കൂടെ വന്നതിന് നന്ദി, നല്ല ആശ്വാസമായി.|Anu|Thanks for coming with me. It really helped.
message_apology|രാഹുൽ|നിന്റെ വിളി എടുക്കാൻ പറ്റിയില്ല, ക്ഷമിക്കണം.|Rahul|Sorry I couldn't answer your call.
message_support|മീര|സംസാരിക്കാൻ തോന്നിയാൽ എന്നെ വിളിക്കണേ.|Meera|Call me whenever you feel like talking.
message_congratulations|നേഹ|നിന്റെ പുതിയ ജോലിക്ക് ആശംസകൾ!|Neha|Congratulations on your new job!
message_congratulations|കിരൺ|നീ പരീക്ഷ പാസായതിൽ ഒരുപാട് സന്തോഷം.|Kiran|I'm so happy you passed your exam.
message_goodnight|ദിയ|ഇനി നാളെ സംസാരിക്കാം, സുഖമായി ഉറങ്ങണേ.|Diya|Let's talk tomorrow. Sleep well.
message_goodmorning|നന്ദു|ഇന്ന് നിനക്ക് നല്ലൊരു ദിവസമാകട്ടെ.|Nandu|Hope you have a good day today.
message_support|ശ്രുതി|വിഷമിക്കേണ്ട, ഞാൻ നിന്റെ കൂടെയുണ്ട്.|Shruthi|Don't worry, I'm here for you.
message_affection|അമ്മ|അമ്മേ, അമ്മയെ കാണാൻ കൊതിയാകുന്നു.|Amma|Amma, I can't wait to see you.
message_thanks|അമൽ|പുസ്തകം കൊണ്ടുവന്നതിന് നന്ദി.|Amal|Thanks for bringing the book.
message_apology|അഞ്ജലി|നിന്നെ കാത്തിരിപ്പിച്ചതിന് ക്ഷമിക്കണം.|Anjali|I'm sorry I kept you waiting.
message_encouragement|വിവേക്|ഇന്റർവ്യൂവിന് എല്ലാ ആശംസകളും, നന്നായി ചെയ്യണേ.|Vivek|All the best for your interview. Do well.
message_checkin|ചേച്ചി|ചേച്ചീ, ഇന്ന് വിശ്രമിക്കാൻ സമയം കിട്ടിയോ?|Chechi|Chechi, did you get some time to rest today?
message_congratulations|അച്ഛൻ|അച്ഛന്റെ ചിത്രത്തിന് സമ്മാനം കിട്ടിയതിൽ സന്തോഷം.|Achan|I'm so happy your painting won a prize.
message_support|അമ്മാവൻ|എന്തെങ്കിലും സഹായം വേണമെങ്കിൽ എന്നോട് പറയണേ.|uncle|Please tell me if you need any help.
''',
21: '''
message_schedule_change|അനു|ഇന്നത്തെ കൂടിക്കാഴ്ച നാളത്തേക്ക് മാറ്റാമോ?|Anu|Could we move today's meeting to tomorrow?
message_rsvp_accept|രാഹുൽ|ഞാൻ ഞായറാഴ്ചയുടെ പരിപാടിക്ക് വരാം.|Rahul|I'll come to the programme on Sunday.
message_rsvp_decline|മീര|ക്ഷമിക്കണം, ഇന്ന് വരാൻ പറ്റില്ല.|Meera|Sorry, I won't be able to come today.
message_work_update|കിരൺ|റിപ്പോർട്ട് തയ്യാറായി, ഒന്ന് നോക്കാമോ?|Kiran|The report is ready; could you review it?
message_meeting_point|നേഹ|നമുക്ക് ലൈബ്രറിയുടെ മുന്നിൽ കാണാം.|Neha|Let's meet outside the library.
message_time_question|അമൽ|പരിശീലനം എത്ര മണിക്കാണ് തുടങ്ങുന്നത്?|Amal|What time does practice start?
message_event_update|അഞ്ജലി|പരിപാടി രണ്ടാം നിലയിലെ ഹാളിലാണ്.|Anjali|The programme is in the hall on the second floor.
message_item_return|ശ്രുതി|നിന്റെ പുസ്തകം നാളെ തിരിച്ചുതരാം.|Shruthi|I'll return your book tomorrow.
message_task_progress|വിവേക്|എന്റെ ഭാഗത്തെ ജോലി കഴിഞ്ഞു.|Vivek|I've finished my part of the work.
message_task_request|നന്ദു|യോഗത്തിൽ പറഞ്ഞ കാര്യങ്ങൾ ഒന്ന് അയക്കാമോ?|Nandu|Could you send me the points from the meeting?
message_schedule_question|ദിയ|നാളെ രാവിലെ നിനക്ക് ഒഴിവുണ്ടോ?|Diya|Are you free tomorrow morning?
message_reservation_update|ചേട്ടൻ|ടിക്കറ്റുകൾ കിട്ടിയില്ല, വേറെ ദിവസം നോക്കാം.|Chettan|Tickets weren't available; let's try another day.
message_rsvp_question|ചേച്ചി|ഞായറാഴ്ച നീയും വരുന്നുണ്ടോ?|Chechi|Are you coming along on Sunday?
message_correction|അമ്മ|ഞാൻ പറഞ്ഞത് ചൊവ്വാഴ്ചയാണ്, തിങ്കളാഴ്ചയല്ല.|mom|I meant Tuesday, not Monday.
message_document_request|അച്ഛൻ|അപേക്ഷയുടെ പകർപ്പ് ഒന്ന് അയച്ചുതരാമോ?|dad|Could you send me a copy of the application?
message_schedule_change|അനു|നമുക്ക് വൈകുന്നേരത്തിന് പകരം രാവിലെ കാണാം.|Anu|Let's meet in the morning instead of the evening.
message_meeting_point|രാഹുൽ|ഞാൻ ടിക്കറ്റ് കൗണ്ടറിന് അടുത്ത് നിൽക്കാം.|Rahul|I'll wait near the ticket counter.
message_task_progress|മീര|പ്രസന്റേഷന്റെ ആദ്യ ഭാഗം തീർന്നു.|Meera|The first part of the presentation is done.
message_item_return|അമൽ|നിന്റെ ബാഗ് എന്റെ കയ്യിലുണ്ട്, കൊണ്ടുവരാം.|Amal|I have your bag; I'll bring it along.
message_time_question|കിരൺ|നാളെ എപ്പോഴാണ് ഇറങ്ങേണ്ടത്?|Kiran|What time should we leave tomorrow?
message_rsvp_decline|നേഹ|ഇന്ന് പറ്റില്ല, അടുത്ത തവണ കൂടാം.|Neha|I can't make it today; let's meet next time.
message_event_update|ദിയ|ക്ലാസ് ഇന്ന് ഓൺലൈനിലാണ്.|Diya|Today's class is online.
message_task_request|അഞ്ജലി|നമുക്ക് വേണ്ട സാധനങ്ങളുടെ ലിസ്റ്റ് അയക്കണേ.|Anjali|Please send the list of things we need.
message_correction|ചേച്ചി|ഞാൻ നാലരയ്ക്ക് എത്തും, നാലിനല്ല.|Chechi|I'll arrive at half past four, not four.
message_schedule_question|ശ്രുതി|ഈ ആഴ്ച ഒരുമിച്ച് നടക്കാൻ പോകാമോ?|Shruthi|Shall we go for a walk together this week?
'''
}

DATIVE = {"അമ്മ":"അമ്മയ്ക്ക്", "അച്ഛൻ":"അച്ഛന്", "ചേച്ചി":"ചേച്ചിക്ക്", "ചേട്ടൻ":"ചേട്ടന്", "അനിയൻ":"അനിയന്", "അനിയത്തി":"അനിയത്തിക്ക്", "അമ്മാവൻ":"അമ്മാവന്", "ഭാര്യ":"ഭാര്യയ്ക്ക്", "ഭർത്താവ്":"ഭർത്താവിന്", "അമ്മൂമ്മ":"അമ്മൂമ്മയ്ക്ക്", "അപ്പൂപ്പൻ":"അപ്പൂപ്പന്", "അനു":"അനുവിന്", "രാഹുൽ":"രാഹുലിന്", "നേഹ":"നേഹയ്ക്ക്", "മീര":"മീരയ്ക്ക്", "അമൽ":"അമലിന്", "ദിയ":"ദിയയ്ക്ക്", "കിരൺ":"കിരണിന്", "നന്ദു":"നന്ദുവിന്", "അഞ്ജലി":"അഞ്ജലിക്ക്", "ശ്രുതി":"ശ്രുതിക്ക്", "വിവേക്":"വിവേകിന്"}
ML_MSG_TEMPLATES = [
    '{to} "{body}" എന്ന് {via} അയക്കൂ.',
    'മിലോ, {to} {via} തയ്യാറാക്കൂ: "{body}"',
    '"{body}" — ഇത് {to} {via} അയക്കണേ.',
    '{to} {via} ഇങ്ങനെ എഴുതാമോ: "{body}"',
    'മോളേ, {to} "{body}" എന്ന് {via} കൊടുക്കാമോ?',
    '{to} അയക്കാനുള്ള {via}: "{body}"',
    'ഈ വാക്കുകൾ മാറ്റാതെ {to} {via} ഇടൂ: "{body}"',
    '{to} "{body}" എന്ന് {via} പറയണേ.',
]
EN_MSG_TEMPLATES = [
    'Send {contact} this {via}: "{body}"',
    'Milo, prepare a {via} to {contact} saying "{body}"',
    'Tell {contact} "{body}" by {via}.',
    'Could you write "{body}" in a {via} to {contact}?',
    'A {via} for {contact}, please: "{body}"',
    'Put this in a {via} to {contact}: "{body}"',
    'Please message {contact} through {via}: "{body}"',
]
MIX_MSG_TEMPLATES = [
    '{contact}-യ്ക്ക് {via} message ഇടൂ: "{body}"',
    'മിലോ, {contact}-ന് "{body}" എന്ന് {via} അയക്കൂ.',
    '"{body}" എന്ന് {contact}-ന് {via} message തയ്യാറാക്കണേ.',
    '{contact}-യ്ക്ക് ഈ text {via}-ൽ അയക്കാമോ: "{body}"',
    '{via} തുറന്ന് {contact}-ന് ഇങ്ങനെ എഴുതൂ: "{body}"',
]

def build_messages():
    for batch, block in MESSAGE_DATA.items():
        data = [line.split('|') for line in block.strip().splitlines()]
        assert len(data) == 25
        for slot, (family, contact, body, other_contact, other_body) in enumerate(data):
            app = "sms" if (slot + batch) % 5 == 0 else "whatsapp"
            ml_via = "എസ്എംഎസ്" if app == "sms" else "വാട്സ്ആപ്പ് മെസേജ്"
            en_via = "SMS" if app == "sms" else "WhatsApp"
            u1 = ML_MSG_TEMPLATES[(slot + batch) % len(ML_MSG_TEMPLATES)].format(to=DATIVE[contact], body=body, via=ml_via)
            template = EN_MSG_TEMPLATES[(slot + batch) % len(EN_MSG_TEMPLATES)] if slot < 15 else MIX_MSG_TEMPLATES[slot % len(MIX_MSG_TEMPLATES)]
            u2 = template.format(contact=other_contact, body=other_body, via=en_via)
            pair(batch, slot, (u1, {"contact": contact, "message": body, "app": app}), (u2, {"contact": other_contact, "message": other_body, "app": app}), "send_message", family, "messages_exact")

# Composition requests differ from quoting: the warm output is drafted for the user.
WISHES = '''
compose_birthday|അമ്മ|അമ്മയ്ക്ക് സ്നേഹത്തോടെ ഒരു പിറന്നാൾ ആശംസ അയക്കൂ.|അമ്മേ, പിറന്നാൾ ആശംസകൾ! ഒരുപാട് സ്നേഹം ❤️|mom|Wish mom a happy birthday in a loving message.|Happy birthday, Mom! Lots of love ❤️
compose_birthday|അച്ഛൻ|അച്ഛന് ഒരു നല്ല ബർത്ത്ഡേ വിഷ് മെസേജ് തയ്യാറാക്കൂ.|അച്ഛാ, പിറന്നാൾ ആശംസകൾ! സന്തോഷമുള്ള ദിവസമാകട്ടെ ❤️|dad|Write dad a warm birthday message.|Happy birthday, Dad! Hope you have a lovely day ❤️
compose_onam|ചേച്ചി|ചേച്ചിക്ക് ഓണാശംസകൾ മെസേജ് അയക്കണേ.|ചേച്ചീ, ഹൃദയം നിറഞ്ഞ ഓണാശംസകൾ! ഒരുപാട് സ്നേഹം ❤️|Chechi|Send Chechi a cheerful Onam wish.|Happy Onam, Chechi! Wishing you a lovely celebration ❤️
compose_vishu|അമ്മൂമ്മ|അമ്മൂമ്മയ്ക്ക് ഒരു വിഷു ആശംസ അയക്കാമോ?|അമ്മൂമ്മേ, വിഷു ആശംസകൾ! ഒരുപാട് സ്നേഹവും ഉമ്മയും ❤️|grandma|Prepare a loving Vishu wish for grandma.|Happy Vishu, Grandma! Sending you lots of love ❤️
compose_christmas|മീര|മീരയ്ക്ക് ക്രിസ്മസ് ആശംസകൾ എഴുതൂ.|മീരേ, ക്രിസ്മസ് ആശംസകൾ! സന്തോഷവും സ്നേഹവും നിറഞ്ഞ ദിവസമാകട്ടെ 🎄|Meera|Wish Meera a happy Christmas over WhatsApp.|Merry Christmas, Meera! Wishing you a day full of joy 🎄
compose_eid|അനു|അനുവിന് പെരുന്നാൾ ആശംസകൾ അയക്കൂ.|അനു, പെരുന്നാൾ ആശംസകൾ! നല്ലൊരു ദിവസമാകട്ടെ ❤️|Anu|Draft a warm Eid greeting to Anu.|Eid Mubarak, Anu! Wishing you a lovely day ❤️
compose_newyear|രാഹുൽ|രാഹുലിന് പുതുവത്സരാശംസകൾ മെസേജ് ഇടൂ.|രാഹുൽ, പുതുവത്സരാശംസകൾ! നല്ല കാര്യങ്ങൾ നിറഞ്ഞ വർഷമാകട്ടെ 🎉|Rahul|Write a New Year greeting for Rahul.|Happy New Year, Rahul! Wishing you a wonderful year ahead 🎉
compose_congratulations|കിരൺ|പുതിയ ജോലി കിട്ടിയ കിരണിന് അഭിനന്ദനം അയക്കൂ.|കിരൺ, പുതിയ ജോലിക്ക് അഭിനന്ദനങ്ങൾ! ഒരുപാട് സന്തോഷം 🎉|Kiran|Congratulate Kiran on getting a new job.|Congratulations on your new job, Kiran! So happy for you 🎉
compose_exam_wish|അനിയത്തി|അനിയത്തിക്ക് പരീക്ഷയ്ക്ക് ആശംസകൾ അയക്കണേ.|പരീക്ഷയ്ക്ക് എല്ലാ ആശംസകളും! നിനക്ക് നന്നായി ചെയ്യാൻ കഴിയും ❤️|my sister|Send my sister a kind message before her exam.|All the best for your exam! You've got this ❤️
compose_apology|ചേട്ടൻ|വൈകിയതിന് ചേട്ടനോട് ക്ഷമ ചോദിച്ച് മെസേജ് എഴുതൂ.|ചേട്ടാ, വൈകിയതിന് ക്ഷമിക്കണം. കാത്തിരിപ്പിച്ചതിൽ വിഷമമുണ്ട്.|Chettan|Write Chettan an apology for being late.|Sorry I'm late, Chettan. I'm sorry I kept you waiting.
compose_thankyou|നേഹ|സഹായിച്ചതിന് നേഹയ്ക്ക് നന്ദി മെസേജ് അയക്കൂ.|നേഹേ, സഹായിച്ചതിന് ഒരുപാട് നന്ദി! വലിയ ആശ്വാസമായി ❤️|Neha|Thank Neha warmly for helping me.|Thanks so much for your help, Neha! I really appreciate it ❤️
compose_dinner_invite|അമൽ|ഇന്ന് രാത്രി ഭക്ഷണത്തിന് അമലിനെ വീട്ടിലേക്ക് ക്ഷണിക്കൂ, മെസേജായി.|അമൽ, ഇന്ന് രാത്രി വീട്ടിൽ ഭക്ഷണം കഴിക്കാൻ വരാമോ?|Amal|Message Amal inviting him home for dinner tonight.|Amal, would you like to come home for dinner tonight?
compose_getwell|അപ്പൂപ്പൻ|അപ്പൂപ്പന് വേഗം സുഖമാകട്ടെ എന്ന് മെസേജ് അയക്കൂ.|അപ്പൂപ്പാ, വേഗം സുഖമാകട്ടെ. ഒരുപാട് സ്നേഹം ❤️|grandpa|Write grandpa a caring get-well message.|Get well soon, Grandpa. Sending you lots of love ❤️
compose_anniversary|ഭാര്യ|ഭാര്യയ്ക്ക് വിവാഹവാർഷിക ആശംസകൾ എഴുതൂ.|വിവാഹവാർഷിക ആശംസകൾ! നിന്റെ കൂടെയുള്ള ഓരോ ദിവസവും എനിക്ക് പ്രിയപ്പെട്ടതാണ് ❤️|wife|Send my wife a loving wedding anniversary wish.|Happy anniversary, my love! Every day with you is special ❤️
compose_goodnight|ഭർത്താവ്|ഭർത്താവിന് സ്നേഹത്തോടെ ശുഭരാത്രി മെസേജ് ഇടൂ.|ശുഭരാത്രി! സുഖമായി ഉറങ്ങണേ, ഒരുപാട് സ്നേഹം ❤️|husband|Write my husband a sweet good-night message.|Good night, my love. Sleep well ❤️
compose_birthday|ദിയ|ദിയയ്ക്ക് ഒരു ചെറു ബർത്ത്ഡേ വിഷ് അയക്കൂ.|ദിയ, പിറന്നാൾ ആശംസകൾ! നല്ലൊരു ദിവസമാകട്ടെ 🎂|Diya|Diya-യ്ക്ക് Malayalam-ൽ birthday wish അയക്കൂ.|ദിയ, പിറന്നാൾ ആശംസകൾ! നല്ലൊരു ദിവസമാകട്ടെ 🎂
compose_onam|അച്ഛൻ|അച്ഛന് സ്നേഹത്തോടെ ഓണാശംസകൾ അയക്കാമോ?|അച്ഛാ, ഓണാശംസകൾ! ഒരുപാട് സ്നേഹം ❤️|Achan|Achan-ന് Malayalam Onam wish തയ്യാറാക്കൂ.|അച്ഛാ, ഓണാശംസകൾ! ഒരുപാട് സ്നേഹം ❤️
compose_vishu|ചേട്ടൻ|ചേട്ടന് വിഷു ആശംസകൾ മെസേജ് എഴുതണേ.|ചേട്ടാ, വിഷു ആശംസകൾ! സന്തോഷമുള്ള ദിവസമാകട്ടെ ❤️|Chettan|Chettan-ന് English-ൽ Vishu wish അയക്കൂ.|Happy Vishu, Chettan! Hope you have a joyful day ❤️
compose_christmas|ശ്രുതി|ശ്രുതിക്ക് സ്നേഹത്തോടെ ക്രിസ്മസ് ആശംസ അയക്കൂ.|ശ്രുതീ, ക്രിസ്മസ് ആശംസകൾ! സ്നേഹവും സന്തോഷവും നിറയട്ടെ 🎄|Shruthi|Shruthi-യ്ക്ക് English Christmas greeting എഴുതൂ.|Merry Christmas, Shruthi! Wishing you lots of love and joy 🎄
compose_eid|നന്ദു|നന്ദുവിന് ഒരു നല്ല പെരുന്നാൾ ആശംസ മെസേജ് ഇടൂ.|നന്ദു, പെരുന്നാൾ ആശംസകൾ! സന്തോഷത്തോടെ ആഘോഷിക്കണേ ❤️|Nandu|Nandu-ന് Malayalam Eid wish ഒന്ന് അയക്കൂ.|നന്ദു, പെരുന്നാൾ ആശംസകൾ! സന്തോഷത്തോടെ ആഘോഷിക്കണേ ❤️
compose_congratulations|അഞ്ജലി|പരീക്ഷ പാസായ അഞ്ജലിക്ക് അഭിനന്ദനങ്ങൾ അയക്കൂ.|അഞ്ജലീ, പരീക്ഷ പാസായതിന് അഭിനന്ദനങ്ങൾ! ഒരുപാട് സന്തോഷം 🎉|Anjali|Anjali exam pass ആയി, English congratulations message അയക്കൂ.|Congratulations on passing your exam, Anjali! So happy for you 🎉
compose_apology|അമ്മ|മറുപടി വൈകിയതിന് അമ്മയോട് ക്ഷമ ചോദിക്കുന്ന മെസേജ് തയ്യാറാക്കൂ.|അമ്മേ, മറുപടി വൈകിയതിന് ക്ഷമിക്കണേ. ഒരുപാട് സ്നേഹം ❤️|Amma|Amma-യ്ക്ക് reply വൈകിയതിന് Malayalam sorry message ഇടൂ.|അമ്മേ, മറുപടി വൈകിയതിന് ക്ഷമിക്കണേ. ഒരുപാട് സ്നേഹം ❤️
compose_thankyou|വിവേക്|കൂടെ വന്നതിന് വിവേകിന് നന്ദി പറഞ്ഞ് മെസേജ് അയക്കൂ.|വിവേക്, കൂടെ വന്നതിന് നന്ദി! നല്ല ആശ്വാസമായി.|Vivek|Vivek-ന് കൂടെ വന്നതിന് English thank-you text തയ്യാറാക്കൂ.|Thanks for coming along, Vivek! It really helped.
compose_invitation|അമ്മാവൻ|ഞായറാഴ്ച വീട്ടിൽ വരാൻ അമ്മാവനെ മെസേജിൽ ക്ഷണിക്കൂ.|അമ്മാവാ, ഞായറാഴ്ച വീട്ടിലേക്ക് വരാമോ? കാണാൻ സന്തോഷമാകും.|uncle|uncle-നെ Sunday വീട്ടിലേക്ക് invite ചെയ്ത് English message അയക്കൂ.|Would you like to come home on Sunday? It would be lovely to see you.
compose_affection|അമ്മൂമ്മ|അമ്മൂമ്മയെ ഒരുപാട് സ്നേഹിക്കുന്നു എന്ന് ഒരു മെസേജ് എഴുതൂ.|അമ്മൂമ്മേ, ഒരുപാട് സ്നേഹിക്കുന്നു. കാണാൻ കൊതിയാകുന്നു ❤️|grandma|grandma-യ്ക്ക് I love you എന്ന് warm English message എഴുതൂ.|I love you so much, Grandma. Can't wait to see you ❤️
'''


def build_wishes():
    data = [line.split('|') for line in WISHES.strip().splitlines()]
    assert len(data) == 25
    for i, (family, c1, u1, m1, c2, u2, m2) in enumerate(data):
        pair(20, i, (u1, {"contact": c1, "message": m1}), (u2, {"contact": c2, "message": m2}), "send_message", family, "messages_composed")

# kind: default phone, explicit phone, WhatsApp voice/video, current-location draft.
CALL_DATA = {
22: '''
relative|p|അമ്മ|അമ്മയെ ഒന്ന് വിളിക്കൂ.|mom|Call mom for me, please.
relative|p|അച്ഛൻ|മിലോ, അച്ഛനെ വിളിക്കാമോ?|dad|I'd like to ring dad now.
relative|p|ചേച്ചി|ചേച്ചിയോട് സംസാരിക്കണം, വിളിക്കൂ.|Chechi|Open a call to Chechi.
relative|p|ചേട്ടൻ|ചേട്ടനെ ഒന്ന് ഫോൺ ചെയ്യണേ.|Chettan|Could you phone Chettan?
relative|p|അമ്മൂമ്മ|മോളേ, അമ്മൂമ്മയെ വിളിച്ചു തരാമോ?|grandma|Please help me call grandma.
named|phone|അനു|അനുവിനെ സാധാരണ ഫോൺ കോളിൽ വിളിക്കൂ.|Anu|Use a regular phone call to reach Anu.
named|p|രാഹുൽ|രാഹുലിന്റെ നമ്പറിൽ വിളിക്കാൻ തുറക്കൂ.|Rahul|Bring up Rahul's call screen.
whatsapp_voice|wa|മീര|മീരയെ വാട്സ്ആപ്പിൽ ഓഡിയോ കോൾ ചെയ്യൂ.|Meera|Start a WhatsApp voice call to Meera.
whatsapp_video|video|അപ്പൂപ്പൻ|അപ്പൂപ്പനെ വാട്സ്ആപ്പിൽ വീഡിയോ വിളിക്കാമോ?|grandpa|Open a WhatsApp video call with grandpa.
whatsapp_video|video|ഭാര്യ|ഭാര്യയെ വാട്സ്ആപ്പിൽ വീഡിയോ കോൾ ചെയ്യണം.|wife|I'd like a WhatsApp video call with my wife.
location_relative|loc|അച്ഛൻ|അച്ഛന് ഞാൻ നിൽക്കുന്ന സ്ഥലത്തിന്റെ ലൊക്കേഷൻ അയക്കൂ.|dad|Share my current location with dad.
location_named|loc|നേഹ|നേഹയ്ക്ക് എന്റെ ഇപ്പോഴത്തെ ലൊക്കേഷൻ കൊടുക്കണേ.|Neha|Prepare my current location for Neha on WhatsApp.
location_relative|loc|അമ്മാവൻ|അമ്മാവന് ഇവിടത്തെ ലൊക്കേഷൻ മെസേജിൽ ഇടൂ.|uncle|Send uncle the location where I am now.
location_named|loc|കിരൺ|കിരണിന് ഞാൻ ഇപ്പോൾ ഉള്ള സ്ഥലത്തിന്റെ പിൻ അയക്കൂ.|Kiran|Share a pin for my current location with Kiran.
named|p|ദിയ|ദിയയെ ഒന്ന് വിളിച്ചു തരൂ മിലോ.|Diya|Let me call Diya, please.
relative|p|അനിയൻ|അനിയനെ വിളിക്കാനുള്ള സ്ക്രീൻ തുറക്കൂ.|my brother|my brother-നെ call ചെയ്യാനുള്ള screen തുറക്കൂ.
relative|phone|ഭർത്താവ്|ഭർത്താവിനെ സാധാരണ ഫോൺ കോളിൽ വിളിക്കൂ.|husband|husband-നെ normal phone call ചെയ്യൂ.
whatsapp_voice|wa|ചേച്ചി|ചേച്ചിയെ വാട്സ്ആപ്പ് വോയ്സ് കോളിൽ കിട്ടുമോ, വിളിക്കൂ.|Chechi|Chechi-യെ WhatsApp voice call ചെയ്യാമോ?
whatsapp_video|video|അമ്മ|അമ്മയെ വാട്സ്ആപ്പ് വീഡിയോ കോളിൽ വിളിക്കണേ.|Amma|Amma-യെ WhatsApp-ൽ video call ചെയ്യണേ.
location_relative|loc|ചേട്ടൻ|ചേട്ടന് എന്റെ ഇപ്പോഴത്തെ സ്ഥലം അയച്ചു കൊടുക്കൂ.|Chettan|Chettan-ന് എന്റെ current location share ചെയ്യൂ.
location_named|loc|അമൽ|അമലിന് എന്റെ ഇപ്പോഴത്തെ ലൊക്കേഷൻ വാട്സ്ആപ്പിൽ അയക്കൂ.|Amal|Amal-ന് ഇപ്പോഴത്തെ location WhatsApp-ൽ അയക്കൂ.
named|p|നന്ദു|നന്ദുവുമായി ഫോണിൽ സംസാരിക്കാൻ വിളിക്കൂ.|Nandu|Nandu-നെ phone ചെയ്യൂ, സംസാരിക്കാനുണ്ട്.
location_named|loc|ശ്രുതി|ഞാൻ ഇപ്പോൾ നിൽക്കുന്ന ഇടം ശ്രുതിക്ക് അയക്കാമോ?|Shruthi|Shruthi-യ്ക്ക് ഞാൻ നിൽക്കുന്ന location അയക്കാമോ?
whatsapp_voice|wa|വിവേക്|വിവേകിനെ വാട്സ്ആപ്പിൽ ശബ്ദ കോളിൽ വിളിക്കൂ.|Vivek|Vivek-നെ WhatsApp audio call ചെയ്യൂ.
location_relative|loc|അനിയത്തി|അനിയത്തിക്ക് എന്റെ ഇപ്പോഴത്തെ ലൊക്കേഷൻ അയക്കണേ.|my sister|my sister-ന് എന്റെ current location അയച്ചുകൊടുക്കണേ.
''',
23: '''
relative|p|അപ്പൂപ്പൻ|അപ്പൂപ്പനോട് സംസാരിക്കട്ടെ, ഒന്ന് വിളിക്കൂ.|grandpa|Can you bring up a call to grandpa?
named|phone|അഞ്ജലി|അഞ്ജലിയെ സിം വഴിയുള്ള കോളിൽ വിളിക്കണേ.|Anjali|Call Anjali using the phone app.
relative|p|അമ്മാവൻ|അമ്മാവനെ വിളിക്കാൻ ഡയലർ തുറക്കണേ.|uncle|Open the dialler for uncle.
whatsapp_voice|wa|നേഹ|നേഹയെ വാട്സ്ആപ്പ് ഓഡിയോ കോളിൽ വിളിക്കാമോ?|Neha|Can you set up a WhatsApp audio call to Neha?
whatsapp_video|video|അനിയത്തി|അനിയത്തിയെ വാട്സ്ആപ്പിൽ വീഡിയോ കോളിൽ കാണണം, വിളിക്കൂ.|my sister|Please open a WhatsApp video call with my sister.
whatsapp_video|video|ചേട്ടൻ|ചേട്ടനെ വാട്സ്ആപ്പ് വീഡിയോ കോളിൽ വിളിച്ചു തരാമോ?|Chettan|I'd like to see Chettan on a WhatsApp video call.
whatsapp_voice|wa|ഭാര്യ|ഭാര്യയെ വാട്സ്ആപ്പിൽ വോയ്സ് കോൾ ചെയ്യണേ.|wife|Ring my wife using WhatsApp voice calling.
named|p|ശ്രുതി|ശ്രുതിയെ വിളിക്കാനുള്ള പേജ് തുറന്നുതരൂ.|Shruthi|Get the call screen ready for Shruthi.
location_relative|loc|അമ്മ|അമ്മയ്ക്ക് വാട്സ്ആപ്പിൽ എന്റെ ഇപ്പോഴത്തെ സ്ഥലം അയക്കൂ.|mom|Let mom know my current location on WhatsApp.
location_named|loc|രാഹുൽ|രാഹുലിന് ഈ നിമിഷം ഞാൻ ഉള്ള സ്ഥലം അയച്ചു കൊടുക്കൂ.|Rahul|Prepare a current-location pin for Rahul.
location_relative|loc|ഭാര്യ|ഭാര്യയ്ക്ക് എന്റെ ഇപ്പോഴത്തെ ലൊക്കേഷൻ പങ്കുവെക്കൂ.|wife|Share where I am right now with my wife.
location_named|loc|അനു|അനുവിന് ഈ സ്ഥലത്തിന്റെ ലൊക്കേഷൻ അയക്കൂ, ഞാൻ ഇവിടെയാണ്.|Anu|Send Anu the location where I'm standing.
relative|phone|അനിയൻ|അനിയനെ സിം കോളിൽ വിളിക്കാൻ തുറക്കൂ.|my brother|Open a regular phone call to my brother.
named|p|അമൽ|അമലിനെ വിളിക്കാൻ സഹായിക്കാമോ?|Amal|I want to speak to Amal; open a call.
location_relative|loc|ചേച്ചി|ചേച്ചിക്ക് എന്റെ ഇപ്പോഴത്തെ ലൊക്കേഷൻ പിൻ അയക്കാമോ?|Chechi|Could you draft my current location for Chechi?
whatsapp_video|video|ദിയ|ദിയയെ വാട്സ്ആപ്പ് വീഡിയോ കോളിൽ വിളിക്കൂ.|Diya|Diya-യെ WhatsApp video-യിൽ വിളിക്കൂ.
whatsapp_voice|wa|അച്ഛൻ|അച്ഛനെ വാട്സ്ആപ്പ് ഓഡിയോ കോൾ ചെയ്യണേ.|Achan|Achan-ന് WhatsApp voice call ഇടൂ.
relative|p|അമ്മൂമ്മ|അമ്മൂമ്മയോട് ഒന്ന് സംസാരിക്കണം, ഫോൺ വിളിക്കൂ.|grandma|grandma-യെ call ചെയ്യൂ, ഒന്ന് സംസാരിക്കണം.
named|phone|കിരൺ|കിരണിനെ ഫോൺ ആപ്പിലൂടെ വിളിക്കൂ.|Kiran|Kiran-നെ Phone app വഴി വിളിക്കാമോ?
location_named|loc|മീര|മീരയ്ക്ക് ഇവിടെ ഞാൻ നിൽക്കുന്ന സ്ഥലത്തിന്റെ ലൊക്കേഷൻ അയക്കണേ.|Meera|Meera-യ്ക്ക് ഇപ്പോൾ ഞാൻ നിൽക്കുന്ന place-ന്റെ location അയക്കൂ.
location_relative|loc|ഭർത്താവ്|ഭർത്താവിന് എന്റെ ഇപ്പോഴത്തെ സ്ഥലം വാട്സ്ആപ്പിൽ കൊടുക്കൂ.|husband|husband-ന് WhatsApp-ൽ എന്റെ current location കൊടുക്കൂ.
whatsapp_video|video|നന്ദു|നന്ദുവിനെ വാട്സ്ആപ്പിൽ വീഡിയോ കോൾ ചെയ്യാമോ?|Nandu|Nandu-നെ video call ചെയ്യൂ WhatsApp-ൽ.
location_named|loc|വിവേക്|വിവേകിന് എന്റെ നിലവിലെ സ്ഥലം ഒരു ലൊക്കേഷൻ മെസേജായി അയക്കൂ.|Vivek|Vivek-ന് current-location message തയ്യാറാക്കൂ.
relative|p|അനിയത്തി|അനിയത്തിയോട് പറയാനുണ്ട്, അവളെ വിളിക്കണേ.|my sister|my sister-നെ ഒന്ന് call ചെയ്യണേ.
location_named|loc|അഞ്ജലി|അഞ്ജലിക്ക് ഞാൻ നിൽക്കുന്ന സ്ഥലത്തിന്റെ മാപ്പ് പിൻ അയക്കൂ.|Anjali|Anjali-യ്ക്ക് ഞാൻ നിൽക്കുന്ന സ്ഥലത്തിന്റെ Maps pin share ചെയ്യൂ.
'''
}

def build_calls():
    for batch, block in CALL_DATA.items():
        data = [line.split('|') for line in block.strip().splitlines()]
        assert len(data) == 25
        for i, (family, kind, c1, u1, c2, u2) in enumerate(data):
            a1, a2 = {"contact": c1}, {"contact": c2}
            name = "share_location" if kind == "loc" else "call_contact"
            if kind in ("phone", "wa", "video"):
                a1["app"] = a2["app"] = "phone" if kind == "phone" else "whatsapp"
            if kind == "video":
                a1["video"] = a2["video"] = True
            pair(batch, i, (u1, a1), (u2, a2), name, "calls_" + family, "location" if kind == "loc" else "calls")

# Emails are drafts. Text includes only facts asked for, with no invented names,
# signatures, attachments, account details, or meeting dates.
# family | to ML | user ML | subject ML | body ML | to other | user other | subject other | body other
EMAIL_DATA = {
24: '''
email_leave|മാനേജർ|നാളെ അവധിയാണെന്ന് മാനേജർക്ക് ഇമെയിൽ എഴുതൂ.|നാളത്തെ അവധി|നമസ്കാരം, ഞാൻ നാളെ അവധിയിലായിരിക്കും. നന്ദി.|manager|Email my manager saying I'll be on leave tomorrow.|Leave tomorrow|Hi, I will be on leave tomorrow. Thank you.
email_sick_leave|ടീം ലീഡർ|ഇന്ന് അസുഖമായതിനാൽ അവധി വേണമെന്ന് ടീം ലീഡർക്ക് ഇമെയിൽ ഇടൂ.|ഇന്നത്തെ അവധി അപേക്ഷ|നമസ്കാരം, ഇന്ന് അസുഖമായതിനാൽ അവധി അനുവദിക്കണമെന്ന് അപേക്ഷിക്കുന്നു. നന്ദി.|team lead|Write my team lead an email requesting sick leave today.|Sick leave request|Hi, I am unwell today and would like to request sick leave. Thank you.
email_late_arrival|മാനേജർ|ട്രെയിൻ വൈകിയതിനാൽ ഓഫീസിൽ എത്താൻ വൈകുമെന്ന് മാനേജർക്ക് മെയിൽ അയക്കൂ.|ഓഫീസിൽ എത്താൻ വൈകും|നമസ്കാരം, ട്രെയിൻ വൈകിയതിനാൽ ഇന്ന് ഓഫീസിൽ എത്താൻ വൈകും. അസൗകര്യത്തിന് ക്ഷമിക്കണം.|manager|Email my manager that the delayed train will make me late to work today.|Late arrival today|Hi, my train is delayed, so I will be late to work today. Sorry for the inconvenience.
email_meeting_reschedule|അനു|ഇന്നത്തെ യോഗം നാളത്തേക്ക് മാറ്റാമോ എന്ന് അനുവിന് ഇമെയിൽ എഴുതൂ.|യോഗം മാറ്റാനുള്ള അഭ്യർത്ഥന|അനു, ഇന്നത്തെ യോഗം നാളത്തേക്ക് മാറ്റാമോ? സൗകര്യം അറിയിക്കണേ.|Anu|Email Anu asking whether today's meeting can be moved to tomorrow.|Meeting reschedule request|Hi Anu, could we move today's meeting to tomorrow? Please let me know if that works for you.
email_document_request|എച്ച്ആർ|എന്റെ പ്രവൃത്തിപരിചയ സർട്ടിഫിക്കറ്റ് വേണമെന്ന് എച്ച്ആറിന് ഇമെയിൽ തയ്യാറാക്കൂ.|പ്രവൃത്തിപരിചയ സർട്ടിഫിക്കറ്റ് അപേക്ഷ|നമസ്കാരം, എന്റെ പ്രവൃത്തിപരിചയ സർട്ടിഫിക്കറ്റ് നൽകണമെന്ന് അഭ്യർത്ഥിക്കുന്നു. നന്ദി.|HR|Draft an email to HR requesting my experience certificate.|Experience certificate request|Hello, could you please provide my experience certificate? Thank you.
email_work_status|കിരൺ|റിപ്പോർട്ട് തയ്യാറായെന്ന് കിരണിന് മെയിൽ എഴുതൂ.|റിപ്പോർട്ട് തയ്യാറായി|കിരൺ, റിപ്പോർട്ട് തയ്യാറായി എന്ന് അറിയിക്കുന്നു. നന്ദി.|Kiran|Email Kiran to say the report is ready.|Report ready|Hi Kiran, the report is ready. Thank you.
email_school_absence|ക്ലാസ് ടീച്ചർ|മകന് ഇന്ന് പനി ആയതിനാൽ ക്ലാസിൽ വരില്ലെന്ന് ക്ലാസ് ടീച്ചർക്ക് ഇമെയിൽ അയക്കൂ.|ഇന്നത്തെ ക്ലാസിലെ അഭാവം|നമസ്കാരം ടീച്ചർ, എന്റെ മകന് ഇന്ന് പനി ആയതിനാൽ ക്ലാസിൽ വരാൻ കഴിയില്ല. നന്ദി.|class teacher|Email the class teacher that my son has a fever and cannot attend class today.|Absent from class today|Dear Teacher, my son has a fever and cannot attend class today. Thank you.
email_school_information|സ്കൂൾ ഓഫീസ്|അഡ്മിഷന് വേണ്ട രേഖകൾ ഏതൊക്കെയെന്ന് സ്കൂൾ ഓഫീസിന് ഇമെയിൽ ചോദിക്കൂ.|അഡ്മിഷൻ രേഖകൾ|നമസ്കാരം, അഡ്മിഷന് ആവശ്യമായ രേഖകൾ ഏതൊക്കെയെന്ന് അറിയിക്കാമോ? നന്ദി.|school office|Email the school office asking which documents are needed for admission.|Admission documents|Hello, could you please tell me which documents are needed for admission? Thank you.
email_fee_receipt|സ്കൂൾ ഓഫീസ്|ഫീസ് അടച്ചതിന്റെ രസീത് വേണമെന്ന് സ്കൂൾ ഓഫീസിന് മെയിൽ തയ്യാറാക്കൂ.|ഫീസ് രസീത് അപേക്ഷ|നമസ്കാരം, അടച്ച ഫീസിന്റെ രസീത് നൽകാമോ? നന്ദി.|school office|Request a receipt for the paid fees in an email to the school office.|Fee receipt request|Hello, could you please provide a receipt for the fees paid? Thank you.
email_meeting_request|പ്രിൻസിപ്പൽ|പ്രിൻസിപ്പലിനെ കാണാൻ സമയം ചോദിച്ച് ഇമെയിൽ അയക്കൂ.|കാണാൻ സമയം ചോദിക്കുന്നു|നമസ്കാരം, നേരിൽ കാണാൻ സൗകര്യപ്രദമായ സമയം അറിയിക്കാമോ? നന്ദി.|principal|Email the principal to ask for a convenient meeting time.|Meeting request|Dear Principal, could you please let me know a convenient time to meet? Thank you.
email_thankyou|മീര|നോട്ട്സ് തന്നതിന് മീരയ്ക്ക് നന്ദി പറഞ്ഞ് ഇമെയിൽ എഴുതൂ.|നന്ദി|മീരേ, നോട്ട്സ് തന്നതിന് ഒരുപാട് നന്ദി. വലിയ സഹായമായി.|Meera|Write Meera an email thanking her for the notes.|Thank you for the notes|Hi Meera, thank you so much for sharing the notes. They really helped.
email_deadline_extension|ടീം ലീഡർ|റിപ്പോർട്ടിന് രണ്ട് ദിവസം കൂടി വേണമെന്ന് ടീം ലീഡർക്ക് ഇമെയിൽ എഴുതൂ.|റിപ്പോർട്ടിന് കൂടുതൽ സമയം|നമസ്കാരം, റിപ്പോർട്ട് പൂർത്തിയാക്കാൻ രണ്ട് ദിവസം കൂടി അനുവദിക്കാമോ? നന്ദി.|team lead|Ask my team lead by email for two extra days to finish the report.|Report extension request|Hi, could I please have two extra days to complete the report? Thank you.
email_confirmation|അനു|നാളത്തെ യോഗത്തിൽ പങ്കെടുക്കുമെന്ന് അനുവിന് ഇമെയിൽ ഇടൂ.|യോഗത്തിൽ പങ്കെടുക്കും|അനു, നാളത്തെ യോഗത്തിൽ ഞാൻ പങ്കെടുക്കും എന്ന് അറിയിക്കുന്നു. നന്ദി.|Anu|Email Anu confirming I'll attend tomorrow's meeting.|Meeting attendance confirmation|Hi Anu, I confirm that I will attend tomorrow's meeting. Thank you.
email_contact_address|office@example.com|ഓഫീസ് തുറക്കുന്ന സമയം ചോദിച്ച് office@example.com എന്ന വിലാസത്തിലേക്ക് ഇമെയിൽ എഴുതൂ.|ഓഫീസ് പ്രവർത്തന സമയം|നമസ്കാരം, ഓഫീസ് തുറക്കുന്ന സമയം അറിയിക്കാമോ? നന്ദി.|office@example.com|Email office@example.com to ask when the office opens.|Office opening hours|Hello, could you please let me know the office opening hours? Thank you.
email_subject_explicit|അനു|അനുവിന് ഇമെയിൽ എഴുതൂ, വിഷയം അവധി, ഉള്ളടക്കം നാളെ ഞാൻ അവധിയിലാണ്.|അവധി|നാളെ ഞാൻ അവധിയിലാണ്.|Anu|Email Anu with subject "Leave" and body "I am on leave tomorrow."|Leave|I am on leave tomorrow.
email_leave|manager|manager-ന് English email എഴുതൂ, Friday leave വേണമെന്ന്.|Leave request for Friday|Hi, I would like to request leave on Friday. Thank you.|manager|manager-ന് Friday leave വേണമെന്ന് English email അയക്കൂ.|Leave request for Friday|Hi, I would like to request leave on Friday. Thank you.
email_meeting_reschedule|Anu|Anu-ന് English mail എഴുതൂ, meeting Monday-ലേക്ക് മാറ്റാമോ എന്ന്.|Meeting reschedule request|Hi Anu, could we move the meeting to Monday? Thank you.|Anu|meeting Monday-ലേക്ക് മാറ്റാമോ എന്ന് Anu-ന് English email ഇടൂ.|Meeting reschedule request|Hi Anu, could we move the meeting to Monday? Thank you.
email_school_absence|class teacher|class teacher-ന് English email എഴുതൂ, മകൾ ഇന്ന് class-ൽ വരില്ലെന്ന്.|Absent from class today|Dear Teacher, my daughter will not attend class today. Thank you.|class teacher|മകൾ ഇന്ന് class-ൽ വരില്ലെന്ന് class teacher-ന് English mail തയ്യാറാക്കൂ.|Absent from class today|Dear Teacher, my daughter will not attend class today. Thank you.
email_document_request|HR|HR-ന് English mail അയക്കൂ, employment letter വേണമെന്ന്.|Employment letter request|Hello, could you please provide my employment letter? Thank you.|HR|employment letter വേണമെന്ന് HR-ന് English email എഴുതണേ.|Employment letter request|Hello, could you please provide my employment letter? Thank you.
email_work_status|Kiran|Kiran-ന് English mail എഴുതൂ, presentation ready ആയി എന്ന്.|Presentation ready|Hi Kiran, the presentation is ready. Thank you.|Kiran|presentation ready ആണെന്ന് Kiran-ന് English email അയക്കൂ.|Presentation ready|Hi Kiran, the presentation is ready. Thank you.
email_thankyou|Meera|Meera-യ്ക്ക് English thank-you mail എഴുതൂ, project-ൽ സഹായിച്ചതിന്.|Thank you for your help|Hi Meera, thank you for helping with the project. I really appreciate it.|Meera|project-ൽ help ചെയ്തതിന് Meera-യ്ക്ക് English thank-you email ഇടൂ.|Thank you for your help|Hi Meera, thank you for helping with the project. I really appreciate it.
email_information|school@example.com|school@example.com-ലേക്ക് English mail അയക്കൂ, library timing ചോദിക്കൂ.|Library opening hours|Hello, could you please share the library opening hours? Thank you.|school@example.com|library timing ചോദിച്ച് school@example.com-ലേക്ക് English email തയ്യാറാക്കൂ.|Library opening hours|Hello, could you please share the library opening hours? Thank you.
email_meeting_request|principal|principal-ന് English email എഴുതൂ, Wednesday കാണാൻ പറ്റുമോ എന്ന്.|Meeting request for Wednesday|Dear Principal, would it be possible to meet on Wednesday? Thank you.|principal|Wednesday കാണാൻ പറ്റുമോ എന്ന് principal-ന് English mail ഇടൂ.|Meeting request for Wednesday|Dear Principal, would it be possible to meet on Wednesday? Thank you.
email_deadline_extension|team lead|team lead-ന് English mail അയക്കൂ, task തീർക്കാൻ ഒരു ദിവസം കൂടി വേണം.|One-day extension request|Hi, could I please have one more day to complete the task? Thank you.|team lead|task complete ചെയ്യാൻ ഒരു ദിവസം കൂടി വേണമെന്ന് team lead-ന് English email തയ്യാറാക്കൂ.|One-day extension request|Hi, could I please have one more day to complete the task? Thank you.
email_subject_explicit|Anu|Anu-ന് email, subject "Notes", body "Please send the notes." എന്ന് എഴുതൂ.|Notes|Please send the notes.|Anu|Anu-ന് email തയ്യാറാക്കൂ: subject "Notes", body "Please send the notes."|Notes|Please send the notes.
''',
25: '''
email_bank_information|ബാങ്ക് മാനേജർ|പുതിയ അക്കൗണ്ട് തുറക്കാൻ വേണ്ട രേഖകൾ ചോദിച്ച് ബാങ്ക് മാനേജർക്ക് ഇമെയിൽ എഴുതൂ.|അക്കൗണ്ട് തുറക്കാനുള്ള രേഖകൾ|നമസ്കാരം, പുതിയ അക്കൗണ്ട് തുറക്കാൻ ആവശ്യമായ രേഖകൾ ഏതൊക്കെയാണ്? ദയവായി അറിയിക്കണേ.|bank manager|Email the bank manager asking which documents are needed to open an account.|Documents for a new account|Hello, could you please tell me which documents are needed to open an account? Thank you.
email_bank_hours|ബാങ്ക്|ശനിയാഴ്ച ബാങ്ക് തുറക്കുമോ എന്ന് ബാങ്കിന് ഇമെയിൽ അയക്കൂ.|ശനിയാഴ്ച പ്രവർത്തന സമയം|നമസ്കാരം, ശനിയാഴ്ച ബാങ്ക് തുറക്കുമോ എന്ന് അറിയിക്കാമോ? നന്ദി.|bank|Email the bank asking whether it is open on Saturday.|Saturday opening hours|Hello, could you please confirm whether the bank is open on Saturday? Thank you.
email_application_status|പഞ്ചായത്ത് ഓഫീസ്|എന്റെ അപേക്ഷയുടെ സ്ഥിതി ചോദിച്ച് പഞ്ചായത്ത് ഓഫീസിന് ഇമെയിൽ എഴുതൂ.|അപേക്ഷയുടെ സ്ഥിതി|നമസ്കാരം, എന്റെ അപേക്ഷയുടെ നിലവിലെ സ്ഥിതി അറിയിക്കാമോ? നന്ദി.|panchayat office|Draft an email to the panchayat office asking for my application status.|Application status enquiry|Hello, could you please update me on the status of my application? Thank you.
email_certificate_information|വില്ലേജ് ഓഫീസ്|താമസ സർട്ടിഫിക്കറ്റിന് വേണ്ട രേഖകൾ ചോദിച്ച് വില്ലേജ് ഓഫീസിന് ഇമെയിൽ ഇടൂ.|താമസ സർട്ടിഫിക്കറ്റ് രേഖകൾ|നമസ്കാരം, താമസ സർട്ടിഫിക്കറ്റിന് ആവശ്യമായ രേഖകൾ അറിയിക്കാമോ? നന്ദി.|village office|Email the village office asking about documents for a residence certificate.|Residence certificate documents|Hello, could you please tell me the documents required for a residence certificate? Thank you.
email_office_hours|പഞ്ചായത്ത് ഓഫീസ്|പഞ്ചായത്ത് ഓഫീസ് എത്ര മണിവരെ തുറക്കുമെന്ന് ഇമെയിൽ ചോദിക്കൂ.|ഓഫീസ് പ്രവർത്തന സമയം|നമസ്കാരം, ഓഫീസ് എത്ര മണിവരെ തുറക്കുമെന്ന് അറിയിക്കാമോ? നന്ദി.|panchayat office|Email the panchayat office to ask its closing time.|Office closing time|Hello, could you please let me know the office closing time? Thank you.
email_repair_request|വീട്ടുടമ|അടുക്കളയിലെ പൈപ്പ് ചോരുന്നു, നന്നാക്കാൻ ആവശ്യപ്പെട്ട് വീട്ടുടമയ്ക്ക് ഇമെയിൽ എഴുതൂ.|പൈപ്പ് നന്നാക്കാനുള്ള അപേക്ഷ|നമസ്കാരം, അടുക്കളയിലെ പൈപ്പ് ചോരുന്നുണ്ട്. അത് നന്നാക്കാനുള്ള സൗകര്യം ചെയ്യാമോ? നന്ദി.|landlord|Email my landlord requesting repair of the leaking kitchen pipe.|Kitchen pipe repair request|Hello, the kitchen pipe is leaking. Could you please arrange a repair? Thank you.
email_service_enquiry|support@example.com|ഇന്റർനെറ്റ് പ്രവർത്തിക്കുന്നില്ലെന്ന് support@example.com-ലേക്ക് ഇമെയിൽ അയക്കൂ.|ഇന്റർനെറ്റ് പ്രവർത്തിക്കുന്നില്ല|നമസ്കാരം, എന്റെ ഇന്റർനെറ്റ് കണക്ഷൻ പ്രവർത്തിക്കുന്നില്ല. സഹായിക്കാമോ? നന്ദി.|support@example.com|Email support@example.com saying my internet connection is not working.|Internet connection issue|Hello, my internet connection is not working. Could you please help? Thank you.
email_library_renewal|ലൈബ്രറി|പുസ്തകം പുതുക്കാൻ പറ്റുമോ എന്ന് ലൈബ്രറിക്ക് ഇമെയിൽ എഴുതൂ.|പുസ്തകം പുതുക്കാനുള്ള അപേക്ഷ|നമസ്കാരം, എടുത്ത പുസ്തകം പുതുക്കാൻ പറ്റുമോ എന്ന് അറിയിക്കാമോ? നന്ദി.|library|Email the library asking whether I can renew my borrowed book.|Book renewal enquiry|Hello, could you please let me know whether I can renew my borrowed book? Thank you.
email_farewell|മീര|ഒരുമിച്ച് ജോലി ചെയ്തതിൽ സന്തോഷമെന്ന് മീരയ്ക്ക് വിടവാങ്ങൽ ഇമെയിൽ എഴുതൂ.|ആശംസകൾ|മീരേ, ഒരുമിച്ച് ജോലി ചെയ്തതിൽ ഒരുപാട് സന്തോഷം. മുന്നോട്ടുള്ള യാത്രയ്ക്ക് എല്ലാ ആശംസകളും.|Meera|Write Meera a farewell email saying I enjoyed working with her.|Best wishes|Hi Meera, I really enjoyed working with you. Wishing you all the best for what comes next.
email_thankyou|ടീച്ചർ|നല്ല രീതിയിൽ പഠിപ്പിച്ചതിന് ടീച്ചർക്ക് നന്ദി പറഞ്ഞ് ഇമെയിൽ ഇടൂ.|നന്ദി|നമസ്കാരം ടീച്ചർ, നല്ല രീതിയിൽ പഠിപ്പിച്ചതിന് ഒരുപാട് നന്ദി. വലിയ സഹായമായി.|teacher|Email my teacher to thank them for teaching so well.|Thank you|Dear Teacher, thank you for explaining everything so well. It really helped me.
email_family_invite|അമ്മാവൻ|ഞായറാഴ്ച ഉച്ചഭക്ഷണത്തിന് ക്ഷണിച്ച് അമ്മാവന് ഇമെയിൽ എഴുതൂ.|ഞായറാഴ്ച ഉച്ചഭക്ഷണം|അമ്മാവാ, ഞായറാഴ്ച ഉച്ചഭക്ഷണത്തിന് വീട്ടിലേക്ക് വരാമോ? കാണാൻ സന്തോഷമാകും.|uncle|Email uncle inviting him home for Sunday lunch.|Sunday lunch invitation|Hi Uncle, would you like to come home for lunch on Sunday? It would be lovely to see you.
email_booking_enquiry|stay@example.com|stay@example.com-ലേക്ക് ഇമെയിൽ എഴുതൂ, ഡിസംബർ 12-ന് മുറി ഒഴിവുണ്ടോ എന്ന്.|മുറിയുടെ ലഭ്യത|നമസ്കാരം, ഡിസംബർ 12-ന് മുറി ലഭ്യമാണോ? ദയവായി അറിയിക്കണേ.|stay@example.com|Email stay@example.com asking if a room is available on 12 December.|Room availability on 12 December|Hello, is a room available on 12 December? Please let me know. Thank you.
email_job_enquiry|jobs@example.com|jobs@example.com-ലേക്ക് ജോലി ഒഴിവുകളുണ്ടോ എന്ന് ഇമെയിൽ ചോദിക്കൂ.|ജോലി ഒഴിവുകൾ|നമസ്കാരം, നിലവിൽ ജോലി ഒഴിവുകളുണ്ടോ എന്ന് അറിയിക്കാമോ? നന്ദി.|jobs@example.com|Email jobs@example.com to ask about current job openings.|Job openings enquiry|Hello, could you please let me know whether there are any current job openings? Thank you.
email_address_exact|anu@example.com|anu@example.com-ലേക്ക് വിഷയം നന്ദി എന്നും ഉള്ളടക്കം സഹായിച്ചതിന് നന്ദി. എന്നും ഇമെയിൽ തയ്യാറാക്കൂ.|നന്ദി|സഹായിച്ചതിന് നന്ദി.|anu@example.com|To anu@example.com, write an email with subject "Thanks" and body "Thank you for your help."|Thanks|Thank you for your help.
email_schedule_request|ക്ലിനിക്|ക്ലിനിക്കിൽ ഡോക്ടറെ കാണാൻ സമയം ചോദിച്ച് ഇമെയിൽ അയക്കൂ.|പരിശോധനയ്ക്ക് സമയം|നമസ്കാരം, ഡോക്ടറെ കാണാൻ ലഭ്യമായ സമയം അറിയിക്കാമോ? നന്ദി.|clinic|Email the clinic asking for available appointment times.|Appointment availability|Hello, could you please share the available appointment times? Thank you.
email_bank_information|ബാങ്ക്|അക്കൗണ്ടിന്റെ പേര് മാറ്റാൻ വേണ്ട രേഖകൾ ചോദിച്ച് ബാങ്കിന് ഇമെയിൽ അയക്കൂ.|പേര് മാറ്റാനുള്ള രേഖകൾ|നമസ്കാരം, അക്കൗണ്ടിലെ പേര് മാറ്റാൻ ആവശ്യമായ രേഖകൾ അറിയിക്കാമോ? നന്ദി.|bank|bank-ന് English email എഴുതൂ, name change-ന് വേണ്ട documents ചോദിക്കൂ.|Documents for name change|Hello, could you please tell me the documents required to change the name on my account? Thank you.
email_application_status|വില്ലേജ് ഓഫീസ്|എന്റെ സർട്ടിഫിക്കറ്റ് അപേക്ഷയുടെ സ്ഥിതി ചോദിച്ച് വില്ലേജ് ഓഫീസിന് ഇമെയിൽ തയ്യാറാക്കൂ.|സർട്ടിഫിക്കറ്റ് അപേക്ഷയുടെ സ്ഥിതി|നമസ്കാരം, എന്റെ സർട്ടിഫിക്കറ്റ് അപേക്ഷയുടെ നിലവിലെ സ്ഥിതി അറിയിക്കാമോ? നന്ദി.|village office|village office-ന് English mail ഇടൂ, certificate application status ചോദിക്കൂ.|Certificate application status|Hello, could you please update me on the status of my certificate application? Thank you.
email_office_hours|ബാങ്ക് മാനേജർ|ബാങ്ക് ഉച്ചയ്ക്ക് ഇടവേളയ്ക്ക് അടയ്ക്കുമോ എന്ന് മാനേജർക്ക് മെയിൽ ചോദിക്കൂ.|ഉച്ചയിലെ പ്രവർത്തന സമയം|നമസ്കാരം, ഉച്ചയ്ക്ക് ഇടവേളയ്ക്ക് ബാങ്ക് അടയ്ക്കുമോ എന്ന് അറിയിക്കാമോ? നന്ദി.|bank manager|bank manager-ന് English email അയക്കൂ, lunch break-ന് bank അടയ്ക്കുമോ എന്ന്.|Lunch break opening hours|Hello, could you please confirm whether the bank closes for lunch? Thank you.
email_repair_request|വീട്ടുടമ|മുറിയിലെ ഫാൻ പ്രവർത്തിക്കുന്നില്ല, നന്നാക്കാൻ വീട്ടുടമയ്ക്ക് ഇമെയിൽ അയക്കൂ.|ഫാൻ നന്നാക്കാനുള്ള അപേക്ഷ|നമസ്കാരം, മുറിയിലെ ഫാൻ പ്രവർത്തിക്കുന്നില്ല. നന്നാക്കാൻ സഹായിക്കാമോ? നന്ദി.|landlord|landlord-ന് English mail ഇടൂ, room fan repair വേണമെന്ന്.|Room fan repair request|Hello, the fan in my room is not working. Could you please arrange a repair? Thank you.
email_library_information|ലൈബ്രറി|ലൈബ്രറിയിൽ അംഗമാകാൻ എന്ത് ചെയ്യണമെന്ന് ഇമെയിലിൽ ചോദിക്കൂ.|ലൈബ്രറി അംഗത്വം|നമസ്കാരം, ലൈബ്രറിയിൽ അംഗമാകാൻ എന്ത് ചെയ്യണമെന്ന് അറിയിക്കാമോ? നന്ദി.|library|library-യ്ക്ക് English mail എഴുതൂ, membership എങ്ങനെ എടുക്കണം എന്ന്.|Library membership enquiry|Hello, could you please explain how to become a library member? Thank you.
email_service_enquiry|help@example.com|help@example.com-ലേക്ക് ഓർഡർ വൈകുന്നു എന്ന് ഇമെയിൽ എഴുതൂ.|ഓർഡർ വൈകുന്നു|നമസ്കാരം, എന്റെ ഓർഡർ വൈകുകയാണ്. നിലവിലെ സ്ഥിതി അറിയിക്കാമോ? നന്ദി.|help@example.com|help@example.com-ലേക്ക് English email തയ്യാറാക്കൂ, order delay ആയി എന്ന്.|Delayed order|Hello, my order is delayed. Could you please provide an update? Thank you.
email_thankyou|അനു|പരിപാടി ഒരുക്കിയതിന് അനുവിന് നന്ദി പറഞ്ഞ് ഇമെയിൽ അയക്കൂ.|പരിപാടിക്ക് നന്ദി|അനു, പരിപാടി ഒരുക്കിയതിന് ഒരുപാട് നന്ദി. വളരെ സന്തോഷമായി.|Anu|Anu-ന് event arrange ചെയ്തതിന് English thank-you email എഴുതൂ.|Thank you for organising the event|Hi Anu, thank you for organising the event. I really enjoyed it.
email_family_invite|ചേച്ചി|അടുത്ത ശനിയാഴ്ച വീട്ടിലേക്ക് ക്ഷണിച്ച് ചേച്ചിക്ക് ഇമെയിൽ ഇടൂ.|ശനിയാഴ്ച വീട്ടിലേക്ക് ക്ഷണം|ചേച്ചീ, അടുത്ത ശനിയാഴ്ച വീട്ടിലേക്ക് വരാമോ? കാണാൻ കൊതിയാകുന്നു.|Chechi|Chechi-യ്ക്ക് next Saturday വീട്ടിലേക്ക് invite ചെയ്ത് English mail എഴുതൂ.|Invitation for next Saturday|Hi Chechi, would you like to come home next Saturday? I'd love to see you.
email_subject_explicit|office@example.com|office@example.com-ലേക്ക് വിഷയം സമയം എന്നും ഉള്ളടക്കം നാളെ ഓഫീസ് തുറക്കുമോ? എന്നും എഴുതൂ.|സമയം|നാളെ ഓഫീസ് തുറക്കുമോ?|office@example.com|office@example.com-ലേക്ക് email, subject "Hours", body "Will the office be open tomorrow?" എന്ന് ഇടൂ.|Hours|Will the office be open tomorrow?
email_schedule_request|ഡോക്ടർ|ഡോക്ടർക്ക് അടുത്ത ചൊവ്വാഴ്ച കാണാൻ പറ്റുമോ എന്ന് ഇമെയിൽ അയക്കൂ.|ചൊവ്വാഴ്ച പരിശോധന|നമസ്കാരം ഡോക്ടർ, അടുത്ത ചൊവ്വാഴ്ച പരിശോധനയ്ക്ക് സമയം ലഭ്യമാണോ? നന്ദി.|doctor|doctor-ന് English mail അയക്കൂ, next Tuesday appointment കിട്ടുമോ എന്ന്.|Appointment next Tuesday|Dear Doctor, is an appointment available next Tuesday? Thank you.
'''
}

def build_emails():
    # Batch 24's final ten Malayalam forms are separately localized below;
    # language labels describe the user's language, not the requested output.
    ml_replacements = [
        ("മാനേജർ", "വെള്ളിയാഴ്ച അവധി വേണമെന്ന് മാനേജർക്ക് ഇംഗ്ലീഷിൽ ഇമെയിൽ എഴുതൂ."),
        ("അനു", "യോഗം തിങ്കളാഴ്ചയിലേക്ക് മാറ്റാമോ എന്ന് അനുവിന് ഇംഗ്ലീഷിൽ ഇമെയിൽ എഴുതൂ."),
        ("ക്ലാസ് ടീച്ചർ", "മകൾ ഇന്ന് ക്ലാസിൽ വരില്ലെന്ന് ക്ലാസ് ടീച്ചർക്ക് ഇംഗ്ലീഷിൽ ഇമെയിൽ അയക്കൂ."),
        ("എച്ച്ആർ", "എച്ച്ആറിന് ഇംഗ്ലീഷിൽ ഇമെയിൽ എഴുതൂ, തൊഴിൽ സ്ഥിരീകരണ കത്ത് വേണമെന്ന്."),
        ("കിരൺ", "പ്രസന്റേഷൻ തയ്യാറായെന്ന് കിരണിന് ഇംഗ്ലീഷിൽ ഇമെയിൽ എഴുതൂ."),
        ("മീര", "പ്രോജക്ടിൽ സഹായിച്ചതിന് മീരയ്ക്ക് ഇംഗ്ലീഷിൽ നന്ദി ഇമെയിൽ എഴുതൂ."),
        ("school@example.com", "ലൈബ്രറി തുറക്കുന്ന സമയം ചോദിച്ച് school@example.com-ലേക്ക് ഇംഗ്ലീഷിൽ ഇമെയിൽ എഴുതൂ."),
        ("പ്രിൻസിപ്പൽ", "ബുധനാഴ്ച കാണാൻ പറ്റുമോ എന്ന് പ്രിൻസിപ്പലിന് ഇംഗ്ലീഷിൽ ഇമെയിൽ അയക്കൂ."),
        ("ടീം ലീഡർ", "ജോലി തീർക്കാൻ ഒരു ദിവസം കൂടി വേണമെന്ന് ടീം ലീഡർക്ക് ഇംഗ്ലീഷിൽ ഇമെയിൽ എഴുതൂ."),
        ("അനു", 'അനുവിന് ഇമെയിൽ എഴുതൂ, വിഷയം "Notes", ഉള്ളടക്കം "Please send the notes." എന്ന് വേണം.'),
    ]
    for batch, block in EMAIL_DATA.items():
        data = [line.split('|') for line in block.strip().splitlines()]
        assert len(data) == 25
        for i, (family, to1, u1, s1, b1, to2, u2, s2, b2) in enumerate(data):
            if batch == 24 and i >= 15:
                to1, u1 = ml_replacements[i - 15]
            pair(batch, i, (u1, {"to": to1, "subject": s1, "body": b1}), (u2, {"to": to2, "subject": s2, "body": b2}), "send_email", family, "email")

# Contact names are fictional; absent phone numbers remain absent.
# kind | name ML | relation | input ML | name other | input other
CONTACT_DATA = '''
contact|ലത|mother|എന്റെ അമ്മയുടെ പേര് ലത എന്നാണ്, കോൺടാക്ടായി ഓർത്തുവെക്കൂ.|Latha|Save Latha as my mother.
contact|മോഹൻ|father|മോഹനാണ് എന്റെ അച്ഛൻ, കോൺടാക്ടിൽ വെക്കണേ.|Mohan|Remember Mohan as my father in my contacts.
name|അച്ചു||എന്നെ അച്ചു എന്ന് വിളിച്ചാൽ മതി, ഓർത്തുവെക്കണേ.|Achu|Please remember to call me Achu.
contact|നിത്യ|elder sister|നിത്യ എന്റെ ചേച്ചിയാണ്, ബന്ധം ഓർത്തുവെക്കൂ.|Nithya|Save Nithya as my elder sister.
contact|അരുൺ|elder brother|എന്റെ ചേട്ടൻ അരുണാണ്, കോൺടാക്ടായി ചേർക്കൂ.|Arun|Add Arun to my contacts as my elder brother.
contact|മീര|friend|മീര എന്റെ കൂട്ടുകാരിയാണ്, കോൺടാക്ട് സേവ് ചെയ്യൂ.|Meera|Save Meera as my friend.
name|കണ്ണൻ||മിലോ, ഇനി മുതൽ എന്നെ കണ്ണൻ എന്ന് വിളിക്കണേ.|Kannan|From now on, remember my name as Kannan.
contact|ദേവി|wife|ദേവി എന്റെ ഭാര്യയാണ്, കോൺടാക്ടിൽ ഓർത്തുവെക്കൂ.|Devi|Remember Devi is my wife; save the contact.
contact|അജയ്|husband|എന്റെ ഭർത്താവിന്റെ പേര് അജയ്, കോൺടാക്ട് ചേർക്കൂ.|Ajay|Add Ajay as my husband in contacts.
contact|കിരൺ|boss|എന്റെ ബോസ് കിരൺ ആണ്, കോൺടാക്ടായി സേവ് ചെയ്യണേ.|Kiran|Save Kiran with the relation boss.
name|മിന്നു||എന്റെ വിളിപ്പേര് മിന്നു, എന്നെ അങ്ങനെ വിളിക്കാൻ ഓർത്തുവെക്കൂ.|Minnu|My nickname is Minnu; remember to call me that.
contact|രാധ|grandmother|രാധയാണ് എന്റെ അമ്മൂമ്മ, കോൺടാക്ട് ഓർത്തുവെക്കൂ.|Radha|Add Radha as my grandmother.
contact|രവി|grandfather|എന്റെ അപ്പൂപ്പൻ രവി ആണ്, കോൺടാക്ടിൽ ചേർക്കണേ.|Ravi|Remember Ravi as my grandfather in contacts.
contact|അനു||അനു എന്ന പേര് പുതിയ കോൺടാക്ടായി ചേർക്കൂ.|Anu|Create a contact named Anu.
name|ദേവു||എന്നെ ദേവു എന്ന് വിളിക്കാൻ പേര് സേവ് ചെയ്യൂ.|Devu|Save Devu as the name you call me.
contact|ദിയ|younger sister|ദിയ എന്റെ അനിയത്തിയാണ്, കോൺടാക്ടിൽ സേവ് ചെയ്യൂ.|Diya|Diya എന്റെ younger sister ആണ്, contact save ചെയ്യൂ.
name|അമു||നീ എന്നെ അമു എന്ന് വിളിക്കണം, ഓർത്തുവെക്കണേ.|Amu|എന്നെ Amu എന്ന് വിളിക്കണം, name remember ചെയ്യൂ.
contact|നന്ദു|younger brother|നന്ദു എന്റെ അനിയനാണ്, കോൺടാക്ട് ചേർക്കണേ.|Nandu|Nandu എന്റെ younger brother ആണ്, contact ആയി ചേർക്കൂ.
contact|സജു|uncle|സജുവാണ് എന്റെ അമ്മാവൻ, കോൺടാക്ട് ഓർത്തുവെക്കണേ.|Saju|Saju എന്റെ uncle ആണ്, contacts-ൽ save ചെയ്യൂ.
name|കിച്ചു||എന്റെ പേരിന് പകരം കിച്ചു എന്ന് വിളിക്കണേ, ഓർത്തുവെക്കൂ.|Kichu|nickname Kichu ആയി save ചെയ്യൂ, എന്നെ അങ്ങനെ വിളിക്കണേ.
contact|താര|daughter|താര എന്റെ മകളാണ്, കോൺടാക്ടായി സേവ് ചെയ്യൂ.|Tara|Tara എന്റെ daughter ആണ്, contact save ചെയ്യണേ.
contact|അഖിൽ|son|അഖിൽ എന്റെ മകനാണ്, കോൺടാക്ടിൽ വെക്കൂ.|Akhil|Akhil എന്റെ son ആണ്, contact ആയി ഓർത്തുവെക്കൂ.
name|പാറു||മിലോ മോളേ, എന്നെ പാറു എന്ന് വിളിച്ചോളൂ, പേര് ഓർത്തുവെക്കണം.|Paru|ഇനി എന്നെ Paru എന്ന് വിളിക്കൂ, name update ചെയ്യണേ.
contact|ശ്രുതി||ശ്രുതി എന്ന് ഒരു പുതിയ കോൺടാക്ട് ഉണ്ടാക്കൂ.|Shruthi|Shruthi എന്ന പേരിൽ പുതിയ contact create ചെയ്യൂ.
name|നീലു||എന്നെ വിളിക്കാനുള്ള പേര് നീലു ആക്കൂ.|Neelu|എന്നെ വിളിക്കുന്ന name Neelu ആക്കണേ.
'''

def build_contacts():
    data = [line.split('|') for line in CONTACT_DATA.strip().splitlines()]
    assert len(data) == 25
    for i, (kind, n1, relation, u1, n2, u2) in enumerate(data):
        name = "save_contact" if kind == "contact" else "remember_name"
        a1, a2 = {"name": n1}, {"name": n2}
        if relation:
            a1["relation"] = a2["relation"] = relation
        family = "remember_preferred_name" if kind == "name" else ("contact_name_only" if not relation else "contact_relation_" + relation.replace(" ", "_"))
        pair(26, i, (u1, a1), (u2, a2), name, family, "preferred_name" if kind == "name" else "contacts")

# Each table has a fully written input and explicit argument object per language.
# tool | family | ML user | ML arguments JSON | other user | other arguments JSON
CONTROL_DATA = {
27: '''
open_app|open_video_app|യൂട്യൂബ് ഒന്ന് തുറക്കൂ.|{"app":"YouTube"}|Open YouTube for me.|{"app":"YouTube"}
open_app|open_messaging_app|എനിക്ക് വാട്സ്ആപ്പ് തുറന്നു തരാമോ?|{"app":"WhatsApp"}|Could you launch WhatsApp?|{"app":"WhatsApp"}
open_app|open_payment_app|ഗൂഗിൾ പേ ആപ്പ് തുറക്കൂ, പണം അയക്കണ്ട.|{"app":"Google Pay"}|Open Google Pay; don't send any money.|{"app":"Google Pay"}
open_app|open_camera|ഒരു ഫോട്ടോ എടുക്കാൻ ക്യാമറ തുറക്കൂ.|{"app":"Camera"}|Bring up Camera so I can take a photo.|{"app":"Camera"}
open_app|open_gallery|എന്റെ ഫോട്ടോകൾ കാണാൻ ഗാലറി തുറക്കണേ.|{"app":"Gallery"}|Open Gallery to view my pictures.|{"app":"Gallery"}
open_app|open_email_app|ജിമെയിൽ ഒന്ന് തുറന്നുതരൂ മിലോ.|{"app":"Gmail"}|I'd like Gmail open, please.|{"app":"Gmail"}
open_app|open_maps_app|മാപ്സ് ആപ്പ് തുറന്നു തരാമോ?|{"app":"Maps"}|Please launch the Maps app.|{"app":"Maps"}
open_app|open_calculator|കണക്കു നോക്കണം, കാൽക്കുലേറ്റർ തുറക്കൂ.|{"app":"Calculator"}|Open Calculator; I need to work something out.|{"app":"Calculator"}
phone_setting|flashlight_on|ഇവിടെ ഇരുട്ടാണ്, ടോർച്ച് ഓൺ ആക്കൂ.|{"setting":"flashlight","on":true}|It's dark here; turn the torch on.|{"setting":"flashlight","on":true}
phone_setting|flashlight_off|ഇനി ടോർച്ച് വേണ്ട, ഓഫ് ആക്കൂ.|{"setting":"flashlight","on":false}|Switch the flashlight off now.|{"setting":"flashlight","on":false}
phone_setting|wifi_on|വൈഫൈ ഓൺ ആക്കി തരൂ.|{"setting":"wifi","on":true}|Please enable Wi-Fi on my phone.|{"setting":"wifi","on":true}
phone_setting|wifi_off|ഫോണിലെ വൈഫൈ ഓഫ് ചെയ്യണേ.|{"setting":"wifi","on":false}|Turn Wi-Fi off, please.|{"setting":"wifi","on":false}
phone_setting|bluetooth_on|ബ്ലൂടൂത്ത് ഓൺ ആക്കാമോ?|{"setting":"bluetooth","on":true}|Switch Bluetooth on for me.|{"setting":"bluetooth","on":true}
phone_setting|silent_on|യോഗത്തിലാണ്, ഫോൺ സൈലന്റ് ആക്കൂ.|{"setting":"silent","on":true}|I'm in a meeting; put my phone on silent.|{"setting":"silent","on":true}
phone_setting|volume_up|ശബ്ദം കുറവാണ്, ഒന്ന് കൂട്ടൂ.|{"setting":"volume_up"}|I can't hear well; turn the volume up.|{"setting":"volume_up"}
open_app|open_social_app|ഇൻസ്റ്റഗ്രാം തുറക്കണേ.|{"app":"Instagram"}|Instagram ഒന്ന് open ചെയ്യണേ.|{"app":"Instagram"}
open_app|open_music_app|സ്പോട്ടിഫൈ ആപ്പ് തുറക്കൂ.|{"app":"Spotify"}|Spotify app തുറന്ന് തരൂ.|{"app":"Spotify"}
open_app|open_browser|ക്രോം ബ്രൗസർ തുറക്കാമോ?|{"app":"Chrome"}|Chrome browser open ചെയ്യാമോ?|{"app":"Chrome"}
open_app|open_settings_app|ഫോണിന്റെ സെറ്റിങ്സ് തുറന്നുതരൂ.|{"app":"Settings"}|phone Settings തുറക്കൂ മിലോ.|{"app":"Settings"}
open_app|open_clock_app|ക്ലോക്ക് ആപ്പ് ഒന്ന് തുറക്കൂ.|{"app":"Clock"}|Clock app open ചെയ്യണേ.|{"app":"Clock"}
phone_setting|bluetooth_off|ബ്ലൂടൂത്ത് ഇനി ഓഫ് ചെയ്യൂ.|{"setting":"bluetooth","on":false}|Bluetooth off ആക്കൂ ഇനി.|{"setting":"bluetooth","on":false}
phone_setting|silent_off|ഫോൺ സൈലന്റ് മോഡിൽ നിന്ന് മാറ്റൂ.|{"setting":"silent","on":false}|silent mode off ചെയ്യൂ മിലോ.|{"setting":"silent","on":false}
phone_setting|dnd_on|ഡു നോട്ട് ഡിസ്റ്റർബ് ഓൺ ആക്കൂ.|{"setting":"do_not_disturb","on":true}|Do Not Disturb on ആക്കണേ.|{"setting":"do_not_disturb","on":true}
phone_setting|dnd_off|ഡു നോട്ട് ഡിസ്റ്റർബ് ഓഫ് ചെയ്യൂ.|{"setting":"do_not_disturb","on":false}|Do Not Disturb disable ചെയ്യൂ.|{"setting":"do_not_disturb","on":false}
phone_setting|volume_down|ഫോണിന്റെ ശബ്ദം കുറയ്ക്കൂ.|{"setting":"volume_down"}|phone volume ഒന്ന് down ചെയ്യാമോ?|{"setting":"volume_down"}
''',
28: '''
navigate|navigate_driving|കാറിൽ കൊച്ചിയിലേക്ക് പോകാനുള്ള വഴി കാണിക്കൂ.|{"place":"കൊച്ചി","mode":"driving"}|Show me driving directions to Kochi.|{"place":"Kochi","mode":"driving"}
navigate|navigate_walking|നടന്ന് ലുലു മാളിലേക്ക് പോകാൻ വഴി തരൂ.|{"place":"ലുലു മാൾ","mode":"walking"}|Give me walking directions to Lulu Mall.|{"place":"Lulu Mall","mode":"walking"}
navigate|navigate_bus|ബസിൽ തൃശ്ശൂരിലേക്ക് പോകാനുള്ള വഴി തുറക്കൂ.|{"place":"തൃശ്ശൂർ","mode":"bus"}|Find a bus route to Thrissur.|{"place":"Thrissur","mode":"bus"}
navigate|navigate_bike|ബൈക്കിൽ കോഴിക്കോട് ബീച്ചിലേക്ക് വഴി കാണിക്കൂ.|{"place":"കോഴിക്കോട് ബീച്ച്","mode":"bike"}|Open a bike route to Kozhikode Beach.|{"place":"Kozhikode Beach","mode":"bike"}
navigate|navigate_destination|കോട്ടയം റെയിൽവേ സ്റ്റേഷനിലേക്ക് വഴി കാണിക്കൂ.|{"place":"കോട്ടയം റെയിൽവേ സ്റ്റേഷൻ"}|Get directions to Kottayam railway station.|{"place":"Kottayam railway station"}
navigate|navigate_walking|അടുത്തുള്ള ബസ് സ്റ്റോപ്പിലേക്ക് നടക്കാനുള്ള വഴി കാണിക്കൂ.|{"place":"അടുത്തുള്ള ബസ് സ്റ്റോപ്പ്","mode":"walking"}|Show the walking route to the nearest bus stop.|{"place":"nearest bus stop","mode":"walking"}
play_media|media_song|മലരേ എന്ന പാട്ട് പ്ലേ ചെയ്യൂ.|{"query":"മലരേ"}|Play the song Malare.|{"query":"Malare"}
play_media|media_artist|യേശുദാസിന്റെ പാട്ടുകൾ കേൾപ്പിക്കൂ.|{"query":"യേശുദാസിന്റെ പാട്ടുകൾ"}|Play some songs by Yesudas.|{"query":"songs by Yesudas"}
play_media|media_playlist|സ്പോട്ടിഫൈയിൽ ശാന്തമായ മലയാളം പാട്ടുകൾ പ്ലേ ചെയ്യൂ.|{"query":"ശാന്തമായ മലയാളം പാട്ടുകൾ","app":"spotify"}|Play relaxing Malayalam songs on Spotify.|{"query":"relaxing Malayalam songs","app":"spotify"}
play_media|media_video|യൂട്യൂബിൽ ദോശ ഉണ്ടാക്കുന്ന വീഡിയോ പ്ലേ ചെയ്യൂ.|{"query":"ദോശ ഉണ്ടാക്കുന്ന വീഡിയോ","app":"youtube"}|Play a dosa-making video on YouTube.|{"query":"dosa-making video","app":"youtube"}
play_media|media_radio|മലയാളം റേഡിയോ പ്ലേ ചെയ്യാമോ?|{"query":"മലയാളം റേഡിയോ"}|Put on Malayalam radio.|{"query":"Malayalam radio"}
web_search|search_transport|കൊച്ചിയിൽ നിന്ന് തൃശ്ശൂരിലേക്കുള്ള ട്രെയിൻ സമയം വെബിൽ തിരയൂ.|{"query":"കൊച്ചി തൃശ്ശൂർ ട്രെയിൻ സമയം"}|Search the web for train timings from Kochi to Thrissur.|{"query":"train timings Kochi to Thrissur"}
web_search|search_weather|ഇന്നത്തെ കോഴിക്കോട് കാലാവസ്ഥ വെബിൽ തിരയൂ.|{"query":"ഇന്നത്തെ കോഴിക്കോട് കാലാവസ്ഥ"}|Search for today's weather in Kozhikode.|{"query":"today weather Kozhikode"}
web_search|search_recipe|അവിയൽ ഉണ്ടാക്കുന്നത് എങ്ങനെ എന്ന് വെബിൽ തിരയൂ.|{"query":"അവിയൽ ഉണ്ടാക്കുന്നത് എങ്ങനെ"}|Search online for how to make avial.|{"query":"how to make avial"}
web_search|search_hours|തൃശ്ശൂർ മ്യൂസിയം തുറക്കുന്ന സമയം തിരഞ്ഞു നോക്കൂ.|{"query":"തൃശ്ശൂർ മ്യൂസിയം തുറക്കുന്ന സമയം"}|Search for Thrissur museum opening hours.|{"query":"Thrissur museum opening hours"}
navigate|navigate_driving|കാറിൽ ആലപ്പുഴ ബീച്ചിലേക്ക് വഴി കാണിക്കാമോ?|{"place":"ആലപ്പുഴ ബീച്ച്","mode":"driving"}|Alappuzha Beach-ലേക്ക് driving route കാണിക്കൂ.|{"place":"Alappuzha Beach","mode":"driving"}
navigate|navigate_bus|ബസിൽ തിരുവനന്തപുരത്തേക്ക് പോകാൻ വഴി തുറക്കൂ.|{"place":"തിരുവനന്തപുരം","mode":"bus"}|Thiruvananthapuram-ലേക്ക് bus route കാണിക്കണേ.|{"place":"Thiruvananthapuram","mode":"bus"}
play_media|media_song|സ്പോട്ടിഫൈയിൽ പവിഴമഴ എന്ന പാട്ട് ഇടൂ.|{"query":"പവിഴമഴ","app":"spotify"}|Spotify-ൽ Pavizha Mazha song ഇടൂ.|{"query":"Pavizha Mazha","app":"spotify"}
play_media|media_video|യൂട്യൂബിൽ പേപ്പർ പൂക്കൾ ഉണ്ടാക്കുന്ന വീഡിയോ പ്ലേ ചെയ്യൂ.|{"query":"പേപ്പർ പൂക്കൾ ഉണ്ടാക്കുന്ന വീഡിയോ","app":"youtube"}|YouTube-ൽ paper flower making video പ്ലേ ചെയ്യൂ.|{"query":"paper flower making video","app":"youtube"}
play_media|media_playlist|പഴയ മലയാളം പാട്ടുകൾ കേൾപ്പിക്കാമോ?|{"query":"പഴയ മലയാളം പാട്ടുകൾ"}|old Malayalam songs പ്ലേ ചെയ്യാമോ?|{"query":"old Malayalam songs"}
web_search|search_recipe|ഇഡ്ലി മാവ് തയ്യാറാക്കുന്നത് വെബിൽ തിരയൂ.|{"query":"ഇഡ്ലി മാവ് തയ്യാറാക്കുന്നത്"}|idli batter recipe web-ൽ search ചെയ്യൂ.|{"query":"idli batter recipe"}
web_search|search_local_service|കോട്ടയത്ത് സൈക്കിൾ നന്നാക്കുന്ന കടകൾ തിരയൂ.|{"query":"കോട്ടയം സൈക്കിൾ നന്നാക്കുന്ന കടകൾ"}|Kottayam bicycle repair shops search ചെയ്യൂ.|{"query":"Kottayam bicycle repair shops"}
navigate|navigate_destination|കണ്ണൂർ റെയിൽവേ സ്റ്റേഷനിലേക്കുള്ള വഴി തുറക്കൂ.|{"place":"കണ്ണൂർ റെയിൽവേ സ്റ്റേഷൻ"}|Kannur railway station-ലേക്ക് directions തുറക്കൂ.|{"place":"Kannur railway station"}
play_media|media_radio|സ്പോട്ടിഫൈയിൽ മഴയുടെ ശബ്ദം പ്ലേ ചെയ്യൂ.|{"query":"മഴയുടെ ശബ്ദം","app":"spotify"}|Spotify-ൽ rain sounds പ്ലേ ചെയ്യൂ.|{"query":"rain sounds","app":"spotify"}
web_search|search_translation|ചക്കയുടെ ഇംഗ്ലീഷ് പേര് വെബിൽ തിരയൂ.|{"query":"ചക്ക ഇംഗ്ലീഷ് പേര്"}|ചക്ക English name web search ചെയ്യൂ.|{"query":"ചക്ക English name"}
''',
29: '''
open_app|open_messaging_app|ടെലഗ്രാം ആപ്പ് തുറക്കൂ.|{"app":"Telegram"}|Launch Telegram, please.|{"app":"Telegram"}
open_app|open_shopping_app|ഫ്ലിപ്കാർട്ട് തുറന്നുതരാമോ?|{"app":"Flipkart"}|I'd like to open Flipkart.|{"app":"Flipkart"}
open_app|open_shopping_app|ആമസോൺ ആപ്പ് തുറക്കണേ.|{"app":"Amazon"}|Please bring up Amazon.|{"app":"Amazon"}
open_app|open_video_app|നെറ്റ്ഫ്ലിക്സ് തുറക്കാമോ മിലോ?|{"app":"Netflix"}|Open Netflix for me, Milo.|{"app":"Netflix"}
open_app|open_payment_app|ഫോൺപേ ആപ്പ് മാത്രം തുറക്കൂ.|{"app":"PhonePe"}|Just open the PhonePe app.|{"app":"PhonePe"}
phone_setting|flashlight_on|താക്കോൽ കാണുന്നില്ല, ടോർച്ച് വെളിച്ചം ഓൺ ആക്കൂ.|{"setting":"flashlight","on":true}|I dropped my key; enable the flashlight.|{"setting":"flashlight","on":true}
phone_setting|volume_down|പാട്ടിന്റെ ശബ്ദം കുറച്ചു താഴ്ത്തൂ.|{"setting":"volume_down"}|Lower the volume a little, please.|{"setting":"volume_down"}
phone_setting|dnd_on|ഉറങ്ങാൻ പോകുന്നു, ശല്യപ്പെടുത്തരുത് മോഡ് ഓൺ ചെയ്യൂ.|{"setting":"do_not_disturb","on":true}|I'm going to sleep; enable Do Not Disturb.|{"setting":"do_not_disturb","on":true}
navigate|navigate_walking|ഫോർട്ട് കൊച്ചി ബീച്ചിലേക്ക് നടന്ന് പോകാൻ വഴി തുറക്കൂ.|{"place":"ഫോർട്ട് കൊച്ചി ബീച്ച്","mode":"walking"}|Open walking directions to Fort Kochi Beach.|{"place":"Fort Kochi Beach","mode":"walking"}
navigate|navigate_bike|ബൈക്കിൽ പാളയം മാർക്കറ്റിലേക്ക് വഴി കാണിക്കൂ.|{"place":"പാളയം മാർക്കറ്റ്","mode":"bike"}|Show a bike route to Palayam Market.|{"place":"Palayam Market","mode":"bike"}
navigate|navigate_destination|ഗുരുവായൂരിലേക്ക് പോകാൻ മാപ്സിൽ വഴി തുറക്കൂ.|{"place":"ഗുരുവായൂർ"}|Open Maps directions to Guruvayur.|{"place":"Guruvayur"}
play_media|media_children|കുട്ടികൾക്കുള്ള മലയാളം പാട്ടുകൾ പ്ലേ ചെയ്യൂ.|{"query":"കുട്ടികൾക്കുള്ള മലയാളം പാട്ടുകൾ"}|Play Malayalam songs for children.|{"query":"Malayalam songs for children"}
play_media|media_tutorial|യൂട്യൂബിൽ മലയാളം ടൈപ്പിങ് പഠിക്കുന്ന വീഡിയോ ഇടൂ.|{"query":"മലയാളം ടൈപ്പിങ് പഠിക്കുന്ന വീഡിയോ","app":"youtube"}|Play a Malayalam typing tutorial on YouTube.|{"query":"Malayalam typing tutorial","app":"youtube"}
web_search|search_price|കേരളത്തിലെ ഇന്നത്തെ സ്വർണവില വെബിൽ തിരയൂ.|{"query":"കേരളത്തിലെ ഇന്നത്തെ സ്വർണവില"}|Search the web for today's gold price in Kerala.|{"query":"today gold price Kerala"}
web_search|search_local_service|കോഴിക്കോട്ടെ പൊതു ലൈബ്രറികൾ വെബിൽ തിരയൂ.|{"query":"കോഴിക്കോട് പൊതു ലൈബ്രറികൾ"}|Search online for public libraries in Kozhikode.|{"query":"public libraries in Kozhikode"}
open_app|open_messaging_app|വാട്സ്ആപ്പ് തുറന്ന് തരൂ, ഞാൻ മെസേജ് നോക്കട്ടെ.|{"app":"WhatsApp"}|WhatsApp open ചെയ്യൂ, message ഞാൻ നോക്കാം.|{"app":"WhatsApp"}
phone_setting|wifi_off|വൈഫൈ കണക്ഷൻ ഓഫാക്കി വെക്കൂ.|{"setting":"wifi","on":false}|Wi-Fi connection off ആക്കിവെക്കൂ.|{"setting":"wifi","on":false}
phone_setting|volume_up|വീഡിയോയുടെ ശബ്ദം കുറച്ചു കൂട്ടാമോ?|{"setting":"volume_up"}|video sound ഒന്ന് increase ചെയ്യാമോ?|{"setting":"volume_up"}
navigate|navigate_bus|ബസിൽ മലപ്പുറത്തേക്ക് പോകാനുള്ള വഴി കാണിക്കൂ.|{"place":"മലപ്പുറം","mode":"bus"}|Malappuram-ലേക്ക് bus directions കാണിക്കൂ.|{"place":"Malappuram","mode":"bus"}
navigate|navigate_driving|കാറിൽ നെടുമ്പാശ്ശേരി വിമാനത്താവളത്തിലേക്ക് വഴി തുറക്കൂ.|{"place":"നെടുമ്പാശ്ശേരി വിമാനത്താവളം","mode":"driving"}|Nedumbassery airport-ലേക്ക് car route തുറക്കൂ.|{"place":"Nedumbassery airport","mode":"driving"}
play_media|media_artist|സ്പോട്ടിഫൈയിൽ ചിത്രയുടെ പാട്ടുകൾ ഇടൂ.|{"query":"ചിത്രയുടെ പാട്ടുകൾ","app":"spotify"}|Spotify-ൽ Chithra songs play ചെയ്യണേ.|{"query":"Chithra songs","app":"spotify"}
play_media|media_playlist|മലയാളം ഓണപ്പാട്ടുകൾ ഏതെങ്കിലും ആപ്പിൽ പ്ലേ ചെയ്യൂ.|{"query":"മലയാളം ഓണപ്പാട്ടുകൾ","app":"any"}|Malayalam Onam songs ഏതെങ്കിലും app-ൽ play ചെയ്യൂ.|{"query":"Malayalam Onam songs","app":"any"}
web_search|search_howto|ബാൽക്കണിയിൽ തുളസി വളർത്തുന്നത് വെബിൽ തിരയൂ.|{"query":"ബാൽക്കണിയിൽ തുളസി വളർത്തുന്നത്"}|balcony-ൽ tulsi growing tips web-ൽ search ചെയ്യൂ.|{"query":"tulsi growing tips balcony"}
web_search|search_transport|എറണാകുളം കോട്ടയം ബസ് സമയം തിരയൂ.|{"query":"എറണാകുളം കോട്ടയം ബസ് സമയം"}|Ernakulam Kottayam bus timings search ചെയ്യൂ.|{"query":"Ernakulam Kottayam bus timings"}
web_search|search_events|ഈ വാരാന്ത്യത്തിലെ കൊച്ചിയിലെ പരിപാടികൾ വെബിൽ തിരയൂ.|{"query":"ഈ വാരാന്ത്യം കൊച്ചി പരിപാടികൾ"}|ഈ weekend Kochi events web search ചെയ്യൂ.|{"query":"this weekend Kochi events"}
'''
}

def build_controls():
    categories = {"open_app": "apps", "phone_setting": "settings", "navigate": "navigation", "play_media": "media", "web_search": "search"}
    for batch, block in CONTROL_DATA.items():
        data = [line.split('|') for line in block.strip().splitlines()]
        assert len(data) == 25
        for i, (name, family, u1, j1, u2, j2) in enumerate(data):
            pair(batch, i, (u1, json.loads(j1)), (u2, json.loads(j2)), name, family, categories[name])

STATUS_DATA = '''
phone_status|status_time|ഇപ്പോൾ സമയം എത്രയായി മിലോ?|{}|What's the time right now, Milo?|{}
phone_status|status_date|ഇന്നത്തെ തീയതി പറഞ്ഞുതരാമോ?|{}|Could you tell me today's date?|{}
phone_status|status_weekday|ഇന്ന് ആഴ്ചയിൽ ഏത് ദിവസമാണ്?|{}|What day of the week is it today?|{}
phone_status|status_battery|ഫോണിൽ എത്ര ശതമാനം ചാർജുണ്ട്?|{}|How much battery is left on my phone?|{}
phone_status|status_time_date|ഇന്നത്തെ തീയതിയും ഇപ്പോഴത്തെ സമയവും പറയൂ.|{}|Tell me the current time and today's date.|{}
show_plan|plan_today|ഇന്ന് എനിക്ക് എന്തൊക്കെയുണ്ട്?|{"day":"today"}|What's on my plan for today?|{"day":"today"}
show_plan|plan_tomorrow|നാളെ ചെയ്യാനുള്ള കാര്യങ്ങൾ ഒന്ന് പറയൂ.|{"day":"tomorrow"}|Show me what I have planned tomorrow.|{"day":"tomorrow"}
show_plan|plan_weekday|തിങ്കളാഴ്ചയിലെ എന്റെ പ്ലാൻ കാണിക്കൂ.|{"day":"monday"}|Read out my plan for Monday.|{"day":"monday"}
show_plan|plan_weekday|ചൊവ്വാഴ്ച എനിക്ക് വല്ല പരിപാടിയുമുണ്ടോ?|{"day":"tuesday"}|Do I have anything scheduled on Tuesday?|{"day":"tuesday"}
show_plan|plan_weekday|ബുധനാഴ്ചയിലെ റിമൈൻഡറുകളും പരിപാടികളും കാണിക്കൂ.|{"day":"wednesday"}|Show my reminders and events for Wednesday.|{"day":"wednesday"}
show_plan|plan_date|2027 മാർച്ച് 12-ലെ എന്റെ പ്ലാൻ പറയൂ.|{"day":"2027-03-12"}|Show my plan for 12 March 2027.|{"day":"2027-03-12"}
show_plan|plan_default|എന്റെ ദിവസത്തെ പ്ലാൻ വായിച്ചു തരാമോ?|{}|Could you read out my daily plan?|{}
show_plan|plan_tomorrow|നാളത്തെ റിമൈൻഡറുകൾ നോക്കാമോ?|{"day":"tomorrow"}|What reminders do I have tomorrow?|{"day":"tomorrow"}
phone_status|status_battery_time|സമയവും ബാറ്ററി എത്ര ബാക്കിയുണ്ടെന്നും പറയൂ.|{}|Tell me the time and remaining battery level.|{}
show_plan|plan_weekday|വെള്ളിയാഴ്ച ഞാൻ എന്തൊക്കെ ചെയ്യാനുണ്ട്?|{"day":"friday"}|What do I have to do on Friday?|{"day":"friday"}
phone_status|status_time|മിലോ, ഇപ്പോൾ എത്ര മണിയായി?|{}|മിലോ, current time പറയാമോ?|{}
phone_status|status_date|ഇന്ന് എത്രാം തീയതിയാണ്?|{}|today date ഒന്ന് പറയൂ.|{}
phone_status|status_battery|എന്റെ ഫോൺ ബാറ്ററി നില നോക്കൂ.|{}|phone battery percentage നോക്കൂ.|{}
show_plan|plan_today|ഇന്നത്തെ കാര്യങ്ങൾ എല്ലാം ഒന്ന് വായിക്കൂ.|{"day":"today"}|today plan ഒന്ന് വായിക്കാമോ?|{"day":"today"}
show_plan|plan_tomorrow|നാളെ എനിക്ക് എന്തെങ്കിലും ചെയ്യാനുണ്ടോ?|{"day":"tomorrow"}|tomorrow എന്തൊക്കെ planned ഉണ്ട്?|{"day":"tomorrow"}
show_plan|plan_weekday|വ്യാഴാഴ്ചയിലെ കാര്യങ്ങൾ കാണിക്കണേ.|{"day":"thursday"}|Thursday-ലെ plan കാണിക്കണേ.|{"day":"thursday"}
show_plan|plan_weekday|ശനിയാഴ്ച എനിക്ക് എന്താണ് പ്ലാൻ?|{"day":"saturday"}|Saturday എന്റെ plan എന്താണ്?|{"day":"saturday"}
show_plan|plan_weekday|ഞായറാഴ്ചയിലെ എന്റെ പരിപാടികൾ വായിക്കൂ.|{"day":"sunday"}|Sunday schedule ഒന്ന് വായിക്കൂ.|{"day":"sunday"}
show_plan|plan_date|2027 ഡിസംബർ 25-ന് ഞാൻ എന്തൊക്കെയാണ് വെച്ചിരിക്കുന്നത്?|{"day":"2027-12-25"}|2027 December 25-ലെ എന്റെ plan കാണിക്കൂ.|{"day":"2027-12-25"}
show_plan|plan_default|എനിക്ക് ചെയ്യാനുള്ള റിമൈൻഡറുകളും ലിസ്റ്റുകളും കാണിക്കൂ.|{}|എന്റെ reminders ഉം lists ഉം കാണിക്കൂ.|{}
'''

def build_status():
    data = [line.split('|') for line in STATUS_DATA.strip().splitlines()]
    assert len(data) == 25
    for i, (name, family, u1, j1, u2, j2) in enumerate(data):
        pair(30, i, (u1, json.loads(j1)), (u2, json.loads(j2)), name, family, "status" if name == "phone_status" else "plan")


def main():
    build_messages()
    build_wishes()
    build_calls()
    build_emails()
    build_contacts()
    build_controls()
    build_status()
    # Wrapper families must never be split merely because message content differs.
    # The 200 exact-message rows reuse a pool of literal sentence wrappers.
    for meta in metadata:
        if meta["category"] == "messages_exact":
            meta["scenario"] = meta["family"]
            meta["family"] = "send_message_exact_quote_wrapper_pool"
    order = lambda row: tuple(map(int, row["id"].split('-')))
    rows.sort(key=order)
    metadata.sort(key=order)
    assert len(rows) == len(metadata) == 700
    assert len({r["id"] for r in rows}) == 700
    for batch in range(17, 31):
        selected = [r for r in rows if r["id"].startswith(f"{batch}-")]
        assert Counter(r["lang"] for r in selected) == Counter(ml=25, en=15, mixed=10)
    import sys
    sys.path.insert(0, str(OUT.parent))
    from assemble_dataset import validate_row, input_key
    schema_source = json.loads((OUT.parent / "tools.json").read_text(encoding="utf-8-sig"))
    schemas = {s.get("function", s)["name"]: s.get("function", s) for s in schema_source}
    seen = {}
    for row in rows:
        try:
            validate_row(row, schemas)
        except Exception as exc:
            raise ValueError(f'{row["id"]}: {exc}') from exc
        key = input_key(row)
        assert key not in seen, (row["id"], seen.get(key), "duplicate input")
        seen[key] = row["id"]
        assert row["now"].startswith(("2026", "2027"))
        call = row["messages"][-1]["tool_calls"][0]
        if call["name"] in ("send_message", "send_email", "call_contact", "share_location"):
            assert "sent" not in row["say"].lower()
            assert not any(term in row["say"] for term in ("അയച്ചു", "വിളിച്ചു", "സംസാരിച്ചു"))
        if call["name"] == "send_message" and metadata[rows.index(row)]["category"] == "messages_exact":
            assert call["arguments"]["message"] in row["messages"][0]["content"]
    for filename, content in (("actions.jsonl", rows), ("actions-meta.jsonl", metadata)):
        (OUT / filename).write_text("".join(json.dumps(r, ensure_ascii=False, separators=(",", ":")) + "\n" for r in content), encoding="utf-8")
    print(json.dumps({"rows": len(rows), "batches": "17-30", "languages": dict(Counter(r["lang"] for r in rows)), "categories": dict(Counter(m["category"] for m in metadata)), "families": len({m["family"] for m in metadata}), "schema_errors": 0, "duplicate_inputs": 0, "malayalam_review": "pending_fluent_speaker"}, indent=2))

if __name__ == "__main__":
    main()
