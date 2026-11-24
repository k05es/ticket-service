package com.k05es.ticketservice;

import com.k05es.ticketservice.concurrent.TicketQueueProcessor;
import com.k05es.ticketservice.model.AdminUser;
import com.k05es.ticketservice.model.ContactPreference;
import com.k05es.ticketservice.model.CustomerUser;
import com.k05es.ticketservice.model.Department;
import com.k05es.ticketservice.model.SpecialistUser;
import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.model.UserRole;
import com.k05es.ticketservice.model.UserStatus;
import com.k05es.ticketservice.service.LocalTicketService;
import com.k05es.ticketservice.service.LocalUserService;
import com.k05es.ticketservice.service.TicketService;
import com.k05es.ticketservice.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.HashSet;

@SpringBootApplication
public class TicketServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketServiceApplication.class, args);
    }

    // Демонстрационный сценарий, запускается после старта Spring Boot
    @Bean
    public CommandLineRunner demoRunner() {
        return args -> runDemo();
    }

    // Основной сценарий демонстрации
    private void runDemo() {
        System.out.println("\n========== ДЕМОНСТРАЦИЯ ==========\n");

        // 1. Создаём сервисы
        UserService userService = new LocalUserService();
        TicketService ticketService = new LocalTicketService();

        // 2. Регистрируем пользователей
        userService.registerUser(createAdmin("admin",
                "Кузнецов", "Андрей", "Петрович"));

        userService.registerUser(createSpecialist("ivan_spec",
                "Смирнов", "Иван", "Сергеевич"));

        userService.registerUser(createSpecialist("petr_spec",
                "Петров", "Пётр", "Андреевич"));

        CustomerUser client1 = (CustomerUser) userService.registerUser(createCustomer("anna",
                "Иванова", "Анна", "Александровна"));

        CustomerUser client2 = (CustomerUser) userService.registerUser(createCustomer("oleg",
                "Соколов", "Олег", "Викторович"));

        System.out.println("Пользователи:");
        for (User user : userService.getAllUsers()) {
            System.out.println("  " + user.getDisplayName());
        }
        System.out.println();

        // 3. Создаём заявки — они попадут в очередь
        ticketService.createTicket("Принтер не печатает",
                "При отправке на печать выдаёт ошибку 501", client1);

        ticketService.createTicket("Пропал интернет",
                "Wi-Fi подключён, но сайты не открываются", client2);

        ticketService.createTicket("Медленный ПК",
                "Загрузка программ занимает больше минуты", client1);

        ticketService.createTicket("Не приходят письма",
                "Письма на внешние адреса уходят, а входящие не приходят", client2);

        ticketService.createTicket("Звук пропал",
                "Динамики работают, но звука нет", client1);

        ticketService.createTicket("Не запускается 1С",
                "После обновления Windows 1С вылетает с ошибкой", client2);

        System.out.println("Создано заявок: " + ticketService.getAllTickets().size());
        System.out.println();

        // 3.1 Добавляем сообщение от клиента в первую заявку
        ticketService.addMessageToTicket(
                ticketService.getAllTickets().get(0).getId(),
                client1,
                "Спасибо за оперативное решение!"
        );
        System.out.println("Добавлено сообщение в заявку #"
                + ticketService.getAllTickets().get(0).getId());
        System.out.println();

        // 4. Запускаем многопоточный обработчик на 3 потока
        TicketQueueProcessor processor = new TicketQueueProcessor(ticketService, userService, 3);
        processor.start();

        // 5. Ждём, пока очередь опустеет
        while (!processor.isQueueEmpty()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 6. Останавливаем процессор
        processor.stop(5);

        // 7. Итоги
        System.out.println("\n========== ИТОГИ ==========");
        System.out.println("Специалисты:");
        for (User user : userService.findUsersByRole(UserRole.SPECIALIST)) {
            SpecialistUser spec = (SpecialistUser) user;
            System.out.printf("  %s — завершено: %d, рейтинг: %.2f%n",
                    spec.getUsername(),
                    spec.getCompletedTicketCount(),
                    spec.getAverageRating());
        }
        System.out.println("\n========== КОНЕЦ ==========\n");
    }

    // Создаёт администратора
    private AdminUser createAdmin(String username,
                                  String lastName, String firstName, String middleName) {
        return new AdminUser(
                null, username, username + "@ticket.ru", "hash",
                UserStatus.ACTIVE,
                lastName, firstName, middleName,
                new HashSet<>(),
                Department.TECHNICAL_SUPPORT,
                new HashSet<>()
        );
    }

    // Создаёт специалиста
    private SpecialistUser createSpecialist(String username,
                                            String lastName, String firstName, String middleName) {
        return new SpecialistUser(
                null, username, username + "@ticket.ru", "hash",
                UserStatus.ACTIVE,
                lastName, firstName, middleName,
                0.0, 0, 0, 0, true,
                Department.TECHNICAL_SUPPORT
        );
    }

    // Создаёт клиента
    private CustomerUser createCustomer(String username,
                                        String lastName, String firstName, String middleName) {
        return new CustomerUser(
                null, username, username + "@ticket.ru", "hash",
                UserStatus.ACTIVE,
                lastName, firstName, middleName,
                "+79001234567",
                "ООО Ромашка",
                "Москва",
                ContactPreference.EMAIL,
                0
        );
    }
}