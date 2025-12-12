package io.leavesfly.jimi.android.tool;

/**
 * 工具接口
 * 定义 Agent 可调用的工具规范
 */
public interface Tool {

    /**
     * 获取工具名称
     * 必须与 LLM 调用时使用的名称一致
     */
    String getName();

    /**
     * 获取工具描述
     * 用于告诉 LLM 这个工具的用途
     */
    String getDescription();

    /**
     * 获取工具参数 Schema
     * 使用 OpenAI Function Calling 标准
     */
    ToolSchema getSchema();

    /**
     * 执行工具
     * 注意：此方法会在后台线程调用，可以执行耗时操作
     *
     * @param arguments JSON 格式的参数字符串
     * @return 执行结果
     */
    ToolResult execute(String arguments);
}
