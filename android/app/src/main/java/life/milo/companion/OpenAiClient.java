package life.milo.companion;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import org.json.*;

/**
 * OpenAI-compatible provider (ChatGPT).
 * Streams via server-sent events with the standard /v1/chat/completions endpoint.
 * Uses gpt-4o-mini by default for lower cost; the user supplies their own key.
 */
final class OpenAiClient extends AiProvider {

    static final String DEFAULT_MODEL = "gpt-4.1-mini";
    final String MODEL;
    OpenAiClient(){this(DEFAULT_MODEL);}
    OpenAiClient(String model){MODEL=configuredModel(model,DEFAULT_MODEL);}
    static final String API_URL = "https://api.openai.com/v1/chat/completions";

    @Override String displayName() { return "OpenAI ChatGPT"; }
    @Override String id() { return OPENAI; }
    @Override boolean isFree() { return false; }
    @Override boolean needsNetwork() { return true; }

    @Override
    MainActivity.GeminiReply chat(
            String key, String name, String language, String practice,
            String style, String prompt, List<JSONObject> history,
            String context, boolean useSearch, Consumer<String> partial) throws Exception {

        validateKey(key,true);validateModel(MODEL);
        String instruction = systemInstruction(name, language, practice, style, context);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", instruction));

        JSONArray turns=chatMessages(history,prompt);for(int i=0;i<turns.length();i++)messages.put(turns.get(i));

        JSONObject body = new JSONObject()
                .put("model", MODEL)
                .put("messages", messages)
                .put("max_completion_tokens", 2048)
                .put("stream", true);

        HttpURLConnection conn = (HttpURLConnection)
                new URL(API_URL).openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(60000);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + key);
            conn.setDoOutput(true);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                if (code == 429) throw new MainActivity.ApiFailure(
                        "OpenAI rate limit reached. Wait a moment and try again.");
                if (code == 401) throw new MainActivity.ApiFailure(
                        "OpenAI rejected your key. Check it in Setup.");
                throw new MainActivity.ApiFailure(
                        "OpenAI returned an error (" + code + "). Try again later.");
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                return parseOpenAiSse(reader, partial);
            }
        } finally {
            conn.disconnect();
        }
    }
}
