package com.k05es.ticketservice.service;

import com.k05es.ticketservice.dto.request.LoginRequest;
import com.k05es.ticketservice.dto.request.RegisterRequest;
import com.k05es.ticketservice.dto.response.AuthResponse;

/** Сервис регистрации и аутентификации пользователей. */
public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
