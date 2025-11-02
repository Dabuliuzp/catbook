package com.example.virtual_campus.Utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JWTUtils类的测试类
 */
public class JWTUtilsTest {

    @Test
    public void testGenerateAndValidateToken() {
        // 测试数据
        String username = "testuser";
        Long userId = 1L;
        Integer userType = 0;

        // 生成令牌
        String token = JWTUtils.generateToken(username, userId, userType);
        assertNotNull(token, "生成的令牌不应为空");
        System.out.println("生成的令牌: " + token);

        // 验证令牌
        boolean isValid = JWTUtils.validateToken(token);
        assertTrue(isValid, "生成的令牌应有效");

        // 从令牌中提取信息
        String extractedUsername = JWTUtils.getUsernameFromToken(token);
        Long extractedUserId = JWTUtils.getUserIdFromToken(token);
        Integer extractedUserType = JWTUtils.getUserTypeFromToken(token);

        // 验证提取的信息是否正确
        assertEquals(username, extractedUsername, "用户名应与生成时一致");
        assertEquals(userId, extractedUserId, "用户ID应与生成时一致");
        assertEquals(userType, extractedUserType, "用户类型应与生成时一致");
    }

    @Test
    public void testInvalidToken() {
        // 测试无效令牌
        String invalidToken = "invalid.token.here";
        boolean isValid = JWTUtils.validateToken(invalidToken);
        assertFalse(isValid, "无效令牌应验证失败");

        // 测试损坏的令牌
        String brokenToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ";
        boolean isBrokenValid = JWTUtils.validateToken(brokenToken);
        assertFalse(isBrokenValid, "损坏的令牌应验证失败");

        // 测试空令牌
        String nullToken = null;
        boolean isNullValid = JWTUtils.validateToken(nullToken);
        assertFalse(isNullValid, "空令牌应验证失败");
    }

    @Test
    public void testExtractFromInvalidToken() {
        // 从无效令牌中提取信息应返回null
        String invalidToken = "invalid.token.here";
        assertNull(JWTUtils.getUsernameFromToken(invalidToken), "从无效令牌提取用户名应返回null");
        assertNull(JWTUtils.getUserIdFromToken(invalidToken), "从无效令牌提取用户ID应返回null");
        assertNull(JWTUtils.getUserTypeFromToken(invalidToken), "从无效令牌提取用户类型应返回null");
    }
}