package io.leavesfly.jimi.android.llm;

import java.util.List;

/**
 * LLM 流式响应数据块
 * 对应 OpenAI API 的 ChatCompletionChunk
 */
public class ChatCompletionChunk {

    /**
     * 响应 ID
     */
    private String id;

    /**
     * 内容增量
     */
    private String content;

    /**
     * 工具调用增量列表
     */
    private List<ToolCallDelta> toolCalls;

    /**
     * 结束原因 (stop, tool_calls, length, etc.)
     */
    private String finishReason;

    /**
     * 输入 token 数
     */
    private int promptTokens;

    /**
     * 输出 token 数
     */
    private int completionTokens;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<ToolCallDelta> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCallDelta> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
    }

    /**
     * 是否有内容
     */
    public boolean hasContent() {
        return content != null && !content.isEmpty();
    }

    /**
     * 是否有工具调用
     */
    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    /**
     * 是否是流结束
     */
    public boolean isFinished() {
        return finishReason != null;
    }

    @Override
    public String toString() {
        return "ChatCompletionChunk{" +
                "id='" + id + '\'' +
                ", content='" + content + '\'' +
                ", toolCalls=" + toolCalls +
                ", finishReason='" + finishReason + '\'' +
                '}';
    }
}
