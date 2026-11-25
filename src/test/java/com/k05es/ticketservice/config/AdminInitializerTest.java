package com.k05es.ticketservice.config;

import com.k05es.ticketservice.model.AdminUser;
import com.k05es.ticketservice.model.UserRole;
import com.k05es.ticketservice.repository.UserRepository;
import com.k05es.ticketservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    private AdminInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new AdminInitializer(
                userRepository,
                userService,
                "admin",
                "admin@ticketservice.local",
                "admin123"
        );
    }

    @Test
    @DisplayName("Создаёт администратора по умолчанию, если он отсутствует в БД")
    void createsAdminWhenMissing() {
        when(userRepository.existsByUsernameIgnoreCase("admin")).thenReturn(false);

        initializer.run();

        ArgumentCaptor<AdminUser> captor = ArgumentCaptor.forClass(AdminUser.class);
        verify(userService).registerUser(captor.capture());
        assertEquals(UserRole.ADMIN, captor.getValue().getRole());
        assertEquals("admin", captor.getValue().getUsername());
    }

    @Test
    @DisplayName("Пропускает создание, если администратор уже существует")
    void skipsWhenAdminExists() {
        when(userRepository.existsByUsernameIgnoreCase("admin")).thenReturn(true);

        initializer.run();

        verify(userService, never()).registerUser(any());
    }
}
