package life.milo.companion;

import android.content.Context;
import android.net.Uri;
import com.google.mediapipe.tasks.genai.llminference.LlmInference;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import org.json.*;

/** User-imported Qwen or Gemma task model. No prompt, model or key is sent to a server. */
final class PortableLocalAi extends AiProvider {
    final Context app;
    static final String GEMMA="gemma",QWEN="qwen";
    PortableLocalAi(Context context){app=context.getApplicationContext();}
    static String model(Context c){return c.getSharedPreferences("milo",Context.MODE_PRIVATE).getString("portable_model",GEMMA);}
    static File file(Context c){return new File(c.getNoBackupFilesDir(),QWEN.equals(model(c))?"milo-qwen.task":"milo-gemma.task");}
    static String modelName(Context c){return QWEN.equals(model(c))?"Qwen 2.5 0.5B":"Gemma 3 1B";}
    static boolean ready(Context c){return file(c).isFile()&&file(c).length()>100_000_000;}
    static void importModel(Context c,Uri uri,String kind)throws Exception{
        if(!QWEN.equals(kind)&&!GEMMA.equals(kind))throw new IOException("Unknown local model");
        release();
        try(android.database.Cursor cursor=c.getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){
            if(cursor!=null&&cursor.moveToFirst()){String name=cursor.getString(0).toLowerCase(java.util.Locale.ROOT);if(!name.endsWith(".task")||!name.contains(QWEN.equals(kind)?"qwen2.5-0.5b":"gemma3-1b"))throw new IOException("Choose the linked "+kind+" .task file");}
        }
        File target=new File(c.getNoBackupFilesDir(),QWEN.equals(kind)?"milo-qwen.task":"milo-gemma.task"),temp=new File(c.getNoBackupFilesDir(),"milo-model-import.tmp");
        try(InputStream in=c.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(temp)){
            if(in==null)throw new IOException("Cannot open model");byte[] buffer=new byte[1024*1024];long count=0;int size;
            while((size=in.read(buffer))!=-1){count+=size;if(count>1_500_000_000L)throw new IOException("Use the linked model .task file (under 1.5 GB)");out.write(buffer,0,size);}
            if(count<100_000_000)throw new IOException("The file is too small to be the model. Download the .task file, not the webpage.");
        }catch(Exception e){temp.delete();throw e;}
        try{Files.move(temp.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}finally{temp.delete();}
        c.getSharedPreferences("milo",Context.MODE_PRIVATE).edit().putString("portable_model",kind).commit();
    }
    String displayName(){return "Local "+modelName(app);}String id(){return LOCAL;}boolean isFree(){return true;}boolean needsNetwork(){return false;}

    /* Loading the model takes seconds, so one engine stays loaded between messages. Each call still starts a fresh session. */
    static final java.util.concurrent.locks.ReentrantLock engineLock=new java.util.concurrent.locks.ReentrantLock();
    static LlmInference engine;static String engineKey;static volatile boolean closeAfterUse;
    /** Frees the loaded model now, or right after the reply in progress. Never blocks the caller. */
    static void release(){if(!engineLock.tryLock()){closeAfterUse=true;return;}try{closeEngine();}finally{engineLock.unlock();}}
    private static void closeEngine(){closeAfterUse=false;if(engine!=null){try{engine.close();}catch(Exception|LinkageError ignored){}engine=null;engineKey=null;}}
    private static LlmInference engine(Context c,boolean qwen){
        if(closeAfterUse)closeEngine();File f=file(c);String key=f.getAbsolutePath()+'|'+f.length()+'|'+f.lastModified();
        if(engine!=null&&key.equals(engineKey))return engine;closeEngine();
        engine=LlmInference.createFromOptions(c,LlmInference.LlmInferenceOptions.builder().setModelPath(f.getAbsolutePath()).setMaxTokens(qwen?1024:2048).setMaxTopK(40).setPreferredBackend(LlmInference.Backend.CPU).build());engineKey=key;return engine;
    }
    MainActivity.GeminiReply chat(String key,String name,String language,String practice,String style,String prompt,List<JSONObject> history,String context,boolean search,Consumer<String> partial)throws Exception{
        if(!ready(app)){release();throw new MainActivity.ApiFailure("In Setup → Free on-phone AI, download and import a .task model first.");}
        boolean qwen=QWEN.equals(model(app));String input=localPrompt(qwen,name,language,practice,style,prompt,history,context);
        engineLock.lock();
        try{
            long start=android.os.SystemClock.elapsedRealtime();boolean warm=engine!=null;LlmInference model=engine(app,qwen);long loaded=android.os.SystemClock.elapsedRealtime(),first[]={0};
            StringBuilder answer=new StringBuilder();
            com.google.common.util.concurrent.ListenableFuture<String> future=model.generateResponseAsync(input,(piece,done)->{
                if(piece==null||piece.isEmpty())return;String shown;
                synchronized(answer){if(first[0]==0)first[0]=android.os.SystemClock.elapsedRealtime();answer.append(piece);shown=answer.toString().replaceFirst("^\\s+","");}
                if(!shown.isEmpty())partial.accept(shown);
            });
            String full=finish(future);
            android.util.Log.i("MiloLocalAi",(warm?"warm":"cold")+" load="+(loaded-start)+"ms firstText="+(first[0]==0?-1:first[0]-loaded)+"ms total="+(android.os.SystemClock.elapsedRealtime()-start)+"ms");
            synchronized(answer){return textReply(full==null||full.trim().isEmpty()?answer.toString():full);}
        }catch(MainActivity.ApiFailure e){throw e;}
        catch(Exception|LinkageError e){closeEngine();throw new MainActivity.ApiFailure("The local "+modelName(app)+" model could not run. Check that you imported the complete linked .task file, close heavy apps, and try a short message.");}
        finally{if(closeAfterUse)closeEngine();engineLock.unlock();}
    }
    /** Native generation cannot be stopped mid-reply, so an interrupted worker still waits before the engine can be reused or closed. */
    private static String finish(java.util.concurrent.Future<String> future)throws Exception{
        boolean interrupted=false;
        try{while(true){try{return future.get();}catch(InterruptedException e){interrupted=true;}}}
        catch(java.util.concurrent.ExecutionException e){throw e.getCause() instanceof Exception?(Exception)e.getCause():e;}
        finally{if(interrupted)Thread.currentThread().interrupt();}
    }
    static String localPrompt(boolean qwen,String name,String language,String practice,String style,String prompt,List<JSONObject> history,String context)throws JSONException{
        String instruction="You are "+name+", a kind AI pet companion. Reply in "+language+". Be concise, supportive, and honest. Help with learning and small goals. Do not claim human feelings, diagnose illness, or pretend to search the web or contact people. Respect the user's choices. Style: "+style+". Practice language: "+practice+". Context: "+context.substring(0,Math.min(200,context.length()));
        JSONArray turns=chatMessages(history,prompt);StringBuilder input=new StringBuilder();
        if(qwen){input.append("<|im_start|>system\n").append(instruction).append("<|im_end|>\n");for(int i=Math.max(0,turns.length()-3);i<turns.length();i++){JSONObject t=turns.getJSONObject(i);String words=t.optString("content");input.append("<|im_start|>").append(t.optString("role").equals("assistant")?"assistant":"user").append('\n').append(words.substring(0,Math.min(500,words.length()))).append("<|im_end|>\n");}input.append("<|im_start|>assistant\n");}
        else{input.append("<start_of_turn>user\n").append(instruction).append('\n');for(int i=Math.max(0,turns.length()-3);i<turns.length();i++){JSONObject t=turns.getJSONObject(i);String words=t.optString("content");input.append(t.optString("role")).append(": ").append(words.substring(0,Math.min(500,words.length()))).append('\n');}input.append("<end_of_turn>\n<start_of_turn>model\n");}
        return input.toString();
    }
}
