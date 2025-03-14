package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

/**
 * 购物车合并请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartMergeRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 临时购物车的会话ID
     */
    @NotEmpty(message = "会话ID不能为空")
    private String sessionId;

    /**
     * 合并策略：
     * 1-数量累加（默认）
     * 2-取临时购物车商品数量
     * 3-取用户购物车商品数量
     * 4-取两者较大值
     */
    private Integer mergeStrategy = 1;

    /**
     * 合并后是否清空临时购物车
     */
    private Boolean clearTempCart = true;
}