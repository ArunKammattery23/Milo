"""Synthetic Milo scheduling data, batches 1-16; requires native Malayalam review.
Post-action say strings assume the tool succeeded; the caller must gate playback.
Only Python standard library; deterministic regeneration in this directory.
"""
import calendar
import collections
import datetime as dt
import json
import re
from pathlib import Path

OUT = Path(__file__).resolve().parent
ROWS = []
META = []

def context(batch, i):
    day = dt.date(2026, 1, 1) + dt.timedelta(days=(batch * 43 + i * 11) % 730)
    return dt.datetime.combine(day, dt.time((batch * 3 + i * 7) % 24, (i * 13) % 60))

def emit(batch, i, lang, now, user, name, args, say, family, category):
    # Explicit 'today' examples must be future even on stricter app parsers.
    if str(args.get('time', '')).startswith('today '):
        now = now.replace(hour=0, minute=0)
    row = {'id': f'{batch}-{i+1:02d}', 'lang': lang,
           'now': now.strftime('%Y-%m-%dT%H:%M %A'),
           'messages': [{'role':'user', 'content':user},
                        {'role':'assistant', 'tool_calls':[{'name':name,'arguments':args}]}],
           'say':say}
    ROWS.append(row)
    META.append({'id':row['id'], 'family':family, 'category':category,
                 'provenance':'synthetic', 'malayalam_review_required':lang != 'en',
                 'say_requires_success':True})

def language(i):
    return 'ml' if i < 25 else ('en' if i < 40 else 'mixed')

def words(ml, en, mixed=None):
    return {'ml':ml,'en':en,'mixed':mixed if mixed is not None else ml}

def tm(ml, en, canon, mixed=None):
    return {'src':words(ml,en,mixed), 'time':canon, 'ml':ml, 'en':en}

def next_date(now, month, day, hour=9, minute=0):
    year = now.year
    while True:
        try:
            d = dt.datetime(year, month, day, hour, minute)
        except ValueError:
            year += 1
            continue
        if d > now:
            return d
        year += 1

MONTHS = ['', 'ജനുവരി','ഫെബ്രുവരി','മാർച്ച്','ഏപ്രിൽ','മേയ്','ജൂൺ',
          'ജൂലൈ','ഓഗസ്റ്റ്','സെപ്റ്റംബർ','ഒക്ടോബർ','നവംബർ','ഡിസംബർ']

def dated(now, month, day, hour=9, minute=0, no_hour=False):
    d = next_date(now, month, day, hour, minute)
    part = 'രാവിലെ' if hour < 12 else ('ഉച്ചയ്ക്ക്' if hour <=14 else ('വൈകിട്ട്' if hour <20 else 'രാത്രി'))
    hour12 = hour % 12 or 12
    clock = str(hour12) + (f':{minute:02d}' if minute else '')
    ml = f'{MONTHS[month]} {day}ന്' + ('' if no_hour else f' {part} {clock}ന്')
    en = f'on {calendar.month_name[month]} {day}' + ('' if no_hour else f' at {clock} {"am" if hour<12 else "pm"}')
    mixed = f'{calendar.month_name[month]} {day}ന്' + ('' if no_hour else f' {clock} {"am" if hour<12 else "pm"}ന്')
    return tm(ml,en,d.strftime('%Y-%m-%d %H:%M'),mixed)

CLOCKS = [
 tm('രാവിലെ 7 മണിക്ക്','at 7 am','7 am','രാവിലെ 7 amന്'),
 tm('വൈകിട്ട് 6 മണിക്ക്','at 6 pm','6 pm','വൈകിട്ട് 6 pmന്'),
 tm('ഉച്ചയ്ക്ക് 12 മണിക്ക്','at noon','12 pm','ഉച്ചയ്ക്ക് 12 pmന്'),
 tm('രാത്രി 12 മണിക്ക്','at midnight','12 am','രാത്രി 12 amന്'),
 tm('രാത്രി 2 മണിക്ക്','at 2 am','2 am','രാത്രി 2 amന്'),
 tm('പുലർച്ചെ 4 മണിക്ക്','at 4 am','4 am','പുലർച്ചെ 4 amന്'),
 tm('രാത്രി 9 മണിക്ക്','at 9 pm','9 pm','രാത്രി 9 pmന്'),
 tm('7 മണിക്ക്','at 7','7','7 മണിക്ക്'),
 tm('11 മണിക്ക്','at 11','11','11 മണിക്ക്'),
 tm('19:30ന്','at 19:30','19:30','19:30ന്'),
 tm('രാവിലെ ഏഴരയ്ക്ക്','at half past seven in the morning','7:30 am','രാവിലെ 7:30ന്'),
 tm('വൈകിട്ട് അഞ്ചരയ്ക്ക്','at 5:30 pm','5:30 pm','വൈകിട്ട് 5:30ന്'),
 tm('ഉച്ചയ്ക്ക് ഒന്നരയ്ക്ക്','at 1:30 pm','1:30 pm','ഉച്ചയ്ക്ക് 1:30ന്'),
 tm('രാത്രി പത്തരയ്ക്ക്','at 10:30 pm','10:30 pm','രാത്രി 10:30ന്'),
 tm('രാത്രി 3 മണിക്ക്','at 3 am','3 am','രാത്രി 3 amന്'),
 tm('06:45ന്','at 06:45','06:45','06:45ന്'),
 tm('ഉച്ചയ്ക്ക് 2 മണിക്ക്','at 2 pm','2 pm','ഉച്ചയ്ക്ക് 2 pmന്'),
 tm('രാവിലെ എട്ടരയ്ക്ക്','at 8:30 am','8:30 am','രാവിലെ 8:30ന്'),
 tm('വൈകുന്നേരം ഏഴ് മണിക്ക്','at seven in the evening','7 pm','വൈകുന്നേരം 7 pmന്'),
 tm('രാത്രി പതിനൊന്ന് മണിക്ക്','at eleven tonight','11 pm','രാത്രി 11 pmന്'),
]
RELATIVES = [
 tm('20 മിനിറ്റ് കഴിഞ്ഞ്','in 20 minutes','in 20 minutes','20 minutes കഴിഞ്ഞ്'),
 tm('ഒരു മണിക്കൂർ കഴിഞ്ഞ്','in one hour','in 1 hour','ഒരു hour കഴിഞ്ഞ്'),
 tm('അര മണിക്കൂർ കഴിഞ്ഞ്','in half an hour','in 30 minutes','അര hour കഴിഞ്ഞ്'),
 tm('രണ്ട് മണിക്കൂർ കഴിഞ്ഞ്','in two hours','in 2 hours','2 hours കഴിഞ്ഞ്'),
 tm('ഒന്നര മണിക്കൂർ കഴിഞ്ഞ്','in an hour and a half','in 1 hour 30 minutes','1 hour 30 minutes കഴിഞ്ഞ്'),
 tm('5 മിനിറ്റ് കഴിഞ്ഞ്','in five minutes','in 5 minutes','5 minutes കഴിഞ്ഞ്'),
 tm('45 മിനിറ്റ് കഴിഞ്ഞ്','in 45 minutes','in 45 minutes','45 minutes കഴിഞ്ഞ്'),
 tm('10 മിനിറ്റ് കഴിഞ്ഞ്','in ten minutes','in 10 minutes','10 minutes കഴിഞ്ഞ്'),
 tm('മൂന്ന് മണിക്കൂർ കഴിഞ്ഞ്','in three hours','in 3 hours','3 hours കഴിഞ്ഞ്'),
 tm('ഒരു മണിക്കൂറും 15 മിനിറ്റും കഴിഞ്ഞ്','in one hour and 15 minutes','in 1 hour 15 minutes','1 hour 15 minutes കഴിഞ്ഞ്'),
]
DAYS = [
 tm('നാളെ രാവിലെ','tomorrow morning','tomorrow 9 am','നാളെ morning'),
 tm('ഇന്ന് വൈകിട്ട്','this evening','today 6 pm','ഇന്ന് evening'),
 tm('മറ്റന്നാൾ രാവിലെ 10 മണിക്ക്','the day after tomorrow at 10 am','day after tomorrow 10 am','മറ്റന്നാൾ 10 amന്'),
 tm('നാളെ ഉച്ചയ്ക്ക്','tomorrow afternoon','tomorrow 2 pm','നാളെ afternoon'),
 tm('നാളെ രാത്രി','tomorrow night','tomorrow 9 pm','നാളെ night'),
 tm('തിങ്കളാഴ്ച വൈകിട്ട് 6 മണിക്ക്','on Monday at 6 pm','monday 6 pm','Monday വൈകിട്ട് 6ന്'),
 tm('ചൊവ്വാഴ്ച രാവിലെ 8 മണിക്ക്','on Tuesday at 8 am','tuesday 8 am','Tuesday രാവിലെ 8ന്'),
 tm('ബുധനാഴ്ച ഉച്ചയ്ക്ക് 2 മണിക്ക്','on Wednesday at 2 pm','wednesday 2 pm','Wednesday ഉച്ചയ്ക്ക് 2ന്'),
 tm('വ്യാഴാഴ്ച വൈകിട്ട് 5 മണിക്ക്','on Thursday at 5 pm','thursday 5 pm','Thursday വൈകിട്ട് 5ന്'),
 tm('വെള്ളിയാഴ്ച രാവിലെ 11 മണിക്ക്','on Friday at 11 am','friday 11 am','Friday രാവിലെ 11ന്'),
 tm('ശനിയാഴ്ച രാവിലെ 9 മണിക്ക്','on Saturday at 9 am','saturday 9 am','Saturday രാവിലെ 9ന്'),
 tm('ഞായറാഴ്ച വൈകിട്ട് 4 മണിക്ക്','on Sunday at 4 pm','sunday 4 pm','Sunday വൈകിട്ട് 4ന്'),
 tm('ഇന്ന് രാത്രി 8 മണിക്ക്','today at 8 pm','today 8 pm','ഇന്ന് 8 pmന്'),
 tm('നാളെ രാവിലെ ആറരയ്ക്ക്','tomorrow at 6:30 am','tomorrow 6:30 am','നാളെ 6:30 amന്'),
 tm('മറ്റന്നാൾ വൈകിട്ട്','the day after tomorrow in the evening','day after tomorrow 6 pm','മറ്റന്നാൾ evening'),
]

