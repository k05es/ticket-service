package com.k05es.ticketservice.config;

import com.k05es.ticketservice.security.JwtAuthenticationFilter;
import com.k05es.ticketservice.security.JwtService;
import com.k05es.ticketservice.security.RestAccessDeniedHandler;
import com.k05es.ticketservice.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Конфигурация Spring Security:
 * - аутентификация по JWT (Bearer) и HTTP Basic;
 * - разграничение прав по ролям ADMIN, SPECIALIST, CUSTOMER;
 * - операция DELETE разрешена только роли ADMIN.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   UserDetailsService userDetailsService,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                        // Пользователи: просмотр списка, создание, обновление и удаление — только ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/*").authenticated()

                        // Заявки:
                        // DELETE заявок может выполнять только ADMIN
                        .requestMatchers(HttpMethod.DELETE, "/api/tickets/**").hasRole("ADMIN")
                        // Просмотр списка и карточки заявки — любая аутентифицированная роль
                        .requestMatchers(HttpMethod.GET, "/api/tickets/**").authenticated()
                        // Создание и изменение текста заявки до взятия в работу — CUSTOMER и ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/tickets").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/tickets/*").hasAnyRole("CUSTOMER", "ADMIN")
                        // Назначение специалиста и завершение заявки — SPECIALIST и ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/tickets/*/assign/*").hasAnyRole("SPECIALIST", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/tickets/*/complete").hasAnyRole("SPECIALIST", "ADMIN")
                        // Отмена заявки и отправка сообщений — все аутентифицированные роли (доп. проверка в сервисе)
                        .requestMatchers(HttpMethod.POST, "/api/tickets/*/cancel").hasAnyRole("CUSTOMER", "SPECIALIST", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/tickets/*/messages").authenticated()

                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
