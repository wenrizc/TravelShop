package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.entity.CartPromotion;
import com.hmdp.dto.Result;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车促销服务接口
 */
public interface CartPromotionService extends IService<CartPromotion> {

    /**
     * 查询购物车关联的促销活动
     * @param cartId 购物车ID
     * @return 促销关联列表
     */
    List<CartPromotion> listByCartId(Long cartId);

    /**
     * 查询购物车关联的特定类型促销活动
     * @param cartId 购物车ID
     * @param promotionType 促销类型
     * @return 促销关联列表
     */
    List<CartPromotion> listByType(Long cartId, Integer promotionType);

    /**
     * 添加促销活动到购物车
     * @param cartId 购物车ID
     * @param promotionId 促销活动ID
     * @param promotionType 促销类型
     * @param discountAmount 优惠金额
     * @return 结果
     */
    Result addPromotion(Long cartId, Long promotionId, Integer promotionType, BigDecimal discountAmount);

    /**
     * 从购物车移除促销活动
     * @param cartId 购物车ID
     * @param promotionId 促销活动ID
     * @return 结果
     */
    Result removePromotion(Long cartId, Long promotionId);

    /**
     * 清空购物车所有促销关联
     * @param cartId 购物车ID
     * @return 结果
     */
    Result clearPromotions(Long cartId);

    /**
     * 计算购物车所有促销活动优惠总额
     * @param cartId 购物车ID
     * @return 优惠总额
     */
    BigDecimal calculateTotalDiscount(Long cartId);

    /**
     * 检查购物车是否包含指定促销活动
     * @param cartId 购物车ID
     * @param promotionId 促销活动ID
     * @return 是否存在
     */
    boolean hasPromotion(Long cartId, Long promotionId);

    /**
     * 应用满减促销
     * @param cartId 购物车ID
     * @return 应用结果
     */
    Result applyFullReduction(Long cartId);

    /**
     * 应用满折促销
     * @param cartId 购物车ID
     * @return 应用结果
     */
    Result applyDiscount(Long cartId);

    /**
     * 应用商品直减促销
     * @param cartId 购物车ID
     * @param itemIds 购物车项ID列表
     * @return 应用结果
     */
    Result applyDirectReduction(Long cartId, List<Long> itemIds);

    /**
     * 自动应用最优促销方案
     * @param cartId 购物车ID
     * @return 应用结果
     */
    Result applyBestPromotion(Long cartId);
}