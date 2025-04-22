package com.travelshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travelshop.dto.Result;
import com.travelshop.entity.BlogComments;

/**
 * 博客评论服务接口
 */
public interface IBlogCommentsService extends IService<BlogComments> {
    /**
     * 添加评论
     * @param blogComments 评论内容
     * @return 操作结果
     */
    Result addComment(BlogComments blogComments);

    /**
     * 查询博客评论列表
     * @param blogId 博客ID
     * @return 评论列表
     */
    Result queryBlogComments(Long blogId);

    /**
     * 点赞评论
     * @param commentId 评论ID
     * @return 操作结果
     */
    Result likeComment(Long commentId);
}