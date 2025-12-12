package io.leavesfly.jimi.android.sdk;

import io.leavesfly.jimi.android.core.agent.Agent;
import io.leavesfly.jimi.android.core.compaction.Compaction;
import io.leavesfly.jimi.android.core.compaction.SimpleCompaction;
import io.leavesfly.jimi.android.core.context.Context;
import io.leavesfly.jimi.android.core.context.ContextImpl;
import io.leavesfly.jimi.android.core.engine.Engine;
import io.leavesfly.jimi.android.core.engine.JimiEngine;
import io.leavesfly.jimi.android.core.runtime.Runtime;
import io.leavesfly.jimi.android.core.runtime.RuntimeImpl;
import io.leavesfly.jimi.android.core.wire.Wire;
import io.leavesfly.jimi.android.core.wire.WireImpl;
import io.leavesfly.jimi.android.llm.LLM;
import io.leavesfly.jimi.android.llm.LLMCache;
import io.leavesfly.jimi.android.llm.LLMFactory;
import io.leavesfly.jimi.android.tool.Tool;
import io.leavesfly.jimi.android.tool.ToolRegistry;

/**
 * Jimi 工厂类
 * 
 * 负责创建 Engine 实例和组件装配
 */
public class JimiFactory {
    
    private final JimiConfig config;
    private final LLMCache llmCache;
    private final ToolRegistry toolRegistry;
    
    public JimiFactory(JimiConfig config) {
        this(config, new LLMCache(), new ToolRegistry());
    }
    
    public JimiFactory(JimiConfig config, LLMCache llmCache) {
        this(config, llmCache, new ToolRegistry());
    }
    
    public JimiFactory(JimiConfig config, LLMCache llmCache, ToolRegistry toolRegistry) {
        this.config = config;
        this.llmCache = llmCache;
        this.toolRegistry = toolRegistry;
        this.config.validate();
    }
    
    /**
     * 创建 Engine 实例
     * 
     * @param agent Agent 配置
     * @return Engine 实例
     */
    public Engine createEngine(Agent agent) {
        // 创建或获取缓存的 LLM
        LLM llm = getOrCreateLLM();
        
        // 创建组件
        Runtime runtime = new RuntimeImpl(config, llm);
        Context context = new ContextImpl();
        Wire wire = new WireImpl();
        Compaction compaction = new SimpleCompaction();
        
        // 装配 Engine
        return new JimiEngine(agent, runtime, context, wire, compaction, toolRegistry);
    }
    
    /**
     * 注册工具
     * 
     * @param tool 工具实例
     */
    public void registerTool(Tool tool) {
        toolRegistry.register(tool);
    }
    
    /**
     * 获取工具注册表
     */
    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }
    
    /**
     * 获取或创建 LLM 实例
     */
    private LLM getOrCreateLLM() {
        String modelName = config.getModelName();
        
        // 从缓存获取或创建新实例
        return llmCache.getOrCreate(modelName, name -> LLMFactory.create(config));
    }
    
    /**
     * 获取 LLM 缓存
     */
    public LLMCache getLLMCache() {
        return llmCache;
    }
    
    /**
     * 关闭工厂，释放资源
     */
    public void shutdown() {
        llmCache.clear();
    }
}
