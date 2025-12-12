package io.leavesfly.jimi.android.core.context;

import io.leavesfly.jimi.android.llm.message.Message;
import java.util.List;

/**
 * 上下文接口
 * 
 * 管理对话历史和 Token 计数
 */
public interface Context {
    
    /**
     * 获取历史消息列表
     */
    List<Message> getHistory();
    
    /**
     * 添加消息到历史
     */
    void appendMessage(Message message);
    
    /**
     * 获取当前 Token 计数
     */
    int getTokenCount();
    
    /**
     * 更新 Token 计数
     */
    void updateTokenCount(int tokenCount);
    
    /**
     * 清空历史
     */
    void clear();
    
    /**
     * 创建检查点
     */
    void checkpoint(boolean isStepCheckpoint);
    
    /**
     * 获取检查点数量
     */
    int getCheckpointCount();
}
