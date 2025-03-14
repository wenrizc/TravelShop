package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 门票基本信息
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_ticket")
public class Ticket implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 门票名称
     */
    private String name;

    /**
     * 所属商铺ID
     */
    private Long shopId;

    /**
     * 门票类型ID
     */
    private Integer typeId;

    /**
     * 门票描述
     */
    private String description;

    /**
     * 门票图片
     */
    private String images;

    /**
     * 预订须知
     */
    private String notice;

    /**
     * 使用地点
     */
    private String address;

    /**
     * 是否限时
     */
    private Boolean isTimeLimited;

    /**
     * 有效天数
     */
    private Integer effectiveDays;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 商铺名称
     */
    private String shopName;

    /**
     * 销量
     */
    private Integer saleCount;

    /**
     * 门票规格
     */
    private List<TicketSku> skus;

}