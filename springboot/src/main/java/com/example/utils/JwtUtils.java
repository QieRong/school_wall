package com.example.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtils {

    // 【安全增强】强制从配置文件读取密钥，不提供默认值
    @Value("${jwt.secret}")
    private String secret;

    // 【安全优化】过期时间也可配置，默认7天
    @Value("${jwt.expiration:604800000}")
    private long expiration;
    
    /**
     * 初始化时验证JWT配置
     */
    @PostConstruct
    public void init() {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("JWT密钥未配置！请在application.yml中设置jwt.secret或通过环境变量JWT_SECRET设置");
        }
        
        // 检查是否使用了不安全的默认密钥
        if (secret.equals("TXY_CONFESSION_WALL_SECRET_KEY_2025")) {
            throw new IllegalStateException("检测到使用默认JWT密钥！这是不安全的，请更换为强密钥");
        }
        
        // 检查密钥长度
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT密钥长度不足32位！当前长度: " + secret.length());
        }
    }

    /**
     * 生成 Token
     */
    public String createToken(Long userId, String account) {
        return JWT.create()
                .withAudience(userId.toString()) // 将 UserID 存入 token
                .withClaim("account", account)
                .withExpiresAt(new Date(System.currentTimeMillis() + expiration))
                .sign(Algorithm.HMAC256(secret));
    }

    /**
     * 验证 Token 并获取 UserId
     * 
     * @return userId 如果验证失败返回 null
     */
    public Long verifyToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret)).build();
            DecodedJWT jwt = verifier.verify(token);
            return Long.valueOf(jwt.getAudience().get(0));
        } catch (JWTVerificationException | NumberFormatException e) {
            return null; // 验证失败
        }
    }
}