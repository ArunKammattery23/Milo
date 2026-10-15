package life.milo.companion;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.*;

/**
 * Human, emotional speech from Google's Gemini voices, using the user's own Gemini key.
 * The reply text goes to Google; when there is no key, internet or quota, Milo falls back to its offline voice.
 */
final class GeminiVoice {
    static final String TAG="MiloHumanVoice";
    /** Newest models first; the first that works for this key is remembered. */
    static final String[] MODELS={"gemini-3.8-flash-lite-tts","gemini-3.8-flash-tts","gemini-2.5-flash-preview-tts"};
    private static volatile long pausedUntil;
    static String lastError="";
    /** Recent request times, to stay under the key's per-minute limit (3 on Google's free tier). */
    private static final java.util.ArrayDeque<Long> recent=new java.util.ArrayDeque<>();
    static int perMinute(SharedPreferences p){return p.getInt("cloud_tts_rpm",3);}
    /** Takes a request slot, waiting up to about 3 seconds for one; false when the minute's quota is used up. */
    static synchronized boolean slot(SharedPreferences p){
        long now=System.currentTimeMillis();while(!recent.isEmpty()&&now-recent.peekFirst()>61_000)recent.pollFirst();
        if(recent.size()>=perMinute(p)){long wait=recent.peekFirst()+61_000-now;if(wait>3000)return false;try{Thread.sleep(wait);}catch(InterruptedException e){return false;}recent.pollFirst();}
        recent.addLast(System.currentTimeMillis());return true;
    }

    static boolean available(Context c){
        try{return !KeyVault.loadForProvider(c,AiProvider.GEMINI).isEmpty()&&System.currentTimeMillis()>=pausedUntil;}catch(Exception e){return false;}
    }

    /** How Milo should sound: the character's voice, the emotion, and Malayalam if needed. */
    static String style(MiloVoice.Character ch,String tone,boolean malayalam){
        String base;
        switch(ch.id){
            case "cartoon_fairy":base="a bright, bubbly cartoon fairy";break;
            case "tiny_pixie":base="a tiny, squeaky, adorable pixie";break;
            case "chechi":base="a gentle, caring big sister";break;
            case "playful_kid":base="a lively, playful child";break;
            case "robot_buddy":base="a friendly robot buddy";break;
            case "brave_boy":base="a cheerful, brave young boy";break;
            default:base="a sweet, warm young girl";
        }
        String feel;
        switch(tone==null?"":tone){
            case "happy":feel="cheerful and smiling";break;
            case "playful":feel="playful and giggly";break;
            case "excited":feel="very excited and bubbly";break;
            case "celebrate":feel="joyful and proud";break;
            case "surprised":feel="surprised, with a little gasp";break;
            case "laughing":feel="laughing while speaking";break;
            case "curious":feel="curious and wondering";break;
            case "thinking":feel="thoughtful and unhurried";break;
            case "shy":feel="shy, soft and a little bashful";break;
            case "grateful":feel="warm and heartfelt";break;
            case "comfort":feel="gentle, soothing and soft";break;
            case "calm":feel="calm and relaxed";break;
            case "sad":feel="sad and sympathetic, soft and slow";break;
            case "concerned":feel="worried and caring";break;
            case "confused":feel="puzzled";break;
            case "sleepy":feel="sleepy, slow and yawning";break;
            case "oops":feel="embarrassed, with a quick apology";break;
            case "angry":feel="annoyed and indignant with a playful huff, never shouting at the listener";break;
            default:feel="warm and friendly";
        }
        return "Speak as "+base+", natural and human, "+feel+(malayalam?", in natural Kerala Malayalam":"");
    }

    /** Speech as 16-bit PCM at 24 kHz, or null. Keeps a short pause after quota or key errors. */
    static short[] speak(Context c,SharedPreferences prefs,String text,String tone){
        String key;try{key=KeyVault.loadForProvider(c,AiProvider.GEMINI);}catch(Exception e){return null;}
        if(key.isEmpty()||System.currentTimeMillis()<pausedUntil)return null;
        MiloVoice.Character ch=MiloVoice.character(prefs);String style=style(ch,tone,MiloVoice.malayalam(prefs));
        if(!slot(prefs)){lastError="free limit of "+perMinute(prefs)+" voice requests per minute reached";return null;}
        String remembered=prefs.getString("cloud_tts_model","");
        for(String model:MODELS){
            if(!remembered.isEmpty()&&!remembered.equals(model))continue;
            try{
                short[] pcm=model.startsWith("gemini-2.5")?legacy(key,model,ch.cloudVoice,style,text):interaction(key,model,ch.cloudVoice,style,text);
                if(pcm!=null&&pcm.length>0){if(remembered.isEmpty())prefs.edit().putString("cloud_tts_model",model).apply();lastError="";return pcm;}
            }catch(HttpError e){
                lastError=model+": HTTP "+e.code;Log.w(TAG,lastError+" "+e.getMessage());
                if(e.code==429){
                    // "limit: 3 requests per minute": remember the limit and wait out this minute instead of pausing for long.
                    java.util.regex.Matcher m=java.util.regex.Pattern.compile("limit: ([0-9]+) requests per minute").matcher(e.getMessage());
                    if(m.find()){prefs.edit().putInt("cloud_tts_rpm",Integer.parseInt(m.group(1))).apply();pausedUntil=System.currentTimeMillis()+50_000;}
                    else pausedUntil=System.currentTimeMillis()+10*60_000;
                    return null;}
                if(e.code==401||e.code==403){pausedUntil=System.currentTimeMillis()+30*60_000;return null;}
            }catch(Exception e){lastError=model+": "+e;Log.w(TAG,lastError);}
            if(!remembered.isEmpty()){prefs.edit().remove("cloud_tts_model").apply();remembered="";}
        }
        return null;
    }

