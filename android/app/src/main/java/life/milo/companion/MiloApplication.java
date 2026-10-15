package life.milo.companion;
import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
/** Cloud messaging remains disabled until a user opts in from People. */
public final class MiloApplication extends Application {
    @Override public void onCreate(){super.onCreate();if(FirebaseApp.getApps(this).isEmpty())return;
        if(BuildConfig.DEBUG){try{Class<?> debug=Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory");FirebaseAppCheck.getInstance().installAppCheckProviderFactory((com.google.firebase.appcheck.AppCheckProviderFactory)debug.getMethod("getInstance").invoke(null));}catch(Exception ignored){FirebaseAppCheck.getInstance().installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());}}
        else FirebaseAppCheck.getInstance().installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());
    }
    /** The loaded local model holds hundreds of MB; give it back once Milo is in the background and Android asks. */
    @Override public void onTrimMemory(int level){super.onTrimMemory(level);if(level>=TRIM_MEMORY_BACKGROUND&&!FloatingPetService.talking){PortableLocalAi.release();LiteRtLocalAi.release();}}
}
