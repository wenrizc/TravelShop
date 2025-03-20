package com.hmdp.repository;

import com.hmdp.entity.es.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends ElasticsearchRepository<ProductDocument, Long> {
    // 根据商品名称或描述模糊查询
    List<ProductDocument> findByNameContainingOrDescriptionContaining(String name, String description);

    // 根据分类ID和关键词搜索
    List<ProductDocument> findByCategoryIdAndNameContaining(Long categoryId, String keyword);
}