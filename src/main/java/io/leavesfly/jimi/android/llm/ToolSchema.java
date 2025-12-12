package io.leavesfly.jimi.android.llm;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 工具 Schema 定义
 * 用于描述工具的名称、描述和参数格式
 */
public class ToolSchema {

    private String name;
    private String description;
    private JSONObject parameters;

    public ToolSchema() {
    }

    public ToolSchema(String name, String description, JSONObject parameters) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JSONObject getParameters() {
        return parameters;
    }

    public void setParameters(JSONObject parameters) {
        this.parameters = parameters;
    }

    /**
     * 转换为 OpenAI Function Calling 格式的 JSON
     *
     * @return JSON 对象
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

        public ToolSchema build() {
            return new ToolSchema(name, description, parameters);
        }
    }
}
