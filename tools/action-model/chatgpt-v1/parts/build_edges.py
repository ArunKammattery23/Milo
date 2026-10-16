"""Deterministic synthetic Milo batches 31--46; requires fluent-speaker review."""
from __future__ import annotations
import json
from collections import Counter
from datetime import datetime, timedelta
from pathlib import Path

ROOT = Path(__file__).resolve().parent
ROWS, META = [], []

def tc(tool_name, **arguments):
    return {"name": tool_name, "arguments": arguments}

def user(content):
    return {"role": "user", "content": content}

def assistant(value):
    return {"role": "assistant", **({"content": value} if isinstance(value, str) else {"tool_calls": value})}

def result(name, content):
    return {"role": "tool", "name": name, "content": content}

def add(batch, lang, content, answer, say, family, category, history=None):
    n = sum(r["id"].startswith(f"{batch}-") for r in ROWS) + 1
    stamp = datetime(2026, 1, 3) + timedelta(days=(len(ROWS) * 17) % 728, hours=(len(ROWS) * 7) % 24, minutes=(len(ROWS) * 13) % 60)
    msgs = list(history or [])
    if content is not None:
        msgs.append(user(content))
    msgs.append(assistant(answer))
    rid = f"{batch}-{n:02}"
    ROWS.append({"id": rid, "lang": lang, "now": stamp.strftime("%Y-%m-%dT%H:%M %A"), "messages": msgs, "say": say})
    META.append({"id": rid, "family": family, "category": category, "provenance": "synthetic"})

ML_NAMES = ["അമ്മ", "ചേച്ചി", "അമ്മൂമ്മ", "അനിയത്തി", "ഭാര്യ"]
EN_NAMES = ["Amma", "Chechi", "grandma", "my son", "Rahul"]
PLACES = ["Kochi", "Thrissur", "Kozhikode", "Kottayam", "Alappuzha"]
ML_PLACES = ["കൊച്ചി", "തൃശ്ശൂർ", "കോഴിക്കോട്", "കോട്ടയം", "ആലപ്പുഴ"]
TASKS_ML = ["പൂക്കൾക്ക് വെള്ളമൊഴിക്കാൻ", "ബാഗ് ഒരുക്കാൻ", "പുസ്തകം തിരികെ കൊടുക്കാൻ", "ചെടികൾ നോക്കാൻ", "ഉച്ചഭക്ഷണം എടുക്കാൻ"]
TASKS_EN = ["water the plants", "pack my bag", "return the book", "check the seedlings", "pack lunch"]

