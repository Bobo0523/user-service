package org.example.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;




public class JwtUtil {

    // 生成安全密钥（实际项目中建议将密钥存放在配置文件中）
    private static final SecretKey SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final long EXPIRATION_TIME = 86400000L; // 1天（毫秒）

    public static String generateToken(String userId, Map<String, Object> claims) {
        return Jwts.builder().claims(claims).subject(userId).issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 设置过期时间
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256) // 设置签名算法与密钥
                .compact(); // 生成JWT字符串
    }
    public static Claims parseToken(String token) {
        try {
            return // 0.12.x 正确语法
                    Jwts.parser().verifyWith(SECRET_KEY).build().parseSignedClaims(token).getPayload();

        } catch (ExpiredJwtException e) {
            System.out.println("Token 已过期");
            // 这里可以根据业务抛出自定义异常
            throw new RuntimeException("Token已过期，请重新登录");
        } catch (JwtException e) {
            System.out.println("Token 签名非法或已被篡改");
            throw new RuntimeException("Token不合法");
        } catch (Exception e) {
            System.out.println("Token 解析未知错误");
            throw new RuntimeException("认证失败");
        }
    }
}
