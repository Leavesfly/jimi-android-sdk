package io.leavesfly.jimi.android.core.compaction;

import io.leavesfly.jimi.android.core.context.Context;

/**
 * 上下文压缩接口
 */
public interface Compaction {
    
    /**
     * 压缩上下文
     * 
     * @param context 上下文
     */
    void compact(Context context);
}
