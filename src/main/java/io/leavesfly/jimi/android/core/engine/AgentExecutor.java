package io.leavesfly.jimi.android.core.engine;

import io.leavesfly.jimi.android.core.agent.Agent;
import io.leavesfly.jimi.android.core.compaction.Compaction;
import io.leavesfly.jimi.android.core.context.Context;
import io.leavesfly.jimi.android.core.runtime.Runtime;
import io.leavesfly.jimi.android.core.wire.Wire;
import io.leavesfly.jimi.android.core.wire.message.ContentPartMessage;
import io.leavesfly.jimi.android.core.wire.message.StepBegin;
import io.leavesfly.jimi.android.core.wire.message.StepInterrupted;
import io.leavesfly.jimi.android.core.wire.message.ToolCallMessage;
import io.leavesfly.jimi.android.core.wire.message.ToolResultMessage;
import io.leavesfly.jimi.android.llm.ChatCompletionChunk;
import io.leavesfly.jimi.android.llm.LLM;
import io.leavesfly.jimi.android.llm.StreamCallback;
import io.leavesfly.jimi.android.llm.ToolCallAccumulator;
import io.leavesfly.jimi.android.llm.ToolCallDelta;
import io.leavesfly.jimi.android.llm.message.ContentPart;
import io.leavesfly.jimi.android.llm.message.Message;
import io.leavesfly.jimi.android.llm.message.ToolCall;
import io.leavesfly.jimi.android.tool.ToolRegistry;
import io.leavesfly.jimi.android.tool.ToolResult;
import io.leavesfly.jimi.android.tool.ToolSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Agent 执行器
 * 
 * 职责：
 * - Agent 主循环调度
 * - LLM 交互处理（流式响应）
 * - 工具调用编排（阶段三实现）
 * - 步骤流程控制
 */
public class AgentExecutor {
    
    private final Agent agent;
    private final Runtime runtime;
    private final Context context;
    private final Wire wire;
    private final Compaction compaction;
    private final ToolRegistry toolRegistry;
    
    private int consecutiveNoToolCallSteps = 0;
    
    public AgentExecutor(
            Agent agent,
            Runtime runtime,
            Context context,
            Wire wire,
            Compaction compaction) {
        this(agent, runtime, context, wire, compaction, null);
    }
    
    public AgentExecutor(
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
    }
    
    /**
     * 执行入口（同步执行，需在后台线程调用）
     */
    public void execute(List<ContentPart> userInput) throws Exception {
        // 1. 创建检查点 0
        context.checkpoint(false);
        
        // 2. 添加用户消息
        context.appendMessage(Message.user(userInput));
        
        // 3. 启动主循环
        agentLoop();
    }
    
    /**
     * Agent 主循环
     */
    private void agentLoop() throws Exception {
        int stepNo = 1;
        while (true) {
            boolean finished = agentLoopStep(stepNo);
            if (finished) {
                break;
            }
            stepNo++;
        }
    }
    
    /**
     * Agent 循环步骤
     */
    private boolean agentLoopStep(int stepNo) throws Exception {
        int maxSteps = runtime.getConfig().getMaxStepsPerRun();
        if (stepNo > maxSteps) {
            throw new RuntimeException("Max steps reached: " + maxSteps);
        }
        
        // 发送步骤开始消息
        wire.send(new StepBegin(stepNo));
        
        try {
            // 检查上下文压缩
            checkAndCompactContext();
            
            // 创建检查点
            context.checkpoint(true);
            
            // 执行单步
            return step();
        } catch (Exception e) {
            wire.send(new StepInterrupted());
            throw e;
        }
    }
    
    /**
     * 执行单步 - 调用 LLM 并处理响应
     */
    private boolean step() throws Exception {
        LLM llm = runtime.getLLM();
        
        // 如果没有配置 LLM，使用 Mock 实现
        if (llm == null) {
            return stepMock();
        }
        
        // 调用真实 LLM
        return stepWithLLM(llm);
    }
    
