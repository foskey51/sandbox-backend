package com.example.sandbox_backend.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final long accessMillis;
    private final long refreshMillis;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-minutes}") long accessMinutes,
            @Value("${jwt.refresh-token-days}") long refreshDays
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessMillis = accessMinutes * 60 * 1000;
        this.refreshMillis = refreshDays * 24 * 60 * 60 * 1000;
    }

    public String generateAccessToken(String userId, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(userId)
                .claim("roles", roles)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(accessMillis)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String userId, List<String> roles){
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(userId)
                .claim("roles", roles)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(refreshMillis)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public UUID getUserId(String token) {
        String subject = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();

        return UUID.fromString(subject);
    }


    public long getAccessExpirySeconds() {
        return accessMillis / 1000;
    }

    public long getRefreshExpiryMillis() {
        return refreshMillis;
    }
}
