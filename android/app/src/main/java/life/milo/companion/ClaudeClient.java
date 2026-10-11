package life.milo.companion;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import org.json.*;

/**
 * Anthropic Claude provider using the Messages API with SSE streaming.
 * Uses claude-3-5-haiku for lower cost; the user supplies their own key.
 */
final class ClaudeClient extends AiProvider {

    static final String DEFAULT_MODEL = "claude-haiku-4-5-20251001";
    final String MODEL;
    ClaudeClient(){this(DEFAULT_MODEL);}
    ClaudeClient(String model){MODEL=configuredModel(model,DEFAULT_MODEL);}
    static final String API_URL = "https://api.anthropic.com/v1/messages";
    static final String API_VERSION = "2023-06-01";

    @Override String displayName() { return "Anthropic Claude"; }
    @Override String id() { return CLAUDE; }
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
        JSONArray turns=chatMessages(history,prompt);for(int i=0;i<turns.length();i++)messages.put(turns.get(i));

        JSONObject body = new JSONObject()
                .put("model", MODEL)
                .put("system", instruction)
                .put("messages", messages)
                .put("max_tokens", 1024)
                .put("stream", true);

        HttpURLConnection conn = (HttpURLConnection)
                new URL(API_URL).openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(60000);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-api-key", key);
            conn.setRequestProperty("anthropic-version", API_VERSION);
            conn.setDoOutput(true);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                if (code == 429) throw new MainActivity.ApiFailure(
                        "Claude rate limit reached. Wait a moment and try again.");
                if (code == 401) throw new MainActivity.ApiFailure(
                        "Claude rejected your key. Check it in Setup.");
                throw new MainActivity.ApiFailure(
                        "Claude returned an error (" + code + "). Try again later.");
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                return parseClaudeSse(reader, partial);
            }
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Claude uses a different SSE format from OpenAI:
     * event: content_block_delta
     * data: {"type":"content_block_delta","delta":{"type":"text_delta","text":"..."}}
     */
    static MainActivity.GeminiReply parseClaudeSse(BufferedReader reader,Consumer<String> partial)throws Exception{
        StringBuilder answer=new StringBuilder();int[] total={0};String data;boolean complete=false,ended=false;
        while((data=nextSseData(reader,total))!=null){JSONObject json=streamJson(data);String type=json.optString("type");if(type.equals("error"))throw new MainActivity.ApiFailure("Claude interrupted the reply. Try again later.");if(type.equals("content_block_delta")){JSONObject delta=json.optJSONObject("delta");if(delta!=null&&delta.optString("type").equals("text_delta")){if(ended)throw new IOException("Text arrived after the reply ended");answer.append(delta.optString("text"));partial.accept(answer.toString());}}if(type.equals("message_delta")){JSONObject delta=json.optJSONObject("delta");if(delta!=null&&!delta.isNull("stop_reason")){String reason=delta.optString("stop_reason");if(!reason.equals("end_turn")&&!reason.equals("stop_sequence"))throw new MainActivity.ApiFailure("Claude’s reply was cut short or requires unsupported tools. Try a shorter request.");ended=true;}}if(type.equals("message_stop")){complete=true;break;}}
        if(!complete||!ended)throw new IOException("Claude response was interrupted. Try again.");return textReply(answer.toString());
    }
}
