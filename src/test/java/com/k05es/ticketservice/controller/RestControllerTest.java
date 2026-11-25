package com.k05es.ticketservice.controller;

import com.k05es.ticketservice.config.SecurityConfig;
import com.k05es.ticketservice.exception.GlobalExceptionHandler;
import com.k05es.ticketservice.exception.TicketNotFoundException;
import com.k05es.ticketservice.exception.UserNotFoundException;
import com.k05es.ticketservice.mapper.TicketMapper;
import com.k05es.ticketservice.mapper.UserMapper;
import com.k05es.ticketservice.model.TicketStatus;
import com.k05es.ticketservice.security.JwtService;
import com.k05es.ticketservice.security.RestAccessDeniedHandler;
import com.k05es.ticketservice.security.RestAuthenticationEntryPoint;
import com.k05es.ticketservice.service.TicketService;
import com.k05es.ticketservice.service.UserService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP-тесты REST-контроллеров и ролевого доступа Spring Security без подключения к PostgreSQL. */
@WebMvcTest(controllers = {TicketController.class, UserController.class})
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class RestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TicketMapper ticketMapper;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("Запрос без токена возвращает HTTP 401 и формат ApiError")
    void unauthenticatedRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.solution").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "client1", roles = "CUSTOMER")
    @DisplayName("CUSTOMER не может удалять заявки (HTTP 403)")
    void customerCannotDeleteTicketReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/tickets/20"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.solution").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "specialist1", roles = "SPECIALIST")
    @DisplayName("SPECIALIST не может удалять заявки (HTTP 403)")
    void specialistCannotDeleteTicketReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/tickets/20"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("ADMIN может удалить заявку (HTTP 204)")
    void adminCanDeleteTicketReturnsNoContent() throws Exception {
        when(ticketService.deleteTicket(20L)).thenReturn(true);

        mockMvc.perform(delete("/api/tickets/20"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "client1", roles = "CUSTOMER")
    @DisplayName("CUSTOMER не имеет доступа к списку пользователей ADMIN (HTTP 403)")
    void customerCannotListUsersReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("GET заявки с отсутствующим ID возвращает HTTP 404 и поле solution")
    void getMissingTicketReturnsNotFound() throws Exception {
        when(ticketService.findTicketById(404L))
                .thenThrow(TicketNotFoundException.byId(404L));

        mockMvc.perform(get("/api/tickets/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.solution").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("GET пользователя с отсутствующим ID возвращает HTTP 404 и поле solution")
    void getMissingUserReturnsNotFound() throws Exception {
        when(userService.findUserById(404L))
                .thenThrow(UserNotFoundException.byId(404L));

        mockMvc.perform(get("/api/users/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.solution").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "client1", roles = "CUSTOMER")
    @DisplayName("GET заявок передаёт фильтры сервисному слою")
    void ticketSearchPassesQueryFilters() throws Exception {
        when(ticketService.searchTickets(TicketStatus.PENDING, 7L, null, null))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/tickets")
                        .param("status", "PENDING")
                        .param("customerId", "7"))
                .andExpect(status().isOk());

        verify(ticketService).searchTickets(TicketStatus.PENDING, 7L, null, null);
    }

    @Test
    @WithMockUser(username = "client1", roles = "CUSTOMER")
    @DisplayName("POST заявки с некорректными данными возвращает HTTP 400 и детали")
    void invalidTicketRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType("application/json")
                        .content("{\"title\":\"\",\"description\":\"\",\"customerId\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.solution").isNotEmpty());

        verify(ticketService, never()).createTicket(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any());
    }
}
