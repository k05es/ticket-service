package com.k05es.ticketservice.concurrent;

import com.k05es.ticketservice.exception.ResourceConflictException;
import com.k05es.ticketservice.exception.ResourceNotFoundException;
import com.k05es.ticketservice.model.SpecialistUser;
import com.k05es.ticketservice.model.Ticket;
import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.model.UserRole;
import com.k05es.ticketservice.service.TicketService;
import com.k05es.ticketservice.service.UserService;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/** Многопоточный обработчик очереди заявок с логированием через SLF4J. */
@Slf4j
public class TicketQueueProcessor {

    private static final long POLL_TIMEOUT_SECONDS = 1;

    private final int workerCount;
    private final TicketService ticketService;
    private final UserService userService;
    private final ExecutorService executor;
    private volatile boolean running = false;
    private final Random random = new Random();
    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger counter = new AtomicInteger(0);
    private final Set<Long> claimedTicketIds = ConcurrentHashMap.newKeySet();

    public TicketQueueProcessor(TicketService ticketService,
                                UserService userService,
                                int workerCount) {
        this.ticketService = ticketService;
        this.userService = userService;
        this.workerCount = workerCount;
        this.executor = Executors.newFixedThreadPool(workerCount, new WorkerFactory());
    }

    public void start() {
        running = true;
        log.info("[Processor] Запуск обработчика, потоков: {}", workerCount);

        for (int i = 0; i < workerCount; i++) {
            executor.submit(new TicketWorker());
        }
    }

    public void stop(int timeoutSeconds) {
        running = false;
        log.info("[Processor] Остановка.");

        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                log.warn("[Processor] Принудительное завершение потоков.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }

        log.info("[Processor] Обработано заявок: {}", processedCount.get());
    }

    public boolean isQueueEmpty() {
        return !ticketService.hasPendingTickets();
    }

    private static class WorkerFactory implements ThreadFactory {

        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("TicketWorker-" + counter.getAndIncrement());
            return thread;
        }
    }

    private class TicketWorker implements Runnable {

        @Override
        public void run() {
            String name = Thread.currentThread().getName();
            log.info("[{}] Стартовал.", name);

            while (running) {
                SpecialistUser specialist = findAvailableSpecialist();
                if (specialist == null) {
                    sleep(500L);
                    continue;
                }
                Ticket ticket = takeTicket();
                if (ticket != null) {
                    processTicket(ticket, specialist, name);
                }
            }

            log.info("[{}] Завершил работу.", name);
        }

        private Ticket takeTicket() {
            try {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(POLL_TIMEOUT_SECONDS);
                do {
                    Ticket candidate = ticketService.pollNextTicket(
                            POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS, claimedTicketIds);
                    if (candidate == null) {
                        return null;
                    }
                    if (claimedTicketIds.add(candidate.getId())) {
                        return candidate;
                    }
                } while (running && System.nanoTime() < deadline);
                return null;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        private void processTicket(Ticket ticket, SpecialistUser specialist, String workerName) {
            log.info("[{}] Взял заявку #{}: "{}" (заказчик: {})",
                    workerName, ticket.getId(), ticket.getTitle(), ticket.getCustomer().getUsername());

            boolean assigned = false;
            try {
                assigned = ticketService.assignTicketToSpecialist(ticket.getId(), specialist);
            } catch (ResourceConflictException | ResourceNotFoundException exception) {
                log.warn("[{}] Не удалось назначить заявку #{} специалисту {}: {}",
                        workerName, ticket.getId(), specialist.getUsername(), exception.getMessage());
                claimedTicketIds.remove(ticket.getId());
                return;
            } catch (RuntimeException exception) {
                log.error("[{}] Ошибка при назначении заявки #{} специалисту {}",
                        workerName, ticket.getId(), specialist.getUsername(), exception);
                claimedTicketIds.remove(ticket.getId());
                return;
            }

            if (!assigned) {
                claimedTicketIds.remove(ticket.getId());
                return;
            }

            try {
                log.info("[{}] Назначен специалист {} на заявку #{}",
                        workerName, specialist.getUsername(), ticket.getId());

                sleep(500 + random.nextInt(500));

                if (ticketService.addMessageToTicket(
                        ticket.getId(),
                        ticket.getCustomer(),
                        "Спасибо за оперативное решение!")) {
                    log.info("[{}] Добавлено сообщение в заявку #{}", workerName, ticket.getId());
                }

                double rating = 4.0 + random.nextInt(11) / 10.0;
                if (ticketService.completeTicket(ticket.getId(), rating)) {
                    log.info("[{}] Завершил заявку #{} ({} - оценка {})",
                            workerName, ticket.getId(), specialist.getUsername(),
                            String.format("%.1f", rating));
                    processedCount.incrementAndGet();
                }
            } finally {
                claimedTicketIds.remove(ticket.getId());
            }
        }

        private SpecialistUser findAvailableSpecialist() {
            List<User> specialists = userService.findUsersByRole(UserRole.SPECIALIST);
            List<SpecialistUser> availableSpecialists = specialists.stream()
                    .filter(SpecialistUser.class::isInstance)
                    .map(SpecialistUser.class::cast)
                    .filter(SpecialistUser::canTakeMoreTickets)
                    .toList();
            if (availableSpecialists.isEmpty()) {
                return null;
            }
            int index = Math.floorMod(counter.getAndIncrement(), availableSpecialists.size());
            return availableSpecialists.get(index);
        }

        private void sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
