package io.leavesfly.jimi.android.llm;

/**
 * 工具调用增量数据
 * 用于流式响应中累积工具调用信息
 */
public class ToolCallDelta {

    /**
     * 工具调用在列表中的索引
     */
    private int index;

    /**
     * 工具调用 ID（仅在第一个 chunk 中出现）
     */
    private String id;

    /**
     * 类型（通常为 "function"）
     */
    private String type;

    /**
     * 函数名称（仅在第一个 chunk 中出现）
     */
    private String functionName;

    /**
     * 函数参数增量（JSON 字符串片段）
     */
    private String functionArguments;

    // Getters and Setters
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

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

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getFunctionArguments() {
        return functionArguments;
    }

    public void setFunctionArguments(String functionArguments) {
        this.functionArguments = functionArguments;
    }

    @Override
    public String toString() {
        return "ToolCallDelta{" +
                "index=" + index +
                ", id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", functionName='" + functionName + '\'' +
                ", functionArguments='" + functionArguments + '\'' +
                '}';
    }
}
