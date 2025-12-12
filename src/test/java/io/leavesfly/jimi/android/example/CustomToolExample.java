package io.leavesfly.jimi.android.example;

import io.leavesfly.jimi.android.core.agent.Agent;
import io.leavesfly.jimi.android.core.engine.Engine;
import io.leavesfly.jimi.android.core.engine.EngineCallback;
import io.leavesfly.jimi.android.core.wire.WireListener;
import io.leavesfly.jimi.android.core.wire.message.ContentPartMessage;
import io.leavesfly.jimi.android.core.wire.message.ToolCallMessage;
import io.leavesfly.jimi.android.core.wire.message.WireMessage;
import io.leavesfly.jimi.android.sdk.JimiConfig;
import io.leavesfly.jimi.android.sdk.JimiSDK;
import io.leavesfly.jimi.android.tool.Tool;
import io.leavesfly.jimi.android.tool.ToolResult;
import io.leavesfly.jimi.android.tool.ToolSchema;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

/**
 * 自定义工具示例
 * 
 * 演示如何创建和注册自定义工具
 */
public class CustomToolExample {
    
    public static void main(String[] args) throws Exception {
        // 1. 初始化 SDK
        JimiConfig config = new JimiConfig.Builder()
            .apiKey("your-api-key-here")
            .apiEndpoint("https://api.moonshot.cn/v1")
            .modelName("moonshot-v1-8k")
            .build();
        
        JimiSDK.initialize(config);
        
        // 2. 注册自定义工具
        JimiSDK.registerTool(new WeatherTool());
        
        // 3. 创建 Agent
        Agent agent = Agent.builder()
            .name("智能助手")
            .systemPrompt("你是一个智能助手，可以查询天气信息")
            .tools(Arrays.asList("get_weather"))
            .build();
        
        // 4. 创建 Engine
        Engine engine = JimiSDK.createEngine(agent);
        
        // 5. 添加监听器
        engine.addWireListener(new WireListener() {
            @Override
            public void onMessage(WireMessage message) {
                if (message instanceof ContentPartMessage) {
                    ContentPartMessage content = (ContentPartMessage) message;
                    System.out.print(content.getContent());
                } else if (message instanceof ToolCallMessage) {
                    ToolCallMessage toolCall = (ToolCallMessage) message;
                    System.out.println("\n[调用工具: " + toolCall.getToolName() + "]");
                }
            }
        });
        
        // 6. 执行对话
        CountDownLatch latch = new CountDownLatch(1);
        System.out.println("=== 自定义工具示例 ===\n");
        System.out.println("用户: 北京今天天气怎么样？\n");
        System.out.print("助手: ");
        
        engine.run("北京今天天气怎么样？", new EngineCallback() {
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
        
        latch.await();
        
        // 7. 清理资源
        JimiSDK.destroyEngine(engine);
        JimiSDK.shutdown();
    }
    
    /**
     * 天气查询工具
     */
    static class WeatherTool implements Tool {
        
        @Override
        public String getName() {
            return "get_weather";
        }
        
        @Override
        public String getDescription() {
            return "查询指定城市的天气信息";
        }
        
        @Override
        public ToolSchema getSchema() {
            try {
                // 构建符合 OpenAI Function Calling 标准的参数 Schema
                JSONObject parameters = new JSONObject();
                parameters.put("type", "object");
                
                JSONObject properties = new JSONObject();
                JSONObject cityProp = new JSONObject();
                cityProp.put("type", "string");
                cityProp.put("description", "城市名称，例如：北京、上海");
                properties.put("city", cityProp);
                
                parameters.put("properties", properties);
                parameters.put("required", new String[]{"city"});
                
                return new ToolSchema(getName(), getDescription(), parameters);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create tool schema", e);
            }
        }
        
        @Override
        public ToolResult execute(String arguments) {
            try {
                JSONObject args = new JSONObject(arguments);
                String city = args.optString("city", "未知城市");
                
                // 模拟天气查询
                String weather = String.format(
                    "%s今天天气：晴朗，温度 20-28°C，空气质量良好",
                    city
                );
                
                return ToolResult.success(weather);
            } catch (Exception e) {
                return ToolResult.error("天气查询失败: " + e.getMessage());
            }
        }
    }
}
