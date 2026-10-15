package life.milo.companion;

import android.content.*;
import android.content.pm.*;
import android.os.BatteryManager;
import android.util.Log;
import com.google.ai.edge.litertlm.OpenApiTool;
import com.google.ai.edge.litertlm.ToolKt;
import com.google.ai.edge.litertlm.ToolProvider;
import java.text.SimpleDateFormat;
import java.util.*;
import org.json.*;

/**
 * Milo's small set of actions on this phone. The on-phone model decides when to call them; each runs here,
 * on the phone, and returns a short result that Milo then puts into her own words.
 * Milo can only do what these tools allow: she cannot read messages, contacts, photos or other apps.
 */
final class MiloAgent {
    static final String TAG="MiloAgent";
    /** Tools called during the latest reply, e.g. "set_reminder → Saved …", for the reply log and tests. */
    static final List<String> lastCalls=Collections.synchronizedList(new ArrayList<>());

    static String instruction(Context c){
        String name=MiloTasks.userName(c);
        return " You can act on this phone with tools. Follow these rules exactly:"
            +" 1) If the user asks you to remind them, to remember or schedule a task for a time, or to set an alarm or timer (in any language, e.g. 'remind me at 1 pm to go to the office'), call set_reminder at once with the task and the time. Do not ask 'is that right?' first, and never only promise."
            +" 2) If the user asks to open an app, call open_app at once."
            +" 3) For the time, date, day or battery, call phone_status."
            +" 4) Use list_reminders, cancel_reminder and remember_name when they fit."
            +" 5) Never say you set, opened or saved anything unless you called the tool and its result says it worked; then tell the user the exact time and the day (today or tomorrow) from the result in a friendly way. If the time is missing, ask for it."
            +" You cannot read messages, contacts, photos or the screen, and you cannot call or text anyone."
            +(name.isEmpty()?"":" The user's name is "+name+"; use it now and then.");
    }

    /* Small on-phone models sometimes answer "I've set it!" without calling the tool. When a request clearly asks for an action,
       a short note added to the message points the model at the right tool. */
    static final java.util.regex.Pattern REMIND=java.util.regex.Pattern.compile("(?i)\\b(remind|reminder|remember to|don'?t let me forget|alarm|timer|wake me|notify me)\\b|ഓർമ്മിപ്പിക്ക|ഓർമിപ്പിക്ക|ഓർമ്മപ്പെടുത്ത|ഓർമപ്പെടുത്ത|അലാറം|ടൈമർ|റിമൈൻഡ്");
    static final java.util.regex.Pattern OPEN=java.util.regex.Pattern.compile("(?i)^\\s*(?:please\\s+|can you\\s+|could you\\s+)?(?:open|launch|start)\\s+\\S|തുറക്ക|ഓപ്പൺ");
    static final java.util.regex.Pattern LIST=java.util.regex.Pattern.compile("(?i)\\b(what|which|any|my|show)\\b.*\\breminders\\b|\\breminders?\\b.*\\b(do i have|today|tomorrow|list)\\b");
    static final java.util.regex.Pattern CANCEL=java.util.regex.Pattern.compile("(?i)\\b(cancel|delete|remove|stop|clear)\\b.*\\b(reminders?|alarms?)\\b|\\b(reminders?|alarms?)\\b.*\\b(cancel|delete|remove)\\b");
    static final java.util.regex.Pattern TIME=java.util.regex.Pattern.compile("(?i)\\b(what time is it|what'?s the time|what is the time|time now|current time|battery|what day is it|today'?s date)\\b|സമയം എത്ര|എത്ര മണിയായി|ബാറ്ററി");
    /** The tool a request clearly asks for, or null when it is ordinary conversation. */
    static String intent(String prompt){
        if(prompt==null)return null;
        if(LIST.matcher(prompt).find())return "list_reminders";if(CANCEL.matcher(prompt).find())return "cancel_reminder";
        if(REMIND.matcher(prompt).find())return "set_reminder";if(OPEN.matcher(prompt).find())return "open_app";if(TIME.matcher(prompt).find())return "phone_status";return null;
    }
    static String withHint(String prompt){
        String tool=intent(prompt);if(tool==null)return prompt==null?"":prompt;
        return prompt+"\n(Note for Milo: call "+tool+" now, before writing any words"+(tool.equals("set_reminder")?"; if no time is given, ask for it":"")+".)";
    }
    static boolean called(String tool){synchronized(lastCalls){for(String s:lastCalls)if(s.startsWith(tool+" "))return true;}return false;}

