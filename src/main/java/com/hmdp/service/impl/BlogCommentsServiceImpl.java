package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.BlogComments;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogCommentsMapper;
import com.hmdp.service.IBlogCommentsService;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

    @Resource
    private IUserService userService;

    @Resource
    private IBlogService blogService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result addComment(BlogComments blogComments) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        blogComments.setUserId(user.getId());
        blogComments.setCreateTime(LocalDateTime.now());
        blogComments.setUpdateTime(LocalDateTime.now());
        blogComments.setLiked(0);
        blogComments.setStatus(0);
        boolean success = save(blogComments);
        if (!success) {
            return Result.fail("评论失败");
        }
        blogService.update().setSql("comments = comments + 1").eq("id", blogComments.getBlogId()).update();
        return Result.ok(blogComments.getId());
    }

    @Override
    public Result queryBlogComments(Long blogId) {
        List<BlogComments> commentsList = lambdaQuery().eq(BlogComments::getBlogId, blogId).eq(BlogComments::getStatus, 0).orderByDesc(BlogComments::getCreateTime).list();
        if (commentsList.isEmpty()) {
            return Result.ok(new ArrayList<>());
        }
        List<Long> userIds = commentsList.stream().map(BlogComments::getUserId).collect(Collectors.toList());
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        List<Map<String, Object>> result = new ArrayList<>();
        for (BlogComments comment : commentsList) {
            Map<String, Object> commentMap = new HashMap<>();
            // 评论基本信息
            commentMap.put("id", comment.getId());
            commentMap.put("content", comment.getContent());
            commentMap.put("createTime", comment.getCreateTime());
            commentMap.put("liked", comment.getLiked());
            commentMap.put("parentId", comment.getParentId());
            commentMap.put("answerId", comment.getAnswerId());
            UserDTO curUser = UserHolder.getUser();
            if (curUser != null) {
                String key = RedisConstants.COMMENT_LIKED_KEY + comment.getId();
                Double score = stringRedisTemplate.opsForZSet().score(key, curUser.getId().toString());
                commentMap.put("isLike", score != null);
            } else {
                commentMap.put("isLike", false);
            }
            User user = userMap.get(comment.getUserId());
            if (user != null) {
                commentMap.put("userId", user.getId());
                commentMap.put("nickName", user.getNickName());
                commentMap.put("icon", user.getIcon());
            }
            result.add(commentMap);
        }
        Map<Long, List<Map<String, Object>>> parentMap = new HashMap<>();
        List<Map<String, Object>> topComments = new ArrayList<>();
        for (Map<String, Object> comment : result) {
            long parentId = (long) comment.get("parentId");
            if (parentId == 0) {
                topComments.add(comment);
            } else {
                List<Map<String, Object>> children = parentMap.computeIfAbsent(parentId, k -> new ArrayList<>());
                children.add(comment);
            }
        }
        for (Map<String, Object> comment : topComments) {
            long commentId = (long) comment.get("id");
            List<Map<String, Object>> children = parentMap.get(commentId);
            if (children != null) {
                comment.put("children", children);
            }
        }
        return Result.ok(topComments);
    }

    @Override
    public Result likeComment(Long commentId) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("未登录");
        }
        Long userId = user.getId();
        String key = RedisConstants.COMMENT_LIKED_KEY + commentId;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        BlogComments comment = getById(commentId);
        if (comment == null) {
            return Result.fail("评论不存在");
        }
        if (score == null) {
            // 数据库点赞数+1
            boolean success = update().setSql("liked = liked + 1").eq("id", commentId).update();
            if (success) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        } else {
            boolean success = update().setSql("liked = liked - 1").eq("id", commentId).update();
            if (success) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }
}
