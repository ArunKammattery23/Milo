package life.milo.companion;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.speech.*;
import android.speech.tts.*;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import java.util.*;
import java.util.concurrent.*;
import org.json.JSONObject;

/**
 * User-started floating pet. Drag it anywhere, tap it to talk hands-free, hold it to open Milo, drop it on ✕ to hide.
 * The microphone is used only during a conversation the user starts by tapping, with an ongoing Stop notification.
 * It never reads the screen and never starts at reboot.
 */
public final class FloatingPetService extends Service implements RecognitionListener {
    static volatile boolean visible,talking;
    /** The running pet, for device tests. */
    static volatile FloatingPetService current;
    static final int ID=502;
    static final String TAG="MiloPet";
    enum Mode{IDLE,LISTENING,THINKING,SPEAKING}

    WindowManager windows;WindowManager.LayoutParams layout,bubbleLayout,targetLayout;
    PetFaceView face;TextView bubble,target;SharedPreferences prefs;final Handler ui=new Handler(Looper.getMainLooper());
    Mode mode=Mode.IDLE;
    float downX,downY;int startX,startY;boolean dragged,longPressed,overTarget;
    final Runnable longPress=()->{if(!dragged&&face!=null){longPressed=true;face.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);openApp(false);}};
    final Runnable hideBubble=()->setBubble(null);
    final SharedPreferences.OnSharedPreferenceChangeListener prefsChanged=(p,key)->{
        if("pet_enabled".equals(key)&&!p.getBoolean("pet_enabled",true))stopSelf();
        else if("pet_expression".equals(key)&&mode==Mode.IDLE&&face!=null)face.setExpression(idleExpression());
    };

    SpeechRecognizer recognizer;MiloSpeaker speech;boolean ttsReady;
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    final List<JSONObject> history=new ArrayList<>();
    int misses,spoken,turn;String tone;long toneUntil,trampolineAt=-10_000;

    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        String action=intent==null?"STOP":String.valueOf(intent.getAction());
        if("STOP".equals(action)){stopSelf();return START_NOT_STICKY;}
        if(face!=null){
            if("STOP_TALK".equals(action))end("Tap me when you want to talk.");
            else if("TALK".equals(action)&&mode==Mode.IDLE)startConversation();
            else if("SAY".equals(action)&&BuildConfig.DEBUG&&intent.hasExtra("text")){goForeground(true);talking=true;ensureVoice();respond(intent.getStringExtra("text"));}
            else if("REMIND".equals(action))remind(intent.getStringExtra("text"),intent.getStringExtra("tone"));
            return START_NOT_STICKY;
        }
        prefs=getSharedPreferences("milo",MODE_PRIVATE);
        if(!prefs.getBoolean("pet_enabled",true)||!Settings.canDrawOverlays(this)){stopSelf();return START_NOT_STICKY;}
        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("floating_pet","Floating Milo pet",NotificationManager.IMPORTANCE_LOW));
        try{
            goForeground(false);
            windows=getSystemService(WindowManager.class);
            face=new PetFaceView(this);face.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            face.setContentDescription("Floating "+name()+" pet. Tap to talk. Hold to open Milo. Drag to the close mark to hide.");
            face.setExpression(idleExpression());
            layout=overlay(dp(118),dp(120),true);layout.x=prefs.getInt("pet_x",24);layout.y=prefs.getInt("pet_y",180);clamp();
            face.setOnTouchListener(this::touch);
            bubble=new TextView(this);bubble.setTextSize(15);bubble.setTextColor(0xff243830);bubble.setPadding(dp(14),dp(10),dp(14),dp(10));bubble.setMaxLines(6);bubble.setVisibility(View.GONE);
            GradientDrawable card=new GradientDrawable();card.setColor(0xf7ffffff);card.setCornerRadius(dp(18));card.setStroke(dp(1),0xffcfe0d2);bubble.setBackground(card);bubble.setElevation(dp(4));
            bubbleLayout=overlay(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,false);
            target=new TextView(this);target.setText("✕");target.setTextSize(26);target.setTextColor(0xffffffff);target.setGravity(Gravity.CENTER);target.setVisibility(View.GONE);
            GradientDrawable circle=new GradientDrawable();circle.setShape(GradientDrawable.OVAL);circle.setColor(0xcc243830);target.setBackground(circle);target.setContentDescription("Drop here to hide the pet");
            targetLayout=overlay(dp(68),dp(68),false);int[] screen=screen();targetLayout.x=(screen[0]-dp(68))/2;targetLayout.y=screen[1]-dp(170);
            windows.addView(target,targetLayout);windows.addView(bubble,bubbleLayout);windows.addView(face,layout);
            visible=true;current=this;prefs.registerOnSharedPreferenceChangeListener(prefsChanged);ensureVoice();
            // Loading Gemma 4 onto the GPU takes ~12 s; do it now so the first tap gets a quick reply.
            if(AiProvider.current(this) instanceof LiteRtLocalAi)worker.execute(()->LiteRtLocalAi.warm(this));
            if("TALK".equals(action))startConversation();
            else if("REMIND".equals(action))remind(intent.getStringExtra("text"),intent.getStringExtra("tone"));
        }catch(Exception e){Log.w(TAG,"overlay failed: "+e);stopSelf();android.widget.Toast.makeText(this,"Android could not show the floating pet. Check display-over-other-apps permission.",android.widget.Toast.LENGTH_LONG).show();}
        return START_NOT_STICKY;
    }

    /* ---- window, drag, tap ---- */
    WindowManager.LayoutParams overlay(int w,int h,boolean touchable){
        int flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|(touchable?0:WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,flags,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.LEFT;return p;
    }
    boolean touch(View v,MotionEvent e){
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:downX=e.getRawX();downY=e.getRawY();startX=layout.x;startY=layout.y;dragged=longPressed=false;ui.postDelayed(longPress,550);return true;
            case MotionEvent.ACTION_MOVE:{
                float dx=e.getRawX()-downX,dy=e.getRawY()-downY;
                if(!dragged&&Math.abs(dx)+Math.abs(dy)>dp(10)){dragged=true;ui.removeCallbacks(longPress);target.setVisibility(View.VISIBLE);}
                if(dragged){layout.x=startX+(int)dx;layout.y=startY+(int)dy;clamp();update(face,layout);
                    float cx=layout.x+layout.width/2f-(targetLayout.x+targetLayout.width/2f),cy=layout.y+layout.height/2f-(targetLayout.y+targetLayout.height/2f);
                    boolean near=Math.hypot(cx,cy)<dp(80);if(near!=overTarget){overTarget=near;target.setScaleX(near?1.3f:1f);target.setScaleY(near?1.3f:1f);if(near)face.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);}
                    placeBubble();}
                return true;}
            case MotionEvent.ACTION_UP:
                ui.removeCallbacks(longPress);
                if(dragged){target.setVisibility(View.GONE);if(overTarget){overTarget=false;android.widget.Toast.makeText(this,name()+" is hidden. Show it again from Milo → Setup.",android.widget.Toast.LENGTH_SHORT).show();stopSelf();return true;}
                    prefs.edit().putInt("pet_x",layout.x).putInt("pet_y",layout.y).apply();}
                else if(!longPressed){v.performClick();tap();}
                return true;
            case MotionEvent.ACTION_CANCEL:ui.removeCallbacks(longPress);target.setVisibility(View.GONE);overTarget=false;return true;
            default:return false;
        }
    }
    void tap(){
        if(mode==Mode.IDLE)startConversation();
        else if(mode==Mode.LISTENING){if(recognizer!=null)recognizer.stopListening();face.setExpression("thinking");}
        else interrupt();
    }
    void openApp(boolean askMicrophone){
        Intent i=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if(askMicrophone)i.putExtra("pet_microphone",true);
        startActivity(i);
    }
    void update(View v,WindowManager.LayoutParams p){try{windows.updateViewLayout(v,p);}catch(Exception ignored){}}
    int[] screen(){if(Build.VERSION.SDK_INT>=30){android.graphics.Rect r=windows.getCurrentWindowMetrics().getBounds();return new int[]{r.width(),r.height()};}android.util.DisplayMetrics m=new android.util.DisplayMetrics();windows.getDefaultDisplay().getRealMetrics(m);return new int[]{m.widthPixels,m.heightPixels};}
    void clamp(){int[] s=screen();layout.x=Math.max(0,Math.min(layout.x,s[0]-layout.width));layout.y=Math.max(dp(30),Math.min(layout.y,s[1]-layout.height-dp(40)));}
    int dp(int value){return (int)(value*getResources().getDisplayMetrics().density);}
    String name(){return prefs==null?"Milo":prefs.getString("pet_name","Milo");}

    /** Shows short live text beside the pet; null hides it. */
    void setBubble(String text){
        if(bubble==null)return;ui.removeCallbacks(hideBubble);
        if(text==null||text.trim().isEmpty()){bubble.setVisibility(View.GONE);return;}
        String shown=text.length()>240?"…"+text.substring(text.length()-240):text;
        bubble.setText(shown);bubble.setVisibility(View.VISIBLE);placeBubble();
    }
    void placeBubble(){
        if(bubble==null||bubble.getVisibility()!=View.VISIBLE)return;int[] s=screen();
        bubble.setMaxWidth((int)(s[0]*0.72f));bubble.measure(View.MeasureSpec.makeMeasureSpec((int)(s[0]*0.72f),View.MeasureSpec.AT_MOST),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        int w=bubble.getMeasuredWidth(),h=bubble.getMeasuredHeight();
        bubbleLayout.x=Math.max(dp(8),Math.min(layout.x+layout.width/2-w/2,s[0]-w-dp(8)));
        bubbleLayout.y=layout.y-h-dp(6)>=dp(30)?layout.y-h-dp(6):layout.y+layout.height+dp(6);
        update(bubble,bubbleLayout);
    }
    String idleExpression(){return tone!=null&&SystemClock.uptimeMillis()<toneUntil?tone:prefs.getString("pet_expression",CompanionStyle.forMood(prefs.getString("mood","Okay")));}

    /* ---- foreground state: the microphone type is added only while talking ---- */
    boolean goForeground(boolean microphone){
        PendingIntent open=PendingIntent.getActivity(this,71,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent hide=PendingIntent.getService(this,72,new Intent(this,FloatingPetService.class).setAction("STOP"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=new Notification.Builder(this,"floating_pet").setSmallIcon(R.drawable.ic_stat_milo).setOngoing(true).setVisibility(Notification.VISIBILITY_PRIVATE).setContentIntent(open)
            .setContentTitle(microphone?name()+" is listening in conversation":name()+" pet is visible")
            .setContentText(microphone?"Tap Stop talking to turn off the microphone":"Tap Milo to talk · hold to open · drag to ✕ to hide");
        if(microphone)b.addAction(new Notification.Action.Builder(null,"Stop talking",PendingIntent.getService(this,73,new Intent(this,FloatingPetService.class).setAction("STOP_TALK"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).build());
        b.addAction(new Notification.Action.Builder(null,"Hide pet",hide).build());
        try{
            if(Build.VERSION.SDK_INT>=34)startForeground(ID,b.build(),ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE|(microphone?ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE:0));
            else if(Build.VERSION.SDK_INT>=30)startForeground(ID,b.build(),microphone?ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE:0);
            else startForeground(ID,b.build());
            return true;
        }catch(RuntimeException e){Log.w(TAG,"foreground "+(microphone?"with microphone ":"")+"refused: "+e);return false;}
    }

    /* ---- hands-free conversation ---- */
    void startConversation(){
        if(!prefs.getBoolean("pet_enabled",true))return;
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){setBubble("Allow the microphone in Milo, then tap me again.");openApp(true);return;}
        if(!SpeechRecognizer.isRecognitionAvailable(this)){say("Speech recognition is unavailable on this phone.");return;}
        AiProvider provider=AiProvider.current(this);
        if(provider instanceof LiteRtLocalAi&&!LiteRtLocalAi.ready(this)||provider instanceof PortableLocalAi&&!PortableLocalAi.ready(this)){say("Set up on-phone AI in Milo → Setup first.");return;}
        try{if(provider.needsNetwork()&&KeyVault.loadForProvider(this,provider.id()).isEmpty()){say("Connect an AI in Milo → Setup first.");return;}}catch(Exception e){say("Open Milo → Setup to unlock your AI key.");return;}
        if(!goForeground(true)){
            // Android allows the microphone only after a visible start; a see-through screen gives it that moment, once per tap.
            if(SystemClock.uptimeMillis()-trampolineAt<3000){say("Android blocked the microphone. Open Milo and tap Talk instead.");return;}
            trampolineAt=SystemClock.uptimeMillis();
            startActivity(new Intent(this,PetTalkActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_NO_ANIMATION));return;
        }
        talking=true;misses=0;
        // Process Milo's instructions and this chat's history while the user is still speaking.
        if(provider instanceof LiteRtLocalAi){List<JSONObject> past=new ArrayList<>(history);String name=name(),language=language(),practice=prefs.getString("practice_language",""),style=prefs.getString("companion_style","Gentle"),context=voiceContext();worker.execute(()->LiteRtLocalAi.prepare(this,name,language,practice,style,context,past));}
        ensureVoice();listen();
    }
    void ensureVoice(){
        if(speech!=null){MiloSpeaker s=speech;s.refresh(()->{if(speech!=s)return;ttsReady=s.ready();if(ttsReady&&pendingRemind!=null){Runnable r=pendingRemind;pendingRemind=null;r.run();}});return;}
        speech=new MiloSpeaker(this,prefs,new MiloSpeaker.Listener(){
            public void onStart(String id,String tone){if(mode==Mode.SPEAKING&&face!=null){face.setExpression(tone!=null?tone:"speaking");face.setTalking(true);}}
            public void onDone(String id){if(id.equals(remindId)){remindId=null;mode=Mode.IDLE;if(face!=null){face.setTalking(false);face.setExpression(idleExpression());}ui.postDelayed(hideBubble,20_000);return;}
                if(id.equals("final"+turn)){if(face!=null)face.setTalking(false);afterSpeaking();}}
            public void onLevel(float level){if(face!=null&&mode==Mode.SPEAKING)face.setLevel(level);}
        },()->{if(speech==null)return; // the pet was closed while the voice was loading
            ttsReady=speech.ready();if(!ttsReady)Log.w(TAG,"no voice for "+MiloVoice.locale(prefs));
            if(ttsReady&&pendingRemind!=null){Runnable r=pendingRemind;pendingRemind=null;r.run();}});
    }

    /* ---- reminders the user asked for: Milo appears, says their name and the task ---- */
    String remindId;Runnable pendingRemind;
    void remind(String text,String remindTone){
        if(face==null||text==null||text.isEmpty())return;
        if(mode!=Mode.IDLE)end(null);
        String expression=remindTone==null?"excited":remindTone,id="remind"+(++turn);remindId=id;
        face.setExpression(expression);setBubble(text);ui.removeCallbacks(hideBubble);
        Log.i(TAG,"reminder: "+text.length()+" chars");
        Runnable say=()->{if(!id.equals(remindId)||speech==null||!ttsReady)return;
            mode=Mode.SPEAKING;tone=expression;toneUntil=SystemClock.uptimeMillis()+15_000;speech.speak(MainActivity.cleanSpeech(text),expression,id);};
        if(speech!=null&&ttsReady)say.run();else{pendingRemind=say;ensureVoice();
            // Without a voice in a minute, the reminder stays as text and in the notification.
            ui.postDelayed(()->{if(pendingRemind==say){pendingRemind=null;remindId=null;ui.postDelayed(hideBubble,30_000);}},60_000);}
    }
    void listen(){
        if(face==null||!talking)return;
        mode=Mode.LISTENING;face.setExpression("listening");face.setLevel(0);
        if(recognizer==null){recognizer=SpeechRecognizer.createSpeechRecognizer(this);recognizer.setRecognitionListener(this);}
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE,MiloVoice.locale(prefs).toLanguageTag()).putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true).putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1)
            .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,getPackageName());
        showDots();muteBeep();
        try{recognizer.startListening(i);}catch(RuntimeException e){end("Speech recognition could not start.");}
    }
    @Override public void onReadyForSpeech(Bundle b){ui.removeCallbacks(unmute);ui.postDelayed(unmute,400);}
    @Override public void onBeginningOfSpeech(){}
    @Override public void onRmsChanged(float rms){if(face!=null)face.setLevel((rms+2)/12f);}
    @Override public void onBufferReceived(byte[] b){}
    @Override public void onEndOfSpeech(){if(mode==Mode.LISTENING&&face!=null)face.setExpression("thinking");}
    @Override public void onEvent(int e,Bundle b){}
    @Override public void onPartialResults(Bundle b){}
    @Override public void onResults(Bundle b){if(mode!=Mode.LISTENING)return;String words=first(b);Log.i(TAG,"heard "+words.length()+" chars");if(words.isEmpty())missed();else respond(words);}
    @Override public void onError(int error){
        Log.i(TAG,"listen error "+error+" in "+mode+", misses="+misses);
        if(mode!=Mode.LISTENING)return;
        switch(error){
            case SpeechRecognizer.ERROR_NO_MATCH:case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:case SpeechRecognizer.ERROR_CLIENT:missed();break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:ui.postDelayed(this::listen,500);break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:end("Allow the microphone in Milo, then tap me again.");openApp(true);break;
            case SpeechRecognizer.ERROR_NETWORK:case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:case SpeechRecognizer.ERROR_SERVER:case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE:
                end(MiloVoice.locale(prefs).getLanguage().equals("ml")?"Malayalam listening needs an internet connection. Check it and tap me again.":"Listening needs your speech service; check the connection and tap me again.");break;
            default:end("Listening stopped. Tap me to try again.");
        }
    }
    static String first(Bundle b){ArrayList<String> r=b==null?null:b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);return r==null||r.isEmpty()||r.get(0)==null?"":r.get(0).trim();}
    /** Silence: keep listening a little longer before ending, so there is time to think of what to say. */
    void missed(){if(++misses>=3)end("Tap me when you want to talk.");else ui.postDelayed(this::listen,150);}

    void respond(String prompt){
        mode=Mode.THINKING;misses=0;face.setExpression("thinking");showDots();
        final int id=++turn;spoken=0;tone=null;ToneTag.reset();if(speech!=null){speech.stop();speech.beginReply();}
        AiProvider provider=AiProvider.current(this);String key;
        try{key=KeyVault.loadForProvider(this,provider.id());}catch(Exception e){end("Open Milo → Setup to unlock your AI key.");return;}
        if(provider.needsNetwork()){
            String today=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());int used=prefs.getString("usage_day","").equals(today)?prefs.getInt("usage",0):0;
            if(used>=30){end("You’ve reached Milo’s 30-attempt daily cloud limit.");return;}prefs.edit().putString("usage_day",today).putInt("usage",used+1).apply();
        }
        List<JSONObject> past=new ArrayList<>(history);String name=name();
        String context=voiceContext(),language=language();
        worker.execute(()->{
            try{
                MainActivity.GeminiReply r=provider.chat(key,name,language,prefs.getString("practice_language",""),prefs.getString("companion_style","Gentle"),prompt,past,context,false,partial->ui.post(()->{
                    if(id!=turn||mode==Mode.IDLE)return;if(showWords())setBubble(name+": "+partial);
                    int end=MainActivity.sentenceBoundary(partial,spoken);if(end>spoken){speak(partial.substring(spoken,end),"chunk"+id,ToneTag.toneAt(spoken));spoken=end;}
                }));
                ui.post(()->{
                    if(id!=turn||mode==Mode.IDLE)return;
                    try{history.add(MainActivity.turn("user",prompt));history.add(MainActivity.turn("model",r.text));while(history.size()>16){history.remove(0);history.remove(0);}}catch(Exception ignored){}
                    tone=r.tone;toneUntil=SystemClock.uptimeMillis()+10_000;if(showWords())setBubble(name+": "+r.text);else setBubble(null);
                    if(!ttsReady){afterSpeaking();return;}
                    mode=Mode.SPEAKING;String rest=r.text.substring(Math.min(spoken,r.text.length()));
                    if(rest.trim().isEmpty())speech.mark("final"+id);else speak(rest,"final"+id,ToneTag.toneAt(spoken)!=null?ToneTag.toneAt(spoken):r.tone);
                });
            }catch(Exception e){
                String message=e instanceof MainActivity.ApiFailure?e.getMessage():"I couldn’t reply. Tap me to try again.";
                ui.post(()->{if(id!=turn||"Stopped.".equals(message))return;face.setExpression("concerned");end(message);});
            }
        });
    }
    /** Whether Milo's words appear in the bubble while she speaks (Setup → Floating pet). */
    boolean showWords(){return prefs.getBoolean("pet_words",true);}

    /* ---- listening and thinking dots, and a quiet start without the recognizer's beep ---- */
    int dotFrame;
    final Runnable dots=new Runnable(){public void run(){
        if(bubble==null||(mode!=Mode.LISTENING&&mode!=Mode.THINKING))return;
        String[] frames={"●  ○  ○","○  ●  ○","○  ○  ●","○  ●  ○"};dotFrame=(dotFrame+1)%frames.length;
        setBubble(frames[dotFrame]);bubble.setContentDescription(mode==Mode.LISTENING?"Listening":"Thinking");ui.postDelayed(this,320);}};
    void showDots(){ui.removeCallbacks(dots);dotFrame=-1;dots.run();}
    static final int[] BEEP_STREAMS={android.media.AudioManager.STREAM_MUSIC,android.media.AudioManager.STREAM_SYSTEM};
    boolean beepMuted;
    /** Google's recognizer beeps each time listening starts; Milo mutes that moment and restores the volume right after. */
    void muteBeep(){
        if(!beepMuted){beepMuted=true;android.media.AudioManager am=getSystemService(android.media.AudioManager.class);
            for(int s:BEEP_STREAMS)try{am.adjustStreamVolume(s,android.media.AudioManager.ADJUST_MUTE,0);}catch(RuntimeException ignored){}}
        ui.removeCallbacks(unmute);ui.postDelayed(unmute,1500);
    }
    final Runnable unmute=()->{if(!beepMuted)return;beepMuted=false;android.media.AudioManager am=getSystemService(android.media.AudioManager.class);
        for(int s:BEEP_STREAMS)try{am.adjustStreamVolume(s,android.media.AudioManager.ADJUST_UNMUTE,0);}catch(RuntimeException ignored){}};
    String language(){return MiloVoice.locale(prefs).getDisplayLanguage(Locale.ENGLISH);}
    String voiceContext(){return "Current mood: "+prefs.getString("mood","Okay")+". Interests: "+prefs.getString("interests","")+". Current goal: "+prefs.getString("goal","")+". Hands-free voice chat through the floating pet: keep casual answers short, but tell full stories and full motivational talks when asked.";}
    void speak(String words,String utterance,String chunkTone){
        if(!ttsReady||speech==null)return;mode=Mode.SPEAKING;
        speech.speak(MainActivity.cleanSpeech(words),chunkTone!=null?chunkTone:tone,utterance);
    }
    void afterSpeaking(){
        if(mode==Mode.IDLE||face==null)return;face.setTalking(false);
        face.setExpression(tone!=null?tone:idleExpression());
        if(talking)ui.postDelayed(()->{if(mode!=Mode.IDLE&&talking)listen();},300);
    }
    /** Tapping while Milo thinks or speaks stops that reply and listens again. */
    void interrupt(){turn++;LiteRtLocalAi.cancel();if(speech!=null)speech.stop();if(face!=null)face.setTalking(false);listen();}
    void say(String message){setBubble(message);face.setExpression("concerned");ui.postDelayed(hideBubble,6000);ui.postDelayed(()->{if(mode==Mode.IDLE&&face!=null)face.setExpression(idleExpression());},2500);}
    void end(String message){
        boolean generating=mode==Mode.THINKING||mode==Mode.SPEAKING;
        mode=Mode.IDLE;talking=false;turn++;
        if(recognizer!=null)recognizer.cancel();if(speech!=null)speech.stop();if(face!=null)face.setTalking(false);if(generating)LiteRtLocalAi.cancel();
        if(face!=null){face.setLevel(0);face.setExpression(idleExpression());}
        ui.removeCallbacks(unmute);unmute.run();setBubble(message);ui.postDelayed(hideBubble,6000);goForeground(false);
    }

    @Override public void onDestroy(){
        visible=false;talking=false;current=null;turn++;ui.removeCallbacksAndMessages(null);unmute.run();
        if(prefs!=null)prefs.unregisterOnSharedPreferenceChangeListener(prefsChanged);
        if(recognizer!=null){recognizer.cancel();recognizer.destroy();recognizer=null;}
        if(speech!=null){speech.shutdown();speech=null;}
        if(mode!=Mode.IDLE)LiteRtLocalAi.cancel();worker.shutdownNow();
        if(windows!=null)for(View v:new View[]{face,bubble,target})if(v!=null)try{windows.removeView(v);}catch(Exception ignored){}
        face=null;bubble=null;target=null;stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();
    }
}
