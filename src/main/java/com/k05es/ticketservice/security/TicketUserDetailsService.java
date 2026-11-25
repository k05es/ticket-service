package com.k05es.ticketservice.security;

import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Загружает пользователей иерархии User из БД и назначает роль ROLE_{UserRole}. */
@Slf4j
@Service
public class TicketUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public TicketUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalized = username == null ? "" : username.trim();
        User user = userRepository.findByUsernameIgnoreCase(normalized);
        if (user == null) {
            log.warn("Попытка аутентификации несуществующего пользователя '{}'", username);
            throw new UsernameNotFoundException("Пользователь не найден: " + username);
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassHash())
                .roles(user.getRole().name())
                .disabled(!user.isActive())
                .build();
    }
}
