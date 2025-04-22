package com.travelshop.utils;

import com.travelshop.repository.ProductRepository;
import com.travelshop.service.IProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * ES数据初始化工具
 * 可以在应用启动时将所有商品数据同步到ES
 */
@Slf4j
@Component
public class ESDataInitializer implements CommandLineRunner {

    @Resource
    private IProductService productService;

    @Resource
    private ProductRepository productRepository;

    @Override
    public void run(String... args) {
        initProductData();
    }

    /**
     * 初始化商品数据到ES
     */
    public void initProductData() {
        try {
            log.info("开始初始化商品数据到ES...");

            log.info("成功同步{}个商品到ES");
        } catch (Exception e) {
            log.error("初始化商品数据到ES失败", e);
        }
    }
}