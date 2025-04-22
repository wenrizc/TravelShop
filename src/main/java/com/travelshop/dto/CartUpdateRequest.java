package com.travelshop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 更新购物车请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 购物车项ID
     */
    @NotNull(message = "购物车项ID不能为空")
    private Long itemId;

    /**
     * 数量
     */
    @Min(value = 1, message = "商品数量必须大于0")
    private Integer quantity;

    /**
     * 是否选中
     */
    private Boolean selected;

    /**
     * 会话ID（未登录用户）
     */
    private String sessionId;

    /**
     * 更新类型：1-数量 2-选中状态
     */
    @NotNull(message = "更新类型不能为空")
    private Integer updateType;

    /**
     * 验证请求是否有效
     */
    public boolean isValid() {
        if (updateType == 1 && quantity == null) {
            return false;
        }
        if (updateType == 2 && selected == null) {
            return false;
        }
        return true;
    }
}