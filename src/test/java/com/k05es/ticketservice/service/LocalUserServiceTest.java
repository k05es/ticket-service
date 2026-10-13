package com.k05es.ticketservice.service;

import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.model.UserRole;
import com.k05es.ticketservice.testdata.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты для LocalUserService.
 */
class LocalUserServiceTest {

    private LocalUserService service;

    @BeforeEach
    void setUp() {
        service = new LocalUserService();
    }

    // Регистрация

    @Test
    @DisplayName("Регистрация присваивает ID")
    void registerUser_shouldAssignId() {
        User registered = service.registerUser(TestUsers.customer("ivan"));

        assertNotNull(registered.getId());
        assertEquals(1L, registered.getId());
    }

    @Test
    @DisplayName("Два пользователя получают разные ID")
    void registerUser_shouldAssignDifferentIds() {
        User first = service.registerUser(TestUsers.customer("ivan"));
        User second = service.registerUser(TestUsers.customer("petr"));

        assertEquals(1L, first.getId());
        assertEquals(2L, second.getId());
    }

    @Test
    @DisplayName("Дубликат логина запрещён")
    void registerUser_shouldThrow_whenUsernameAlreadyExists() {
        service.registerUser(TestUsers.customer("ivan"));
        User duplicate = TestUsers.customer("ivan");
        assertThrows(IllegalArgumentException.class, () -> service.registerUser(duplicate));
    }

    @Test
    @DisplayName("Регистрация null запрещена")
    void registerUser_shouldThrow_whenUserIsNull() {
        assertThrows(IllegalArgumentException.class, () -> service.registerUser(null));
    }

    @Test
    @DisplayName("Пустой логин запрещён")
    void registerUser_shouldThrow_whenUsernameIsBlank() {
        User user = TestUsers.customer("   ");
        assertThrows(IllegalArgumentException.class, () -> service.registerUser(user));
    }

    // Поиск по ID

    @Test
    @DisplayName("Поиск по ID находит пользователя")
    void findUserById_shouldReturnUser() {
        User registered = service.registerUser(TestUsers.customer("ivan"));

        User found = service.findUserById(registered.getId());

        assertNotNull(found);
        assertEquals("ivan", found.getUsername());
    }

    @Test
    @DisplayName("Поиск по неизвестному ID возвращает null")
    void findUserById_shouldReturnNull_whenNotFound() {
        assertNull(service.findUserById(999L));
        assertNull(service.findUserById(null));
    }

    // Поиск по логину

    @Test
    @DisplayName("Поиск по логину не зависит от регистра")
    void findUserByUsername_shouldBeCaseInsensitive() {
        service.registerUser(TestUsers.customer("ivan"));

        User found = service.findUserByUsername("IVAN");

        assertNotNull(found);
        assertEquals("ivan", found.getUsername());
    }

    @Test
    @DisplayName("Поиск по неизвестному логину возвращает null")
    void findUserByUsername_shouldReturnNull_whenNotFound() {
        assertNull(service.findUserByUsername("unknown"));
    }

    // Поиск по email

    @Test
    @DisplayName("Поиск по email находит пользователя")
    void findUserByEmail_shouldReturnUser() {
        service.registerUser(TestUsers.customer("ivan"));

        User found = service.findUserByEmail("ivan@test.ru");

        assertNotNull(found);
        assertEquals("ivan", found.getUsername());
    }

    @Test
    @DisplayName("Поиск по email не зависит от регистра")
    void findUserByEmail_shouldBeCaseInsensitive() {
        service.registerUser(TestUsers.customer("ivan"));

        User found = service.findUserByEmail("IVAN@TEST.RU");

        assertNotNull(found);
        assertEquals("ivan", found.getUsername());
    }

    // Фильтр по роли

    @Test
    @DisplayName("Фильтр по роли возвращает только нужных")
    void findUsersByRole_shouldReturnOnlyMatchingRole() {
        service.registerUser(TestUsers.customer("client1"));
        service.registerUser(TestUsers.customer("client2"));
        service.registerUser(TestUsers.specialist("spec1"));

        List<User> customers = service.findUsersByRole(UserRole.CUSTOMER);
        List<User> specialists = service.findUsersByRole(UserRole.SPECIALIST);

        assertEquals(2, customers.size());
        assertEquals(1, specialists.size());
    }

    // getAllUsers

    @Test
    @DisplayName("getAllUsers возвращает копию")
    void getAllUsers_shouldReturnCopy() {
        service.registerUser(TestUsers.customer("ivan"));
        service.registerUser(TestUsers.customer("petr"));

        List<User> firstCall = service.getAllUsers();
        firstCall.clear();

        List<User> secondCall = service.getAllUsers();
        assertEquals(2, secondCall.size());
    }

    // Удаление

    @Test
    @DisplayName("Удаление существующего возвращает true")
    void deleteUser_shouldReturnTrue() {
        User registered = service.registerUser(TestUsers.customer("ivan"));

        boolean result = service.deleteUser(registered.getId());

        assertTrue(result);
        assertNull(service.findUserById(registered.getId()));
    }

    @Test
    @DisplayName("Удаление неизвестного возвращает false")
    void deleteUser_shouldReturnFalse_whenNotFound() {
        assertFalse(service.deleteUser(999L));
    }

    @Test
    @DisplayName("После удаления логин можно занять снова")
    void deleteUser_shouldFreeUsername() {
        User registered = service.registerUser(TestUsers.customer("ivan"));
        service.deleteUser(registered.getId());

        User newUser = service.registerUser(TestUsers.customer("ivan"));

        assertNotNull(newUser.getId());
    }
}