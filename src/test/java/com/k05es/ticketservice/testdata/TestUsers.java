package com.k05es.ticketservice.testdata;

import com.k05es.ticketservice.model.AdminUser;
import com.k05es.ticketservice.model.ContactPreference;
import com.k05es.ticketservice.model.CustomerUser;
import com.k05es.ticketservice.model.Department;
import com.k05es.ticketservice.model.SpecialistUser;
import com.k05es.ticketservice.model.UserStatus;

import java.util.HashSet;

/**
 * Готовые тестовые пользователи.
 */
public class TestUsers {

    private TestUsers() {
    }

    public static AdminUser admin(String username) {
        return new AdminUser(
                null, username, username + "@test.ru", "hash",
                UserStatus.ACTIVE,
                "Кузнецов", "Админ", "Петрович",
                new HashSet<>(),
                Department.TECHNICAL_SUPPORT,
                new HashSet<>()
        );
    }

    public static SpecialistUser specialist(String username) {
        return new SpecialistUser(
                null, username, username + "@test.ru", "hash",
                UserStatus.ACTIVE,
                "Смирнов", "Специалист", "Сергеевич",
                0.0, 0, 0, 0, true,
                Department.TECHNICAL_SUPPORT
        );
    }

    public static CustomerUser customer(String username) {
        return new CustomerUser(
                null, username, username + "@test.ru", "hash",
                UserStatus.ACTIVE,
                "Иванов", "Клиент", "Александрович",
                "+79001234567",
                "ООО Тест",
                "Москва",
                ContactPreference.EMAIL,
                0
        );
    }
}