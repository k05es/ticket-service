package com.k05es.ticketservice;

import com.k05es.ticketservice.dto.request.CreateUserRequest;
import com.k05es.ticketservice.mapper.UserMapper;
import com.k05es.ticketservice.model.CustomerUser;
import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.model.UserRole;
import com.k05es.ticketservice.model.UserStatus;
import com.k05es.ticketservice.model.Ticket;
import com.k05es.ticketservice.model.TicketStatus;
import com.k05es.ticketservice.repository.TicketRepository;
import com.k05es.ticketservice.repository.UserRepository;
import com.k05es.ticketservice.service.UserService;
import com.k05es.ticketservice.service.TicketService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Тесты Spring-приложения без подключения к реальной базе данных. */
@SpringBootTest(properties = {
        "app.security.admin.init=false",
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
class TicketServiceApplicationTests {

    @Autowired
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TicketRepository ticketRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TicketService ticketService;

    @TestConfiguration
    static class NoDatabaseTransactionConfiguration {

        // Имитирует границы транзакций, не обращаясь к базе данных.
        @Bean
        PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override
                protected Object doGetTransaction() {
                    return new Object();
                }

                @Override
                protected void doBegin(Object transaction, TransactionDefinition definition) {
                }

                @Override
                protected void doCommit(DefaultTransactionStatus status) {
                }

                @Override
                protected void doRollback(DefaultTransactionStatus status) {
                }
            };
        }
    }

    @BeforeEach
    void setUp() {
        when(userRepository.existsByUsernameIgnoreCase("ivan")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("ivan@test.ru")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
    }

    @Test
    @DisplayName("Контекст Spring загружается без подключения к базе данных")
    void contextLoads_withoutDatabaseConnection() {
        assertNotNull(userService);
        assertNotNull(userMapper);
    }

    @Test
    @DisplayName("Создание заявки использует подменённые репозитории")
    void createTicket_shouldSaveTicketWithoutDatabaseConnection() {
        CustomerUser customer = new CustomerUser(null, "client", "client@test.ru", "hash",
                UserStatus.ACTIVE, "Иванов", "Иван", "Иванович",
                "+79001234567", null, null, null, 0);
        customer.setId(7L);
        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.<User>of(customer));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setId(15L);
            return ticket;
        });

        Ticket created = ticketService.createTicket("Не работает принтер", "Ошибка печати", customer);

        assertEquals(15L, created.getId());
        assertEquals(TicketStatus.PENDING, created.getStatus());
        assertEquals(1, customer.getCreatedTicketCount());
    }

    @Test
    @DisplayName("Регистрация пользователя использует подменённый репозиторий")
    void registerUser_shouldSaveUserUsingMockRepository() {
        CustomerUser customer = new CustomerUser(null, "ivan", "ivan@test.ru", "hash",
                UserStatus.ACTIVE, "Иванов", "Иван", "Иванович",
                "+79001234567", null, null, null, 0);

        User registered = userService.registerUser(customer);

        assertNotNull(registered.getId());
        assertEquals(UserRole.CUSTOMER, registered.getRole());
        assertEquals("ivan", registered.getUsername());
    }
}