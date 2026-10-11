package life.milo.companion;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import org.json.*;

/**
 * Custom OpenAI-compatible API endpoint.
 * The user provides a base URL (e.g. http://localhost:1234/v1) and an optional key.
 * Supports any server that speaks OpenAI's chat completion SSE protocol
 * (LM Studio, Ollama with OpenAI compat, vLLM, text-generation-webui, etc.)
 */
final class CustomApiClient extends AiProvider {

    @Override String displayName() { return "Custom API"; }
    @Override String id() { return CUSTOM; }
    @Override boolean isFree() { return true; }   // optional key; operator pricing is independent
    @Override boolean needsNetwork() { return true; }

    @Override
    MainActivity.GeminiReply chat(
            String key, String name, String language, String practice,
            String style, String prompt, List<JSONObject> history,
            String context, boolean useSearch, Consumer<String> partial) throws Exception {

        // Key format for custom: "url|model|apikey"
        // Example: "http://192.168.1.10:1234/v1|llama3|sk-xxx"
        String[] parts = key.split("\\|", -1);
        if (parts.length < 2 || parts[0].isEmpty() || parts[1].isEmpty())
            throw new MainActivity.ApiFailure(
                    "Custom API not configured. Open Setup → choose Custom API → enter your server URL and model name.");
        String baseUrl = parts[0];
        String model = parts[1];
        String apiKey = parts.length > 2 ? parts[2] : "";

        URI uri=new URI(baseUrl);if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null)throw new MainActivity.ApiFailure("Use an HTTPS API URL without credentials, query or fragment.");validateKey(apiKey,false);validateModel(model);
        String instruction = systemInstruction(name, language, practice, style, context);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", instruction));

        JSONArray turns=chatMessages(history,prompt);for(int i=0;i<turns.length();i++)messages.put(turns.get(i));

        JSONObject body = new JSONObject()
                .put("model", model)
                .put("messages", messages)
                .put("max_tokens", 1024)
                .put("stream", true);

        String endpoint = baseUrl.endsWith("/")
                ? baseUrl + "chat/completions"
                : baseUrl + "/chat/completions";

        HttpURLConnection conn = (HttpURLConnection)
                new URL(endpoint).openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(120000);  // local models can be slower
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Content-Type", "application/json");
            if (!apiKey.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            }
            conn.setDoOutput(true);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                throw new MainActivity.ApiFailure(
                        "Custom API returned error (" + code + "). Check your server URL and model name in Setup.");
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
