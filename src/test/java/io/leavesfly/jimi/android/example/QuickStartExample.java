package io.leavesfly.jimi.android.example;

import io.leavesfly.jimi.android.core.agent.Agent;
import io.leavesfly.jimi.android.core.engine.Engine;
import io.leavesfly.jimi.android.core.engine.EngineCallback;
import io.leavesfly.jimi.android.core.wire.WireListener;
import io.leavesfly.jimi.android.core.wire.message.ContentPartMessage;
import io.leavesfly.jimi.android.core.wire.message.WireMessage;
import io.leavesfly.jimi.android.sdk.JimiConfig;
import io.leavesfly.jimi.android.sdk.JimiSDK;

import java.util.concurrent.CountDownLatch;

/**
 * 快速入门示例
 * 
 * 演示 Jimi SDK 的基本用法（不使用工具）
 */
public class QuickStartExample {
    
    public static void main(String[] args) throws Exception {
        // 1. 配置 SDK
        JimiConfig config = new JimiConfig.Builder()
            .apiKey("your-api-key-here")
            .apiEndpoint("https://api.moonshot.cn/v1")
            .modelName("moonshot-v1-8k")
            .build();
        
        // 2. 初始化 SDK
        JimiSDK.initialize(config);
        
        // 3. 创建 Agent
        Agent agent = Agent.builder()
            .name("助手")
            .systemPrompt("你是一个友好的 AI 助手")
            .build();
        
        // 4. 创建 Engine
        Engine engine = JimiSDK.createEngine(agent);
        
        // 5. 监听响应（可选）
        engine.addWireListener(new WireListener() {
            @Override
            public void onMessage(WireMessage message) {
                if (message instanceof ContentPartMessage) {
                    ContentPartMessage content = (ContentPartMessage) message;
                    System.out.print(content.getContent());
                }
            }
        });
        
        // 6. 执行对话
        CountDownLatch latch = new CountDownLatch(1);
        System.out.println("用户: 你好，介绍一下自己\n");
        System.out.print("助手: ");
        
        engine.run("你好，介绍一下自己", new EngineCallback() {
            @Override
            public void onComplete() {
                System.out.println("\n");
                latch.countDown();
            }
            
            @Override
            public void onError(Throwable error) {
                System.err.println("\n错误: " + error.getMessage());
                latch.countDown();
            }
        });
        
        // 等待完成
        latch.await();
        
        // 7. 清理资源
        JimiSDK.destroyEngine(engine);
        JimiSDK.shutdown();
    }
}