    /**
     * 使用真实 LLM 执行单步
     */
    private boolean stepWithLLM(LLM llm) throws Exception {
        String systemPrompt = agent.getSystemPrompt();
        List<Message> history = context.getHistory();
        
        // 获取工具 Schema 列表
        List<io.leavesfly.jimi.android.llm.ToolSchema> tools = null;
        if (toolRegistry != null && toolRegistry.size() > 0) {
            List<io.leavesfly.jimi.android.tool.ToolSchema> toolSchemas = toolRegistry.getToolSchemas(agent.getTools());
            // 转换 tool.ToolSchema 到 llm.ToolSchema
            tools = new ArrayList<>();
            for (io.leavesfly.jimi.android.tool.ToolSchema schema : toolSchemas) {
                tools.add(new io.leavesfly.jimi.android.llm.ToolSchema(
                    schema.getName(),
                    schema.getDescription(),
                    schema.getParameters()
                ));
            }
        }
        
        // 使用同步回调收集流式响应
        StringBuilder contentBuilder = new StringBuilder();
        ToolCallAccumulator toolCallAccumulator = new ToolCallAccumulator();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        llm.getChatProvider().generateStream(
            systemPrompt, history, tools,
            new StreamCallback<ChatCompletionChunk>() {
                @Override
                public void onNext(ChatCompletionChunk chunk) {
                    // 处理内容
                    if (chunk.hasContent()) {
                        String content = chunk.getContent();
                        contentBuilder.append(content);
                        wire.send(new ContentPartMessage(content));
                    }
                    
                    // 累积工具调用
                    if (chunk.hasToolCalls()) {
                        for (ToolCallDelta delta : chunk.getToolCalls()) {
                            toolCallAccumulator.accumulate(delta);
                        }
                    }
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
                
                @Override
                public void onError(Throwable error) {
                    errorRef.set(error);
                    latch.countDown();
                }
            }
        );
        
        // 等待流完成
        latch.await();
        
        // 检查错误
        if (errorRef.get() != null) {
            throw new RuntimeException("LLM call failed", errorRef.get());
        }
        
        // 构建助手消息
        String content = contentBuilder.toString();
        List<ToolCall> toolCalls = toolCallAccumulator.build();
        Message assistantMessage = Message.assistant(content, toolCalls);
        context.appendMessage(assistantMessage);
        
        // 更新 Token 计数（估算）
        int estimatedTokens = context.getTokenCount() + content.length() / 4;
        context.updateTokenCount(estimatedTokens);
        
        // 处理工具调用
        return handleToolCalls(toolCalls);
    }
    
    /**
     * 处理工具调用
     */
    private boolean handleToolCalls(List<ToolCall> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            // 无工具调用，增加计数器
            consecutiveNoToolCallSteps++;
            
            // 如果连续多步都只是思考没有行动，强制终止
            if (consecutiveNoToolCallSteps >= EngineConstants.MAX_THINKING_STEPS) {
                return true; // 强制结束
            }
            
            // LLM 返回了内容但没有工具调用，认为完成
            return true;
        }
        
        // 重置计数器
        consecutiveNoToolCallSteps = 0;
        
        // 串行执行工具调用
        for (ToolCall toolCall : toolCalls) {
            wire.send(new ToolCallMessage(toolCall));
            
            String toolResult;
            if (toolRegistry != null) {
                // 执行工具
                ToolResult result = toolRegistry.execute(
                        toolCall.getFunction().getName(),
                        toolCall.getFunction().getArguments()
                );
                toolResult = result.toResultString();
            } else {
                toolResult = "[工具未注册] " + toolCall.getFunction().getName();
            }
            
            // 添加工具结果消息
            context.appendMessage(Message.toolResult(toolCall.getId(), toolResult));
            wire.send(new ToolResultMessage(toolCall.getId(), toolResult));
        }
        
        return false; // 继续循环
    }
    
    /**
     * Mock 实现（当没有配置 LLM 时使用）
     */
    private boolean stepMock() {
        String mockResponse = "这是一个 Mock 响应，请配置 LLM 以启用真实调用";
        
        wire.send(new ContentPartMessage(mockResponse));
        context.appendMessage(Message.assistant(mockResponse));
        
        int estimatedTokens = context.getTokenCount() + mockResponse.length() / 4;
        context.updateTokenCount(estimatedTokens);
        
        consecutiveNoToolCallSteps++;
        
        if (consecutiveNoToolCallSteps >= EngineConstants.MAX_THINKING_STEPS) {
            return true;
        }
        
        return true;
    }
    
    /**
     * 检查并压缩上下文（如果需要）
     */
    private void checkAndCompactContext() {
        int tokenCount = context.getTokenCount();
        int maxContextSize = runtime.getConfig().getMaxContextSize();
        
        if (tokenCount > maxContextSize - EngineConstants.RESERVED_TOKENS) {
            compaction.compact(context);
        }
    }
}
