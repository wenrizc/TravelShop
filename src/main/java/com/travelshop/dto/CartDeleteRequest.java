package com.travelshop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

/**
 * 删除购物车请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartDeleteRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 要删除的购物车项ID列表
     */
    @NotEmpty(message = "购物车项ID列表不能为空")
    private List<Long> itemIds;

    /**
     * 会话ID（未登录用户）
     */
    private String sessionId;

    /**
     * 是否清空购物车
     */
    private Boolean clearAll = false;
}