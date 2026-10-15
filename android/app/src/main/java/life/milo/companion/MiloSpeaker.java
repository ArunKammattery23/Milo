package life.milo.companion;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.*;
import android.os.*;
import android.speech.tts.*;
import android.util.Log;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Milo's voice: renders each sentence with the chosen character and emotion (phone voice or on-phone AI voice),
 * adds effects, plays it while reporting loudness for lip sync, and prepares the next sentence while one plays.
 */
final class MiloSpeaker {
    interface Listener{void onStart(String id,String tone);void onDone(String id);void onLevel(float level);}
    static final String TAG="MiloSpeaker";

    private final Context app;private final SharedPreferences prefs;private final Listener listener;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ExecutorService render=Executors.newSingleThreadExecutor(),play=Executors.newSingleThreadExecutor();
    private final AtomicInteger generation=new AtomicInteger(),pending=new AtomicInteger();
    private final Map<String,CountDownLatch> synthesis=new ConcurrentHashMap<>();
    private final Set<String> failed=ConcurrentHashMap.newKeySet();
    private TextToSpeech tts;private volatile boolean phoneOk,ready,leadNext,closed;
    /** What renders speech right now: "phone" or "ai:kokoro:af_heart" / "ai:meera". */
    private volatile String engine="phone";
    private AudioTrack track;private int trackRate;private AudioFocusRequest focus;

