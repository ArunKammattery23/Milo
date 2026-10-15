package life.milo.companion;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import com.google.firebase.messaging.*;
import com.google.firebase.auth.FirebaseUser;
/** Push contains an identifier only; private message contents stay out of notifications. */
public final class PeopleMessagingService extends FirebaseMessagingService {
    @Override public void onNewToken(String token){if(!BuildConfig.PEOPLE_RELAY_ENABLED||!PeopleClient.ready(this))return;FirebaseUser u=PeopleClient.current();if(u!=null&&u.isEmailVerified()&&getSharedPreferences("milo",0).getBoolean("people_notifications",false)&&u.getUid().equals(getSharedPreferences("milo",0).getString("people_notifications_owner","")))PeopleClient.call("registerDevice",PeopleClient.data("token",token,"ownerUid",u.getUid()));}
    @Override public void onMessageReceived(RemoteMessage message){if(!BuildConfig.PEOPLE_RELAY_ENABLED||!PeopleClient.ready(this)||!"milo_people".equals(message.getData().get("kind")))return;FirebaseUser u=PeopleClient.current();if(u==null||!u.isEmailVerified()||!getSharedPreferences("milo",0).getBoolean("people_notifications",false)||!u.getUid().equals(getSharedPreferences("milo",0).getString("people_notifications_owner",""))||!getSharedPreferences("milo",0).getBoolean("pet_enabled",true)||!u.getUid().equals(message.getData().get("ownerUid")))return;
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        NotificationManager manager=getSystemService(NotificationManager.class);manager.createNotificationChannel(new NotificationChannel("people_messages","Messages from your people",NotificationManager.IMPORTANCE_DEFAULT));
        Intent open=new Intent(this,MainActivity.class).putExtra("open_people",true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);PendingIntent tap=PendingIntent.getActivity(this,81,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(this,"people_messages").setSmallIcon(R.drawable.ic_stat_milo).setContentTitle("New message in Milo").setContentText("Open Milo to read a message from your people.").setVisibility(Notification.VISIBILITY_PRIVATE).setAutoCancel(true).setContentIntent(tap).build();manager.notify(801,n);
    }
}
