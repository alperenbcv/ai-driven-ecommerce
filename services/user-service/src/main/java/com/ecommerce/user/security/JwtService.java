package com.ecommerce.user.security;

import com.ecommerce.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT token üretimi ve doğrulaması.
 *
 * Token yapısı (Base64 decode edilince):
 * Header:  { "alg": "HS256" }
 * Payload: { "sub": "42", "email": "test@mail.com", "role": "USER",
 *             "iat": 1714576000, "exp": 1714662400 }
 * Signature: HMAC-SHA256(header + payload, secretKey)
 *
 * secretKey application.yml'den gelir.
 */
@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    public String generateAccessToken(User user) {
        return buildToken(user, expirationMs, "ACCESS");
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, refreshExpirationMs, "REFRESH");
    }

    private String buildToken(User user, long ttl, String type) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        claims.put("type", type);

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(user.getId()))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttl))
                .signWith(getKey())
                .compact();
    }

    public boolean isRefreshToken(String token) {
        try {
            return "REFRESH".equals(extractAllClaims(token).get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /** Sadece access token Bearer ile kimlik oluşturulur refresh token reddedilir. */
    public boolean isAccessToken(String token) {
        try {
            return "ACCESS".equals(extractAllClaims(token).get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractUserId(String token) {
        return Long.parseLong(extractAllClaims(token).getSubject());
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.warn("Geçersiz JWT token: {}", e.getMessage());
            return false;
        }
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
