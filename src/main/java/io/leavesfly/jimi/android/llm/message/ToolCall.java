package io.leavesfly.jimi.android.llm.message;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 工具调用实体
 * 对应 LLM 返回的 tool_calls 中的单个调用
 */
public class ToolCall {

    /**
     * 工具调用的唯一 ID
     */
    private String id;

    /**
     * 类型，固定为 "function"
     */
    private String type;

    /**
     * 函数调用详情
     */
    private FunctionCall function;

    public ToolCall() {
        this.type = "function";
    }

    public ToolCall(String id, FunctionCall function) {
        this.id = id;
        this.type = "function";
        this.function = function;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public FunctionCall getFunction() {
        return function;
    }

    public void setFunction(FunctionCall function) {
        this.function = function;
    }

    /**
     * 转换为 JSON
     */
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("type", type);
        if (function != null) {
            json.put("function", function.toJson());
        }
        return json;
    }

    /**
     * 从 JSON 解析
     */
    public static ToolCall fromJson(JSONObject json) throws JSONException {
        ToolCall toolCall = new ToolCall();
        toolCall.setId(json.optString("id", null));
        toolCall.setType(json.optString("type", "function"));

        JSONObject functionJson = json.optJSONObject("function");
        if (functionJson != null) {
            toolCall.setFunction(FunctionCall.fromJson(functionJson));
        }

        return toolCall;
    }

    @Override
    public String toString() {
        return "ToolCall{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", function=" + function +
                '}';
    }
}
