package life.milo.companion;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;
/** Show actionable classifications, never raw server text which could echo a key. */
final class ProviderErrors {
    static String message(String provider,int status,InputStream stream){
        String reason="";
        if(stream!=null)try(Reader r=new InputStreamReader(stream,StandardCharsets.UTF_8)){
            StringBuilder b=new StringBuilder();char[] c=new char[1024];int n;while(b.length()<16384&&(n=r.read(c,0,Math.min(c.length,16384-b.length())))!=-1)b.append(c,0,n);
            JSONObject error=new JSONObject(b.toString()).optJSONObject("error");if(error!=null){reason=error.optString("code")+" "+error.optString("type")+" "+error.optString("status");JSONArray details=error.optJSONArray("details");if(details!=null)for(int i=0;i<details.length();i++){JSONObject d=details.optJSONObject(i);if(d!=null)reason+=" "+d.optString("reason");}}
        }catch(Exception ignored){}
        return classify(provider,status,reason);
    }
    static String classify(String provider,int status,String reason){
        String tail=" Your saved key has not been erased.";
        if(reason.contains("API_KEY_INVALID")||reason.contains("API_KEY_EXPIRED")||reason.contains("invalid_api_key")||status==401)return provider+" rejected the key ("+status+"). Paste an active developer API key for this provider and save again."+tail;
        if(status==402||reason.contains("insufficient_quota"))return provider+" has no available API credits. Check your provider account; a chat subscription may not include API credits."+tail;
        if(status==403||reason.contains("API_KEY_SERVICE_BLOCKED")||reason.contains("API_KEY_HTTP_REFERRER_BLOCKED"))return provider+" denied access ("+status+"). Check this key's API/application restrictions, project access and region in the provider console."+tail;
        if(status==404)return provider.equals("Google")?"Google could not use this model (404). In Setup tap Choose Gemini model automatically, then Save connection and Test again."+tail:provider+" could not find the model (404). Enter an available model ID in Setup and save."+tail;
        if(status==429)return provider.equals("Google")?"Google Gemini reached this project's rate or daily quota (429). Your key is saved. Check this project's limits in Google AI Studio, or use on-phone AI in Setup. A new key in the same project will not reset the limit.":provider+" reports a rate or quota limit (429). Wait or check the account's quota and credits; repeatedly replacing the key will not fix a quota limit."+tail;
        if(status==400)return provider+" rejected the model or request settings (400). Check the model ID and whether your account supports it."+tail;
        if(status>=500)return provider+" is temporarily unavailable ("+status+"). Try later."+tail;
        return provider+" returned error "+status+". Check the provider account and connection."+tail;
    }
}
