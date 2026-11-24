package com.k05es.ticketservice.controller;

import com.k05es.ticketservice.dto.request.LoginRequest;
import com.k05es.ticketservice.dto.request.RegisterRequest;
import com.k05es.ticketservice.dto.response.AuthResponse;
import com.k05es.ticketservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST-контроллер регистрации и входа в систему с выдачей JWT. */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Регистрация клиентов и аутентификация с выдачей JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Регистрация нового клиента (роль CUSTOMER)")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Вход в систему и получение JWT-токена")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
