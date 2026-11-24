package com.k05es.ticketservice.service;

import com.k05es.ticketservice.dto.request.UpdateTicketRequest;
import com.k05es.ticketservice.exception.CompletedTicketDeletionException;
import com.k05es.ticketservice.exception.InvalidRequestException;
import com.k05es.ticketservice.exception.InvalidTicketStateException;
import com.k05es.ticketservice.exception.SpecialistUnavailableException;
import com.k05es.ticketservice.exception.TicketNotFoundException;
import com.k05es.ticketservice.exception.UnauthorizedActionException;
import com.k05es.ticketservice.exception.UserNotFoundException;
import com.k05es.ticketservice.model.AdminUser;
import com.k05es.ticketservice.model.CustomerUser;
import com.k05es.ticketservice.model.Permission;
import com.k05es.ticketservice.model.SpecialistUser;
import com.k05es.ticketservice.model.Ticket;
import com.k05es.ticketservice.model.TicketStatus;
import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.repository.TicketRepository;
import com.k05es.ticketservice.repository.UserRepository;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Реализация TicketService с использованием Spring Data JPA. */
@Service
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketServiceImpl(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Ticket createTicket(String title, String description, CustomerUser customer) {
        if (customer == null || customer.getId() == null) {
            throw new InvalidRequestException("Клиент не может быть null");
        }
        User lockedUser = userRepository.findByIdForUpdate(customer.getId())
                .orElseThrow(() -> UserNotFoundException.byId(customer.getId()));
        if (!(lockedUser instanceof CustomerUser savedCustomer)) {
            throw new UserNotFoundException("Клиент с ID " + customer.getId() + " не найден.");
        }
        if (!savedCustomer.isActive()) {
            throw new UnauthorizedActionException(
                    "Заблокированный или неактивный клиент не может создавать заявки");
        }
        Ticket ticket = new Ticket(null, title, description, savedCustomer);
        savedCustomer.incrementCreatedTicketCount();
        userRepository.save(savedCustomer);
        return ticketRepository.save(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket findTicketById(Long id) {
        if (id == null) {
            throw new TicketNotFoundException("Идентификатор заявки не указан");
        }
        return ticketRepository.findById(id)
                .orElseThrow(() -> TicketNotFoundException.byId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findTicketsByCustomerEmail(String email) {
        if (email == null || email.isBlank()) {
            return List.of();
        }
        return ticketRepository.searchTickets(null, null, null, email.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findTicketsByStatus(TicketStatus status) {
        return status == null ? List.of() : ticketRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findTicketsByCustomer(Long customerId) {
        return customerId == null ? List.of() : ticketRepository.findByCustomerId(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findTicketsBySpecialist(Long specialistId) {
        return specialistId == null ? List.of() : ticketRepository.findBySpecialistId(specialistId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> searchTickets(TicketStatus status, Long customerId, Long specialistId, String customerEmail) {
        String normalizedEmail = customerEmail == null || customerEmail.isBlank() ? null : customerEmail.trim();
        return ticketRepository.searchTickets(status, customerId, specialistId, normalizedEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    @Override
    public Ticket updateTicket(Long id, UpdateTicketRequest request) {
        Ticket ticket = ticketRepository.findByIdForUpdate(id)
                .orElseThrow(() -> TicketNotFoundException.byId(id));
        if (!ticket.isPending()) {
            throw new InvalidTicketStateException(
                    "Изменять можно только заявку в статусе PENDING (текущий статус: " + ticket.getStatus() + ")");
        }
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        return ticketRepository.save(ticket);
    }

    /** Блокировка строки заявки исключает двойное назначение несколькими потоками. */
    @Override
    public boolean assignTicketToSpecialist(Long ticketId, SpecialistUser specialist) {
        if (ticketId == null || specialist == null || specialist.getId() == null) {
            throw new InvalidRequestException("Необходимо указать заявку и специалиста");
        }
        Ticket ticket = ticketRepository.findByIdForUpdate(ticketId)
                .orElseThrow(() -> TicketNotFoundException.byId(ticketId));
        User foundUser = userRepository.findByIdForUpdate(specialist.getId())
                .orElseThrow(() -> UserNotFoundException.byId(specialist.getId()));
        if (!(foundUser instanceof SpecialistUser savedSpecialist)) {
            throw new UserNotFoundException(
                    "Пользователь с ID " + specialist.getId() + " не является специалистом");
        }
        if (!ticket.isPending()) {
            throw new InvalidTicketStateException(
                    "Заявка уже взята в работу или закрыта (текущий статус: " + ticket.getStatus() + ")");
        }
        if (!savedSpecialist.canTakeMoreTickets()) {
            throw new SpecialistUnavailableException("Специалист недоступен для назначения");
        }
        ticket.assignToSpecialist(savedSpecialist);
        savedSpecialist.incrementActiveTicketCount();
        ticketRepository.save(ticket);
        userRepository.save(savedSpecialist);
        return true;
    }

    @Override
    public boolean completeTicket(Long ticketId, double rating) {
        if (!Double.isFinite(rating) || rating < 0.0 || rating > 5.0) {
            throw new InvalidRequestException("Оценка должна быть от 0.0 до 5.0");
        }
        Ticket ticket = ticketRepository.findByIdForUpdate(ticketId)
                .orElseThrow(() -> TicketNotFoundException.byId(ticketId));
        if (!ticket.isInProgress() || ticket.getSpecialist() == null) {
            throw new InvalidTicketStateException(
                    "Завершить можно только заявку в работе (текущий статус: " + ticket.getStatus() + ")");
        }
        User lockedSpecialistUser = userRepository.findByIdForUpdate(ticket.getSpecialist().getId())
                .orElseThrow(() -> UserNotFoundException.byId(ticket.getSpecialist().getId()));
        if (!(lockedSpecialistUser instanceof SpecialistUser specialist)) {
            throw new UserNotFoundException(
                    "Специалист заявки с ID " + ticket.getSpecialist().getId() + " не найден");
        }
        ticket.complete();
        specialist.decrementActiveTicketCount();
        specialist.registerCompletedTicket(rating);
        ticketRepository.save(ticket);
        userRepository.save(specialist);
        return true;
    }

    @Override
    public boolean cancelTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findByIdForUpdate(ticketId)
                .orElseThrow(() -> TicketNotFoundException.byId(ticketId));
        if (ticket.isClosed()) {
            throw new InvalidTicketStateException(
                    "Заявка уже закрыта (текущий статус: " + ticket.getStatus() + ")");
        }
        if (ticket.isInProgress() && ticket.getSpecialist() != null) {
            User lockedSpecialistUser = userRepository.findByIdForUpdate(ticket.getSpecialist().getId())
                    .orElseThrow(() -> UserNotFoundException.byId(ticket.getSpecialist().getId()));
            if (!(lockedSpecialistUser instanceof SpecialistUser specialist)) {
                throw new UserNotFoundException(
                        "Специалист заявки с ID " + ticket.getSpecialist().getId() + " не найден");
            }
            specialist.decrementActiveTicketCount();
            userRepository.save(specialist);
        }
        ticket.cancel();
        ticketRepository.save(ticket);
        return true;
    }

    @Override
    public boolean addMessageToTicket(Long ticketId, User sender, String text) {
        if (sender == null || sender.getId() == null || text == null || text.isBlank()) {
            throw new InvalidRequestException("Отправитель и текст сообщения обязательны");
        }
        Ticket ticket = ticketRepository.findByIdForUpdate(ticketId)
                .orElseThrow(() -> TicketNotFoundException.byId(ticketId));
        User savedSender = userRepository.findByIdForUpdate(sender.getId())
                .orElseThrow(() -> UserNotFoundException.byId(sender.getId()));
        if (!savedSender.isActive()) {
            throw new UnauthorizedActionException(
                    "Неактивный пользователь не может отправлять сообщения");
        }
        if (ticket.isClosed()) {
            throw new InvalidTicketStateException(
                    "Нельзя писать в закрытую заявку (текущий статус: " + ticket.getStatus() + ")");
        }
        boolean isCustomer = ticket.getCustomer().getId().equals(savedSender.getId());
        boolean isAssignedSpecialist = ticket.getSpecialist() != null
                && ticket.getSpecialist().getId().equals(savedSender.getId());
        boolean isAdministrator = savedSender instanceof AdminUser admin
                && admin.hasPermission(Permission.MANAGE_TICKETS);
        if (!isCustomer && !isAssignedSpecialist && !isAdministrator) {
            throw new UnauthorizedActionException(
                    "Пользователь не имеет права писать в эту заявку");
        }
        ticket.addMessage(savedSender, text.trim());
        ticketRepository.save(ticket);
        return true;
    }

    @Override
    public boolean deleteTicket(Long id) {
        Ticket ticket = ticketRepository.findByIdForUpdate(id)
                .orElseThrow(() -> TicketNotFoundException.byId(id));
        if (ticket.getStatus() == TicketStatus.COMPLETED) {
            throw new CompletedTicketDeletionException(
                    "Нельзя удалить завершённую заявку: она участвует в статистике специалиста");
        }
        if (ticket.isInProgress() && ticket.getSpecialist() != null) {
            User lockedSpecialistUser = userRepository.findByIdForUpdate(ticket.getSpecialist().getId())
                    .orElseThrow(() -> UserNotFoundException.byId(ticket.getSpecialist().getId()));
            if (!(lockedSpecialistUser instanceof SpecialistUser specialist)) {
                throw new UserNotFoundException(
                        "Специалист заявки с ID " + ticket.getSpecialist().getId() + " не найден");
            }
            specialist.decrementActiveTicketCount();
            userRepository.save(specialist);
        }
        CustomerUser customer = ticket.getCustomer();
        if (customer != null) {
            User lockedCustomer = userRepository.findByIdForUpdate(customer.getId())
                    .orElseThrow(() -> UserNotFoundException.byId(customer.getId()));
            if (lockedCustomer instanceof CustomerUser savedCustomer
                    && savedCustomer.getCreatedTicketCount() > 0) {
                savedCustomer.setCreatedTicketCount(savedCustomer.getCreatedTicketCount() - 1);
                userRepository.save(savedCustomer);
            }
        }
        ticketRepository.delete(ticket);
        return true;
    }

    @Override
    public Ticket pollNextTicket(long timeout, TimeUnit unit) throws InterruptedException {
        return pollNextTicket(timeout, unit, Set.of());
    }

    @Override
    public Ticket pollNextTicket(long timeout, TimeUnit unit, Set<Long> excludedTicketIds)
            throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        Set<Long> excluded = excludedTicketIds == null ? Set.of() : excludedTicketIds;
        do {
            Ticket ticket = ticketRepository.findByStatusOrderByCreatedAtAsc(TicketStatus.PENDING)
                    .stream()
                    .filter(candidate -> !excluded.contains(candidate.getId()))
                    .findFirst()
                    .orElse(null);
            if (ticket != null) {
                return ticket;
            }
            if (System.nanoTime() >= deadline) {
                return null;
            }
            Thread.sleep(Math.min(100L, Math.max(1L, unit.toMillis(timeout))));
        } while (true);
    }

    @Override
    public void returnToQueue(Ticket ticket) {
        // До успешного назначения заявка остаётся в PENDING и не требует возврата.
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPendingTickets() {
        return !ticketRepository.findByStatus(TicketStatus.PENDING).isEmpty();
    }
}
