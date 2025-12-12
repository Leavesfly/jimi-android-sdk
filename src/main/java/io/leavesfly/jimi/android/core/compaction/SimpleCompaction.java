package io.leavesfly.jimi.android.core.compaction;

import io.leavesfly.jimi.android.core.context.Context;
import io.leavesfly.jimi.android.llm.message.Message;
import java.util.List;

/**
 * 简单压缩实现
 * 
 * 简化版：仅保留最近的消息
 */
public class SimpleCompaction implements Compaction {
    
    private static final int MAX_MESSAGES_TO_KEEP = 10;
    
    @Override
    public void compact(Context context) {
        List<Message> history = context.getHistory();
        
        if (history.size() > MAX_MESSAGES_TO_KEEP) {
            // 简化实现：清空并保留最近的消息
            // 实际实现应该更智能地保留关键信息
            context.clear();
            
            int startIndex = history.size() - MAX_MESSAGES_TO_KEEP;
            for (int i = startIndex; i < history.size(); i++) {
                context.appendMessage(history.get(i));
            }
            
            // 重新估算 Token
            int estimatedTokens = MAX_MESSAGES_TO_KEEP * 100; // 简化估算
            context.updateTokenCount(estimatedTokens);
        }
    }
}
