package life.milo.companion;
import com.google.mlkit.genai.prompt.*;
import com.google.mlkit.genai.prompt.java.GenerativeModelFutures;
import com.google.mlkit.genai.common.FeatureStatus;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.json.*;
/** Actual on-device Gemini Nano inference. No canned response substituted for a model. */
final class LocalAiClient extends AiProvider {
    String displayName(){return "On-device AI";}String id(){return LOCAL;}
    boolean isFree(){return true;}boolean needsNetwork(){return false;}
    MainActivity.GeminiReply chat(String key,String name,String language,String practice,String style,String prompt,List<JSONObject> history,String context,boolean search,Consumer<String> partial)throws Exception{
        GenerativeModel model=Generation.INSTANCE.getClient();
        try{
            GenerativeModelFutures futures=GenerativeModelFutures.from(model);
            int code=futures.checkStatus().get(30,TimeUnit.SECONDS);
            if(code!=FeatureStatus.AVAILABLE)throw new MainActivity.ApiFailure(LocalModelSupport.status(code).message);
            StringBuilder text=new StringBuilder(systemInstruction(name,language,practice,style,context.substring(0,Math.min(800,context.length()))));
            text.append("\nThis model runs on-device. No web search is available. Say when unsure; never invent current facts.\nRecent conversation:\n");
            JSONArray turns=chatMessages(history,prompt);
            for(int i=Math.max(0,turns.length()-5);i<turns.length()-1;i++){JSONObject turn=turns.getJSONObject(i);String words=turn.optString("content");text.append(turn.optString("role")).append(": ").append(words.substring(0,Math.min(600,words.length()))).append('\n');}
            text.append("User: ").append(prompt).append("\nAssistant:");
            GenerateContentRequest.Builder builder=new GenerateContentRequest.Builder(new TextPart(text.toString()));builder.setMaxOutputTokens(512);GenerateContentRequest request=builder.build();
            GenerateContentResponse response=futures.generateContent(request).get(120,TimeUnit.SECONDS);
            StringBuilder answer=new StringBuilder();if(!response.getCandidates().isEmpty())answer.append(response.getCandidates().get(0).getText());
            MainActivity.GeminiReply reply=textReply(answer.toString());partial.accept(reply.text);return reply;
        }catch(MainActivity.ApiFailure e){throw e;}catch(Exception e){throw new MainActivity.ApiFailure("Local generation could not finish. Keep Milo open; check model readiness in Setup or try a shorter question.");}finally{model.close();}
    }
}
