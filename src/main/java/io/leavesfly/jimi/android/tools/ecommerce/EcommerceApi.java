package io.leavesfly.jimi.android.tools.ecommerce;

import java.util.List;

/**
 * 电商 API 接口
 * 定义电商平台需要实现的服务接口
 */
public interface EcommerceApi {

    /**
     * 搜索商品
     *
     * @param keyword  搜索关键词
     * @param category 品类（可选）
     * @param minPrice 最低价格（可选）
     * @param maxPrice 最高价格（可选）
     * @param sortBy   排序方式：price_asc, price_desc, sales, rating
     * @param limit    返回数量限制
     * @return 商品列表
     */
    List<Product> searchProducts(
            String keyword,
            String category,
            Double minPrice,
            Double maxPrice,
            String sortBy,
            int limit
    );

    /**
     * 获取商品详情
     *
     * @param productId 商品 ID
     * @return 商品信息
     */
    Product getProductById(String productId);

    /**
     * 查询订单列表
     *
     * @param status 订单状态（可选）：pending, shipped, delivered, cancelled
     * @param limit  返回数量限制
     * @return 订单列表
     */
    List<Order> queryOrders(String status, int limit);

    /**
     * 获取订单详情
     *
     * @param orderId 订单 ID
     * @return 订单信息
     */
    Order getOrderById(String orderId);

    /**
     * 获取购物车
     *
     * @return 购物车信息
     */
    Cart getCart();

    /**
     * 添加商品到购物车
     *
     * @param productId 商品 ID
     * @param quantity  数量
     * @return 更新后的购物车
     */
    Cart addToCart(String productId, int quantity);

    /**
     * 从购物车移除商品
     *
     * @param productId 商品 ID
     * @return 更新后的购物车
     */
    Cart removeFromCart(String productId);

    /**
     * 更新购物车商品数量
     *
     * @param productId 商品 ID
     * @param quantity  新数量
     * @return 更新后的购物车
     */
    Cart updateCartQuantity(String productId, int quantity);
}
