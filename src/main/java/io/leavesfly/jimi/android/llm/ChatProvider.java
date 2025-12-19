package io.leavesfly.jimi.android.llm;

import io.leavesfly.jimi.android.llm.message.Message;
import io.leavesfly.jimi.android.tool.ToolSchema;

import java.util.List;

/**
 * ChatProvider 接口 (Android版)
 * 定义与 LLM 交互的核心接口，使用回调模式替代 Reactor
 */
public interface ChatProvider {

    /**
     * 获取模型名称
     *
     * @return 模型名称
     */
    String getModelName();

    /**
     * 获取最大上下文大小
     *
     * @return 最大上下文 token 数
     */
    int getMaxContextSize();

    /**
     * 生成聊天完成（流式）
     *
     * @param systemPrompt 系统提示词
     * @param history      历史消息列表
     * @param tools        可用工具列表（可为 null）
     * @param callback     流式响应回调
     */
    void generateStream(
            String systemPrompt,
            List<Message> history,
            List<ToolSchema> tools,
            StreamCallback<ChatCompletionChunk> callback
    );

    /**
     * 关闭并释放资源
     */
    void shutdown();
}
