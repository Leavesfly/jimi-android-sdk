package io.leavesfly.jimi.android.llm;

import io.leavesfly.jimi.android.sdk.JimiConfig;

/**
 * LLM 工厂类
 * 负责创建 LLM 实例
 */
public class LLMFactory {

    /**
     * 根据配置创建 LLM 实例
     *
     * @param config SDK 配置
     * @return LLM 实例
     */
    public static LLM create(JimiConfig config) {
        return create(
                config.getModelName(),
                config.getBaseUrl(),
                config.getApiKey(),
                config.getMaxContextSize()
        );
    }

    /**
     * 创建 LLM 实例
     *
     * @param modelName      模型名称
     * @param baseUrl        API 基础 URL
     * @param apiKey         API Key
     * @param maxContextSize 最大上下文大小
     * @return LLM 实例
     */
    public static LLM create(String modelName, String baseUrl, String apiKey, int maxContextSize) {
        // 创建 HttpChatProvider
        ChatProvider chatProvider = new HttpChatProvider(modelName, baseUrl, apiKey, maxContextSize);

        // 创建 LLM 包装类
        return new LLM(modelName, chatProvider, maxContextSize);
    }
}
