package life.milo.companion;

import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import java.util.*;

/** Milo's voice characters, emotion voices and little sounds, plus helpers for the phone's Google voices. */
final class MiloVoice {
    /** Google's engine has Malayalam and many Indian voices; Android falls back to the default engine if it is missing. */
    static final String GOOGLE="com.google.android.tts";

    /** How Milo sounds before any emotion: a voice gender, pitch, speed, loudness, an effect and a matching AI voice. */
    static final class Character{
        final String id,name,gender,effect,aiVoice,cloudVoice;final float pitch,rate,volume;
        Character(String id,String name,String gender,float pitch,float rate,float volume,String effect,String aiVoice,String cloudVoice){this.id=id;this.name=name;this.gender=gender;this.pitch=pitch;this.rate=rate;this.volume=volume;this.effect=effect;this.aiVoice=aiVoice;this.cloudVoice=cloudVoice;}
    }
    static final Character[] CHARACTERS={
        new Character("sweet_girl","Sweet Girl","f",1.04f,0.98f,1f,"none","af_heart","Leda"),
        new Character("cartoon_fairy","Cartoon Fairy","f",1.38f,1.06f,1f,"sparkle","af_bella","Zephyr"),
        new Character("tiny_pixie","Tiny Pixie","f",1.75f,1.1f,1f,"sparkle","af_nicole","Laomedeia"),
        new Character("chechi","Chechi · gentle big sister","f",1.02f,0.92f,0.95f,"none","af_sarah","Sulafat"),
        new Character("playful_kid","Playful Kid","f",1.3f,1.1f,1f,"none","af_sky","Autonoe"),
        new Character("robot_buddy","Robot Buddy","m",0.98f,1f,1f,"robot","am_michael","Charon"),
        new Character("brave_boy","Brave Boy","m",1.08f,1.02f,1f,"none","am_puck","Puck"),
    };
    static Character character(SharedPreferences p){String id=p.getString("voice_character",CHARACTERS[0].id);for(Character c:CHARACTERS)if(c.id.equals(id))return c;return CHARACTERS[0];}

    /** Emotion voice: {pitch, speed, loudness} multipliers, applied on top of the character and scaled by emotion strength. */
    static float[] emotion(String tone){
        if(tone==null)return new float[]{1,1,1};
        switch(tone){
            case "happy":return new float[]{1.05f,1.02f,1};
            case "playful":return new float[]{1.1f,1.06f,1};
            case "excited":return new float[]{1.14f,1.1f,1};
            case "celebrate":return new float[]{1.12f,1.06f,1};
            case "surprised":return new float[]{1.12f,1.05f,1};
            case "laughing":return new float[]{1.1f,1.06f,1};
            case "curious":return new float[]{1.05f,0.98f,1};
            case "thinking":return new float[]{1,0.92f,0.9f};
            case "shy":return new float[]{1.04f,0.92f,0.75f};
            case "grateful":return new float[]{1.03f,0.95f,0.95f};
            case "comfort":return new float[]{0.95f,0.88f,0.8f};
            case "calm":return new float[]{0.96f,0.88f,0.85f};
            case "sad":return new float[]{0.92f,0.85f,0.75f};
            case "concerned":return new float[]{0.96f,0.92f,0.9f};
            case "confused":return new float[]{1,0.94f,1};
            case "sleepy":return new float[]{0.9f,0.8f,0.7f};
            case "oops":return new float[]{1.06f,1.04f,0.95f};
            case "angry":return new float[]{0.9f,1.12f,1};
            default:return new float[]{1,1,1};
        }
    }
    /** An emotion's own sound effect, such as the harsh edge of an angry voice. */
    static String emotionEffect(String tone){return "angry".equals(tone)?"harsh":null;}

