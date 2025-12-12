package io.leavesfly.jimi.android.core.engine;

import io.leavesfly.jimi.android.core.wire.WireListener;
import io.leavesfly.jimi.android.llm.message.ContentPart;
import java.util.List;

/**
 * Engine 接口
 * 
 * 定义 Agent 的核心行为
 */
public interface Engine {
    
    /**
     * 获取 Agent 名称
     */
    String getName();
    
    /**
     * 获取使用的模型名称
     */
    String getModel();
    
    /**
     * 运行 Agent（文本输入）
     * 
     * @param userInput 用户输入文本
     * @param callback 执行回调
     */
    void run(String userInput, EngineCallback callback);
    
    /**
     * 运行 Agent（多部分内容输入）
     * 
     * @param userInput 用户输入内容部分列表
     * @param callback 执行回调
     */
    void run(List<ContentPart> userInput, EngineCallback callback);
    
    /**
     * 添加 Wire 消息监听器
     * 
     * @param listener 监听器
     */
    void addWireListener(WireListener listener);
    
    /**
     * 移除 Wire 消息监听器
     * 
     * @param listener 监听器
     */
    void removeWireListener(WireListener listener);
    
    /**
     * 清除对话历史
     */
    void clearHistory();
    
    /**
     * 释放资源
     */
    void shutdown();
}
