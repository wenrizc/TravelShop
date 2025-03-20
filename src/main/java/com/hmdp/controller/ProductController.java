package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.service.ProductSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Resource
    private ProductSearchService productSearchService;

    /**
     * 搜索商品
     * @param keyword 关键词
     * @param categoryId 分类ID
     * @param page 页码
     * @param size 页大小
     * @return 搜索结果
     */
    @GetMapping("/search")
    public Result search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size
    ) {
        return productSearchService.searchProducts(keyword, categoryId, page, size);
    }
}