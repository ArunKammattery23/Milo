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
    static final String KIND="gemma4",FILE="gemma-4-E2B-it.litertlm",NAME="Gemma 4 E2B";
    static final String URL="https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm";
    static final long SIZE=2_588_147_712L;
    static final String TAG="MiloLocalAi";
    final Context app;
    LiteRtLocalAi(Context context){app=context.getApplicationContext();}
    String displayName(){return "Local "+NAME;}String id(){return LOCAL;}boolean isFree(){return true;}boolean needsNetwork(){return false;}

    static File file(Context c){File dir=c.getExternalFilesDir("models");return new File(dir!=null?dir:c.getNoBackupFilesDir(),FILE);}
    static boolean ready(Context c){File f=file(c);return f.isFile()&&f.length()>2_000_000_000L&&!downloading(c);}
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
        // A GPU driver crash kills the process and leaves the flag behind. The app can also be closed for other reasons
        // during the GPU start, so only two interrupted starts in a row move Milo to the CPU, and a new app version tries the GPU again.
        if(p.getBoolean("gemma4_cpu_only",false)&&p.getInt("gemma4_cpu_only_version",0)!=BuildConfig.VERSION_CODE)p.edit().remove("gemma4_cpu_only").remove("gemma4_gpu_crashes").commit();
        if(p.getBoolean("gemma4_gpu_trying",false)){
            int crashes=p.getInt("gemma4_gpu_crashes",0)+1;SharedPreferences.Editor ed=p.edit().remove("gemma4_gpu_trying").putInt("gemma4_gpu_crashes",crashes);
            if(crashes>=2)ed.putBoolean("gemma4_cpu_only",true).putInt("gemma4_cpu_only_version",BuildConfig.VERSION_CODE);ed.commit();
        }
        if(!p.getBoolean("gemma4_cpu_only",false)){
            p.edit().putBoolean("gemma4_gpu_trying",true).commit();
            try{engine=create(f,new Backend.GPU(),c);backend="GPU";p.edit().remove("gemma4_gpu_crashes").commit();}
            catch(Throwable e){Log.w(TAG,"GPU start failed, using CPU: "+e);}
            p.edit().remove("gemma4_gpu_trying").commit();
        }
        if(engine==null){engine=create(f,new Backend.CPU(),c);backend="CPU";}
        engineKey=key;Log.i(TAG,"Gemma 4 loaded on "+backend+" in "+(SystemClock.elapsedRealtime()-start)+"ms");
        return engine;
    }
    private static Engine create(File f,Backend b,Context c){Engine e=new Engine(new EngineConfig(f.getAbsolutePath(),b,null,null,4096,null,c.getCacheDir().getPath()));try{e.initialize();return e;}catch(RuntimeException|Error x){try{e.close();}catch(Throwable ignored){}throw x;}}

    /** The small model sometimes slips a word from another script (Russian, Hindi, Tamil, Chinese…) into Malayalam; those are dropped. */
    static final java.util.regex.Pattern CJK=java.util.regex.Pattern.compile("[\\x{0370}-\\x{06FF}\\x{0900}-\\x{0CFF}\\x{0E00}-\\x{0FFF}\\x{3040}-\\x{30FF}\\x{3400}-\\x{9FFF}\\x{AC00}-\\x{D7AF}\\x{F900}-\\x{FAFF}]+ ?");
    static String stray(String text,boolean malayalam){return text==null||!malayalam?text:CJK.matcher(text).replaceAll("");}

    /** Malayalam: correct words first, then a friendly tone. Complete example sentences work better than lists of slang, which made the small model invent words. */
    static final String ML_RULES=" Reply only in Malayalam script (മലയാളം). Correct Malayalam matters most: use standard Malayalam words and grammar (എന്താണ്, പറയൂ, ഉണ്ടായി), never slang endings or invented words. Keep sentences short and simple. Be warm like a close friend: call the user നീ / നിനക്ക്, never നിങ്ങൾ. No English words, except technical words the user used or when the user asks for English."
        +" Use അയ്യോ only for bad news and ആഹാ or അടിപൊളി only for good news. Never make light of a failure or a sad event."
        +" Examples of the right style, for your own new sentences: bad news: '[sad] അയ്യോ, അത് കേട്ടപ്പോൾ എനിക്കും വിഷമമായി. എന്താണ് ഉണ്ടായത്? എന്നോട് പറയൂ.' good news: '[excited] ആഹാ, അടിപൊളി! എനിക്കും ഒരുപാട് സന്തോഷമായി. എല്ലാം എന്നോട് പറയൂ!' comfort: '[comfort] സാരമില്ല, ഞാൻ നിന്റെ കൂടെ ഉണ്ട്. നിന്റെ മനസ്സിലുള്ളത് എന്നോട് പറയൂ.' bored: '[playful] നമുക്ക് ഒരു കളി കളിക്കാം! ഞാൻ ഒരു മൃഗത്തെ മനസ്സിൽ വിചാരിച്ചു. അത് ഏതാണെന്ന് പറയാമോ?'";
    /** Lower randomness in Malayalam keeps the small model's words correct. */
    static SamplerConfig sampler(String system){return system.contains(ML_RULES)?new SamplerConfig(20,0.9,0.3,0):new SamplerConfig(40,0.95,0.8,0);}

    static String instruction(String name,String language,String practice,String style,String context){
        String lang=language.toLowerCase(Locale.ROOT).startsWith("malayalam")?ML_RULES:"";
        return systemInstruction(name,language,practice,style,context)
            +" You run fully on this phone with no internet or web search: never claim to look things up, and say when you are unsure."
            +" Your words are spoken aloud: no markdown, emoji, headings or bullet lists unless the user asks; write in natural spoken sentences and paragraphs."
            +lang+ToneTag.instruction();
    }

    MainActivity.GeminiReply chat(String key,String name,String language,String practice,String style,String prompt,List<JSONObject> history,String context,boolean search,Consumer<String> partial)throws Exception{
        if(!ready(app)){release();throw new MainActivity.ApiFailure("In Setup → Free on-phone AI, download Gemma 4 first.");}
        String system=instruction(name,language,practice,style,context);List<String> past=new ArrayList<>();List<Message> initial=new ArrayList<>();turns(history,prompt,past,initial);
        lock.lock();
        try{
            long start=SystemClock.elapsedRealtime();boolean warm=engine!=null;Engine e=engine(app);long loaded=SystemClock.elapsedRealtime();
            boolean reuse=conversation(e,system,past,initial);
            boolean ml=language.toLowerCase(Locale.ROOT).startsWith("malayalam");
            StringBuilder raw=new StringBuilder();long[] first={0};CountDownLatch done=new CountDownLatch(1);Throwable[] failure={null};
            ToneTag.reset();cancelled=false;active=conversation;
            conversation.sendMessageAsync(prompt,new MessageCallback(){
                public void onMessage(Message m){
                    String piece=text(m);if(piece.isEmpty())return;String shown;
                    synchronized(raw){if(first[0]==0)first[0]=SystemClock.elapsedRealtime();
                        // Chunks are deltas; tolerate a runtime that sends the whole text so far.
                        if(raw.length()>8&&piece.startsWith(raw.toString())){raw.setLength(0);}raw.append(piece);
                        ToneTag.track(raw.toString());shown=stray(ToneTag.visible(raw.toString()),ml);}
                    if(shown!=null&&!shown.trim().isEmpty())partial.accept(shown);
                }
                public void onDone(){done.countDown();}
                public void onError(Throwable t){failure[0]=t;done.countDown();}
            });
            boolean interrupted=false;
            while(true){try{if(done.await(cancelled?5:300,TimeUnit.SECONDS))break;cancel();if(!done.await(5,TimeUnit.SECONDS))break;}catch(InterruptedException x){interrupted=true;cancel();}}
            if(interrupted)Thread.currentThread().interrupt();
            active=null;
            String text;synchronized(raw){text=raw.toString();}
            if(cancelled||failure[0]!=null||done.getCount()>0){closeConversation();if(cancelled)throw new MainActivity.ApiFailure("Stopped.");Log.w(TAG,"generation failed: "+failure[0]);throw new MainActivity.ApiFailure("Gemma 4 could not finish this reply. Try again or a shorter message.");}
            String tone=ToneTag.lastTone(text),body=ToneTag.visible(text);if(body==null)body=text;body=stray(body,ml);
            MainActivity.GeminiReply reply=textReply(body);reply.tone=tone;
            conversationTurns.add("user:"+prompt);conversationTurns.add("assistant:"+reply.text);
            Log.i(TAG,(warm?"warm":"cold")+" "+backend+" load="+(loaded-start)+"ms reuse="+reuse+" firstText="+(first[0]==0?-1:first[0]-loaded)+"ms total="+(SystemClock.elapsedRealtime()-start)+"ms chars="+reply.text.length()+" tone="+tone);
            return reply;
        }catch(MainActivity.ApiFailure x){throw x;}
        catch(Exception|LinkageError x){Log.w(TAG,"Gemma 4 failed: "+x);closeAll();throw new MainActivity.ApiFailure("Gemma 4 could not run on this phone. Close heavy apps and try again, or choose a smaller model in Setup.");}
        finally{active=null;if(closeAfterUse)closeAll();lock.unlock();}
    }
    /** Recent history (without the new prompt) as comparable strings and as model messages. */
    private static void turns(List<JSONObject> history,String prompt,List<String> past,List<Message> initial)throws JSONException{
        JSONArray all=chatMessages(history,prompt);
        for(int i=Math.max(0,all.length()-13);i<all.length()-1;i++){JSONObject t=all.getJSONObject(i);String role=t.optString("role"),words=t.optString("content");past.add(role+":"+words);initial.add("user".equals(role)?Message.Companion.user(words):Message.Companion.model(words));}
    }
    /** Keeps the live conversation when it already holds this history; otherwise starts one with the instructions processed up front. True when reused. */
    private static boolean conversation(Engine e,String system,List<String> past,List<Message> initial){
        // The live conversation may remember more than the trimmed history it is given; that is fine until it gets long.
        boolean reuse=conversation!=null&&conversation.isAlive()&&system.equals(conversationSystem)&&conversationTurns.size()<=24
            &&(past.isEmpty()?conversationTurns.isEmpty():conversationTurns.size()>=past.size()&&conversationTurns.subList(conversationTurns.size()-past.size(),conversationTurns.size()).equals(past));
        if(reuse)return true;
        closeConversation();
        conversation=e.createConversation(new ConversationConfig(Contents.Companion.of(system),initial,Collections.emptyList(),sampler(system),true,null,Collections.emptyMap(),null,true));
        conversationSystem=system;conversationTurns.addAll(past);return false;
    }
    /** Loads the model and processes the instructions and history ahead of the next question, e.g. while the user is speaking. */
    static void prepare(Context c,String name,String language,String practice,String style,String context,List<JSONObject> history){
        if(!ready(c))return;lock.lock();
        try{long start=SystemClock.elapsedRealtime();List<String> past=new ArrayList<>();List<Message> initial=new ArrayList<>();turns(history,"",past,initial);
            boolean reused=conversation(engine(c.getApplicationContext()),instruction(name,language,practice,style,context),past,initial);
            Log.i(TAG,"prepared in "+(SystemClock.elapsedRealtime()-start)+"ms reused="+reused);}
        catch(Throwable e){Log.w(TAG,"prepare failed: "+e);}finally{lock.unlock();}
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
        if(ready(c))return NAME+" is already downloaded.";if(downloading(c))return "The download is already running.";
        File f=file(c);release();f.delete();File dir=f.getParentFile();
        if(dir==null||dir.getUsableSpace()<SIZE+300_000_000L)return "Not enough free storage. Gemma 4 needs about 2.9 GB free; this phone has "+(dir==null?0:dir.getUsableSpace()/1_000_000)+" MB.";
        DownloadManager.Request r=new DownloadManager.Request(Uri.parse(URL)).setTitle("Milo · "+NAME+" on-phone AI").setDescription("2.6 GB · Apache-2.0 · runs offline after download")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE).setAllowedOverMetered(false).setAllowedOverRoaming(false).setDestinationInExternalFilesDir(c,"models",FILE);
        prefs(c).edit().putLong("gemma4_download",c.getSystemService(DownloadManager.class).enqueue(r)).apply();
        return "Download started on Wi-Fi. You can leave Milo; progress shows in notifications.";
    }
    static void cancelDownload(Context c){long id=prefs(c).getLong("gemma4_download",-1);if(id>=0)c.getSystemService(DownloadManager.class).remove(id);prefs(c).edit().remove("gemma4_download").apply();}
    /** Removes the model file and frees its memory. */
    static void delete(Context c){cancelDownload(c);release();file(c).delete();File[] caches=c.getCacheDir().listFiles();if(caches!=null)for(File f:caches)if(f.getName().startsWith(FILE+"_"))f.delete();}
}