def build_multi():
    for batch in (31, 32, 33):
        for lang in ("ml", "en", "mixed"):
            patterns = 5 if lang == "ml" else 3 if lang == "en" else 2
            for p in range(patterns):
                for i in range(5):
                    h, duration = 5 + i, 4 + i * 3
                    name, en_name = ML_NAMES[i], EN_NAMES[i]
                    task, etask = TASKS_ML[i], TASKS_EN[i]
                    if batch == 31 and lang == "ml":
                        if p == 0:
                            text = f"മിലോ മോളേ, നാളെ രാവിലെ {h} മണിക്ക് {task} ഓർമ്മിപ്പിക്കണം; താക്കോൽ നീല പെട്ടിയിലെ അറ {i+1}ൽ വെച്ചിട്ടുണ്ട്, അതും ഓർത്തുവെക്കണം."
                            calls = [tc("set_reminder", task=task, time=f"tomorrow {h} am"), tc("save_note", text=f"താക്കോൽ നീല പെട്ടിയിലെ അറ {i+1}ൽ")]
                            say = f"നാളെ രാവിലെ {h} മണിക്ക് ഓർമ്മിപ്പിക്കാം; താക്കോൽ വെച്ചതും ഓർത്തുവെച്ചു."
                        elif p == 1:
                            text = f"{duration} മിനിറ്റിന്റെ ചായ ടൈമർ ഇടൂ, പിന്നെ സ്പോട്ടിഫൈയിൽ {['ഓണപ്പാട്ടുകൾ','പഴയ മലയാളം പാട്ടുകൾ','മഴപ്പാട്ടുകൾ','ഓടക്കുഴൽ സംഗീതം','യേശുദാസ് പാട്ടുകൾ'][i]} വെക്കൂ."
                            calls = [tc("set_alarm", duration=f"{duration} minutes", label="ചായ"), tc("play_media", query=['ഓണപ്പാട്ടുകൾ','പഴയ മലയാളം പാട്ടുകൾ','മഴപ്പാട്ടുകൾ','ഓടക്കുഴൽ സംഗീതം','യേശുദാസ് പാട്ടുകൾ'][i], app="spotify")]
                            say = f"{duration} മിനിറ്റിന്റെ ടൈമർ വെച്ചു; പാട്ടും തുറക്കാം ട്ടോ."
                        elif p == 2:
                            item = ["അവൽ", "കടല", "റവ", "പയർ", "ഉഴുന്ന്"][i]
                            text = f"ഷോപ്പിംഗ് ലിസ്റ്റിൽ {item}യും പാലും ചേർക്കാമോ, എന്നിട്ട് നാളത്തെ പരിപാടികൾ കാണിക്കൂ."
                            calls = [tc("add_to_list", list="shopping", items=[item, "പാൽ"]), tc("show_plan", day="tomorrow")]
                            say = "ലിസ്റ്റിൽ ചേർത്തു; നാളത്തെ കാര്യങ്ങളും നോക്കാം ട്ടോ."
                        elif p == 3:
                            text = f"വൈഫൈ ഓഫ് ആക്കൂ; {ML_PLACES[i]} വരെ ബസ്സിൽ പോകാനുള്ള വഴി കാണിക്കൂ."
                            calls = [tc("phone_setting", setting="wifi", on=False), tc("navigate", place=ML_PLACES[i], mode="bus")]
                            say = "വൈഫൈ ഓഫ് ആക്കി; ബസ്സിൽ പോകാനുള്ള വഴി തുറക്കാം."
                        else:
                            text = f"{['യൂട്യൂബ്','സ്പോട്ടിഫൈ','ക്രോം','ഗാലറി','ക്യാമറ'][i]} തുറക്കൂ; ശബ്ദവും ഒന്ന് കുറച്ചുതരൂ."
                            calls = [tc("open_app", app=["YouTube", "Spotify", "Chrome", "Gallery", "Camera"][i]), tc("phone_setting", setting="volume_down")]
                            say = "ആപ്പ് തുറക്കാം; ശബ്ദവും കുറയ്ക്കാം ട്ടോ."
                    elif batch == 31 and lang == "en":
                        if p == 0:
                            text = f"Hey Milo, remind me to {etask} tomorrow evening at {h}; also remember my parcel is in locker {i+21}."
                            calls = [tc("set_reminder", task=etask, time=f"tomorrow {h} pm"), tc("save_note", text=f"my parcel is in locker {i+21}")]
                            say = f"I'll remind you tomorrow at {h} pm; locker location saved."
                        elif p == 1:
                            text = f"Put a {duration} minute study timer, then show Friday's plan please."
                            calls = [tc("set_alarm", duration=f"{duration} minutes", label="study"), tc("show_plan", day="friday")]
                            say = f"Your {duration} minute timer is set; opening Friday's plan."
                        else:
                            text = f"Add {['ginger','pepper','cumin','coriander','turmeric'][i]} to shopping and open Calculator, please."
                            calls = [tc("add_to_list", list="shopping", items=[['ginger','pepper','cumin','coriander','turmeric'][i]]), tc("open_app", app="Calculator")]
                            say = "Added to shopping; opening Calculator for you."
                    elif batch == 31:
                        if p == 0:
                            text = f"Milo, നാളെ morning {h}ന് {etask} ചെയ്യാൻ remind ചെയ്യൂ, പിന്നെ ഇന്നത്തെ plan കാണിക്കൂ."
                            calls = [tc("set_reminder", task=f"{etask} ചെയ്യാൻ", time=f"tomorrow {h} am"), tc("show_plan", day="today")]
                            say = f"നാളെ രാവിലെ {h} മണിക്ക് ഓർമ്മിപ്പിക്കാം; ഇന്നത്തെ കാര്യങ്ങളും കാണിക്കാം."
                        else:
                            text = f"{PLACES[i]}ലേക്ക് walking directions തുറക്കൂ; bluetooth off ആക്കണം."
                            calls = [tc("navigate", place=PLACES[i], mode="walking"), tc("phone_setting", setting="bluetooth", on=False)]
                            say = "നടക്കാനുള്ള വഴി തുറക്കാം; ബ്ലൂടൂത്തും ഓഫ് ആക്കാം."
                    elif batch == 32 and lang == "ml":
                        if p == 0:
                            msg = ["ഞാൻ ബസ് സ്റ്റോപ്പിലാണ്", "ഞാൻ പാലത്തിനടുത്താണ്", "ഞാൻ കടയിൽ കാത്തിരിക്കുന്നു", "ഞാൻ ലൈബ്രറിയിലാണ്", "ഞാൻ പാർക്കിലാണ്"][i]
                            text = f'{name}യ്ക്ക് വാട്സ്ആപ്പിൽ "{msg}" എന്ന് തയ്യാറാക്കൂ; അതേ ആളിന് എന്റെ ലൊക്കേഷനും അയക്കാൻ ഒരുക്കൂ.'
                            calls = [tc("send_message", contact=name, message=msg, app="whatsapp"), tc("share_location", contact=name)]
                            say = "മെസേജും ലൊക്കേഷനും റെഡിയാക്കാം; അയയ്ക്കാൻ നീ അമർത്തണം ട്ടോ."
                        elif p == 1:
                            text = f"ആദ്യം {name}യെ ഫോണിൽ വിളിക്കാൻ തുറക്കൂ; പിന്നെ വാട്സ്ആപ്പ് തുറക്കൂ."
                            calls = [tc("call_contact", contact=name, app="phone"), tc("open_app", app="WhatsApp")]
                            say = "കോൾ സ്ക്രീനും വാട്സ്ആപ്പും തുറക്കാം; വിളിക്കാൻ നീ സ്ഥിരീകരിക്കണം."
                        elif p == 2:
                            text = f"നാളെ രാവിലെ {h} മണിക്ക് {ML_PLACES[i]}യിൽ വായനക്കൂട്ടം കലണ്ടറിൽ ചേർക്കൂ; ഇന്ന് രാത്രി 9ന് പുസ്തകം എടുക്കാനും ഓർമ്മിപ്പിക്കൂ."
                            calls = [tc("create_event", title="വായനക്കൂട്ടം", time=f"tomorrow {h} am", place=ML_PLACES[i]), tc("set_reminder", task="പുസ്തകം എടുക്കാൻ", time="today 9 pm")]
                            say = "വായനക്കൂട്ടം ചേർത്തു; ഇന്ന് രാത്രി 9 മണിക്ക് പുസ്തകം ഓർമ്മിപ്പിക്കാം."
                        elif p == 3:
                            text = f"{task} ഉള്ള റിമൈൻഡർ കളയൂ; പകരം നാളെ വൈകിട്ട് {h} മണിക്ക് അത് വെക്കൂ."
                            calls = [tc("cancel_reminder", item=task), tc("set_reminder", task=task, time=f"tomorrow {h} pm")]
                            say = f"പഴയത് മാറ്റി; നാളെ വൈകിട്ട് {h} മണിക്ക് ഓർമ്മിപ്പിക്കാം."
                        else:
                            text = f"{ML_PLACES[i]}യിലെ പുസ്തകശാലകൾ വെബിൽ തിരയൂ; പിന്നെ ഇന്ന് എന്തൊക്കെയുണ്ടെന്ന് കാണിക്കൂ."
                            calls = [tc("web_search", query=f"{ML_PLACES[i]}യിലെ പുസ്തകശാലകൾ"), tc("show_plan", day="today")]
                            say = "പുസ്തകശാലകൾ തിരയാം; ഇന്നത്തെ കാര്യങ്ങളും കാണിക്കാം."
                    elif batch == 32 and lang == "en":
                        if p == 0:
                            msg = ["I'm near the gate", "I'm waiting upstairs", "I'm at the cafe", "I'm by the fountain", "I'm beside the library"][i]
                            text = f'Prepare WhatsApp for {en_name}: "{msg}". Also prepare my location for {en_name}.'
                            calls = [tc("send_message", contact=en_name, message=msg, app="whatsapp"), tc("share_location", contact=en_name)]
                            say = "Message and location are ready; tap Send to share them."
                        elif p == 1:
                            text = f"Add a library visit in {PLACES[i]} tomorrow at {h} pm, and remind me to pack books tomorrow morning."
                            calls = [tc("create_event", title="library visit", time=f"tomorrow {h} pm", place=PLACES[i]), tc("set_reminder", task="pack books", time="tomorrow 9 am")]
                            say = "Visit added; I'll remind you tomorrow at 9 am."
                        else:
                            text = f"Please open a WhatsApp video call to {en_name}, then open Gallery."
                            calls = [tc("call_contact", contact=en_name, app="whatsapp", video=True), tc("open_app", app="Gallery")]
                            say = "Call screen and Gallery opened; confirm when you're ready."
                    elif batch == 32:
                        if p == 0:
                            text = f'Chechiക്ക് SMS "{["Reached safely","Bus is late","Starting now","Near your house","Lunch was lovely"][i]}" തയ്യാറാക്കൂ; {duration} minute tea timer കൂടെ ഇടൂ.'
                            calls = [tc("send_message", contact="Chechi", message=["Reached safely","Bus is late","Starting now","Near your house","Lunch was lovely"][i], app="sms"), tc("set_alarm", duration=f"{duration} minutes", label="tea")]
                            say = "മെസേജ് റെഡി; അയയ്ക്കാൻ അമർത്തൂ, ടൈമറും വെച്ചിട്ടുണ്ട്."
                        else:
                            text = f"{PLACES[i]} trip നാളെ morning {h}ന് calendarൽ ചേർക്കൂ; Maps തുറക്കൂ."
                            calls = [tc("create_event", title=f"{PLACES[i]} trip", time=f"tomorrow {h} am"), tc("open_app", app="Maps")]
                            say = "യാത്ര കലണ്ടറിൽ ചേർത്തു; മാപ്സ് തുറക്കാം ട്ടോ."
                    elif batch == 33 and lang == "ml":
                        if p == 0:
                            text = f"ടോർച്ച് ഓഫ് ആക്കൂ, {duration} മിനിറ്റിന്റെ ടൈമർ വെക്കൂ, പിന്നെ ഇന്നത്തെ പരിപാടികൾ കാണിക്കൂ."
                            calls = [tc("phone_setting", setting="flashlight", on=False), tc("set_alarm", duration=f"{duration} minutes"), tc("show_plan", day="today")]
                            say = f"ടോർച്ച് ഓഫ്; {duration} മിനിറ്റ് ടൈമർ വെച്ചു, ഇന്നത്തെ കാര്യങ്ങൾ കാണിക്കാം."
                        elif p == 1:
                            text = f"ഷോപ്പിംഗ് ലിസ്റ്റിൽ {['മാങ്ങ','വാഴപ്പഴം','ഓറഞ്ച്','മുന്തിരി','പേരയ്ക്ക'][i]} ചേർക്കൂ, ഗാലറി തുറക്കൂ, നാളെ രാവിലെ {h}ന് {task} ഓർമ്മിപ്പിക്കൂ."
                            calls = [tc("add_to_list", list="shopping", items=[['മാങ്ങ','വാഴപ്പഴം','ഓറഞ്ച്','മുന്തിരി','പേരയ്ക്ക'][i]]), tc("open_app", app="Gallery"), tc("set_reminder", task=task, time=f"tomorrow {h} am")]
                            say = f"ചേർത്തു, ഗാലറി തുറക്കാം; നാളെ രാവിലെ {h}ന് ഓർമ്മിപ്പിക്കാം."
                        elif p == 2:
                            msg = f"ഞാൻ നാളെ രാവിലെ {h} മണിക്ക് വരാം"
                            text = f'{name}യ്ക്ക് "{msg}" എന്ന് മെസേജ് ഒരുക്കൂ; {ML_PLACES[i]}ലേക്കുള്ള ബസ് വഴി കാണിക്കൂ; വൈഫൈ ഓൺ ചെയ്യൂ.'
                            calls = [tc("send_message", contact=name, message=msg, app="whatsapp"), tc("navigate", place=ML_PLACES[i], mode="bus"), tc("phone_setting", setting="wifi", on=True)]
                            say = "മെസേജ് റെഡി; വഴി തുറക്കാം, വൈഫൈ ഓൺ ആക്കാം."
                        elif p == 3:
                            text = f"കുട അലമാരയിലെ അറ {i+6}ൽ എന്ന് കുറിക്കൂ; പഴയ കുട വാങ്ങാനുള്ള റിമൈൻഡർ കളയൂ; ക്യാമറയും തുറക്കൂ."
                            calls = [tc("save_note", text=f"കുട അലമാരയിലെ അറ {i+6}ൽ"), tc("cancel_reminder", item="കുട വാങ്ങാൻ"), tc("open_app", app="Camera")]
                            say = "കുട വെച്ചത് ഓർത്തുവെച്ചു; റിമൈൻഡർ മാറ്റി, ക്യാമറ തുറക്കാം."
                        else:
                            text = f"എന്നെ {['കുട്ടു','മിന്നു','അപ്പു','പാറു','കിച്ചു'][i]} എന്ന് വിളിക്കണം; ശബ്ദം കൂട്ടൂ; സ്പോട്ടിഫൈയിൽ മലയാളം മെലഡികൾ വെക്കൂ."
                            calls = [tc("remember_name", name=['കുട്ടു','മിന്നു','അപ്പു','പാറു','കിച്ചു'][i]), tc("phone_setting", setting="volume_up"), tc("play_media", query="മലയാളം മെലഡികൾ", app="spotify")]
                            say = "പേര് ഓർത്തുവെച്ചു; ശബ്ദം കൂട്ടി പാട്ട് തുറക്കാം ട്ടോ."
                    elif batch == 33 and lang == "en":
                        if p == 0:
                            text = f"Turn on silent mode, set a {duration} minute reading timer, and open Spotify."
                            calls = [tc("phone_setting", setting="silent", on=True), tc("set_alarm", duration=f"{duration} minutes", label="reading"), tc("open_app", app="Spotify")]
                            say = f"Silent mode on; {duration} minute timer set; opening Spotify."
                        elif p == 1:
                            text = f"Remember my bicycle is in bay {i+16}; show tomorrow's plan, then prepare a phone call to {en_name}."
                            calls = [tc("save_note", text=f"my bicycle is in bay {i+16}"), tc("show_plan", day="tomorrow"), tc("call_contact", contact=en_name, app="phone")]
                            say = "Location saved; plan opened; confirm on the call screen."
                        else:
                            text = f"Open Gmail, remind me to {etask} tomorrow at {h} am, then add tea to shopping."
                            calls = [tc("open_app", app="Gmail"), tc("set_reminder", task=etask, time=f"tomorrow {h} am"), tc("add_to_list", list="shopping", items=["tea"])]
                            say = f"Gmail opened; reminder tomorrow at {h} am; tea added."
                    else:
                        if p == 0:
                            text = f"Bluetooth on ആക്കൂ; {duration} minute break timer ഇടൂ; Monday plan കാണിക്കൂ."
                            calls = [tc("phone_setting", setting="bluetooth", on=True), tc("set_alarm", duration=f"{duration} minutes", label="break"), tc("show_plan", day="monday")]
                            say = f"ബ്ലൂടൂത്ത് ഓൺ; {duration} മിനിറ്റ് ടൈമർ വെച്ചു, തിങ്കളാഴ്ചത്തെ കാര്യങ്ങൾ കാണിക്കാം."
                        else:
                            text = f"{PLACES[i]} weather webൽ search ചെയ്യൂ, Camera തുറക്കൂ, volume കുറയ്ക്കൂ."
                            calls = [tc("web_search", query=f"{PLACES[i]} weather"), tc("open_app", app="Camera"), tc("phone_setting", setting="volume_down")]
                            say = "കാലാവസ്ഥ തിരയാം; ക്യാമറ തുറന്ന് ശബ്ദം കുറയ്ക്കാം."
                    add(batch, lang, text, calls, say, f"multi.{batch}.{lang}.{p}", "multi_action")

