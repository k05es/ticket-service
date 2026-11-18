package com.k05es.ticketservice.dto.request;

import com.k05es.ticketservice.model.ContactPreference;
import com.k05es.ticketservice.model.Department;
import com.k05es.ticketservice.model.Permission;
import com.k05es.ticketservice.model.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Необязательные поля для частичного обновления профиля пользователя. */
@Getter
@Setter
@NoArgsConstructor
public class UpdateUserRequest {
    @Size(min = 3, max = 50)
    private String username;
    @Email
    @Size(max = 120)
    private String email;
    private UserStatus status;
    @Size(max = 80)
    private String lastName;
    @Size(max = 80)
    private String firstName;
    @Size(max = 80)
    private String middleName;
    @Pattern(regexp = "^\\+7\\d{10}$", message = "Телефон должен быть в формате +7XXXXXXXXXX")
    private String phone;
    @Size(max = 120)
    private String companyName;
    @Size(max = 255)
    private String address;
    private ContactPreference preferredContactMethod;
    private Department department;
    private Set<Permission> permissions;
    private Set<Long> responsibleForSpecialists;
    private Boolean available;
}