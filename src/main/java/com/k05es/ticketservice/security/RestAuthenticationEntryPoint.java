package com.k05es.ticketservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** Возвращает HTTP 401 в формате ApiError при отсутствии или некорректности аутентификации. */
@Slf4j
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        log.warn("401 Unauthorized: {} {} ({})", request.getMethod(), request.getRequestURI(), exception.getMessage());
        JsonErrorWriter.write(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "Требуется аутентификация: передайте валидный JWT-токен или учётные данные Basic Auth.",
                "Выполните вход через POST /api/auth/login и передайте токен в заголовке Authorization: Bearer <token>."
        );
    }
}
