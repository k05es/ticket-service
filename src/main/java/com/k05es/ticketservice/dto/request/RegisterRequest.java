package com.k05es.ticketservice.dto.request;

import com.k05es.ticketservice.model.ContactPreference;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Запрос на самостоятельную регистрацию клиента (роль CUSTOMER). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Логин обязателен")
    @Size(min = 3, max = 50, message = "Логин должен содержать от 3 до 50 символов")
    private String username;

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    @Size(max = 120)
    private String email;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 4, max = 72, message = "Пароль должен содержать от 4 до 72 символов")
    private String password;

    @NotBlank(message = "Телефон обязателен")
    @Pattern(regexp = "^\\+7\\d{10}$", message = "Телефон должен быть в формате +7XXXXXXXXXX")
    private String phone;

    @Size(max = 80)
    private String lastName;

    @Size(max = 80)
    private String firstName;

    @Size(max = 80)
    private String middleName;

    @Size(max = 120)
    private String companyName;

    @Size(max = 255)
    private String address;

    private ContactPreference preferredContactMethod;
}
