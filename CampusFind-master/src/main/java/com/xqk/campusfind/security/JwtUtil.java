package com.xqk.campusfind.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT 工具类：负责令牌的签发、解析与用户信息提取。
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    // 过期时间（毫秒）
    @Value("${jwt.expiration:86400000}")
    private long expiration;

    private Key key;

    @PostConstruct
    public void init() {
        // HS256 要求密钥长度 >= 256 位
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 生成 token，携带用户核心信息（id / username / role） */
    public String generateToken(LoginUser user) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .setIssuedAt(now)
                .setExpiration(expireAt)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 解析并校验 token，非法 / 过期会抛出异常 */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** 从 claims 还原登录用户 */
    public LoginUser toLoginUser(Claims claims) {
        LoginUser lu = new LoginUser();
        lu.setUsername(claims.getSubject());
        Number userId = claims.get("userId", Number.class);
        lu.setId(userId != null ? userId.longValue() : null);
        Number role = claims.get("role", Number.class);
        lu.setRole(role != null ? role.intValue() : null);
        return lu;
    }
}
