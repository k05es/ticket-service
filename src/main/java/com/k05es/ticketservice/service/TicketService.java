package com.k05es.ticketservice.service;

import com.k05es.ticketservice.model.CustomerUser;
import com.k05es.ticketservice.model.SpecialistUser;
import com.k05es.ticketservice.model.Ticket;
import com.k05es.ticketservice.model.TicketStatus;
import com.k05es.ticketservice.model.User;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Сервис для работы с заявками.
 */
public interface TicketService {

    // Создаёт новую заявку от имени клиента
    Ticket createTicket(String title, String description, CustomerUser customer);

    // Находит заявку по ID
    Ticket findTicketById(Long id);

    // Находит заявки по email клиента
    List<Ticket> findTicketsByCustomerEmail(String email);

    // Возвращает заявки с указанным статусом
    List<Ticket> findTicketsByStatus(TicketStatus status);

    // Возвращает заявки указанного клиента
    List<Ticket> findTicketsByCustomer(Long customerId);

    // Возвращает заявки указанного специалиста
    List<Ticket> findTicketsBySpecialist(Long specialistId);

    // Возвращает все заявки
    List<Ticket> getAllTickets();

    // Назначает специалиста на заявку
    boolean assignTicketToSpecialist(Long ticketId, SpecialistUser specialist);

    // Завершает заявку с оценкой работы специалиста
    boolean completeTicket(Long ticketId, double rating);

    // Добавляет сообщение в переписку по заявке
    boolean addMessageToTicket(Long ticketId, User sender, String text);

    // Удаляет заявку
    boolean deleteTicket(Long id);

    // Берёт следующую заявку из очереди с ожиданием.
    // Возвращает null, если за timeout не пришло ни одной заявки
    Ticket pollNextTicket(long timeout, TimeUnit unit) throws InterruptedException;

    // Возвращает заявку обратно в очередь
    void returnToQueue(Ticket ticket);

    // Проверяет, есть ли заявки в очереди
    boolean hasPendingTickets();
}