package io.leavesfly.jimi.android.example;

import io.leavesfly.jimi.android.core.agent.Agent;
import io.leavesfly.jimi.android.core.engine.Engine;
import io.leavesfly.jimi.android.core.engine.EngineCallback;
import io.leavesfly.jimi.android.core.wire.WireListener;
import io.leavesfly.jimi.android.core.wire.message.ContentPartMessage;
import io.leavesfly.jimi.android.core.wire.message.StepBegin;
import io.leavesfly.jimi.android.core.wire.message.ToolCallMessage;
import io.leavesfly.jimi.android.core.wire.message.ToolResultMessage;
import io.leavesfly.jimi.android.core.wire.message.WireMessage;
import io.leavesfly.jimi.android.sdk.JimiConfig;
import io.leavesfly.jimi.android.sdk.JimiSDK;
import io.leavesfly.jimi.android.tools.CartManagerTool;
import io.leavesfly.jimi.android.tools.OrderQueryTool;
import io.leavesfly.jimi.android.tools.ProductSearchTool;
import io.leavesfly.jimi.android.tools.ecommerce.*;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

/**
 * 购物助手示例
 * 
 * 演示如何使用 Jimi SDK 构建一个电商购物助手
 */
public class ShoppingAssistantExample {
    
    public static void main(String[] args) throws Exception {
        // 1. 创建 Mock 电商 API 实现
        EcommerceApi ecommerceApi = new MockEcommerceApi();
        
        // 2. 初始化 Jimi SDK
        JimiConfig config = new JimiConfig.Builder()
            .apiKey("your-api-key-here")
            .apiEndpoint("https://api.moonshot.cn/v1")
            .modelName("moonshot-v1-8k")
            .maxStepsPerRun(10)
            .maxContextSize(8000)
            .build();
        
        JimiSDK.initialize(config);
        
        // 3. 注册电商工具
        JimiSDK.registerTools(
            new ProductSearchTool(ecommerceApi),
            new OrderQueryTool(ecommerceApi),
            new CartManagerTool(ecommerceApi)
        );
        
        // 4. 创建购物助手 Agent
        Agent shoppingAgent = Agent.builder()
            .name("购物助手")
            .systemPrompt(
                "你是一个专业的电商购物助手。\n" +
                "你可以帮助用户：\n" +
                "1. 搜索商品\n" +
                "2. 查询订单状态\n" +
                "3. 管理购物车\n" +
                "请根据用户需求，调用合适的工具完成任务。"
            )
            .tools(Arrays.asList("product_search", "order_query", "cart_manager"))
            .build();
        
        // 5. 创建 Engine
        Engine engine = JimiSDK.createEngine(shoppingAgent);
        
        // 6. 添加消息监听器
        CountDownLatch latch = new CountDownLatch(1);
        engine.addWireListener(new WireListener() {
            @Override
            public void onMessage(WireMessage message) {
                if (message instanceof StepBegin) {
                    StepBegin stepBegin = (StepBegin) message;
                    System.out.println("\n[Step " + stepBegin.getStepNo() + " 开始]");
                    
                } else if (message instanceof ContentPartMessage) {
                    ContentPartMessage content = (ContentPartMessage) message;
                    System.out.print(content.getContent());
                    
                } else if (message instanceof ToolCallMessage) {
                    ToolCallMessage toolCall = (ToolCallMessage) message;
                    System.out.println("\n[调用工具: " + toolCall.getToolName() + "]");
                    System.out.println("参数: " + toolCall.getArguments());
                    
                } else if (message instanceof ToolResultMessage) {
                    ToolResultMessage toolResult = (ToolResultMessage) message;
                    System.out.println("[工具结果]: " + toolResult.getResult());
                }
            }
        });
        
        // 7. 执行对话
        System.out.println("=== 购物助手示例 ===\n");
        System.out.println("用户: 帮我搜索一下蓝牙耳机\n");
        
        engine.run("帮我搜索一下蓝牙耳机", new EngineCallback() {
            @Override
            public void onComplete() {
                System.out.println("\n\n=== 对话完成 ===");
                latch.countDown();
            }
            
            @Override
            public void onError(Throwable error) {
                System.err.println("\n错误: " + error.getMessage());
                error.printStackTrace();
                latch.countDown();
            }
        });
        
        // 等待完成
        latch.await();
        
        // 8. 清理资源
        JimiSDK.destroyEngine(engine);
        JimiSDK.shutdown();
    }
    
    /**
     * Mock 电商 API 实现（用于演示）
     */
    static class MockEcommerceApi implements EcommerceApi {
        
        @Override
        public java.util.List<Product> searchProducts(String keyword, String category, 
                                                       Double minPrice, Double maxPrice, 
                                                       Integer limit) {
            // 返回模拟商品数据
            java.util.List<Product> products = new java.util.ArrayList<>();
            products.add(new Product(
                "P001",
                "AirPods Pro 蓝牙耳机",
                "Apple AirPods Pro 主动降噪无线蓝牙耳机",
                1999.0,
                "https://example.com/airpods.jpg",
                "电子产品",
                150
            ));
            products.add(new Product(
                "P002",
                "索尼 WH-1000XM5 蓝牙耳机",
                "索尼降噪无线蓝牙耳机",
                2499.0,
                "https://example.com/sony.jpg",
                "电子产品",
                80
            ));
            return products;
        }
        
        @Override
        public Order getOrderById(String orderId) {
            // 返回模拟订单数据
            return new Order(
                orderId,
                "USER001",
                Arrays.asList(
                    new Order.OrderItem("P001", "AirPods Pro 蓝牙耳机", 1, 1999.0)
                ),
                1999.0,
                "已发货",
                "2025-12-10",
                "广东省深圳市"
            );
        }
        
        @Override
        public java.util.List<Order> getOrdersByUserId(String userId, String status, Integer limit) {
            // 返回模拟订单列表
            java.util.List<Order> orders = new java.util.ArrayList<>();
            orders.add(getOrderById("ORD001"));
            return orders;
        }
        
        @Override
        public Cart getCartByUserId(String userId) {
            // 返回模拟购物车
            return new Cart(
                "CART001",
                userId,
                new java.util.ArrayList<>(),
                0.0
            );
        }
        
        @Override
        public boolean addToCart(String userId, String productId, int quantity) {
            return true;
        }
        
        @Override
        public boolean removeFromCart(String userId, String productId) {
            return true;
        }
        
        @Override
        public boolean updateCartItemQuantity(String userId, String productId, int quantity) {
            return true;
        }
        
        @Override
        public boolean clearCart(String userId) {
            return true;
        }
    }
}
