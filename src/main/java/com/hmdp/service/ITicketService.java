package com.hmdp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Ticket;

import java.util.List;

/**
 * <p>
 * 门票服务接口
 * </p>
 */
public interface ITicketService extends IService<Ticket> {

    /**
     * 核销门票
     * @param code 核销码
     * @param shopId 商铺ID（验证门票是否属于该商铺）
     * @return 核销结果
     */
    Result verifyTicket(String code, Long shopId);

    /**
     * 根据ID获取门票详情（包含商铺信息）
     * @param id 门票ID
     * @return 门票详情
     */
    Ticket getTicketWithShop(Long id);

    /**
     * 根据商铺ID获取门票列表
     * @param shopId 商铺ID
     * @return 门票列表
     */
    List<Ticket> queryTicketsByShopId(Long shopId);

    /**
     * 分页查询门票
     * @param page 页码
     * @param size 每页大小
     * @param shopId 商铺ID，可选
     * @param typeId 门票类型ID，可选
     * @return 分页结果
     */
    Page<Ticket> queryTicketsPage(int page, int size, Long shopId, Integer typeId);

    /**
     * 创建门票
     * @param ticket 门票信息
     * @return 是否成功
     */
    boolean createTicket(Ticket ticket);

    /**
     * 更新门票
     * @param ticket 门票信息
     * @return 是否成功
     */
    boolean updateTicket(Ticket ticket);

    /**
     * 检查门票是否可用（未过期、有效）
     * @param ticketId 门票ID
     * @return 是否可用
     */
    boolean isTicketAvailable(Long ticketId);

    Ticket queryTicketById(Long id);

    int evaluateTicketHeat(int shardIndex, int shardTotal, int batchSize, String periodType, boolean immediateApply);
}