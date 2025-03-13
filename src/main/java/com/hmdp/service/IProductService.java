package com.hmdp.service;

import java.math.BigDecimal;

public interface IProductService {

    /**
     * 获取商品信息
     * @param productId 商品ID
     * @param skuId SKU ID
     * @return 商品信息
     */
    ProductInfo getProductInfo(Long productId, Long skuId);

    /**
     * 锁定商品库存
     * @param productId 商品ID
     * @param skuId SKU ID
     * @param count 数量
     * @return 是否成功
     */
    boolean lockStock(Long productId, Long skuId, Integer count);

    /**
     * 释放商品库存
     * @param productId 商品ID
     * @param skuId SKU ID
     * @param count 数量
     * @return 是否成功
     */
    boolean unlockStock(Long productId, Long skuId, Integer count);

    /**
     * 商品信息DTO
     */
    class ProductInfo {
        private Long productId;
        private Long skuId;
        private String name;
        private String imageUrl;
        private String skuName;
        private BigDecimal price;
        private Integer stock;
        private Integer productType;
        private boolean onSale = true;

        // Getters and setters
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Long getSkuId() { return skuId; }
        public void setSkuId(Long skuId) { this.skuId = skuId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public String getSkuName() { return skuName; }
        public void setSkuName(String skuName) { this.skuName = skuName; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
        public Integer getProductType() { return productType; }
        public void setProductType(Integer productType) { this.productType = productType; }
        public boolean isOnSale() { return onSale; }
        public void setOnSale(boolean onSale) { this.onSale = onSale; }
    }
}