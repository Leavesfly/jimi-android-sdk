package io.leavesfly.jimi.android.core.context;

import io.leavesfly.jimi.android.llm.message.Message;
import java.util.ArrayList;
import java.util.List;

/**
 * Context 实现类
 * 
 * 简化版实现，仅维护消息历史和 Token 计数
 */
public class ContextImpl implements Context {
    
    private final List<Message> history = new ArrayList<>();
    private int tokenCount = 0;
    private int checkpointCount = 0;
    
    @Override
    public List<Message> getHistory() {
        return new ArrayList<>(history);
    }
    
    @Override
    public void appendMessage(Message message) {
        if (message != null) {
            history.add(message);
        }
    }
    
    @Override
    public int getTokenCount() {
        return tokenCount;
    }
    
    @Override
    public void updateTokenCount(int tokenCount) {
        this.tokenCount = tokenCount;
    }
    
    @Override
    public void clear() {
        history.clear();
        tokenCount = 0;
        checkpointCount = 0;
    }
    
    @Override
    public void checkpoint(boolean isStepCheckpoint) {
        checkpointCount++;
    }
    
    @Override
    public int getCheckpointCount() {
        return checkpointCount;
    }
}
