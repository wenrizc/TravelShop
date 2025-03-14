package com.hmdp.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 商品数据访问接口
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 根据店铺ID查询商品列表
     */
    @Select("SELECT * FROM tb_product WHERE shop_id = #{shopId} AND status = 1 ORDER BY sort DESC")
    List<Product> queryByShopId(@Param("shopId") Long shopId);

    /**
     * 根据分类ID查询商品列表
     */
    @Select("SELECT * FROM tb_product WHERE category_id = #{categoryId} AND status = 1 ORDER BY sort DESC")
    List<Product> queryByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 分页查询商品（支持多条件）
     */
    @Select("<script>" +
            "SELECT * FROM tb_product " +
            "<where>" +
            "   <if test='shopId != null'> AND shop_id = #{shopId} </if>" +
            "   <if test='categoryId != null'> AND category_id = #{categoryId} </if>" +
            "   <if test='status != null'> AND status = #{status} </if>" +
            "   <if test='keyword != null and keyword != \"\"'> AND name LIKE CONCAT('%', #{keyword}, '%') </if>" +
            "</where>" +
            "ORDER BY sort DESC" +
            "</script>")
    IPage<Product> queryProductsByCondition(Page<Product> page,
                                            @Param("shopId") Long shopId,
                                            @Param("categoryId") Long categoryId,
                                            @Param("status") Integer status,
                                            @Param("keyword") String keyword);

    /**
     * 增加商品销量
     */
    @Update("UPDATE tb_product SET sales = sales + #{increment}, update_time = NOW() WHERE id = #{productId}")
    int incrementSales(@Param("productId") Long productId, @Param("increment") Integer increment);

    /**
     * 更新商品状态
     */
    @Update("UPDATE tb_product SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 查询所有商品ID（用于缓存预热/布隆过滤器）
     */
    @Select("SELECT id FROM tb_product")
    List<Long> selectAllIds();

    /**
     * 根据ID列表批量查询商品信息
     */
    @Select("<script>" +
            "SELECT * FROM tb_product " +
            "WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "   #{id}" +
            "</foreach>" +
            " ORDER BY FIELD(id," +
            "<foreach collection='ids' item='id' separator=','>" +
            "   #{id}" +
            "</foreach>" +
            ")" +
            "</script>")
    List<Product> selectBatchByIds(@Param("ids") List<Long> ids);

    /**
     * 查询热门商品
     */
    @Select("SELECT * FROM tb_product WHERE status = 1 ORDER BY sales DESC, create_time DESC LIMIT #{limit}")
    List<Product> selectHotProducts(@Param("limit") Integer limit);

    /**
     * 更新商品评分
     */
    @Update("UPDATE tb_product SET score = #{score}, update_time = NOW() WHERE id = #{id}")
    int updateScore(@Param("id") Long id, @Param("score") Double score);
}