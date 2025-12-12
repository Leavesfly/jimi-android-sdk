# Jimi 核心功能移植 Android 电商购物助手技术方案

## 文档信息

| 项目 | 内容 |
|------|------|
| 版本 | v2.0 |
| 创建日期 | 2025-12-12 |
| 状态 | 待评审 |
| 实现语言 | **Java** |

---

## 一、项目背景与目标

### 1.1 项目背景

将 Jimi 的 AI Agent 核心功能从当前 CLI 应用中抽离，移植到 Android 平台，作为电商 App 内嵌的购物助手功能。

### 1.2 核心目标

1. **保持核心功能不变** - Engine/Agent/Wire 核心逻辑完全兼容
2. **允许必要的代码结构调整** - 为适配Android平台进行合理重构
3. **最小化依赖** - 控制 SDK 包大小在 500KB 以内
4. **纯Java实现** - 确保与现有Java代码库兼容
5. **易于集成** - 提供简洁的 API 供电商 App 调用

### 1.3 包大小控制策略

| 策略 | 预期节省 |
|------|----------|
| 移除 Spring Boot 依赖 | ~15MB |
| 移除 JLine 终端库 | ~1MB |
| 使用 Android JSONObject 替代 Jackson | ~2.5MB |
| 移除 Caffeine 使用 LruCache | ~500KB |
| 移除 Reactor 使用回调/CompletableFuture | ~1.5MB |
| 精简工具集（仅保留电商相关） | ~200KB |

**最终 SDK 预期大小**: 200-400KB（纯Java，零第三方依赖）

---

## 二、依赖精简方案

### 2.1 依赖替换对照表

| 原依赖 | 大小 | 替换方案 | 替换后大小 |
|--------|------|----------|-----------|
| Spring Boot 3.2.5 | ~15MB | 手动工厂模式 | 0 |
| Spring WebFlux | ~3MB | HttpURLConnection + 回调 | 0 |
| JLine 3.25.1 | ~1MB | Android UI | 0 |
| Jackson 2.16.2 | ~2.5MB | Android JSONObject | 0 (系统自带) |
| Caffeine 3.1.8 | ~500KB | Android LruCache | 0 (系统自带) |
| Reactor Core | ~1.5MB | 回调接口 + ExecutorService | 0 |
| Lombok | ~2MB | 手写 getter/setter | 0 |

### 2.2 响应式框架替换策略 (Java实现)

**问题**: 原项目大量使用 Reactor (`Mono`/`Flux`)，直接引入会增加 1.5MB 包大小。

**方案**: 使用回调接口 + ExecutorService 替代

```java
// 原 Reactor 代码
Mono<Void> execute(String input);
Flux<ChatCompletionChunk> generateStream();

// 替换为回调模式
void execute(String input, EngineCallback callback);
void generateStream(StreamCallback<ChatCompletionChunk> callback);
```

**核心回调接口设计**:

```java
/**
 * 引擎执行回调
 */
public interface EngineCallback {
    /** 执行完成 */
    void onComplete();
    /** 执行失败 */
    void onError(Throwable error);
}

/**
 * 流式响应回调
 */
public interface StreamCallback<T> {
    /** 收到数据块 */
    void onNext(T chunk);
    /** 流完成 */
    void onComplete();
    /** 流错误 */
    void onError(Throwable error);
}

/**
 * Wire 消息监听器
 */
public interface WireListener {
    void onMessage(WireMessage message);
}
```

### 2.3 JSON处理方案 (Android原生)

使用 Android 原生 JSONObject，零额外依赖：

```java
// 解析 LLM 响应
public ChatCompletionChunk parseChunk(String json) throws JSONException {
    JSONObject obj = new JSONObject(json);
    JSONArray choices = obj.getJSONArray("choices");
    JSONObject delta = choices.getJSONObject(0).optJSONObject("delta");
    
    ChatCompletionChunk chunk = new ChatCompletionChunk();
    chunk.setId(obj.optString("id"));
    if (delta != null) {
        chunk.setContent(delta.optString("content", null));
        chunk.setToolCalls(parseToolCalls(delta.optJSONArray("tool_calls")));
    }
    return chunk;
}
```

### 2.4 缓存方案 (LruCache)

使用 Android 自带 `LruCache` 替代 Caffeine:

```java
public class LLMCache {
    private static final int MAX_SIZE = 5;
    private final LruCache<String, LLM> cache = new LruCache<>(MAX_SIZE);
    
    public synchronized LLM getOrCreate(String modelName, LLMFactory factory) {
        LLM llm = cache.get(modelName);
        if (llm == null) {
            llm = factory.create(modelName);
            cache.put(modelName, llm);
        }
        return llm;
    }
    
    public interface LLMFactory {
        LLM create(String modelName);
    }
}
```

---

## 三、代码结构调整与核心模块移植

### 3.1 整体架构调整

为适配 Android 平台，对原有代码结构进行以下调整：

```
原项目结构                              Android SDK 结构
io.leavesfly.jimi/                    io.leavesfly.jimi.android/
├── agent/                            ├── core/
│   ├── Agent.java                    │   ├── agent/
│   ├── AgentSpec.java                │   │   ├── Agent.java          (移除Lombok)
│   └── AgentRegistry.java            │   │   └── AgentConfig.java    (简化配置)
├── engine/                           │   ├── engine/
│   ├── Engine.java                   │   │   ├── Engine.java         (回调版)
│   ├── JimiEngine.java               │   │   ├── JimiEngine.java     (移除Spring)
│   ├── AgentExecutor.java            │   │   ├── AgentExecutor.java  (回调版)
│   └── context/Context.java          │   │   └── context/
├── llm/                              │   ├── llm/
│   ├── ChatProvider.java             │   │   ├── ChatProvider.java   (回调版)
│   ├── LLM.java                      │   │   ├── HttpChatProvider.java (新增)
│   └── message/                      │   │   ├── LLM.java
├── wire/                             │   │   └── message/            (移除Lombok)
│   ├── Wire.java                     │   ├── wire/
│   └── message/                      │   │   ├── Wire.java           (监听器版)
├── tool/                             │   │   ├── WireImpl.java
│   ├── Tool.java                     │   │   └── message/
│   ├── ToolRegistry.java             │   └── tool/
│   └── [bash/file等工具]             │       ├── Tool.java           (回调版)
└── ui/shell/ (不移植)                 │       └── ToolRegistry.java
                                      ├── tools/                      (电商工具)
                                      │   ├── ProductSearchTool.java
                                      │   ├── OrderQueryTool.java
                                      │   └── CartManagerTool.java
                                      └── sdk/                        (集成入口)
                                          ├── JimiSDK.java
                                          └── JimiConfig.java
```

### 3.2 核心接口改造 - Engine

**原始接口 (Reactor版)**:
```java
public interface Engine {
    String getName();
    String getModel();
    Map<String, Object> getStatus();
    Mono<Void> run(String userInput);
    Mono<Void> run(List<ContentPart> userInput);
}
```

