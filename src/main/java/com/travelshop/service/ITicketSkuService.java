package com.travelshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travelshop.entity.TicketSku;

import java.util.List;

/**
 * <p>
 * 门票规格服务接口
 * </p>
 */
public interface ITicketSkuService extends IService<TicketSku> {

    /**
     * 根据门票ID查询规格列表
     * @param ticketId 门票ID
     * @return 规格列表
     */
    List<TicketSku> queryByTicketId(Long ticketId);

    /**
     * 锁定库存（下单时使用）
     * @param skuId 规格ID
     * @param count 数量
     * @return 是否成功
     */
    boolean lockStock(Long skuId, Integer count);

    /**
     * 减少库存并增加销量（支付成功后）
     * @param skuId 规格ID
     * @param count 数量
     * @return 是否成功
     */
    boolean decreaseStock(Long skuId, Integer count);

    /**
     * 增加库存（退款时使用）
     * @param skuId 规格ID
     * @param count 数量
     * @return 是否成功
     */
    boolean increaseStock(Long skuId, Integer count);

    /**
     * 检查库存是否充足
     * @param skuId 规格ID
     * @param count 数量
     * @return 是否充足
     */
    boolean checkStock(Long skuId, Integer count);

}