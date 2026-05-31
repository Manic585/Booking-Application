package com.bookingsystem.user.security;

import com.bookingsystem.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiry-ms:900000}") // 15 min
    private long accessTokenExpiryMs;

    @Value("${jwt.refresh-token-expiry-ms:604800000}") // 7 days
    private long refreshTokenExpiryMs;

    public String generateAccessToken(User user) {
        return buildToken(user.getId().toString(), user.getRole().name(), accessTokenExpiryMs);
    }

    public String generateRefreshToken(User user) {
        return buildToken(user.getId().toString(), user.getRole().name(), refreshTokenExpiryMs);
    }

    private String buildToken(String subject, String role, long expiryMs) {
        SecretKey key = getSigningKey();
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiryMs)))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseToken(token).getSubject());
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            log.debug("Invalid token: {}", e.getMessage());
            return false;
        }
    }

    public long getAccessTokenExpiryMs() {
        return accessTokenExpiryMs;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