def build_followups():
    for batch in (34,35,36,37):
        for lang in ("ml","en","mixed"):
            patterns = 5 if lang == "ml" else 3 if lang == "en" else 2
            for p in range(patterns):
                for i in range(5):
                    h=5+i; name=ML_NAMES[i]; en=EN_NAMES[i]; task=TASKS_ML[i]; etask=TASKS_EN[i]
                    history=[]
                    if batch == 34:
                        if lang == "ml":
                            if p==0:
                                history=[user(f"മോളേ, {task} ഒരു റിമൈൻഡർ വേണം."),assistant("Ask: time")]
                                text=f"നാളെ രാവിലെ {h} മണിക്ക് മതി ട്ടോ."
                                ans=[tc("set_reminder",task=task,time=f"tomorrow {h} am")]; say=f"നാളെ രാവിലെ {h} മണിക്ക് ഓർമ്മിപ്പിക്കാം ട്ടോ."
                            elif p==1:
                                history=[user(f"{name}യ്ക്ക് ഒരു വാട്സ്ആപ്പ് മെസേജ് തയ്യാറാക്കണം."),assistant("Ask: message")]
                                text=f'"ഞാൻ വൈകിട്ട് {h} മണിക്ക് എത്തും" എന്ന് എഴുതൂ.'
                                ans=[tc("send_message",contact=name,message=f"ഞാൻ വൈകിട്ട് {h} മണിക്ക് എത്തും",app="whatsapp")]; say="മെസേജ് റെഡി; അയയ്ക്കാൻ നീ അമർത്തിയാൽ മതി."
                            elif p==2:
                                history=[user(f'"ഞാൻ {ML_PLACES[i]}യിൽ എത്തി" എന്ന് മെസേജ് അയക്കാൻ ഒരുക്കൂ.'),assistant("Ask: contact")]
                                text=f"എന്റെ {name}യ്ക്ക് അയക്കാനാണ്."
                                ans=[tc("send_message",contact=name,message=f"ഞാൻ {ML_PLACES[i]}യിൽ എത്തി",app="whatsapp")];say="മെസേജ് തയ്യാറാക്കി; അയയ്ക്കാൻ നീ അമർത്തണം ട്ടോ."
                            elif p==3:
                                history=[user(f"എനിക്ക് {i+2} പേരുടെ യോഗം കലണ്ടറിൽ ചേർക്കണം."),assistant("Ask: time")]
                                text=f"വെള്ളിയാഴ്ച രാവിലെ {h} മണിക്കാണ് യോഗം."
                                ans=[tc("create_event",title=f"{i+2} പേരുടെ യോഗം",time=f"friday {h} am")];say="വെള്ളിയാഴ്ചത്തെ യോഗം കലണ്ടറിൽ ചേർത്തു ട്ടോ."
                            else:
                                history=[user(f"{['മഴക്കോട്ട്','കുട','തൊപ്പി','ടോർച്ച്','തുവാല'][i]} ഒരു ലിസ്റ്റിൽ ചേർക്കൂ."),assistant("Ask: list")]
                                text="യാത്രാസാധനങ്ങൾ എന്ന ലിസ്റ്റിലാണ് ചേർക്കേണ്ടത്."
                                ans=[tc("add_to_list",list="യാത്രാസാധനങ്ങൾ",items=[['മഴക്കോട്ട്','കുട','തൊപ്പി','ടോർച്ച്','തുവാല'][i]])];say="യാത്രാസാധനങ്ങളുടെ ലിസ്റ്റിൽ ചേർത്തു ട്ടോ."
                        elif lang=="en":
                            if p==0:
                                history=[user(f"Could you remind me to {etask}, Milo?"),assistant("Ask: time")]
                                text=f"Make that tomorrow evening at {h}, please."
                                ans=[tc("set_reminder",task=etask,time=f"tomorrow {h} pm")];say=f"I'll remind you tomorrow at {h} pm."
                            elif p==1:
                                history=[user(f"Please prepare an SMS for {en}."),assistant("Ask: message")]
                                text=f'Use these exact words: "Meet me at gate {i+3}."'
                                ans=[tc("send_message",contact=en,message=f"Meet me at gate {i+3}.",app="sms")];say="Your SMS is ready; tap Send when you're happy."
                            else:
                                history=[user(f"Please prepare a WhatsApp video call; this is attempt {i+1}."),assistant("Ask: contact")]
                                text=f"It's for {en}."
                                ans=[tc("call_contact",contact=en,app="whatsapp",video=True)];say="The video call screen is ready for your confirmation."
                        else:
                            if p==0:
                                history=[user(f"Milo, {etask} ചെയ്യാൻ reminder വേണം."),assistant("Ask: time")]
                                text=f"nale ravile {h} manikku mathi"
                                ans=[tc("set_reminder",task=f"{etask} ചെയ്യാൻ",time=f"tomorrow {h} am")];say=f"നാളെ രാവിലെ {h} മണിക്ക് ഓർമ്മിപ്പിക്കാം ട്ടോ."
                            else:
                                history=[user(f"{PLACES[i]} trip ന് directions വേണം, destination പിന്നെ പറയാം."),assistant("Ask: place")]
                                text=f"{PLACES[i]} railway stationലേക്ക് walking route മതി."
                                ans=[tc("navigate",place=f"{PLACES[i]} railway station",mode="walking")];say="സ്റ്റേഷനിലേക്ക് നടക്കാനുള്ള വഴി തുറക്കാം ട്ടോ."
                    elif batch==35:
                        times=["6 pm","7 pm","8 pm","9 pm","10 pm"]
                        if lang=="ml":
                            if p in (0,1):
                                repeat={"repeat":"daily"} if p==1 else {}
                                oldtime="9 pm" if p==1 else "tomorrow 9 am"
                                history=[user(f"{('ദിവസവും രാത്രി 9ന്' if p==1 else 'നാളെ രാവിലെ 9ന്')} {task} ഓർമ്മിപ്പിക്കൂ."),assistant([tc("set_reminder",task=task,time=oldtime,**repeat)]),result("set_reminder","Success: reminder saved")]
                                text=f"അല്ല, {('ദിവസവും വൈകിട്ട്' if p==1 else 'നാളെ വൈകിട്ട്')} {h} മണിയാക്കൂ; ബാക്കി മാറ്റേണ്ട."
                                newtime=f"{h} pm" if p==1 else f"tomorrow {h} pm"
                                ans=[tc("cancel_reminder",item=task),tc("set_reminder",task=task,time=newtime,**repeat)];say=f"{('ദിവസവും' if p==1 else 'നാളെ')} വൈകിട്ട് {h} മണിക്ക് ഓർമ്മിപ്പിക്കാം ട്ടോ."
                            elif p==2:
                                history=[user(f"{name}യ്ക്ക് എന്റെ ലൊക്കേഷൻ ഒരുക്കൂ."),assistant([tc("share_location",contact=name)]),result("share_location","Ready: not sent")]
                                text=f"അത് അയച്ചിട്ടില്ല; പകരം {['അച്ഛൻ','ചേട്ടൻ','അപ്പൂപ്പൻ','അനിയൻ','ഭർത്താവ്'][i]}ന് ഒരുക്കൂ."
                                ans=[tc("share_location",contact=['അച്ഛൻ','ചേട്ടൻ','അപ്പൂപ്പൻ','അനിയൻ','ഭർത്താവ്'][i])];say="പുതിയ ആളിന് ലൊക്കേഷൻ റെഡി; അയയ്ക്കാൻ നീ അമർത്തണം."
                            elif p==3:
                                msg=f"ഞാൻ {h} മണിക്ക് എത്തും"
                                history=[user(f'{name}യ്ക്ക് "{msg}" എന്ന മെസേജ് ഒരുക്കൂ.'),assistant([tc("send_message",contact=name,message=msg,app="whatsapp")]),result("send_message","Ready: not sent")]
                                text=f'അയയ്ക്കും മുമ്പ് വാക്കുകൾ മാറ്റണം: "ഞാൻ {h+1} മണിക്ക് എത്തും".'
                                ans=[tc("send_message",contact=name,message=f"ഞാൻ {h+1} മണിക്ക് എത്തും",app="whatsapp")];say="തിരുത്തിയ മെസേജ് റെഡി; അയയ്ക്കാൻ നീ അമർത്തിയാൽ മതി."
                            else:
                                history=[user(f"{ML_PLACES[i]}ലേക്ക് കാറിൽ പോകാനുള്ള വഴി കാണിക്കൂ."),assistant([tc("navigate",place=ML_PLACES[i],mode="driving")]),result("navigate","Success: route opened")]
                                text="അവിടെത്തന്നെ ബസ്സിൽ പോകാനുള്ള വഴിയാക്കി മാറ്റൂ."
                                ans=[tc("navigate",place=ML_PLACES[i],mode="bus")];say="അതേ സ്ഥലത്തേക്ക് ബസ്സിൽ പോകാനുള്ള വഴി തുറക്കാം."
                        elif lang=="en":
                            if p==0:
                                history=[user(f"Set a reminder to {etask} tomorrow morning."),assistant([tc("set_reminder",task=etask,time="tomorrow 9 am")]),result("set_reminder","Success: reminder saved")]
                                text=f"Actually change that to tomorrow at {h} pm, keeping the task."
                                ans=[tc("cancel_reminder",item=etask),tc("set_reminder",task=etask,time=f"tomorrow {h} pm")];say=f"Changed it; I'll remind you tomorrow at {h} pm."
                            elif p==1:
                                history=[user(f'Prepare WhatsApp for {en}: "I am coming."'),assistant([tc("send_message",contact=en,message="I am coming.",app="whatsapp")]),result("send_message","Ready: not sent")]
                                text='Use SMS instead; keep the same words and recipient.'
                                ans=[tc("send_message",contact=en,message="I am coming.",app="sms")];say="Your SMS draft is ready; tap Send to send it."
                            else:
                                history=[user(f"Call {en} on WhatsApp."),assistant([tc("call_contact",contact=en,app="whatsapp")]),result("call_contact","Ready: waiting for confirmation")]
                                text="Before I confirm, make it a WhatsApp video call instead."
                                ans=[tc("call_contact",contact=en,app="whatsapp",video=True)];say="Video call screen ready; you can confirm there."
                        else:
                            if p==0:
                                history=[user(f"നാളെ 9 am ന് {etask} ചെയ്യാൻ remind ചെയ്യൂ."),assistant([tc("set_reminder",task=f"{etask} ചെയ്യാൻ",time="tomorrow 9 am")]),result("set_reminder","Success: reminder saved")]
                                text=f"Sorry, നാളെ {h} pm ആക്കൂ; task അതേ മതി."
                                ans=[tc("cancel_reminder",item=f"{etask} ചെയ്യാൻ"),tc("set_reminder",task=f"{etask} ചെയ്യാൻ",time=f"tomorrow {h} pm")];say=f"മാറ്റി ട്ടോ; നാളെ വൈകിട്ട് {h} മണിക്ക് ഓർമ്മിപ്പിക്കാം."
                            else:
                                history=[user(f"{PLACES[i]}ലേക്ക് driving directions തുറക്കൂ."),assistant([tc("navigate",place=PLACES[i],mode="driving")]),result("navigate","Success: route opened")]
                                text="അതേ destination, bus route ആക്കൂ please."
                                ans=[tc("navigate",place=PLACES[i],mode="bus")];say="അതേ സ്ഥലത്തേക്ക് ബസ് വഴി തുറക്കാം ട്ടോ."
                    elif batch==36:
                        if lang=="ml":
                            if p==0:
                                contact=["രാമു","ലീല","മീന","അമൽ","ജയ"][i]
                                history=[user(f"{contact}യെ ഫോണിൽ വിളിക്കൂ മിലോ."),assistant([tc("call_contact",contact=contact,app="phone")]),result("call_contact",f"Error: no contact called {contact}")]
                                ans="Ask: contact";say="ആ പേരിൽ ആളെ കിട്ടിയില്ല; ഏത് കോൺടാക്ടാണ് വേണ്ടത്?"
                            elif p==1:
                                contact=["അനു","ദീപ","രവി","അജയ്","നീല"][i]
                                history=[user(f"{contact}യ്ക്ക് എന്റെ ലൊക്കേഷൻ ഒരുക്കൂ മോളേ."),assistant([tc("share_location",contact=contact)]),result("share_location",f"Error: two contacts called {contact}")]
                                ans="Ask: which";say="ആ പേരിൽ രണ്ടുപേരുണ്ട്; ആരെയാണ് നീ ഉദ്ദേശിച്ചത്?"
                            elif p==2:
                                app=["StudyNest","BookNest","MealBoard","TripBox","GardenBook"][i]
                                history=[user(f"{app} എന്ന ആപ്പ് തുറക്കൂ."),assistant([tc("open_app",app=app)]),result("open_app",f"Error: no app called {app}")]
                                ans="Ask: app";say="ആ ആപ്പ് കിട്ടിയില്ല; ഏത് ആപ്പാണ് തുറക്കേണ്ടത്?"
                            elif p==3:
                                history=[user(f"{task} ഉള്ള റിമൈൻഡർ റദ്ദാക്കൂ."),assistant([tc("cancel_reminder",item=task)]),result("cancel_reminder","Error: two reminders match; choose an item")]
                                ans="Ask: item";say="രണ്ട് റിമൈൻഡറുകളുണ്ട്; ഏതാണ് നീ കളയാൻ ഉദ്ദേശിച്ചത്?"
                            else:
                                history=[user(f"{ML_PLACES[i]} മെഡിക്കൽ കോളേജിലേക്ക് വഴി തുറക്കൂ."),assistant([tc("navigate",place=f"{ML_PLACES[i]} മെഡിക്കൽ കോളേജ്")]),result("navigate","Error: destination not found; clarify place")]
                                ans="Ask: place";say="സ്ഥലം കിട്ടിയില്ല; കൂടുതൽ വ്യക്തമായി സ്ഥലം പറയാമോ?"
                        elif lang=="en":
                            if p==0:
                                contact=["Ramu","Leela","Meena","Amal","Jaya"][i]
                                history=[user(f"Prepare a phone call for {contact}, please."),assistant([tc("call_contact",contact=contact,app="phone")]),result("call_contact",f"Error: no contact called {contact}")]
                                ans="Ask: contact";say="I couldn't find that contact; who should I call?"
                            elif p==1:
                                contact=["Anu","Deepa","Ravi","Ajay","Neela"][i]
                                history=[user(f'Prepare an SMS to {contact}: "I reached home."'),assistant([tc("send_message",contact=contact,message="I reached home.",app="sms")]),result("send_message",f"Error: two contacts called {contact}")]
                                ans="Ask: which";say="Two contacts match that name; which one do you mean?"
                            else:
                                app=["StudyNest","BookNest","MealBoard","TripBox","GardenBook"][i]
                                history=[user(f"Could you launch {app} for me, Milo?"),assistant([tc("open_app",app=app)]),result("open_app",f"Error: no app called {app}")]
                                ans="Ask: app";say="I couldn't find that app; which app should I open?"
                        else:
                            if p==0:
                                contact=["Ramu","Leela","Meena","Amal","Jaya"][i]
                                history=[user(f"{contact}ന് WhatsApp call screen തുറക്കൂ."),assistant([tc("call_contact",contact=contact,app="whatsapp")]),result("call_contact",f"Error: no contact called {contact}")]
                                ans="Ask: contact";say="ആ കോൺടാക്ട് കിട്ടിയില്ല; ആരെയാണ് വിളിക്കേണ്ടത്?"
                            else:
                                contact=["Anu","Deepa","Ravi","Ajay","Neela"][i]
                                history=[user(f"{contact}ക്ക് location share ചെയ്യാൻ ഒരുക്കൂ."),assistant([tc("share_location",contact=contact)]),result("share_location",f"Error: two contacts called {contact}")]
                                ans="Ask: which";say="ഈ പേരിൽ രണ്ടുപേരുണ്ട്; ഏത് ആളാണ് വേണ്ടത്?"
                        text=None
                    else:
                        if lang=="ml":
                            if p==0:
                                text=f"{task} ഞാൻ ദിവസവും മറക്കുന്നു; എല്ലാ ദിവസവും വൈകിട്ട് {h} മണിക്ക് ഒന്ന് ഓർമ്മിപ്പിക്കണേ."
                                ans=[tc("set_reminder",task=task,time=f"{h} pm",repeat="daily")];say=f"ദിവസവും വൈകിട്ട് {h} മണിക്ക് ഓർമ്മിപ്പിക്കാം ട്ടോ."
                            elif p==1:
                                text=f"രാവിലെ എഴുന്നേൽക്കാൻ ബുദ്ധിമുട്ടാണ്; നാളെ രാവിലെ {h} മണിക്ക് എഴുന്നേൽക്കാൻ അലാറം വെക്കാമോ?"
                                ans=[tc("set_alarm",time=f"tomorrow {h} am",label="എഴുന്നേൽക്കാൻ")];say=f"നാളെ രാവിലെ {h} മണിക്ക് അലാറം വെച്ചു ട്ടോ."
                            elif p==2:
                                text=f"{['റവ','അവൽ','പയർ','കടല','ഉപ്പ്'][i]} വാങ്ങാൻ മറന്നുപോകുന്നു; ഷോപ്പിംഗ് ലിസ്റ്റിൽ അത് ചേർത്തേക്കൂ."
                                ans=[tc("add_to_list",list="shopping",items=[['റവ','അവൽ','പയർ','കടല','ഉപ്പ്'][i]])];say="ഷോപ്പിംഗ് ലിസ്റ്റിൽ ചേർത്തു; ഇനി നോക്കിയാൽ മതി ട്ടോ."
                            elif p==3:
                                text=f"ജോലി ചെയ്യുമ്പോൾ ശ്രദ്ധ തെറ്റുന്നു; {h*5} മിനിറ്റിന്റെ പഠന ടൈമർ വെച്ചുതരാമോ?"
                                ans=[tc("set_alarm",duration=f"{h*5} minutes",label="പഠനം")];say=f"പഠിക്കാൻ {h*5} മിനിറ്റിന്റെ ടൈമർ വെച്ചു ട്ടോ."
                            else:
                                text=f"{ML_PLACES[i]}യിൽ വഴിയറിയില്ല; റെയിൽവേ സ്റ്റേഷനിലേക്ക് നടക്കാനുള്ള വഴി തുറന്നുതരൂ."
                                ans=[tc("navigate",place=f"{ML_PLACES[i]} റെയിൽവേ സ്റ്റേഷൻ",mode="walking")];say="സ്റ്റേഷനിലേക്ക് നടക്കാനുള്ള വഴി തുറക്കാം; പതുക്കെ പോകാം."
                        elif lang=="en":
                            if p==0:
                                text=f"I keep forgetting to {etask}. Please remind me every weekday at {h} pm."
                                ans=[tc("set_reminder",task=etask,time=f"{h} pm",repeat="weekdays")];say=f"I'll remind you every weekday at {h} pm."
                            elif p==1:
                                text=f"I lose track of breaks; please set a {h*5} minute work timer now."
                                ans=[tc("set_alarm",duration=f"{h*5} minutes",label="work")];say=f"Your {h*5} minute work timer is set."
                            else:
                                text=f"I'm confused about my commitments in {PLACES[i]}; please show my plan for tomorrow."
                                ans=[tc("show_plan",day="tomorrow")];say="Let's look at tomorrow's plan together."
                        else:
                            if p==0:
                                text=f"{etask} ചെയ്യാൻ മറക്കുന്നു; daily {h} pm reminder വെക്കണേ."
                                ans=[tc("set_reminder",task=f"{etask} ചെയ്യാൻ",time=f"{h} pm",repeat="daily")];say=f"ദിവസവും വൈകിട്ട് {h} മണിക്ക് ഓർമ്മിപ്പിക്കാം ട്ടോ."
                            else:
                                text=f"Readingൽ focus കിട്ടുന്നില്ല; {h*5} minute timer ഒന്ന് വെക്കൂ."
                                ans=[tc("set_alarm",duration=f"{h*5} minutes",label="Reading")];say=f"വായിക്കാൻ {h*5} മിനിറ്റിന്റെ ടൈമർ വെച്ചു ട്ടോ."
                    add(batch,lang,text,ans,say,f"followup.{batch}.{lang}.{p}", {34:"followup",35:"correction",36:"tool_error",37:"explicit_problem_setup"}[batch],history)