    MiloSpeaker(Context context,SharedPreferences prefs,Listener listener,Runnable onReady){
        app=context.getApplicationContext();this.prefs=prefs;this.listener=listener;
        tts=MiloVoice.create(app,status->{phoneOk=status==TextToSpeech.SUCCESS;refresh(onReady);});
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
            public void onStart(String id){}
            public void onDone(String id){CountDownLatch l=synthesis.get(id);if(l!=null)l.countDown();}
            @Override public void onError(String id){failed.add(id);CountDownLatch l=synthesis.get(id);if(l!=null)l.countDown();}
            @Override public void onError(String id,int code){onError(id);}
        });
    }
    boolean ready(){return ready;}
    /** Hands work to a queue unless the speaker has shut down (e.g. the screen closed mid-sentence). */
    private void run(ExecutorService queue,Runnable task){if(closed)return;try{queue.execute(task);}catch(RejectedExecutionException ignored){}}
    String engine(){return engine;}
    TextToSpeech phone(){return tts;}

    /** Re-reads language, character and voice; runs after any queued speech. */
    void refresh(Runnable after){run(render,()->{configure();if(after!=null)ui.post(after);});}

    private void configure(){
        Locale locale=MiloVoice.locale(prefs);MiloVoice.Character ch=MiloVoice.character(prefs);boolean ml="ml".equals(locale.getLanguage());
        String want=prefs.getString("voice_name_"+locale.getLanguage(),"");
        boolean phoneLang=false;
        if(phoneOk)try{phoneLang=tts.setLanguage(locale)>=0;}catch(RuntimeException ignored){}
        String chosen="phone";
        if(want.startsWith("ai:kokoro")&&!ml&&NeuralVoices.installed(app,NeuralVoices.KOKORO))chosen=want;
        else if(want.equals("ai:meera")&&ml&&NeuralVoices.installed(app,NeuralVoices.MEERA))chosen=want;
        else if(want.isEmpty()){
            // Automatic picks only voices fast enough for live talk: Meera for Malayalam girl characters, else the phone voice
            // closest to the character's gender. Kokoro (about 1.6x slower than real time on a Galaxy S23) is opt-in.
            if(ml&&"f".equals(ch.gender)&&NeuralVoices.installed(app,NeuralVoices.MEERA))chosen="ai:meera";
        }
        if(chosen.startsWith("ai:")&&NeuralVoices.engine(app,chosen.startsWith("ai:kokoro")?NeuralVoices.KOKORO:NeuralVoices.MEERA)==null)chosen="phone";
        if(chosen.equals("phone")&&phoneLang){
            List<String> bases=MiloVoice.choices(tts,locale);String base=want.startsWith("phone:")?want.substring(6):"";
            if(!bases.contains(base))base=autoVoice(bases,locale,ch.gender);
            if(base!=null){Voice v=MiloVoice.resolve(tts,locale,base);if(v!=null)try{tts.setVoice(v);}catch(RuntimeException ignored){}}
        }
        engine=chosen;ready=!chosen.equals("phone")||phoneLang;
        Log.i(TAG,"voice "+engine+" character="+ch.id+" ready="+ready);
    }
    /** Picks the phone voice whose measured pitch best fits a girl or boy character; pitches are measured once and remembered. */
    private String autoVoice(List<String> bases,Locale locale,String gender){
        if(bases.isEmpty())return null;String best=null;float bestHz=0;
        for(String b:bases){
            float hz=prefs.getFloat("voice_f0_"+b,-1);
            if(hz<0){Voice v=MiloVoice.resolve(tts,locale,b);if(v==null)continue;try{tts.setVoice(v);}catch(RuntimeException e){continue;}
                tts.setPitch(1f);tts.setSpeechRate(1f);Clip probe=phoneClip(locale.getLanguage().equals("ml")?"നമസ്കാരം, എന്റെ പേര് മിലോ.":"Hello, my name is Milo.","probe-"+b);
                hz=probe==null?0:MiloVoice.pitchHz(probe.pcm,probe.rate);prefs.edit().putFloat("voice_f0_"+b,hz).apply();Log.i(TAG,b+" pitch "+hz+" Hz");}
            if(hz<=0)continue;
            boolean better=best==null||("f".equals(gender)?hz>bestHz:hz<bestHz);
            if(better){best=b;bestHz=hz;}
        }
        return best!=null?best:bases.get(0);
    }

    /** The next sentence spoken starts a reply, so it gets the emotion's little sound first (if enabled). */
    void beginReply(){leadNext=true;humanStarted=false;}

    /** Queues a sentence in an emotion; id is reported to the listener when that sentence finishes. */
    void speak(String text,String tone,String id){
        if(text==null||text.trim().isEmpty()){mark(id);return;}
        if(human()){
            // Human voice: each request costs quota, so after the first sentence, sentences in one tone go together.
            boolean last=id.contains("final");
            if(!humanStarted){humanStarted=true;flushBatch();queue(text,tone,id);return;}
            if(batch.length()>0&&!java.util.Objects.equals(tone,batchTone))flushBatch();
            if(batch.length()>0)batch.append(' ');batch.append(text);batchTone=tone;batchId=id;ui.removeCallbacks(batchTimer);
            if(last||batch.length()>=700)flushBatch();else ui.postDelayed(batchTimer,1500);
            return;
        }
        queue(text,tone,id);
    }
    private final StringBuilder batch=new StringBuilder();private String batchTone,batchId;private boolean humanStarted;
    private final Runnable batchTimer=this::flushBatch;
    private void flushBatch(){ui.removeCallbacks(batchTimer);if(batch.length()==0)return;String t=batch.toString();batch.setLength(0);queue(t,batchTone,batchId);}
    /** Human voice is on and usable: chosen in Voice Studio, a Gemini key is saved, and no recent quota error. */
    boolean human(){return prefs.getBoolean("voice_human",false)&&GeminiVoice.available(app);}
    /** Engine that rendered the last sentence: "human", "phone" or an AI voice id. */
    volatile String lastEngine="";
    private void queue(String text,String tone,String id){
        int gen=generation.get();boolean lead=leadNext;leadNext=false;pending.incrementAndGet();
        run(render,()->{
            if(gen!=generation.get()||closed){pending.decrementAndGet();return;}
            List<Clip> clips=new ArrayList<>();
            String sound=lead&&prefs.getBoolean("voice_sounds",true)?MiloVoice.interjection(tone,MiloVoice.malayalam(prefs)):null;
            Clip main;
            if(sound!=null&&human()){main=clip(sound+" "+text,tone,id);}
            else{if(sound!=null){Clip s=clip(sound,tone,id+"·lead");if(s!=null)clips.add(s);}main=clip(text,tone,id);}
            if(main!=null)clips.add(main);
            run(play,()->{try{for(Clip c:clips){if(gen!=generation.get()||closed)break;playClip(c,gen,id,tone);}}finally{finish(id,gen);}});
        });
    }
    /** Reports id once everything queued before it has been spoken. */
    void mark(String id){flushBatch();int gen=generation.get();pending.incrementAndGet();run(render,()->run(play,()->finish(id,gen)));}
    private void finish(String id,int gen){if(pending.decrementAndGet()<=0)releaseFocus();if(gen==generation.get()&&!closed)ui.post(()->{listener.onLevel(0);listener.onDone(id);});}

    /** Stops speaking now and drops anything queued. */
    void stop(){
        generation.incrementAndGet();leadNext=false;ui.removeCallbacks(batchTimer);batch.setLength(0);
        try{tts.stop();}catch(RuntimeException ignored){}
        for(CountDownLatch l:synthesis.values())l.countDown();
        AudioTrack t=track;if(t!=null)try{t.pause();t.flush();}catch(IllegalStateException ignored){}
        ui.post(()->listener.onLevel(0));
    }
    void shutdown(){closed=true;stop();render.shutdownNow();play.shutdownNow();try{tts.shutdown();}catch(RuntimeException ignored){}AudioTrack t=track;if(t!=null)try{t.release();}catch(RuntimeException ignored){}releaseFocus();}

    /* ---- rendering ---- */
    static final class Clip{final short[] pcm;final int rate;final float playback,volume;Clip(short[] pcm,int rate,float playback,float volume){this.pcm=pcm;this.rate=rate;this.playback=playback;this.volume=volume;}}

    /** Renders one sentence in the current character, voice and emotion (package-visible for device tests). */
    Clip clip(String text,String tone,String id){
        MiloVoice.Character ch=MiloVoice.character(prefs);float strength=prefs.getInt("voice_emotion",100)/100f;
        float[] e=MiloVoice.emotion(tone);
        float pitch=ch.pitch*(1+(e[0]-1)*strength),rate=ch.rate*(1+(e[1]-1)*strength),volume=Math.min(1,ch.volume*(1+(e[2]-1)*strength));
        long start=SystemClock.elapsedRealtime();Clip c=null;String engineNow=engine;
        if(human()){
            // Gemini acts the emotion itself, so no pitch shifting: the voice stays natural.
            short[] pcm=GeminiVoice.speak(app,prefs,text,tone);
            if(pcm!=null){c=new Clip(pcm,24000,1,Math.min(1,ch.volume*(1+(e[2]-1)*0.4f)));engineNow="human";}
        }
        if(c!=null){}
        else if(engineNow.startsWith("ai:")){
            // AI voices already sound like the character, so they get half the pitch change; resampled playback carries it.
            float aiPitch=1+(pitch-1)*0.5f;
            c=aiClip(text,engineNow,rate/aiPitch,aiPitch,volume);
        }else{
            try{tts.setPitch(pitch);tts.setSpeechRate(rate);}catch(RuntimeException ignored){}
            Clip raw=phoneClip(text,"milo-"+id+"-"+SystemClock.uptimeMillis());c=raw==null?null:new Clip(raw.pcm,raw.rate,1,volume);
        }
        if(c==null)return null;
        effects(c.pcm,c.rate,ch.effect);String extra=MiloVoice.emotionEffect(tone);if(extra!=null&&strength>0&&!engineNow.equals("human"))effects(c.pcm,c.rate,extra);lastEngine=engineNow;
        Log.i(TAG,engineNow+" rendered "+text.length()+" chars in "+(SystemClock.elapsedRealtime()-start)+"ms tone="+tone);
        return c;
    }
    private Clip aiClip(String text,String engineId,float speed,float playback,float volume){
        boolean kokoro=engineId.startsWith("ai:kokoro");com.k2fsa.sherpa.onnx.OfflineTts t=NeuralVoices.engine(app,kokoro?NeuralVoices.KOKORO:NeuralVoices.MEERA);
        if(t==null)return null;
        try{
            int sid=kokoro?NeuralVoices.kokoroId(engineId.substring("ai:kokoro:".length())):0;
            com.k2fsa.sherpa.onnx.GeneratedAudio a=t.generate(text,sid,Math.max(0.5f,Math.min(2f,speed)));
            float[] f=a.getSamples();short[] s=new short[f.length];for(int i=0;i<f.length;i++)s[i]=(short)Math.max(-32768,Math.min(32767,f[i]*32767));
            return new Clip(s,a.getSampleRate(),playback,volume);
        }catch(Throwable e){Log.w(TAG,"AI voice failed: "+e);return null;}
    }
    /** Phone voice to PCM through a temporary WAV file. */
    private Clip phoneClip(String text,String id){
        File f=new File(app.getCacheDir(),"milo-speech-"+Math.abs(id.hashCode())+".wav");CountDownLatch done=new CountDownLatch(1);synthesis.put(id,done);
        try{
            if(tts.synthesizeToFile(text,null,f,id)!=TextToSpeech.SUCCESS)return null;
            if(!done.await(20,TimeUnit.SECONDS)||failed.remove(id))return null;
            return readWav(f);
        }catch(Exception e){Log.w(TAG,"phone voice failed: "+e);return null;}
        finally{synthesis.remove(id);f.delete();}
    }
    static Clip readWav(File f)throws IOException{
        byte[] b;try(FileInputStream in=new FileInputStream(f)){b=new byte[(int)f.length()];int off=0,n;while(off<b.length&&(n=in.read(b,off,b.length-off))>0)off+=n;}
        if(b.length<44)return null;int rate=22050,channels=1,bits=16,pos=12,dataStart=-1,dataLen=0;
        while(pos+8<=b.length){
            String id=new String(b,pos,4,"US-ASCII");int len=(b[pos+4]&255)|(b[pos+5]&255)<<8|(b[pos+6]&255)<<16|(b[pos+7]&255)<<24;
            if(id.equals("fmt ")){channels=(b[pos+10]&255)|(b[pos+11]&255)<<8;rate=(b[pos+12]&255)|(b[pos+13]&255)<<8|(b[pos+14]&255)<<16|(b[pos+15]&255)<<24;bits=(b[pos+22]&255)|(b[pos+23]&255)<<8;}
            if(id.equals("data")){dataStart=pos+8;dataLen=Math.min(len<0?Integer.MAX_VALUE:len,b.length-dataStart);break;}
            pos+=8+len+(len&1);
        }
        if(dataStart<0||bits!=16)return null;
        int frames=dataLen/2/channels;short[] pcm=new short[frames];
        for(int i=0;i<frames;i++){int o=dataStart+i*2*channels;pcm[i]=(short)((b[o]&255)|(b[o+1]<<8));}
        return new Clip(pcm,rate,1,1);
    }

    /** Character and emotion effects, applied in place: fairy sparkle, robot, and the harsh edge of an angry voice. */
    static void effects(short[] s,int rate,String effect){
        if(effect==null||effect.equals("none")||s.length==0)return;
        float[] x=new float[s.length];for(int i=0;i<s.length;i++)x[i]=s[i]/32768f;
        switch(effect){
            case "sparkle":{int d=rate*55/1000;for(int i=x.length-1;i>=d;i--)x[i]+=0.2f*x[i-d];break;}
            case "robot":{int d=rate*18/1000;float[] y=x.clone();for(int i=0;i<x.length;i++){float ring=(float)Math.sin(2*Math.PI*58*i/rate);y[i]=0.55f*x[i]*ring+0.45f*x[i]+(i>=d?0.3f*x[i-d]:0);}x=y;break;}
            case "harsh":{float prev=0;for(int i=0;i<x.length;i++){float edge=x[i]-0.55f*prev;prev=x[i];float driven=(float)Math.tanh(3.2*(x[i]+0.35f*edge));x[i]=0.45f*x[i]+0.55f*driven*0.8f;}break;}
            default:return;
        }
        float peak=0;for(float v:x)peak=Math.max(peak,Math.abs(v));float g=peak>0.97f?0.97f/peak:1;
        for(int i=0;i<s.length;i++)s[i]=(short)Math.max(-32768,Math.min(32767,x[i]*g*32767));
    }

    /* ---- playback with lip sync ---- */
    private void playClip(Clip c,int gen,String id,String tone){
        if(c.pcm.length==0)return;
        holdFocus();
        if(track==null||trackRate!=c.rate){if(track!=null)track.release();trackRate=c.rate;
            int min=AudioTrack.getMinBufferSize(c.rate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
            track=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(c.rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setBufferSizeInBytes(Math.max(min,c.rate/5*2)).setTransferMode(AudioTrack.MODE_STREAM).build();}
        AudioTrack t=track;
        try{t.setPlaybackRate(Math.round(c.rate*c.playback));}catch(RuntimeException e){t.setPlaybackRate(c.rate);}
        t.setVolume(c.volume);t.play();
        int frame=c.rate/50;float[] env=new float[c.pcm.length/frame+1];
        for(int i=0;i<env.length;i++){double sum=0;int from=i*frame,to=Math.min(c.pcm.length,from+frame);for(int j=from;j<to;j++)sum+=(double)c.pcm[j]*c.pcm[j];
            double rms=to>from?Math.sqrt(sum/(to-from)):0;env[i]=(float)Math.max(0,Math.min(1,(20*Math.log10(rms/32768+1e-6)+42)/32));}
        int startHead=t.getPlaybackHeadPosition();ui.post(()->{if(gen==generation.get())listener.onStart(id,tone);});
        int written=0;
        while(written<c.pcm.length&&gen==generation.get()&&!closed){
            int n=t.write(c.pcm,written,Math.min(frame*2,c.pcm.length-written));if(n<=0)break;written+=n;reportLevel(t,startHead,env,frame,gen);
        }
        long deadline=SystemClock.uptimeMillis()+(long)(written*1000f/(c.rate*c.playback))+1500;
        while(gen==generation.get()&&!closed&&t.getPlaybackHeadPosition()-startHead<written&&SystemClock.uptimeMillis()<deadline){reportLevel(t,startHead,env,frame,gen);SystemClock.sleep(20);}
        if(gen!=generation.get()){try{t.pause();t.flush();}catch(IllegalStateException ignored){}}
        else t.stop();
    }
    private void reportLevel(AudioTrack t,int startHead,float[] env,int frame,int gen){
        int i=(t.getPlaybackHeadPosition()-startHead)/frame;float level=i>=0&&i<env.length?env[i]:0;
        ui.post(()->{if(gen==generation.get())listener.onLevel(level);});
    }
    private void holdFocus(){
        if(focus!=null)return;AudioManager am=app.getSystemService(AudioManager.class);
        focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).build();
        am.requestAudioFocus(focus);
    }
    private void releaseFocus(){AudioFocusRequest f=focus;focus=null;if(f!=null)app.getSystemService(AudioManager.class).abandonAudioFocusRequest(f);}
}
