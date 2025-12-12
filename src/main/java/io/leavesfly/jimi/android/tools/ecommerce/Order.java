package io.leavesfly.jimi.android.tools.ecommerce;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单实体
 */
public class Order {

    private String id;
    private String status;
    private double totalAmount;
    private String createTime;
    private String payTime;
    private String shipTime;
    private String deliverTime;
    private List<OrderItem> items;
    private String shippingAddress;
    private String trackingNumber;

    public Order() {
        this.items = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getPayTime() {
        return payTime;
    }

    public void setPayTime(String payTime) {
        this.payTime = payTime;
    }

    public String getShipTime() {
        return shipTime;
    }

    public void setShipTime(String shipTime) {
        this.shipTime = shipTime;
    }

    public String getDeliverTime() {
        return deliverTime;
    }

    public void setDeliverTime(String deliverTime) {
        this.deliverTime = deliverTime;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    /**
     * 转换为 JSON
     */
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("status", status);
        json.put("totalAmount", totalAmount);
        json.put("createTime", createTime);
        if (trackingNumber != null) {
            json.put("trackingNumber", trackingNumber);
        }
        if (items != null && !items.isEmpty()) {
            JSONArray itemsArray = new JSONArray();
            for (OrderItem item : items) {
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
        String statusText = getStatusText(status);
        return String.format("订单 %s - %s - ￥%.2f (%s)", id, statusText, totalAmount, createTime);
    }

    private String getStatusText(String status) {
        if (status == null) return "未知";
        switch (status) {
            case "pending":
                return "待付款";
            case "paid":
                return "已付款";
            case "shipped":
                return "已发货";
            case "delivered":
                return "已送达";
            case "cancelled":
                return "已取消";
            default:
                return status;
        }
    }

    /**
     * 订单项
     */
    public static class OrderItem {
        private String productId;
        private String productName;
        private double price;
        private int quantity;

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

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("productId", productId);
            json.put("productName", productName);
            json.put("price", price);
            json.put("quantity", quantity);
            return json;
        }
    }

    /**
     * Builder 模式
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Order order = new Order();

        public Builder id(String id) {
            order.id = id;
            return this;
        }

        public Builder status(String status) {
            order.status = status;
            return this;
        }

        public Builder totalAmount(double totalAmount) {
            order.totalAmount = totalAmount;
            return this;
        }

        public Builder createTime(String createTime) {
            order.createTime = createTime;
            return this;
        }

        public Builder trackingNumber(String trackingNumber) {
            order.trackingNumber = trackingNumber;
            return this;
        }

        public Builder items(List<OrderItem> items) {
            order.items = items;
            return this;
        }

        public Order build() {
            return order;
        }
    }
}
