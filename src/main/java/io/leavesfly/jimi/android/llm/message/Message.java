package io.leavesfly.jimi.android.llm.message;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息实体
 * 支持 OpenAI API 消息格式
 */
public class Message {

    private final MessageRole role;
    private final String content;
    private final List<ContentPart> contentParts;
    private final List<ToolCall> toolCalls;
    private final String toolCallId;

    private Message(MessageRole role, String content, List<ContentPart> contentParts,
                    List<ToolCall> toolCalls, String toolCallId) {
        this.role = role;
        this.content = content;
        this.contentParts = contentParts;
        this.toolCalls = toolCalls;
        this.toolCallId = toolCallId;
    }

    public MessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public List<ContentPart> getContentParts() {
        return contentParts != null ? new ArrayList<>(contentParts) : new ArrayList<>();
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    /**
     * 转换为 OpenAI API JSON 格式
     */
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("role", role.getValue());

        if (content != null) {
            json.put("content", content);
        }

        if (toolCalls != null && !toolCalls.isEmpty()) {
            JSONArray array = new JSONArray();
            for (ToolCall tc : toolCalls) {
                array.put(tc.toJson());
            }
            json.put("tool_calls", array);
        }

        if (toolCallId != null) {
            json.put("tool_call_id", toolCallId);
        }

        return json;
    }

    // 静态工厂方法
    public static Message user(String content) {
        return new Message(MessageRole.USER, content, null, null, null);
    }

    public static Message user(List<ContentPart> contentParts) {
        // 简化：将 ContentPart 列表转换为纯文本
        StringBuilder sb = new StringBuilder();
        for (ContentPart part : contentParts) {
            if (part.getText() != null) {
                sb.append(part.getText());
            }
        }
        return new Message(MessageRole.USER, sb.toString(), contentParts, null, null);
    }

    public static Message assistant(String content) {
        return new Message(MessageRole.ASSISTANT, content, null, null, null);
    }

    public static Message assistant(String content, List<ToolCall> toolCalls) {
        return new Message(MessageRole.ASSISTANT, content, null, toolCalls, null);
    }

    public static Message system(String content) {
        return new Message(MessageRole.SYSTEM, content, null, null, null);
    }

    public static Message toolResult(String toolCallId, String content) {
        return new Message(MessageRole.TOOL, content, null, null, toolCallId);
    }
}
