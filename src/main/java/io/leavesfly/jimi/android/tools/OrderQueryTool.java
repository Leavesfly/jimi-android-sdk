package io.leavesfly.jimi.android.tools;

import io.leavesfly.jimi.android.tool.Tool;
import io.leavesfly.jimi.android.tool.ToolResult;
import io.leavesfly.jimi.android.tool.ToolSchema;
import io.leavesfly.jimi.android.tools.ecommerce.EcommerceApi;
import io.leavesfly.jimi.android.tools.ecommerce.Order;

import org.json.JSONObject;

import java.util.List;

/**
 * 订单查询工具
 * 用于查询用户的订单信息
 */
public class OrderQueryTool implements Tool {

    public static final String NAME = "order_query";

    private final EcommerceApi api;

    public OrderQueryTool(EcommerceApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "查询订单。可以查询订单列表或指定订单的详情，支持按状态筛选。";
    }

    @Override
    public ToolSchema getSchema() {
        try {
            JSONObject parameters = new JSONObject();
            parameters.put("type", "object");

            JSONObject properties = new JSONObject();

            // orderId 参数
            JSONObject orderId = new JSONObject();
            orderId.put("type", "string");
            orderId.put("description", "订单ID，如果指定则返回该订单详情");
            properties.put("orderId", orderId);

            // status 参数
            JSONObject status = new JSONObject();
            status.put("type", "string");
            status.put("enum", new org.json.JSONArray("[\"pending\", \"paid\", \"shipped\", \"delivered\", \"cancelled\"]"));
            status.put("description", "订单状态筛选：pending=待付款, paid=已付款, shipped=已发货, delivered=已送达, cancelled=已取消");
            properties.put("status", status);

            // limit 参数
            JSONObject limit = new JSONObject();
            limit.put("type", "integer");
            limit.put("description", "返回结果数量，默认5");
            properties.put("limit", limit);

            parameters.put("properties", properties);

            return new ToolSchema(NAME, getDescription(), parameters);
        } catch (Exception e) {
            return new ToolSchema(NAME, getDescription(), null);
        }
    }

    @Override
    public ToolResult execute(String arguments) {
        try {
            JSONObject args = new JSONObject(arguments);

            String orderId = args.optString("orderId", null);
            String status = args.optString("status", null);
            int limit = args.optInt("limit", 5);

            // 如果指定了订单ID，返回订单详情
            if (orderId != null && !orderId.isEmpty()) {
                Order order = api.getOrderById(orderId);
                if (order == null) {
                    return ToolResult.error("订单不存在: " + orderId);
                }
                return ToolResult.success(formatOrderDetail(order));
            }

            // 否则返回订单列表
            List<Order> orders = api.queryOrders(status, limit);

            if (orders == null || orders.isEmpty()) {
                String msg = status != null ? "没有找到状态为 " + status + " 的订单" : "没有找到订单";
                return ToolResult.success(msg);
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("找到 %d 个订单:\n\n", orders.size()));
            for (int i = 0; i < orders.size(); i++) {
                Order o = orders.get(i);
                sb.append(String.format("%d. %s\n", i + 1, o.toSummary()));
            }

            return ToolResult.success(sb.toString().trim());

        } catch (Exception e) {
            return ToolResult.error("查询订单失败: " + e.getMessage());
        }
    }

    private String formatOrderDetail(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("订单详情:\n");
        sb.append(String.format("订单号: %s\n", order.getId()));
        sb.append(String.format("状态: %s\n", order.getStatus()));
        sb.append(String.format("金额: ￥%.2f\n", order.getTotalAmount()));
        sb.append(String.format("下单时间: %s\n", order.getCreateTime()));
        if (order.getTrackingNumber() != null) {
            sb.append(String.format("快递单号: %s\n", order.getTrackingNumber()));
        }
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            sb.append("商品列表:\n");
            for (Order.OrderItem item : order.getItems()) {
                sb.append(String.format("  - %s x%d = ￥%.2f\n",
                        item.getProductName(), item.getQuantity(), item.getPrice() * item.getQuantity()));
            }
        }
        return sb.toString().trim();
    }
}
