package com.k05es.ticketservice.security;

import com.k05es.ticketservice.model.AdminUser;
import com.k05es.ticketservice.model.CustomerUser;
import com.k05es.ticketservice.model.Department;
import com.k05es.ticketservice.model.Permission;
import com.k05es.ticketservice.model.UserStatus;
import com.k05es.ticketservice.repository.UserRepository;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TicketUserDetailsService userDetailsService;

    @Test
    @DisplayName("Загружает администратора с authority ROLE_ADMIN")
    void loadsAdminWithRoleAuthority() {
        AdminUser admin = new AdminUser(1L, "admin", "admin@test.ru", "$2a$encoded",
                UserStatus.ACTIVE, "Иванов", "Иван", null,
                Set.of(Permission.MANAGE_USERS), Department.GENERAL_SUPPORT, Set.of());
        when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(admin);

        UserDetails details = userDetailsService.loadUserByUsername("admin");

        assertEquals("admin", details.getUsername());
        assertEquals("$2a$encoded", details.getPassword());
        assertTrue(details.isEnabled());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Заблокированный пользователь помечается как disabled")
    void marksBlockedUserAsDisabled() {
        CustomerUser blocked = new CustomerUser(2L, "client", "c@test.ru", "$2a$encoded",
                UserStatus.BLOCKED, "Петров", "Пётр", null,
                "+79001234567", null, null, null, 0);
        when(userRepository.findByUsernameIgnoreCase("client")).thenReturn(blocked);

        UserDetails details = userDetailsService.loadUserByUsername("client");

        assertFalse(details.isEnabled());
    }

    @Test
    @DisplayName("Выбрасывает UsernameNotFoundException при отсутствии пользователя")
    void throwsWhenUserMissing() {
        when(userRepository.findByUsernameIgnoreCase("ghost")).thenReturn(null);

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("ghost"));
    }
}
