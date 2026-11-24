package com.k05es.ticketservice.service;

import com.k05es.ticketservice.model.CustomerUser;
import com.k05es.ticketservice.model.SpecialistUser;
import com.k05es.ticketservice.model.Ticket;
import com.k05es.ticketservice.model.TicketStatus;
import com.k05es.ticketservice.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Локальная реализация TicketService в памяти.
 */
public class LocalTicketService implements TicketService {

    // ID заявки -> сама заявка
    private final Map<Long, Ticket> tickets = new HashMap<>();

    // Очередь необработанных заявок
    private final BlockingQueue<Ticket> pendingQueue = new LinkedBlockingQueue<>();

    // Счётчик ID
    private final AtomicLong idGenerator = new AtomicLong(1);

    // Создаёт новую заявку
    @Override
    public Ticket createTicket(String title, String description, CustomerUser customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Клиент не может быть null");
        }

        long newId = idGenerator.getAndIncrement();
        Ticket ticket = new Ticket(newId, title, description, customer);
        tickets.put(newId, ticket);
        pendingQueue.add(ticket);
        customer.incrementCreatedTicketCount();

        return ticket;
    }

    // Находит заявку по ID
    @Override
    public Ticket findTicketById(Long id) {
        if (id == null) {
            return null;
        }
        return tickets.get(id);
    }

    // Находит заявки по email клиента
    @Override
    public List<Ticket> findTicketsByCustomerEmail(String email) {
        List<Ticket> result = new ArrayList<>();
        if (email == null || email.isBlank()) {
            return result;
        }
        for (Ticket ticket : tickets.values()) {
            CustomerUser customer = ticket.getCustomer();
            if (customer != null
                    && customer.getEmail() != null
                    && customer.getEmail().equalsIgnoreCase(email)) {
                result.add(ticket);
            }
        }
        return result;
    }

    // Возвращает заявки с указанным статусом
    @Override
    public List<Ticket> findTicketsByStatus(TicketStatus status) {
        List<Ticket> result = new ArrayList<>();
        if (status == null) {
            return result;
        }
        for (Ticket ticket : tickets.values()) {
            if (ticket.getStatus() == status) {
                result.add(ticket);
            }
        }
        return result;
    }

    // Возвращает заявки указанного клиента
    @Override
    public List<Ticket> findTicketsByCustomer(Long customerId) {
        List<Ticket> result = new ArrayList<>();
        if (customerId == null) {
            return result;
        }
        for (Ticket ticket : tickets.values()) {
            CustomerUser customer = ticket.getCustomer();
            if (customer != null && customerId.equals(customer.getId())) {
                result.add(ticket);
            }
        }
        return result;
    }

    // Возвращает заявки указанного специалиста
    @Override
    public List<Ticket> findTicketsBySpecialist(Long specialistId) {
        List<Ticket> result = new ArrayList<>();
        if (specialistId == null) {
            return result;
        }
        for (Ticket ticket : tickets.values()) {
            SpecialistUser specialist = ticket.getSpecialist();
            if (specialist != null && specialistId.equals(specialist.getId())) {
                result.add(ticket);
            }
        }
        return result;
    }

    // Возвращает все заявки
    @Override
    public List<Ticket> getAllTickets() {
        return new ArrayList<>(tickets.values());
    }

    // Назначает специалиста на заявку
    @Override
    public boolean assignTicketToSpecialist(Long ticketId, SpecialistUser specialist) {
        if (ticketId == null || specialist == null) {
            return false;
        }
        Ticket ticket = tickets.get(ticketId);
        if (ticket == null) {
            return false;
        }
        try {
            ticket.assignToSpecialist(specialist);
        } catch (IllegalStateException e) {
            return false;
        }
        pendingQueue.remove(ticket);
        specialist.incrementActiveTicketCount();
        return true;
    }

    // Завершает заявку с оценкой
    @Override
    public boolean completeTicket(Long ticketId, double rating) {
        if (ticketId == null) {
            return false;
        }
        Ticket ticket = tickets.get(ticketId);
        if (ticket == null) {
            return false;
        }
        SpecialistUser specialist = ticket.getSpecialist();
        if (specialist == null) {
            return false;
        }
        try {
            ticket.complete();
        } catch (IllegalStateException e) {
            return false;
        }
        specialist.decrementActiveTicketCount();
        try {
            specialist.registerCompletedTicket(rating);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    // Добавляет сообщение в переписку по заявке
    @Override
    public boolean addMessageToTicket(Long ticketId, User sender, String text) {
        if (ticketId == null || sender == null || text == null || text.isBlank()) {
            return false;
        }
        Ticket ticket = tickets.get(ticketId);
        if (ticket == null) {
            return false;
        }
        try {
            ticket.addMessage(sender, text);
        } catch (IllegalStateException e) {
            return false;
        }
        return true;
    }

    // Удаляет заявку
    @Override
    public boolean deleteTicket(Long id) {
        if (id == null) {
            return false;
        }
        Ticket removed = tickets.remove(id);
        if (removed != null) {
            pendingQueue.remove(removed);
        }
        return removed != null;
    }

    // Берёт следующую заявку из очереди с ожиданием
    @Override
    public Ticket pollNextTicket(long timeout, TimeUnit unit) throws InterruptedException {
        return pendingQueue.poll(timeout, unit);
    }

    // Возвращает заявку обратно в очередь
    @Override
    public void returnToQueue(Ticket ticket) {
        if (ticket != null) {
            pendingQueue.add(ticket);
        }
    }

    // Проверяет, есть ли заявки в очереди
    @Override
    public boolean hasPendingTickets() {
        return !pendingQueue.isEmpty();
    }
}