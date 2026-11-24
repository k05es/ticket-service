package com.k05es.ticketservice.dto.response;

import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.model.UserRole;

/** Ответ при успешной аутентификации или регистрации с JWT-токеном. */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        Long id,
        String username,
        String email,
        UserRole role,
        String displayName
) {
    public static AuthResponse of(User user, String token, long expiresInSeconds) {
        return new AuthResponse(
                token,
                "Bearer",
                expiresInSeconds,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getDisplayName()
        );
    }
}
