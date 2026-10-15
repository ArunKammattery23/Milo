package life.milo.companion;

/** Explicit user preferences and illustrative expression cues, never inferred feelings. */
final class CompanionStyle {
    static final String[] STYLES={"Gentle","Playful","Straightforward"};
    static String normalize(String style){for(String item:STYLES)if(item.equals(style))return item;return "Gentle";}
    static String instruction(String style){
        String tone=normalize(style).equals("Playful")?"Be an extra silly, giggly friend who loves jokes and games, but drop the jokes when they are hurting.":normalize(style).equals("Straightforward")?"Be an honest, down-to-earth friend: say things simply and kindly, with little fuss.":"Be a soft, gentle and patient friend who is easy to talk to.";
        return " Your style: "+tone+" If they skip a plan or say no, that is completely fine; never sulk. Never suggest hurting anyone. Expressions only illustrate your tone.";
    }
    static String forMood(String mood){
        if("Stressed".equals(mood)||"Low".equals(mood))return "comfort";
        if("Tired".equals(mood))return "calm";
        if("Excited".equals(mood))return "celebrate";
        return "happy";
    }
    static String label(String state){switch(state){
        case "listening":return "Listening";case "thinking":return "Thinking";case "speaking":return "Speaking";
        case "comfort":return "Here with you";case "calm":return "A gentle pause";case "curious":return "Ready to learn";
        case "celebrate":return "Your effort matters";case "playful":return "A little playfulness";case "concerned":return "Let’s take this slowly";
        case "surprised":return "Oh, wow!";case "excited":return "This is exciting!";case "laughing":return "That made me laugh";
        case "shy":return "Aww, thank you";case "grateful":return "Thank you for sharing";case "sad":return "I'm sorry that happened";
        case "angry":return "Hmph! That is not fair";case "confused":return "Help me understand";case "sleepy":return "Getting sleepy";case "oops":return "Oops, let me fix that";
        case "off":return "Resting · voice and reminders off";default:return "Ready when you are";
    }}
}
