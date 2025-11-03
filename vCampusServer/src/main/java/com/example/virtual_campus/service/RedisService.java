package com.example.virtual_campus.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis服务类
 * 提供Redis基本操作和JWT令牌黑名单管理功能
 */
@Service
public class RedisService {

    private static final Logger logger = LoggerFactory.getLogger(RedisService.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // JWT黑名单前缀，用于区分不同类型的缓存
    private static final String JWT_BLACKLIST_PREFIX = "jwt:blacklist:";
    
    @Autowired
    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        logger.info("RedisService initialized successfully");
    }
    
    /**
     * 设置键值对，带过期时间
     * @param key 键
     * @param value 值
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     */
    public void set(String key, Object value, long expireTime, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, expireTime, timeUnit);
            logger.debug("Set key: {}, expire: {} {}", key, expireTime, timeUnit.name());
        } catch (Exception e) {
            logger.error("Error setting key: {}", key, e);
            throw e;
        }
    }
    
    /**
     * 获取值
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            logger.debug("Get key: {}, exists: {}", key, value != null);
            return value;
        } catch (Exception e) {
            logger.error("Error getting key: {}", key, e);
            throw e;
        }
    }
    
    /**
     * 删除键
     * @param key 键
     * @return 是否删除成功
     */
    public Boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            logger.debug("Delete key: {}, result: {}", key, result);
            return result;
        } catch (Exception e) {
            logger.error("Error deleting key: {}", key, e);
            throw e;
        }
    }
    
    /**
     * 检查键是否存在
     * @param key 键
     * @return 是否存在
     */
    public Boolean hasKey(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            logger.debug("Check key exists: {}, result: {}", key, result);
            return result;
        } catch (Exception e) {
            logger.error("Error checking key exists: {}", key, e);
            throw e;
        }
    }
    
    /**
     * 将JWT令牌添加到黑名单
     * @param token JWT令牌
     * @param expireTime 过期时间（秒）
     */
    public void addToBlacklist(String token, long expireTime) {
        String blacklistKey = JWT_BLACKLIST_PREFIX + token;
        try {
            // 可以存储任意值，这里用"1"表示令牌在黑名单中
            redisTemplate.opsForValue().set(blacklistKey, "1", expireTime, TimeUnit.SECONDS);
            logger.info("Token added to blacklist, expire in {} seconds", expireTime);
        } catch (Exception e) {
            logger.error("Error adding token to blacklist", e);
            throw e;
        }
    }
    
    /**
     * 检查JWT令牌是否在黑名单中
     * @param token JWT令牌
     * @return 是否在黑名单中
     */
    public Boolean isInBlacklist(String token) {
        String blacklistKey = JWT_BLACKLIST_PREFIX + token;
        try {
            Boolean result = redisTemplate.hasKey(blacklistKey);
            // 安全地截取token的前20个字符用于日志记录
            String tokenPreview = token.length() > 20 ? token.substring(0, 20) + "..." : token;
            logger.debug("Check token in blacklist: {}, result: {}", tokenPreview, result);
            return result != null && result;
        } catch (Exception e) {
            logger.error("Error checking token in blacklist", e);
            throw e;
        }
    }
}