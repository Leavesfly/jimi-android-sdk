package io.leavesfly.jimi.android.tools.ecommerce;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 商品实体
 */
public class Product {

    private String id;
    private String name;
    private double price;
    private double originalPrice;
    private double rating;
    private int sales;
    private String category;
    private String imageUrl;
    private String description;
    private int stock;

    public Product() {
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getSales() {
        return sales;
    }

    public void setSales(int sales) {
        this.sales = sales;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    /**
     * 转换为 JSON
     */
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("price", price);
        json.put("originalPrice", originalPrice);
        json.put("rating", rating);
        json.put("sales", sales);
        json.put("category", category);
        json.put("stock", stock);
        return json;
    }

    /**
     * 转换为简要字符串（用于 LLM）
     */
    public String toSummary() {
        return String.format("[%s] %s - ￥%.2f (评分: %.1f, 销量: %d)",
                id, name, price, rating, sales);
    }

    /**
     * Builder 模式
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Product product = new Product();

        public Builder id(String id) {
            product.id = id;
            return this;
        }

        public Builder name(String name) {
            product.name = name;
            return this;
        }

        public Builder price(double price) {
            product.price = price;
            return this;
        }

        public Builder originalPrice(double originalPrice) {
            product.originalPrice = originalPrice;
            return this;
        }

        public Builder rating(double rating) {
            product.rating = rating;
            return this;
        }

        public Builder sales(int sales) {
            product.sales = sales;
            return this;
        }

        public Builder category(String category) {
            product.category = category;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            product.imageUrl = imageUrl;
            return this;
        }

        public Builder description(String description) {
            product.description = description;
            return this;
        }

        public Builder stock(int stock) {
            product.stock = stock;
            return this;
        }

        public Product build() {
            return product;
        }
    }
}
