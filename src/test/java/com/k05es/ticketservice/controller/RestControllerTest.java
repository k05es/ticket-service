package com.k05es.ticketservice.controller;

import com.k05es.ticketservice.exception.ResourceNotFoundException;
import com.k05es.ticketservice.mapper.TicketMapper;
import com.k05es.ticketservice.mapper.UserMapper;
import com.k05es.ticketservice.model.TicketStatus;
import com.k05es.ticketservice.service.TicketService;
import com.k05es.ticketservice.service.UserService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP-тесты REST-контроллеров без подключения к PostgreSQL. */
@WebMvcTest(controllers = {TicketController.class, UserController.class})
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

    @Test
    @DisplayName("GET заявки с отсутствующим ID возвращает HTTP 404")
    void getMissingTicketReturnsNotFound() throws Exception {
        when(ticketService.findTicketById(404L))
                .thenThrow(new ResourceNotFoundException("Заявка не найдена"));

        mockMvc.perform(get("/api/tickets/404"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET пользователя с отсутствующим ID возвращает HTTP 404")
    void getMissingUserReturnsNotFound() throws Exception {
        when(userService.findUserById(404L))
                .thenThrow(new ResourceNotFoundException("Пользователь не найден"));

        mockMvc.perform(get("/api/users/404"))
                .andExpect(status().isNotFound());
    }

    @Test
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
    @DisplayName("POST заявки с некорректными данными возвращает HTTP 400")
    void invalidTicketRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType("application/json")
                        .content("{\"title\":\"\",\"description\":\"\",\"customerId\":0}"))
                .andExpect(status().isBadRequest());

        verify(ticketService, never()).createTicket(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }
}