**改造后 (Java回调版)**:
```java
package io.leavesfly.jimi.android.core.engine;

import java.util.List;
import java.util.Map;

/**
 * Engine 接口 (Android版)
 * 定义 Agent 的核心行为，使用回调模式替代Reactor
 */
public interface Engine {
    
    /** 获取 Agent 名称 */
    String getName();
    
    /** 获取使用的模型名称 */
    String getModel();
    
    /** 获取当前状态快照 */
    Map<String, Object> getStatus();
    
    /**
     * 运行 Agent（文本输入）
     * @param userInput 用户输入文本
     * @param callback 执行回调
     */
    void run(String userInput, EngineCallback callback);
    
    /**
     * 运行 Agent（多部分内容输入）
     * @param userInput 用户输入内容部分列表
     * @param callback 执行回调
     */
    void run(List<ContentPart> userInput, EngineCallback callback);
    
    /**
     * 添加 Wire 消息监听器
     * @param listener 监听器
     */
    void addWireListener(WireListener listener);
    
    /**
     * 移除 Wire 消息监听器
     * @param listener 监听器
     */
    void removeWireListener(WireListener listener);
    
    /** 清除对话历史 */
    void clearHistory();
    
    /** 释放资源 */
    void shutdown();
}
```

### 3.3 JimiEngine 实现

```java
package io.leavesfly.jimi.android.core.engine;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * JimiEngine Android实现
 * - 移除 Spring 依赖
 * - 使用回调替代 Reactor
 * - 支持主线程回调
 */
public class JimiEngine implements Engine {
    
    private final Agent agent;
    private final Runtime runtime;
    private final Context context;
    private final ToolRegistry toolRegistry;
    private final Wire wire;
    private final Compaction compaction;
    private final AgentExecutor executor;
    
    // 后台线程池
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    // 主线程Handler
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public JimiEngine(
            Agent agent,
            Runtime runtime,
            Context context,
            ToolRegistry toolRegistry,
            Wire wire,
            Compaction compaction) {
        this.agent = agent;
        this.runtime = runtime;
        this.context = context;
        this.toolRegistry = toolRegistry;
        this.wire = wire;
        this.compaction = compaction;
        this.executor = new AgentExecutor(
            agent, runtime, context, wire, toolRegistry, compaction
        );
    }
    
    @Override
    public String getName() {
        return agent.getName();
    }
    
    @Override
    public String getModel() {
        return runtime.getLLM().getModelName();
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
                notifyOnMainThread(() -> callback.onComplete());
            } catch (Exception e) {
                notifyOnMainThread(() -> callback.onError(e));
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
    }
    
    private void notifyOnMainThread(Runnable action) {
        mainHandler.post(action);
    }
}
```

### 3.4 AgentExecutor 核心逻辑

```java
package io.leavesfly.jimi.android.core.engine;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Agent 执行器 (Android版)
 * - 保持原有主循环逻辑
 * - 使用同步回调替代 Reactor
 */
public class AgentExecutor {
    
    private static final int MAX_THINKING_STEPS = 5;
    private static final String TAG = "AgentExecutor";
    
    private final Agent agent;
    private final Runtime runtime;
    private final Context context;
    private final Wire wire;
    private final ToolRegistry toolRegistry;
    private final Compaction compaction;
    
    private int consecutiveNoToolCallSteps = 0;
    
    public AgentExecutor(
            Agent agent,
            Runtime runtime,
            Context context,
            Wire wire,
            ToolRegistry toolRegistry,
            Compaction compaction) {
        this.agent = agent;
        this.runtime = runtime;
        this.context = context;
        this.wire = wire;
        this.toolRegistry = toolRegistry;
        this.compaction = compaction;
    }
    
    /**
     * 执行入口 (同步执行，需在后台线程调用)
     */
    public void execute(List<ContentPart> userInput) throws Exception {
        // 1. 创建检查点
        context.checkpoint(false);
        
        // 2. 添加用户消息
        context.appendMessage(Message.user(userInput));
        
        // 3. 启动主循环
        agentLoop();
    }
    
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
    
    private boolean agentLoopStep(int stepNo) throws Exception {
        int maxSteps = runtime.getConfig().getMaxStepsPerRun();
        if (stepNo > maxSteps) {
            throw new MaxStepsReachedException(maxSteps);
        }
        
        // 发送步骤开始消息
        wire.send(new StepBegin(stepNo));
        
        // 检查上下文压缩
        checkAndCompactContext();
        
        // 创建检查点
        context.checkpoint(true);
        
        // 执行单步
        return step();
    }
    
    private boolean step() throws Exception {
        LLM llm = runtime.getLLM();
        String systemPrompt = agent.getSystemPrompt();
        List<Message> history = context.getMessages();
        List<ToolSchema> tools = toolRegistry.getToolSchemas(agent.getTools());
        
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
                    if (chunk.getContent() != null) {
                        contentBuilder.append(chunk.getContent());
                        wire.send(new ContentPart(chunk.getContent()));
                    }
                    // 累积工具调用
                    if (chunk.getToolCalls() != null) {
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
        
        if (errorRef.get() != null) {
            throw new RuntimeException(errorRef.get());
        }
        
        // 构建助手消息
        Message assistantMessage = Message.assistant(
            contentBuilder.toString(),
            toolCallAccumulator.build()
        );
        context.appendMessage(assistantMessage);
        
        // 处理工具调用
        return handleToolCalls(assistantMessage);
    }
    
    private boolean handleToolCalls(Message message) throws Exception {
        List<ToolCall> toolCalls = message.getToolCalls();
        
        if (toolCalls == null || toolCalls.isEmpty()) {
            consecutiveNoToolCallSteps++;
            return consecutiveNoToolCallSteps >= MAX_THINKING_STEPS;
        }
        
        consecutiveNoToolCallSteps = 0;
        
        // 串行执行工具调用
        for (ToolCall toolCall : toolCalls) {
            wire.send(new ToolCallMessage(toolCall));
            
            ToolResult result = toolRegistry.execute(
                toolCall.getFunction().getName(),
                toolCall.getFunction().getArguments()
            );
            
            context.appendMessage(Message.toolResult(toolCall.getId(), result));
            wire.send(new ToolResultMessage(toolCall.getId(), result));
        }
        
        return false; // 继续循环
    }
    
    private void checkAndCompactContext() {
        // 上下文压缩逻辑
        int tokenCount = context.estimateTokenCount();
        int maxContextSize = runtime.getLLM().getMaxContextSize();
        if (tokenCount > maxContextSize * 0.8) {
            compaction.compact(context);
        }
    }
}
```

### 3.5 Wire 消息总线 (Java监听器版)

```java
package io.leavesfly.jimi.android.core.wire;

import android.os.Handler;
import android.os.Looper;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Wire 接口 (Android版)
 * 使用监听器模式替代 Reactor Flux
 */
public interface Wire {
    void send(WireMessage message);
    void addListener(WireListener listener);
    void removeListener(WireListener listener);
}

/**
 * Wire 实现
 */
public class WireImpl implements Wire {
    
    private final List<WireListener> listeners = new CopyOnWriteArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final boolean notifyOnMainThread;
    
    public WireImpl() {
        this(true);
    }
    
    public WireImpl(boolean notifyOnMainThread) {
        this.notifyOnMainThread = notifyOnMainThread;
    }
    
    @Override
    public void send(WireMessage message) {
        for (WireListener listener : listeners) {
            if (notifyOnMainThread) {
                mainHandler.post(() -> listener.onMessage(message));
            } else {
                listener.onMessage(message);
            }
        }
    }
    
    @Override
    public void addListener(WireListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    @Override
    public void removeListener(WireListener listener) {
        listeners.remove(listener);
    }
}
```

