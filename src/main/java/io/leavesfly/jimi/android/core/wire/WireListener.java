package io.leavesfly.jimi.android.core.wire;

import io.leavesfly.jimi.android.core.wire.message.WireMessage;

/**
 * Wire 消息监听器接口
 * 
 * 用于接收 Engine 运行过程中的各种消息通知
 */
public interface WireListener {
    
    /**
     * 接收 Wire 消息
     * 
     * @param message Wire 消息
     */
    void onMessage(WireMessage message);
}
