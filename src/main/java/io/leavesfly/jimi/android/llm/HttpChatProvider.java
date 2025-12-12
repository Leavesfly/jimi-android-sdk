package io.leavesfly.jimi.android.llm;

import io.leavesfly.jimi.android.llm.message.Message;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 基于 HttpURLConnection 的 ChatProvider 实现
 * - 零第三方依赖
 * - 支持 SSE 流式响应
 * - 兼容 OpenAI API 标准（Kimi/DeepSeek/Qwen 等）
 */
public class HttpChatProvider implements ChatProvider {

    private static final String TAG = "HttpChatProvider";
    private static final int CONNECT_TIMEOUT = 30_000;
    private static final int READ_TIMEOUT = 120_000;

    private final String modelName;
    private final String baseUrl;
    private final String apiKey;
    private final int maxContextSize;
    private final ExecutorService executor;

    private volatile boolean isShutdown = false;

    public HttpChatProvider(String modelName, String baseUrl, String apiKey, int maxContextSize) {
        this.modelName = modelName;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.maxContextSize = maxContextSize;
        this.executor = Executors.newCachedThreadPool();
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public int getMaxContextSize() {
        return maxContextSize;
    }

    @Override
    public void generateStream(
            String systemPrompt,
            List<Message> history,
            List<ToolSchema> tools,
            StreamCallback<ChatCompletionChunk> callback) {

        if (isShutdown) {
            callback.onError(new IllegalStateException("ChatProvider has been shutdown"));
            return;
        }

        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                // 创建连接
                URL url = new URL(baseUrl + "/chat/completions");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                connection.setRequestProperty("Accept", "text/event-stream");
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);

                // 构建请求体
                String requestBody = buildRequestBody(systemPrompt, history, tools);

                // 发送请求
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(requestBody.getBytes("UTF-8"));
                    os.flush();
                }

                // 检查响应码
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    String error = readErrorResponse(connection);
                    callback.onError(new IOException("HTTP " + responseCode + ": " + error));
                    return;
                }

                // 解析 SSE 流
                parseSSEStream(connection.getInputStream(), callback);

            } catch (Exception e) {
                callback.onError(e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    /**
     * 构建 OpenAI 格式的请求体
     */
    private String buildRequestBody(
            String systemPrompt,
            List<Message> history,
            List<ToolSchema> tools) throws Exception {

        JSONObject json = new JSONObject();
        json.put("model", modelName);
        json.put("stream", true);

        // 构建 messages 数组
        JSONArray messages = new JSONArray();

        // System message
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.put(systemMsg);
        }

        // History messages
        if (history != null) {
            for (Message msg : history) {
                messages.put(msg.toJson());
            }
        }

        json.put("messages", messages);

        // Tools
        if (tools != null && !tools.isEmpty()) {
            JSONArray toolsArray = new JSONArray();
            for (ToolSchema tool : tools) {
                toolsArray.put(tool.toJson());
            }
            json.put("tools", toolsArray);
        }

        return json.toString();
    }

    /**
     * 解析 SSE 流
     */
    private void parseSSEStream(
            InputStream inputStream,
            StreamCallback<ChatCompletionChunk> callback) throws IOException {

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, "UTF-8"))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // 跳过空行
                if (line.isEmpty()) {
                    continue;
                }

                // SSE 格式: "data: {...}"
                if (line.startsWith("data: ")) {
                    String json = line.substring(6).trim();

                    // 结束标记
                    if ("[DONE]".equals(json)) {
                        callback.onComplete();
                        return;
                    }

                    try {
                        ChatCompletionChunk chunk = parseChunk(json);
                        if (chunk != null) {
                            callback.onNext(chunk);
                        }
                    } catch (Exception e) {
                        // 解析单个 chunk 失败，记录警告但继续处理
                        System.err.println(TAG + ": Parse chunk failed: " + e.getMessage());
                    }
                }
            }
            // 流结束但没有 [DONE] 标记
            callback.onComplete();
        }
    }

    /**
     * 解析单个响应块
     */
    private ChatCompletionChunk parseChunk(String json) throws Exception {
        JSONObject obj = new JSONObject(json);

        ChatCompletionChunk chunk = new ChatCompletionChunk();
        chunk.setId(obj.optString("id", null));

        JSONArray choices = obj.optJSONArray("choices");
        if (choices != null && choices.length() > 0) {
            JSONObject choice = choices.getJSONObject(0);
            JSONObject delta = choice.optJSONObject("delta");

            if (delta != null) {
                // 解析内容
                String content = delta.optString("content", null);
                if (content != null && !content.isEmpty()) {
                    chunk.setContent(content);
                }

                // 解析工具调用
                JSONArray toolCalls = delta.optJSONArray("tool_calls");
                if (toolCalls != null && toolCalls.length() > 0) {
                    chunk.setToolCalls(parseToolCallDeltas(toolCalls));
                }
            }

            // 检查结束原因
            String finishReason = choice.optString("finish_reason", null);
            if (finishReason != null && !"null".equals(finishReason)) {
                chunk.setFinishReason(finishReason);
            }
        }

        // 解析 usage（部分 API 会在最后一个 chunk 返回）
        JSONObject usage = obj.optJSONObject("usage");
        if (usage != null) {
            chunk.setPromptTokens(usage.optInt("prompt_tokens", 0));
            chunk.setCompletionTokens(usage.optInt("completion_tokens", 0));
        }

        return chunk;
    }

    /**
     * 解析工具调用增量列表
     */
    private List<ToolCallDelta> parseToolCallDeltas(JSONArray array) throws Exception {
        List<ToolCallDelta> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            ToolCallDelta delta = new ToolCallDelta();
            delta.setIndex(obj.optInt("index", 0));
            delta.setId(obj.optString("id", null));
            delta.setType(obj.optString("type", null));

            JSONObject function = obj.optJSONObject("function");
            if (function != null) {
                String name = function.optString("name", null);
                if (name != null && !name.isEmpty()) {
                    delta.setFunctionName(name);
                }
                String arguments = function.optString("arguments", null);
                if (arguments != null) {
                    delta.setFunctionArguments(arguments);
                }
            }
            result.add(delta);
        }
        return result;
    }

    /**
     * 读取错误响应
     */
    private String readErrorResponse(HttpURLConnection connection) {
        try {
            InputStream es = connection.getErrorStream();
            if (es != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(es, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                return sb.toString();
            }
        } catch (Exception e) {
            // ignore
        }
        return "Unknown error";
    }

    @Override
    public void shutdown() {
        isShutdown = true;
        executor.shutdown();
    }
}
