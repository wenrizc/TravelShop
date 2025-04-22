package com.travelshop.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class DistributedLock {
    private final StringRedisTemplate stringRedisTemplate;
    private static final String ID_PREFIX = UUID.randomUUID().toString() + "-";

    public DistributedLock(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 尝试获取锁
     * @param key 锁的key
     * @param timeoutSec 锁的超时时间
     * @return 是否成功获取锁
     */
    public boolean tryLock(String key, long timeoutSec) {
        // 生成线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, threadId, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放锁
     * @param key 锁的key
     */
    public void unlock(String key) {
        // 获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁中的标识
        String id = stringRedisTemplate.opsForValue().get(key);
        // 判断是否是自己的锁
        if (threadId.equals(id)) {
            stringRedisTemplate.delete(key);
        }
    }
}