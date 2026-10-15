package life.milo.companion;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;
import org.json.*;

/**
 * Tasks the user asks Milo to remember, e.g. "remind me at 1 pm to go to the office".
 * Stored on this phone only. Milo comes back a few minutes before and again at the time:
 * the floating pet appears, says the user's name and the task, and a notification offers Done and Snooze.
 */
final class MiloTasks {
    static final String TAG="MiloTasks",PREF="tasks";
    static final int BEFORE=0,AT=1;

    static SharedPreferences prefs(Context c){return c.getSharedPreferences("milo",Context.MODE_PRIVATE);}
    static synchronized JSONArray all(Context c){try{return new JSONArray(prefs(c).getString(PREF,"[]"));}catch(JSONException e){return new JSONArray();}}
    static synchronized void save(Context c,JSONArray tasks){prefs(c).edit().putString(PREF,tasks.toString()).commit();}
    /** Tasks that have not happened yet, soonest first. */
    static List<JSONObject> upcoming(Context c){
        List<JSONObject> list=new ArrayList<>();JSONArray a=all(c);
        for(int i=0;i<a.length();i++){JSONObject t=a.optJSONObject(i);if(t!=null&&!t.optBoolean("done")&&t.optLong("when")>System.currentTimeMillis()-60_000)list.add(t);}
        list.sort((x,y)->Long.compare(x.optLong("when"),y.optLong("when")));return list;
    }
    static JSONObject find(Context c,int id){JSONArray a=all(c);for(int i=0;i<a.length();i++){JSONObject t=a.optJSONObject(i);if(t!=null&&t.optInt("id")==id)return t;}return null;}

