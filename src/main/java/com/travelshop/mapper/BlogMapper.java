package com.travelshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelshop.entity.Blog;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface BlogMapper extends BaseMapper<Blog> {
    @Select("SELECT id FROM tb_blog")
    List<Long> selectAllIds();
}
