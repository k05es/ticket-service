package com.k05es.ticketservice.service;

import com.k05es.ticketservice.dto.request.UpdateTicketRequest;
import com.k05es.ticketservice.exception.ResourceConflictException;
import com.k05es.ticketservice.exception.ResourceNotFoundException;
import com.k05es.ticketservice.model.CustomerUser;
import com.k05es.ticketservice.model.SpecialistUser;
import com.k05es.ticketservice.model.Ticket;
import com.k05es.ticketservice.model.TicketStatus;
import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.model.AdminUser;
import com.k05es.ticketservice.model.Permission;
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
            throw new IllegalArgumentException("Клиент не может быть null");
        }
        CustomerUser savedCustomer = userRepository.findByIdForUpdate(customer.getId())
                .filter(CustomerUser.class::isInstance).map(CustomerUser.class::cast)
                .orElseThrow(() -> new ResourceNotFoundException("Клиент не найден"));
        if (!savedCustomer.isActive()) {
            throw new ResourceConflictException("Заблокированный или неактивный клиент не может создавать заявки");
        }
        Ticket ticket = new Ticket(null, title, description, savedCustomer);
        savedCustomer.incrementCreatedTicketCount();
        userRepository.save(savedCustomer);
        return ticketRepository.save(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket findTicketById(Long id) {
        if (id == null) throw new ResourceNotFoundException("Идентификатор заявки не указан");
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка с ID " + id + " не найдена"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findTicketsByCustomerEmail(String email) {
        if (email == null || email.isBlank()) return List.of();
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
                .orElseThrow(() -> new ResourceNotFoundException("Заявка с ID " + id + " не найдена"));
        if (!ticket.isPending()) {
            throw new ResourceConflictException("Изменять можно только заявку в статусе PENDING");
        }
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        return ticketRepository.save(ticket);
    }

    /** Блокировка строки заявки исключает двойное назначение несколькими потоками. */
    @Override
    public boolean assignTicketToSpecialist(Long ticketId, SpecialistUser specialist) {
        if (ticketId == null || specialist == null || specialist.getId() == null) {
            throw new IllegalArgumentException("Необходимо указать заявку и специалиста");
        }
        Ticket ticket = ticketRepository.findByIdForUpdate(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка с ID " + ticketId + " не найдена"));
        User foundUser = userRepository.findByIdForUpdate(specialist.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Специалист с ID " + specialist.getId() + " не найден"));
        if (!(foundUser instanceof SpecialistUser savedSpecialist)) {
            throw new ResourceNotFoundException("Пользователь не является специалистом");
        }
        if (!ticket.isPending()) {
            throw new ResourceConflictException("Заявка уже взята в работу или закрыта");
        }
        if (!savedSpecialist.canTakeMoreTickets()) {
            throw new ResourceConflictException("Специалист недоступен для назначения");
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
            throw new IllegalArgumentException("Оценка должна быть от 0.0 до 5.0");
        }
        Ticket ticket = ticketRepository.findByIdForUpdate(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка с ID " + ticketId + " не найдена"));
        if (!ticket.isInProgress() || ticket.getSpecialist() == null) {
            throw new ResourceConflictException("Завершить можно только заявку в работе");
        }
        SpecialistUser specialist = (SpecialistUser) userRepository.findByIdForUpdate(ticket.getSpecialist().getId())
                .filter(SpecialistUser.class::isInstance).map(SpecialistUser.class::cast)
                .orElseThrow(() -> new ResourceNotFoundException("Специалист заявки не найден"));
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
                .orElseThrow(() -> new ResourceNotFoundException("Заявка с ID " + ticketId + " не найдена"));
        if (ticket.isClosed()) throw new ResourceConflictException("Заявка уже закрыта");
        if (ticket.isInProgress() && ticket.getSpecialist() != null) {
            SpecialistUser specialist = (SpecialistUser) userRepository.findByIdForUpdate(ticket.getSpecialist().getId())
                    .filter(SpecialistUser.class::isInstance).map(SpecialistUser.class::cast)
                    .orElseThrow(() -> new ResourceNotFoundException("Специалист заявки не найден"));
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
            throw new IllegalArgumentException("Отправитель и текст сообщения обязательны");
        }
        Ticket ticket = ticketRepository.findByIdForUpdate(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка с ID " + ticketId + " не найдена"));
        User savedSender = userRepository.findByIdForUpdate(sender.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Отправитель не найден"));
        if (!savedSender.isActive()) {
            throw new ResourceConflictException("Неактивный пользователь не может отправлять сообщения");
        }
        if (ticket.isClosed()) throw new ResourceConflictException("Нельзя писать в закрытую заявку");
        boolean isCustomer = ticket.getCustomer().getId().equals(savedSender.getId());
        boolean isAssignedSpecialist = ticket.getSpecialist() != null
                && ticket.getSpecialist().getId().equals(savedSender.getId());
        boolean isAdministrator = savedSender instanceof AdminUser admin
                && admin.hasPermission(Permission.MANAGE_TICKETS);
        if (!isCustomer && !isAssignedSpecialist && !isAdministrator) {
            throw new ResourceConflictException("Пользователь не имеет права писать в эту заявку");
        }
        ticket.addMessage(savedSender, text.trim());
        ticketRepository.save(ticket);
        return true;
    }

    @Override
    public boolean deleteTicket(Long id) {
        Ticket ticket = ticketRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка с ID " + id + " не найдена"));
        if (ticket.getStatus() == TicketStatus.COMPLETED) {
            throw new ResourceConflictException(
                    "Нельзя удалить завершённую заявку: она участвует в статистике специалиста");
        }
        if (ticket.isInProgress() && ticket.getSpecialist() != null) {
            SpecialistUser specialist = (SpecialistUser) userRepository.findByIdForUpdate(ticket.getSpecialist().getId())
                    .filter(SpecialistUser.class::isInstance).map(SpecialistUser.class::cast)
                    .orElseThrow(() -> new ResourceNotFoundException("Специалист заявки не найден"));
            specialist.decrementActiveTicketCount();
            userRepository.save(specialist);
        }
        CustomerUser customer = ticket.getCustomer();
        if (customer != null) {
            User lockedCustomer = userRepository.findByIdForUpdate(customer.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Клиент заявки не найден"));
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
            if (ticket != null) return ticket;
            if (System.nanoTime() >= deadline) return null;
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