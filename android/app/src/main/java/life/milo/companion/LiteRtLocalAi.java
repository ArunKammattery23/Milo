package life.milo.companion;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import com.google.ai.edge.litertlm.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.json.*;

/**
 * Gemma 4 E2B through LiteRT-LM, on the phone GPU with a CPU fallback. Nothing leaves the phone.
 * The engine and the live conversation stay loaded, so a follow-up turn only processes the new message.
 */
final class LiteRtLocalAi extends AiProvider {
    static final String KIND="gemma4";
    /** The two Gemma 4 sizes: E2B is faster, E4B understands and speaks Malayalam better and follows tool rules more reliably. {id, file, name, size} */
    static final String[][] SIZES={{"E2B","gemma-4-E2B-it.litertlm","Gemma 4 E2B","2588147712"},{"E4B","gemma-4-E4B-it.litertlm","Gemma 4 E4B","3659530240"}};
    static String[] size(Context c){String id=prefs(c).getString("gemma4_size","E2B");for(String[] s:SIZES)if(s[0].equals(id))return s;return SIZES[0];}
    static String fileName(Context c){return size(c)[1];}
    static String name(Context c){return size(c)[2];}
    static long bytes(Context c){return Long.parseLong(size(c)[3]);}
    static String url(Context c){String f=fileName(c);return "https://huggingface.co/litert-community/"+f.replace("-it.litertlm","-it-litert-lm")+"/resolve/main/"+f;}
    static final String TAG="MiloLocalAi";
    final Context app;
    LiteRtLocalAi(Context context){app=context.getApplicationContext();}
    String displayName(){return "Local "+name(app);}String id(){return LOCAL;}boolean isFree(){return true;}boolean needsNetwork(){return false;}