TASKS = [
 words('അമ്മയെ വിളിക്കാൻ','call Amma','Ammaയെ call ചെയ്യാൻ'),
 words('മരുന്ന് കഴിക്കാൻ','take my medicine','medicine കഴിക്കാൻ'),
 words('ചെടികൾക്ക് വെള്ളം ഒഴിക്കാൻ','water the plants','plantsന് വെള്ളം ഒഴിക്കാൻ'),
 words('വൈദ്യുതി ബിൽ അടയ്ക്കാൻ','pay the electricity bill','electricity bill അടയ്ക്കാൻ'),
 words('പുസ്തകം തിരികെ കൊടുക്കാൻ','return the library book','library book തിരികെ കൊടുക്കാൻ'),
 words('സ്കൂൾ ബാഗ് ഒരുക്കാൻ','pack the school bag','school bag ഒരുക്കാൻ'),
 words('ഡോക്ടറെ വിളിക്കാൻ','call the doctor','doctorനെ call ചെയ്യാൻ'),
 words('പാൽ വാങ്ങാൻ','buy milk','milk വാങ്ങാൻ'),
 words('വസ്ത്രങ്ങൾ ഉണക്കാൻ ഇടാൻ','hang the clothes to dry','clothes ഉണക്കാൻ ഇടാൻ'),
 words('വ്യായാമം ചെയ്യാൻ','exercise','exercise ചെയ്യാൻ'),
 words('ചേച്ചിക്ക് ഫോട്ടോ അയക്കാൻ','send Chechi the photo','Chechiക്ക് photo അയക്കാൻ'),
 words('അച്ഛനെ കൂട്ടിക്കൊണ്ടുവരാൻ','pick up Achan','Achanനെ pick up ചെയ്യാൻ'),
 words('വെള്ളം കുടിക്കാൻ','drink water','water കുടിക്കാൻ'),
 words('കണ്ണട എടുക്കാൻ','take my glasses','glasses എടുക്കാൻ'),
 words('റിപ്പോർട്ട് അയക്കാൻ','send the report','report അയക്കാൻ'),
 words('നായയ്ക്ക് ഭക്ഷണം കൊടുക്കാൻ','feed the dog','dogന് food കൊടുക്കാൻ'),
 words('മാലിന്യം പുറത്തുവെക്കാൻ','take out the rubbish','rubbish പുറത്തുവെക്കാൻ'),
 words('ഫോൺ ചാർജ് ചെയ്യാൻ','charge the phone','phone charge ചെയ്യാൻ'),
 words('തൈര് ഫ്രിഡ്ജിൽ വെക്കാൻ','put the curd in the fridge','curd fridgeൽ വെക്കാൻ'),
 words('ടിക്കറ്റ് ബുക്ക് ചെയ്യാൻ','book the ticket','ticket book ചെയ്യാൻ'),
 words('അമ്മൂമ്മയെ കാണാൻ പോകാൻ','visit grandma','grandmaയെ കാണാൻ പോകാൻ'),
 words('താക്കോൽ കൊണ്ടുപോകാൻ','take the keys','keys കൊണ്ടുപോകാൻ'),
 words('ഹോംവർക്ക് ചെയ്യാൻ','do my homework','homework ചെയ്യാൻ'),
 words('പിറന്നാൾ സമ്മാനം വാങ്ങാൻ','buy a birthday gift','birthday gift വാങ്ങാൻ'),
 words('കുട കൊണ്ടുപോകാൻ','take an umbrella','umbrella കൊണ്ടുപോകാൻ'),
 words('അപേക്ഷ സമർപ്പിക്കാൻ','submit the application','application submit ചെയ്യാൻ'),
 words('ബസ് പാസ് പുതുക്കാൻ','renew the bus pass','bus pass renew ചെയ്യാൻ'),
 words('കേക്ക് വാങ്ങാൻ','collect the cake','cake വാങ്ങാൻ'),
 words('അരി കഴുകാൻ','wash the rice','rice കഴുകാൻ'),
 words('പൂച്ചയ്ക്ക് വെള്ളം വെക്കാൻ','leave water for the cat','catന് water വെക്കാൻ'),
 words('ചെരുപ്പ് നന്നാക്കാൻ കൊടുക്കാൻ','take the sandals for repair','sandals repairന് കൊടുക്കാൻ'),
]

REMINDER_PATTERNS = [
 words('{t} {task} ഓർമ്മിപ്പിക്കണം.', 'Remind me to {task} {t}.', '{t} {task} remind ചെയ്യണം.'),
 words('{task} മറക്കാതിരിക്കാൻ {t} ഒന്ന് ഓർമ്മിപ്പിക്കാമോ?', 'Could you remind me {t} to {task}?', '{task} മറക്കാതിരിക്കാൻ {t} reminder ഇടാമോ?'),
 words('മിലോ, {task}; {t} എന്നെ ഓർമ്മിപ്പിക്കണേ.', 'Milo, I need to {task}; remind me {t}.', 'Milo, {task}; {t} ഒന്ന് remind ചെയ്യണേ.'),
 words('{t} ഒരു റിമൈൻഡർ വേണം, {task}.', 'Set a reminder {t}: {task}.', '{t} reminder വേണം, {task}.'),
 words('എനിക്ക് {task} ഉണ്ട്. {t} പറഞ്ഞുതരണേ.', 'I have to {task}. Give me a reminder {t}.', 'എനിക്ക് {task} ഉണ്ട്. {t} remind ചെയ്യണേ.'),
 words('മോളേ, {t} {task} ഒന്ന് ഓർമ്മിപ്പിച്ചുതരാമോ?', 'Please nudge me {t} to {task}.', 'മോളേ, {t} {task} reminder വെക്കാമോ?'),
 words('{task} വേണ്ടി {t} റിമൈൻഡർ വെക്കൂ.', 'A reminder to {task} {t}, please.', '{task} വേണ്ടി {t} reminder set ചെയ്യൂ.'),
 words('ഞാൻ മറന്നുപോകും, {task} {t} ഓർമ്മിപ്പിക്കണേ.', 'I might forget to {task}, so remind me {t}.', 'ഞാൻ മറക്കും, {task} {t} remind ചെയ്യണേ.'),
 words('{t} എന്നെ ഓർമ്മിപ്പിച്ചാൽ മതി, {task}.', 'Remind me {t}; the task is to {task}.', '{t} reminder മതി, {task}.'),
 words('എന്റെ മിലോ, {t} {task} പറഞ്ഞുതരണേ.', 'Hey Milo, add a reminder to {task} {t}.', 'എന്റെ Milo, {t} {task} remind ചെയ്യണേ.'),
 words('{task} കാര്യം {t} ഓർമ്മിപ്പിച്ചുതരണം കേട്ടോ.', 'Can you remind me about this {t}: {task}?', '{task} കാര്യം {t} remind ചെയ്തുതരണം കേട്ടോ.'),
 words('{t} {task} എന്നൊരു റിമൈൻഡർ ചേർക്കണേ.', 'Put down a reminder for me to {task} {t}.', '{t} {task} എന്നൊരു reminder ചേർക്കണേ.'),
]

