package life.milo.companion;

import java.io.*;
import java.util.function.Consumer;
import org.json.*;

/** Parses server-sent events without retaining raw provider responses or keys. */
final class GeminiStream {
    static MainActivity.GeminiReply read(BufferedReader reader, Consumer<String> partial) throws Exception {
        StringBuilder event=new StringBuilder(),answer=new StringBuilder();
        JSONObject grounding=null; String finish=""; int total=0; String line;
        while((line=reader.readLine())!=null){
            total+=line.length();if(total>2_000_000)throw new IOException("Response too large");
            if(line.startsWith("data:")){if(event.length()>0)event.append('\n');event.append(line.substring(5).trim());}
            if(!line.isEmpty())continue;
            if(event.length()==0)continue;
            String data=event.toString();event.setLength(0);if(data.equals("[DONE]"))break;
            JSONObject json=new JSONObject(data);if(json.has("error"))throw new IOException("Provider stream error");
            JSONObject feedback=json.optJSONObject("promptFeedback");if(feedback!=null&&feedback.has("blockReason"))throw new MainActivity.ApiFailure("Milo couldn’t answer that request. Try rephrasing it.");
            JSONArray candidates=json.optJSONArray("candidates");if(candidates==null||candidates.length()==0)continue;
            JSONObject candidate=candidates.getJSONObject(0);JSONObject content=candidate.optJSONObject("content");
            JSONArray parts=content==null?null:content.optJSONArray("parts");int before=answer.length();
            if(parts!=null)for(int i=0;i<parts.length();i++){JSONObject p=parts.optJSONObject(i);if(p!=null&&!p.optBoolean("thought"))answer.append(p.optString("text"));}
            if(candidate.has("groundingMetadata"))grounding=candidate.optJSONObject("groundingMetadata");
            if(candidate.has("finishReason"))finish=candidate.optString("finishReason");
            if(!finish.isEmpty()&&!finish.equals("STOP")&&!finish.equals("MAX_TOKENS"))throw new MainActivity.ApiFailure("Milo couldn’t complete that reply. Try a different question.");
            if(answer.length()>before)partial.accept(answer.toString());
        }
        if(finish.isEmpty())throw new IOException("Incomplete stream");
        if(answer.toString().trim().isEmpty())throw new MainActivity.ApiFailure("No text reply was returned. Try a different question.");
        MainActivity.GeminiReply r=new MainActivity.GeminiReply();r.text=answer.toString().trim();r.sources=grounding==null?null:grounding.optJSONArray("groundingChunks");if(r.sources==null)r.sources=new JSONArray();r.supports=grounding==null?null:grounding.optJSONArray("groundingSupports");JSONObject entry=grounding==null?null:grounding.optJSONObject("searchEntryPoint");r.suggestions=entry==null?"":entry.optString("renderedContent");return r;
    }
}
