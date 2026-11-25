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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Реализация TicketService с использованием Spring Data JPA, проверками прав доступа и логированием SLF4J. */
@Slf4j
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
        enforceCustomerOwnershipIfAuthenticated(savedCustomer);

        Ticket ticket = new Ticket(null, title, description, savedCustomer);
        savedCustomer.incrementCreatedTicketCount();
        userRepository.save(savedCustomer);
        Ticket saved = ticketRepository.save(ticket);
        log.info("Создана заявка id={}, title='{}', customer='{}'",
                saved.getId(), saved.getTitle(), savedCustomer.getUsername());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket findTicketById(Long id) {
        if (id == null) {
            throw new TicketNotFoundException("Идентификатор заявки не указан");
        }
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> TicketNotFoundException.byId(id));
        enforceTicketReadAccess(ticket);
        return ticket;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findTicketsByCustomerEmail(String email) {
        if (email == null || email.isBlank()) {
            return List.of();
        }
        return filterTicketsForCurrentUser(
                ticketRepository.searchTickets(null, null, null, email.trim()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findTicketsByStatus(TicketStatus status) {
        if (status == null) {
            return List.of();
        }
        return filterTicketsForCurrentUser(ticketRepository.findByStatus(status));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findTicketsByCustomer(Long customerId) {
        if (customerId == null) {
            return List.of();
        }
        return filterTicketsForCurrentUser(ticketRepository.findByCustomerId(customerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findTicketsBySpecialist(Long specialistId) {
        if (specialistId == null) {
            return List.of();
        }
        return filterTicketsForCurrentUser(ticketRepository.findBySpecialistId(specialistId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> searchTickets(TicketStatus status, Long customerId, Long specialistId, String customerEmail) {
        String normalizedEmail = customerEmail == null || customerEmail.isBlank() ? null : customerEmail.trim();
        User currentUser = getCurrentAuthenticatedUserOrNull();
        if (currentUser != null && currentUser instanceof CustomerUser currentCustomer) {
            if (customerId != null && !customerId.equals(currentCustomer.getId())) {
                log.warn("Клиент '{}' пытался запросить заявки другого клиента id={}",
                        currentCustomer.getUsername(), customerId);
                throw new UnauthorizedActionException("Клиент может просматривать только собственные заявки.");
            }
            if (normalizedEmail != null && !normalizedEmail.equalsIgnoreCase(currentCustomer.getEmail())) {
                log.warn("Клиент '{}' пытался запросить заявки по чужому email '{}'",
                        currentCustomer.getUsername(), normalizedEmail);
                throw new UnauthorizedActionException("Клиент может просматривать только собственные заявки.");
            }
            return ticketRepository.searchTickets(status, currentCustomer.getId(), specialistId, null);
        }
        return filterTicketsForCurrentUser(
                ticketRepository.searchTickets(status, customerId, specialistId, normalizedEmail));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> getAllTickets() {
        return filterTicketsForCurrentUser(ticketRepository.findAll());
    }

    @Override
    public Ticket updateTicket(Long id, UpdateTicketRequest request) {
        Ticket ticket = ticketRepository.findByIdForUpdate(id)
                .orElseThrow(() -> TicketNotFoundException.byId(id));
        enforceCustomerOwnershipIfAuthenticated(ticket.getCustomer());
        if (!ticket.isPending()) {
            throw new InvalidTicketStateException(
                    "Изменять можно только заявку в статусе PENDING (текущий статус: " + ticket.getStatus() + ")");
        }
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        Ticket saved = ticketRepository.save(ticket);
        log.info("Обновлена заявка id={}", saved.getId());
        return saved;
    }

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
        enforceSpecialistSelfAssignmentIfAuthenticated(savedSpecialist);

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
        log.info("Заявка id={} назначена специалисту '{}'", ticketId, savedSpecialist.getUsername());
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
        enforceSpecialistOwnershipIfAuthenticated(specialist);

        ticket.complete();
        specialist.decrementActiveTicketCount();
        specialist.registerCompletedTicket(rating);
        ticketRepository.save(ticket);
        userRepository.save(specialist);
        log.info("Заявка id={} завершена специалистом '{}' с оценкой {}",
                ticketId, specialist.getUsername(), rating);
        return true;
    }

    @Override
    public boolean cancelTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findByIdForUpdate(ticketId)
                .orElseThrow(() -> TicketNotFoundException.byId(ticketId));
        enforceTicketParticipantIfAuthenticated(ticket);
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
        log.info("Заявка id={} отменена", ticketId);
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
        enforceSenderMatchesAuthenticatedUserIfPresent(savedSender);

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
        log.info("Добавлено сообщение в заявку id={} от пользователя '{}'", ticketId, savedSender.getUsername());
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
        log.info("Удалена заявка id={}", id);
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

    private User getCurrentAuthenticatedUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        String username = authentication.getName();
        if (username == null || username.isBlank()) {
            return null;
        }
        return userRepository.findByUsernameIgnoreCase(username.trim());
    }

    private void enforceCustomerOwnershipIfAuthenticated(CustomerUser targetCustomer) {
        User currentUser = getCurrentAuthenticatedUserOrNull();
        if (currentUser instanceof CustomerUser currentCustomer) {
            if (targetCustomer.getId() != null && !targetCustomer.getId().equals(currentCustomer.getId())) {
                log.warn("Клиент '{}' пытался выполнить операцию от имени клиента id={}",
                        currentCustomer.getUsername(), targetCustomer.getId());
                throw new UnauthorizedActionException("Клиент может создавать и изменять только собственные заявки.");
            }
        }
    }

    private void enforceSpecialistSelfAssignmentIfAuthenticated(SpecialistUser targetSpecialist) {
        User currentUser = getCurrentAuthenticatedUserOrNull();
        if (currentUser instanceof SpecialistUser currentSpecialist) {
            if (targetSpecialist.getId() != null && !targetSpecialist.getId().equals(currentSpecialist.getId())) {
                log.warn("Специалист '{}' пытался назначить заявку другому специалисту id={}",
                        currentSpecialist.getUsername(), targetSpecialist.getId());
                throw new UnauthorizedActionException("Специалист может назначать заявку только на себя.");
            }
        }
    }

    private void enforceSpecialistOwnershipIfAuthenticated(SpecialistUser assignedSpecialist) {
        User currentUser = getCurrentAuthenticatedUserOrNull();
        if (currentUser instanceof SpecialistUser currentSpecialist) {
            if (assignedSpecialist.getId() != null && !assignedSpecialist.getId().equals(currentSpecialist.getId())) {
                log.warn("Специалист '{}' пытался завершить чужую заявку (назначен '{}')",
                        currentSpecialist.getUsername(), assignedSpecialist.getUsername());
                throw new UnauthorizedActionException("Завершить заявку может только назначенный на неё специалист или администратор.");
            }
        }
    }

    private void enforceTicketParticipantIfAuthenticated(Ticket ticket) {
        User currentUser = getCurrentAuthenticatedUserOrNull();
        if (currentUser == null || currentUser instanceof AdminUser) {
            return;
        }
        boolean isOwner = ticket.getCustomer() != null
                && ticket.getCustomer().getId() != null
                && ticket.getCustomer().getId().equals(currentUser.getId());
        boolean isAssigned = ticket.getSpecialist() != null
                && ticket.getSpecialist().getId() != null
                && ticket.getSpecialist().getId().equals(currentUser.getId());
        if (!isOwner && !isAssigned) {
            log.warn("Пользователь '{}' пытался отменить чужую заявку id={}",
                    currentUser.getUsername(), ticket.getId());
            throw new UnauthorizedActionException("Недостаточно прав для отмены этой заявки.");
        }
    }

    private void enforceTicketReadAccess(Ticket ticket) {
        User currentUser = getCurrentAuthenticatedUserOrNull();
        if (currentUser == null || currentUser instanceof AdminUser) {
            return;
        }
        boolean isCustomerOwner = currentUser instanceof CustomerUser
                && ticket.getCustomer() != null
                && ticket.getCustomer().getId() != null
                && ticket.getCustomer().getId().equals(currentUser.getId());
        boolean isAssignedSpecialist = currentUser instanceof SpecialistUser
                && ticket.getSpecialist() != null
                && ticket.getSpecialist().getId() != null
                && ticket.getSpecialist().getId().equals(currentUser.getId());
        if (!isCustomerOwner && !isAssignedSpecialist) {
            log.warn("Пользователь '{}' пытался просмотреть заявку id={}, к которой не имеет доступа",
                    currentUser.getUsername(), ticket.getId());
            throw new UnauthorizedActionException("Недостаточно прав для просмотра этой заявки.");
        }
    }

    private void enforceSenderMatchesAuthenticatedUserIfPresent(User sender) {
        User currentUser = getCurrentAuthenticatedUserOrNull();
        if (currentUser == null) {
            return;
        }
        if (sender.getId() != null && !sender.getId().equals(currentUser.getId())) {
            log.warn("Пользователь '{}' пытался отправить сообщение от имени пользователя id={}",
                    currentUser.getUsername(), sender.getId());
            throw new UnauthorizedActionException("Нельзя отправлять сообщения от имени другого пользователя.");
        }
    }

    private List<Ticket> filterTicketsForCurrentUser(List<Ticket> tickets) {
        User currentUser = getCurrentAuthenticatedUserOrNull();
        if (currentUser == null || currentUser instanceof AdminUser) {
            return tickets;
        }
        List<Ticket> filtered = new ArrayList<Ticket>();
        for (Ticket ticket : tickets) {
            if (currentUser instanceof CustomerUser) {
                if (ticket.getCustomer() != null && currentUser.getId().equals(ticket.getCustomer().getId())) {
                    filtered.add(ticket);
                }
            } else if (currentUser instanceof SpecialistUser) {
                if (ticket.getSpecialist() != null && currentUser.getId().equals(ticket.getSpecialist().getId())) {
                    filtered.add(ticket);
                }
            }
        }
        return filtered;
    }
}
