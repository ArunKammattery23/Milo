package life.milo.companion;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.Calendar;
import org.json.*;
public class ReminderReceiver extends BroadcastReceiver {
    static PendingIntent pending(Context c,int id,String type){return PendingIntent.getBroadcast(c,id,new Intent(c,ReminderReceiver.class).setAction(type),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    static JSONArray memories(Context c){try{return new JSONArray(c.getSharedPreferences("milo",Context.MODE_PRIVATE).getString("memories","[]"));}catch(Exception e){return new JSONArray();}}
    static void schedule(Context c){SharedPreferences p=c.getSharedPreferences("milo",Context.MODE_PRIVATE);AlarmManager alarms=c.getSystemService(AlarmManager.class);alarms.cancel(pending(c,601,"DAILY"));JSONArray ms=memories(c);for(int n=0;n<ms.length();n++){JSONObject m=ms.optJSONObject(n);if(m!=null)alarms.cancel(pending(c,m.optInt("id"),"MEMORY"));}if(!p.getBoolean("pet_enabled",true)||!p.getBoolean("reminders",false))return;
        Calendar next=Calendar.getInstance();next.set(Calendar.HOUR_OF_DAY,9);next.set(Calendar.MINUTE,0);next.set(Calendar.SECOND,0);if(next.getTimeInMillis()<=System.currentTimeMillis())next.add(Calendar.DAY_OF_YEAR,1);
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pending(c,601,"DAILY"));
        for(int n=0;n<ms.length();n++){JSONObject m=ms.optJSONObject(n);if(m!=null&&m.optLong("when")>System.currentTimeMillis())alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,m.optLong("when"),pending(c,m.optInt("id"),"MEMORY"));}
    }
    @Override public void onReceive(Context c,Intent i){SharedPreferences p=c.getSharedPreferences("milo",Context.MODE_PRIVATE);if("android.intent.action.BOOT_COMPLETED".equals(i.getAction())){schedule(c);return;}if(!p.getBoolean("pet_enabled",true)||!p.getBoolean("reminders",false))return;
        if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        NotificationManager n=c.getSystemService(NotificationManager.class);n.createNotificationChannel(new NotificationChannel("care","Gentle reminders",NotificationManager.IMPORTANCE_DEFAULT));PendingIntent open=PendingIntent.getActivity(c,3,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        String message="MEMORY".equals(i.getAction())?"An important moment you saved is coming up. Open Memories.":"A small pause, a stretch, or one kind step? Choose what suits you.";
        n.notify("MEMORY".equals(i.getAction())?603:602,new Notification.Builder(c,"care").setSmallIcon(R.drawable.ic_stat_milo).setContentTitle(p.getString("pet_name","Milo")+" · a gentle reminder").setContentText(message).setContentIntent(open).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build());if("DAILY".equals(i.getAction()))schedule(c);
    }
}
