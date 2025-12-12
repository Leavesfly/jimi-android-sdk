package io.leavesfly.jimi.android.tool;

/**
 * 工具执行结果
 */
public class ToolResult {

    private final boolean success;
    private final String content;
    private final String error;

    private ToolResult(boolean success, String content, String error) {
        this.success = success;
        this.content = content;
        this.error = error;
    }

    /**
     * 创建成功结果
     */
    public static ToolResult success(String content) {
        return new ToolResult(true, content, null);
    }

    /**
     * 创建失败结果
     */
    public static ToolResult error(String message) {
        return new ToolResult(false, null, message);
    }

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 获取内容（成功时）
     */
    public String getContent() {
        return content;
    }

    /**
     * 获取错误信息（失败时）
     */
    public String getError() {
        return error;
    }

    /**
     * 转换为字符串（用于返回给 LLM）
     */
    public String toResultString() {
        if (success) {
            return content != null ? content : "";
        } else {
            return "[Error] " + (error != null ? error : "Unknown error");
        }
    }

    @Override
    public String toString() {
        return "ToolResult{" +
                "success=" + success +
                ", content='" + content + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
}
