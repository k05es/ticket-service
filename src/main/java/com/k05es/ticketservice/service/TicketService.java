package com.k05es.ticketservice.service;

import com.k05es.ticketservice.dto.request.UpdateTicketRequest;
import com.k05es.ticketservice.model.CustomerUser;
import com.k05es.ticketservice.model.SpecialistUser;
import com.k05es.ticketservice.model.Ticket;
import com.k05es.ticketservice.model.TicketStatus;
import com.k05es.ticketservice.model.User;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Сервис для работы с заявками. */
public interface TicketService {
    Ticket createTicket(String title, String description, CustomerUser customer);
    Ticket findTicketById(Long id);
    List<Ticket> findTicketsByCustomerEmail(String email);
    List<Ticket> findTicketsByStatus(TicketStatus status);
    List<Ticket> findTicketsByCustomer(Long customerId);
    List<Ticket> findTicketsBySpecialist(Long specialistId);
    List<Ticket> searchTickets(TicketStatus status, Long customerId, Long specialistId, String customerEmail);
    List<Ticket> getAllTickets();
    Ticket updateTicket(Long id, UpdateTicketRequest request);
    boolean assignTicketToSpecialist(Long ticketId, SpecialistUser specialist);
    boolean completeTicket(Long ticketId, double rating);
    boolean cancelTicket(Long ticketId);
    boolean addMessageToTicket(Long ticketId, User sender, String text);
    boolean deleteTicket(Long id);
    Ticket pollNextTicket(long timeout, TimeUnit unit) throws InterruptedException;
    Ticket pollNextTicket(long timeout, TimeUnit unit, Set<Long> excludedTicketIds)
            throws InterruptedException;
    void returnToQueue(Ticket ticket);
    boolean hasPendingTickets();
}