    /** A little sound Milo makes before a reply in that emotion: {English, Malayalam}. */
    static String interjection(String tone,boolean malayalam){
        if(tone==null)return null;
        String[] s;
        switch(tone){
            case "happy":s=new String[]{"Hehe!","ഹായ്!"};break;
            case "playful":s=new String[]{"Hee hee!","ഹി ഹി!"};break;
            case "excited":s=new String[]{"Wow!","ഹായ്!"};break;
            case "celebrate":s=new String[]{"Yay!","അടിപൊളി!"};break;
            case "surprised":s=new String[]{"Ooh!","ഓഹ്!"};break;
            case "laughing":s=new String[]{"Hahaha!","ഹ ഹ ഹ!"};break;
            case "curious":s=new String[]{"Hmm?","ഓഹോ?"};break;
            case "shy":s=new String[]{"Aww…","അയ്യേ…"};break;
            case "grateful":s=new String[]{"Aww!","ഓ…"};break;
            case "comfort":s=new String[]{"Hey…","സാരമില്ല…"};break;
            case "sad":s=new String[]{"Oh…","അയ്യോ…"};break;
            case "concerned":s=new String[]{"Oh…","ശ്ശോ…"};break;
            case "confused":s=new String[]{"Huh?","ങേ?"};break;
            case "sleepy":s=new String[]{"Mmm…","ഉറക്കം വരുന്നു…"};break;
            case "oops":s=new String[]{"Oops!","അയ്യോ!"};break;
            case "angry":s=new String[]{"Hmph!","ഹും!"};break;
            default:return null;
        }
        return s[malayalam?1:0];
    }
    /** Sample lines for "Hear emotions": {tone, English, Malayalam}. */
    static final String[][] SAMPLES={
        {"happy","Hi! I'm so happy to see you.","നിന്നെ കണ്ടതിൽ ഒരുപാട് സന്തോഷം."},
        {"excited","Guess what? That's amazing news!","അറിയാമോ? ഇത് അടിപൊളി വാർത്തയാണ്!"},
        {"laughing","That's so funny!","അത് കൊള്ളാം, നല്ല തമാശ!"},
        {"sad","I'm sorry. That sounds really hard.","എനിക്ക് വിഷമമുണ്ട്. അത് ശരിക്കും ബുദ്ധിമുട്ടാണ്."},
        {"angry","That is so unfair!","ഇത് ഒട്ടും ശരിയല്ല!"},
        {"sleepy","I'm getting sleepy. Good night.","എനിക്ക് ഉറക്കം വരുന്നു. ശുഭരാത്രി."},
    };

    static TextToSpeech create(Context c,TextToSpeech.OnInitListener listener){return new TextToSpeech(c,listener,GOOGLE);}
    static Locale locale(SharedPreferences p){String code=p.getString("voice_language","");return code.isEmpty()?Locale.getDefault():Locale.forLanguageTag(code);}
    static boolean malayalam(SharedPreferences p){return "ml".equals(locale(p).getLanguage());}

    /** Distinct phone voices for a language, e.g. "ml-in-x-mlc" and "ml-in-x-mle"; local and online copies are one choice. */
    static List<String> choices(TextToSpeech tts,Locale locale){
        TreeSet<String> names=new TreeSet<>();
        try{Set<Voice> all=tts.getVoices();if(all!=null)for(Voice v:all)if(matches(v,locale)&&!v.getName().endsWith("-language"))names.add(base(v.getName()));}catch(RuntimeException ignored){}
        return new ArrayList<>(names);
    }
    static String label(TextToSpeech tts,Locale locale,String base,int index,SharedPreferences p){
        Voice v=resolve(tts,locale,base);
        String where=v==null?"":v.isNetworkConnectionRequired()?" · needs internet":" · offline";
        float hz=p.getFloat("voice_f0_"+base,0);String kind=hz<=0?"":hz>=165?" · girl":" · boy";
        return "Phone voice "+(index+1)+" ("+base.substring(base.lastIndexOf('-')+1)+")"+kind+where;
    }
    /** Prefers an installed offline copy (faster, private), then the online copy. */
    static Voice resolve(TextToSpeech tts,Locale locale,String base){
        Voice offline=null,online=null;
        try{for(Voice v:tts.getVoices())if(matches(v,locale)&&base(v.getName()).equals(base)){
            boolean missing=v.getFeatures()!=null&&v.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED);
            if(!v.isNetworkConnectionRequired()&&!missing)offline=v;else if(v.isNetworkConnectionRequired())online=v;
        }}catch(RuntimeException ignored){}
        return offline!=null?offline:online;
    }
    static boolean matches(Voice v,Locale locale){
        Locale l=v.getLocale();return l!=null&&l.getLanguage().equals(locale.getLanguage())&&(locale.getCountry().isEmpty()||locale.getCountry().equals(l.getCountry()));
    }
    static String base(String name){return name.replaceFirst("-(local|network)$","");}

    /** Median speaking pitch in Hz, by autocorrelation over voiced 30 ms frames; 0 when unclear. Above ~165 Hz usually sounds like a girl. */
    static float pitchHz(short[] pcm,int rate){
        int frame=rate*30/1000,minLag=rate/400,maxLag=rate/70;List<Float> found=new ArrayList<>();
        for(int start=0;start+frame+maxLag<pcm.length;start+=frame){
            double energy=0;for(int i=0;i<frame;i++)energy+=(double)pcm[start+i]*pcm[start+i];
            if(energy/frame<500*500)continue;
            double best=0;int bestLag=0;
            for(int lag=minLag;lag<=maxLag;lag++){double sum=0;for(int i=0;i<frame;i++)sum+=(double)pcm[start+i]*pcm[start+i+lag];if(sum>best){best=sum;bestLag=lag;}}
            if(bestLag>0&&best>0.3*energy)found.add((float)rate/bestLag);
        }
        if(found.size()<3)return 0;Collections.sort(found);return found.get(found.size()/2);
    }
}
