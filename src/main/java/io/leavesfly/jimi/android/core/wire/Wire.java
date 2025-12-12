package io.leavesfly.jimi.android.core.wire;

import io.leavesfly.jimi.android.core.wire.message.WireMessage;

/**
 * Wire 消息总线接口
 * 
 * 用于组件间解耦通信
 */
public interface Wire {
    
    /**
     * 发送消息
     * 
     * @param message 消息
     */
    void send(WireMessage message);
    
    /**
     * 添加消息监听器
     * 
     * @param listener 监听器
     */
    void addListener(WireListener listener);
    
    /**
     * 移除消息监听器
     * 
     * @param listener 监听器
     */
    void removeListener(WireListener listener);
}
