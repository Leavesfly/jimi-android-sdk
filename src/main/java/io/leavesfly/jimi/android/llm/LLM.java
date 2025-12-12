package io.leavesfly.jimi.android.llm;

/**
 * LLM 包装类
 * 封装 ChatProvider，提供统一的 LLM 接口
 */
public class LLM {

    private final String modelName;
    private final ChatProvider chatProvider;
    private final int maxContextSize;

    public LLM(String modelName, ChatProvider chatProvider, int maxContextSize) {
        this.modelName = modelName;
        this.chatProvider = chatProvider;
        this.maxContextSize = maxContextSize;
    }

    /**
     * 获取模型名称
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * 获取 ChatProvider
     */
    public ChatProvider getChatProvider() {
        return chatProvider;
    }

    /**
     * 获取最大上下文大小
     */
    public int getMaxContextSize() {
        return maxContextSize;
    }

    /**
     * 关闭并释放资源
     */
    public void shutdown() {
        if (chatProvider != null) {
            chatProvider.shutdown();
        }
    }
}
