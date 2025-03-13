package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.BlogComments;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
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