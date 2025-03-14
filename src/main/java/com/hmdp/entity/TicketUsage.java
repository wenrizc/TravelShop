package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 门票使用记录
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_ticket_usage")
public class TicketUsage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单项ID
     */
    private Long orderItemId;

    /**
     * 门票ID
     */
    private Long ticketId;

    /**
     * 门票规格ID
     */
    private Long ticketSkuId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 核销码
     */
    private String code;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 使用时间
     */
    private LocalDateTime useTime;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}