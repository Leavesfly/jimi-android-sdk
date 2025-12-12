# Jimi SDK

[![Java](https://img.shields.io/badge/Java-8%2B-orange)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Version](https://img.shields.io/badge/Version-1.0.0--SNAPSHOT-green)](https://github.com)

> 极简、高效、零依赖的 AI Agent SDK，专为 Java/Android 平台打造

Jimi SDK 是一个轻量级的 AI Agent 框架，支持构建智能对话助手和工具调用系统。它采用回调驱动的异步架构，无需复杂依赖，开箱即用。

## ✨ 核心特性

- 🚀 **零依赖** - 仅依赖 `org.json` 进行 JSON 处理，无其他第三方库
- 🎯 **轻量级** - 核心代码简洁高效，包体积小
- 🔄 **流式响应** - 支持 SSE 流式输出，实时响应用户
- 🛠️ **工具调用** - 内置工具系统，可自定义扩展业务能力
- 🧵 **异步设计** - 后台线程执行，主线程回调，不阻塞 UI
- 🔌 **可扩展** - 灵活的架构设计，支持自定义 LLM 提供商
- 📦 **开箱即用** - Builder 模式，API 简洁直观

## 📋 目录

- [快速开始](#-快速开始)
- [架构设计](#-架构设计)
- [核心概念](#-核心概念)
- [使用指南](#-使用指南)
- [示例代码](#-示例代码)
- [API 文档](#-api-文档)
- [开发路线](#-开发路线)

## 🚀 快速开始

### 环境要求

- Java 8 或更高版本
- Maven 3.6+ 或 Gradle 6.0+

### Maven 依赖

```xml
<dependency>
    <groupId>io.leavesfly.jimi</groupId>
    <artifactId>jimi-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 5 分钟上手

```java
import io.leavesfly.jimi.android.core.agent.Agent;
import io.leavesfly.jimi.android.core.engine.Engine;
import io.leavesfly.jimi.android.core.engine.EngineCallback;
import io.leavesfly.jimi.android.sdk.JimiConfig;
import io.leavesfly.jimi.android.sdk.JimiSDK;

public class QuickStart {
    public static void main(String[] args) {
        // 1. 配置 SDK
        JimiConfig config = new JimiConfig.Builder()
            .apiKey("your-api-key")
            .apiEndpoint("https://api.moonshot.cn/v1")
            .modelName("moonshot-v1-8k")
            .build();
        
        // 2. 初始化
        JimiSDK.initialize(config);
        
        // 3. 创建 Agent
        Agent agent = Agent.builder()
            .name("助手")
            .systemPrompt("你是一个友好的 AI 助手")
            .build();
        
        // 4. 创建 Engine 并运行
        Engine engine = JimiSDK.createEngine(agent);
        engine.run("你好", new EngineCallback() {
            @Override
            public void onComplete() {
                System.out.println("完成！");
            }
            
            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
            }
        });
    }
}
```

## 🏗️ 架构设计

### 核心架构

```
┌─────────────────────────────────────────┐
│           JimiSDK (门面层)               │
│  - 初始化配置                             │
│  - Engine 生命周期管理                    │
│  - 工具注册                               │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│          Engine (引擎层)                 │
│  - Agent 执行主循环                       │
│  - 上下文管理 (Context)                   │
│  - 运行时状态 (Runtime)                   │
│  - 消息总线 (Wire)                        │
└─────────────────┬───────────────────────┘
                  │
    ┌─────────────┼─────────────┐
    │             │             │
┌───▼────┐  ┌────▼─────┐  ┌───▼────┐
│  LLM   │  │  Tool    │  │ Wire   │
│ 调用层  │  │  工具层   │  │ 消息层  │
└────────┘  └──────────┘  └────────┘
```

### 包结构

```
io.leavesfly.jimi.android/
├── sdk/                    # SDK 对外接口
│   ├── JimiSDK            # SDK 门面
│   ├── JimiConfig         # 配置管理
│   └── JimiFactory        # 工厂类
│
├── core/                   # 核心引擎
│   ├── engine/            # 执行引擎
│   │   ├── Engine         # 引擎接口
│   │   ├── JimiEngine     # 引擎实现
│   │   └── AgentExecutor  # Agent 执行器
│   │
│   ├── agent/             # Agent 配置
│   ├── context/           # 上下文管理
│   ├── runtime/           # 运行时状态
│   ├── compaction/        # 上下文压缩
│   └── wire/              # 消息总线
│       ├── Wire           # 消息总线
│       ├── WireListener   # 消息监听器
│       └── message/       # 消息类型
│
├── llm/                    # LLM 层
│   ├── LLM                # LLM 接口
│   ├── ChatProvider       # 聊天提供商
│   ├── HttpChatProvider   # HTTP 实现
│   └── message/           # LLM 消息模型
│
├── tool/                   # 工具系统
│   ├── Tool               # 工具接口
│   ├── ToolRegistry       # 工具注册表
│   └── ToolSchema         # 工具 Schema
│
└── tools/                  # 内置工具
    ├── ProductSearchTool  # 商品搜索
    ├── OrderQueryTool     # 订单查询
    └── CartManagerTool    # 购物车管理
```

## 💡 核心概念

### 1. Agent（智能体）

Agent 是 AI 助手的配置单元，定义了助手的身份、能力和行为规则。

```java
Agent agent = Agent.builder()
    .name("购物助手")
    .systemPrompt("你是一个专业的电商助手")
    .tools(Arrays.asList("product_search", "cart_manager"))
    .build();
```

### 2. Engine（执行引擎）

Engine 是 Agent 的运行容器，负责管理对话流程、上下文和消息分发。

```java
Engine engine = JimiSDK.createEngine(agent);
engine.run("用户消息", callback);
```

### 3. Wire（消息总线）

Wire 是事件驱动的消息系统，用于实时推送执行过程中的各种事件。

```java
engine.addWireListener(new WireListener() {
    @Override
    public void onMessage(WireMessage message) {
        // 处理消息
    }
});
```

支持的消息类型：
- `StepBegin` - 步骤开始
- `ContentPartMessage` - 内容片段（流式输出）
- `ToolCallMessage` - 工具调用
- `ToolResultMessage` - 工具结果
- `StepInterrupted` - 步骤中断

### 4. Tool（工具）

Tool 是扩展 Agent 能力的关键，可以实现各种业务逻辑。

```java
public class MyTool implements Tool {
    @Override
    public String getName() {
        return "my_tool";
    }
    
    @Override
    public ToolSchema getSchema() {
        // 定义工具的 JSON Schema
    }
    
    @Override
    public ToolResult execute(String arguments) {
        // 实现工具逻辑
    }
}

// 注册工具
JimiSDK.registerTool(new MyTool());
```

### 5. Context（上下文）

Context 管理对话历史，支持自动压缩以控制 Token 消耗。

```java
// SDK 会自动管理上下文
// 支持配置最大上下文大小和压缩策略
config.setMaxContextSize(8000);
```

## 📖 使用指南

### 配置 SDK

```java
JimiConfig config = new JimiConfig.Builder()
    .apiKey("your-api-key")              // API 密钥
    .apiEndpoint("https://api.xxx.com")  // API 端点
    .modelName("moonshot-v1-8k")         // 模型名称
    .maxStepsPerRun(10)                  // 最大步骤数
    .maxContextSize(8000)                // 最大上下文 Token
    .timeout(30000)                      // 超时时间（毫秒）
    .build();

JimiSDK.initialize(config);
```

### 监听流式响应

```java
engine.addWireListener(new WireListener() {
    @Override
    public void onMessage(WireMessage message) {
        if (message instanceof ContentPartMessage) {
            // 实时接收 AI 输出的文本片段
            String text = ((ContentPartMessage) message).getContent();
            System.out.print(text);  // 流式打印
        }
    }
});
```

### 自定义工具

参考 `CustomToolExample.java` 了解如何创建自定义工具。

### 构建购物助手

完整示例请查看 `ShoppingAssistantExample.java`，演示了如何构建一个完整的电商购物助手。

## 📚 示例代码

### 基础对话

```java
// 见 src/test/java/io/leavesfly/jimi/android/example/QuickStartExample.java
```

### 工具调用

```java
// 见 src/test/java/io/leavesfly/jimi/android/example/ShoppingAssistantExample.java
```

### 自定义工具

```java
// 见 src/test/java/io/leavesfly/jimi/android/example/CustomToolExample.java
```

## 📝 API 文档

### JimiSDK

| 方法 | 说明 |
|------|------|
| `initialize(config)` | 初始化 SDK |
| `createEngine(agent)` | 创建 Engine 实例 |
| `destroyEngine(engine)` | 销毁 Engine |
| `registerTool(tool)` | 注册单个工具 |
| `registerTools(tools...)` | 批量注册工具 |
| `shutdown()` | 关闭 SDK |

### Engine

| 方法 | 说明 |
|------|------|
| `run(message, callback)` | 执行对话 |
| `addWireListener(listener)` | 添加消息监听器 |
| `removeWireListener(listener)` | 移除消息监听器 |
| `shutdown()` | 关闭 Engine |

### Agent

| 方法 | 说明 |
|------|------|
| `builder()` | 创建 Builder |
| `name(name)` | 设置名称 |
| `systemPrompt(prompt)` | 设置系统提示词 |
| `tools(toolNames)` | 设置工具列表 |
| `build()` | 构建 Agent |

## 🛣️ 开发路线

### ✅ 已完成（v1.0）

- [x] 基础框架搭建
- [x] Engine 执行引擎
- [x] Wire 消息总线
- [x] Context 上下文管理
- [x] Agent 配置系统
- [x] Tool 工具系统
- [x] LLM 集成（HTTP + SSE）
- [x] 流式响应支持
- [x] 内置电商工具示例

### 🚧 进行中（v1.1）

- [ ] 多轮对话优化
- [ ] 上下文压缩策略
- [ ] 更多 LLM 提供商支持
- [ ] 性能优化
- [ ] 单元测试覆盖

### 📅 计划中（v2.0）

- [ ] Android 特定优化
- [ ] 内存管理优化
- [ ] 插件系统
- [ ] 可视化调试工具
- [ ] 更多内置工具

## 🔧 技术特点

| 特性 | 说明 |
|------|------|
| **极简依赖** | 仅依赖 `org.json`，无其他第三方库 |
| **纯 Java** | 100% Java 8 实现，跨平台兼容 |
| **异步架构** | 回调驱动，后台执行，不阻塞主线程 |
| **线程安全** | 使用 `CopyOnWriteArrayList` 等线程安全集合 |
| **流式处理** | 支持 SSE 流式输出，实时响应 |
| **可扩展** | 插件化设计，易于扩展自定义能力 |

## 💻 开发环境

### 编译项目

```bash
mvn clean compile
```

### 运行测试

```bash
mvn test
```

### 运行示例

```bash
mvn exec:java -Dexec.mainClass="io.leavesfly.jimi.android.example.QuickStartExample"
```

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

## 📄 License

Apache License 2.0

## 📧 联系方式

- GitHub: [jimi-sdk](https://github.com)
- Email: support@leavesfly.io

---

**Made with ❤️ by Leavesfly Team**
