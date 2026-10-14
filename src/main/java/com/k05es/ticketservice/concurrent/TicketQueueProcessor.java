package com.k05es.ticketservice.concurrent;

import com.k05es.ticketservice.model.SpecialistUser;
import com.k05es.ticketservice.model.Ticket;
import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.model.UserRole;
import com.k05es.ticketservice.service.TicketService;
import com.k05es.ticketservice.service.UserService;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Многопоточный обработчик очереди заявок.
 */
public class TicketQueueProcessor {

    // Сколько секунд ждать заявку из очереди
    private static final long POLL_TIMEOUT_SECONDS = 1;

    // Сколько потоков работает одновременно
    private final int workerCount;

    // Сервис заявок
    private final TicketService ticketService;

    // Сервис пользователей
    private final UserService userService;

    // Пул потоков
    private final ExecutorService executor;

    // Флаг работы
    private volatile boolean running = false;

    // Генератор случайных задержек и оценок
    private final Random random = new Random();

    // Сколько заявок обработано
    private final AtomicInteger processedCount = new AtomicInteger(0);

    // Счётчик для чередования специалистов (round-robin)
    private final AtomicInteger counter = new AtomicInteger(0);

    public TicketQueueProcessor(TicketService ticketService,
                                UserService userService,
                                int workerCount) {
        this.ticketService = ticketService;
        this.userService = userService;
        this.workerCount = workerCount;
        this.executor = Executors.newFixedThreadPool(workerCount, new WorkerFactory());
    }

    // Запускает воркеры
    public void start() {
        running = true;
        System.out.println("[Processor] Запуск обработчика, потоков: " + workerCount);

        for (int i = 0; i < workerCount; i++) {
            executor.submit(new TicketWorker());
        }
    }

    // Останавливает воркеры
    public void stop(int timeoutSeconds) {
        running = false;
        System.out.println("[Processor] Остановка.");

        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                System.out.println("[Processor] Принудительное завершение потоков.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }

        System.out.println("[Processor] Обработано заявок: " + processedCount.get());
    }

    // Проверяет, что очередь пуста
    public boolean isQueueEmpty() {
        return !ticketService.hasPendingTickets();
    }

    // Фабрика потоков с именами TicketWorker-1, TicketWorker-2, ...
    private static class WorkerFactory implements ThreadFactory {

        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("TicketWorker-" + counter.getAndIncrement());
            return thread;
        }
    }

    // Задача одного воркера
    private class TicketWorker implements Runnable {

        @Override
        public void run() {
            String name = Thread.currentThread().getName();
            System.out.println("[" + name + "] Стартовал.");

            while (running) {
                Ticket ticket = takeTicket();
                if (ticket != null) {
                    processTicket(ticket, name);
                }
            }

            System.out.println("[" + name + "] Завершил работу.");
        }

        // Берёт заявку из очереди
        private Ticket takeTicket() {
            try {
                return ticketService.pollNextTicket(POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        // Обрабатывает заявку
        private void processTicket(Ticket ticket, String workerName) {
            System.out.println("[" + workerName + "] Взял заявку #" + ticket.getId()
                    + ": \"" + ticket.getTitle() + "\""
                    + " (заказчик: " + ticket.getCustomer().getUsername() + ")");

            SpecialistUser specialist = findAvailableSpecialist();
            if (specialist == null) {
                ticketService.returnToQueue(ticket);
                return;
            }

            if (!ticketService.assignTicketToSpecialist(ticket.getId(), specialist)) {
                return;
            }

            System.out.println("[" + workerName + "] Назначен специалист "
                    + specialist.getUsername() + " на заявку #" + ticket.getId());

            // Имитация работы: 500–1000 мс
            sleep(500 + random.nextInt(500));

            // Клиент оставляет сообщение, пока заявка в работе
            if (ticketService.addMessageToTicket(
                    ticket.getId(),
                    ticket.getCustomer(),
                    "Спасибо за оперативное решение!")) {
                System.out.println("[" + workerName + "] Добавлено сообщение в заявку #"
                        + ticket.getId());
            }

            // Оценка от 4.0 до 5.0
            double rating = 4.0 + random.nextInt(11) / 10.0;
            if (ticketService.completeTicket(ticket.getId(), rating)) {
                System.out.println("[" + workerName + "] Завершил заявку #" + ticket.getId()
                        + " (" + specialist.getUsername() + " - оценка "
                        + String.format("%.1f", rating) + ")");
                processedCount.incrementAndGet();
            }
        }

        // Выбирает специалиста по кругу (round-robin).
        // Гарантирует, что оба специалиста получат заявки.
        private SpecialistUser findAvailableSpecialist() {
            List<User> specialists = userService.findUsersByRole(UserRole.SPECIALIST);
            if (specialists.isEmpty()) {
                return null;
            }
            int index = counter.getAndIncrement() % specialists.size();
            return (SpecialistUser) specialists.get(index);
        }

        // Спит без выброса исключения
        private void sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}