package io.leavesfly.jimi.android.core.wire;

import io.leavesfly.jimi.android.core.wire.message.WireMessage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Wire 消息总线实现
 * 
 * - 使用 CopyOnWriteArrayList 保证线程安全
 * - 支持异步通知监听器
 */
public class WireImpl implements Wire {
    
    private final List<WireListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService notifyExecutor;
    private final boolean notifyAsync;
    
    /**
     * 构造函数（默认异步通知）
     */
    public WireImpl() {
        this(true);
    }
    
    /**
     * 构造函数
     * 
     * @param notifyAsync 是否异步通知监听器
     */
    public WireImpl(boolean notifyAsync) {
        this.notifyAsync = notifyAsync;
        this.notifyExecutor = notifyAsync ? Executors.newSingleThreadExecutor() : null;
    }
    
    @Override
    public void send(WireMessage message) {
        if (message == null) {
            return;
        }
        
        for (WireListener listener : listeners) {
            if (notifyAsync && notifyExecutor != null) {
                notifyExecutor.execute(() -> listener.onMessage(message));
            } else {
                listener.onMessage(message);
            }
        }
    }
    
    @Override
    public void addListener(WireListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    @Override
    public void removeListener(WireListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }
    
    /**
     * 关闭通知线程池
     */
    public void shutdown() {
        if (notifyExecutor != null) {
            notifyExecutor.shutdown();
        }
    }
}
