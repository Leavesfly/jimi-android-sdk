package io.leavesfly.jimi.android.llm;

/**
 * 流式响应回调接口
 * 
 * @param <T> 数据块类型
 */
public interface StreamCallback<T> {
    
    /**
     * 收到数据块
     * 
     * @param chunk 数据块
     */
    void onNext(T chunk);
    
    /**
     * 流完成
     */
    void onComplete();
    
    /**
     * 流错误
     * 
     * @param error 错误信息
     */
    void onError(Throwable error);
}