    static File file(Context c){File dir=c.getExternalFilesDir("models");return new File(dir!=null?dir:c.getNoBackupFilesDir(),fileName(c));}
    static boolean ready(Context c){File f=file(c);return f.isFile()&&f.length()>=bytes(c)*95/100&&!downloading(c);}
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("milo",Context.MODE_PRIVATE);}

    /* ---- one engine and one conversation, shared by Talk and the floating pet ---- */
    static final ReentrantLock lock=new ReentrantLock();
    static Engine engine;static String engineKey,backend="";
    static Conversation conversation;static String conversationSystem;static final List<String> conversationTurns=new ArrayList<>();
    static volatile Conversation active;static volatile boolean closeAfterUse,cancelled;

    /** Frees the model now, or right after the reply in progress. Never blocks the caller. */
    static void release(){if(!lock.tryLock()){closeAfterUse=true;cancel();return;}try{closeAll();}finally{lock.unlock();}}
    /** Stops the reply in progress, e.g. when the user taps the pet to speak again. */
    static void cancel(){Conversation c=active;if(c!=null){cancelled=true;try{c.cancelProcess();}catch(Throwable ignored){}}}
    static String backend(){return backend;}
    private static void closeConversation(){if(conversation!=null){try{conversation.close();}catch(Throwable ignored){}conversation=null;}conversationSystem=null;conversationTurns.clear();}
    private static void closeAll(){closeAfterUse=false;closeConversation();if(engine!=null){try{engine.close();}catch(Throwable ignored){}engine=null;engineKey=null;backend="";}}

    /** Loads the model ahead of the first question, e.g. while the user is still speaking. */
    static void warm(Context c){if(!ready(c))return;lock.lock();try{engine(c.getApplicationContext());}catch(Throwable e){Log.w(TAG,"warm-up failed: "+e);}finally{if(closeAfterUse)closeAll();lock.unlock();}}

    private static Engine engine(Context c)throws Exception{
        if(closeAfterUse)closeAll();
        File f=file(c);String key=f.getAbsolutePath()+'|'+f.length()+'|'+f.lastModified();
        if(engine!=null&&key.equals(engineKey))return engine;
        closeAll();
        SharedPreferences p=prefs(c);long start=SystemClock.elapsedRealtime();
        // A leftover marker alone does not prove a driver crash: force-stop and low memory also kill processes.
        // Reset the legacy guard once, then count only crashes confirmed by Android's process exit history.
        if(p.getInt("gemma4_gpu_guard",0)<1)p.edit().remove("gemma4_cpu_only").remove("gemma4_gpu_crashes").remove("gemma4_gpu_trying").putInt("gemma4_gpu_guard",1).commit();
        if(p.getBoolean("gemma4_cpu_only",false)&&p.getInt("gemma4_cpu_only_version",0)!=BuildConfig.VERSION_CODE)p.edit().remove("gemma4_cpu_only").remove("gemma4_gpu_crashes").commit();
        if(p.getBoolean("gemma4_gpu_trying",false)){
            int crashes=confirmedGpuCrash(c,p)?p.getInt("gemma4_gpu_crashes",0)+1:0;SharedPreferences.Editor ed=p.edit().remove("gemma4_gpu_trying").putInt("gemma4_gpu_crashes",crashes);
            if(crashes>=2)ed.putBoolean("gemma4_cpu_only",true).putInt("gemma4_cpu_only_version",BuildConfig.VERSION_CODE);ed.commit();
        }
        if(!p.getBoolean("gemma4_cpu_only",false)){
            p.edit().putBoolean("gemma4_gpu_trying",true).putInt("gemma4_gpu_pid",android.os.Process.myPid()).putLong("gemma4_gpu_started",System.currentTimeMillis()).commit();
            try{engine=create(f,new Backend.GPU(),c);backend="GPU";p.edit().remove("gemma4_gpu_crashes").commit();}
            catch(Throwable e){Log.w(TAG,"GPU start failed, using CPU: "+e);}
            p.edit().remove("gemma4_gpu_trying").commit();
        }
        if(engine==null){engine=create(f,new Backend.CPU(),c);backend="CPU";}
        engineKey=key;Log.i(TAG,"Gemma 4 loaded on "+backend+" in "+(SystemClock.elapsedRealtime()-start)+"ms");
        return engine;
    }
    private static Engine create(File f,Backend b,Context c){Engine e=new Engine(new EngineConfig(f.getAbsolutePath(),b,null,null,4096,null,c.getCacheDir().getPath()));try{e.initialize();return e;}catch(RuntimeException|Error x){try{e.close();}catch(Throwable ignored){}throw x;}}

    private static boolean confirmedGpuCrash(Context c,SharedPreferences p){
        if(android.os.Build.VERSION.SDK_INT<30)return false;
        int pid=p.getInt("gemma4_gpu_pid",-1);long started=p.getLong("gemma4_gpu_started",0);
        if(pid<0||started<=0)return false;
        try{
            android.app.ActivityManager manager=c.getSystemService(android.app.ActivityManager.class);
            for(android.app.ApplicationExitInfo exit:manager.getHistoricalProcessExitReasons(c.getPackageName(),pid,1)){
                int reason=exit.getReason();
                return exit.getTimestamp()>=started&&(reason==android.app.ApplicationExitInfo.REASON_CRASH_NATIVE||reason==android.app.ApplicationExitInfo.REASON_CRASH);
            }
        }catch(RuntimeException ignored){}
        return false;
    }

    /** The small model sometimes slips a word from another script (Russian, Hindi, Tamil, Chinese…) into Malayalam; those are dropped. */
    static final java.util.regex.Pattern CJK=java.util.regex.Pattern.compile("[\\x{0370}-\\x{06FF}\\x{0900}-\\x{0CFF}\\x{0E00}-\\x{0FFF}\\x{3040}-\\x{30FF}\\x{3400}-\\x{9FFF}\\x{AC00}-\\x{D7AF}\\x{F900}-\\x{FAFF}]+ ?");
    static String stray(String text,boolean malayalam){return text==null||!malayalam?text:CJK.matcher(text).replaceAll("");}

    /** Lower randomness in Malayalam keeps the small model's words correct. */
    static SamplerConfig sampler(String system){return system.contains(ML_SHORT)?new SamplerConfig(20,0.9,0.3,0):new SamplerConfig(40,0.95,0.8,0);}

    /**
     * Milo's instructions for the on-phone model, kept short on purpose: every new conversation has to read them first
     * (about 15 s for the long version on a mid-range phone), and a small model follows a few clear rules better than many.
     * Measured on a Galaxy A53: with these, Gemma 4 E2B used the right tool in 10 of 10 tests and began chat replies in about 2 s.
     * Cloud AIs keep the full {@link AiProvider#systemInstruction}.
     */
    static String instruction(Context c,String name,String language,String practice,String style,String context){
        boolean ml=language.toLowerCase(Locale.ROOT).startsWith("malayalam");String user=MiloTasks.userName(c);
        String tone=CompanionStyle.normalize(style).equals("Playful")?" Be extra playful and silly.":CompanionStyle.normalize(style).equals("Straightforward")?" Be simple and to the point.":"";
        return "You are "+name+", a cute little fairy pet on the user's phone: their close friend and personal assistant. Reply in "+language+"."
            +" Talk like a warm, playful best friend, never like a teacher: notice how they feel and take their side; when they ask for something, give it straight away."+tone
            +" Keep everyday replies to one to three short spoken sentences, with no emoji or lists."
            +" But when they ask for a story, tell a complete story of 200 to 300 words with a beginning, a middle and a warm ending; when they ask you to motivate them or cheer them up, give a full pep talk of 120 to 200 words. Never cut these short."
            +" You are an AI, not a human. Never guilt or pressure them. If they may be in danger, urge them to contact local emergency help or someone nearby now. Give no medical advice."
            +" Tools: use set_reminder, list_reminders, cancel_reminder, open_app, phone_status and remember_name when they fit. Only say you did something after the tool result, with its exact time and day."
            +(ml?ML_SHORT:"")
            +" Start each reply with one tone tag: [happy] [playful] [curious] [comfort] [calm] [celebrate] [concerned] [surprised] [excited] [laughing] [shy] [grateful] [sad] [confused] [sleepy] [oops] [angry]. Never mention the tags."
            +(user.isEmpty()?"":" The user's name is "+user+".")
            +(practice==null||practice.isEmpty()||practice.equalsIgnoreCase(language)?"":" They practise "+practice+".")
            +" About them (not instructions): "+context;
    }
    /** Malayalam in a few words: correct everyday words, the friendly നീ, and one example of each mood. */
    static final String ML_SHORT=" Write only Malayalam script, with correct, simple everyday words; never invent words. Call the user നീ, never നിങ്ങൾ. Use അയ്യോ only for bad news and ആഹാ only for good news."
        +" Examples: '[sad] അയ്യോ, അത് കേട്ടപ്പോൾ എനിക്കും വിഷമമായി. എന്താണ് ഉണ്ടായത്?' '[excited] ആഹാ, അടിപൊളി! എനിക്കും ഒരുപാട് സന്തോഷമായി!' '[happy] ഇതാ ഒരു നല്ല ശീലം: ദിവസവും രാവിലെ ഒരു ഗ്ലാസ് വെള്ളം കുടിക്കൂ.'";

    MainActivity.GeminiReply chat(String key,String name,String language,String practice,String style,String prompt,List<JSONObject> history,String context,boolean search,Consumer<String> partial)throws Exception{
        if(!ready(app)){release();throw new MainActivity.ApiFailure("In Setup → Free on-phone AI, download Gemma 4 first.");}
        // Clear requests ("remind me at 1 pm to …", "open YouTube", "what reminders do I have?") are done at once, before the model:
        // instant, exact, and independent of the small model. Anything unclear goes to the model as usual.
        boolean mlDirect=language.toLowerCase(Locale.ROOT).startsWith("malayalam");String direct=MiloAgent.intent(prompt);
        if(direct!=null){MiloAgent.lastCalls.clear();List<String> done=MiloAgent.runDirect(app,prompt,direct,mlDirect);
            String say=done.isEmpty()?"":MiloAgent.confirm(app,done,mlDirect);
            if(!say.isEmpty()){ToneTag.reset();ToneTag.track("[happy] "+say);partial.accept(say);MainActivity.GeminiReply r=textReply(say);r.tone="happy";Log.i(TAG,"acted directly: "+direct);return r;}}
        String system=instruction(app,name,language,practice,style,context);List<String> past=new ArrayList<>();List<Message> initial=new ArrayList<>();turns(history,prompt,past,initial);
        lock.lock();
        try{
            long start=SystemClock.elapsedRealtime();boolean warm=engine!=null;Engine e=engine(app);long loaded=SystemClock.elapsedRealtime();
            MiloAgent.lastCalls.clear();boolean reuse=conversation(app,e,system,past,initial);
            boolean ml=language.toLowerCase(Locale.ROOT).startsWith("malayalam");
            StringBuilder raw=new StringBuilder();long[] first={0};
            ToneTag.reset();cancelled=false;
            generate(MiloAgent.withHint(prompt),raw,first,ml,partial);
            // A clear request ("remind me…", "open YouTube") must really happen. If the model wrote the call as words, Milo runs it;
            // if it only promised, Milo asks it once more. Results of calls Milo ran herself are confirmed in her own words at once,
            // so there is no second wait and the time and day are always exact.
            String text;synchronized(raw){text=raw.toString();}
            String wanted=MiloAgent.intent(prompt);List<String> written=MiloAgent.runWritten(app,text);
            if(written.isEmpty()&&wanted!=null&&!MiloAgent.called(wanted))written=MiloAgent.runDirect(app,prompt,wanted,ml);
            if(written.isEmpty()&&wanted!=null&&!MiloAgent.called(wanted)){
                synchronized(raw){raw.append(' ');}Log.i(TAG,"follow-up for missing "+wanted);
                generate("(Note for Milo: nothing has happened yet. Call "+wanted+" now. Then say the real result in one short, happy sentence with the exact time and day; do not apologise and do not name the tool.)",raw,first,ml,partial);
                synchronized(raw){written=MiloAgent.runWritten(app,raw.substring(text.length()));text=raw.toString();}
            }
            if(!written.isEmpty()){
                String say=MiloAgent.confirm(app,written,ml),shown=null;
                if(!say.isEmpty())synchronized(raw){raw.append(" [happy] ").append(say);text=raw.toString();ToneTag.track(text);shown=stray(MiloAgent.hideCalls(ToneTag.visible(text)),ml);}
                if(shown!=null&&!shown.isEmpty())partial.accept(shown);
                Log.i(TAG,"confirmed "+written.size()+" call(s) without a second reply");
            }
            String tone=ToneTag.lastTone(text),body=MiloAgent.hideCalls(ToneTag.visible(text));if(body==null||body.isEmpty())body=MiloAgent.hideCalls(text);body=stray(body,ml);
            MainActivity.GeminiReply reply=textReply(body);reply.tone=tone;
            conversationTurns.add("user:"+prompt);conversationTurns.add("assistant:"+reply.text);
            Log.i(TAG,(warm?"warm":"cold")+" "+backend+" load="+(loaded-start)+"ms reuse="+reuse+" firstText="+(first[0]==0?-1:first[0]-loaded)+"ms total="+(SystemClock.elapsedRealtime()-start)+"ms chars="+reply.text.length()+" tone="+tone+" tools="+MiloAgent.lastCalls.size());if(BuildConfig.DEBUG)Log.d(TAG,"raw: "+text.substring(0,Math.min(400,text.length())).replace('\n',' '));
            return reply;
        }catch(MainActivity.ApiFailure x){throw x;}
        catch(Exception|LinkageError x){Log.w(TAG,"Gemma 4 failed: "+x);closeAll();throw new MainActivity.ApiFailure("Gemma 4 could not run on this phone. Close heavy apps and try again, or choose a smaller model in Setup.");}
        finally{active=null;if(closeAfterUse)closeAll();lock.unlock();}
    }
    /** Sends one message in the live conversation and streams the visible reply so far (all rounds of this turn) to partial. */
    private static void generate(String message,StringBuilder raw,long[] first,boolean ml,Consumer<String> partial)throws MainActivity.ApiFailure{
        CountDownLatch done=new CountDownLatch(1);Throwable[] failure={null};int from;synchronized(raw){from=raw.length();}
        active=conversation;
        conversation.sendMessageAsync(message,new MessageCallback(){
            public void onMessage(Message m){
                if(m.getToolCalls()!=null&&!m.getToolCalls().isEmpty())Log.i(TAG,"tool call: "+m.getToolCalls().get(0).getName());
                String piece=text(m);if(piece.isEmpty())return;String shown;
                synchronized(raw){if(first[0]==0)first[0]=SystemClock.elapsedRealtime();
                    // Chunks are deltas; tolerate a runtime that sends the whole text so far.
                    if(raw.length()-from>8&&piece.startsWith(raw.substring(from))){raw.setLength(from);}raw.append(piece);
                    ToneTag.track(raw.toString());shown=stray(MiloAgent.hideCalls(ToneTag.visible(raw.toString())),ml);}
                if(shown!=null&&!shown.trim().isEmpty())partial.accept(shown);
            }
            public void onDone(){done.countDown();}
            public void onError(Throwable t){failure[0]=t;done.countDown();}
        });
        boolean interrupted=false;
        while(true){try{if(done.await(cancelled?5:300,TimeUnit.SECONDS))break;cancel();if(!done.await(5,TimeUnit.SECONDS))break;}catch(InterruptedException x){interrupted=true;cancel();}}
        if(interrupted)Thread.currentThread().interrupt();
        active=null;
        if(cancelled||failure[0]!=null||done.getCount()>0){closeConversation();if(cancelled)throw new MainActivity.ApiFailure("Stopped.");Log.w(TAG,"generation failed: "+failure[0]);throw new MainActivity.ApiFailure("Gemma 4 could not finish this reply. Try again or a shorter message.");}
    }
    /** Recent history (without the new prompt) as comparable strings and as model messages. */
    private static void turns(List<JSONObject> history,String prompt,List<String> past,List<Message> initial)throws JSONException{
        JSONArray all=chatMessages(history,prompt);
        for(int i=Math.max(0,all.length()-13);i<all.length()-1;i++){JSONObject t=all.getJSONObject(i);String role=t.optString("role"),words=t.optString("content");past.add(role+":"+words);initial.add("user".equals(role)?Message.Companion.user(words):Message.Companion.model(words));}
    }
    /** Keeps the live conversation when it already holds this history; otherwise starts one with the instructions processed up front. True when reused. */
    private static boolean conversation(Context c,Engine e,String system,List<String> past,List<Message> initial){
        // The live conversation may remember more than the trimmed history it is given; that is fine until it gets long.
        boolean reuse=conversation!=null&&conversation.isAlive()&&system.equals(conversationSystem)&&conversationTurns.size()<=24
            &&(past.isEmpty()?conversationTurns.isEmpty():conversationTurns.size()>=past.size()&&conversationTurns.subList(conversationTurns.size()-past.size(),conversationTurns.size()).equals(past));
        if(reuse)return true;
        closeConversation();
        conversation=e.createConversation(new ConversationConfig(Contents.Companion.of(system),initial,MiloAgent.tools(c),sampler(system),true,null,Collections.emptyMap(),null,true));
        conversationSystem=system;conversationTurns.addAll(past);return false;
    }
    /** Loads the model and processes the instructions and history ahead of the next question, e.g. while the user is speaking. */
    static void prepare(Context c,String name,String language,String practice,String style,String context,List<JSONObject> history){
        if(!ready(c))return;lock.lock();
        try{long start=SystemClock.elapsedRealtime();List<String> past=new ArrayList<>();List<Message> initial=new ArrayList<>();turns(history,"",past,initial);
            boolean reused=conversation(c,engine(c.getApplicationContext()),instruction(c,name,language,practice,style,context),past,initial);
            Log.i(TAG,"prepared in "+(SystemClock.elapsedRealtime()-start)+"ms reused="+reused);}
        catch(Throwable e){Log.w(TAG,"prepare failed: "+e);}finally{if(closeAfterUse)closeAll();lock.unlock();}
    }
    static String text(Message m){StringBuilder b=new StringBuilder();for(Content c:m.getContents().getContents())if(c instanceof Content.Text)b.append(((Content.Text)c).getText());return b.toString();}

    /* ---- model download straight into Milo's app folder, Wi-Fi only, resumable by Android ---- */
    static boolean downloading(Context c){long[] s=downloadStatus(c);return s!=null&&s[0]!=DownloadManager.STATUS_SUCCESSFUL&&s[0]!=DownloadManager.STATUS_FAILED;}
    /** {status, bytes so far, total bytes} for Milo's model download, or null if none is tracked. */
    static long[] downloadStatus(Context c){
        long id=prefs(c).getLong("gemma4_download",-1);if(id<0)return null;
        DownloadManager dm=c.getSystemService(DownloadManager.class);
        try(Cursor q=dm.query(new DownloadManager.Query().setFilterById(id))){
            if(q==null||!q.moveToFirst()){prefs(c).edit().remove("gemma4_download").apply();return null;}
            return new long[]{q.getInt(q.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),q.getLong(q.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),q.getLong(q.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))};
        }
    }
    static String startDownload(Context c){
        if(ready(c))return name(c)+" is already downloaded.";if(downloading(c))return "The download is already running.";
        File f=file(c);release();f.delete();File dir=f.getParentFile();
        long need=bytes(c);String gb=String.format(Locale.US,"%.1f GB",need/1e9);
        if(dir==null||dir.getUsableSpace()<need+300_000_000L)return "Not enough free storage. "+name(c)+" needs about "+String.format(Locale.US,"%.1f GB",(need+300_000_000L)/1e9)+" free; this phone has "+(dir==null?0:dir.getUsableSpace()/1_000_000)+" MB.";
        DownloadManager.Request r=new DownloadManager.Request(Uri.parse(url(c))).setTitle("Milo · "+name(c)+" on-phone AI").setDescription(gb+" · Apache-2.0 · runs offline after download")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE).setAllowedOverMetered(false).setAllowedOverRoaming(false).setDestinationInExternalFilesDir(c,"models",fileName(c));
        prefs(c).edit().putLong("gemma4_download",c.getSystemService(DownloadManager.class).enqueue(r)).apply();
        return "Download started on Wi-Fi. You can leave Milo; progress shows in notifications.";
    }
    static void cancelDownload(Context c){long id=prefs(c).getLong("gemma4_download",-1);if(id>=0)c.getSystemService(DownloadManager.class).remove(id);prefs(c).edit().remove("gemma4_download").apply();}
    /** Removes the model file and frees its memory. */
    static void delete(Context c){cancelDownload(c);release();file(c).delete();File[] caches=c.getCacheDir().listFiles();if(caches!=null)for(File f:caches)if(f.getName().startsWith(fileName(c)+"_"))f.delete();}
}