def time_for(batch, j, now):
    if batch == 1: return CLOCKS[j % 10]
    if batch == 2: return CLOCKS[(10+j) % len(CLOCKS)]
    if batch == 3: return RELATIVES[j % len(RELATIVES)]
    if batch == 4: return DAYS[j % len(DAYS)]
    if batch == 5:
        month, day = [(1,12),(2,28),(3,12),(4,14),(5,1),(6,8),(7,20),(8,16),(9,5),(10,25),(11,14),(12,31)][j%12]
        h,m = [(9,0),(18,0),(14,30),(7,15)][(j//12)%4]
        return dated(now,month,day,h,m,no_hour=j%4==0)
    # Boundary dates include leap day, year rollover, noon, midnight and 24-hour clocks.
    if j % 5 == 0: return dated(now,2,29,9,0,no_hour=True)
    if j % 5 == 1: return dated(now,1,1,9,0,no_hour=True)
    if j % 5 == 2: return tm('23:50ന്','at 23:50','23:50','23:50ന്')
    if j % 5 == 3: return CLOCKS[(j+3)%len(CLOCKS)]
    return DAYS[j%len(DAYS)]

def reminder(batch, i, lang, now):
    j = (batch-1)*50+i
    t = time_for(batch,i,now)
    task = TASKS[(j*7+batch)%len(TASKS)][lang]
    pat = (j*5+batch) % len(REMINDER_PATTERNS)
    user = REMINDER_PATTERNS[pat][lang].format(t=t['src'][lang],task=task)
    say = f"I'll remind you {t['en']}." if lang=='en' else f"ശരി, {t['ml']} ഓർമ്മിപ്പിക്കാം ട്ടോ."
    emit(batch,i,lang,now,user,'set_reminder',{'task':task,'time':t['time']},say,
         f'reminder-single-form-{pat:02d}',f'reminder-single-{batch}')

REPEAT_SCENARIOS = [
 # task, time spec, normalized repeat, recurrence phrasing.
 (words('മരുന്ന് കഴിക്കാൻ','take my medicine','medicine കഴിക്കാൻ'), CLOCKS[6], 'daily',words('എല്ലാ ദിവസവും','every day','എല്ലാ ദിവസവും')),
 (words('വെള്ളം കുടിക്കാൻ','drink water','water കുടിക്കാൻ'), CLOCKS[0], 'daily',words('ദിവസവും','daily','daily')),
 (words('ഓഫീസിലേക്ക് ഇറങ്ങാൻ','leave for the office','officeലേക്ക് ഇറങ്ങാൻ'),CLOCKS[17],'weekdays',words('തിങ്കൾ മുതൽ വെള്ളി വരെ','Monday to Friday','Monday മുതൽ Friday വരെ')),
 (words('വ്യായാമം ചെയ്യാൻ','exercise','exercise ചെയ്യാൻ'),CLOCKS[5],'weekdays',words('എല്ലാ തിങ്കൾ മുതൽ വെള്ളി വരെയും','on weekdays','weekdaysൽ')),
 (words('അമ്മൂമ്മയെ വിളിക്കാൻ','call grandma','grandmaയെ call ചെയ്യാൻ'),DAYS[11],'weekly',words('എല്ലാ ആഴ്ചയും','every week','every week')),
 (words('ചെടികൾക്ക് വളം ഇടാൻ','fertilise the plants','plantsന് വളം ഇടാൻ'),DAYS[10],'weekly',words('ആഴ്ചതോറും','weekly','weekly')),
 (words('വാടക അടയ്ക്കാൻ','pay the rent','rent അടയ്ക്കാൻ'),DAYS[0],'monthly',words('മാസംതോറും','every month','monthly')),
 (words('ഫിൽട്ടർ വൃത്തിയാക്കാൻ','clean the filter','filter clean ചെയ്യാൻ'),DAYS[3],'monthly',words('എല്ലാ മാസവും','monthly','എല്ലാ മാസവും')),
 (words('അനുവിന് പിറന്നാൾ ആശംസിക്കാൻ','wish Anu a happy birthday','Anuന് birthday wish ചെയ്യാൻ'),None,'yearly',words('എല്ലാ വർഷവും','every year','yearly')),
 (words('വിവാഹവാർഷികം ഓർക്കാൻ','remember our wedding anniversary','wedding anniversary ഓർക്കാൻ'),None,'yearly',words('വർഷംതോറും','yearly','every year')),
]
REPEAT_PATTERNS = [
 words('{t} മുതൽ {r} {task} ഓർമ്മിപ്പിക്കണം{early}.','Starting {t}, remind me to {task} {r}{early}.','{t} മുതൽ {r} {task} remind ചെയ്യണം{early}.'),
 words('{task} വേണ്ടി {r} വരുന്ന റിമൈൻഡർ വേണം, {t} മുതൽ{early}.','Set a {r} reminder to {task}, starting {t}{early}.','{task} വേണ്ടി {r} reminder വേണം, {t} മുതൽ{early}.'),
 words('{r} {task} മറക്കുന്നു; {t} മുതൽ ഓർമ്മിപ്പിക്കാമോ{early}?','I forget to {task}; remind me {r} starting {t}{early}.','{r} {task} മറക്കുന്നു; {t} മുതൽ remind ചെയ്യാമോ{early}?'),
 words('മിലോ, {t} മുതൽ {task} ഓർമ്മിപ്പിക്കണേ. {r} വേണം{early}.','Milo, remind me to {task} from {t}; repeat it {r}{early}.','Milo, {t} മുതൽ {task} remind ചെയ്യണേ; {r} വേണം{early}.'),
 words('{t} മുതൽ {r} റിമൈൻഡർ ഇടൂ: {task}{early}.','Please schedule a repeating reminder: {task}, {r}, starting {t}{early}.','{t} മുതൽ {r} reminder ഇടൂ: {task}{early}.'),
 words('മോളേ, {task} ഓർമ്മിപ്പിച്ചുതരണം; {t} മുതൽ {r}{early}.','Could you nudge me to {task} {r}, beginning {t}{early}?','മോളേ, {task} remind ചെയ്തുതരണം; {t} മുതൽ {r}{early}.'),
 words('{task} എന്ന കാര്യം {r} ഓർമ്മിപ്പിക്കൂ, ആദ്യത്തേത് {t}{early}.','Add a recurring reminder to {task}, {r}; first one {t}{early}.','{task} എന്ന കാര്യം {r} remind ചെയ്യൂ, first reminder {t}{early}.'),
 words('{r} {task} എന്ന് എന്നോട് പറയണം, {t} തുടങ്ങാം{early}.','I want a reminder to {task} {r}, starting {t}{early}.','{r} {task} എന്ന് remind ചെയ്യണം, {t} തുടങ്ങാം{early}.'),
]
EARLY_PATTERNS = [
 words('{t} {task} ഓർമ്മിപ്പിക്കണം; {early} മിനിറ്റ് മുമ്പ് അറിയിക്കണേ.','Remind me to {task} {t}, with a warning {early} minutes before.','{t} {task} remind ചെയ്യണം; {early} minutes മുമ്പ് അറിയിക്കണേ.'),
 words('{task} വേണ്ടി {t} റിമൈൻഡർ ഇടൂ, {early} മിനിറ്റ് നേരത്തേ പറഞ്ഞുതരണം.','Set my reminder to {task} {t}; alert me {early} minutes early.','{task} വേണ്ടി {t} reminder ഇടൂ; {early} minutes നേരത്തേ alert വേണം.'),
 words('മിലോ, {t} {task} ഉണ്ട്; {early} മിനിറ്റ് മുമ്പ് ഓർമ്മിപ്പിക്കാമോ?','I need to {task} {t}; can you remind me {early} minutes beforehand?','Milo, {t} {task} ഉണ്ട്; {early} minutes മുമ്പ് remind ചെയ്യാമോ?'),
 words('{early} മിനിറ്റ് മുമ്പുള്ള അറിയിപ്പോടെ {t} {task} റിമൈൻഡർ വെക്കൂ.','Add a reminder to {task} {t}, including a {early}-minute early alert.','{early} minutes മുമ്പുള്ള alert കൂടി {t} {task} reminder വെക്കൂ.'),
]

def repeat_reminder(batch,i,lang,now):
    if batch == 8 and i % 3 == 0:
        t = DAYS[(i+3)%len(DAYS)]
        task = TASKS[(i*3+2)%len(TASKS)][lang]
        before = [5,10,15,20,30,45,60][(i//3)%7]
        pat = (i//3)%len(EARLY_PATTERNS)
        user = EARLY_PATTERNS[pat][lang].format(t=t['src'][lang],task=task,early=before)
        args = {'task':task,'time':t['time'],'minutes_before':before}
        say = f"Reminder: {t['en']}, {before} minutes early." if lang=='en' else f"ശരി, {t['ml']}; {before} മിനിറ്റ് മുമ്പ് ഓർമ്മിപ്പിക്കാം."
        return emit(batch,i,lang,now,user,'set_reminder',args,say,f'reminder-early-form-{pat:02d}','reminder-early')
    n = (i+batch*7)%len(REPEAT_SCENARIOS)
    task,t,repeat,recurrence = REPEAT_SCENARIOS[n]
    if t is None:
        t = dated(now,3 if n==8 else 11,12 if n==8 else 18,9,0,no_hour=True)
    pat = (i//2+batch)%len(REPEAT_PATTERNS)
    before = [5,10,15,30,60][i%5] if batch==8 else None
    early = words(f', {before} മിനിറ്റ് മുമ്പ് അറിയിക്കണേ', f', with a {before}-minute early warning',f', {before} minutes മുമ്പ് alert വേണം')[lang] if before else ''
    user = REPEAT_PATTERNS[pat][lang].format(t=t['src'][lang],task=task[lang],r=recurrence[lang],early=early)
    args = {'task':task[lang],'time':t['time'],'repeat':repeat}
    if before: args['minutes_before'] = before
    # A short spoken schedule; date/time and recurrence retained, early warning acknowledged.
    if lang=='en':
        compact = t['time']
        say = f"Reminder: {compact}, {repeat}" + (f", {before} minutes early." if before else '.')
    else:
        rec = {'daily':'ദിവസവും','weekdays':'തിങ്കൾമുതൽവെള്ളിവരെ','weekly':'ആഴ്ചതോറും','monthly':'മാസംതോറും','yearly':'വർഷംതോറും'}[repeat]
        say = f"ശരി, {t['ml']} {rec}" + (f'; {before} മിനിറ്റ് മുമ്പ് ഓർമ്മിപ്പിക്കാം.' if before else ' ഓർമ്മിപ്പിക്കാം.')
    emit(batch,i,lang,now,user,'set_reminder',args,say,f'reminder-repeat-form-{pat:02d}','reminder-repeating-early' if before else 'reminder-repeating')

ALARM_PATTERNS = [
 words('{t} അലാറം വെക്കാമോ?', 'Set an alarm {t}.', '{t} alarm set ചെയ്യാമോ?'),
 words('എന്നെ {t} വിളിച്ചുണർത്തണേ.', 'Wake me up {t}, please.', 'എന്നെ {t} wake up ചെയ്യണേ.'),
 words('മിലോ, {t} എഴുന്നേൽക്കണം; അലാറം ഇടൂ.', 'Milo, I need to get up {t}; set an alarm.', 'Milo, {t} എഴുന്നേൽക്കണം; alarm ഇടൂ.'),
 words('{t} ഒരു അലാറം വേണം കേട്ടോ.', 'An alarm {t}, please.', '{t} ഒരു alarm വേണം കേട്ടോ.'),
 words('അലാറം ഒന്ന് വെച്ചുതരൂ, {t}.', 'Could you put an alarm on {t}?', 'Alarm ഒന്ന് set ചെയ്തുതരൂ, {t}.'),
 words('ഉറങ്ങിപ്പോയാലും {t} എഴുന്നേൽക്കണം, അലാറം വെക്കണേ.', 'Make sure an alarm rings {t} to wake me.', 'ഉറങ്ങിപ്പോയാലും {t} എഴുന്നേൽക്കണം, alarm വെക്കണേ.'),
 words('{t} എന്നെ ഉണർത്താൻ അലാറം വെക്കൂ.', 'Add a wake-up alarm {t}.', '{t} എന്നെ wake up ചെയ്യാൻ alarm വെക്കൂ.'),
 words('മോളേ, {t} വിളിച്ചുണർത്താൻ ഒരു അലാറം ഇടണേ.', 'Please arrange a wake-up alarm {t}, Milo.', 'മോളേ, {t} wake-up alarm ഇടണേ.'),
]
TIMER_PATTERNS = [
 words('{d} ടൈമർ വെക്കൂ.', 'Set a timer for {d}.', '{d} timer set ചെയ്യൂ.'),
 words('ഒരു ടൈമർ തുടങ്ങാമോ, {d}?', 'Start a {d} countdown, please.', 'ഒരു timer തുടങ്ങാമോ, {d}?'),
 words('{d} കഴിഞ്ഞാൽ ടൈമർ അടിക്കണം.', 'Have a timer ring after {d}.', '{d} കഴിഞ്ഞാൽ timer അടിക്കണം.'),
 words('മിലോ, {d} നേരത്തേക്ക് ടൈമർ ഇടണേ.', 'Milo, give me a timer lasting {d}.', 'Milo, {d} നേരത്തേക്ക് timer ഇടണേ.'),
 words('ഇപ്പോൾ {d} ടൈമർ തുടങ്ങൂ കേട്ടോ.', 'Please start counting down {d} now.', 'ഇപ്പോൾ {d} timer start ചെയ്യൂ കേട്ടോ.'),
 words('ടൈമറിൽ {d} സെറ്റ് ചെയ്തുതരാമോ?', 'Can you set the countdown to {d}?', 'Timerൽ {d} set ചെയ്തുതരാമോ?'),
]
DURATIONS = [
 words('5 മിനിറ്റ്','5 minutes','5 minutes'), words('10 മിനിറ്റ്','10 minutes','10 minutes'),
 words('അര മണിക്കൂർ','30 minutes','30 minutes'),words('ഒരു മണിക്കൂർ','1 hour','1 hour'),
 words('ഒന്നര മണിക്കൂർ','1 hour 30 minutes','1 hour 30 minutes'),words('2 മിനിറ്റ്','2 minutes','2 minutes'),
 words('45 സെക്കൻഡ്','45 seconds','45 seconds'),words('15 മിനിറ്റ്','15 minutes','15 minutes'),
 words('രണ്ട് മണിക്കൂർ','2 hours','2 hours'),words('25 മിനിറ്റ്','25 minutes','25 minutes'),
 words('90 സെക്കൻഡ്','90 seconds','90 seconds'),words('12 മിനിറ്റ്','12 minutes','12 minutes'),
]
LABELS = [words('ചോറ്','rice','rice'),words('പഠനം','study','study'),words('ഉറക്കം','nap','nap'),
          words('ചായ','tea','tea'),words('കേക്ക്','cake','cake'),words('വ്യായാമം','exercise','exercise'),
          words('ഇഡ്ഡലി','idli','idli'),words('ഇടവേള','break','break'),words('ബസ്','bus','bus'),
          words('ഓഫീസ്','office','office'),words('ട്രെയിൻ','train','train')]

def alarm_timer(batch,i,lang,now):
    is_timer = (i+batch)%5 < 2
    j = i + (batch-9)*50
    if is_timer:
        d = DURATIONS[(j*7+batch)%len(DURATIONS)]
        pat = (j//2)%len(TIMER_PATTERNS)
        user = TIMER_PATTERNS[pat][lang].format(d=d[lang])
        args = {'duration':d['en']}
        family = f'timer-form-{pat:02d}'
        say = f"Your {d['en']} timer is running." if lang=='en' else f"ശരി, {d['ml']} ടൈമർ തുടങ്ങി."
        category = 'timer'
    else:
        t = DAYS[(j*3+1)%len(DAYS)] if j%2 else CLOCKS[(j+5)%len(CLOCKS)]
        pat = (j//2)%len(ALARM_PATTERNS)
        user = ALARM_PATTERNS[pat][lang].format(t=t['src'][lang])
        args = {'time':t['time']}
        family = f'alarm-form-{pat:02d}'
        say = f"Alarm set {t['en']}." if lang=='en' else f"ശരി, {t['ml']} അലാറം വെച്ചു."
        category = 'alarm'
    if i%3:
        label = LABELS[(j*3+batch)%len(LABELS)][lang]
        user += words(f' പേര് {label} എന്ന് വെക്കണേ.',f' Label it {label}.',f' Label {label} എന്ന് വെക്കണേ.')[lang]
        args['label'] = label
    emit(batch,i,lang,now,user,'set_alarm',args,say,family,category)

EVENTS = [
 (words('ഡോക്ടറെ കാണൽ','doctor appointment','doctor appointment'),words('മെഡിക്കൽ കോളേജ്','Medical College','Medical College')),
 (words('രക്ഷാകർതൃയോഗം','parents meeting','parents meeting'),words('സ്കൂൾ','school','school')),
 (words('ടീം മീറ്റിംഗ്','team meeting','team meeting'),words('ഓഫീസ്','office','office')),
 (words('വിവാഹം','wedding','wedding'),words('തൃശ്ശൂർ','Thrissur','Thrissur')),
 (words('കൊച്ചി യാത്ര','Kochi trip','Kochi trip'),words('കൊച്ചി','Kochi','Kochi')),
 (words('ദന്തപരിശോധന','dental check-up','dental check-up'),words('മെഡിക്കൽ കോളേജ്','Medical College','Medical College')),
 (words('പുസ്തകമേള','book fair','book fair'),words('കോഴിക്കോട്','Kozhikode','Kozhikode')),
 (words('കുടുംബസംഗമം','family gathering','family gathering'),words('കോട്ടയം','Kottayam','Kottayam')),
 (words('പിറന്നാൾ ആഘോഷം','birthday party','birthday party'),words('ലുലു മാൾ','Lulu Mall','Lulu Mall')),
 (words('കലോത്സവം','school arts festival','school arts festival'),words('സ്കൂൾ','school','school')),
 (words('കണ്ണുപരിശോധന','eye check-up','eye check-up'),words('ആശുപത്രി','hospital','hospital')),
 (words('പരിശീലന ക്ലാസ്','training class','training class'),words('ഓഫീസ്','office','office')),
 (words('ഉച്ചഭക്ഷണം','lunch','lunch'),words('ലുലു മാൾ','Lulu Mall','Lulu Mall')),
 (words('ഇന്റർവ്യൂ','interview','interview'),words('കാക്കനാട്','Kakkanad','Kakkanad')),
 (words('യോഗ ക്ലാസ്','yoga class','yoga class'),words('കോട്ടയം','Kottayam','Kottayam')),
 (words('ചിത്രപ്രദർശനം','art exhibition','art exhibition'),words('ഫോർട്ട് കൊച്ചി','Fort Kochi','Fort Kochi')),
 (words('പൂർവവിദ്യാർഥി സംഗമം','alumni meetup','alumni meetup'),words('തൃശ്ശൂർ','Thrissur','Thrissur')),
 (words('സംഗീത ക്ലാസ്','music lesson','music lesson'),words('തിരുവനന്തപുരം','Thiruvananthapuram','Thiruvananthapuram')),
 (words('രക്തപരിശോധന','blood test','blood test'),words('മെഡിക്കൽ കോളേജ്','Medical College','Medical College')),
 (words('ക്രിക്കറ്റ് പരിശീലനം','cricket practice','cricket practice'),words('മൈതാനം','playground','playground')),
 (words('നാടകപരിശീലനം','drama rehearsal','drama rehearsal'),words('സ്കൂൾ','school','school')),
 (words('ചായക്കൂടിക്കാഴ്ച','tea meetup','tea meetup'),words('കോഴിക്കോട്','Kozhikode','Kozhikode')),
 (words('ബാങ്ക് കൂടിക്കാഴ്ച','bank appointment','bank appointment'),words('കൊച്ചി','Kochi','Kochi')),
]
EVENT_PATTERNS = [
 words('{t} {title} കലണ്ടറിൽ ചേർക്കൂ{extra}.','Add {title} to my calendar {t}{extra}.','{t} {title} calendarൽ ചേർക്കൂ{extra}.'),
 words('കലണ്ടറിൽ {title} കുറിച്ചുവെക്കണേ, {t}{extra}.','Put {title} on the calendar {t}{extra}.','Calendarൽ {title} കുറിച്ചുവെക്കണേ, {t}{extra}.'),
 words('മിലോ, {t} {title} ഉണ്ട്. കലണ്ടറിൽ ഇടൂ{extra}.','Milo, I have {title} {t}. Add it to my calendar{extra}.','Milo, {t} {title} ഉണ്ട്. Calendarൽ ഇടൂ{extra}.'),
 words('{title} എന്നൊരു പരിപാടി ചേർക്കാമോ? സമയം {t}{extra}.','Create a calendar event called {title} {t}{extra}.','{title} എന്നൊരു event ചേർക്കാമോ? സമയം {t}{extra}.'),
 words('{t} കലണ്ടറിൽ {title} കാണണം, ഒന്ന് ചേർക്കൂ{extra}.','Please schedule {title} {t} in my calendar{extra}.','{t} calendarൽ {title} വേണം, ഒന്ന് add ചെയ്യൂ{extra}.'),
 words('എന്റെ കലണ്ടറിൽ {title} ചേർത്തുതരൂ; {t}{extra}.','Could you enter {title} {t} in my calendar{extra}?','എന്റെ calendarൽ {title} add ചെയ്തുതരൂ; {t}{extra}.'),
 words('{title} നഷ്ടപ്പെടരുത്; {t} കലണ്ടറിൽ ചേർക്കണേ{extra}.','Save a calendar entry for {title} {t}{extra}.','{title} miss ആവരുത്; {t} calendarൽ add ചെയ്യണേ{extra}.'),
 words('മോളേ, {title} കലണ്ടറിൽ ഇടാമോ, {t}{extra}?','Can you book {title} into my calendar {t}{extra}?','മോളേ, {title} calendarൽ ഇടാമോ, {t}{extra}?'),
 words('ഒരു കലണ്ടർ പരിപാടി വേണം: {title}, {t}{extra}.','New calendar event: {title}, {t}{extra}.','ഒരു calendar event വേണം: {title}, {t}{extra}.'),
 words('{t} {title} എന്ന് കലണ്ടറിൽ രേഖപ്പെടുത്തൂ{extra}.','Record {title} {t} on my calendar{extra}.','{t} {title} എന്ന് calendarൽ record ചെയ്യൂ{extra}.'),
 words('കലണ്ടർ തുറന്ന് {title} ചേർക്കണം; {t}{extra}.','Make a calendar entry: {title} {t}{extra}.','Calendarൽ {title} add ചെയ്യണം; {t}{extra}.'),
]
PEOPLE = [words('അനു','Anu','Anu'),words('രാഹുൽ','Rahul','Rahul'),words('മീന','Meena','Meena'),
          words('ദിയ','Diya','Diya'),words('ഹരി','Hari','Hari'),words('അമ്മ','Amma','Amma'),
          words('ചേച്ചി','Chechi','Chechi'),words('അച്ഛൻ','Achan','Achan')]

def event(batch,i,lang,now):
    j = (batch-11)*50+i
    title,place = EVENTS[(j*7+batch)%len(EVENTS)]
    t = time_for(1+j%6,j,now)
    args = {'title':title[lang],'time':t['time']}
    extra = ''
    if i%4 != 0:
        args['place'] = place[lang]
        extra += words(f'; സ്ഥലം {place[lang]}',f'; venue: {place[lang]}',f'; place {place[lang]}')[lang]
    if i%3 != 0:
        names = [PEOPLE[(j+2)%len(PEOPLE)][lang]]
        if i%5 == 0: names.append(PEOPLE[(j+5)%len(PEOPLE)][lang])
        args['people'] = ', '.join(names)
        extra += words('; കൂടെ '+', '.join(names),'; with '+', '.join(names),'; കൂടെ '+', '.join(names))[lang]
    pat = (j*3+batch)%len(EVENT_PATTERNS)
    user = EVENT_PATTERNS[pat][lang].format(t=t['src'][lang],title=title[lang],extra=extra)
    say = f"Added to your calendar {t['en']}." if lang=='en' else f"ശരി, {t['ml']} കലണ്ടറിൽ ചേർത്തു."
    emit(batch,i,lang,now,user,'create_event',args,say,f'event-form-{pat:02d}','calendar-event')

# Each fact and query pair is authored. Nothing is actually saved by this generator.
NOTES = [
 (words('താക്കോൽ നീല ബാഗിൽ','The keys are in the blue bag','Keys നീല bagൽ'),words('താക്കോൽ എവിടെയാണ്','Where are my keys','Keys എവിടെയാണ്')),
 (words('വണ്ടി ബി2 നിലയിൽ പാർക്ക് ചെയ്തു','The car is parked on level B2','Car B2 levelൽ park ചെയ്തു'),words('വണ്ടി എവിടെയാണ് പാർക്ക് ചെയ്തത്','Where did I park the car','Car എവിടെയാണ് park ചെയ്തത്')),
 (words('അനുവിന്റെ പിറന്നാൾ മാർച്ച് 12','Anu’s birthday is March 12','Anuയുടെ birthday March 12'),words('അനുവിന്റെ പിറന്നാൾ എന്നാണ്','When is Anu’s birthday','Anuയുടെ birthday എന്നാണ്')),
 (words('വൈഫൈ പേര് മാമ്പഴം','The Wi-Fi name is MangoHouse','Wi-Fi name MangoHouse'),words('വൈഫൈ പേര് എന്താണ്','What is the Wi-Fi name','Wi-Fi name എന്താണ്')),
 (words('കണ്ണട മേശയുടെ വലത്തെ ഡ്രോയറിൽ','My glasses are in the right desk drawer','Glasses deskന്റെ right drawerൽ'),words('കണ്ണട എവിടെയാണ് വെച്ചത്','Where did I keep my glasses','Glasses എവിടെയാണ് വെച്ചത്')),
 (words('അമ്മയ്ക്ക് നീല നിറം ഇഷ്ടമാണ്','Amma likes blue','Ammaക്ക് blue ഇഷ്ടമാണ്'),words('അമ്മയ്ക്ക് ഇഷ്ടമുള്ള നിറം ഏതാണ്','Which colour does Amma like','Ammaക്ക് ഇഷ്ടമുള്ള colour ഏതാണ്')),
 (words('കുട ചെരുപ്പുറക്കരികിൽ','The umbrella is beside the shoe rack','Umbrella shoe rackനരികിൽ'),words('കുട എവിടെയാണ്','Where is the umbrella','Umbrella എവിടെയാണ്')),
 (words('ഡോക്ടറുടെ പേര് മീന','The doctor’s name is Meena','Doctorന്റെ പേര് Meena'),words('ഡോക്ടറുടെ പേര് എന്താണ്','What is the doctor’s name','Doctorന്റെ പേര് എന്താണ്')),
 (words('ലൈബ്രറി പുസ്തകം മഞ്ഞ അലമാരയിൽ','The library book is in the yellow cupboard','Library book മഞ്ഞ cupboardൽ'),words('ലൈബ്രറി പുസ്തകം എവിടെയാണ്','Where is the library book','Library book എവിടെയാണ്')),
 (words('പൂച്ചയ്ക്ക് ഇഷ്ടം മീനാണ്','The cat likes fish','Catന് fish ഇഷ്ടമാണ്'),words('പൂച്ചയ്ക്ക് ഇഷ്ടമുള്ള ഭക്ഷണം എന്താണ്','What food does the cat like','Catന് ഇഷ്ടമുള്ള food എന്താണ്')),
 (words('അച്ഛന്റെ ചെരുപ്പ് വലുപ്പം എട്ട്','Achan’s shoe size is eight','Achanന്റെ shoe size eight'),words('അച്ഛന്റെ ചെരുപ്പ് വലുപ്പം എത്രയാണ്','What is Achan’s shoe size','Achanന്റെ shoe size എത്രയാണ്')),
 (words('കാറിന്റെ സർവീസ് കേന്ദ്രം കാക്കനാട്','The car service centre is in Kakkanad','Car service centre Kakkanadൽ'),words('കാറിന്റെ സർവീസ് കേന്ദ്രം എവിടെയാണ്','Where is the car service centre','Car service centre എവിടെയാണ്')),
 (words('വിത്തുകൾ അടുക്കളയിലെ ചെറിയ പെട്ടിയിൽ','The seeds are in the small kitchen box','Seeds kitchenലെ ചെറിയ boxൽ'),words('വിത്തുകൾ എവിടെയാണ് വെച്ചത്','Where did I put the seeds','Seeds എവിടെയാണ് വെച്ചത്')),
 (words('രാഹുലിന് ചായയിൽ പഞ്ചസാര വേണ്ട','Rahul takes tea without sugar','Rahulന് teaയിൽ sugar വേണ്ട'),words('രാഹുലിന് ചായ എങ്ങനെയാണ് ഇഷ്ടം','How does Rahul like his tea','Rahulന് tea എങ്ങനെയാണ് ഇഷ്ടം')),
 (words('സൈക്കിൾ പമ്പ് വരാന്തയിൽ','The bicycle pump is on the veranda','Cycle pump വരാന്തയിൽ'),words('സൈക്കിൾ പമ്പ് എവിടെയാണ്','Where is the bicycle pump','Cycle pump എവിടെയാണ്')),
 (words('അമ്മൂമ്മയുടെ ഇഷ്ടപ്പൂവ് മുല്ല','Grandma’s favourite flower is jasmine','Grandmaയുടെ favourite flower മുല്ല'),words('അമ്മൂമ്മയ്ക്ക് ഇഷ്ടമുള്ള പൂവ് ഏതാണ്','What is grandma’s favourite flower','Grandmaക്ക് ഇഷ്ടമുള്ള flower ഏതാണ്')),
 (words('സംഗീത ക്ലാസ് വെള്ളിയാഴ്ചയാണ്','Music class is on Friday','Music class Fridayയാണ്'),words('സംഗീത ക്ലാസ് ഏതു ദിവസമാണ്','Which day is music class','Music class ഏതു ദിവസമാണ്')),
 (words('കേക്ക് വാങ്ങുന്ന കടയുടെ പേര് മധുരം','The cake shop is called Madhuram','Cake shopന്റെ പേര് Madhuram'),words('കേക്ക് വാങ്ങുന്ന കടയുടെ പേര് എന്താണ്','What is the name of the cake shop','Cake shopന്റെ പേര് എന്താണ്')),
 (words('ചെറിയ ബാഗ് കട്ടിലിനടിയിൽ','The small bag is under the bed','Small bag bedനടിയിൽ'),words('ചെറിയ ബാഗ് എവിടെയാണ്','Where is the small bag','Small bag എവിടെയാണ്')),
 (words('ദിയയുടെ പിറന്നാൾ ഓഗസ്റ്റ് 16','Diya’s birthday is August 16','Diyaയുടെ birthday August 16'),words('ദിയയുടെ പിറന്നാൾ എന്നാണ്','When is Diya’s birthday','Diyaയുടെ birthday എന്നാണ്')),
 (words('വെള്ളക്കുപ്പി സ്കൂൾ ബാഗിൽ','The water bottle is in the school bag','Water bottle school bagൽ'),words('വെള്ളക്കുപ്പി എവിടെയാണ്','Where is the water bottle','Water bottle എവിടെയാണ്')),
 (words('ടോർച്ചിന് രണ്ട് ബാറ്ററി വേണം','The torch needs two batteries','Torchന് two batteries വേണം'),words('ടോർച്ചിന് എത്ര ബാറ്ററി വേണം','How many batteries does the torch need','Torchന് എത്ര batteries വേണം')),
 (words('അധിക ചാർജർ ഓഫീസിലെ ലോക്കറിൽ','The spare charger is in the office locker','Spare charger office lockerൽ'),words('അധിക ചാർജർ എവിടെയാണ്','Where is the spare charger','Spare charger എവിടെയാണ്')),
 (words('കൈത്തറി കട തൃശ്ശൂരിലാണ്','The handloom shop is in Thrissur','Handloom shop Thrissurലാണ്'),words('കൈത്തറി കട എവിടെയാണ്','Where is the handloom shop','Handloom shop എവിടെയാണ്')),
 (words('പാചകക്കുറിപ്പ് പച്ച നോട്ട്ബുക്കിൽ','The recipe is in the green notebook','Recipe green notebookൽ'),words('പാചകക്കുറിപ്പ് എവിടെയാണ് എഴുതിയത്','Where did I write the recipe','Recipe എവിടെയാണ് എഴുതിയത്')),
 (words('മീനയ്ക്ക് പച്ചക്കറിഭക്ഷണം മതി','Meena prefers vegetarian food','Meenaക്ക് vegetarian food മതി'),words('മീനയ്ക്ക് ഏതു ഭക്ഷണമാണ് ഇഷ്ടം','What food does Meena prefer','Meenaക്ക് ഏതു food ആണ് ഇഷ്ടം')),
 (words('നൂലും സൂചിയും ചുവന്ന പെട്ടിയിൽ','The needle and thread are in the red box','Needle and thread ചുവന്ന boxൽ'),words('നൂലും സൂചിയും എവിടെയാണ്','Where are the needle and thread','Needle and thread എവിടെയാണ്')),
 (words('ഹരിയുടെ ഇഷ്ടകായികം ബാഡ്മിന്റൺ','Hari’s favourite sport is badminton','Hariയുടെ favourite sport badminton'),words('ഹരിക്ക് ഇഷ്ടമുള്ള കായികം ഏതാണ്','What is Hari’s favourite sport','Hariക്ക് ഇഷ്ടമുള്ള sport ഏതാണ്')),
 (words('ഫോട്ടോ ആൽബം മുകളിലെ ഷെൽഫിൽ','The photo album is on the top shelf','Photo album top shelfൽ'),words('ഫോട്ടോ ആൽബം എവിടെയാണ്','Where is the photo album','Photo album എവിടെയാണ്')),
]
SAVE_PATTERNS = [
 words('{fact}. ഓർത്തുവെക്കണേ.', '{fact}. Remember that, please.', '{fact}. Remember ചെയ്തുവെക്കണേ.'),
 words('മിലോ, ഇത് കുറിച്ചുവെക്കൂ: {fact}.', 'Milo, save this note: {fact}.', 'Milo, ഇത് note ആക്കൂ: {fact}.'),
 words('{fact} എന്ന് ഒരു കുറിപ്പ് ചേർക്കാമോ?', 'Keep a note that {fact}.', '{fact} എന്ന് ഒരു note ചേർക്കാമോ?'),
 words('പിന്നെ ചോദിക്കുമ്പോൾ പറയണം, {fact}.', 'Remember this for when I ask later: {fact}.', 'പിന്നെ ചോദിക്കുമ്പോൾ പറയണം, {fact}; save ചെയ്യൂ.'),
 words('ഇത് മറക്കരുതേ: {fact}. കുറിച്ചിടൂ.', 'Please store this for me: {fact}.', 'ഇത് മറക്കരുതേ: {fact}. Note ചെയ്യൂ.'),
 words('{fact}. എന്റെ കുറിപ്പുകളിൽ വെച്ചുതരൂ.', '{fact}. Add it to my notes.', '{fact}. എന്റെ notesൽ വെച്ചുതരൂ.'),
 words('മോളേ, ഒരു കാര്യം ഓർത്തിരിക്കണം: {fact}.', 'Could you remember one thing for me: {fact}?', 'മോളേ, ഒരു കാര്യം remember ചെയ്യണം: {fact}.'),
 words('ഈ വിവരം സേവ് ചെയ്യണേ: {fact}.', 'Save this information: {fact}.', 'ഈ information save ചെയ്യണേ: {fact}.'),
]
FIND_PATTERNS = [
 words('{q}? ഞാൻ നിന്നോട് പറഞ്ഞിരുന്നല്ലോ.', '{q}? I told you earlier.', '{q}? ഞാൻ നിന്നോട് പറഞ്ഞിരുന്നല്ലോ.'),
 words('എന്റെ കുറിപ്പിൽ നോക്കൂ, {q}?', 'Check my notes: {q}?', 'എന്റെ notesൽ നോക്കൂ, {q}?'),
 words('മിലോ, ഓർത്തുവെച്ചത് നോക്കി പറയാമോ, {q}?', 'Milo, look up what I saved: {q}?', 'Milo, save ചെയ്തത് നോക്കി പറയാമോ, {q}?'),
 words('{q} എന്ന് സേവ് ചെയ്തതിൽ തിരയൂ.', 'Search my saved notes for this: {q}?', '{q} എന്ന് saved notesൽ search ചെയ്യൂ.'),
 words('ഞാൻ ഓർത്തുവെക്കാൻ പറഞ്ഞതിൽ നിന്ന് പറയൂ, {q}?', 'From what I asked you to remember, {q}?', 'ഞാൻ remember ചെയ്യാൻ പറഞ്ഞതിൽ നിന്ന് പറയൂ, {q}?'),
 words('ഒരു പഴയ കുറിപ്പ് നോക്കണം: {q}?', 'Find the old note that answers this: {q}?', 'ഒരു old note നോക്കണം: {q}?'),
 words('മോളേ, നിന്റെ ഓർമ്മയിൽ നോക്കണേ, {q}?', 'Could you check what you remember: {q}?', 'മോളേ, memoryൽ നോക്കണേ, {q}?'),
 words('{q}? അതിനെപ്പറ്റിയുള്ള കുറിപ്പ് എടുത്തുതരൂ.', '{q}? Pull up my note about it.', '{q}? അതിനെപ്പറ്റിയുള്ള note എടുത്തുതരൂ.'),
]

def note(batch,i,lang,now):
    j = (batch-14)*50+i
    fact,q = NOTES[(j*11+batch)%len(NOTES)]
    save = (i+batch)%2 == 0
    pat = (j//2+batch)%8
    if save:
        user = SAVE_PATTERNS[pat][lang].format(fact=fact[lang])
        args = {'text':fact[lang]}
        say = 'Saved that in your notes.' if lang=='en' else 'ശരി, നിന്റെ കുറിപ്പുകളിൽ ഓർത്തുവെച്ചു.'
        name = 'save_note'
        family = f'note-save-form-{pat:02d}'
    else:
        user = FIND_PATTERNS[pat][lang].format(q=q[lang])
        args = {'question':q[lang]+'?'}
        say = "I'll check your saved notes." if lang=='en' else 'നിന്റെ കുറിപ്പുകളിൽ ഒന്ന് തിരഞ്ഞുനോക്കാം.'
        name = 'find_note'
        family = f'note-find-form-{pat:02d}'
    emit(batch,i,lang,now,user,name,args,say,family,name)

LIST_ITEMS = {
 'shopping':[
  words('പാൽ','milk','milk'),words('അരി','rice','rice'),words('മുട്ട','eggs','eggs'),
  words('തക്കാളി','tomatoes','tomatoes'),words('സവാള','onions','onions'),words('വെളിച്ചെണ്ണ','coconut oil','coconut oil'),
  words('ചായപ്പൊടി','tea powder','tea powder'),words('സോപ്പ്','soap','soap'),words('തേങ്ങ','coconut','coconut'),
  words('പഴം','bananas','bananas'),words('തൈര്','curd','curd'),words('ബ്രെഡ്','bread','bread'),
  words('കറിവേപ്പില','curry leaves','curry leaves'),words('പഞ്ചസാര','sugar','sugar'),words('കടല','chickpeas','chickpeas')],
 'to-do':[
  words('പുസ്തകം തിരികെ കൊടുക്കുക','return the book','book തിരികെ കൊടുക്കുക'),words('മേശ വൃത്തിയാക്കുക','clean the desk','desk clean ചെയ്യുക'),
  words('ചെടികൾക്ക് വെള്ളം ഒഴിക്കുക','water the plants','plantsന് വെള്ളം ഒഴിക്കുക'),words('ചെരുപ്പ് നന്നാക്കുക','repair the sandals','sandals repair ചെയ്യുക'),
  words('ബാഗ് ഒരുക്കുക','pack the bag','bag pack ചെയ്യുക'),words('അപേക്ഷ എഴുതുക','write the application','application എഴുതുക'),
  words('വസ്ത്രങ്ങൾ മടക്കുക','fold the clothes','clothes fold ചെയ്യുക'),words('ഫോട്ടോ പ്രിന്റ് എടുക്കുക','print the photos','photos print എടുക്കുക'),
  words('ജനൽ തുടയ്ക്കുക','wipe the window','window തുടയ്ക്കുക'),words('സൈക്കിൾ കഴുകുക','wash the bicycle','cycle കഴുകുക'),
  words('ചാർജർ കണ്ടെത്തുക','find the charger','charger കണ്ടെത്തുക'),words('റിപ്പോർട്ട് പരിശോധിക്കുക','review the report','report review ചെയ്യുക'),
  words('കുട നന്നാക്കുക','repair the umbrella','umbrella repair ചെയ്യുക')],
 'packing':[
  words('കുട','umbrella','umbrella'),words('ചാർജർ','charger','charger'),words('വെള്ളക്കുപ്പി','water bottle','water bottle'),
  words('ടിക്കറ്റ്','ticket','ticket'),words('തൂവാല','towel','towel'),words('ബ്രഷ്','toothbrush','toothbrush'),
  words('മരുന്ന്','medicine','medicine'),words('കണ്ണട','glasses','glasses'),words('നോട്ട്ബുക്ക്','notebook','notebook'),
  words('സോക്സ്','socks','socks'),words('ചെരിപ്പ്','slippers','slippers'),words('പവർ ബാങ്ക്','power bank','power bank'),
  words('തൊപ്പി','cap','cap')]
}
LIST_PATTERNS = [
 words('{lst} ലിസ്റ്റിൽ {items} ചേർക്കൂ.', 'Add {items} to my {lst} list.', '{lst} listൽ {items} add ചെയ്യൂ.'),
 words('{items} വേണം; {lst} ലിസ്റ്റിൽ ഇടണേ.', 'Put {items} on the {lst} list, please.', '{items} വേണം; {lst} listൽ ഇടണേ.'),
 words('മിലോ, {lst} ലിസ്റ്റിലേക്ക് {items} എഴുതിവെക്കൂ.', 'Milo, write {items} onto my {lst} list.', 'Milo, {lst} listലേക്ക് {items} എഴുതിവെക്കൂ.'),
 words('{lst} ലിസ്റ്റിൽ ഇതും വേണം: {items}.', 'Please include these in the {lst} list: {items}.', '{lst} listൽ ഇതും വേണം: {items}.'),
 words('{items} കൂടി {lst} ലിസ്റ്റിൽ ചേർക്കാമോ?', 'Could you add {items} to the {lst} list?', '{items} കൂടി {lst} listൽ add ചെയ്യാമോ?'),
 words('എന്റെ {lst} ലിസ്റ്റ് കൂട്ടണം, {items} ചേർക്കൂ.', 'Update my {lst} list by adding {items}.', 'എന്റെ {lst} list update ചെയ്യണം, {items} add ചെയ്യൂ.'),
 words('{lst} എന്ന ലിസ്റ്റിലേക്ക് {items} കുറിച്ചിടൂ.', 'For the list called {lst}, add {items}.', '{lst} എന്ന listലേക്ക് {items} കുറിച്ചിടൂ.'),
 words('മോളേ, {items} {lst} ലിസ്റ്റിൽ എഴുതണേ.', 'Please remember to put {items} on my {lst} list.', 'മോളേ, {items} {lst} listൽ എഴുതണേ.'),
 words('ലിസ്റ്റിൽ ചേർക്കണം: {items}; {lst} ലിസ്റ്റിലാണ്.', 'List update: add {items} to {lst}.', 'Listൽ add ചെയ്യണം: {items}; {lst} listലാണ്.'),
 words('{lst} ലിസ്റ്റ് എടുത്ത് {items} കൂടി വെക്കൂ.', 'Open my {lst} list and add {items}.', '{lst} list എടുത്ത് {items} കൂടി വെക്കൂ.'),
]

def list_row(batch,i,lang,now):
    kind = ['shopping','to-do','packing'][i%3]
    custom = i%7 == 0
    if custom:
        name = words('ഓണസാധനങ്ങൾ','Onam essentials','Onam essentials') if kind=='shopping' else (words('വീട്ടുപണികൾ','Home jobs','Home jobs') if kind=='to-do' else words('മൂന്നാർ യാത്ര','Munnar trip','Munnar trip'))
        argname = name[lang]
    else:
        name = {'shopping':words('ഷോപ്പിംഗ്','shopping','shopping'),'to-do':words('ചെയ്യേണ്ട കാര്യങ്ങൾ','to-do','to-do'),'packing':words('പാക്കിംഗ്','packing','packing')}[kind]
        argname = kind if kind!='packing' else name[lang]
    pool = LIST_ITEMS[kind]
    items = [pool[(i*5+k*3)%len(pool)][lang] for k in range(1+i%4)]
    connector = ', '
    joined = connector.join(items)
    pat = (i*3+2)%len(LIST_PATTERNS)
    user = LIST_PATTERNS[pat][lang].format(lst=name[lang],items=joined)
    say = 'Added those to your list.' if lang=='en' else 'ശരി, നിന്റെ ലിസ്റ്റിൽ ചേർത്തു ട്ടോ.'
    emit(batch,i,lang,now,user,'add_to_list',{'list':argname,'items':items},say,f'list-add-form-{pat:02d}','list-'+kind)


def build():
    for batch in range(1,17):
        for i in range(50):
            lang, now = language(i), context(batch,i)
            fn = reminder if batch<=6 else repeat_reminder if batch<=8 else alarm_timer if batch<=10 else event if batch<=13 else note if batch<=15 else list_row
            fn(batch,i,lang,now)
    assert len(ROWS)==800 and len(META)==800
    assert len({r['id'] for r in ROWS})==800
    duplicates = [u for u,c in collections.Counter(r['messages'][0]['content'] for r in ROWS).items() if c>1]
    assert not duplicates, duplicates
    for batch in range(1,17):
        subset = [r for r in ROWS if r['id'].split('-')[0]==str(batch)]
        assert collections.Counter(r['lang'] for r in subset)=={'ml':25,'en':15,'mixed':10}
    for r in ROWS:
        assert len(r['say'].split())<=12, (r['id'],r['say'],len(r['say'].split()))
        date,weekday = r['now'].split()
        assert dt.datetime.fromisoformat(date).strftime('%A')==weekday
        if r['lang']!='en': assert re.search('[\u0d00-\u0d7f]',r['say'])
        args = r['messages'][-1]['tool_calls'][0]['arguments']
        value = args.get('time','')
        if re.match(r'^\d{4}-\d{2}-\d{2} ',value):
            target = dt.datetime.strptime(value,'%Y-%m-%d %H:%M')
            assert target>dt.datetime.fromisoformat(date)
        if 'minutes_before' in args: assert args['minutes_before']>0
    for filename,content in [('scheduling.jsonl',ROWS),('scheduling-meta.jsonl',META)]:
        (OUT/filename).write_text(''.join(json.dumps(x,ensure_ascii=False,separators=(',',':'))+'\n' for x in content),encoding='utf-8')
    stats = {'rows':len(ROWS),'languages':dict(collections.Counter(r['lang'] for r in ROWS)),
             'tools':dict(collections.Counter(r['messages'][-1]['tool_calls'][0]['name'] for r in ROWS)),
             'families':len({x['family'] for x in META}),'max_say_words':max(len(r['say'].split()) for r in ROWS),
             'malayalam_review_required':True,'say_requires_success':True,
             'notes':['Synthetic authored patterns and slot combinations, not human conversations.',
                      'Post-action say strings require successful tool results; find_note says only that it will look.',
                      'No audio, voice training, external actions or source repository edits performed.']}
    (OUT/'scheduling-stats.json').write_text(json.dumps(stats,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(stats,ensure_ascii=False))

if __name__=='__main__': build()
