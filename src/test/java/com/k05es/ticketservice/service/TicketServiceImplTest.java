package com.k05es.ticketservice.service;

import com.k05es.ticketservice.exception.ResourceConflictException;
import com.k05es.ticketservice.exception.ResourceNotFoundException;
import com.k05es.ticketservice.model.AdminUser;
import com.k05es.ticketservice.model.CustomerUser;
import com.k05es.ticketservice.model.Department;
import com.k05es.ticketservice.model.Permission;
import com.k05es.ticketservice.model.SpecialistUser;
import com.k05es.ticketservice.model.Ticket;
import com.k05es.ticketservice.model.TicketStatus;
import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.model.UserStatus;
import com.k05es.ticketservice.repository.TicketRepository;
import com.k05es.ticketservice.repository.UserRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private TicketServiceImpl ticketService;

    private CustomerUser customer;
    private SpecialistUser specialist;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        customer = new CustomerUser(1L, "client", "client@example.test", "hash",
                UserStatus.ACTIVE, "Иванов", "Иван", null,
                "+79001234567", null, null, null, 1);
        specialist = new SpecialistUser(2L, "specialist", "specialist@example.test", "hash",
                UserStatus.ACTIVE, "Петров", "Пётр", null,
                0.0, 0, 0, 0, true, Department.TECHNICAL_SUPPORT);
        ticket = new Ticket(20L, "Не работает принтер", "Описание проблемы", customer);
    }

    @Test
    @DisplayName("Создание заявки увеличивает счётчик клиента")
    void createTicketIncrementsCustomerCounter() {
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.<User>of(customer));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket saved = invocation.getArgument(0);
            saved.setId(21L);
            return saved;
        });
        Ticket created = ticketService.createTicket("Новая заявка", "Описание", customer);
        assertEquals(21L, created.getId());
        assertEquals(2, customer.getCreatedTicketCount());
    }

    @Test
    @DisplayName("Назначение переводит заявку в работу и увеличивает счётчик специалиста")
    void assignTicketUpdatesStatusAndCounter() {
        when(ticketRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.<User>of(specialist));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.assignTicketToSpecialist(20L, specialist);

        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
        assertEquals(1, specialist.getActiveTicketCount());
    }

    @Test
    @DisplayName("Повторное назначение одной заявки отклоняется")
    void assignTicketRejectsAlreadyAssignedTicket() {
        ticket.assignToSpecialist(specialist);
        when(ticketRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.<User>of(specialist));

        assertThrows(ResourceConflictException.class,
                () -> ticketService.assignTicketToSpecialist(20L, specialist));
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Завершение заявки освобождает специалиста и учитывает оценку")
    void completeTicketUpdatesStatistics() {
        ticket.assignToSpecialist(specialist);
        specialist.incrementActiveTicketCount();
        when(ticketRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.<User>of(specialist));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.completeTicket(20L, 4.5);

        assertEquals(TicketStatus.COMPLETED, ticket.getStatus());
        assertEquals(0, specialist.getActiveTicketCount());
        assertEquals(1, specialist.getCompletedTicketCount());
        assertEquals(4.5, specialist.getAverageRating());
    }

    @Test
    @DisplayName("Неактивный клиент не может создавать заявки")
    void createTicketRejectsInactiveCustomer() {
        customer.block();
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.<User>of(customer));
        assertThrows(ResourceConflictException.class,
                () -> ticketService.createTicket("Новая заявка", "Описание", customer));
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Пользователь, не участвующий в заявке, не может отправить сообщение")
    void addMessageRejectsUnrelatedSender() {
        CustomerUser otherCustomer = new CustomerUser(3L, "other", "other@example.test", "hash",
                UserStatus.ACTIVE, "Сидоров", "Сидор", null,
                "+79001234568", null, null, null, 0);
        when(ticketRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByIdForUpdate(3L)).thenReturn(Optional.<User>of(otherCustomer));

        assertThrows(ResourceConflictException.class,
                () -> ticketService.addMessageToTicket(20L, otherCustomer, "Постороннее сообщение"));
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Клиент может отправить сообщение в собственную заявку")
    void addMessageAllowsTicketCustomer() {
        when(ticketRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.<User>of(customer));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.addMessageToTicket(20L, customer, "Уточнение по заявке");

        assertEquals(1, ticket.getMessages().size());
        assertEquals("Уточнение по заявке", ticket.getMessages().get(0).getText());
    }


    @Test
    @DisplayName("Администратор без разрешения MANAGE_TICKETS не может писать в заявку")
    void addMessageRejectsAdminWithoutPermission() {
        AdminUser admin = new AdminUser(4L, "admin", "admin@example.test", "hash",
                UserStatus.ACTIVE, "Админов", "Админ", null,
                Set.of(Permission.VIEW_REPORTS), Department.GENERAL_SUPPORT, Set.of());
        when(ticketRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByIdForUpdate(4L)).thenReturn(Optional.<User>of(admin));
        assertThrows(ResourceConflictException.class,
                () -> ticketService.addMessageToTicket(20L, admin, "Сообщение администратора"));
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Отмена заявки освобождает активного специалиста")
    void cancelTicketReleasesSpecialist() {
        ticket.assignToSpecialist(specialist);
        specialist.incrementActiveTicketCount();
        when(ticketRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.<User>of(specialist));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.cancelTicket(20L);

        assertEquals(TicketStatus.CANCELLED, ticket.getStatus());
        assertEquals(0, specialist.getActiveTicketCount());
    }

    @Test
    @DisplayName("Удаление заявки уменьшает счётчик созданных заявок клиента")
    void deleteTicketDecrementsCustomerCounter() {
        when(ticketRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.<User>of(customer));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.deleteTicket(20L);

        assertEquals(0, customer.getCreatedTicketCount());
        verify(ticketRepository).delete(ticket);
    }

    @Test
    @DisplayName("Завершённую заявку нельзя удалить, чтобы не исказить статистику")
    void deleteTicketRejectsCompletedTicket() {
        ticket.assignToSpecialist(specialist);
        ticket.complete();
        when(ticketRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(ticket));
        assertThrows(ResourceConflictException.class, () -> ticketService.deleteTicket(20L));
        verify(ticketRepository, never()).delete(any(Ticket.class));
    }

    @Test
    @DisplayName("Фильтрация заявок передаёт критерии репозиторию")
    void searchTicketsDelegatesFilters() {
        when(ticketRepository.searchTickets(TicketStatus.PENDING, 1L, null, "client@example.test"))
                .thenReturn(java.util.List.of(ticket));

        assertEquals(1, ticketService.searchTickets(TicketStatus.PENDING, 1L, null,
                " client@example.test ").size());
    }

    @Test
    @DisplayName("Обращение к несуществующей заявке возвращает not-found исключение")
    void findTicketThrowsWhenMissing() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> ticketService.findTicketById(99L));
    }
}