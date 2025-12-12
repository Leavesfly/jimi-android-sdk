package io.leavesfly.jimi.android.core.engine;

/**
 * Engine 执行结果回调接口
 * 
 * 用于异步通知 Engine 执行完成或失败
 */
public interface EngineCallback {
    
    /**
     * 执行成功完成
     */
    void onComplete();
    
    /**
     * 执行失败
     * 
     * @param error 错误信息
     */
    void onError(Throwable error);
}
