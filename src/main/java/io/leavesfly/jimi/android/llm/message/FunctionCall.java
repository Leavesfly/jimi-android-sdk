package io.leavesfly.jimi.android.llm.message;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 函数调用详情
 * 包含函数名称和参数
 */
public class FunctionCall {

    /**
     * 函数名称
     */
    private String name;

    /**
     * 函数参数（JSON 格式字符串）
     */
    private String arguments;

    public FunctionCall() {
    }

    public FunctionCall(String name, String arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    /**
     * 获取参数作为 JSONObject
     */
    public JSONObject getArgumentsAsJson() throws JSONException {
        if (arguments == null || arguments.isEmpty()) {
            return new JSONObject();
        }
        return new JSONObject(arguments);
    }

    /**
     * 转换为 JSON
     */
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("arguments", arguments);
        return json;
    }

    /**
     * 从 JSON 解析
     */
    public static FunctionCall fromJson(JSONObject json) throws JSONException {
        FunctionCall functionCall = new FunctionCall();
        functionCall.setName(json.optString("name", null));
        functionCall.setArguments(json.optString("arguments", null));
        return functionCall;
    }

    @Override
    public String toString() {
        return "FunctionCall{" +
                "name='" + name + '\'' +
                ", arguments='" + arguments + '\'' +
                '}';
    }
}
