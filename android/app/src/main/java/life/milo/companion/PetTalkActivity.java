package life.milo.companion;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * See-through, momentary screen opened when the user taps the floating pet while Milo is in the background.
 * Android grants a foreground microphone session only after a visible start, so the pet starts listening from here.
 */
public final class PetTalkActivity extends Activity {
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);
        try{startForegroundService(new Intent(this,FloatingPetService.class).setAction("TALK"));}catch(RuntimeException ignored){}
        getWindow().getDecorView().postDelayed(()->{finish();overridePendingTransition(0,0);},700);
    }
}
