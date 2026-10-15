package life.milo.companion;

import java.util.*;
import java.util.regex.*;

/**
 * Milo's own speaking tone, chosen by the model as tags such as "[playful]" at the start of a reply or of a later sentence.
 * It drives the pet's expression and voice; it never describes the user's feelings.
 */
final class ToneTag {
    static final String[] TONES={"happy","playful","curious","comfort","calm","celebrate","concerned","surprised","excited","laughing","shy","grateful","sad","confused","sleepy","oops","angry"};
    static final Pattern LEADING=Pattern.compile("^\\s*\\[([A-Za-z]{2,12})\\]\\s*");
    static final Pattern ANY=Pattern.compile("\\[("+String.join("|",TONES)+")\\]\\s*",Pattern.CASE_INSENSITIVE);
    static final Pattern PARTIAL=Pattern.compile("\\[[A-Za-z]{0,11}$");
    /** Latest tone seen in a reply that is still streaming. */
    static volatile String live;
    /** Where each tone starts in the visible reply text: {offset, tone index}. */
    private static final List<int[]> marks=Collections.synchronizedList(new ArrayList<>());

    static String instruction(){
        return " Start every reply with exactly one tone tag for your own speaking tone, chosen from [happy] [playful] [curious] [comfort] [calm] [celebrate] [concerned] [surprised] [excited] [laughing] [shy] [grateful] [sad] [confused] [sleepy] [oops] [angry], then a space and the reply."
            +" If your tone clearly changes, you may start one later sentence with a different tag; use at most two tags in a reply."
            +" Use [comfort] or [calm] when the user is having a hard time, [sad] only to share sympathy about sad news, [celebrate] or [excited] for their progress and good news, [surprised] for unexpected news, [laughing] for a joke,"
            +" [shy] when complimented, [grateful] when thanked, [confused] when you need them to clarify, [sleepy] for goodnight, [oops] when correcting your own mistake, [concerned] for safety."
            +" Use [angry] only for playful pretend anger in a game or story, or for indignation about an unfair situation the user describes; never be angry at the user or about their choices. Never mention the tags.";
    }
    static boolean known(String tone){for(String t:TONES)if(t.equals(tone))return true;return false;}
    static int index(String tone){for(int i=0;i<TONES.length;i++)if(TONES[i].equals(tone))return i;return -1;}

    /** Text without tone tags; null while a leading tag may still be arriving. A half-written tag at the end is held back. */
    static String visible(String text){
        String trimmed=text.replaceFirst("^\\s+","");
        if(trimmed.startsWith("[")&&trimmed.indexOf(']')<0)return trimmed.length()<16?null:trimmed;
        Matcher m=LEADING.matcher(trimmed);
        if(m.find())trimmed=trimmed.substring(m.end());
        trimmed=ANY.matcher(trimmed).replaceAll("");
        return PARTIAL.matcher(trimmed).replaceFirst("");
    }
    /** The leading tag's tone if it is one of {@link #TONES}, otherwise null. */
    static String tone(String text){
        Matcher m=LEADING.matcher(text);
        if(!m.find())return null;String t=m.group(1).toLowerCase(Locale.ROOT);return known(t)?t:null;
    }
    /** The last tone tag in the text, or null. */
    static String lastTone(String text){String last=null;Matcher m=ANY.matcher(text);while(m.find())last=m.group(1).toLowerCase(Locale.ROOT);return last!=null?last:tone(text);}

    /** Starts a new streamed reply. */
    static void reset(){live=null;marks.clear();}
    /** Records where each tag starts in the visible text of a streaming reply. */
    static void track(String raw){
        List<int[]> found=new ArrayList<>();Matcher m=ANY.matcher(raw);
        while(m.find()){String before=visible(raw.substring(0,m.start()));found.add(new int[]{before==null?0:before.length(),index(m.group(1).toLowerCase(Locale.ROOT))});}
        synchronized(marks){marks.clear();marks.addAll(found);}
        String t=lastTone(raw);if(t!=null)live=t;
    }
    /** The tone in effect at a position of the visible reply (the start of the sentence being spoken). */
    static String toneAt(int offset){
        String tone=null;
        synchronized(marks){for(int[] mk:marks)if(mk[0]<=offset+2&&mk[1]>=0)tone=TONES[mk[1]];}
        return tone!=null?tone:live;
    }
}
