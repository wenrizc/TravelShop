package com.travelshop.controller.user;


import com.travelshop.dto.Result;
import com.travelshop.entity.BlogComments;
import com.travelshop.service.IBlogCommentsService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 博客评论控制器
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