---

## 四、LLM 网络层实现 (Java版)

### 4.1 ChatProvider 接口

```java
package io.leavesfly.jimi.android.core.llm;

import java.util.List;

/**
 * ChatProvider 接口 (Android版)
 * 使用回调模式替代 Reactor
 */
public interface ChatProvider {
    
    /** 获取模型名称 */
    String getModelName();
    
    /**
     * 生成聊天完成（流式）
     * @param systemPrompt 系统提示词
     * @param history 历史消息列表
     * @param tools 可用工具列表
     * @param callback 流式响应回调
     */
    void generateStream(
        String systemPrompt,
        List<Message> history,
        List<ToolSchema> tools,
        StreamCallback<ChatCompletionChunk> callback
    );
    
    /**
     * 生成聊天完成（非流式，可选实现）
     */
    default void generate(
        String systemPrompt,
        List<Message> history,
        List<ToolSchema> tools,
        ResultCallback<ChatCompletionResult> callback
    ) {
        throw new UnsupportedOperationException("Non-streaming not implemented");
    }
}
```

### 4.2 零依赖 HTTP 客户端 (HttpURLConnection)

```java
package io.leavesfly.jimi.android.core.llm;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 基于 HttpURLConnection 的 ChatProvider 实现
 * - 零第三方依赖
 * - 支持 SSE 流式响应
 */
public class HttpChatProvider implements ChatProvider {
    
    private static final String TAG = "HttpChatProvider";
    private static final int CONNECT_TIMEOUT = 30_000;
    private static final int READ_TIMEOUT = 120_000;
    
    private final String modelName;
    private final String baseUrl;
    private final String apiKey;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    public HttpChatProvider(String modelName, String baseUrl, String apiKey) {
        this.modelName = modelName;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }
    
    @Override
    public String getModelName() {
        return modelName;
    }
    
    @Override
    public void generateStream(
            String systemPrompt,
            List<Message> history,
            List<ToolSchema> tools,
            StreamCallback<ChatCompletionChunk> callback) {
        
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                // 创建连接
                URL url = new URL(baseUrl + "/chat/completions");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                connection.setRequestProperty("Accept", "text/event-stream");
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);
                
                // 构建请求体
                String requestBody = buildRequestBody(systemPrompt, history, tools);
                
                // 发送请求
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(requestBody.getBytes("UTF-8"));
                }
                
                // 检查响应码
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    String error = readErrorResponse(connection);
                    callback.onError(new IOException("HTTP " + responseCode + ": " + error));
                    return;
                }
                
                // 解析 SSE 流
                parseSSEStream(connection.getInputStream(), callback);
                
            } catch (Exception e) {
                callback.onError(e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }
    
    private String buildRequestBody(
            String systemPrompt,
            List<Message> history,
            List<ToolSchema> tools) throws Exception {
        
        JSONObject json = new JSONObject();
        json.put("model", modelName);
        json.put("stream", true);
        
        // 构建 messages 数组
        JSONArray messages = new JSONArray();
        
        // System message
        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.put(systemMsg);
        
        // History messages
        for (Message msg : history) {
            messages.put(msg.toJson());
        }
        
        json.put("messages", messages);
        
        // Tools
        if (tools != null && !tools.isEmpty()) {
            JSONArray toolsArray = new JSONArray();
            for (ToolSchema tool : tools) {
                toolsArray.put(tool.toJson());
            }
            json.put("tools", toolsArray);
        }
        
        return json.toString();
    }
    
    private void parseSSEStream(
            InputStream inputStream,
            StreamCallback<ChatCompletionChunk> callback) throws IOException {
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, "UTF-8"))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                // SSE 格式: "data: {...}"
                if (line.startsWith("data: ")) {
                    String json = line.substring(6).trim();
                    
                    // 结束标记
                    if ("[DONE]".equals(json)) {
                        callback.onComplete();
                        return;
                    }
                    
                    try {
                        ChatCompletionChunk chunk = parseChunk(json);
                        callback.onNext(chunk);
                    } catch (Exception e) {
                        // 解析单个chunk失败，继续处理
                        android.util.Log.w(TAG, "Parse chunk failed: " + e.getMessage());
                    }
                }
            }
            callback.onComplete();
        }
    }
    
    private ChatCompletionChunk parseChunk(String json) throws Exception {
        JSONObject obj = new JSONObject(json);
        
        ChatCompletionChunk chunk = new ChatCompletionChunk();
        chunk.setId(obj.optString("id", null));
        
        JSONArray choices = obj.optJSONArray("choices");
        if (choices != null && choices.length() > 0) {
            JSONObject choice = choices.getJSONObject(0);
            JSONObject delta = choice.optJSONObject("delta");
            
            if (delta != null) {
                // 解析内容
                String content = delta.optString("content", null);
                if (content != null && !content.isEmpty()) {
                    chunk.setContent(content);
                }
                
                // 解析工具调用
                JSONArray toolCalls = delta.optJSONArray("tool_calls");
                if (toolCalls != null) {
                    chunk.setToolCalls(parseToolCallDeltas(toolCalls));
                }
            }
            
            // 检查结束原因
            String finishReason = choice.optString("finish_reason", null);
            chunk.setFinishReason(finishReason);
        }
        
        // 解析 usage
        JSONObject usage = obj.optJSONObject("usage");
        if (usage != null) {
            chunk.setPromptTokens(usage.optInt("prompt_tokens", 0));
            chunk.setCompletionTokens(usage.optInt("completion_tokens", 0));
        }
        
        return chunk;
    }
    
    private List<ToolCallDelta> parseToolCallDeltas(JSONArray array) throws Exception {
        List<ToolCallDelta> result = new java.util.ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            ToolCallDelta delta = new ToolCallDelta();
            delta.setIndex(obj.optInt("index", 0));
            delta.setId(obj.optString("id", null));
            delta.setType(obj.optString("type", null));
            
            JSONObject function = obj.optJSONObject("function");
            if (function != null) {
                delta.setFunctionName(function.optString("name", null));
                delta.setFunctionArguments(function.optString("arguments", null));
            }
            result.add(delta);
        }
        return result;
    }
    
    private String readErrorResponse(HttpURLConnection connection) {
        try {
            InputStream es = connection.getErrorStream();
            if (es != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(es));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            }
        } catch (Exception e) {
            // ignore
        }
        return "Unknown error";
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}
```

### 4.3 消息模型 (Java POJO)

