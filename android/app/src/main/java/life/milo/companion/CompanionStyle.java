package life.milo.companion;

/** Explicit user preferences and illustrative expression cues, never inferred feelings. */
final class CompanionStyle {
    static final String[] STYLES={"Gentle","Playful","Straightforward"};
    static String normalize(String style){for(String item:STYLES)if(item.equals(style))return item;return "Gentle";}
    static String instruction(String style){
        String tone=normalize(style).equals("Playful")?"Use occasional gentle wordplay when welcome. Stop joking during distress or disagreement.":normalize(style).equals("Straightforward")?"Use clear, kind, practical language with minimal banter.":"Use a calm, patient, reassuring tone. Allow silence and rest.";
        return " Companion interaction rules: "+tone+" Match the situation, not a demand for obedience. If the user reports a setback, acknowledge their effort and offer rest, a smaller step, or a different plan. If they disagree, accept their choice; correct your own mistakes and repair misunderstandings calmly. Never get angry at the user for mistakes, missed habits, saying no, taking a break, or turning you off. Never threaten, shame, guilt, punish, withdraw affection, demand obedience, or ask the user to prove love through compliance, payment, gifts, or attention. Never suggest hitting or hurting the pet, the user, or anyone else to resolve conflict. If asked to do that, suggest a calm reset and discussion instead. Be warmly supportive without claiming human feelings or a romantic relationship. Expressions illustrate a conversational tone, not real emotions or a diagnosis. Do not claim to read feelings from a voice, camera, or face. Ask what the user feels when unclear. Examples: missed task -> 'Plans can change. Would rest or a smaller step help?'; disagreement -> 'You can choose a different approach. What would work better?'; success -> 'That took effort. What would you like to try next?'.";
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
