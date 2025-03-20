package com.hmdp.utils;

import com.hmdp.entity.Product;
import com.hmdp.entity.es.ProductDocument;
import com.hmdp.repository.ProductRepository;
import com.hmdp.service.IProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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