package com.example.campus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    // String 操作
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // List 操作
    public List<String> lrange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    // Hash 操作
    public void hset(String key, String field, String value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    public String hget(String key, String field) {
        Object val = redisTemplate.opsForHash().get(key, field);
        return val == null ? null : val.toString();
    }

    public Map<Object, Object> hgetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    public Set<String> hkeys(String key) {
        Set<Object> fields = redisTemplate.opsForHash().keys(key);
        if (fields == null) {
            return new HashSet<>();
        }
        // 将 Object 转换为 String，同时过滤可能的 null
        return fields.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    // 其他操作可按需添加
}