    /* The model sometimes writes a call as plain words, e.g. "open_app YouTube" or "set_reminder(task='…', time='6 pm')".
       Such text is never shown or spoken; Milo runs the call herself. */
    static final String NAMES="set_reminder|open_app|phone_status|list_reminders|cancel_reminder|remember_name";
    static final java.util.regex.Pattern WRITTEN=java.util.regex.Pattern.compile("`{0,3}(?:tool_code|tool_call|python)?\\s*\\[?\\b("+NAMES+")\\b\\s*(\\(([^)]*)\\)?|\\{([^}]*)\\}?|[^\\n.!?\\[\\]]*)\\]?`{0,3}");
    static final java.util.regex.Pattern ARG=java.util.regex.Pattern.compile("(\\w+)\\s*[=:]\\s*(?:'([^']*)'|\"([^\"]*)\"|([^,)}]+))|'([^']*)'|\"([^\"]*)\"");
    static String clean(String text){return text==null?null:text.replace("<|\"|>","\"").replaceAll("<\\|[^|>]{0,24}\\|>","");}
    static String hideCalls(String text){return text==null?null:WRITTEN.matcher(clean(text)).replaceAll("").replaceAll("[ \\t]{2,}"," ").trim();}
    /** Runs calls written as text; returns their results. */
    /* ---- clear requests Milo can carry out herself when the model did not: instant, and independent of the small model ---- */
    static final String[][] ML_NUMBERS={{"പന്ത്രണ്ട്","12"},{"പതിനൊന്ന്","11"},{"പത്ത്","10"},{"ഒമ്പത്","9"},{"ഒൻപത്","9"},{"എട്ട്","8"},{"ഏഴ്","7"},{"ആറ്","6"},{"അഞ്ച്","5"},{"നാല്","4"},{"മൂന്ന്","3"},{"രണ്ട്","2"},{"ഒന്ന്","1"},{"ഒരു","1"}};
    static final String ML_PARTS="രാവിലെ|ഉച്ചയ്ക്ക്|ഉച്ചക്ക്|ഉച്ചയ്‌ക്ക്|വൈകുന്നേരം|വൈകിട്ട്|സന്ധ്യയ്ക്ക്|രാത്രി";
    static final String ML_DAYS_WORDS="മറ്റന്നാൾ|നാളെ|ഇന്ന്";
    /** A Malayalam time as English words for {@link MiloTasks#parseWhen}: "നാളെ രാവിലെ 9 മണിക്ക്" → "tomorrow 9:00 am"; null when there is none. */
    static String malayalamTime(String s){
        String t=s;for(String[] n:ML_NUMBERS)t=t.replaceAll(n[0]+"(?=\\s*(മണി|മിനിറ്റ്))",n[1]);
        java.util.regex.Matcher d=java.util.regex.Pattern.compile("(\\d+)\\s*(മിനിറ്റ്|മണിക്കൂർ)\\S*\\s*(കഴിഞ്ഞ്|കഴിയുമ്പോൾ|ശേഷം)").matcher(t);
        if(d.find())return "in "+d.group(1)+(d.group(2).startsWith("മിനിറ്റ്")?" minutes":" hours");
        java.util.regex.Matcher h=java.util.regex.Pattern.compile("(\\d{1,2})(?:[:.](\\d{2}))?\\s*മണി(?!ക്കൂർ)").matcher(t);
        if(!h.find())return null;int hour=Integer.parseInt(h.group(1));String mm=h.group(2)==null?"00":h.group(2);
        String day=t.contains("മറ്റന്നാൾ")?"day after tomorrow ":t.contains("നാളെ")?"tomorrow ":t.contains("ഇന്ന്")?"today ":"";
        String part="";
        if(t.contains("രാവിലെ"))part=" am";
        else if(t.contains("രാത്രി"))part=hour==12||hour<=4?" am":" pm";
        else if(t.matches("(?s).*(ഉച്ച|വൈകുന്നേരം|വൈകിട്ട്|സന്ധ്യ).*"))part=" pm";
        return day+hour+":"+mm+part;
    }
    static String reminderTask(String prompt,boolean malayalam){
        String t=prompt;
        if(malayalam){
            for(String[] n:ML_NUMBERS)t=t.replaceAll(n[0]+"(?=\\s*(മണി|മിനിറ്റ്))",n[1]);
            t=t.replaceAll("\\d+\\s*(മിനിറ്റ്|മണിക്കൂർ)\\S*\\s*(കഴിഞ്ഞ്|കഴിയുമ്പോൾ|ശേഷം)?","").replaceAll("\\d{1,2}([:.]\\d{2})?\\s*മണി\\S*","")
                .replaceAll("("+ML_PARTS+"|"+ML_DAYS_WORDS+")","").replaceAll("എന്നെ|\\S*ഓർമ്മിപ്പിക്ക\\S*|\\S*ഓർമിപ്പിക്ക\\S*|\\S*ഓർമ്മപ്പെടുത്ത\\S*|\\S*ഓർമപ്പെടുത്ത\\S*|റിമൈൻഡ\\S*|ഒന്ന്|പ്ലീസ്","");
        }else{
            t=t.replaceAll("(?i)^\\s*(hey\\s+milo[,!]?\\s*)?((please|can you|could you|will you)\\s+)*(remind me|set (a |an )?(reminder|alarm)|reminder|remember)\\s*(to|that|about|for)?\\s*","")
                .replaceAll("(?i)\\b(at|by|around)\\s+\\d{1,2}([:.]\\d{2})?\\s*(am|pm|a\\.m\\.|p\\.m\\.|o'?clock)?","").replaceAll("(?i)\\b\\d{1,2}([:.]\\d{2})?\\s*(am|pm|a\\.m\\.|p\\.m\\.)","")
                .replaceAll("(?i)\\b(in|after)\\s+(\\d+|an?|one|half an?)\\s*(hours?|hrs?|minutes?|mins?)(\\s*(and\\s*)?\\d+\\s*(minutes?|mins?))?","")
                .replaceAll("(?i)\\b(today|tonight|tomorrow|this (morning|afternoon|evening)|in the (morning|afternoon|evening)|(on )?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)|at noon|noon|midnight|please)\\b","")
                .trim().replaceAll("(?i)^(to|that|about|for)\\s+","");
        }
        return t.replaceAll("[.!?,\\s]+$","").replaceAll("^[,\\s]+","").replaceAll("\\s{2,}"," ").trim();
    }
    static final String[][] ML_APPS={{"യൂട്യൂബ്","YouTube"},{"വാട്സ്ആപ്പ്","WhatsApp"},{"വാട്സാപ്പ്","WhatsApp"},{"വാട്ട്സാപ്പ്","WhatsApp"},{"വാട്ട്സ്ആപ്പ്","WhatsApp"},{"ഇൻസ്റ്റഗ്രാം","Instagram"},{"ഇൻസ്റ്റാഗ്രാം","Instagram"},
        {"ഫേസ്ബുക്ക്","Facebook"},{"ക്യാമറ","Camera"},{"ക്ലോക്ക്","Clock"},{"സെറ്റിംഗ്സ്","Settings"},{"സെറ്റിങ്സ്","Settings"},{"ഗാലറി","Gallery"},{"കാൽക്കുലേറ്റർ","Calculator"},{"ക്രോം","Chrome"},
        {"മാപ്സ്","Maps"},{"മാപ്പ്","Maps"},{"സ്പോട്ടിഫൈ","Spotify"},{"ജിമെയിൽ","Gmail"},{"ഫോൺ","Phone"},{"മെസേജ്","Messages"},{"കലണ്ടർ","Calendar"}};
    static String appName(String prompt){
        java.util.regex.Matcher en=java.util.regex.Pattern.compile("(?i)\\b(?:open|launch|start)\\s+(?:the\\s+|my\\s+)?(.+?)(?:\\s+app)?(?:\\s+(?:please|for me|now))*\\s*[.!?]*$").matcher(prompt.trim());
        String name=en.find()?en.group(1):null;
        if(name==null){java.util.regex.Matcher ml=java.util.regex.Pattern.compile("(\\S+)\\s*(?:ആപ്പ്\\S*\\s*)?(?:ഒന്ന്\\s*)?(?:തുറക്ക|ഓപ്പൺ)").matcher(prompt);if(ml.find())name=ml.group(1);}
        if(name==null)return null;for(String[] a:ML_APPS)if(name.startsWith(a[0]))return a[1];
        return name.replaceAll("[.!?,]+$","").trim();
    }
    /** Carries out a clear request without the model; results as "name: result", or empty when the request is not clear enough. */
    static List<String> runDirect(Context c,String prompt,String tool,boolean malayalam){
        List<String> out=new ArrayList<>();
        try{
            String result=null;
            switch(tool){
                case "set_reminder":{
                    boolean ml=malayalam||prompt.matches("(?s).*[ഀ-ൿ].*");String time=ml?malayalamTime(prompt):null;if(time==null&&MiloTasks.parseWhen(prompt,System.currentTimeMillis())>0)time=prompt;
                    String task=reminderTask(prompt,ml);if(time==null||task.isEmpty())break;
                    result=setReminder(c,new JSONObject().put("task",task).put("time",time));if(!result.startsWith("Saved"))result=null;break;}
                case "open_app":{String app=appName(prompt);if(app!=null&&!app.isEmpty()){result=openApp(c,app,2);if(!result.startsWith("Opened"))result=null;}break;}
                case "list_reminders":result=listReminders(c);break;
                case "phone_status":result=status(c);break;
                case "cancel_reminder":{java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)\\b(?:cancel|delete|remove|stop|clear)\\s+(?:my\\s+|the\\s+)?(all\\s+)?(.*?)\\s*(?:reminders?|alarms?)\\b").matcher(prompt);
                    if(m.find()){String w=m.group(1)!=null?"all":m.group(2).trim();if(!w.isEmpty()){result=cancelReminder(c,w);if(!result.startsWith("Removed"))result=null;}}break;}
            }
            if(result!=null){Log.i(TAG,"direct "+tool+" → "+result);lastCalls.add(tool+" → "+result);out.add(tool+": "+result);}
        }catch(JSONException ignored){}
        return out;
    }

    static List<String> runWritten(Context c,String text){
        List<String> results=new ArrayList<>();if(text==null)return results;java.util.regex.Matcher m=WRITTEN.matcher(clean(text));
        while(m.find()){
            String name=m.group(1),inside=m.group(3)!=null?m.group(3):m.group(4)!=null?m.group(4):m.group(2)==null||m.group(2).startsWith("(")||m.group(2).startsWith("{")?"":m.group(2).trim();JSONObject args=new JSONObject();
            try{java.util.regex.Matcher a=ARG.matcher(inside);List<String> positional=new ArrayList<>();
                while(a.find()){if(a.group(1)!=null){String v=a.group(2)!=null?a.group(2):a.group(3)!=null?a.group(3):a.group(4).trim();args.put(a.group(1),v);}else positional.add(a.group(5)!=null?a.group(5):a.group(6));}
                if(positional.isEmpty()&&args.length()==0&&!inside.isEmpty())positional.add(inside.replaceAll("^[\"'(]+|[\"')]+$",""));
                String[] order=name.equals("set_reminder")?new String[]{"task","time"}:name.equals("open_app")?new String[]{"app"}:name.equals("remember_name")?new String[]{"name"}:new String[]{"task"};
                for(int i=0;i<positional.size()&&i<order.length;i++)if(!args.has(order[i]))args.put(order[i],positional.get(i));
            }catch(JSONException ignored){}
            String result=run(c,name,args);Log.i(TAG,"written "+name+" "+args+" → "+result);lastCalls.add(name+" → "+result);results.add(name+": "+result);
        }
        return results;
    }
    static String run(Context c,String name,JSONObject a){
        try{switch(name){
            case "set_reminder":return setReminder(c,a);case "list_reminders":return listReminders(c);case "cancel_reminder":return cancelReminder(c,a.optString("task"));
            case "open_app":return openApp(c,a.optString("app"));case "phone_status":return status(c);case "remember_name":return rememberName(c,a.optString("name"));
            default:return "Unknown action.";}}
        catch(Exception e){return "Could not do that: "+e.getMessage();}
    }

    static List<ToolProvider> tools(Context context){
        Context c=context.getApplicationContext();List<ToolProvider> list=new ArrayList<>();
        list.add(tool("set_reminder","Saves a task and reminds the user at that time: the pet appears, says their name and the task, and shows a notification. Also reminds a few minutes before.",
            props(new String[][]{{"task","What the user should do, as a short phrase in the user's language, e.g. 'Go to the office' or 'ഓഫീസിൽ പോകണം'."},
                {"time","When, written in English: '1 pm', '13:30', 'in 20 minutes', 'tomorrow 9 am', 'monday 6 pm'."},
                {"minutes_before","How many minutes before to give an early reminder; 5 unless the user says otherwise, 0 for none.","integer"}}),new String[]{"task","time"},
            a->run(c,"set_reminder",a)));
        list.add(tool("list_reminders","Lists the reminders Milo is keeping, soonest first.",new JSONObject(),new String[0],a->run(c,"list_reminders",a)));
        list.add(tool("cancel_reminder","Removes a reminder. Use the words of the task, or 'all'.",props(new String[][]{{"task","Words from the task to remove, or 'all'."}}),new String[]{"task"},a->run(c,"cancel_reminder",a)));
        list.add(tool("open_app","Opens an app installed on this phone, e.g. WhatsApp, YouTube, Camera, Clock, Settings.",props(new String[][]{{"app","The app's name."}}),new String[]{"app"},a->run(c,"open_app",a)));
        list.add(tool("phone_status","Tells the current time, date, day and battery level.",new JSONObject(),new String[0],a->run(c,"phone_status",a)));
        list.add(tool("remember_name","Saves the name the user wants to be called.",props(new String[][]{{"name","The user's name."}}),new String[]{"name"},a->run(c,"remember_name",a)));
        return list;
    }

    interface Action{String run(JSONObject args)throws Exception;}
    static ToolProvider tool(String name,String description,JSONObject properties,String[] required,Action action){
        String schema;
        try{schema=new JSONObject().put("name",name).put("description",description)
            .put("parameters",new JSONObject().put("type","object").put("properties",properties).put("required",new JSONArray(Arrays.asList(required)))).toString();}
        catch(JSONException e){throw new IllegalStateException(e);}
        return ToolKt.tool(new OpenApiTool(){
            public String getToolDescriptionJsonString(){return schema;}
            public String execute(String args){
                String result;
                try{result=action.run(args==null||args.trim().isEmpty()?new JSONObject():new JSONObject(args));}
                catch(Exception e){result="Could not do that: "+e.getMessage();}
                Log.i(TAG,name+" "+args+" → "+result);lastCalls.add(name+" → "+result);return result;
            }
        });
    }
    static JSONObject props(String[][] p){
        JSONObject o=new JSONObject();
        try{for(String[] x:p)o.put(x[0],new JSONObject().put("type",x.length>2?x[2]:"string").put("description",x[1]));}catch(JSONException e){throw new IllegalStateException(e);}
        return o;
    }

    /* ---- the actions ---- */
    /* ---- instant confirmations: when Milo ran a call the model only wrote as text, she says the result herself instead of waiting for a second reply ---- */
    static volatile JSONObject lastSaved;
    static final String[] ML_DAYS={"ഞായറാഴ്ച","തിങ്കളാഴ്ച","ചൊവ്വാഴ്ച","ബുധനാഴ്ച","വ്യാഴാഴ്ച","വെള്ളിയാഴ്ച","ശനിയാഴ്ച"};
    static String dayMl(long when){String d=MiloTasks.day(when);if(d.equals("today"))return "ഇന്ന്";if(d.equals("tomorrow"))return "നാളെ";Calendar k=Calendar.getInstance();k.setTimeInMillis(when);return ML_DAYS[k.get(Calendar.DAY_OF_WEEK)-1];}
    /** One short sentence for each result ("name: result"), in the user's language. */
    static String confirm(Context c,List<String> results,boolean ml){
        StringBuilder b=new StringBuilder();
        for(String r:results){int cut=r.indexOf(": ");String name=cut<0?r:r.substring(0,cut),res=cut<0?"":r.substring(cut+2);String s;
            switch(name){
                case "set_reminder":{JSONObject t=lastSaved;
                    if(!res.startsWith("Saved")||t==null){s=ml?"എപ്പോഴാണ് ഓർമ്മിപ്പിക്കേണ്ടത്? സമയം പറയൂ.":"Sure! What time should I remind you?";break;}
                    long when=t.optLong("when");String task=t.optString("task").replaceAll("^(?i)to\\s+","");int before=t.optInt("before");
                    s=ml?"ശരി! "+dayMl(when)+" "+MiloTasks.time(c,when,true)+" "+task+" ഞാൻ ഓർമ്മിപ്പിക്കാം."
                        :"Done! I'll remind you to "+task+" at "+MiloTasks.time(c,when,false)+" "+MiloTasks.day(when)+(before>0?", and "+before+(before==1?" minute":" minutes")+" before.":".");break;}
                case "open_app":{String app=res.startsWith("Opened ")?res.substring(7).replaceAll("\\.$",""):null;
                    s=app!=null?(ml?app+" തുറക്കുന്നു!":"Opening "+app+"!"):(ml?"ആ ആപ്പ് ഈ ഫോണിൽ കണ്ടില്ല.":"I couldn't find that app on this phone.");break;}
                case "phone_status":{long now=System.currentTimeMillis();Calendar k=Calendar.getInstance();int h=k.get(Calendar.HOUR_OF_DAY),m=k.get(Calendar.MINUTE);
                    int level=c.getSystemService(BatteryManager.class).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                    String part=h<5?"രാത്രി":h<12?"രാവിലെ":h<15?"ഉച്ചയ്ക്ക്":h<20?"വൈകുന്നേരം":"രാത്രി";
                    s=ml?"ഇപ്പോൾ സമയം "+part+" "+(h%12==0?12:h%12)+":"+String.format(Locale.US,"%02d",m)+". ബാറ്ററി "+level+"%."
                        :"It's "+new SimpleDateFormat("h:mm a, EEEE",Locale.US).format(new Date(now))+", and the battery is at "+level+"%.";break;}
                case "list_reminders":{List<JSONObject> list=MiloTasks.upcoming(c);
                    if(list.isEmpty()){s=ml?"ഇപ്പോൾ ഓർമ്മപ്പെടുത്തലുകൾ ഒന്നുമില്ല.":"You don't have any reminders yet.";break;}
                    StringBuilder l=new StringBuilder();for(JSONObject t:list){if(l.length()>0)l.append(ml?", ":"; ");long w=t.optLong("when");
                        l.append(ml?dayMl(w)+" "+MiloTasks.time(c,w,true)+" "+t.optString("task"):t.optString("task")+" at "+MiloTasks.time(c,w,false)+" "+MiloTasks.day(w));}
                    s=ml?"നിനക്കുള്ള ഓർമ്മപ്പെടുത്തലുകൾ: "+l+".":"Here's what I'm keeping for you: "+l+".";break;}
                case "cancel_reminder":s=res.startsWith("Removed")?(ml?"ശരി, മാറ്റി: "+res.substring(9).replaceAll("\\.$","")+".":"Done, I removed: "+res.substring(9)):(ml?"ആ ഓർമ്മപ്പെടുത്തൽ കണ്ടില്ല.":"I couldn't find that reminder.");break;
                case "remember_name":{String n=MiloTasks.userName(c);s=res.startsWith("Saved")&&!n.isEmpty()?(ml?n+", പരിചയപ്പെട്ടതിൽ സന്തോഷം!":"Nice to meet you, "+n+"!"):(ml?"നിന്റെ പേര് ഒന്നുകൂടി പറയാമോ?":"Could you tell me your name again?");break;}
                default:s="";
            }
            if(!s.isEmpty())b.append(b.length()>0?" ":"").append(s);
        }
        return b.toString();
    }

    static String setReminder(Context c,JSONObject a)throws JSONException{
        String task=clean(a.optString("task")).replaceAll("^[\"'\\s]+|[\"'\\s]+$",""),time=clean(a.optString("time")).replaceAll("[\"']","").trim();
        // The model sometimes copies the request into the task ("…വിളിക്കാൻ ഓർമ്മിപ്പിക്കണം", "remind me to …").
        task=task.replaceAll("\\s*(എന്നെ\\s*)?\\S*(ഓർമ്മിപ്പിക്ക|ഓർമിപ്പിക്ക)\\S*\\s*$","").replaceAll("(?i)^(please\\s+)?remind me\\s+(to\\s+)?","").trim();
        if(task.isEmpty())return "Not saved: ask the user what the reminder is for.";
        long when=MiloTasks.parseWhen(time,System.currentTimeMillis());
        if(when<0)return "Not saved: the time '"+time+"' is unclear. Ask the user for the time.";
        int before=a.has("minutes_before")?Math.max(0,Math.min(120,a.optInt("minutes_before",5))):5;
        if(when-System.currentTimeMillis()<=before*60_000L+30_000)before=0;
        JSONObject t=MiloTasks.add(c,task,when,before);lastSaved=t;
        return "Saved: '"+task+"' at "+MiloTasks.time(c,when,false)+" "+MiloTasks.day(when)+(before>0?". An early reminder comes "+before+" minutes before.":".");
    }
    static String listReminders(Context c){
        List<JSONObject> list=MiloTasks.upcoming(c);if(list.isEmpty())return "No reminders are saved.";
        StringBuilder b=new StringBuilder();for(JSONObject t:list)b.append("- ").append(t.optString("task")).append(" at ").append(MiloTasks.time(c,t.optLong("when"),false)).append(' ').append(MiloTasks.day(t.optLong("when"))).append('\n');
        return b.toString().trim();
    }
    static String cancelReminder(Context c,String words){
        String w=words.toLowerCase(Locale.ROOT).trim();int removed=0;List<String> names=new ArrayList<>();
        for(JSONObject t:MiloTasks.upcoming(c)){String task=t.optString("task");
            if(w.equals("all")||w.equals("everything")||(!w.isEmpty()&&(task.toLowerCase(Locale.ROOT).contains(w)||w.contains(task.toLowerCase(Locale.ROOT))))){MiloTasks.remove(c,t.optInt("id"));removed++;names.add(task);}}
        return removed==0?"No reminder matched '"+words+"'. Saved reminders:\n"+listReminders(c):"Removed: "+String.join(", ",names)+".";
    }
    static String openApp(Context c,String name){return openApp(c,name,1);}
    /** minScore 2 opens only an app whose name equals or starts with the words, as when Milo acts on a request without the model. */
    static String openApp(Context c,String name,int minScore){
        String want=name.toLowerCase(Locale.ROOT).replaceAll("\\s+app$","").trim();if(want.length()<2)return "Ask which app to open.";
        PackageManager pm=c.getPackageManager();ResolveInfo best=null;int bestScore=minScore-1;
        for(ResolveInfo r:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)){
            String label=String.valueOf(r.loadLabel(pm)).toLowerCase(Locale.ROOT).trim();
            int score=label.equals(want)?3:label.startsWith(want)||want.startsWith(label)?2:label.contains(want)?1:0;
            if(score>bestScore){bestScore=score;best=r;}
        }
        if(best==null)return "No app called '"+name+"' is installed.";
        Intent i=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClassName(best.activityInfo.packageName,best.activityInfo.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try{c.startActivity(i);}catch(RuntimeException e){return "Android did not let Milo open "+best.loadLabel(pm)+".";}
        return "Opened "+best.loadLabel(pm)+".";
    }
    static String status(Context c){
        BatteryManager bm=c.getSystemService(BatteryManager.class);int level=bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return "Now: "+new SimpleDateFormat("EEEE d MMMM yyyy, h:mm a",Locale.US).format(new Date())+". Battery "+level+"%"+(bm.isCharging()?", charging.":".");
    }
    static String rememberName(Context c,String name){
        String n=name.trim().replaceAll("[^\\p{L}\\p{M} .'-]","");if(n.isEmpty()||n.length()>30)return "Not saved: ask for the name again.";
        MiloTasks.prefs(c).edit().putString("user_name",n).apply();return "Saved. The user's name is "+n+".";
    }
}
