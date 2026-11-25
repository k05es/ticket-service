package com.k05es.ticketservice.security;

import com.k05es.ticketservice.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Выпуск и проверка JWT-токенов (HS256) для пользователей ticket-service. */
@Component
public class JwtService {

    private final SecretKey key;
    private final Duration lifetime;

    public JwtService(@Value("${app.security.jwt.secret}") String secret,
                      @Value("${app.security.jwt.expiration-minutes}") long expirationMinutes) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT-секрет должен содержать не менее 32 символов.");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.lifetime = Duration.ofMinutes(expirationMinutes);
    }

    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + lifetime.toMillis()))
                .signWith(key)
                .compact();
    }

    public Optional<String> extractUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public long getExpiresInSeconds() {
        return lifetime.toSeconds();
    }
}
