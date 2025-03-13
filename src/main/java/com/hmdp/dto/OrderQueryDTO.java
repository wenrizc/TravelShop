package com.hmdp.dto;


import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单查询数据传输对象
 */
@Data
public class OrderQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 订单状态列表
     */
    private List<Integer> statusList;

    /**
     * 单一订单状态
     */
    private Integer status;

    /**
     * 是否包含已删除订单
     */
    private Boolean includeDeleted;

    /**
     * 开始时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 页码，默认第1页
     */
    private Integer pageNum = 1;

    /**
     * 每页大小，默认10条
     */
    private Integer pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方向（asc/desc）
     */
    private String sortDirection;

    /**
     * 商品名称关键词（模糊查询）
     */
    private String productKeyword;

    /**
     * 收货人姓名（模糊查询）
     */
    private String receiverName;

    /**
     * 收货人电话（模糊查询）
     */
    private String receiverPhone;

    /**
     * 最小订单金额
     */
    private Double minAmount;

    /**
     * 最大订单金额
     */
    private Double maxAmount;

    /**
     * 构建方法
     */
    public static OrderQueryDTO builder() {
        return new OrderQueryDTO();
    }

    /**
     * 设置用户ID并返回实例
     */
    public OrderQueryDTO userId(Long userId) {
        this.userId = userId;
        return this;
    }

    /**
     * 设置订单编号并返回实例
     */
    public OrderQueryDTO orderNo(String orderNo) {
        this.orderNo = orderNo;
        return this;
    }

    /**
     * 设置订单状态列表并返回实例
     */
    public OrderQueryDTO statusList(List<Integer> statusList) {
        this.statusList = statusList;
        return this;
    }

    /**
     * 设置单一订单状态并返回实例
     */
    public OrderQueryDTO status(Integer status) {
        this.status = status;
        return this;
    }

    /**
     * 设置是否包含已删除订单并返回实例
     */
    public OrderQueryDTO includeDeleted(Boolean includeDeleted) {
        this.includeDeleted = includeDeleted;
        return this;
    }

    /**
     * 设置开始时间并返回实例
     */
    public OrderQueryDTO startTime(LocalDateTime startTime) {
        this.startTime = startTime;
        return this;
    }

    /**
     * 设置结束时间并返回实例
     */
    public OrderQueryDTO endTime(LocalDateTime endTime) {
        this.endTime = endTime;
        return this;
    }

    /**
     * 设置页码并返回实例
     */
    public OrderQueryDTO pageNum(Integer pageNum) {
        if (pageNum != null && pageNum > 0) {
            this.pageNum = pageNum;
        }
        return this;
    }

    /**
     * 设置每页大小并返回实例
     */
    public OrderQueryDTO pageSize(Integer pageSize) {
        if (pageSize != null && pageSize > 0) {
            this.pageSize = pageSize;
        }
        return this;
    }

    /**
     * 设置排序字段并返回实例
     */
    public OrderQueryDTO sortField(String sortField) {
        this.sortField = sortField;
        return this;
    }

    /**
     * 设置排序方向并返回实例
     */
    public OrderQueryDTO sortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
        return this;
    }

    /**
     * 设置商品关键词并返回实例
     */
    public OrderQueryDTO productKeyword(String productKeyword) {
        this.productKeyword = productKeyword;
        return this;
    }

    /**
     * 设置收货人姓名并返回实例
     */
    public OrderQueryDTO receiverName(String receiverName) {
        this.receiverName = receiverName;
        return this;
    }

    /**
     * 设置收货人电话并返回实例
     */
    public OrderQueryDTO receiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
        return this;
    }

    /**
     * 设置最小订单金额并返回实例
     */
    public OrderQueryDTO minAmount(Double minAmount) {
        this.minAmount = minAmount;
        return this;
    }

    /**
     * 设置最大订单金额并返回实例
     */
    public OrderQueryDTO maxAmount(Double maxAmount) {
        this.maxAmount = maxAmount;
        return this;
    }

    /**
     * 检查排序字段的合法性
     * 防止SQL注入
     */
    public boolean isValidSortField() {
        if (sortField == null || sortField.isEmpty()) {
            return true;
        }

        // 允许的排序字段列表
        List<String> allowedFields = List.of(
                "id", "order_no", "total_amount", "pay_amount",
                "status", "create_time", "pay_time", "delivery_time",
                "receive_time", "finish_time", "update_time"
        );

        return allowedFields.contains(sortField);
    }

    /**
     * 检查排序方向的合法性
     */
    public boolean isValidSortDirection() {
        if (sortDirection == null || sortDirection.isEmpty()) {
            return true;
        }

        return "asc".equalsIgnoreCase(sortDirection) || "desc".equalsIgnoreCase(sortDirection);
    }

    /**
     * 获取排序SQL片段
     */
    public String getSortSql() {
        if (!isValidSortField() || !isValidSortDirection()) {
            return "create_time DESC"; // 默认按创建时间降序
        }

        if (sortField == null || sortField.isEmpty()) {
            return "create_time DESC"; // 默认按创建时间降序
        }

        String direction = (sortDirection == null || sortDirection.isEmpty()) ? "DESC" : sortDirection.toUpperCase();

        return sortField + " " + direction;
    }

    /**
     * 获取查询开始位置
     */
    public int getOffset() {
        return (pageNum - 1) * pageSize;
    }

    /**
     * 验证时间范围是否合法
     */
    public boolean hasValidTimeRange() {
        if (startTime != null && endTime != null) {
            return !startTime.isAfter(endTime);
        }
        return true;
    }

    /**
     * 验证金额范围是否合法
     */
    public boolean hasValidAmountRange() {
        if (minAmount != null && maxAmount != null) {
            return minAmount <= maxAmount;
        }
        return true;
    }

    /**
     * 如果status有值但statusList为空，自动将status加入statusList
     */
    public void normalizeStatusFilter() {
        if (status != null && (statusList == null || statusList.isEmpty())) {
            statusList = List.of(status);
        }
    }
    /**
     * 获取页码
     */
    public Integer getPage() {
        return this.pageNum;
    }

    /**
     * 获取每页大小
     */
    public Integer getSize() {
        return this.pageSize;
    }
}