```java
package io.leavesfly.jimi.android.core.llm.message;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息实体 (Java版，手写getter/setter)
 */
public class Message {
    
    public enum Role {
        SYSTEM("system"),
        USER("user"),
        ASSISTANT("assistant"),
        TOOL("tool");
        
        private final String value;
        
        Role(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    private Role role;
    private String content;
    private List<ToolCall> toolCalls;
    private String toolCallId;
    
    // Getters and Setters
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<ToolCall> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<ToolCall> toolCalls) { this.toolCalls = toolCalls; }
    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }
    
    // 转换为 JSON
    public JSONObject toJson() throws Exception {
        JSONObject json = new JSONObject();
        json.put("role", role.getValue());
        
        if (content != null) {
            json.put("content", content);
        }
        
        if (toolCalls != null && !toolCalls.isEmpty()) {
            JSONArray array = new JSONArray();
            for (ToolCall tc : toolCalls) {
                array.put(tc.toJson());
            }
            json.put("tool_calls", array);
        }
        
        if (toolCallId != null) {
            json.put("tool_call_id", toolCallId);
        }
        
        return json;
    }
    
    // 静态工厂方法
    public static Message user(String content) {
        Message msg = new Message();
        msg.setRole(Role.USER);
        msg.setContent(content);
        return msg;
    }
    
    public static Message user(List<ContentPart> parts) {
        StringBuilder sb = new StringBuilder();
        for (ContentPart part : parts) {
            if (part.getText() != null) {
                sb.append(part.getText());
            }
        }
        return user(sb.toString());
    }
    
    public static Message assistant(String content, List<ToolCall> toolCalls) {
        Message msg = new Message();
        msg.setRole(Role.ASSISTANT);
        msg.setContent(content);
        msg.setToolCalls(toolCalls);
        return msg;
    }
    
    public static Message toolResult(String toolCallId, ToolResult result) {
        Message msg = new Message();
        msg.setRole(Role.TOOL);
        msg.setContent(result.getContent());
        msg.setToolCallId(toolCallId);
        return msg;
    }
}

/**
 * 工具调用
 */
public class ToolCall {
    private String id;
    private String type = "function";
    private FunctionCall function;
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public FunctionCall getFunction() { return function; }
    public void setFunction(FunctionCall function) { this.function = function; }
    
    public JSONObject toJson() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("type", type);
        json.put("function", function.toJson());
        return json;
    }
}

/**
 * 函数调用
 */
public class FunctionCall {
    private String name;
    private String arguments;
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getArguments() { return arguments; }
    public void setArguments(String arguments) { this.arguments = arguments; }
    
    public JSONObject toJson() throws Exception {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("arguments", arguments);
        return json;
    }
}

/**
 * 工具调用累加器 (处理流式工具调用拆分)
 */
public class ToolCallAccumulator {
    
    private final java.util.Map<Integer, ToolCallBuilder> builders = new java.util.HashMap<>();
    
    public void accumulate(ToolCallDelta delta) {
        int index = delta.getIndex();
        ToolCallBuilder builder = builders.get(index);
        if (builder == null) {
            builder = new ToolCallBuilder();
            builders.put(index, builder);
        }
        
        if (delta.getId() != null) {
            builder.setId(delta.getId());
        }
        if (delta.getFunctionName() != null) {
            builder.appendFunctionName(delta.getFunctionName());
        }
        if (delta.getFunctionArguments() != null) {
            builder.appendArguments(delta.getFunctionArguments());
        }
    }
    
    public List<ToolCall> build() {
        List<ToolCall> result = new ArrayList<>();
        for (int i = 0; i < builders.size(); i++) {
            ToolCallBuilder builder = builders.get(i);
            if (builder != null) {
                result.add(builder.build());
            }
        }
        return result.isEmpty() ? null : result;
    }
    
    private static class ToolCallBuilder {
        private String id;
        private StringBuilder functionName = new StringBuilder();
        private StringBuilder arguments = new StringBuilder();
        
        void setId(String id) { this.id = id; }
        void appendFunctionName(String name) { functionName.append(name); }
        void appendArguments(String args) { arguments.append(args); }
        
        ToolCall build() {
            ToolCall tc = new ToolCall();
            tc.setId(id);
            FunctionCall fc = new FunctionCall();
            fc.setName(functionName.toString());
            fc.setArguments(arguments.toString());
            tc.setFunction(fc);
            return tc;
        }
    }
}
```

---

## 五、电商工具层实现 (Java版)

### 5.1 工具接口 (Java回调版)

```java
package io.leavesfly.jimi.android.core.tool;

import org.json.JSONObject;

/**
 * 工具接口 (Android版)
 * 使用回调替代 Reactor Mono
 */
public interface Tool {
    
    /** 获取工具名称 */
    String getName();
    
    /** 获取工具描述 */
    String getDescription();
    
    /** 获取参数 Schema */
    ToolSchema getSchema();
    
    /**
     * 执行工具调用 (同步版本，需在后台线程调用)
     * @param arguments JSON格式的参数
     * @return 工具执行结果
     */
    ToolResult execute(String arguments) throws Exception;
}

/**
 * 工具执行结果
 */
public class ToolResult {
    private final boolean success;
    private final String content;
    private final String error;
    
    private ToolResult(boolean success, String content, String error) {
        this.success = success;
        this.content = content;
        this.error = error;
    }
    
    public boolean isSuccess() { return success; }
    public String getContent() { return content; }
    public String getError() { return error; }
    
    public static ToolResult success(String content) {
        return new ToolResult(true, content, null);
    }
    
    public static ToolResult error(String message) {
        return new ToolResult(false, "", message);
    }
}

/**
 * 工具 Schema (用于 LLM 工具描述)
 */
public class ToolSchema {
    private final String name;
    private final String description;
    private final JSONObject parameters;
    
    public ToolSchema(String name, String description, JSONObject parameters) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
    }
    
    public JSONObject toJson() throws Exception {
        JSONObject json = new JSONObject();
        json.put("type", "function");
        
        JSONObject function = new JSONObject();
        function.put("name", name);
        function.put("description", description);
        function.put("parameters", parameters);
        
        json.put("function", function);
        return json;
    }
}
```

### 5.2 工具注册表

```java
package io.leavesfly.jimi.android.core.tool;

import org.json.JSONObject;
import java.util.*;

/**
 * 工具注册表
 */
public class ToolRegistry {
    
    private final Map<String, Tool> tools = new HashMap<>();
    
    public void register(Tool tool) {
        tools.put(tool.getName(), tool);
    }
    
    public void unregister(String toolName) {
        tools.remove(toolName);
    }
    
    public Tool getTool(String name) {
        return tools.get(name);
    }
    
    public List<ToolSchema> getToolSchemas(List<String> toolNames) {
        List<ToolSchema> schemas = new ArrayList<>();
        for (String name : toolNames) {
            Tool tool = tools.get(name);
            if (tool != null) {
                schemas.add(tool.getSchema());
            }
        }
        return schemas;
    }
    
    public List<ToolSchema> getAllToolSchemas() {
        List<ToolSchema> schemas = new ArrayList<>();
        for (Tool tool : tools.values()) {
            schemas.add(tool.getSchema());
        }
        return schemas;
    }
    
    /**
     * 执行工具调用
     */
    public ToolResult execute(String toolName, String arguments) {
        Tool tool = tools.get(toolName);
        if (tool == null) {
            return ToolResult.error("工具不存在: " + toolName);
        }
        
        try {
            return tool.execute(arguments);
        } catch (Exception e) {
            return ToolResult.error("工具执行失败: " + e.getMessage());
        }
    }
}
```

