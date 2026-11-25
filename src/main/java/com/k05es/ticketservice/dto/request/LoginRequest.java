package com.k05es.ticketservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Запрос на вход в систему по логину и паролю. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Логин обязателен")
    private String username;

    @NotBlank(message = "Пароль обязателен")
    private String password;
}
