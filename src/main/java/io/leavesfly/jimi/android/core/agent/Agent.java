package io.leavesfly.jimi.android.core.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Agent 配置类
 * 
 * 定义 Agent 的名称、系统提示词和可用工具
 */
public class Agent {
    
    private final String name;
    private final String systemPrompt;
    private final List<String> tools;
    
    private Agent(Builder builder) {
        this.name = builder.name;
        this.systemPrompt = builder.systemPrompt;
        this.tools = builder.tools;
    }
    
    public String getName() {
        return name;
    }
    
    public String getSystemPrompt() {
        return systemPrompt;
    }
    
    public List<String> getTools() {
        return new ArrayList<>(tools);
    }
    
    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder 类
     */
    public static class Builder {
        private String name;
        private String systemPrompt;
        private List<String> tools = new ArrayList<>();
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }
        
        public Builder tools(List<String> tools) {
            this.tools = new ArrayList<>(tools);
            return this;
        }
        
        public Builder tools(String... tools) {
            this.tools = new ArrayList<>(Arrays.asList(tools));
            return this;
        }
        
        public Builder addTool(String tool) {
            this.tools.add(tool);
            return this;
        }
        
        public Agent build() {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Agent name cannot be null or empty");
            }
            if (systemPrompt == null || systemPrompt.isEmpty()) {
                throw new IllegalArgumentException("Agent systemPrompt cannot be null or empty");
            }
            return new Agent(this);
        }
    }
}
