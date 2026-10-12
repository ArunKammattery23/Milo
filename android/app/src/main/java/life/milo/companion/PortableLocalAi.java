package life.milo.companion;

import android.content.Context;
import android.net.Uri;
import com.google.mediapipe.tasks.genai.llminference.LlmInference;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import org.json.*;

/** User-imported Gemma task model. No prompt, model or key is sent to a server. */
final class PortableLocalAi extends AiProvider {
    final Context app;
    PortableLocalAi(Context context){app=context.getApplicationContext();}
    static File file(Context c){return new File(c.getNoBackupFilesDir(),"milo-gemma.task");}
    static boolean ready(Context c){return file(c).isFile()&&file(c).length()>100_000_000;}
    static void importModel(Context c,Uri uri)throws Exception{
        File target=file(c),temp=new File(c.getNoBackupFilesDir(),"milo-gemma-import.tmp");
        try(InputStream in=c.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(temp)){
            if(in==null)throw new IOException("Cannot open model");byte[] buffer=new byte[1024*1024];long count=0;int size;
            while((size=in.read(buffer))!=-1){count+=size;if(count>1_500_000_000L)throw new IOException("Use the Gemma 3 1B int4 task file (under 1.5 GB)");out.write(buffer,0,size);}
            if(count<100_000_000)throw new IOException("The file is too small to be the model. Download the .task file, not the webpage.");
        }catch(Exception e){temp.delete();throw e;}
        try{Files.move(temp.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}finally{temp.delete();}
    }
    String displayName(){return "Local Gemma";}String id(){return LOCAL;}boolean isFree(){return true;}boolean needsNetwork(){return false;}
    MainActivity.GeminiReply chat(String key,String name,String language,String practice,String style,String prompt,List<JSONObject> history,String context,boolean search,Consumer<String> partial)throws Exception{
        if(!ready(app))throw new MainActivity.ApiFailure("In Setup → On-device options, import the Gemma 3 1B .task model first.");
        StringBuilder input=new StringBuilder("<start_of_turn>user\n");
        input.append(systemInstruction(name,language,practice,style,context.substring(0,Math.min(400,context.length()))));
        input.append("\nYou are offline. Do not claim to search the web or contact people. For family messages, direct the user to More → People. Answer briefly.\n");
        JSONArray turns=chatMessages(history,prompt);for(int i=Math.max(0,turns.length()-3);i<turns.length();i++){JSONObject t=turns.getJSONObject(i);String words=t.optString("content");input.append(t.optString("role")).append(": ").append(words.substring(0,Math.min(700,words.length()))).append('\n');}
        input.append("<end_of_turn>\n<start_of_turn>model\n");
        try(LlmInference engine=LlmInference.createFromOptions(app,LlmInference.LlmInferenceOptions.builder().setModelPath(file(app).getAbsolutePath()).setMaxTokens(2048).setMaxTopK(40).setPreferredBackend(LlmInference.Backend.CPU).build())){
            MainActivity.GeminiReply reply=textReply(engine.generateResponse(input.toString()));partial.accept(reply.text);return reply;
        }catch(Exception|LinkageError e){throw new MainActivity.ApiFailure("The local model could not run. Import gemma3-1b-it-int4.task, close other heavy apps and try a short message. The first reply can take longer.");}
    }
}