    /** Saves a task and schedules both alarms. Returns the saved task (an existing one if it is the same task at the same time). */
    static synchronized JSONObject add(Context c,String task,long when,int before)throws JSONException{
        JSONArray a=all(c),kept=new JSONArray();
        for(int i=0;i<a.length();i++){JSONObject t=a.optJSONObject(i);if(t==null)continue;
            // Forget finished tasks after a day.
            if((t.optBoolean("done")||t.optLong("when")<System.currentTimeMillis()-60_000)&&t.optLong("when")<System.currentTimeMillis()-86_400_000L)continue;
            if(!t.optBoolean("done")&&t.optLong("when")==when&&t.optString("task").equalsIgnoreCase(task))return t;
            kept.put(t);}
        int id=prefs(c).getInt("task_seq",100)+1;prefs(c).edit().putInt("task_seq",id).apply();
        JSONObject t=new JSONObject().put("id",id).put("task",task).put("when",when).put("before",Math.max(0,before)).put("done",false);
        kept.put(t);save(c,kept);schedule(c,t);return t;
    }
    static synchronized boolean remove(Context c,int id){
        JSONArray a=all(c),kept=new JSONArray();boolean found=false;
        for(int i=0;i<a.length();i++){JSONObject t=a.optJSONObject(i);if(t==null)continue;if(t.optInt("id")==id){found=true;cancel(c,id);continue;}kept.put(t);}
        save(c,kept);c.getSystemService(NotificationManager.class).cancel(TAG,id);return found;
    }
    static synchronized void markDone(Context c,int id){
        JSONArray a=all(c);for(int i=0;i<a.length();i++){JSONObject t=a.optJSONObject(i);if(t!=null&&t.optInt("id")==id)try{t.put("done",true);}catch(JSONException ignored){}}
        save(c,a);cancel(c,id);
    }
    static synchronized void snooze(Context c,int id,int minutes){
        JSONArray a=all(c);JSONObject t=null;
        for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null&&x.optInt("id")==id){t=x;try{x.put("when",System.currentTimeMillis()+minutes*60_000L).put("before",0).put("done",false);}catch(JSONException ignored){}}}
        save(c,a);if(t!=null)schedule(c,t);c.getSystemService(NotificationManager.class).cancel(TAG,id);
    }

    /* ---- alarms ---- */
    static PendingIntent fire(Context c,int id,int stage){
        return PendingIntent.getBroadcast(c,id*2+stage,new Intent(c,TaskReceiver.class).setAction(TaskReceiver.FIRE).putExtra("id",id).putExtra("stage",stage),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static void cancel(Context c,int id){AlarmManager am=c.getSystemService(AlarmManager.class);am.cancel(fire(c,id,BEFORE));am.cancel(fire(c,id,AT));}
    static void schedule(Context c,JSONObject t){
        int id=t.optInt("id");long when=t.optLong("when"),early=when-t.optInt("before")*60_000L,now=System.currentTimeMillis();
        cancel(c,id);if(t.optBoolean("done")||when<=now)return;
        AlarmManager am=c.getSystemService(AlarmManager.class);
        boolean exact=Build.VERSION.SDK_INT<31||am.canScheduleExactAlarms();
        PendingIntent show=PendingIntent.getActivity(c,9,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        // An alarm clock is the most reliable way to wake the phone on time; it also shows the alarm icon in the status bar.
        if(exact)am.setAlarmClock(new AlarmManager.AlarmClockInfo(when,show),fire(c,id,AT));else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,fire(c,id,AT));
        if(t.optInt("before")>0&&early>now){if(exact)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,early,fire(c,id,BEFORE));else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,early,fire(c,id,BEFORE));}
        Log.i(TAG,"scheduled #"+id+" at "+new Date(when)+(exact?" exact":" inexact"));
    }
    /** After a reboot or an app update, alarms must be set again. */
    static void scheduleAll(Context c){for(JSONObject t:upcoming(c))schedule(c,t);}

    /* ---- what Milo says ---- */
    static String userName(Context c){return prefs(c).getString("user_name","").trim();}
    static String time(Context c,long when,boolean malayalam){
        Calendar k=Calendar.getInstance();k.setTimeInMillis(when);int h=k.get(Calendar.HOUR_OF_DAY),m=k.get(Calendar.MINUTE);
        if(!malayalam)return new SimpleDateFormat(m==0?"h a":"h:mm a",Locale.US).format(new Date(when));
        String part=h<5?"രാത്രി":h<12?"രാവിലെ":h<15?"ഉച്ചയ്ക്ക്":h<20?"വൈകുന്നേരം":"രാത്രി";int h12=h%12==0?12:h%12;
        return part+" "+h12+(m==0?" മണിക്ക്":":"+String.format(Locale.US,"%02d",m)+"ന്");
    }
    /** "today", "tomorrow" or the date, for confirming a new task. */
    static String day(long when){
        Calendar k=Calendar.getInstance(),t=Calendar.getInstance();t.setTimeInMillis(when);
        if(k.get(Calendar.YEAR)==t.get(Calendar.YEAR)&&k.get(Calendar.DAY_OF_YEAR)==t.get(Calendar.DAY_OF_YEAR))return "today";
        k.add(Calendar.DAY_OF_YEAR,1);if(k.get(Calendar.YEAR)==t.get(Calendar.YEAR)&&k.get(Calendar.DAY_OF_YEAR)==t.get(Calendar.DAY_OF_YEAR))return "tomorrow";
        return new SimpleDateFormat("EEEE d MMMM",Locale.US).format(new Date(when));
    }
    static String speech(Context c,JSONObject t,int stage){
        boolean ml=MiloVoice.malayalam(prefs(c));String name=userName(c),task=t.optString("task").replaceAll("[.!?।\\s]+$","");long when=t.optLong("when");
        String hi=name.isEmpty()?"":name+", ";
        if(ml)return stage==BEFORE?hi+"ഇനി "+t.optInt("before")+" മിനിറ്റ് കൂടിയേ ഉള്ളൂ. "+time(c,when,true)+": "+task+".":hi+"സമയമായി! "+task+".";
        String cap=task.isEmpty()?task:Character.toUpperCase(task.charAt(0))+task.substring(1);
        return stage==BEFORE?(name.isEmpty()?"Hey! ":"Hey "+name+"! ")+"Just "+t.optInt("before")+(t.optInt("before")==1?" minute":" minutes")+" left. At "+time(c,when,false)+": "+cap+".":(name.isEmpty()?"":name+", ")+"it's "+time(c,when,false)+"! "+cap+"!";
    }

    /** Notification, sound and the floating pet speaking the reminder. */
    static void announce(Context c,JSONObject t,int stage){
        int id=t.optInt("id");String text=speech(c,t,stage);SharedPreferences p=prefs(c);
        NotificationManager nm=c.getSystemService(NotificationManager.class);
        NotificationChannel ch=new NotificationChannel("tasks","Your reminders from Milo",NotificationManager.IMPORTANCE_HIGH);
        ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        ch.enableVibration(true);nm.createNotificationChannel(ch);
        if(Build.VERSION.SDK_INT<33||c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED){
            PendingIntent open=PendingIntent.getActivity(c,8,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder b=new Notification.Builder(c,"tasks").setSmallIcon(R.drawable.ic_stat_milo).setContentTitle(p.getString("pet_name","Milo")+" · "+t.optString("task"))
                .setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setContentIntent(open).setAutoCancel(true).setCategory(Notification.CATEGORY_REMINDER).setVisibility(Notification.VISIBILITY_PRIVATE)
                .addAction(new Notification.Action.Builder(null,"Done",action(c,TaskReceiver.DONE,id)).build())
                .addAction(new Notification.Action.Builder(null,"Snooze 10 min",action(c,TaskReceiver.SNOOZE,id)).build());
            nm.notify(TAG,id,b.build());
        }
        if(p.getBoolean("pet_enabled",true)&&Settings.canDrawOverlays(c)){
            Intent pet=new Intent(c,FloatingPetService.class).setAction("REMIND").putExtra("text",text).putExtra("tone",stage==AT?"excited":"curious");
            try{c.startForegroundService(pet);}catch(RuntimeException e){Log.w(TAG,"pet could not appear: "+e);}
        }
    }
    static PendingIntent action(Context c,String what,int id){return PendingIntent.getBroadcast(c,id*4+(what.equals(TaskReceiver.DONE)?2:3)+1_000_000,new Intent(c,TaskReceiver.class).setAction(what).putExtra("id",id),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}

    /* ---- understanding a time ---- */
    private static final Pattern IN=Pattern.compile("(?:in|after)?\\s*(\\d+(?:\\.\\d+)?|an?|one|half an?)\\s*(hours?|hrs?|h|minutes?|mins?|m)\\b(?:\\s*(?:and\\s*)?(\\d+)\\s*(minutes?|mins?|m)\\b)?");
    private static final Pattern ISO=Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})[T ](\\d{1,2}):(\\d{2})");
    private static final Pattern CLOCK=Pattern.compile("(?:^|[^0-9:])([0-9]{1,2})(?:[.:]([0-9]{2}))?\\s*(am|a\\.m\\.|pm|p\\.m\\.|o'?clock)?(?=$|[^0-9:])");
    static final String[] DAYS={"sunday","monday","tuesday","wednesday","thursday","friday","saturday"};

    /** The moment meant by a time such as "1 pm", "13:30", "in 20 minutes", "tomorrow 9 am" or "2026-10-16 09:00"; -1 when unclear. */
    static long parseWhen(String text,long now){
        if(text==null)return -1;String s=text.toLowerCase(Locale.ROOT).trim();Calendar k=Calendar.getInstance();k.setTimeInMillis(now);
        Matcher iso=ISO.matcher(s);
        if(iso.find()){k.set(Integer.parseInt(iso.group(1)),Integer.parseInt(iso.group(2))-1,Integer.parseInt(iso.group(3)),Integer.parseInt(iso.group(4)),Integer.parseInt(iso.group(5)),0);k.set(Calendar.MILLISECOND,0);return k.getTimeInMillis();}
        Matcher in=IN.matcher(s);
        if(in.find()&&!s.matches(".*\\d\\s*(am|pm|a\\.m|p\\.m|o'?clock).*")&&!s.contains(":")){
            String n=in.group(1);double amount=n.startsWith("half")?0.5:n.equals("a")||n.equals("an")||n.equals("one")?1:Double.parseDouble(n);
            long ms=in.group(2).startsWith("h")?(long)(amount*3_600_000):(long)(amount*60_000);
            if(in.group(3)!=null)ms+=Long.parseLong(in.group(3))*60_000;
            return ms>0?now+ms:-1;
        }
        int addDays=0;boolean dayGiven=false;
        if(s.contains("day after tomorrow")){addDays=2;dayGiven=true;}else if(s.contains("tomorrow")){addDays=1;dayGiven=true;}else if(s.contains("today")||s.contains("tonight"))dayGiven=true;
        int weekday=-1;for(int d=0;d<7;d++)if(s.contains(DAYS[d])){weekday=d;dayGiven=true;}
        int hour=-1,minute=0;Boolean pm=null;
        if(s.contains("noon")){hour=12;pm=true;}else if(s.contains("midnight")){hour=0;pm=false;}
        else{
            // In a whole sentence ("call 2 friends at 5 pm") the time is the number with am/pm, else the one after "at", else the first.
            Matcher m=CLOCK.matcher(s);int best=0;
            while(m.find()){int h=Integer.parseInt(m.group(1));if(h>23)continue;String ap=m.group(3);
                int score=ap!=null?3:s.substring(Math.max(0,m.start(1)-4),m.start(1)).matches(".*\\bat\\s*")?2:1;
                if(score<=best)continue;best=score;hour=h;minute=m.group(2)==null?0:Integer.parseInt(m.group(2));pm=ap==null||ap.startsWith("o")?null:ap.startsWith("a")?Boolean.FALSE:Boolean.TRUE;}
        }
        if(hour<0){if(s.contains("morning")){hour=9;}else if(s.contains("afternoon")){hour=14;}else if(s.contains("evening")){hour=18;}else if(s.contains("night")||s.contains("tonight")){hour=21;}else return -1;}
        if(pm==null){if(s.contains("morning"))pm=false;else if(s.contains("afternoon")||s.contains("evening")||s.contains("night"))pm=true;}
        if(pm!=null&&hour<=12){if(pm&&hour<12)hour+=12;if(!pm&&hour==12)hour=0;}
        if(minute>59)return -1;
        k.set(Calendar.SECOND,0);k.set(Calendar.MILLISECOND,0);
        if(weekday>=0){int diff=(weekday+1-k.get(Calendar.DAY_OF_WEEK)+7)%7;k.add(Calendar.DAY_OF_YEAR,diff);}else k.add(Calendar.DAY_OF_YEAR,addDays);
        k.set(Calendar.HOUR_OF_DAY,hour);k.set(Calendar.MINUTE,minute);
        if(pm==null&&hour>=1&&hour<=11){
            // No am/pm. On a named day, 1–6 means afternoon and 7–11 morning; otherwise the next time the clock shows that hour.
            if(dayGiven){if(hour<=6)k.add(Calendar.HOUR_OF_DAY,12);}
            else{while(k.getTimeInMillis()<=now)k.add(Calendar.HOUR_OF_DAY,12);return k.getTimeInMillis();}
        }
        if(k.getTimeInMillis()<=now){if(weekday>=0&&!s.contains("today"))k.add(Calendar.DAY_OF_YEAR,7);else if(!dayGiven)k.add(Calendar.DAY_OF_YEAR,1);else return -1;}
        return k.getTimeInMillis();
    }
}
