package io.leavesfly.jimi.android.core.engine;

/**
 * Engine 常量定义
 */
public class EngineConstants {
    
    /**
     * 预留 Token 数（用于上下文压缩判断）
     */
    public static final int RESERVED_TOKENS = 2000;
    
    /**
     * 最大连续思考步数（无工具调用）
     */
    public static final int MAX_THINKING_STEPS = 5;
    
    /**
     * 默认最大步数
     */
    public static final int DEFAULT_MAX_STEPS = 10;
    
    private EngineConstants() {
        // 工具类，禁止实例化
    }
}
