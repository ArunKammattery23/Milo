package life.milo.companion;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import org.json.*;
final class GeminiClient {
static MainActivity.GeminiReply request(String key,String name,String language,String practice,String style,String prompt,List<JSONObject> history,String context,boolean useSearch,Consumer<String> partial) throws Exception {
    return request(key,name,language,practice,style,prompt,history,context,useSearch,partial,MainActivity.MODEL);
}
static MainActivity.GeminiReply request(String key,String name,String language,String practice,String style,String prompt,List<JSONObject> history,String context,boolean useSearch,Consumer<String> partial,String model) throws Exception {
    AiProvider.validateKey(key,true);AiProvider.validateModel(model);


        JSONArray contents=new JSONArray();for(JSONObject t:history)contents.put(t);contents.put(MainActivity.turn("user",prompt));
        String instruction=AiProvider.systemInstruction(name,language,practice,style,context);
        JSONObject body=new JSONObject().put("contents",contents).put("systemInstruction",new JSONObject().put("parts",new JSONArray().put(new JSONObject().put("text",instruction)))).put("generationConfig",new JSONObject().put("maxOutputTokens",1024));if(model.startsWith("gemini-2.5-flash"))body.getJSONObject("generationConfig").put("thinkingConfig",new JSONObject().put("thinkingBudget",0));if(useSearch)body.put("tools",new JSONArray().put(new JSONObject().put("google_search",new JSONObject())));
        HttpURLConnection conn=(HttpURLConnection)new URL("https://generativelanguage.googleapis.com/v1beta/models/"+model+":streamGenerateContent?alt=sse").openConnection();
        try{conn.setRequestMethod("POST");conn.setConnectTimeout(20000);conn.setReadTimeout(60000);conn.setInstanceFollowRedirects(false);conn.setRequestProperty("Content-Type","application/json");conn.setRequestProperty("x-goog-api-key",key);conn.setDoOutput(true);try(OutputStream out=conn.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}
            int code=conn.getResponseCode();if(code!=200){if(code==429)throw new MainActivity.ApiFailure("Google’s usage limit has been reached. Please wait and try later. Milo does not switch to paid models.");if(code==400||code==401||code==403)throw new MainActivity.ApiFailure("Google rejected this request. Check your API key, project access, region, and model availability in AI Studio.");if(code==404)throw new MainActivity.ApiFailure("This Gemini model is unavailable for your project. Choose an accessible model ID in Setup.");throw new MainActivity.ApiFailure("Google returned an error ("+code+"). Try again later.");}
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(conn.getInputStream(),StandardCharsets.UTF_8))){return GeminiStream.read(reader,partial);}
        }finally{conn.disconnect();}
    
}
}
