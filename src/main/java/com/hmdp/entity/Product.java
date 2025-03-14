package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_product")
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 商品封面图
     */
    private String cover;

    /**
     * 商品图片，多个图片以,隔开
     */
    private String images;

    /**
     * 商品价格（取自SKU的最低价）
     */
    private BigDecimal price;

    /**
     * 商品分类ID
     */
    private Long categoryId;

    /**
     * 商品分类名称
     */
    private String categoryName;

    /**
     * 商品所属店铺ID
     */
    private Long shopId;

    /**
     * 销量
     */
    private Integer sales;

    /**
     * 商品评分，1-5分
     */
    private Double score;

    /**
     * 商品状态：1-上架，2-下架
     */
    private Integer status;

    /**
     * 排序值，值越大排越前
     */
    private Integer sort;

    /**
     * 商品规格项，例如"颜色,尺寸"
     */
    private String specs;

    /**
     * 商品标签，多个标签以,隔开
     */
    private String tags;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}