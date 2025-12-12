package io.leavesfly.jimi.android.tools.ecommerce;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 购物车实体
 */
public class Cart {

    private List<CartItem> items;
    private double totalAmount;

    public Cart() {
        this.items = new ArrayList<>();
        this.totalAmount = 0;
    }

    // Getters and Setters
    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
        recalculateTotal();
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    /**
     * 重新计算总金额
     */
    public void recalculateTotal() {
        totalAmount = 0;
        if (items != null) {
            for (CartItem item : items) {
                totalAmount += item.getPrice() * item.getQuantity();
            }
        }
    }

    /**
     * 获取商品数量
     */
    public int getItemCount() {
        int count = 0;
        if (items != null) {
            for (CartItem item : items) {
                count += item.getQuantity();
            }
        }
        return count;
    }

    /**
     * 转换为 JSON
     */
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("totalAmount", totalAmount);
        json.put("itemCount", getItemCount());
        if (items != null && !items.isEmpty()) {
            JSONArray itemsArray = new JSONArray();
            for (CartItem item : items) {
                itemsArray.put(item.toJson());
            }
            json.put("items", itemsArray);
        }
        return json;
    }

    /**
     * 转换为简要字符串（用于 LLM）
     */
    public String toSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("购物车共 %d 件商品，合计 ￥%.2f\n", getItemCount(), totalAmount));
        if (items != null && !items.isEmpty()) {
            for (CartItem item : items) {
                sb.append(String.format("- %s x%d = ￥%.2f\n",
                        item.getProductName(), item.getQuantity(), item.getPrice() * item.getQuantity()));
            }
        }
        return sb.toString().trim();
    }

    /**
     * 购物车项
     */
    public static class CartItem {
        private String productId;
        private String productName;
        private double price;
        private int quantity;
        private String imageUrl;

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("productId", productId);
            json.put("productName", productName);
            json.put("price", price);
            json.put("quantity", quantity);
            json.put("subtotal", price * quantity);
            return json;
        }

        public static CartItem create(String productId, String productName, double price, int quantity) {
            CartItem item = new CartItem();
            item.productId = productId;
            item.productName = productName;
            item.price = price;
            item.quantity = quantity;
            return item;
        }
    }
}
