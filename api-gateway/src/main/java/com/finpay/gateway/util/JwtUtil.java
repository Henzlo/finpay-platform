package com.finpay.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey signingKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(normalizeSecret(secret));
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return !isTokenExpired(token);
        } catch (Exception ex) {
            return false;
        }
    }

    public String extractEmail(String token) {
        Claims claims = parseClaims(token);
        String email = claims.get("email", String.class);
        if (email != null && !email.isBlank()) {
            return email;
        }
        return claims.getSubject();
    }

    public String extractRole(String token) {
        Claims claims = parseClaims(token);
        String role = claims.get("role", String.class);
        if (role != null) {
            return role;
        }
        Object roles = claims.get("roles");
        return roles != null ? roles.toString() : "";
    }

    public String extractUserId(String token) {
        Claims claims = parseClaims(token);
        String userId = claims.get("userId", String.class);
        if (userId != null && !userId.isBlank()) {
            return userId;
        }
        Object id = claims.get("id");
        if (id != null) {
            return id.toString();
        }
        return claims.getSubject();
    }

    public boolean isTokenExpired(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return expiration != null && expiration.before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * jjwt 0.11.5 requires an HMAC key of at least 256 bits. Short configured
     * secrets (including the local default) are stretched via SHA-256.
     */
    private static byte[] normalizeSecret(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length >= 32) {
            return keyBytes;
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