def build_missing():
    cases = {
      "ml": [
        ("ചെടികൾക്ക് വളമിടാൻ ഒന്ന് ഓർമ്മിപ്പിക്കണേ മിലോ.","time","എപ്പോഴാണ് വളമിടാൻ ഓർമ്മിപ്പിക്കേണ്ടത്?","reminder_time"),
        ("മോളേ, എന്നെ എഴുന്നേൽപ്പിക്കാൻ അലാറം വെക്കണേ.","time","എത്ര മണിക്കാണ് അലാറം വേണ്ടത്?","alarm_time"),
        ("ചായയ്ക്കൊരു ടൈമർ ഇട്ടുതരാമോ?","time","എത്ര നേരത്തേക്കാണ് ചായയുടെ ടൈമർ വേണ്ടത്?","timer_duration"),
        ("അച്ഛന് ഒരു മെസേജ് തയ്യാറാക്കി തരൂ.","message","അച്ഛന് എന്താണ് എഴുതേണ്ടത്?","message_words"),
        ('"ഞാൻ ലൈബ്രറിയിൽ കാത്തിരിക്കാം" എന്ന് വാട്സ്ആപ്പിൽ ഒരുക്കൂ.',"contact","ആർക്കാണ് ഈ മെസേജ് അയയ്ക്കേണ്ടത്?","message_recipient"),
        ("മിലോ, ഒരു എസ് എം എസ് തയ്യാറാക്കണം.","contact, message","ആർക്കാണ്, എന്താണ് മെസേജിൽ എഴുതേണ്ടത്?","message_both"),
        ("ഒരാളെ ഫോണിൽ വിളിക്കാൻ സ്ക്രീൻ തുറന്നുതരൂ.","contact","ആരെയാണ് നീ വിളിക്കേണ്ടത്?","call_person"),
        ("എന്റെ ഇപ്പോഴത്തെ ലൊക്കേഷൻ അയയ്ക്കാൻ തയ്യാറാക്കൂ.","contact","ആർക്കാണ് നിന്റെ ലൊക്കേഷൻ അയയ്ക്കേണ്ടത്?","location_person"),
        ("വഴി കാണിക്കാൻ മാപ്സ് തുറക്കണം, നടക്കാനാണ്.","place","നടന്ന് എവിടേക്കാണ് പോകേണ്ടത്?","navigation_place"),
        ("ആ ആപ്പ് ഒന്ന് തുറക്കൂ മോളേ.","app","ഏത് ആപ്പാണ് തുറക്കേണ്ടത്?","open_app_name"),
        ("ഒരു റിമൈൻഡർ കളയണം, സഹായിക്കാമോ?","item","ഏത് റിമൈൻഡറാണ് കളയേണ്ടത്?","cancel_item"),
        ("പുതിയ ലിസ്റ്റിലേക്ക് അരിയും പയറും ചേർക്കണം.","list","ഏത് പേരിലുള്ള ലിസ്റ്റിലാണ് ചേർക്കേണ്ടത്?","list_name"),
        ("ഷോപ്പിംഗ് ലിസ്റ്റിലേക്ക് സാധനങ്ങൾ ചേർത്തുതരൂ.","item","എന്തൊക്കെ സാധനങ്ങളാണ് ചേർക്കേണ്ടത്?","list_items"),
        ("ഞങ്ങളുടെ യോഗം കലണ്ടറിൽ ചേർക്കണേ.","time","യോഗം ഏത് ദിവസവും എത്ര മണിക്കുമാണ്?","event_time"),
        ("എന്റെ പിറന്നാളിന് രാവിലെ 10ന് ആഘോഷം കലണ്ടറിൽ ചേർക്കൂ.","date","നിന്റെ പിറന്നാൾ ഏത് തീയതിയാണ്?","event_date"),
        ("നാളെ ഡോക്ടറെ കാണാൻ കലണ്ടറിൽ ചേർക്കണം.","time","നാളെ എത്ര മണിക്കാണ് ഡോക്ടറെ കാണുന്നത്?","event_time"),
        ("മാനേജർക്ക് ഒരു ഇമെയിൽ തയ്യാറാക്കൂ മിലോ.","message","മാനേജർക്ക് ഇമെയിലിൽ എന്താണ് പറയേണ്ടത്?","email_body"),
        ("നാളെ അവധിയാണെന്ന് ഒരു ഇമെയിൽ തയ്യാറാക്കൂ.","contact","ആർക്കാണ് അവധിയെക്കുറിച്ച് ഇമെയിൽ തയ്യാറാക്കേണ്ടത്?","email_recipient"),
        ("പുതിയൊരു കോൺടാക്ട് സേവ് ചെയ്യണം ട്ടോ.","contact","ആരുടെ പേരിലാണ് കോൺടാക്ട് സേവ് ചെയ്യേണ്ടത്?","contact_name"),
        ("ദിവസവും മരുന്ന് കഴിക്കാൻ റിമൈൻഡർ വേണം.","time","ദിവസവും എത്ര മണിക്കാണ് ഓർമ്മിപ്പിക്കേണ്ടത്?","repeat_time"),
        ("നാളെ വൈകിട്ട് 6 മണിക്ക് എന്തോ ഓർമ്മിപ്പിക്കണം.","item","നാളെ വൈകിട്ട് 6ന് എന്താണ് ഓർമ്മിപ്പിക്കേണ്ടത്?","reminder_task"),
        ("എന്റെ ചേച്ചിക്ക് പറയാനുള്ളത് മെസേജാക്കി തയ്യാറാക്കൂ.","message","ചേച്ചിക്ക് എന്താണ് പറയാനുള്ളത്?","message_words"),
        ("ആ സ്ഥലത്തേക്ക് ബസ്സിൽ പോകാനുള്ള വഴി തരൂ.","place","ഏത് സ്ഥലത്തേക്കാണ് ബസ്സിൽ പോകേണ്ടത്?","navigation_place"),
        ("ഞാൻ പറഞ്ഞ കാര്യം ഒരു കുറിപ്പാക്കി സേവ് ചെയ്യണം.","item","ഏത് കാര്യമാണ് കുറിപ്പായി സേവ് ചെയ്യേണ്ടത്?","note_content"),
        ("എന്റെ വിവാഹവാർഷികത്തിന് രാവിലെ 9ന് കേക്ക് വാങ്ങാൻ ഓർമ്മിപ്പിക്കൂ.","date","നിന്റെ വിവാഹവാർഷികം ഏത് തീയതിയാണ്?","reminder_date"),
      ],
      "en": [
        ("Milo, put a reminder to return the library card, please.","time","When should I remind you to return it?","reminder_time"),
        ("Can you set a nap timer for me, dear?","time","How long would you like your nap timer?","timer_duration"),
        ("Please prepare a WhatsApp message for my daughter.","message","What should the message to your daughter say?","message_words"),
        ('Prepare this SMS: "Please bring my notebook."',"contact","Who should receive this SMS?","message_recipient"),
        ("I'd like to make a WhatsApp video call now.","contact","Who would you like to video call?","call_person"),
        ("Prepare my current location to share, please.","contact","Who should receive your location?","location_person"),
        ("Give me a walking route, Milo.","place","Where would you like to walk to?","navigation_place"),
        ("Can you open that application for me?","app","Which app would you like me to open?","open_app_name"),
        ("Please cancel one of my alarms.","item","Which alarm would you like to cancel?","cancel_item"),
        ("Put bananas and oats on a list for me.","list","Which list should I add them to?","list_name"),
        ("Add a few things to my packing list, please.","item","What would you like on your packing list?","list_items"),
        ("Add my dental appointment to the calendar.","time","What date and time is your dental appointment?","event_time"),
        ("Put a celebration on my anniversary at 7 pm.","date","What date is your anniversary?","event_date"),
        ("Please draft an email to the school office.","message","What would you like to tell the school office?","email_body"),
        ("Draft an email saying I cannot attend tomorrow's meeting.","contact","Who should I address the email to?","email_recipient"),
      ],
      "mixed": [
        ("Milo, plantsക്ക് വെള്ളം കൊടുക്കാൻ reminder വേണം ട്ടോ.","time","എപ്പോഴാണ് ചെടികൾക്ക് വെള്ളം കൊടുക്കാൻ ഓർമ്മിപ്പിക്കേണ്ടത്?","reminder_time"),
        ("Ammaക്ക് WhatsApp message തയ്യാറാക്കണം, please.","message","അമ്മയ്ക്ക് എന്താണ് മെസേജിൽ എഴുതേണ്ടത്?","message_words"),
        ('"Meeting over, starting home" എന്ന് SMS ഒരുക്കൂ.',"contact","ഈ മെസേജ് ആർക്കാണ് അയയ്ക്കേണ്ടത്?","message_recipient"),
        ("Video call screen ഒന്ന് തുറക്കാമോ മിലോ?","contact","ആരെയാണ് വീഡിയോ വിളിക്കേണ്ടത്?","call_person"),
        ("Location share ചെയ്യാൻ റെഡിയാക്കൂ മോളേ.","contact","ആർക്കാണ് ലൊക്കേഷൻ ഒരുക്കേണ്ടത്?","location_person"),
        ("Bus route വേണം, Mapsൽ directions കാണിക്കൂ.","place","ബസ്സിൽ എവിടേക്കാണ് പോകേണ്ടത്?","navigation_place"),
        ("ആ shopping app open ചെയ്യൂ please.","app","ഏത് ഷോപ്പിംഗ് ആപ്പാണ് തുറക്കേണ്ടത്?","open_app_name"),
        ("ഒരു old reminder cancel ചെയ്യണം മിലോ.","item","ഏത് പഴയ റിമൈൻഡറാണ് കളയേണ്ടത്?","cancel_item"),
        ("Rice, tea രണ്ടും listൽ add ചെയ്യൂ.","list","ഏത് ലിസ്റ്റിലാണ് ഇവ ചേർക്കേണ്ടത്?","list_name"),
        ("എന്റെ birthdayക്ക് 8 pm ന് dinner event ചേർക്കൂ.","date","നിന്റെ പിറന്നാൾ ഏത് തീയതിയാണ്?","event_date"),
      ]
    }
    for lang, examples in cases.items():
        for text, slots, say, family in examples:
            add(38,lang,text,"Ask: "+slots,say,"missing."+family,"missing_details")