### 5.3 电商工具实现

```java
package io.leavesfly.jimi.android.tools;

import org.json.JSONObject;
import org.json.JSONArray;
import java.util.List;

/**
 * 商品搜索工具
 */
public class ProductSearchTool implements Tool {
    
    private final EcommerceApi api;
    
    public ProductSearchTool(EcommerceApi api) {
        this.api = api;
    }
    
    @Override
    public String getName() {
        return "product_search";
    }
    
    @Override
    public String getDescription() {
        return "搜索商品，支持关键词、品类、价格区间筛选，并可指定排序方式";
    }
    
    @Override
    public ToolSchema getSchema() {
        try {
            JSONObject params = new JSONObject();
            params.put("type", "object");
            
            JSONObject properties = new JSONObject();
            properties.put("keyword", createStringProp("搜索关键词"));
            properties.put("category", createStringProp("商品分类"));
            properties.put("min_price", createNumberProp("最低价格"));
            properties.put("max_price", createNumberProp("最高价格"));
            properties.put("sort_by", createEnumProp("排序方式", 
                new String[]{"price_asc", "price_desc", "sales", "rating"}));
            
            params.put("properties", properties);
            params.put("required", new JSONArray());
            
            return new ToolSchema(getName(), getDescription(), params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public ToolResult execute(String arguments) throws Exception {
        JSONObject args = new JSONObject(arguments);
        
        String keyword = args.optString("keyword", null);
        String category = args.optString("category", null);
        Double minPrice = args.has("min_price") ? args.getDouble("min_price") : null;
        Double maxPrice = args.has("max_price") ? args.getDouble("max_price") : null;
        String sortBy = args.optString("sort_by", null);
        
        List<Product> products = api.searchProducts(
            keyword, category, minPrice, maxPrice, sortBy
        );
        
        return ToolResult.success(formatProducts(products));
    }
    
    private String formatProducts(List<Product> products) {
        if (products.isEmpty()) {
            return "未找到相关商品";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("找到 ").append(products.size()).append(" 件商品：\n\n");
        
        int count = Math.min(products.size(), 5);
        for (int i = 0; i < count; i++) {
            Product p = products.get(i);
            sb.append(i + 1).append(". **").append(p.getName()).append("**\n");
            sb.append("   价格: ¥").append(p.getPrice()).append("\n");
            sb.append("   评分: ").append(p.getRating()).append("分 | ");
            sb.append("销量: ").append(p.getSales()).append("\n\n");
        }
        
        if (products.size() > 5) {
            sb.append("... 还有 ").append(products.size() - 5).append(" 件商品");
        }
        
        return sb.toString();
    }
    
    // 辅助方法
    private JSONObject createStringProp(String desc) throws Exception {
        JSONObject prop = new JSONObject();
        prop.put("type", "string");
        prop.put("description", desc);
        return prop;
    }
    
    private JSONObject createNumberProp(String desc) throws Exception {
        JSONObject prop = new JSONObject();
        prop.put("type", "number");
        prop.put("description", desc);
        return prop;
    }
    
    private JSONObject createEnumProp(String desc, String[] values) throws Exception {
        JSONObject prop = new JSONObject();
        prop.put("type", "string");
        prop.put("description", desc);
        prop.put("enum", new JSONArray(values));
        return prop;
    }
}

/**
 * 订单查询工具
 */
public class OrderQueryTool implements Tool {
    
    private final EcommerceApi api;
    
    public OrderQueryTool(EcommerceApi api) {
        this.api = api;
    }
    
    @Override
    public String getName() {
        return "order_query";
    }
    
    @Override
    public String getDescription() {
        return "查询订单信息，可按订单号、状态或时间范围查询";
    }
    
    @Override
    public ToolSchema getSchema() {
        try {
            JSONObject params = new JSONObject();
            params.put("type", "object");
            
            JSONObject properties = new JSONObject();
            
            JSONObject orderIdProp = new JSONObject();
            orderIdProp.put("type", "string");
            orderIdProp.put("description", "订单号");
            properties.put("order_id", orderIdProp);
            
            JSONObject statusProp = new JSONObject();
            statusProp.put("type", "string");
            statusProp.put("description", "订单状态");
            statusProp.put("enum", new JSONArray(
                new String[]{"pending", "paid", "shipped", "delivered", "cancelled"}));
            properties.put("status", statusProp);
            
            JSONObject daysProp = new JSONObject();
            daysProp.put("type", "integer");
            daysProp.put("description", "最近N天的订单");
            properties.put("recent_days", daysProp);
            
            params.put("properties", properties);
            params.put("required", new JSONArray());
            
            return new ToolSchema(getName(), getDescription(), params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public ToolResult execute(String arguments) throws Exception {
        JSONObject args = new JSONObject(arguments);
        
        String orderId = args.optString("order_id", null);
        String status = args.optString("status", null);
        int recentDays = args.optInt("recent_days", 30);
        
        List<Order> orders = api.queryOrders(orderId, status, recentDays);
        
        return ToolResult.success(formatOrders(orders));
    }
    
    private String formatOrders(List<Order> orders) {
        if (orders.isEmpty()) {
            return "未找到相关订单";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("找到 ").append(orders.size()).append(" 个订单：\n\n");
        
        for (Order order : orders) {
            sb.append("订单号: ").append(order.getId()).append("\n");
            sb.append("状态: ").append(getStatusText(order.getStatus())).append("\n");
            sb.append("金额: ¥").append(order.getTotalAmount()).append("\n");
            sb.append("下单时间: ").append(order.getCreateTime()).append("\n\n");
        }
        
        return sb.toString();
    }
    
    private String getStatusText(String status) {
        switch (status) {
            case "pending": return "待付款";
            case "paid": return "已付款";
            case "shipped": return "已发货";
            case "delivered": return "已签收";
            case "cancelled": return "已取消";
            default: return status;
        }
    }
}

/**
 * 购物车管理工具
 */
public class CartManagerTool implements Tool {
    
    private final EcommerceApi api;
    
    public CartManagerTool(EcommerceApi api) {
        this.api = api;
    }
    
    @Override
    public String getName() {
        return "cart_manager";
    }
    
    @Override
    public String getDescription() {
        return "管理购物车：查看(view)、添加(add)、删除(remove)、修改数量(update)";
    }
    
    @Override
    public ToolSchema getSchema() {
        try {
            JSONObject params = new JSONObject();
            params.put("type", "object");
            
            JSONObject properties = new JSONObject();
            
            JSONObject actionProp = new JSONObject();
            actionProp.put("type", "string");
            actionProp.put("description", "操作类型");
            actionProp.put("enum", new JSONArray(
                new String[]{"view", "add", "remove", "update"}));
            properties.put("action", actionProp);
            
            JSONObject productIdProp = new JSONObject();
            productIdProp.put("type", "string");
            productIdProp.put("description", "商品ID");
            properties.put("product_id", productIdProp);
            
            JSONObject quantityProp = new JSONObject();
            quantityProp.put("type", "integer");
            quantityProp.put("description", "数量");
            properties.put("quantity", quantityProp);
            
            params.put("properties", properties);
            params.put("required", new JSONArray(new String[]{"action"}));
            
            return new ToolSchema(getName(), getDescription(), params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public ToolResult execute(String arguments) throws Exception {
        JSONObject args = new JSONObject(arguments);
        
        String action = args.getString("action");
        String productId = args.optString("product_id", null);
        int quantity = args.optInt("quantity", 1);
        
        switch (action) {
            case "view":
                Cart cart = api.getCart();
                return ToolResult.success(formatCart(cart));
                
            case "add":
                if (productId == null) {
                    return ToolResult.error("缺少商品ID");
                }
                api.addToCart(productId, quantity);
                return ToolResult.success("已添加到购物车");
                
            case "remove":
                if (productId == null) {
                    return ToolResult.error("缺少商品ID");
                }
                api.removeFromCart(productId);
                return ToolResult.success("已从购物车移除");
                
            case "update":
                if (productId == null) {
                    return ToolResult.error("缺少商品ID");
                }
                api.updateCartQuantity(productId, quantity);
                return ToolResult.success("已更新数量");
                
            default:
                return ToolResult.error("未知操作: " + action);
        }
    }
    
    private String formatCart(Cart cart) {
        if (cart.getItems().isEmpty()) {
            return "购物车为空";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("购物车内容：\n\n");
        
        for (CartItem item : cart.getItems()) {
            sb.append("• ").append(item.getProductName()).append("\n");
            sb.append("  单价: ¥").append(item.getPrice());
            sb.append(" x ").append(item.getQuantity());
            sb.append(" = ¥").append(item.getPrice() * item.getQuantity()).append("\n");
        }
        
        sb.append("\n总计: ¥").append(cart.getTotalAmount());
        
        return sb.toString();
    }
}
```

