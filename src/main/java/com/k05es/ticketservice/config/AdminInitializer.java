package com.k05es.ticketservice.config;

import com.k05es.ticketservice.model.AdminUser;
import com.k05es.ticketservice.model.Department;
import com.k05es.ticketservice.model.Permission;
import com.k05es.ticketservice.model.UserStatus;
import com.k05es.ticketservice.repository.UserRepository;
import com.k05es.ticketservice.service.UserService;
import java.util.EnumSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Создаёт администратора по умолчанию при старте приложения, если он ещё не существует в БД. */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.security.admin.init", havingValue = "true", matchIfMissing = true)
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;
    private final String username;
    private final String email;
    private final String password;

    public AdminInitializer(UserRepository userRepository,
                            UserService userService,
                            @Value("${app.security.admin.username}") String username,
                            @Value("${app.security.admin.email}") String email,
                            @Value("${app.security.admin.password}") String password) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            log.info("Администратор по умолчанию '{}' уже существует, инициализация пропущена", username);
            return;
        }
        AdminUser admin = new AdminUser(
                null,
                username,
                email,
                password,
                UserStatus.ACTIVE,
                "Системный",
                "Администратор",
                null,
                EnumSet.allOf(Permission.class),
                Department.GENERAL_SUPPORT,
                Set.of()
        );
        userService.registerUser(admin);
        log.info("Создан администратор по умолчанию '{}'", username);
    }
}
