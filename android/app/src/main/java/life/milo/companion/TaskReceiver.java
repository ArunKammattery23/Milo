package life.milo.companion;

import android.content.*;
import org.json.JSONObject;

/** A reminder's time has come (a few minutes before, or exactly), or the user tapped Done or Snooze on its notification. */
public class TaskReceiver extends BroadcastReceiver {
    static final String FIRE="life.milo.companion.TASK_FIRE",DONE="life.milo.companion.TASK_DONE",SNOOZE="life.milo.companion.TASK_SNOOZE";
    @Override public void onReceive(Context c,Intent i){
        int id=i.getIntExtra("id",-1);String action=String.valueOf(i.getAction());
        if(DONE.equals(action)){MiloTasks.markDone(c,id);c.getSystemService(android.app.NotificationManager.class).cancel(MiloTasks.TAG,id);return;}
        if(SNOOZE.equals(action)){MiloTasks.snooze(c,id,10);return;}
        if(Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)){MiloTasks.scheduleAll(c);return;}
        if(!FIRE.equals(action))return;
        JSONObject t=MiloTasks.find(c,id);if(t==null||t.optBoolean("done"))return;
        int stage=i.getIntExtra("stage",MiloTasks.AT);
        MiloTasks.announce(c,t,stage);
        if(stage==MiloTasks.AT)MiloTasks.markDone(c,id);
    }
}
