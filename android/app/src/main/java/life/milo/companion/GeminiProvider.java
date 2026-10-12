package life.milo.companion;

import java.util.List;
import java.util.function.Consumer;
import org.json.*;

/**
 * Wraps the existing GeminiClient to conform to the AiProvider interface.
 * Preserves all current Gemini behaviour including Google Search grounding.
 */
final class GeminiProvider extends AiProvider {
    final String model;
    GeminiProvider(){this("auto");}
    GeminiProvider(String model){this.model=configuredModel(model,"auto");}

    @Override String displayName() { return "Google Gemini"; }
    @Override String id() { return GEMINI; }
    @Override boolean isFree() { return false; }
    @Override boolean needsNetwork() { return true; }

    @Override
    MainActivity.GeminiReply chat(
            String key, String name, String language, String practice,
            String style, String prompt, List<JSONObject> history,
            String context, boolean useSearch, Consumer<String> partial) throws Exception {
        String selected="auto".equals(model)?GeminiModels.discover(key,useSearch):model;
        return GeminiClient.request(key, name, language, practice, style,
                prompt, history, context, useSearch, partial,selected);
    }
}