### 5.4 电商 API 接口定义

```java
package io.leavesfly.jimi.android.tools;

import java.util.List;

/**
 * 电商 API 接口
 * 由宿主App实现，提供刡电商后端的能力
 */
public interface EcommerceApi {
    
    /**
     * 搜索商品
     */
    List<Product> searchProducts(
        String keyword,
        String category,
        Double minPrice,
        Double maxPrice,
        String sortBy
    ) throws Exception;
    
    /**
     * 获取商品详情
     */
    Product getProductDetail(String productId) throws Exception;
    
    /**
     * 查询订单
     */
    List<Order> queryOrders(
        String orderId,
        String status,
        int recentDays
    ) throws Exception;
    
    /**
     * 获取购物车
     */
    Cart getCart() throws Exception;
    
    /**
     * 添加到购物车
     */
    void addToCart(String productId, int quantity) throws Exception;
    
    /**
     * 从购物车移除
     */
    void removeFromCart(String productId) throws Exception;
    
    /**
     * 更新购物车数量
     */
    void updateCartQuantity(String productId, int quantity) throws Exception;
}

// 数据模型
public class Product {
    private String id;
    private String name;
    private double price;
    private double rating;
    private long sales;
    private String imageUrl;
    private String description;
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public long getSales() { return sales; }
    public void setSales(long sales) { this.sales = sales; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

public class Order {
    private String id;
    private String status;
    private double totalAmount;
    private String createTime;
    private List<OrderItem> items;
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}

public class Cart {
    private List<CartItem> items;
    private double totalAmount;
    
    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
}

public class CartItem {
    private String productId;
    private String productName;
    private double price;
    private int quantity;
    
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
```

---

## 六、SDK 集成接口 (Java版)

### 6.1 统一入口类

```java
package io.leavesfly.jimi.android.sdk;

import android.content.Context;
import java.io.*;
import java.util.Arrays;

/**
 * Jimi SDK 统一入口
 * 提供简单的初始化和调用接口
 */
public class JimiSDK {
    
    private static JimiSDK instance;
    private JimiEngine engine;
    private JimiConfig config;
    private boolean initialized = false;
    
    private JimiSDK() {}
    
    public static synchronized JimiSDK getInstance() {
        if (instance == null) {
            instance = new JimiSDK();
        }
        return instance;
    }
    
    /**
     * 初始化 SDK
     * @param context Application Context
     * @param config SDK 配置
     * @param ecommerceApi 电商 API 实现
     */
    public void init(
            Context context,
            JimiConfig config,
            EcommerceApi ecommerceApi) {
        
        if (initialized) {
            return;
        }
        
        this.config = config;
        
        // 创建工具注册表
        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.register(new ProductSearchTool(ecommerceApi));
        toolRegistry.register(new OrderQueryTool(ecommerceApi));
        toolRegistry.register(new CartManagerTool(ecommerceApi));
        
        // 创建 LLM
        ChatProvider chatProvider = new HttpChatProvider(
            config.getModel(),
            config.getLlmBaseUrl(),
            config.getLlmApiKey()
        );
        LLM llm = new LLM(chatProvider, config.getMaxContextSize());
        
        // 创建 Runtime
        Runtime runtime = new Runtime(llm, config);
        
        // 创建 Agent
        String systemPrompt = loadPrompt(context, config.getPromptAsset());
        Agent agent = new Agent(
            "shopping_assistant",
            systemPrompt,
            Arrays.asList("product_search", "order_query", "cart_manager")
        );
        
        // 创建 Wire
        Wire wire = new WireImpl(true); // 主线程通知
        
        // 创建 Context
        File contextFile = new File(context.getFilesDir(), "jimi_context.json");
        io.leavesfly.jimi.android.core.engine.context.Context jimiContext = 
            new io.leavesfly.jimi.android.core.engine.context.Context(contextFile);
        
        // 创建 Engine
        engine = new JimiEngine(
            agent,
            runtime,
            jimiContext,
            toolRegistry,
            wire,
            new SimpleCompaction()
        );
        
        initialized = true;
    }
    
    /**
     * 发送用户消息
     * @param input 用户输入
     * @param callback 执行回调
     */
    public void sendMessage(String input, EngineCallback callback) {
        checkInitialized();
        engine.run(input, callback);
    }
    
    /**
     * 添加 Wire 消息监听器
     * @param listener 监听器
     */
    public void addWireListener(WireListener listener) {
        checkInitialized();
        engine.addWireListener(listener);
    }
    
    /**
     * 移除 Wire 消息监听器
     * @param listener 监听器
     */
    public void removeWireListener(WireListener listener) {
        checkInitialized();
        engine.removeWireListener(listener);
    }
    
    /**
     * 清除对话历史
     */
    public void clearHistory() {
        checkInitialized();
        engine.clearHistory();
    }
    
    /**
     * 释放资源
     */
    public void shutdown() {
        if (engine != null) {
            engine.shutdown();
        }
        initialized = false;
    }
    
    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("JimiSDK not initialized. Call init() first.");
        }
    }
    
    private String loadPrompt(Context context, String assetPath) {
        try {
            InputStream is = context.getAssets().open(assetPath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            return "你是一个电商购物助手，可以帮助用户搜索商品、查询订单和管理购物车。";
        }
    }
}
```

### 6.2 配置类 (Builder模式)

