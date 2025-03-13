package com.hmdp.utils;


import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class CacheMessage {

    private static final String CACHE_TOPIC = "cache:changes";

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisMessageListenerContainer listenerContainer;

    public CacheMessage(StringRedisTemplate stringRedisTemplate,
                               RedisMessageListenerContainer listenerContainer) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.listenerContainer = listenerContainer;
    }

    public void publishCacheChange(String operation, String key) {
        try {
            Map<String, String> message = new HashMap<>();
            message.put("operation", operation);
            message.put("key", key);
            message.put("timestamp", String.valueOf(System.currentTimeMillis()));
            stringRedisTemplate.convertAndSend(CACHE_TOPIC, JSONUtil.toJsonStr(message));
            log.debug("发布缓存变更消息：{}", message);
        } catch (Exception e) {
            log.error("发布缓存变更消息失败", e);
        }
    }

    public void subscribeToChanges(Consumer<Map<String, String>> handler) {
        listenerContainer.addMessageListener((message, pattern) -> {
              try {
                  String msg = new String(message.getBody());
                    Map<String, String> map = JSONUtil.toBean(msg, Map.class);
                    handler.accept(map);
              } catch (Exception e) {
                  log.error("处理缓存变更消息失败", e);
              }
        }, new ChannelTopic(CACHE_TOPIC));
        log.info("订阅缓存变更消息成功");
    }
}
