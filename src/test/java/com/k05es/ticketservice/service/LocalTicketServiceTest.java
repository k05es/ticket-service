package com.k05es.ticketservice.service;

import com.k05es.ticketservice.model.CustomerUser;
import com.k05es.ticketservice.model.SpecialistUser;
import com.k05es.ticketservice.model.Ticket;
import com.k05es.ticketservice.model.TicketStatus;
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
 * Тесты для LocalTicketService.
 */
class LocalTicketServiceTest {

    private LocalTicketService ticketService;
    private CustomerUser customer;
    private SpecialistUser specialist;

    @BeforeEach
    void setUp() {
        ticketService = new LocalTicketService();

        customer = TestUsers.customer("client");
        customer.setId(1L);

        specialist = TestUsers.specialist("spec");
        specialist.setId(2L);
    }

    // Создание заявки

    @Test
    @DisplayName("Создание присваивает ID и статус PENDING")
    void createTicket_shouldAssignIdAndStatus() {
        Ticket ticket = ticketService.createTicket("Проблема", "Описание", customer);

        assertNotNull(ticket.getId());
        assertEquals(TicketStatus.PENDING, ticket.getStatus());
    }

    @Test
    @DisplayName("Создание увеличивает счётчик клиента")
    void createTicket_shouldIncrementCustomerCounter() {
        ticketService.createTicket("A", "B", customer);
        ticketService.createTicket("C", "D", customer);

        assertEquals(2, customer.getCreatedTicketCount());
    }

    @Test
    @DisplayName("Создание с null клиентом запрещено")
    void createTicket_shouldThrow_whenCustomerIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> ticketService.createTicket("A", "B", null));
    }

    // Поиск по ID

    @Test
    @DisplayName("Поиск по ID находит заявку")
    void findTicketById_shouldReturnTicket() {
        Ticket created = ticketService.createTicket("A", "B", customer);

        Ticket found = ticketService.findTicketById(created.getId());

        assertNotNull(found);
        assertEquals("A", found.getTitle());
    }

    @Test
    @DisplayName("Поиск по неизвестному ID возвращает null")
    void findTicketById_shouldReturnNull_whenNotFound() {
        assertNull(ticketService.findTicketById(999L));
    }

    // Поиск по email клиента

    @Test
    @DisplayName("Поиск по email клиента находит его заявки")
    void findTicketsByCustomerEmail_shouldReturnTickets() {
        ticketService.createTicket("A", "B", customer);
        ticketService.createTicket("C", "D", customer);

        List<Ticket> found = ticketService.findTicketsByCustomerEmail("client@test.ru");

        assertEquals(2, found.size());
    }

    // Фильтр по статусу

    @Test
    @DisplayName("Фильтр по статусу возвращает только нужные")
    void findTicketsByStatus_shouldReturnOnlyMatching() {
        ticketService.createTicket("A", "B", customer);
        Ticket second = ticketService.createTicket("C", "D", customer);
        ticketService.assignTicketToSpecialist(second.getId(), specialist);

        assertEquals(1, ticketService.findTicketsByStatus(TicketStatus.PENDING).size());
        assertEquals(1, ticketService.findTicketsByStatus(TicketStatus.IN_PROGRESS).size());
    }

    // Назначение специалиста

    @Test
    @DisplayName("Назначение переводит заявку в IN_PROGRESS")
    void assignTicketToSpecialist_shouldChangeStatus() {
        Ticket ticket = ticketService.createTicket("A", "B", customer);

        boolean result = ticketService.assignTicketToSpecialist(ticket.getId(), specialist);

        assertTrue(result);
        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
    }

    @Test
    @DisplayName("Назначение увеличивает нагрузку специалиста")
    void assignTicketToSpecialist_shouldIncrementActiveCount() {
        Ticket ticket = ticketService.createTicket("A", "B", customer);

        ticketService.assignTicketToSpecialist(ticket.getId(), specialist);

        assertEquals(1, specialist.getActiveTicketCount());
    }

    @Test
    @DisplayName("Нельзя назначить второго специалиста на уже взятую заявку")
    void assignTicketToSpecialist_shouldReturnFalse_whenAlreadyInProgress() {
        Ticket ticket = ticketService.createTicket("A", "B", customer);
        ticketService.assignTicketToSpecialist(ticket.getId(), specialist);

        SpecialistUser second = TestUsers.specialist("spec2");
        second.setId(3L);

        boolean result = ticketService.assignTicketToSpecialist(ticket.getId(), second);

        assertFalse(result);
    }

    // Завершение

    @Test
    @DisplayName("Завершение меняет статус и обновляет рейтинг")
    void completeTicket_shouldChangeStatusAndUpdateRating() {
        Ticket ticket = ticketService.createTicket("A", "B", customer);
        ticketService.assignTicketToSpecialist(ticket.getId(), specialist);

        boolean result = ticketService.completeTicket(ticket.getId(), 5.0);

        assertTrue(result);
        assertEquals(TicketStatus.COMPLETED, ticket.getStatus());
        assertEquals(1, specialist.getCompletedTicketCount());
    }

    @Test
    @DisplayName("Завершение уменьшает нагрузку специалиста")
    void completeTicket_shouldDecrementActiveCount() {
        Ticket ticket = ticketService.createTicket("A", "B", customer);
        ticketService.assignTicketToSpecialist(ticket.getId(), specialist);

        ticketService.completeTicket(ticket.getId(), 5.0);

        assertEquals(0, specialist.getActiveTicketCount());
    }

    @Test
    @DisplayName("Нельзя завершить заявку не в работе")
    void completeTicket_shouldReturnFalse_whenPending() {
        Ticket ticket = ticketService.createTicket("A", "B", customer);

        assertFalse(ticketService.completeTicket(ticket.getId(), 5.0));
    }

    // Сообщения

    @Test
    @DisplayName("Сообщение добавляется в переписку")
    void addMessageToTicket_shouldAddMessage() {
        Ticket ticket = ticketService.createTicket("A", "B", customer);

        boolean result = ticketService.addMessageToTicket(ticket.getId(), customer, "Текст");

        assertTrue(result);
        assertEquals(1, ticket.getMessages().size());
    }

    @Test
    @DisplayName("Пустой текст сообщения отклоняется")
    void addMessageToTicket_shouldReturnFalse_whenTextIsBlank() {
        Ticket ticket = ticketService.createTicket("A", "B", customer);

        boolean result = ticketService.addMessageToTicket(ticket.getId(), customer, "   ");

        assertFalse(result);
    }

    @Test
    @DisplayName("Нельзя писать в закрытую заявку")
    void addMessageToTicket_shouldReturnFalse_whenTicketClosed() {
        Ticket ticket = ticketService.createTicket("A", "B", customer);
        ticketService.assignTicketToSpecialist(ticket.getId(), specialist);
        ticketService.completeTicket(ticket.getId(), 5.0);

        boolean result = ticketService.addMessageToTicket(ticket.getId(), customer, "Поздно");

        assertFalse(result);
    }

    // Удаление

    @Test
    @DisplayName("Удаление убирает заявку")
    void deleteTicket_shouldRemoveFromMap() {
        Ticket ticket = ticketService.createTicket("A", "B", customer);

        boolean result = ticketService.deleteTicket(ticket.getId());

        assertTrue(result);
        assertNull(ticketService.findTicketById(ticket.getId()));
    }

    @Test
    @DisplayName("Удаление неизвестной заявки возвращает false")
    void deleteTicket_shouldReturnFalse_whenNotFound() {
        assertFalse(ticketService.deleteTicket(999L));
    }
}