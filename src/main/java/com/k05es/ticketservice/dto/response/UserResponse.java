package com.k05es.ticketservice.dto.response;

import com.k05es.ticketservice.model.ContactPreference;
import com.k05es.ticketservice.model.Department;
import com.k05es.ticketservice.model.Permission;
import com.k05es.ticketservice.model.UserRole;
import com.k05es.ticketservice.model.UserStatus;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Данные пользователя для REST-ответа. Хэш пароля не возвращается. */
@Getter
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private UserRole role;
    private UserStatus status;
    private String lastName;
    private String firstName;
    private String middleName;
    private String displayName;
    private String phone;
    private String companyName;
    private String address;
    private ContactPreference preferredContactMethod;
    private Department department;
    private Set<Permission> permissions;
    private Set<Long> responsibleForSpecialists;
    private Double averageRating;
    private Integer completedTicketCount;
    private Integer completedTodayCount;
    private Integer activeTicketCount;
    private Boolean available;
    private Integer createdTicketCount;
}