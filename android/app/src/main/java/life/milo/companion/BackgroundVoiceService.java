package life.milo.companion;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.*;
import android.speech.*;
import android.speech.tts.*;
import java.util.*;
import java.util.concurrent.*;
import java.text.SimpleDateFormat;
import org.json.*;

/** Explicit opt-in voice session with a visible stop action. Never starts at boot. */
public class BackgroundVoiceService extends Service implements RecognitionListener {
    static final int ID=501;
    Handler ui=new Handler(Looper.getMainLooper());
    ExecutorService worker=Executors.newSingleThreadExecutor();
    SpeechRecognizer recognizer;TextToSpeech tts;SharedPreferences prefs;
    boolean stopped,busy,ready;int missed,spoken;String key;List<JSONObject> history=new ArrayList<>();
    @Override public IBinder onBind(Intent i){return null;}
    @Override public void onCreate(){super.onCreate();prefs=getSharedPreferences("milo",MODE_PRIVATE);}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null||"STOP".equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
        if(recognizer!=null)return START_NOT_STICKY;
        if(!prefs.getBoolean("pet_enabled",true)||checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED||!SpeechRecognizer.isRecognitionAvailable(this)){stopSelf();return START_NOT_STICKY;}
        AiProvider provider=AiProvider.current(this);try{key=KeyVault.loadForProvider(this,provider.id());if(!provider.isFree()&&key.isEmpty()){stopSelf();return START_NOT_STICKY;}}catch(Exception e){stopSelf();return START_NOT_STICKY;}
        NotificationManager manager=getSystemService(NotificationManager.class);manager.createNotificationChannel(new NotificationChannel("voice","Active voice session",NotificationManager.IMPORTANCE_LOW));
        if(Build.VERSION.SDK_INT>=30)startForeground(ID,notification("Starting voice session…"),ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);else startForeground(ID,notification("Starting voice session…"));
        recognizer=SpeechRecognizer.createSpeechRecognizer(this);recognizer.setRecognitionListener(this);
        tts=new TextToSpeech(this,result->{if(stopped)return;ready=result==TextToSpeech.SUCCESS;if(ready){ready=tts.setLanguage(locale())>=0;}if(!ready){finish("A spoken voice is unavailable. Choose a supported language in Setup.");return;}listen();});
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){public void onStart(String id){}public void onError(String id){ui.post(()->finish("The voice service could not speak. Please open the app."));}public void onDone(String id){if("final".equals(id))ui.post(()->{if(!stopped&&!busy)listen();});}});
        return START_NOT_STICKY;
    }
    Locale locale(){String code=prefs.getString("voice_language","");return code.isEmpty()?Locale.getDefault():Locale.forLanguageTag(code);}
    Notification notification(String status){PendingIntent open=PendingIntent.getActivity(this,1,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent stop=PendingIntent.getService(this,2,new Intent(this,BackgroundVoiceService.class).setAction("STOP"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);return new Notification.Builder(this,"voice").setSmallIcon(life.milo.companion.R.drawable.milo_icon).setContentTitle(prefs.getString("pet_name","Milo")+" voice is active").setContentText(status).setContentIntent(open).setOngoing(true).setVisibility(Notification.VISIBILITY_PRIVATE).addAction(new Notification.Action.Builder(null,"Stop microphone",stop).build()).build();}
    void status(String s){if(!stopped)getSystemService(NotificationManager.class).notify(ID,notification(s));}
    void listen(){if(stopped||busy||!ready)return;if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){finish("Microphone permission was removed.");return;}status("Listening — tap Stop microphone to end");Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,locale().toLanguageTag());try{recognizer.startListening(i);}catch(Exception e){finish("Speech recognition could not start. Use the app’s Speak button instead.");}}
    void finish(String message){if(!stopped)android.widget.Toast.makeText(this,message,android.widget.Toast.LENGTH_LONG).show();stopSelf();}
    @Override public void onResults(Bundle results){if(stopped||busy)return;ArrayList<String> words=results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(words==null||words.isEmpty()){onError(SpeechRecognizer.ERROR_NO_MATCH);return;}String prompt=words.get(0).trim();if(prompt.isEmpty()){onError(SpeechRecognizer.ERROR_NO_MATCH);return;}missed=0;busy=true;spoken=0;
        String day=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());int used=prefs.getString("usage_day","").equals(day)?prefs.getInt("usage",0):0;if(used>=30){finish("Daily voice/chat limit reached. Try tomorrow.");return;}prefs.edit().putString("usage_day",day).putInt("usage",used+1).apply();status("Thinking — microphone is paused");
        String context="Mood: "+prefs.getString("mood","Okay")+". Interests: "+prefs.getString("interests","")+". Goal: "+prefs.getString("goal","")+". Next step: "+prefs.getString("step","");
        worker.execute(()->{try{AiProvider provider=AiProvider.current(this);boolean useSearch=prefs.getBoolean("search",true)&&AiProvider.GEMINI.equals(provider.id());MainActivity.GeminiReply r=provider.chat(key,prefs.getString("pet_name","Milo"),locale().getDisplayLanguage(Locale.ENGLISH),prefs.getString("practice_language",""),prefs.getString("companion_style","Gentle"),prompt,new ArrayList<>(history),context,useSearch,partial->ui.post(()->{if(stopped)return;int end=MainActivity.sentenceBoundary(partial,spoken);if(end>spoken){tts.speak(MainActivity.cleanSpeech(partial.substring(spoken,end)),TextToSpeech.QUEUE_ADD,null,"chunk");spoken=end;status("Speaking — microphone is paused");}}));ui.post(()->{if(stopped)return;busy=false;try{history.add(MainActivity.turn("user",prompt));history.add(MainActivity.turn("model",r.text));while(history.size()>16){history.remove(0);history.remove(0);}}catch(Exception ignored){}
            // Full reply and citations are available on return to Talk, rather than spoken source URLs.
            MainActivity.backgroundReply=r;MainActivity.backgroundQuestion=prompt;
            String rest=r.text.substring(Math.min(spoken,r.text.length()));if(rest.trim().isEmpty())tts.playSilentUtterance(100,TextToSpeech.QUEUE_ADD,"final");else tts.speak(MainActivity.cleanSpeech(rest),TextToSpeech.QUEUE_ADD,null,"final");});}catch(Exception e){ui.post(()->finish(e instanceof MainActivity.ApiFailure?e.getMessage():"Connection interrupted. Voice stopped; open the app to retry."));}});
    }
    @Override public void onError(int error){if(stopped)return;if(error==SpeechRecognizer.ERROR_NO_MATCH||error==SpeechRecognizer.ERROR_SPEECH_TIMEOUT){if(++missed>=5){finish("Voice stopped after a quiet period. Start again when you’re ready.");return;}ui.postDelayed(this::listen,800);}else finish("Speech recognition stopped. Check your voice service and permissions.");}
    @Override public void onReadyForSpeech(Bundle b){} @Override public void onBeginningOfSpeech(){} @Override public void onRmsChanged(float r){} @Override public void onBufferReceived(byte[] b){} @Override public void onEndOfSpeech(){status("Processing speech — microphone is paused");} @Override public void onPartialResults(Bundle b){} @Override public void onEvent(int e,Bundle b){}
    @Override public void onDestroy(){stopped=true;ui.removeCallbacksAndMessages(null);worker.shutdownNow();if(recognizer!=null){recognizer.cancel();recognizer.destroy();}if(tts!=null){tts.stop();tts.shutdown();}key=null;history.clear();stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
}
