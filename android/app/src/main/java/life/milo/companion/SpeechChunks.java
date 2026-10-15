package life.milo.companion;

/** Shared streaming policy for Talk and the floating pet. Never emit a partial word. */
final class SpeechChunks {
    private SpeechChunks() {}

    static int boundary(String text, int start) {
        if (text == null || start < 0 || start >= text.length()) return start;
        int lastSpace = start;
        int clauseMinimum = start == 0 ? 40 : 70;
        for (int i = start; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isWhitespace(ch)) lastSpace = i + 1;
            if ("!?。！？।\n".indexOf(ch) >= 0 && i - start >= 4) return i + 1;
            // A period at the end of a streaming delta may become 1.30 or example.com.
            // Wait for its next character; final text is flushed by the caller.
            if (ch == '.' && i - start >= 4 && i + 1 < text.length()
                    && Character.isWhitespace(text.charAt(i + 1))) {
                int word = i;
                while (word > start && Character.isLetter(text.charAt(word - 1))) word--;
                String token = text.substring(word, i).toLowerCase(java.util.Locale.ROOT);
                if (!java.util.Set.of("mr", "mrs", "ms", "dr", "prof", "sr", "jr", "st", "vs", "etc").contains(token)
                        && token.length() != 1) return i + 1;
            }
            if (",;:".indexOf(ch) >= 0 && i - start >= clauseMinimum
                    && i + 1 < text.length() && Character.isWhitespace(text.charAt(i + 1))) return i + 1;
            if (i - start >= 180 && lastSpace > start) return lastSpace;
        }
        return start;
    }
}
