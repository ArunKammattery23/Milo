package life.milo.companion;
import android.content.Context;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import com.google.firebase.functions.*;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.android.gms.tasks.Task;
import java.util.*;
final class PeopleClient {
    static FirebaseFirestore store;
    static boolean configured(Context c){return !FirebaseApp.getApps(c).isEmpty();}
    static boolean ready(Context c){return configured(c)&&BuildConfig.PEOPLE_RELAY_ENABLED;}
    static FirebaseUser current(){return FirebaseAuth.getInstance().getCurrentUser();}
    static synchronized FirebaseFirestore db(){if(store==null){store=FirebaseFirestore.getInstance();store.setFirestoreSettings(new FirebaseFirestoreSettings.Builder().setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build());}return store;}
    static Task<HttpsCallableResult> call(String name,Map<String,Object> data){return FirebaseFunctions.getInstance("us-central1").getHttpsCallable(name).call(data);}
    static Map<String,Object> data(Object... pairs){Map<String,Object> m=new HashMap<>();for(int n=0;n<pairs.length;n+=2)m.put((String)pairs[n],pairs[n+1]);return m;}
    static String error(Exception e){if(e instanceof FirebaseFunctionsException){String m=e.getMessage();return m==null?"People service unavailable. Try again.":m;}return "Could not complete this action. Check your connection and account, then try again.";}
    static void signOut(Context c){if(configured(c)){FirebaseAuth.getInstance().signOut();FirebaseMessaging.getInstance().setAutoInitEnabled(false);FirebaseMessaging.getInstance().deleteToken();}c.getSharedPreferences("milo",0).edit().putBoolean("people_notifications",false).remove("people_notifications_owner").apply();}
}
