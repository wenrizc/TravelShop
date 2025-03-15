package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.TempCart;
import java.util.List;

/**
 * 临时购物车服务接口（未登录用户使用）
 */
public interface TempCartService extends IService<TempCart> {

    /**
     * 获取会话购物车项
     * @param sessionId 会话ID
     * @return 购物车项列表
     */
    List<TempCart> getBySessionId(String sessionId);

    /**
     * 根据商品信息查询临时购物车项
     * @param sessionId 会话ID
     * @param productId 商品ID
     * @param productType 商品类型
     * @param skuId SKU ID
     * @return 临时购物车项
     */
    TempCart getByProductInfo(String sessionId, Long productId, Integer productType, Long skuId);

    /**
     * 添加商品到临时购物车
     * @param sessionId 会话ID
     * @param productId 商品ID
     * @param productType 商品类型
     * @param quantity 数量
     * @param skuId SKU ID
     * @return 结果
     */
    Result addItem(String sessionId, Long productId, Integer productType, Integer quantity, Long skuId);

    /**
     * 更新临时购物车项数量
     * @param sessionId 会话ID
     * @param id 购物车项ID
     * @param quantity 新数量
     * @return 结果
     */
    Result updateQuantity(String sessionId, Long id, Integer quantity);

    /**
     * 从临时购物车删除商品
     * @param sessionId 会话ID
     * @param id 购物车项ID
     * @return 结果
     */
    Result removeItem(String sessionId, Long id);

    /**
     * 清空临时购物车
     * @param sessionId 会话ID
     * @return 结果
     */
    Result clearCart(String sessionId);

    /**
     * 临时购物车"立即购买"
     * @param sessionId 会话ID
     * @param productId 商品ID
     * @param productType 商品类型
     * @param quantity 数量
     * @param skuId SKU ID
     * @return 临时订单信息
     */
    Result buyNow(String sessionId, Long productId, Integer productType, Integer quantity, Long skuId);

    /**
     * 清理过期的临时购物车数据
     * @return 清理的记录数
     */
    int cleanExpiredItems();

    /**
     * 获取临时购物车商品数量
     * @param sessionId 会话ID
     * @return 商品数量
     */
    int countItems(String sessionId);

    // 移除mergeToUserCart方法，由CartMergeService负责
}