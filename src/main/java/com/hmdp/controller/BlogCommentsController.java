package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.entity.BlogComments;
import com.hmdp.service.IBlogCommentsService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/blog-comments")
public class BlogCommentsController {

    @Resource
    private IBlogCommentsService blogCommentsService;

    /**
     * 添加评论
     */
    @PostMapping
    public Result addComment(@RequestBody BlogComments blogComments) {
        return blogCommentsService.addComment(blogComments);
    }

    /**
     * 查询博客评论列表
     */
    @GetMapping("/{blogId}")
    public Result queryBlogComments(@PathVariable("blogId") Long blogId) {
        return blogCommentsService.queryBlogComments(blogId);
    }

    /**
     * 点赞评论
     */
    @PutMapping("/like/{id}")
    public Result likeComment(@PathVariable("id") Long commentId) {
        return blogCommentsService.likeComment(commentId);
    }
}
