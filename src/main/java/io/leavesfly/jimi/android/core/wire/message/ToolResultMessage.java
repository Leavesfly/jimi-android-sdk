package io.leavesfly.jimi.android.core.wire.message;

/**
 * 工具执行结果消息
 * 当工具执行完成后发送此消息
 */
public class ToolResultMessage extends WireMessage {

    private final String toolCallId;
    private final String result;
    private final boolean isError;

    public ToolResultMessage(String toolCallId, String result) {
        this(toolCallId, result, false);
    }

    public ToolResultMessage(String toolCallId, String result, boolean isError) {
        this.toolCallId = toolCallId;
        this.result = result;
        this.isError = isError;
    }

    @Override
    public String getType() {
        return "tool_result";
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public String getResult() {
        return result;
    }

    public boolean isError() {
        return isError;
    }
}
