package com.k05es.ticketservice.security;

import com.k05es.ticketservice.model.Department;
import com.k05es.ticketservice.model.SpecialistUser;
import com.k05es.ticketservice.model.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "ticket-service-unit-test-secret-key-32-chars";
    private final SpecialistUser specialist = new SpecialistUser(
            2L, "spec1", "spec1@test.ru", "hash", UserStatus.ACTIVE,
            "Петров", "Пётр", null, 0.0, 0, 0, 0, true, Department.TECHNICAL_SUPPORT
    );

    @Test
    @DisplayName("Выпускает и корректно проверяет валидный JWT")
    void generatesAndParsesValidToken() {
        JwtService jwtService = new JwtService(SECRET, 60);

        String token = jwtService.generateToken(specialist);

        assertEquals("spec1", jwtService.extractUsername(token).orElseThrow());
    }

    @Test
    @DisplayName("Отклоняет подделанный или просроченный токен")
    void rejectsTamperedOrExpiredToken() {
        JwtService validService = new JwtService(SECRET, 60);
        String token = validService.generateToken(specialist);
        assertTrue(validService.extractUsername(token + "x").isEmpty());

        JwtService expiredService = new JwtService(SECRET, -5);
        assertTrue(expiredService.extractUsername(expiredService.generateToken(specialist)).isEmpty());
    }

    @Test
    @DisplayName("Отклоняет короткий секретный ключ")
    void rejectsShortSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtService("short", 60));
    }
}
