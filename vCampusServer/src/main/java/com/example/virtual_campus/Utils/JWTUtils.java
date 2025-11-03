package com.example.virtual_campus.Utils;

import com.example.virtual_campus.service.RedisService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类，用于生成和验证JWT令牌
 */
@Component
public class JWTUtils {
    // 密钥
    private static final String SECRET_KEY_STRING = "vCampusSecretKey1234567890abcdefghijklmnopqrstuvwxyz";
    // 过期时间：24小时
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000;
    // 密钥实例
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes());
    
    // Redis服务静态实例，用于黑名单管理
    private static RedisService redisService;
    
    /**
     * 通过setter方法注入RedisService，支持静态方法调用
     */
    @Autowired
    public void setRedisService(RedisService redisService) {
        JWTUtils.redisService = redisService;
    }

    /**
     * 生成JWT令牌
     * @param username 用户名
     * @param userId 用户ID
     * @param userType 用户类型
     * @return JWT令牌
     */
    public static String generateToken(String username, Long userId, Integer userType) {
        // 测试Redis连接
        testRedisConnection();
        
        // 设置JWT的声明
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("userId", userId);
        claims.put("userType", userType);
        
        // 创建JWT令牌
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * 测试Redis连接是否正常
     * 通过设置和获取一个短暂的测试键值对来验证
     */
    private static void testRedisConnection() {
        if (redisService == null) {
            System.out.println("Redis服务未初始化，无法测试连接");
            return;
        }
        
        String testKey = "jwt:test:connection";
        String testValue = "connection_test";
        
        try {
            // 设置一个短暂的测试键值对（10秒过期）
            redisService.set(testKey, testValue, 10, java.util.concurrent.TimeUnit.SECONDS);
            System.out.println("Redis连接测试：设置测试键成功");
            
            // 尝试获取刚才设置的键值对
            Object retrievedValue = redisService.get(testKey);
            if (retrievedValue != null && retrievedValue.equals(testValue)) {
                System.out.println("Redis连接测试：获取测试键成功，Redis连接正常");
            } else {
                System.out.println("Redis连接测试：获取测试键失败，Redis可能存在问题");
            }
        } catch (Exception e) {
            System.out.println("Redis连接测试失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 验证JWT令牌
     * @param token JWT令牌
     * @return 是否有效
     */
    public static boolean validateToken(String token) {
        try {
            // 首先检查令牌是否在黑名单中
            if (redisService != null && redisService.isInBlacklist(token)) {
                return false;
            }
            
            Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token);
            return true;
        } catch (SignatureException | MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * 将JWT令牌加入黑名单，使其失效
     * @param token JWT令牌
     */
    public static void invalidateToken(String token) {
        if (redisService == null) {
            return; // 如果Redis服务未注入，直接返回
        }
        
        try {
            // 获取令牌的剩余过期时间
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            Date expirationDate = claims.getExpiration();
            long remainingTimeInSeconds = (expirationDate.getTime() - System.currentTimeMillis()) / 1000;
            
            // 确保时间为正数
            if (remainingTimeInSeconds > 0) {
                redisService.addToBlacklist(token, remainingTimeInSeconds);
            } else {
                // 如果令牌已过期，添加一个较短的过期时间到黑名单
                redisService.addToBlacklist(token, 3600); // 1小时
            }
        } catch (ExpiredJwtException e) {
            // 即使令牌已过期，也加入黑名单一段时间
            redisService.addToBlacklist(token, 3600); // 1小时
        } catch (Exception e) {
            // 忽略其他异常
        }
    }

    /**
     * 从令牌中获取用户名
     * @param token JWT令牌
     * @return 用户名
     */
    public static String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("username", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从令牌中获取用户ID
     * @param token JWT令牌
     * @return 用户ID
     */
    public static Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("userId", Long.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从令牌中获取用户类型
     * @param token JWT令牌
     * @return 用户类型
     */
    public static Integer getUserTypeFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("userType", Integer.class);
        } catch (Exception e) {
            return null;
        }
    }
}