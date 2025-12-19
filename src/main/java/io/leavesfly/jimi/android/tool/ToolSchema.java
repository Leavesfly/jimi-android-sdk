package io.leavesfly.jimi.android.tool;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 工具 Schema 定义
 * 用于描述工具的参数格式，遵循 OpenAI Function Calling 标准
 */
public class ToolSchema {

    private final String name;
    private final String description;
    private final JSONObject parameters;

    public ToolSchema(String name, String description, JSONObject parameters) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public JSONObject getParameters() {
        return parameters;
    }

    /**
     * 转换为 OpenAI Function Calling 格式的 JSON
     */
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", "function");

        JSONObject function = new JSONObject();
        function.put("name", name);
        function.put("description", description);
        if (parameters != null) {
            function.put("parameters", parameters);
        }
        json.put("function", function);

        return json;
    }

    /**
     * Builder 模式创建 ToolSchema
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private JSONObject parameters;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder parameters(JSONObject parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * 使用 JSON 字符串设置参数
         */
        public Builder parametersFromJson(String json) throws JSONException {
            this.parameters = new JSONObject(json);
            return this;
        }

        public ToolSchema build() {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Tool name cannot be null or empty");
            }
            if (description == null || description.trim().isEmpty()) {
                throw new IllegalArgumentException("Tool description cannot be null or empty");
            }
            return new ToolSchema(name, description, parameters);
        }
    }
}
