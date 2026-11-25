package com.k05es.ticketservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Проверяет заголовок Authorization: Bearer <token> и устанавливает аутентификацию в SecurityContext. */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            Optional<String> username = jwtService.extractUsername(token);
            if (username.isPresent()) {
                try {
                    UserDetails details = userDetailsService.loadUserByUsername(username.get());
                    if (details.isEnabled()) {
                        UsernamePasswordAuthenticationToken authentication =
                                UsernamePasswordAuthenticationToken.authenticated(
                                        details, null, details.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("JWT-аутентификация успешна для пользователя '{}'", username.get());
                    } else {
                        log.warn("Заблокированный пользователь '{}' пытался использовать JWT", username.get());
                    }
                } catch (UsernameNotFoundException exception) {
                    log.warn("JWT выдан несуществующему пользователю '{}'", username.get());
                }
            } else {
                log.warn("Получен неверный или просроченный JWT при запросе {} {}",
                        request.getMethod(), request.getRequestURI());
            }
        }
        filterChain.doFilter(request, response);
    }
}
