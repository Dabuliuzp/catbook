package com.example.virtual_campus.config;

import com.example.virtual_campus.Utils.JWTUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT认证过滤器，用于验证请求中的JWT令牌
 * 继承OncePerRequestFilter确保每个请求只通过过滤器一次
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final UserDetailsService userDetailsService;

    // 构造函数，记录过滤器初始化日志
    public JwtAuthenticationFilter(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
        logger.info("JWT认证过滤器已初始化并注入UserDetailsService");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 记录过滤器被触发
        logger.info("JWT过滤器被触发，处理请求: " + request.getRequestURI());
        
        // 从请求头中获取Authorization字段
        String authorizationHeader = request.getHeader("Authorization");
        
        // 记录Authorization头信息（不记录完整令牌内容，保护安全）
        logger.info("接收到的Authorization头: " + (authorizationHeader != null ? "已提供" : "未提供"));
        
        String username = null;
        String jwtToken = null;
        
        // 检查Authorization头是否存在且格式正确
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            // 提取JWT令牌（去掉"Bearer "前缀）
            jwtToken = authorizationHeader.substring(7);
            logger.info("提取到JWT令牌，开始验证");
            
            try {
                // 从令牌中提取用户名
                username = JWTUtils.getUsernameFromToken(jwtToken);
                logger.info("从JWT令牌中提取到用户名: " + username);
            } catch (Exception e) {
                logger.error("无法解析JWT令牌: " + e.getMessage());
            }
        }
        
        // 如果提取到用户名且当前没有已认证的用户
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            logger.info("准备验证用户: " + username + " 的JWT令牌");
            
            // 验证令牌是否有效
            if (JWTUtils.validateToken(jwtToken)) {
                logger.info("JWT令牌验证成功");
                
                try {
                    // 加载用户详情
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    logger.info("成功加载用户详情: " + username);
                    
                    // 创建认证令牌
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    
                    // 设置认证详情
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 将认证信息存储到SecurityContext中
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    
                    logger.info("用户 " + username + " 通过JWT认证成功，认证信息已设置到SecurityContext");
                } catch (Exception e) {
                    logger.error("加载用户详情或设置认证信息失败: " + e.getMessage());
                }
            } else {
                logger.error("JWT令牌无效或已过期");
            }
        }
        
        // 继续过滤链
        logger.info("JWT过滤器处理完成，继续执行过滤链");
        filterChain.doFilter(request, response);
    }
}