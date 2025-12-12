package io.leavesfly.jimi.android.tools;

import io.leavesfly.jimi.android.tool.Tool;
import io.leavesfly.jimi.android.tool.ToolResult;
import io.leavesfly.jimi.android.tool.ToolSchema;
import io.leavesfly.jimi.android.tools.ecommerce.EcommerceApi;
import io.leavesfly.jimi.android.tools.ecommerce.Product;

import org.json.JSONObject;

import java.util.List;

/**
 * 商品搜索工具
 * 用于搜索电商平台上的商品
 */
public class ProductSearchTool implements Tool {

    public static final String NAME = "product_search";

    private final EcommerceApi api;

    public ProductSearchTool(EcommerceApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "搜索商品。可以根据关键词、品类、价格范围等条件搜索商品，返回商品列表。";
    }

    @Override
    public ToolSchema getSchema() {
        try {
            JSONObject parameters = new JSONObject();
            parameters.put("type", "object");

            JSONObject properties = new JSONObject();

            // keyword 参数
            JSONObject keyword = new JSONObject();
            keyword.put("type", "string");
            keyword.put("description", "搜索关键词，如商品名称、品牌等");
            properties.put("keyword", keyword);

            // category 参数
            JSONObject category = new JSONObject();
            category.put("type", "string");
            category.put("description", "商品品类，如：手机、电脑、服装等");
            properties.put("category", category);

            // minPrice 参数
            JSONObject minPrice = new JSONObject();
            minPrice.put("type", "number");
            minPrice.put("description", "最低价格");
            properties.put("minPrice", minPrice);

            // maxPrice 参数
            JSONObject maxPrice = new JSONObject();
            maxPrice.put("type", "number");
            maxPrice.put("description", "最高价格");
            properties.put("maxPrice", maxPrice);

            // sortBy 参数
            JSONObject sortBy = new JSONObject();
            sortBy.put("type", "string");
            sortBy.put("enum", new org.json.JSONArray("[\"price_asc\", \"price_desc\", \"sales\", \"rating\"]"));
            sortBy.put("description", "排序方式：price_asc=价格升序, price_desc=价格降序, sales=销量, rating=评分");
            properties.put("sortBy", sortBy);

            // limit 参数
            JSONObject limit = new JSONObject();
            limit.put("type", "integer");
            limit.put("description", "返回结果数量，默认5");
            properties.put("limit", limit);

            parameters.put("properties", properties);
            parameters.put("required", new org.json.JSONArray("[\"keyword\"]"));

            return new ToolSchema(NAME, getDescription(), parameters);
        } catch (Exception e) {
            return new ToolSchema(NAME, getDescription(), null);
        }
    }

    @Override
    public ToolResult execute(String arguments) {
        try {
            JSONObject args = new JSONObject(arguments);

            String keyword = args.optString("keyword", "");
            String category = args.optString("category", null);
            Double minPrice = args.has("minPrice") ? args.getDouble("minPrice") : null;
            Double maxPrice = args.has("maxPrice") ? args.getDouble("maxPrice") : null;
            String sortBy = args.optString("sortBy", null);
            int limit = args.optInt("limit", 5);

            if (keyword.isEmpty()) {
                return ToolResult.error("搜索关键词不能为空");
            }

            List<Product> products = api.searchProducts(keyword, category, minPrice, maxPrice, sortBy, limit);

            if (products == null || products.isEmpty()) {
                return ToolResult.success("没有找到符合条件的商品");
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("找到 %d 个商品:\n\n", products.size()));
            for (int i = 0; i < products.size(); i++) {
                Product p = products.get(i);
                sb.append(String.format("%d. %s\n", i + 1, p.toSummary()));
            }

            return ToolResult.success(sb.toString().trim());

        } catch (Exception e) {
            return ToolResult.error("搜索商品失败: " + e.getMessage());
        }
    }
}