    static final class HttpError extends IOException{final int code;HttpError(int code,String body){super(body.length()>300?body.substring(0,300):body);this.code=code;}}

    private static short[] interaction(String key,String model,String voice,String style,String text)throws Exception{
        JSONObject content=new JSONObject().put("type","text").put("text",text)
            .put("annotations",new JSONArray().put(new JSONObject().put("type","speech_metadata").put("style",style)));
        JSONObject body=new JSONObject().put("model",model)
            .put("input",new JSONArray().put(new JSONObject().put("type","user_input").put("content",new JSONArray().put(content))))
            .put("response_format",new JSONObject().put("type","audio").put("mime_type","audio/l16").put("sample_rate",24000))
            .put("generation_config",new JSONObject().put("speech_config",new JSONArray().put(new JSONObject().put("voice",voice))));
        JSONObject reply=post("https://generativelanguage.googleapis.com/v1beta/interactions",key,body);
        String data=findAudio(reply);return data==null?null:pcm(Base64.decode(data,Base64.DEFAULT));
    }
    private static short[] legacy(String key,String model,String voice,String style,String text)throws Exception{
        JSONObject body=new JSONObject()
            .put("contents",new JSONArray().put(new JSONObject().put("parts",new JSONArray().put(new JSONObject().put("text",style+": "+text)))))
            .put("generationConfig",new JSONObject().put("responseModalities",new JSONArray().put("AUDIO"))
                .put("speechConfig",new JSONObject().put("voiceConfig",new JSONObject().put("prebuiltVoiceConfig",new JSONObject().put("voiceName",voice)))));
        JSONObject reply=post("https://generativelanguage.googleapis.com/v1beta/models/"+model+":generateContent",key,body);
        String data=findAudio(reply);return data==null?null:pcm(Base64.decode(data,Base64.DEFAULT));
    }
    private static JSONObject post(String url,String key,JSONObject body)throws Exception{
        HttpURLConnection http=(HttpURLConnection)new URL(url).openConnection();
        http.setRequestMethod("POST");http.setConnectTimeout(8000);http.setReadTimeout(30000);http.setDoOutput(true);
        http.setRequestProperty("Content-Type","application/json");http.setRequestProperty("x-goog-api-key",key);
        try(OutputStream o=http.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}
        int code=http.getResponseCode();InputStream in=code<300?http.getInputStream():http.getErrorStream();
        String text;try(InputStream s=in){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;if(s!=null)while((n=s.read(buf))>0){b.write(buf,0,n);if(b.size()>20_000_000)throw new IOException("Voice reply too large");}text=b.toString("UTF-8");}
        finally{http.disconnect();}
        if(code>=300)throw new HttpError(code,text);
        return new JSONObject(text);
    }
    /** Finds the base64 audio anywhere in the reply: an audio content block's "data" or an inlineData "data". */
    static String findAudio(Object node)throws JSONException{
        if(node instanceof JSONObject){
            JSONObject o=(JSONObject)node;
            String type=o.optString("type",""),mime=o.optString("mime_type",o.optString("mimeType",""));
            if(o.has("data")&&o.get("data") instanceof String&&(type.equals("audio")||mime.startsWith("audio")))return o.getString("data");
            String found=null;for(java.util.Iterator<String> k=o.keys();k.hasNext();){String r=findAudio(o.get(k.next()));if(r!=null)found=r;}
            return found;
        }
        if(node instanceof JSONArray){JSONArray a=(JSONArray)node;String found=null;for(int i=0;i<a.length();i++){String r=findAudio(a.get(i));if(r!=null)found=r;}return found;}
        return null;
    }
    /** Raw little-endian 16-bit PCM, or a WAV file's samples. */
    static short[] pcm(byte[] b){
        int start=0;
        if(b.length>44&&b[0]=='R'&&b[1]=='I'&&b[2]=='F'&&b[3]=='F'){int pos=12;while(pos+8<=b.length){String id=new String(b,pos,4,StandardCharsets.US_ASCII);int len=(b[pos+4]&255)|(b[pos+5]&255)<<8|(b[pos+6]&255)<<16|(b[pos+7]&255)<<24;if(id.equals("data")){start=pos+8;break;}pos+=8+Math.max(0,len)+(len&1);}}
        short[] s=new short[(b.length-start)/2];for(int i=0;i<s.length;i++)s[i]=(short)((b[start+2*i]&255)|(b[start+2*i+1]<<8));return s;
    }
}
