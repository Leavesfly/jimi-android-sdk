package io.leavesfly.jimi.android.core.engine;

import io.leavesfly.jimi.android.core.agent.Agent;
import io.leavesfly.jimi.android.core.compaction.Compaction;
import io.leavesfly.jimi.android.core.context.Context;
import io.leavesfly.jimi.android.core.runtime.Runtime;
import io.leavesfly.jimi.android.core.wire.Wire;
import io.leavesfly.jimi.android.core.wire.WireListener;
import io.leavesfly.jimi.android.llm.message.ContentPart;
import io.leavesfly.jimi.android.tool.ToolRegistry;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * JimiEngine 实现
 * 
 * 职责：
 * - 组件装配和协调
 * - 提供统一的 Engine 接口实现
 * - 管理组件生命周期
 * 
 * 设计特点：
 * - 委托模式：专注协调，AgentExecutor 负责执行
 * - 回调驱动：使用回调接口处理异步结果
 * - 线程隔离：后台线程执行，回调线程通知
 */
public class JimiEngine implements Engine {
    
    private final Agent agent;
    private final Runtime runtime;
    private final Context context;
    private final Wire wire;
    private final Compaction compaction;
    private final ToolRegistry toolRegistry;
    private final AgentExecutor executor;
    
    // 后台线程池
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    // 回调线程池
    private final ExecutorService callbackExecutor = Executors.newSingleThreadExecutor();
    
    public JimiEngine(
            Agent agent,
            Runtime runtime,
            Context context,
            Wire wire,
            Compaction compaction) {
        this(agent, runtime, context, wire, compaction, null);
    }
    
    public JimiEngine(
            Agent agent,
            Runtime runtime,
            Context context,
            Wire wire,
            Compaction compaction,
            ToolRegistry toolRegistry) {
        this.agent = agent;
        this.runtime = runtime;
        this.context = context;
        this.wire = wire;
        this.compaction = compaction;
        this.toolRegistry = toolRegistry;
        this.executor = new AgentExecutor(
            agent, runtime, context, wire, compaction, toolRegistry
        );
    }
    
    @Override
    public String getName() {
        return agent.getName();
    }
    
    @Override
    public String getModel() {
        return runtime.getConfig().getModelName();
    }
    
    @Override
    public void run(String userInput, EngineCallback callback) {
        run(ContentPart.textList(userInput), callback);
    }
    
    @Override
    public void run(List<ContentPart> userInput, EngineCallback callback) {
        executorService.execute(() -> {
            try {
                executor.execute(userInput);
                notifyCallback(() -> callback.onComplete());
            } catch (Exception e) {
                notifyCallback(() -> callback.onError(e));
            }
        });
    }
    
    @Override
    public void addWireListener(WireListener listener) {
        wire.addListener(listener);
    }
    
    @Override
    public void removeWireListener(WireListener listener) {
        wire.removeListener(listener);
    }
    
    @Override
    public void clearHistory() {
        context.clear();
    }
    
    @Override
    public void shutdown() {
        executorService.shutdown();
        callbackExecutor.shutdown();
    }
    
    /**
     * 在回调线程通知回调
     */
    private void notifyCallback(Runnable action) {
        callbackExecutor.execute(action);
    }
}