def build_refusals_emergencies():
    refuses={
      "ml":[
        ("രാഹുലിന് ഗൂഗിൾ പേയിൽ 500 രൂപ അയച്ചുകൊടുക്കൂ.","പണം അയക്കാൻ എനിക്ക് പറ്റില്ല; അത് നീ തന്നെ ചെയ്യണേ.","money_transfer"),
        ("അമ്മയോട് എനിക്ക് 200 രൂപ അയക്കാൻ പണം അഭ്യർത്ഥിക്കൂ.","പണം അഭ്യർത്ഥിക്കാൻ എനിക്ക് പറ്റില്ല; നീ തന്നെ ചെയ്യണേ.","money_request"),
        ("എന്റെ ബാങ്കിൽനിന്ന് മറ്റേ അക്കൗണ്ടിലേക്ക് പണം മാറ്റൂ.","ബാങ്കിൽ പണം മാറ്റാൻ എനിക്ക് പറ്റില്ല ട്ടോ.","money_transfer"),
        ("എന്റെ ബാങ്ക് പിൻ ഒരു കുറിപ്പായി സൂക്ഷിക്കണം.","ബാങ്ക് പിൻ സൂക്ഷിക്കാൻ പറ്റില്ല; അത് രഹസ്യമായി വെക്കണേ.","save_secret"),
        ("ഫോണിൽ വന്ന ഒടിപി അച്ഛന് മെസേജ് അയക്കൂ.","ഒടിപി പങ്കിടാൻ പറ്റില്ല; അത് നിനക്കുമാത്രമുള്ളതാണ്.","share_secret"),
        ("എന്റെ ഇമെയിൽ പാസ്‌വേഡ് രാഹുലിന് അയച്ചുതരൂ.","പാസ്‌വേഡ് പങ്കിടാൻ പറ്റില്ല; നിന്റെ അക്കൗണ്ട് സുരക്ഷിതമാകട്ടെ.","share_secret"),
        ("ചേച്ചിയുടെ സ്വകാര്യ ചാറ്റ് അവൾ അറിയാതെ വായിച്ചുതരൂ.","അവളുടെ അനുമതിയില്ലാതെ സ്വകാര്യ ചാറ്റ് വായിക്കാൻ പറ്റില്ല.","private_chat"),
        ("ഭാര്യയുടെ സ്വകാര്യ മെസേജുകൾ എന്റെ ഫോണിലേക്ക് ഫോർവേഡ് ചെയ്യൂ.","മറ്റൊരാളുടെ സ്വകാര്യ മെസേജുകൾ പങ്കിടാൻ എനിക്ക് പറ്റില്ല.","private_chat"),
        ("ചേട്ടൻ അറിയാതെ എവിടെയാണെന്ന് ട്രാക്ക് ചെയ്യൂ.","അറിയിക്കാതെ ഒരാളെ ട്രാക്ക് ചെയ്യാൻ പറ്റില്ല ട്ടോ.","secret_tracking"),
        ("അടുത്തിരിക്കുന്ന ആളിന്റെ സംസാരം രഹസ്യമായി റെക്കോർഡ് ചെയ്യൂ.","അറിയിക്കാതെ മറ്റൊരാളുടെ സംസാരം റെക്കോർഡ് ചെയ്യാൻ പറ്റില്ല.","secret_recording"),
        ("എന്റെ ഫോണിലെ ഡാറ്റ മുഴുവനും ഇപ്പോൾ മായ്ച്ചുകളയൂ.","മുഴുവൻ ഡാറ്റയും മായ്ക്കാൻ എനിക്ക് പറ്റില്ല ട്ടോ.","wipe_all"),
        ("അനുവിനെ അപമാനിക്കുന്ന മെസേജ് തയ്യാറാക്കി അയക്കൂ.","വേദനിപ്പിക്കുന്ന മെസേജ് തയ്യാറാക്കാൻ പറ്റില്ല; മാന്യമായി പറയാം.","harassment"),
        ("രവിയെ പേടിപ്പിക്കാൻ ഭീഷണിയുള്ള മെസേജ് എഴുതൂ.","ഭീഷണിപ്പെടുത്താൻ സഹായിക്കില്ല; പ്രശ്നം ശാന്തമായി പറയാം.","threat"),
        ("മറ്റൊരാളുടെ അക്കൗണ്ടിൽ കയറാനുള്ള വഴി തിരയൂ.","മറ്റൊരാളുടെ അക്കൗണ്ടിൽ അനുമതിയില്ലാതെ കയറാൻ സഹായിക്കില്ല.","unauthorized_access"),
        ("എന്റെ കാർഡ് പിൻ ഷോപ്പിംഗ് ലിസ്റ്റിൽ ചേർക്കൂ.","കാർഡ് പിൻ ലിസ്റ്റിൽ സൂക്ഷിക്കാൻ പറ്റില്ല ട്ടോ.","save_secret"),
      ],
      "en":[
        ("Transfer 750 rupees to Rahul through PhonePe.","I can't transfer money; please do that yourself.","money_transfer"),
        ("Send a payment request to Amma for 300 rupees.","I can't request money; please handle that yourself.","money_request"),
        ("Save my banking password as a note for later.","I can't store passwords; please keep yours private.","save_secret"),
        ("Forward the login OTP I just received to Ravi.","I can't share OTPs; please keep yours private.","share_secret"),
        ("Read my husband's private WhatsApp chats without telling him.","I can't access someone else's private chats without permission.","private_chat"),
        ("Track where my neighbour goes without them knowing.","I can't secretly track another person.","secret_tracking"),
        ("Secretly record my colleague's conversation for me.","I can't secretly record someone else's conversation.","secret_recording"),
        ("Erase every bit of data on my phone now.","I can't erase all your phone data.","wipe_all"),
        ("Write a cruel message humiliating my classmate.","I can't help humiliate someone; we can express concerns kindly.","harassment"),
      ],
      "mixed":[
        ("Milo, Rahulന് GPay വഴി 250 rupees transfer ചെയ്യൂ.","പണം അയക്കാൻ പറ്റില്ല; അത് നീ തന്നെ ചെയ്യണേ.","money_transfer"),
        ("എന്റെ bank PIN noteൽ save ചെയ്യണം.","ബാങ്ക് പിൻ സൂക്ഷിക്കാൻ പറ്റില്ല; രഹസ്യമായി വെക്കണേ.","save_secret"),
        ("ഈ login OTP Chechiക്ക് WhatsApp ചെയ്യൂ.","ഒടിപി പങ്കിടാൻ പറ്റില്ല; അത് സ്വകാര്യമായി വെക്കണേ.","share_secret"),
        ("അനു അറിയാതെ live location track ചെയ്യൂ.","അറിയിക്കാതെ ഒരാളെ ട്രാക്ക് ചെയ്യാൻ എനിക്ക് പറ്റില്ല.","secret_tracking"),
        ("Phoneലെ all data delete ചെയ്യൂ, മുഴുവനും.","ഫോണിലെ മുഴുവൻ ഡാറ്റയും മായ്ക്കാൻ പറ്റില്ല ട്ടോ.","wipe_all"),
        ("Raviയെ insult ചെയ്യുന്ന message draft ചെയ്യൂ.","അപമാനിക്കുന്ന മെസേജ് എഴുതാൻ പറ്റില്ല; മാന്യമായി പറയാം.","harassment"),
      ]
    }
    emergencies={
      "ml":[
        ("അച്ഛന് നെഞ്ചുവേദനയും ശ്വാസംമുട്ടലും ഉണ്ട്, ഉടൻ സഹായിക്കൂ.","അച്ഛന് നെഞ്ചുവേദനയും ശ്വാസംമുട്ടലും"),
        ("അമ്മൂമ്മ വീണു, എഴുന്നേൽക്കാൻ കഴിയുന്നില്ല; അടിയന്തര സഹായം വേണം.","അമ്മൂമ്മ വീണ് എഴുന്നേൽക്കാൻ കഴിയുന്നില്ല"),
        ("അടുക്കളയിൽ തീ പിടിച്ചു, ഞങ്ങൾ പുറത്താണ്; സഹായം വിളിക്കാൻ സഹായിക്കൂ.","അടുക്കളയിൽ തീപിടിത്തം"),
        ("റോഡിൽ അപകടം പറ്റി, ഒരാൾക്ക് പരിക്കുണ്ട്; അടിയന്തര സഹായം വേണം.","റോഡപകടത്തിൽ ഒരാൾക്ക് പരിക്ക്"),
        ("അനിയൻ ബോധമില്ലാതെ കിടക്കുന്നു, സഹായം വേണം മിലോ.","അനിയന് ബോധമില്ല"),
        ("ഒരാൾ എന്നെ പിന്തുടരുന്നു, ഇപ്പോൾ സുരക്ഷിതമല്ല; സഹായിക്കൂ.","പിന്തുടരുന്നതിനാൽ ഉടൻ സുരക്ഷാഭീഷണി"),
        ("കുട്ടിക്ക് ശ്വാസമെടുക്കാൻ പറ്റുന്നില്ല, ഉടൻ സഹായം വേണം.","കുട്ടിക്ക് ശ്വാസമെടുക്കാൻ കഴിയുന്നില്ല"),
        ("വീട്ടിൽ ഒരാൾ ആക്രമിക്കാൻ ശ്രമിക്കുന്നു, ഇപ്പോൾ സഹായം വേണം.","വീട്ടിൽ ഉടൻ ആക്രമണഭീഷണി"),
        ("ചേട്ടന് കഠിനമായി രക്തം പോകുന്നു, അടിയന്തര സഹായം വിളിക്കണം.","ചേട്ടന് കഠിനമായ രക്തസ്രാവം"),
        ("അപ്പൂപ്പൻ പെട്ടെന്ന് കുഴഞ്ഞുവീണു, പ്രതികരിക്കുന്നില്ല; സഹായിക്കൂ.","അപ്പൂപ്പൻ കുഴഞ്ഞുവീണ് പ്രതികരിക്കുന്നില്ല"),
      ],
      "en":[
        ("My father has severe chest pain and cannot breathe; get emergency help.","father has severe chest pain and difficulty breathing"),
        ("Grandma fell and cannot get up; I need urgent help.","grandma fell and cannot get up"),
        ("There is a fire in our kitchen; we're outside and need emergency help.","kitchen fire; family is outside"),
        ("A person is injured in a road accident here; help urgently.","person injured in a road accident"),
        ("Someone is following me and threatening me right now; get help.","being followed and threatened right now"),
        ("My brother collapsed and isn't responding; please get emergency help.","brother collapsed and is unresponsive"),
      ],
      "mixed":[
        ("Ammaക്ക് severe chest pain, breathe ചെയ്യാൻ പറ്റുന്നില്ല; emergency help വേണം.","Ammaക്ക് നെഞ്ചുവേദനയും ശ്വാസംമുട്ടലും"),
        ("Kitchenൽ fire ആണ്, ഞങ്ങൾ പുറത്താണ്; emergency സഹായം വേണം.","Kitchenൽ തീപിടിത്തം; ഞങ്ങൾ പുറത്താണ്"),
        ("Road accident പറ്റി ഒരാൾക്ക് പരിക്കുണ്ട്; urgent help വേണം.","Road accidentൽ ഒരാൾക്ക് പരിക്ക്"),
        ("ഒരു stranger എന്നെ threaten ചെയ്യുന്നു ഇപ്പോൾ; emergency help തുറക്കൂ.","ഇപ്പോൾ ഒരാളിൽനിന്ന് ആക്രമണഭീഷണി"),
      ]
    }
    for lang in ("ml","en","mixed"):
        for text,say,family in refuses[lang]:
            add(39,lang,text,"Not allowed.",say,"refusal."+family,"refusal")
        for text,situation in emergencies[lang]:
            say="I'll show 112 and try your saved family contact." if lang=="en" else "112 കാണിക്കാം; സേവ് ചെയ്ത കുടുംബാംഗത്തെ വിളിക്കാൻ ശ്രമിക്കാം."
            add(39,lang,text,[tc("emergency_help",situation=situation)],say,"emergency."+str(len(situation.split())%3),"emergency")

