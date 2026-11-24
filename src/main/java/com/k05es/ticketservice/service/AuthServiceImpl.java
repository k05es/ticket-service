package com.k05es.ticketservice.service;

import com.k05es.ticketservice.dto.request.LoginRequest;
import com.k05es.ticketservice.dto.request.RegisterRequest;
import com.k05es.ticketservice.dto.response.AuthResponse;
import com.k05es.ticketservice.exception.InvalidRequestException;
import com.k05es.ticketservice.exception.UnauthorizedActionException;
import com.k05es.ticketservice.model.CustomerUser;
import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.model.UserStatus;
import com.k05es.ticketservice.repository.UserRepository;
import com.k05es.ticketservice.security.JwtService;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Реализация аутентификации и регистрации клиентов с выдачей JWT-токена. */
@Slf4j
@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserService userService,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Тело запроса регистрации обязательно");
        }
        CustomerUser customer = new CustomerUser(
                null,
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                UserStatus.ACTIVE,
                request.getLastName(),
                request.getFirstName(),
                request.getMiddleName(),
                request.getPhone(),
                request.getCompanyName(),
                request.getAddress(),
                request.getPreferredContactMethod(),
                0
        );
        User created = userService.registerUser(customer);
        String token = jwtService.generateToken(created);
        log.info("Зарегистрирован новый клиент '{}' (id={})", created.getUsername(), created.getId());
        return AuthResponse.of(created, token, jwtService.getExpiresInSeconds());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new InvalidRequestException("Логин и пароль обязательны");
        }

        String normalizedUsername = request.getUsername().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByUsernameIgnoreCase(normalizedUsername);
        if (user == null) {
            log.warn("Неудачная попытка входа: пользователь '{}' не найден", normalizedUsername);
            throw new BadCredentialsException("Неверный логин или пароль.");
        }
        if (!user.isActive()) {
            log.warn("Попытка входа заблокированного пользователя '{}'", user.getUsername());
            throw new UnauthorizedActionException("Учётная запись пользователя заблокирована.");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassHash())) {
            log.warn("Неудачная попытка входа: неверный пароль для пользователя '{}'", user.getUsername());
            throw new BadCredentialsException("Неверный логин или пароль.");
        }

        String token = jwtService.generateToken(user);
        log.info("Успешный вход пользователя '{}' (роль: {})", user.getUsername(), user.getRole());
        return AuthResponse.of(user, token, jwtService.getExpiresInSeconds());
    }
}
