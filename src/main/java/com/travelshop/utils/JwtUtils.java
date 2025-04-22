package com.travelshop.utils;

import com.travelshop.dto.UserDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {

    // JWT密钥，需256位
    private static final String SECRET_KEY = "travel-shop-secret-key-travel-shop-secret-key";

    // 令牌有效期（分钟）
    private static final long TOKEN_VALIDITY = 30;

    // 刷新令牌有效期（天）
    private static final long REFRESH_TOKEN_VALIDITY = 7;

    // 创建密钥
    private static final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));


    /**
     * 获取签名密钥
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成JWT访问令牌
     * @param userDTO 用户DTO对象
     * @return JWT令牌
     */
    public String generateToken(UserDTO userDTO) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", userDTO.getId());
        claims.put("nickName", userDTO.getNickName());
        claims.put("icon", userDTO.getIcon());
        claims.put("role", userDTO.getRole()); // 添加角色信息

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDTO.getId().toString())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + TOKEN_VALIDITY * 60 * 1000))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成JWT刷新令牌
     * @param userDTO 用户DTO对象
     * @return 刷新令牌
     */
    public String generateRefreshToken(UserDTO userDTO) {
        return Jwts.builder()
                .setSubject(userDTO.getId().toString())
                .claim("type", "refresh")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_VALIDITY * 60 * 1000))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析JWT令牌
     * @param token JWT令牌
     * @return JWT声明
     */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从JWT中获取用户ID
     * @param token JWT令牌
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        if (claims != null) {
            return Long.parseLong(claims.getSubject());
        }
        return null;
    }

    /**
     * 验证令牌是否有效
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            // 令牌验证失败
            return false;
        }
    }

    /**
     * 从JWT中提取UserDTO
     * @param token JWT令牌
     * @return 用户DTO对象
     */
    public UserDTO getUserFromToken(String token) {
        Claims claims = parseClaims(token);
        if (claims == null) {
            return null;
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setId(Long.valueOf(claims.getSubject()));
        userDTO.setNickName((String) claims.get("nickName"));
        userDTO.setIcon((String) claims.get("icon"));

        // 获取角色信息
        Object roleObj = claims.get("role");
        if (roleObj != null) {
            if (roleObj instanceof Integer) {
                userDTO.setRole((Integer) roleObj);
            } else if (roleObj instanceof Number) {
                userDTO.setRole(((Number) roleObj).intValue());
            } else if (roleObj instanceof String) {
                try {
                    userDTO.setRole(Integer.parseInt((String) roleObj));
                } catch (NumberFormatException ignored) {}
            }
        }

        return userDTO;
    }

    /**
     * 检查令牌是否过期
     * @param token JWT令牌
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        Claims claims = parseClaims(token);
        if (claims == null) {
            return true;
        }
        return claims.getExpiration().before(new Date());
    }

    /**
     * 获取JWT的过期时间
     * @param token JWT令牌
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = parseClaims(token);
        if (claims == null) {
            return null;
        }
        return claims.getExpiration();
    }

    /**
     * 计算令牌剩余有效期（秒）
     * @param token JWT令牌
     * @return 剩余有效期（秒）
     */
    public long getTokenRemainingTimeInSeconds(String token) {
        Date expiration = getExpirationDateFromToken(token);
        if (expiration == null) {
            return 0;
        }
        long now = System.currentTimeMillis();
        return Math.max(0, (expiration.getTime() - now) / 1000);
    }
}