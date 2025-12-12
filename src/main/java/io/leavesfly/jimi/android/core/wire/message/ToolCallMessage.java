package io.leavesfly.jimi.android.core.wire.message;

import io.leavesfly.jimi.android.llm.message.ToolCall;

/**
 * 工具调用消息
 * 当 LLM 返回工具调用时发送此消息
 */
public class ToolCallMessage extends WireMessage {

    private final ToolCall toolCall;

    public ToolCallMessage(ToolCall toolCall) {
        this.toolCall = toolCall;
    }

    @Override
    public String getType() {
        return "tool_call";
    }

    public ToolCall getToolCall() {
        return toolCall;
    }

    /**
     * 获取工具名称
     */
    public String getToolName() {
        if (toolCall != null && toolCall.getFunction() != null) {
            return toolCall.getFunction().getName();
        }
        return null;
    }

    /**
     * 获取工具参数
     */
    public String getArguments() {
        if (toolCall != null && toolCall.getFunction() != null) {
            return toolCall.getFunction().getArguments();
        }
        return null;
    }
}
