package life.milo.companion;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.json.*;

/** Select a text model advertised to this key, without exposing key or model-list response in UI. */
final class GeminiModels {
    private static volatile String cachedFingerprint="", cachedModel="";
    private static volatile boolean cachedSearch;
    private GeminiModels() {}

    static String discover(String key, boolean search) throws Exception {
        String fingerprint=fingerprint(key);
        if(fingerprint.equals(cachedFingerprint)&&search==cachedSearch&&!cachedModel.isEmpty())return cachedModel;
        HttpURLConnection connection=(HttpURLConnection)new URL("https://generativelanguage.googleapis.com/v1beta/models?pageSize=1000").openConnection();
        try {
            connection.setRequestMethod("GET");connection.setConnectTimeout(20000);connection.setReadTimeout(20000);
            connection.setInstanceFollowRedirects(false);connection.setRequestProperty("x-goog-api-key",key);
            int status=connection.getResponseCode();
            if(status!=200)throw new MainActivity.ApiFailure(ProviderErrors.message("Google",status,connection.getErrorStream()));
            StringBuilder body=new StringBuilder();
            try(Reader reader=new InputStreamReader(connection.getInputStream(),StandardCharsets.UTF_8)){
                char[] part=new char[2048];int n;
                while(body.length()<524288&&(n=reader.read(part,0,Math.min(part.length,524288-body.length())))!=-1)body.append(part,0,n);
                if(body.length()==524288)throw new MainActivity.ApiFailure("Google returned a model list too large for Milo. Your key remains saved.");
            }
            String selected=choose(new JSONObject(body.toString()).optJSONArray("models"),search);
            if(selected.isEmpty())throw new MainActivity.ApiFailure(search?"No supported web-search model was listed for this key. Turn Web search off and test again.":"Google listed no compatible chat model for this key. Check its project and API access. Your key remains saved.");
            cachedFingerprint=fingerprint;cachedSearch=search;cachedModel=selected;
            return selected;
        } finally {connection.disconnect();}
    }

    static String choose(JSONArray models, boolean search){
        if(models==null)return "";
        String[] preferred=search?new String[]{"gemini-3.8-flash","gemini-3.6-flash","gemini-3-flash-preview","gemini-2.5-flash"}:new String[]{"gemini-3.5-flash-lite","gemini-3.1-flash-lite","gemini-3.8-flash","gemini-3.6-flash","gemini-3-flash-preview","gemini-2.5-flash-lite","gemini-2.5-flash"};
        for(String candidate:preferred)for(int i=0;i<models.length();i++){
            JSONObject entry=models.optJSONObject(i);if(entry==null||!("models/"+candidate).equals(entry.optString("name")))continue;
            JSONArray methods=entry.optJSONArray("supportedGenerationMethods");
            if(methods!=null)for(int m=0;m<methods.length();m++)if("generateContent".equals(methods.optString(m)))return candidate;
        }
        return "";
    }

    private static String fingerprint(String key)throws Exception{
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex=new StringBuilder();for(byte b:digest)hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return hex.toString();
    }
}