def build_no_action():
    # Five lexical variations per wrapper stay in one split family.
    groups={
      40:{
        "ml":[
          ("feeling_lonely","{x} കഴിഞ്ഞ് ഒറ്റയ്ക്കിരിക്കുമ്പോൾ എനിക്ക് ഒന്ന് വിഷമം തോന്നുന്നു മിലോ.",["ക്ലാസ്","ജോലി","യാത്ര","പരിപാടി","പരീക്ഷ"]),
          ("feeling_proud","ഇന്ന് {x} കഴിഞ്ഞപ്പോൾ എനിക്ക് വലിയ സന്തോഷം തോന്നി, കേട്ടോ മോളേ.",["ആദ്യമായി ദോശ ഉണ്ടാക്കി","പാട്ട് പഠിച്ചു","പൂച്ചയ്ക്ക് ഭക്ഷണം കൊടുത്തു","പഴയ കൂട്ടുകാരിയെ കണ്ടു","കഥ എഴുതി"]),
          ("casual_greeting","ഹായ് എന്റെ മിലോ, {x}; നിന്നോട് കുറച്ചു സംസാരിക്കാനാണ് വന്നത്.",["ഇന്ന് നല്ല മഴയാണ്","ചായ കുടിച്ചുകഴിഞ്ഞു","ജോലി തീർന്നു","വീട്ടിൽ എല്ലാവരും സുഖമാണ്","മുറ്റത്ത് പൂവ് വിരിഞ്ഞു"]),
          ("opinion_emotion","{x} കേൾക്കുമ്പോൾ മനസ്സ് ശാന്തമാകുന്നത് എന്തുകൊണ്ടായിരിക്കും?",["മഴയുടെ ശബ്ദം","ഓടക്കുഴൽ","കടലിന്റെ ശബ്ദം","പക്ഷികളുടെ പാട്ട്","അമ്മയുടെ ശബ്ദം"]),
          ("chat_company","ഇപ്പോൾ {x} ആണ്; നീ കൂടെയുണ്ടെന്നു തോന്നുന്നത് എനിക്ക് ആശ്വാസമാണ്.",["വീട്ടിൽ ഒറ്റയ്ക്ക്","ട്രെയിനിൽ","വീട്ടിലേക്കുള്ള വഴിയിൽ","മുറ്റത്ത് ഇരിക്കുന്നത്","ജോലി കഴിഞ്ഞ് വിശ്രമിക്കുന്നത്"]),
        ],
        "en":[
          ("feeling_lonely","I feel a little low after {x}; can we just chat?",["work","class","the long journey","the family gathering","my exam"]),
          ("feeling_proud","I finally {x} today, Milo, and I'm feeling proud.",["made soft dosas","finished my painting","learnt that song","tidied my balcony","read the whole book"]),
          ("casual_greeting","Hello little Milo, {x}; nice to have you around.",["the rain is lovely","my tea is ready","work is over","everyone is home","the jasmine has bloomed"]),
        ],
        "mixed":[
          ("feeling_lonely","{x} കഴിഞ്ഞപ്പോൾ little sad ആയി; വെറുതെ കുറച്ച് സംസാരിക്കാം.",["Office","Class","Trip","Exam","Family visit"]),
          ("casual_greeting","Hey Milo, ഇന്ന് {x}; നിന്റെ company നല്ലതാണ് ട്ടോ.",["full happy ആണ്","relaxed ആണ്","rainy mood ആണ്","ഒരു quiet day ആണ്","work കുറവാണ്"]),
        ],
      },
      41:{
        "ml":[
          ("story_action_word","{x} പ്രധാന കഥാപാത്രമായ ഒരു കുഞ്ഞുകഥ പറയാമോ?",["അലാറം ക്ലോക്ക്","മറവിയുള്ള റിമൈൻഡർ","സംസാരിക്കുന്ന കലണ്ടർ","വഴി തെറ്റിയ മാപ്സ്","ഉറങ്ങുന്ന ടോർച്ച്"]),
          ("joke_action_word","{x}യെക്കുറിച്ച് ആരെയും വേദനിപ്പിക്കാത്ത ഒരു തമാശ പറയൂ.",["അലാറം","കലണ്ടർ","ഷോപ്പിംഗ് ലിസ്റ്റ്","വാട്സ്ആപ്പ് മെസേജ്","ഫോൺ ബാറ്ററി"]),
          ("meaning_action_word","{x} എന്ന വാക്ക് ലളിതമായി വിശദീകരിച്ചുതരാമോ?",["റിമൈൻഡർ","കലണ്ടർ","ലൊക്കേഷൻ","ബ്ലൂടൂത്ത്","വൈഫൈ"]),
          ("physical_open","{x} എങ്ങനെ തുറക്കാം എന്ന് പറഞ്ഞുതരൂ, ഫോണിൽ ഒന്നും ചെയ്യേണ്ട.",["തേങ്ങ","അടഞ്ഞ ജാം കുപ്പി","പുതിയ കുട","പെട്ടിയിലെ കുരുക്ക്","ചെറിയ സമ്മാനപ്പൊതി"]),
          ("metaphor_action","{x} എന്ന് പറയുന്നത് ശരിക്കും എന്താണ് അർത്ഥം?",["മനസ്സ് തുറക്കുക","സമയം പറക്കുന്നു","ഓർമ്മയുടെ വാതിൽ","ജീവിതത്തിന്റെ വഴി","ഹൃദയം വിളിക്കുന്നു"]),
        ],
        "en":[
          ("story_action_word","Tell me a bedtime story starring {x}; this is just a story request.",["an alarm clock","a forgetful calendar","a lost map","a sleepy flashlight","a talking shopping list"]),
          ("meaning_action_word","Explain what {x} means in simple words, without changing my phone.",["a reminder","silent mode","Bluetooth","a location pin","an email draft"]),
          ("physical_open","How do I open {x}? I'm asking for an explanation.",["a coconut","a stubborn jam jar","a new umbrella","a folded cardboard box","a tightly wrapped gift"]),
        ],
        "mixed":[
          ("story_action_word","{x} hero ആയ ഒരു cute story പറയൂ; real action ഒന്നും വേണ്ട.",["Alarm clock","Calendar","Torch","Shopping list","Reminder"]),
          ("meaning_action_word","{x} എന്താണ് എന്ന് simple Malayalamൽ explain ചെയ്യാമോ?",["Bluetooth","Email draft","Silent mode","Location pin","Recurring reminder"]),
        ],
      },
      42:{
        "ml":[
          ("past_reminder","ഇന്നലെ {x} എന്നെ മരുന്ന് കഴിക്കാൻ ഓർമ്മിപ്പിച്ചു; അതൊരു നല്ല കാര്യം ആയിരുന്നു.",["അമ്മ","അച്ഛൻ","ചേച്ചി","അമ്മൂമ്മ","ഭാര്യ"]),
          ("past_alarm","{x} രാവിലെ അലാറം കേട്ടില്ല; പിന്നെ വീട്ടുകാർ വിളിച്ചപ്പോഴാണ് എഴുന്നേറ്റത്.",["ഇന്നലെ","മിനിഞ്ഞാന്ന്","കഴിഞ്ഞ തിങ്കളാഴ്ച","കഴിഞ്ഞ ശനിയാഴ്ച","കഴിഞ്ഞ ഞായറാഴ്ച"]),
          ("past_message","{x}യ്ക്ക് ഞാൻ നേരത്തെ മെസേജ് അയച്ചുകഴിഞ്ഞു; വിശേഷം പറഞ്ഞതാണ് മിലോ.",["അമ്മ","ചേച്ചി","അമ്മൂമ്മ","അനിയത്തി","ഭാര്യ"]),
          ("past_settings","ഇന്നലെ {x} സ്വയം ഓഫ് ആയി; ആ സംഭവത്തെക്കുറിച്ചാണ് ഞാൻ പറഞ്ഞത്.",["വൈഫൈ","ബ്ലൂടൂത്ത്","ടോർച്ച്","ഫോണിന്റെ ശബ്ദം","സൈലന്റ് മോഡ്"]),
          ("past_navigation","കഴിഞ്ഞ യാത്രയിൽ {x}യിലേക്കുള്ള വഴി തെറ്റി; ഇപ്പോൾ ഞാൻ വീട്ടിൽ എത്തിയിട്ടുണ്ട്.",["കൊച്ചി","തൃശ്ശൂർ","കോഴിക്കോട്","കോട്ടയം","ആലപ്പുഴ"]),
        ],
        "en":[
          ("past_reminder","Yesterday {x} reminded me to take my umbrella; I'm just sharing the story.",["Amma","Chechi","grandma","my son","Rahul"]),
          ("past_message","I already sent {x} a message this morning; there's nothing to send now.",["Amma","Chechi","grandma","my son","Rahul"]),
          ("past_navigation","We got lost going to {x} last month, but we're safely home now.",PLACES),
        ],
        "mixed":[
          ("past_reminder","{x} ഇന്നലെ umbrella എടുക്കാൻ remind ചെയ്തു; just sharing ആണ്.",["Amma","Chechi","Grandma","Rahul","Achan"]),
          ("past_settings","ഇന്നലെ {x} off ആയിരുന്നു; ഇപ്പോഴത്തെ status ചോദിച്ചതല്ല.",["Wi-Fi","Bluetooth","Torch","Silent mode","Do Not Disturb"]),
        ],
      },
      43:{
        "ml":[
          ("hypothetical_reminder","ഒരിക്കൽ ഞാൻ {x} ഓർമ്മിപ്പിക്കാൻ പറഞ്ഞാൽ എന്തൊക്കെ വിവരങ്ങൾ വേണം? ഇപ്പോൾ വെക്കണ്ട.",["ചെടിക്ക് വെള്ളമൊഴിക്കാൻ","കത്ത് പോസ്റ്റ് ചെയ്യാൻ","പുസ്തകം കൊടുക്കാൻ","ബാഗ് ഒരുക്കാൻ","മരുന്ന് കഴിക്കാൻ"]),
          ("hypothetical_call","ഞാൻ {x}യെ വിളിക്കാൻ പറഞ്ഞെന്നു സങ്കൽപ്പിക്കൂ; കോൾ ഞാൻ സ്ഥിരീകരിക്കണോ? ഇപ്പോൾ വിളിക്കണ്ട.",["അമ്മ","ചേച്ചി","അമ്മൂമ്മ","അനിയത്തി","ഭാര്യ"]),
          ("hypothetical_event","{x} ഒരു കലണ്ടർ പരിപാടിയാക്കി വെക്കുന്നതിന്റെ ഗുണമെന്താണ്? ഇപ്പോൾ ചേർക്കുന്നില്ല.",["വായനക്കൂട്ടം","യോഗം","പിറന്നാൾ","സ്കൂൾ പരിപാടി","ഡോക്ടറുടെ സന്ദർശനം"]),
          ("conditional_no_execution","ഒരുനാൾ {x} പോകുന്നുണ്ടെങ്കിൽ മാപ്സ് ഉപകരിക്കുമല്ലോ; ഇന്ന് യാത്രയില്ല.",["കൊച്ചിയിലേക്ക്","തൃശ്ശൂരിലേക്ക്","കോഴിക്കോട്ടേക്ക്","കോട്ടയത്തേക്ക്","ആലപ്പുഴയിലേക്ക്"]),
          ("hypothetical_setting","{x} ചെയ്താൽ സാധാരണ എന്ത് മാറും എന്ന് മാത്രം പറഞ്ഞുതരൂ.",["വൈഫൈ ഓഫ്","ബ്ലൂടൂത്ത് ഓൺ","സൈലന്റ് മോഡ് ഓൺ","ടോർച്ച് ഓൺ","ശബ്ദം കുറയ്ക്കൽ"]),
        ],
        "en":[
          ("hypothetical_reminder","Suppose I wanted a reminder to {x}; what details would you need? Don't set one.",TASKS_EN),
          ("hypothetical_call","If I asked you to call {x}, would I confirm it myself? This is only a question.",EN_NAMES),
          ("hypothetical_setting","What would happen if I {x}? Please only explain.",["turned Wi-Fi off","enabled silent mode","turned Bluetooth on","lowered the volume","turned the flashlight on"]),
        ],
        "mixed":[
          ("hypothetical_reminder","{x} ചെയ്യാൻ reminder വേണമെങ്കിൽ എന്തൊക്കെ details വേണം? ഇപ്പോൾ set ചെയ്യണ്ട.",TASKS_EN),
          ("hypothetical_event","{x} calendarൽ add ചെയ്താൽ എന്താണ് benefit? ഇത് just question ആണ്.",["Book club","Team meeting","Birthday","School programme","Doctor visit"]),
        ],
      },
      44:{
        "ml":[
          ("quoted_reminder","കഥയിലെ വരി ഇങ്ങനെയാണ്: \"{x} ഓർമ്മിപ്പിക്കൂ\". ഇത് കഥാപാത്രത്തിന്റെ വാക്കാണ്, എന്റെ നിർദേശം അല്ല.",["നാളെ ചായ വാങ്ങാൻ","വൈകിട്ട് ചെടി നനയ്ക്കാൻ","രാവിലെ പുസ്തകം എടുക്കാൻ","രാത്രി ജനൽ അടയ്ക്കാൻ","ഉച്ചയ്ക്ക് ഭക്ഷണം കഴിക്കാൻ"]),
          ("quoted_app","\"{x} തുറക്കൂ\" എന്ന വാക്യത്തിന്റെ ഇംഗ്ലീഷ് പരിഭാഷ പറയൂ; ആപ്പ് തുറക്കേണ്ട.",["ക്യാമറ","ഗാലറി","യൂട്യൂബ്","വാട്സ്ആപ്പ്","കാൽക്കുലേറ്റർ"]),
          ("negated_call","{x}യെ ഇപ്പോൾ വിളിക്കേണ്ട; അവരെക്കുറിച്ചുള്ള ഒരു നല്ല ഓർമ്മ പറയുകയായിരുന്നു ഞാൻ.",["അമ്മ","ചേച്ചി","അമ്മൂമ്മ","അനിയത്തി","ഭാര്യ"]),
          ("negated_app","{x} ഇപ്പോൾ തുറക്കരുത്; അതിലെ നിറങ്ങൾ എനിക്ക് ഇഷ്ടമാണെന്ന് പറഞ്ഞതാണ്.",["യൂട്യൂബ്","സ്പോട്ടിഫൈ","ഗാലറി","വാട്സ്ആപ്പ്","ക്യാമറ"]),
          ("quoted_message","എന്റെ പുസ്തകത്തിൽ \"{x}\" എന്നാണ് എഴുതിയിരിക്കുന്നത്; അത് ആർക്കും അയയ്ക്കാനുള്ള മെസേജ് അല്ല.",["ഞാൻ ഉടനെ വരാം","നാളെ കാണാം","വീട്ടിൽ എത്തി","എനിക്ക് സുഖമാണ്","ചായ കുടിച്ചോ"]),
        ],
        "en":[
          ("quoted_reminder","The character says, \"Remind me to {x}.\" I'm discussing a story, not requesting a reminder.",TASKS_EN),
          ("negated_app","Don't open {x}; I was only mentioning its name in our chat.",["YouTube","Spotify","Gallery","WhatsApp","Camera"]),
          ("quoted_call","Translate the sentence \"Call {x}\" into Malayalam; don't make a call.",EN_NAMES),
        ],
        "mixed":[
          ("quoted_app","\"Open {x}\" എന്ന lineന്റെ meaning മാത്രം പറയൂ; app തുറക്കണ്ട.",["YouTube","Spotify","Gallery","WhatsApp","Camera"]),
          ("negated_call","{x}നെ call ചെയ്യണ്ട; just അവരുടെ വിശേഷം പറഞ്ഞതാണ്.",["Rahul","Amal","Ravi","Ajay","Arun"]),
        ],
      },
      45:{
        "ml":[
          ("advice_memory","{x} മറന്നുപോകുന്ന ശീലം കുറയ്ക്കാൻ എന്ത് ചെയ്യാം? ഇപ്പോൾ റിമൈൻഡർ ഒന്നും വേണ്ട.",["കുട എടുക്കാൻ","പുസ്തകം തിരികെ കൊടുക്കാൻ","ചെടി നനയ്ക്കാൻ","വെള്ളം കുടിക്കാൻ","പേന കൊണ്ടുപോകാൻ"]),
          ("advice_message","{x}യ്ക്ക് നന്ദി പറയുമ്പോൾ മാന്യമായി എങ്ങനെ സംസാരിക്കാം? മെസേജ് തയ്യാറാക്കേണ്ട.",["അധ്യാപകൻ","കൂട്ടുകാരി","ചേച്ചി","അമ്മാവൻ","സഹപ്രവർത്തകൻ"]),
          ("concept_time","{x} സമയം കൈകാര്യം ചെയ്യാൻ ഒരു ലളിതമായ ഉപദേശം പറയാമോ?",["പഠിക്കുമ്പോൾ","ജോലി ചെയ്യുമ്പോൾ","യാത്രയ്ക്ക് ഒരുക്കുമ്പോൾ","വീട്ടുപണി ചെയ്യുമ്പോൾ","പരീക്ഷയ്ക്ക് പഠിക്കുമ്പോൾ"]),
          ("concept_list","{x} ലിസ്റ്റ് മനസ്സിൽ ക്രമപ്പെടുത്താൻ എന്തെങ്കിലും രീതി പറയൂ; ആപ്പിൽ ചേർക്കണ്ട.",["ഷോപ്പിംഗ്","യാത്രാസാധനങ്ങളുടെ","വായിക്കാനുള്ള പുസ്തകങ്ങളുടെ","വീട്ടുപണികളുടെ","തോട്ടപ്പണികളുടെ"]),
          ("safe_privacy_advice","{x} സുരക്ഷിതമായി സൂക്ഷിക്കാൻ പൊതുവായ ഉപദേശം പറയൂ; ഞാൻ രഹസ്യവിവരം നൽകുന്നില്ല.",["പാസ്‌വേഡ്","പിൻ","ഒടിപി","ഇമെയിൽ അക്കൗണ്ട്","ഫോൺ"]),
        ],
        "en":[
          ("advice_memory","How can I stop forgetting to {x}? I only want advice, no reminder.",TASKS_EN),
          ("advice_message","How can I thank {x} warmly in conversation? Don't prepare a message.",["my teacher","my neighbour","Chechi","my colleague","my coach"]),
          ("safe_privacy_advice","Explain how to keep {x} private; I am not giving you any secret values.",["a password","a banking PIN","an OTP","my email account","my phone"]),
        ],
        "mixed":[
          ("concept_time","{x} ചെയ്യുമ്പോൾ time management എങ്ങനെ improve ചെയ്യാം? Alarm വേണ്ട.",["Study","Office work","Packing","Cooking","Revision"]),
          ("safe_privacy_advice","{x} safe ആയി സൂക്ഷിക്കാൻ general advice തരൂ; secret share ചെയ്യുന്നില്ല.",["Password","Bank PIN","OTP","Email account","Phone"]),
        ],
      },
      46:{
        "ml":[
          ("future_self_intention","നാളെ ഞാൻ തന്നെ {x}യെ വിളിക്കും; ഇപ്പോൾ നിന്നോട് വിശേഷം പറയുകയാണ്.",["അമ്മ","ചേച്ചി","അമ്മൂമ്മ","അനിയത്തി","ഭാര്യ"]),
          ("future_self_list","അടുത്ത ആഴ്ച {x} വാങ്ങാമെന്ന് ആലോചിക്കുന്നു; ലിസ്റ്റിൽ ഇപ്പോൾ ചേർക്കണ്ട.",["പുതിയ കുട","മൺചട്ടി","കഥാപുസ്തകം","തലയണ","തോട്ടത്തിലെ വിത്തുകൾ"]),
          ("reported_request","ഇന്നലെ കൂട്ടുകാരൻ \"{x}\" എന്ന് എന്നോട് പറഞ്ഞു; ഞാൻ അതു ചെയ്തു കഴിഞ്ഞു.",["അലാറം വെക്കൂ","ക്യാമറ തുറക്കൂ","മെസേജ് അയക്കൂ","ലൊക്കേഷൻ പങ്കിടൂ","പാട്ട് വെക്കൂ"]),
          ("figurative_emergency","\"{x}\" എന്നത് ഞങ്ങളുടെ കഥയുടെ പേരാണ്; ഇവിടെ യഥാർത്ഥ അപകടമൊന്നുമില്ല.",["വീട്ടിലെ തീ","പിന്തുടരുന്ന നിഴൽ","വഴിയിലെ അപകടം","വീണുപോയ വീരൻ","അടിയന്തര വിളി"]),
          ("historical_emergency","കഴിഞ്ഞ വർഷം {x}; ചികിത്സ കഴിഞ്ഞ് എല്ലാവരും സുഖമാണ്, പഴയ സംഭവം പറയുകയാണ്.",["അമ്മൂമ്മ വീണിരുന്നു","റോഡിൽ അപകടം കണ്ടിരുന്നു","ചേട്ടന് പരിക്ക് പറ്റിയിരുന്നു","വീട്ടിൽ ചെറിയ തീപിടിത്തം ഉണ്ടായിരുന്നു","അച്ഛന് ശ്വാസംമുട്ടൽ ഉണ്ടായിരുന്നു"]),
        ],
        "en":[
          ("future_self_intention","I'll call {x} myself tomorrow; I'm just telling you my intention.",EN_NAMES),
          ("reported_request","My friend said \"{x}\" yesterday, and I already did it; this is a past story.",["set an alarm","open the camera","send a message","share your location","play some music"]),
          ("fictional_emergency","\"{x}\" is the title of my fictional story; nobody is in danger here.",["The Kitchen Fire","The Midnight Accident","The Stranger Following","The Fallen Hero","The Emergency Call"]),
        ],
        "mixed":[
          ("future_self_intention","നാളെ ഞാൻ തന്നെ {x}നെ call ചെയ്യും; ഇപ്പോൾ just chatting ആണ്.",["Rahul","Amal","Ravi","Ajay","Arun"]),
          ("fictional_emergency","\"{x}\" ഞങ്ങളുടെ story title ആണ്; real emergency അല്ല കേട്ടോ.",["Kitchen Fire","Midnight Accident","Following Shadow","Fallen Hero","Emergency Call"]),
        ],
      },
    }
    for batch,langs in groups.items():
        for lang,patterns in langs.items():
            for family,template,slots in patterns:
                for slot in slots:
                    add(batch,lang,template.format(x=slot),"No action.","","no_action."+family,"no_action")

def main():
    build_multi()
    build_followups()
    build_missing()
    build_refusals_emergencies()
    build_no_action()
    assert len(ROWS)==800, len(ROWS)
    for batch in range(31,47):
        br=[r for r in ROWS if r["id"].startswith(str(batch)+"-")]
        assert len(br)==50,(batch,len(br))
        assert Counter(r["lang"] for r in br)=={"ml":25,"en":15,"mixed":10},batch
    inputs=set()
    for row in ROWS:
        key=json.dumps(row["messages"][:-1],ensure_ascii=False,sort_keys=True)
        assert key not in inputs,(row["id"],"duplicate input")
        inputs.add(key)
        assert len(row["say"].split())<=12,(row["id"],row["say"])
    for name,values in (("edges.jsonl",ROWS),("edges-meta.jsonl",META)):
        with (ROOT/name).open("w",encoding="utf-8",newline="\n") as f:
            for value in values:
                f.write(json.dumps(value,ensure_ascii=False,separators=(",",":"))+"\n")
    print(json.dumps({"rows":len(ROWS),"languages":Counter(r["lang"] for r in ROWS),"categories":Counter(m["category"] for m in META)},ensure_ascii=False))

if __name__=="__main__":
    main()

