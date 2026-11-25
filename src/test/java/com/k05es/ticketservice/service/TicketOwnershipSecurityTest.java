package com.k05es.ticketservice.service;

import com.k05es.ticketservice.exception.UnauthorizedActionException;
import com.k05es.ticketservice.model.CustomerUser;
import com.k05es.ticketservice.model.Department;
import com.k05es.ticketservice.model.SpecialistUser;
import com.k05es.ticketservice.model.Ticket;
import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.model.UserStatus;
import com.k05es.ticketservice.repository.TicketRepository;
import com.k05es.ticketservice.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketOwnershipSecurityTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private TicketServiceImpl ticketService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String username, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                username,
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Клиент не может создать заявку от имени другого клиента")
    void customerCannotCreateTicketForAnotherCustomer() {
        CustomerUser owner = new CustomerUser(10L, "client10", "c10@test.ru", "hash",
                UserStatus.ACTIVE, "Иванов", "Иван", null,
                "+79001234567", null, null, null, 0);
        CustomerUser victim = new CustomerUser(25L, "client25", "c25@test.ru", "hash",
                UserStatus.ACTIVE, "Петров", "Пётр", null,
                "+79001234568", null, null, null, 0);
        authenticateAs("client10", "CUSTOMER");
        when(userRepository.findByUsernameIgnoreCase("client10")).thenReturn(owner);
        when(userRepository.findByIdForUpdate(25L)).thenReturn(Optional.<User>of(victim));

        assertThrows(UnauthorizedActionException.class,
                () -> ticketService.createTicket("Заявка", "Описание", victim));
        assertEquals("client10", owner.getUsername());
    }

    @Test
    @DisplayName("Пользователь не может отправить сообщение от чужого имени")
    void userCannotSendMessageAsAnotherSender() {
        CustomerUser owner = new CustomerUser(10L, "client10", "c10@test.ru", "hash",
                UserStatus.ACTIVE, "Иванов", "Иван", null,
                "+79001234567", null, null, null, 0);
        CustomerUser spoofedSender = new CustomerUser(25L, "client25", "c25@test.ru", "hash",
                UserStatus.ACTIVE, "Петров", "Пётр", null,
                "+79001234568", null, null, null, 0);
        Ticket ticket = new Ticket(50L, "Тема", "Описание", owner);
        authenticateAs("client10", "CUSTOMER");
        when(ticketRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByIdForUpdate(25L)).thenReturn(Optional.<User>of(spoofedSender));

        assertThrows(UnauthorizedActionException.class,
                () -> ticketService.addMessageToTicket(50L, spoofedSender, "Подложное сообщение"));
    }

    @Test
    @DisplayName("Специалист не может завершить чужую заявку")
    void specialistCannotCompleteUnassignedTicket() {
        CustomerUser owner = new CustomerUser(10L, "client10", "c10@test.ru", "hash",
                UserStatus.ACTIVE, "Иванов", "Иван", null,
                "+79001234567", null, null, null, 0);
        SpecialistUser assignedSpec = new SpecialistUser(20L, "spec20", "s20@test.ru", "hash",
                UserStatus.ACTIVE, "Смирнов", "Сергей", null,
                0.0, 0, 0, 1, true, Department.TECHNICAL_SUPPORT);
        SpecialistUser otherSpec = new SpecialistUser(21L, "spec21", "s21@test.ru", "hash",
                UserStatus.ACTIVE, "Кузнецов", "Алексей", null,
                0.0, 0, 0, 0, true, Department.TECHNICAL_SUPPORT);
        Ticket ticket = new Ticket(50L, "Тема", "Описание", owner);
        ticket.assignToSpecialist(assignedSpec);

        authenticateAs("spec21", "SPECIALIST");
        when(userRepository.findByUsernameIgnoreCase("spec21")).thenReturn(otherSpec);
        when(ticketRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByIdForUpdate(20L)).thenReturn(Optional.<User>of(assignedSpec));

        assertThrows(UnauthorizedActionException.class,
                () -> ticketService.completeTicket(50L, 5.0));
    }

    @Test
    @DisplayName("Клиент не может просматривать чужую заявку по ID")
    void customerCannotReadAnotherCustomersTicket() {
        CustomerUser owner = new CustomerUser(10L, "client10", "c10@test.ru", "hash",
                UserStatus.ACTIVE, "Иванов", "Иван", null,
                "+79001234567", null, null, null, 0);
        CustomerUser otherClient = new CustomerUser(11L, "client11", "c11@test.ru", "hash",
                UserStatus.ACTIVE, "Петров", "Пётр", null,
                "+79001234568", null, null, null, 0);
        Ticket ticket = new Ticket(50L, "Тема", "Описание", owner);

        authenticateAs("client11", "CUSTOMER");
        when(userRepository.findByUsernameIgnoreCase("client11")).thenReturn(otherClient);
        when(ticketRepository.findById(50L)).thenReturn(Optional.of(ticket));

        assertThrows(UnauthorizedActionException.class,
                () -> ticketService.findTicketById(50L));
    }
}