```java
package io.leavesfly.jimi.android.sdk;

/**
 * Jimi SDK 配置
 */
public class JimiConfig {
    
    private String llmBaseUrl;
    private String llmApiKey;
    private String model = "gpt-4-turbo";
    private int maxContextSize = 128000;
    private int maxStepsPerRun = 50;
    private String promptAsset = "prompts/shopping_assistant.md";
    
    private JimiConfig() {}
    
    // Getters
    public String getLlmBaseUrl() { return llmBaseUrl; }
    public String getLlmApiKey() { return llmApiKey; }
    public String getModel() { return model; }
    public int getMaxContextSize() { return maxContextSize; }
    public int getMaxStepsPerRun() { return maxStepsPerRun; }
    public String getPromptAsset() { return promptAsset; }
    
    /**
     * Builder 模式
     */
    public static class Builder {
        private final JimiConfig config = new JimiConfig();
        
        public Builder llmBaseUrl(String url) {
            config.llmBaseUrl = url;
            return this;
        }
        
        public Builder llmApiKey(String apiKey) {
            config.llmApiKey = apiKey;
            return this;
        }
        
        public Builder model(String model) {
            config.model = model;
            return this;
        }
        
        public Builder maxContextSize(int size) {
            config.maxContextSize = size;
            return this;
        }
        
        public Builder maxStepsPerRun(int steps) {
            config.maxStepsPerRun = steps;
            return this;
        }
        
        public Builder promptAsset(String assetPath) {
            config.promptAsset = assetPath;
            return this;
        }
        
        public JimiConfig build() {
            if (config.llmBaseUrl == null || config.llmApiKey == null) {
                throw new IllegalStateException("llmBaseUrl and llmApiKey are required");
            }
            return config;
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
```

### 6.3 电商 App 集成示例 (Java)

```java
// 在 Application 中初始化
public class MyApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 初始化 Jimi SDK
        JimiConfig config = JimiConfig.builder()
            .llmBaseUrl(BuildConfig.LLM_BASE_URL)
            .llmApiKey(BuildConfig.LLM_API_KEY)
            .model("qwen-turbo")
            .maxContextSize(32000)
            .build();
        
        JimiSDK.getInstance().init(
            this,
            config,
            new MyEcommerceApiImpl() // 电商 API 实现
        );
    }
}

// 在 Activity 中使用
public class ShoppingAssistantActivity extends AppCompatActivity {
    
    private MessageAdapter messageAdapter;
    private RecyclerView recyclerView;
    private EditText editInput;
    private WireListener wireListener;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_assistant);
        
        recyclerView = findViewById(R.id.recycler_messages);
        editInput = findViewById(R.id.edit_input);
        Button btnSend = findViewById(R.id.btn_send);
        
        messageAdapter = new MessageAdapter();
        recyclerView.setAdapter(messageAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // 添加 Wire 监听器
        wireListener = new WireListener() {
            @Override
            public void onMessage(WireMessage message) {
                // 已在主线程回调
                handleWireMessage(message);
            }
        };
        JimiSDK.getInstance().addWireListener(wireListener);
        
        // 发送按钮
        btnSend.setOnClickListener(v -> {
            String input = editInput.getText().toString().trim();
            if (!input.isEmpty()) {
                sendMessage(input);
                editInput.setText("");
            }
        });
    }
    
    private void sendMessage(String input) {
        // 添加用户消息到UI
        messageAdapter.addMessage(new ChatMessage(ChatMessage.TYPE_USER, input));
        scrollToBottom();
        
        // 创建助手消息占位
        messageAdapter.addMessage(new ChatMessage(ChatMessage.TYPE_ASSISTANT, ""));
        
        // 发送到 Jimi
        JimiSDK.getInstance().sendMessage(input, new EngineCallback() {
            @Override
            public void onComplete() {
                // 执行完成
            }
            
            @Override
            public void onError(Throwable error) {
                messageAdapter.updateLastMessage("发生错误: " + error.getMessage());
            }
        });
    }
    
    private void handleWireMessage(WireMessage message) {
        if (message instanceof ContentPart) {
            // 流式更新文本
            ContentPart content = (ContentPart) message;
            messageAdapter.appendToLastMessage(content.getText());
            scrollToBottom();
            
        } else if (message instanceof ToolCallMessage) {
            // 工具调用开始
            ToolCallMessage tcm = (ToolCallMessage) message;
            messageAdapter.showToolStatus(tcm.getToolName(), "执行中...");
            
        } else if (message instanceof ToolResultMessage) {
            // 工具调用完成
            ToolResultMessage trm = (ToolResultMessage) message;
            messageAdapter.showToolStatus(trm.getToolCallId(), "完成");
        }
    }
    
    private void scrollToBottom() {
        recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 移除监听器防止内存泄漏
        if (wireListener != null) {
            JimiSDK.getInstance().removeWireListener(wireListener);
        }
    }
}
```

### 6.4 Gradle 依赖配置 (零第三方依赖)

```groovy
// app/build.gradle
dependencies {
    // Jimi Android SDK (本地AAR或远程仓库)
    implementation project(':jimi-android-sdk')
    // 或者
    // implementation 'io.leavesfly:jimi-android-sdk:0.1.0'
    
    // 无需其他第三方依赖！
    // SDK 内部使用 Android 原生 API：
    // - HttpURLConnection (网络请求)
    // - JSONObject/JSONArray (JSON处理)
    // - LruCache (缓存)
    // - ExecutorService (线程管理)
}

// SDK 模块 build.gradle
android {
    defaultConfig {
        minSdk 21
        targetSdk 34
    }
}

dependencies {
    // 只依赖 Android 标准库，无第三方依赖
}
```

---

## 七、购物助手 System Prompt

```markdown
# 电商购物助手

你是一个专业的电商购物助手，帮助用户完成购物相关任务。

## 核心能力

1. **商品搜索与推荐**
   - 根据用户需求搜索商品
   - 提供个性化推荐
   - 比较不同商品的优劣

2. **订单管理**
   - 查询订单状态
   - 追踪物流信息
   - 处理售后问题

3. **购物车操作**
   - 添加/删除商品
   - 修改商品数量
   - 查看购物车总览

## 可用工具

- `product_search`: 搜索商品
- `order_query`: 查询订单
- `cart_manager`: 管理购物车

## 交互原则

1. 简洁友好，避免冗长回复
2. 主动理解用户意图，必要时追问确认
3. 商品信息突出价格、评分、销量等关键指标
4. 敏感操作（如清空购物车）需二次确认

## 响应格式示例

用户: 帮我找一款200以内的蓝牙耳机
助手: 好的，我来帮您搜索200元以内的蓝牙耳机 🎧

[调用 product_search]

为您找到以下热门蓝牙耳机：

1. **XX品牌 入耳式蓝牙耳机**
   - 价格: ¥159
   - 评分: 4.8分 | 销量: 10万+
   
2. **YY品牌 头戴式蓝牙耳机**
   - 价格: ¥199  
   - 评分: 4.7分 | 销量: 5万+

您对哪款感兴趣？我可以帮您查看详情或加入购物车。
```

---

## 八、模块结构与包大小预估 (Java版)

### 8.1 最终模块结构

