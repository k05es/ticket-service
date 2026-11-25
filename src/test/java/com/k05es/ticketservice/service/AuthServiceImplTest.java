package com.k05es.ticketservice.service;

import com.k05es.ticketservice.dto.request.LoginRequest;
import com.k05es.ticketservice.dto.request.RegisterRequest;
import com.k05es.ticketservice.dto.response.AuthResponse;
import com.k05es.ticketservice.exception.UnauthorizedActionException;
import com.k05es.ticketservice.model.CustomerUser;
import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.model.UserRole;
import com.k05es.ticketservice.model.UserStatus;
import com.k05es.ticketservice.repository.UserRepository;
import com.k05es.ticketservice.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("Регистрация создаёт клиента (CUSTOMER) и возвращает JWT-токен")
    void registerCreatesCustomerAndReturnsToken() {
        when(userService.registerUser(any(User.class))).thenAnswer(invocation -> {
            CustomerUser created = invocation.getArgument(0);
            created.setId(15L);
            return created;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(jwtService.getExpiresInSeconds()).thenReturn(7200L);

        RegisterRequest request = new RegisterRequest(
                "ivan", "ivan@test.ru", "secret123", "+79001234567",
                "Иванов", "Иван", null, null, null, null
        );
        AuthResponse response = authService.register(request);

        assertEquals(15L, response.id());
        assertEquals("ivan", response.username());
        assertEquals(UserRole.CUSTOMER, response.role());
        assertEquals("jwt-token", response.token());
    }

    @Test
    @DisplayName("Успешный вход возвращает JWT-токен")
    void loginReturnsTokenWhenCredentialsValid() {
        CustomerUser customer = new CustomerUser(
                1L, "ivan", "ivan@test.ru", "$2a$hash", UserStatus.ACTIVE,
                "Иванов", "Иван", null, "+79001234567", null, null, null, 0
        );
        when(userRepository.findByUsernameIgnoreCase("ivan")).thenReturn(customer);
        when(passwordEncoder.matches("secret123", "$2a$hash")).thenReturn(true);
        when(jwtService.generateToken(customer)).thenReturn("valid-jwt");
        when(jwtService.getExpiresInSeconds()).thenReturn(7200L);

        AuthResponse response = authService.login(new LoginRequest("ivan", "secret123"));

        assertEquals("valid-jwt", response.token());
        assertEquals(UserRole.CUSTOMER, response.role());
    }

    @Test
    @DisplayName("Вход с неверным паролем отклоняется с BadCredentialsException")
    void loginRejectsWrongPassword() {
        CustomerUser customer = new CustomerUser(
                1L, "ivan", "ivan@test.ru", "$2a$hash", UserStatus.ACTIVE,
                "Иванов", "Иван", null, "+79001234567", null, null, null, 0
        );
        when(userRepository.findByUsernameIgnoreCase("ivan")).thenReturn(customer);
        when(passwordEncoder.matches("wrong", "$2a$hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("ivan", "wrong")));
    }

    @Test
    @DisplayName("Заблокированный пользователь не может войти (UnauthorizedActionException)")
    void loginRejectsBlockedUser() {
        CustomerUser blocked = new CustomerUser(
                1L, "ivan", "ivan@test.ru", "$2a$hash", UserStatus.BLOCKED,
                "Иванов", "Иван", null, "+79001234567", null, null, null, 0
        );
        when(userRepository.findByUsernameIgnoreCase("ivan")).thenReturn(blocked);

        assertThrows(UnauthorizedActionException.class,
                () -> authService.login(new LoginRequest("ivan", "secret123")));
    }
}
