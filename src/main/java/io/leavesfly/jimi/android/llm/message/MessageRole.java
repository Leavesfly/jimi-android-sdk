package io.leavesfly.jimi.android.llm.message;

/**
 * 消息角色枚举
 */
public enum MessageRole {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool");
    
    private final String value;
    
    MessageRole(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}