```
jimi-android-sdk/                       (纯Java实现)
├── core/                               # ~120KB
│   ├── engine/
│   │   ├── Engine.java
│   │   ├── JimiEngine.java
│   │   ├── AgentExecutor.java
│   │   ├── EngineCallback.java
│   │   ├── StreamCallback.java
│   │   └── context/
│   ├── agent/
│   │   └── Agent.java
│   ├── llm/
│   │   ├── ChatProvider.java
│   │   ├── HttpChatProvider.java
│   │   ├── LLM.java
│   │   └── message/
│   │       ├── Message.java
│   │       ├── ToolCall.java
│   │       ├── FunctionCall.java
│   │       └── ChatCompletionChunk.java
│   ├── tool/
│   │   ├── Tool.java
│   │   ├── ToolRegistry.java
│   │   ├── ToolResult.java
│   │   └── ToolSchema.java
│   └── wire/
│       ├── Wire.java
│       ├── WireImpl.java
│       ├── WireListener.java
│       └── message/
│           ├── WireMessage.java
│           ├── ContentPart.java
│           ├── ToolCallMessage.java
│           └── ToolResultMessage.java
│
├── tools/                              # ~40KB
│   ├── ProductSearchTool.java
│   ├── OrderQueryTool.java
│   ├── CartManagerTool.java
│   └── model/
│       ├── Product.java
│       ├── Order.java
│       ├── Cart.java
│       └── CartItem.java
│
├── sdk/                                # ~20KB
│   ├── JimiSDK.java
│   ├── JimiConfig.java
│   └── EcommerceApi.java
│
└── ui/ (可选)                           # ~80KB
    ├── ShoppingAssistantActivity.java
    ├── MessageAdapter.java
    ├── ChatMessage.java
    └── res/layout/
```

### 8.2 包大小估算 (零依赖版本)

| 模块 | 代码大小 | 依赖大小 | 总计 |
|------|----------|----------|------|
| core | 120KB | 0 | 120KB |
| tools | 40KB | 0 | 40KB |
| sdk | 20KB | 0 | 20KB |
| ui (可选) | 80KB | 0 | 80KB |
| **第三方依赖** | - | **0** | **0** |
| **总计（不含UI）** | 180KB | 0 | **~180KB** |
| **总计（含UI）** | 260KB | 0 | **~260KB** |

**对比原Kotlin方案：**
- Kotlin版本: ~530KB (含Kotlin Coroutines 300KB)
- Java版本: ~180KB (零依赖)
- **节省: 66%**

---

## 九、实施计划

### 9.1 阶段划分

| 阶段 | 任务 | 预估工时 | 产出物 |
|------|------|----------|--------|
| **Phase 1** | 核心模块移植（engine/agent/wire） | 3天 | core模块 |
| **Phase 2** | LLM层适配（HttpChatProvider） | 2天 | llm模块 |
| **Phase 3** | 工具系统移植 + 电商工具 | 3天 | tools-ecommerce模块 |
| **Phase 4** | SDK集成层 | 1天 | integration模块 |
| **Phase 5** | UI组件开发（可选） | 2天 | ui模块 |
| **Phase 6** | 集成测试 + 性能优化 | 2天 | 测试报告 |
| **总计** | | **13天** | |

### 9.2 验收标准

1. SDK 包大小 ≤ 600KB（不含 UI）
2. 冷启动初始化时间 ≤ 500ms
3. 消息响应延迟（首字节）≤ 2s
4. 内存占用峰值 ≤ 30MB
5. 工具调用成功率 ≥ 99%

---

## 十、风险与应对

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| HttpURLConnection SSE 解析 | 中 | 完善边界情况处理和单元测试 |
| JSON 手动解析错误 | 低 | 添加完整的单元测试覆盖 |
| LLM API 限流 | 中 | 实现请求队列和重试机制 |
| 移动网络不稳定 | 高 | 添加超时和重连逻辑 |
| 主线程阻塞 | 高 | 确保LLM调用在后台线程执行 |
| 内存泄漏 (监听器未移除) | 中 | 文档强调+示例代码展示 |

---

## 十一、附录

### A. 核心接口清单 (Java)

```java
// 必须实现的核心接口
public interface Engine {
    String getName();
    String getModel();
    void run(String input, EngineCallback callback);
    void addWireListener(WireListener listener);
    void removeWireListener(WireListener listener);
    void clearHistory();
    void shutdown();
}

public interface ChatProvider {
    String getModelName();
    void generateStream(
        String systemPrompt,
        List<Message> history,
        List<ToolSchema> tools,
        StreamCallback<ChatCompletionChunk> callback
    );
}

public interface Tool {
    String getName();
    String getDescription();
    ToolSchema getSchema();
    ToolResult execute(String arguments) throws Exception;
}

public interface Wire {
    void send(WireMessage message);
    void addListener(WireListener listener);
    void removeListener(WireListener listener);
}

// 回调接口
public interface EngineCallback {
    void onComplete();
    void onError(Throwable error);
}

public interface StreamCallback<T> {
    void onNext(T item);
    void onComplete();
    void onError(Throwable error);
}

public interface WireListener {
    void onMessage(WireMessage message);
}
```

### B. 电商 API 接口要求 (Java)

```java
/**
 * 电商 API 接口
 * 由宿主 App 实现，提供对电商后端的访问能力
 */
public interface EcommerceApi {
    
    /**
     * 搜索商品
     */
    List<Product> searchProducts(
        String keyword,
        String category,
        Double minPrice,
        Double maxPrice,
        String sortBy
    ) throws Exception;
    
    /**
     * 查询订单
     */
    List<Order> queryOrders(
        String orderId,
        String status,
        int recentDays
    ) throws Exception;
    
    /**
     * 获取购物车
     */
    Cart getCart() throws Exception;
    
    /**
     * 添加到购物车
     */
    void addToCart(String productId, int quantity) throws Exception;
    
    /**
     * 从购物车移除
     */
    void removeFromCart(String productId) throws Exception;
    
    /**
     * 更新购物车数量
     */
    void updateCartQuantity(String productId, int quantity) throws Exception;
}
```

### C. 核心代码结构调整清单

| 原文件 | 调整内容 |
|--------|----------|
| `Engine.java` | 添加回调参数，移除Mono返回值 |
| `JimiEngine.java` | 移除Spring注解，添加ExecutorService |
| `AgentExecutor.java` | Mono/Flux替换为同步回调 |
| `ChatProvider.java` | Flux替换为StreamCallback |
| `Wire.java` | Flux替换为Listener模式 |
| `Agent.java` | 移除Lombok，手写getter/setter |
| `Message.java` | 移除Lombok，添加toJson()方法 |
| `ToolResult.java` | 移除Lombok，添加静态工厂方法 |

### D. 不需要移植的模块

| 模块 | 原因 |
|------|------|
| `ui/shell/` | JLine CLI，Android使用原生UI |
| `cli/` | 命令行入口，Android不需要 |
| `command/` | CLI命令系统，Android不需要 |
| `tool/bash/` | Shell工具，Android不适用 |
| `tool/file/` | 文件操作工具，电商场景不需要 |
| `mcp/` | MCP协议，电商场景不需要 |
| `skill/` | Skills系统，简化版不包含 |
| `session/` | 会话持久化，简化版使用本地文件 |

---

**文档结束**
