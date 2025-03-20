package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.Product;
import com.hmdp.entity.es.ProductDocument;
import com.hmdp.repository.ProductRepository;
import com.hmdp.service.IProductService;
import com.hmdp.service.ProductSearchService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UnifiedCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductSearchServiceImpl implements ProductSearchService {

    @Resource
    private ProductRepository productRepository;

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Resource
    private IProductService productService;

    @Resource
    private UnifiedCache unifiedCache;

    @Override
    public Result searchProducts(String keyword, Long categoryId, Integer page, Integer size) {
        try {
            Pageable pageable = PageRequest.of(page, size);

            // 构建查询条件
            Criteria criteria = new Criteria();

            if (keyword != null && !keyword.isEmpty()) {
                // 商品名称或描述包含关键词
                criteria = criteria.or("name").contains(keyword)
                        .or("description").contains(keyword);
            }

            if (categoryId != null && categoryId > 0) {
                criteria = criteria.and("categoryId").is(categoryId);
            }

            // 只查询上架状态的商品(1-正常)
            criteria = criteria.and("status").is(1);

            Query query = new CriteriaQuery(criteria).setPageable(pageable);

            // 执行查询
            SearchHits<ProductDocument> searchHits = elasticsearchRestTemplate.search(query, ProductDocument.class);

            // 处理结果
            List<ProductDocument> products = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            long total = searchHits.getTotalHits();

            return Result.ok(products);
        } catch (Exception e) {
            log.error("查询商品出错", e);
            return Result.fail("查询商品失败");
        }
    }

    @Override
    public void syncProductToES(Long productId) {
        // 从缓存或数据库获取商品
        Product product = unifiedCache.queryWithHeatAware(
                "product", RedisConstants.CACHE_SHOP_KEY, productId,
                Product.class, id -> productService.getById(id), false);

        if (product != null) {
            // 将Product转换为ProductDocument
            ProductDocument document = ProductDocument.fromProduct(product);
            // 保存到ES
            productRepository.save(document);
            log.info("商品 [{}] 已同步到ES", productId);
        } else {
            log.warn("商品 [{}] 不存在，无法同步到ES", productId);
        }
    }

    @Override
    public void deleteProductFromES(Long productId) {
        productRepository.deleteById(productId);
        log.info("商品 [{}] 已从ES中删除", productId);
    }
}