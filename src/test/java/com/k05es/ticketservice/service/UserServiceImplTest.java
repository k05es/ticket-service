package com.k05es.ticketservice.service;

import com.k05es.ticketservice.dto.request.UpdateUserRequest;
import com.k05es.ticketservice.exception.DuplicateResourceException;
import com.k05es.ticketservice.exception.UserHasDependenciesException;
import com.k05es.ticketservice.exception.UserNotFoundException;
import com.k05es.ticketservice.model.AdminUser;
import com.k05es.ticketservice.model.CustomerUser;
import com.k05es.ticketservice.model.Department;
import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.model.UserRole;
import com.k05es.ticketservice.model.UserStatus;
import com.k05es.ticketservice.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserServiceImpl userService;

    private CustomerUser customer;

    @BeforeEach
    void setUp() {
        customer = new CustomerUser(10L, "client", "client@example.test", "hash",
                UserStatus.ACTIVE, "Иванов", "Иван", null,
                "+79001234567", null, null, null, 2);
    }

    @Test
    @DisplayName("Регистрация отклоняет занятый логин с DuplicateResourceException")
    void registerUserRejectsDuplicateUsername() {
        when(userRepository.existsByUsernameIgnoreCase("client")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> userService.registerUser(customer));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Регистрация отклоняет занятый email с DuplicateResourceException")
    void registerUserRejectsDuplicateEmail() {
        when(userRepository.existsByUsernameIgnoreCase("client")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("client@example.test")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> userService.registerUser(customer));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Поиск пользователей передаёт фильтры репозиторию")
    void searchUsersDelegatesFilters() {
        when(userRepository.searchUsers(UserRole.CUSTOMER, UserStatus.ACTIVE, "cli", null))
                .thenReturn(List.of(customer));
        assertEquals(1, userService.searchUsers(UserRole.CUSTOMER, UserStatus.ACTIVE, " cli ", null).size());
    }

    @Test
    @DisplayName("Регистрация администратора с null-списком специалистов не вызывает NPE")
    void registerAdminWithNullResponsibleSpecialists() {
        AdminUser admin = new AdminUser(null, "admin", "admin@example.test", "hash",
                UserStatus.ACTIVE, "Админов", "Админ", null,
                Set.of(), Department.GENERAL_SUPPORT, null);
        when(userRepository.existsByUsernameIgnoreCase("admin")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("admin@example.test")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User registered = userService.registerUser(admin);

        assertEquals(Set.of(), ((AdminUser) registered).getResponsibleForSpecialists());
    }

    @Test
    @DisplayName("Поиск несуществующего пользователя выбрасывает UserNotFoundException")
    void findUserByIdThrowsWhenMissing() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.findUserById(404L));
    }

    @Test
    @DisplayName("Обновление профиля не требует и не меняет хэш пароля")
    void updateUserChangesOnlySuppliedFields() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Пётр");
        when(userRepository.findById(10L)).thenReturn(Optional.<User>of(customer));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateUser(10L, request);

        assertEquals("Пётр", updated.getFirstName());
        assertEquals("hash", updated.getPassHash());
        assertEquals(UserRole.CUSTOMER, updated.getRole());
    }

    @Test
    @DisplayName("Обновление несуществующего пользователя выбрасывает UserNotFoundException")
    void updateUserThrowsWhenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class,
                () -> userService.updateUser(99L, new UpdateUserRequest()));
    }

    @Test
    @DisplayName("Пользователя с отправленными сообщениями нельзя удалить (UserHasDependenciesException)")
    void deleteUserRejectsUserWithMessages() {
        when(userRepository.existsById(10L)).thenReturn(true);
        when(userRepository.isUsedInTickets(10L)).thenReturn(false);
        when(userRepository.hasSentMessages(10L)).thenReturn(true);

        assertThrows(UserHasDependenciesException.class, () -> userService.deleteUser(10L));
        verify(userRepository, never()).deleteById(10L);
    }

    @Test
    @DisplayName("Пользователя, связанного с заявкой, нельзя удалить (UserHasDependenciesException)")
    void deleteUserRejectsUserUsedInTicket() {
        when(userRepository.existsById(10L)).thenReturn(true);
        when(userRepository.isUsedInTickets(10L)).thenReturn(true);

        assertThrows(UserHasDependenciesException.class, () -> userService.deleteUser(10L));
        verify(userRepository, never()).deleteById(10L);
    }

    @Test
    @DisplayName("Удаление несуществующего пользователя возвращает false")
    void deleteUserReturnsFalseWhenMissing() {
        when(userRepository.existsById(77L)).thenReturn(false);
        assertFalse(userService.deleteUser(77L));
    }
}
