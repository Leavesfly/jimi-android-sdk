package io.leavesfly.jimi.android.tools;

import io.leavesfly.jimi.android.tool.Tool;
import io.leavesfly.jimi.android.tool.ToolResult;
import io.leavesfly.jimi.android.tool.ToolSchema;
import io.leavesfly.jimi.android.tools.ecommerce.Cart;
import io.leavesfly.jimi.android.tools.ecommerce.EcommerceApi;

import org.json.JSONObject;

/**
 * 购物车管理工具
 * 用于查看和管理购物车
 */
public class CartManagerTool implements Tool {

    public static final String NAME = "cart_manager";

    private final EcommerceApi api;

    public CartManagerTool(EcommerceApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "管理购物车。支持查看购物车、添加商品、移除商品、修改数量等操作。";
    }

    @Override
    public ToolSchema getSchema() {
        try {
            JSONObject parameters = new JSONObject();
            parameters.put("type", "object");

            JSONObject properties = new JSONObject();

            // action 参数
            JSONObject action = new JSONObject();
            action.put("type", "string");
            action.put("enum", new org.json.JSONArray("[\"view\", \"add\", \"remove\", \"update\"]"));
            action.put("description", "操作类型：view=查看购物车, add=添加商品, remove=移除商品, update=更新数量");
            properties.put("action", action);

            // productId 参数
            JSONObject productId = new JSONObject();
            productId.put("type", "string");
            productId.put("description", "商品ID（add/remove/update 操作必填）");
            properties.put("productId", productId);

            // quantity 参数
            JSONObject quantity = new JSONObject();
            quantity.put("type", "integer");
            quantity.put("description", "商品数量（add/update 操作使用，默认1）");
            properties.put("quantity", quantity);

            parameters.put("properties", properties);
            parameters.put("required", new org.json.JSONArray("[\"action\"]"));

            return new ToolSchema(NAME, getDescription(), parameters);
        } catch (Exception e) {
            return new ToolSchema(NAME, getDescription(), null);
        }
    }

    @Override
    public ToolResult execute(String arguments) {
        try {
            JSONObject args = new JSONObject(arguments);

            String action = args.optString("action", "view");
            String productId = args.optString("productId", null);
            int quantity = args.optInt("quantity", 1);

            switch (action) {
                case "view":
                    return viewCart();

                case "add":
                    if (productId == null || productId.isEmpty()) {
                        return ToolResult.error("添加商品需要指定 productId");
                    }
                    return addToCart(productId, quantity);

                case "remove":
                    if (productId == null || productId.isEmpty()) {
                        return ToolResult.error("移除商品需要指定 productId");
                    }
                    return removeFromCart(productId);

                case "update":
                    if (productId == null || productId.isEmpty()) {
                        return ToolResult.error("更新数量需要指定 productId");
                    }
                    return updateQuantity(productId, quantity);

                default:
                    return ToolResult.error("不支持的操作: " + action);
            }

        } catch (Exception e) {
            return ToolResult.error("购物车操作失败: " + e.getMessage());
        }
    }

    private ToolResult viewCart() {
        Cart cart = api.getCart();
        if (cart == null || cart.getItems().isEmpty()) {
            return ToolResult.success("购物车为空");
        }
        return ToolResult.success(cart.toSummary());
    }

    private ToolResult addToCart(String productId, int quantity) {
        Cart cart = api.addToCart(productId, quantity);
        return ToolResult.success("已添加到购物车\n\n" + cart.toSummary());
    }

    private ToolResult removeFromCart(String productId) {
        Cart cart = api.removeFromCart(productId);
        return ToolResult.success("已从购物车移除\n\n" + cart.toSummary());
    }

    private ToolResult updateQuantity(String productId, int quantity) {
        Cart cart = api.updateCartQuantity(productId, quantity);
        return ToolResult.success("已更新数量\n\n" + cart.toSummary());
    }
}
