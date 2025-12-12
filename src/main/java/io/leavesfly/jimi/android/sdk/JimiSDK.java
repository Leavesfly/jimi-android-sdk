package io.leavesfly.jimi.android.sdk;

import io.leavesfly.jimi.android.core.agent.Agent;
import io.leavesfly.jimi.android.core.engine.Engine;
import io.leavesfly.jimi.android.tool.Tool;
import io.leavesfly.jimi.android.tool.ToolRegistry;

/**
 * Jimi SDK 门面类
 * 
 * 提供统一的 SDK 初始化和管理接口
 */
public class JimiSDK {
    
    private static JimiFactory factory;
    private static JimiConfig currentConfig;
    private static volatile boolean initialized = false;
    
    /**
     * 初始化 SDK
     * 
     * @param config SDK 配置
     */
    public static synchronized void initialize(JimiConfig config) {
        if (initialized) {
            throw new IllegalStateException("JimiSDK already initialized");
        }
        
        currentConfig = config;
        factory = new JimiFactory(config);
        initialized = true;
    }
    
    /**
     * 创建 Engine 实例
     * 
     * @param agent Agent 配置
     * @return Engine 实例
     */
    public static Engine createEngine(Agent agent) {
        checkInitialized();
        return factory.createEngine(agent);
    }
    
    /**
     * 销毁 Engine 实例
     * 
     * @param engine Engine 实例
     */
    public static void destroyEngine(Engine engine) {
        if (engine != null) {
            engine.shutdown();
        }
    }
    
    /**
     * 获取当前配置
     */
    public static JimiConfig getConfig() {
        checkInitialized();
        return currentConfig;
    }
    
    /**
     * 注册工具
     * 
     * @param tool 工具实例
     */
    public static void registerTool(Tool tool) {
        checkInitialized();
        factory.registerTool(tool);
    }
    
    /**
     * 批量注册工具
     * 
     * @param tools 工具实例数组
     */
    public static void registerTools(Tool... tools) {
        checkInitialized();
        for (Tool tool : tools) {
            factory.registerTool(tool);
        }
    }
    
    /**
     * 获取工具注册表
     */
    public static ToolRegistry getToolRegistry() {
        checkInitialized();
        return factory.getToolRegistry();
    }
    
    /**
     * 检查是否已初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * 关闭 SDK，释放所有资源
     */
    public static synchronized void shutdown() {
        if (factory != null) {
            factory.shutdown();
            factory = null;
        }
        currentConfig = null;
        initialized = false;
    }
    
    private static void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("JimiSDK not initialized. Call initialize() first.");
        }
    }
}
