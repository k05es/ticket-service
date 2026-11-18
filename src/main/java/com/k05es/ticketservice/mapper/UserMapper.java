package com.k05es.ticketservice.mapper;

import com.k05es.ticketservice.dto.request.CreateUserRequest;
import com.k05es.ticketservice.dto.response.UserResponse;
import com.k05es.ticketservice.model.AdminUser;
import com.k05es.ticketservice.model.ContactPreference;
import com.k05es.ticketservice.model.Department;
import com.k05es.ticketservice.model.Permission;
import com.k05es.ticketservice.model.UserRole;
import com.k05es.ticketservice.model.CustomerUser;
import com.k05es.ticketservice.model.SpecialistUser;
import com.k05es.ticketservice.model.User;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Преобразует DTO пользователей в модели и обратно. */
@Component
public class UserMapper {

    public User toEntity(CreateUserRequest request) {
        if (request.getRole() == UserRole.CUSTOMER
                && (request.getPhone() == null || request.getPhone().isBlank())) {
            throw new IllegalArgumentException("Для клиента необходимо указать телефон");
        }
        switch (request.getRole()) {
            case ADMIN:
                return new AdminUser(null, request.getUsername(), request.getEmail(), request.getPassHash(),
                        request.getStatus(), request.getLastName(), request.getFirstName(), request.getMiddleName(),
                        request.getPermissions(), request.getDepartment(), request.getResponsibleForSpecialists());
            case SPECIALIST:
                return new SpecialistUser(null, request.getUsername(), request.getEmail(), request.getPassHash(),
                        request.getStatus(), request.getLastName(), request.getFirstName(), request.getMiddleName(),
                        0.0, 0, 0, 0, request.isAvailable(), request.getDepartment());
            case CUSTOMER:
                return new CustomerUser(null, request.getUsername(), request.getEmail(), request.getPassHash(),
                        request.getStatus(), request.getLastName(), request.getFirstName(), request.getMiddleName(),
                        request.getPhone(), request.getCompanyName(), request.getAddress(),
                        request.getPreferredContactMethod(), 0);
            default:
                throw new IllegalArgumentException("Неизвестная роль пользователя");
        }
    }

    public UserResponse toResponse(User user) {
        String phone = null;
        String companyName = null;
        String address = null;
        ContactPreference contact = null;
        Department department = null;
        Set<Permission> permissions = null;
        Set<Long> responsible = null;
        Double averageRating = null;
        Integer completedCount = null;
        Integer completedToday = null;
        Integer activeCount = null;
        Boolean available = null;
        Integer createdCount = null;

        if (user instanceof CustomerUser customer) {
            phone = customer.getPhone();
            companyName = customer.getCompanyName();
            address = customer.getAddress();
            contact = customer.getPreferredContactMethod();
            createdCount = customer.getCreatedTicketCount();
        } else if (user instanceof SpecialistUser specialist) {
            department = specialist.getDepartment();
            averageRating = specialist.getAverageRating();
            completedCount = specialist.getCompletedTicketCount();
            completedToday = specialist.getCompletedTodayCount();
            activeCount = specialist.getActiveTicketCount();
            available = specialist.isAvailable();
        } else if (user instanceof AdminUser admin) {
            department = admin.getDepartment();
            permissions = admin.getPermissions();
            responsible = admin.getResponsibleForSpecialists();
        }

        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
                user.getStatus(), user.getLastName(), user.getFirstName(), user.getMiddleName(),
                user.getDisplayName(), phone, companyName, address, contact, department,
                permissions, responsible, averageRating, completedCount, completedToday,
                activeCount, available, createdCount);
    }
}