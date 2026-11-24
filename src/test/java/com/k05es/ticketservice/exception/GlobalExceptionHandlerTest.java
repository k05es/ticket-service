package com.k05es.ticketservice.exception;

import com.k05es.ticketservice.dto.response.ApiError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Отсутствующая заявка преобразуется в HTTP 404 с рекомендацией по решению")
    void ticketNotFoundMapsTo404WithSolution() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tickets/404");
        ResponseEntity<ApiError> response = handler.handleTicketNotFound(
                TicketNotFoundException.byId(404L), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("/api/tickets/404", response.getBody().path());
        assertFalse(response.getBody().solution().isBlank());
    }

    @Test
    @DisplayName("Отсутствующий пользователь преобразуется в HTTP 404")
    void userNotFoundMapsTo404() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/404");
        ResponseEntity<ApiError> response = handler.handleUserNotFound(
                UserNotFoundException.byId(404L), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
    }

    @Test
    @DisplayName("Дубликат ресурса преобразуется в HTTP 409")
    void duplicateResourceMapsTo409() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        ResponseEntity<ApiError> response = handler.handleDuplicateResource(
                new DuplicateResourceException("Логин уже занят"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().solution().isBlank());
    }

    @Test
    @DisplayName("Зависимости пользователя преобразуются в HTTP 409")
    void userDependenciesMapsTo409() {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/users/1");
        ResponseEntity<ApiError> response = handler.handleUserDependencies(
                new UserHasDependenciesException("Есть связанные заявки"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
    }

    @Test
    @DisplayName("Удаление завершённой заявки преобразуется в HTTP 409")
    void completedTicketDeletionMapsTo409() {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/tickets/20");
        ResponseEntity<ApiError> response = handler.handleCompletedTicketDeletion(
                new CompletedTicketDeletionException("Заявка завершена"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
    }

    @Test
    @DisplayName("Недоступный специалист преобразуется в HTTP 409")
    void specialistUnavailableMapsTo409() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/tickets/20/assign/2");
        ResponseEntity<ApiError> response = handler.handleSpecialistUnavailable(
                new SpecialistUnavailableException("Специалист недоступен"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
        assertFalse(response.getBody().solution().isBlank());
    }

    @Test
    @DisplayName("Недопустимый статус заявки преобразуется в HTTP 409")
    void invalidTicketStateMapsTo409() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/tickets/20/complete");
        ResponseEntity<ApiError> response = handler.handleInvalidTicketState(
                new InvalidTicketStateException("Недопустимый статус"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
    }

    @Test
    @DisplayName("Недостаточно прав или неактивный статус преобразуется в HTTP 403")
    void unauthorizedActionMapsTo403() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/tickets/20/messages");
        ResponseEntity<ApiError> response = handler.handleUnauthorizedAction(
                new UnauthorizedActionException("Недостаточно прав"), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().status());
        assertFalse(response.getBody().solution().isBlank());
    }

    @Test
    @DisplayName("Нарушение доменного значения преобразуется в HTTP 400")
    void invalidDomainValueMapsTo400() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/tickets/20/complete");
        ResponseEntity<ApiError> response = handler.handleInvalidDomainValue(
                new InvalidDomainValueException("Оценка должна быть от 0.0 до 5.0"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertFalse(response.getBody().solution().isBlank());
    }

    @Test
    @DisplayName("Некорректный запрос преобразуется в HTTP 400")
    void invalidRequestMapsTo400() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/tickets");
        ResponseEntity<ApiError> response = handler.handleInvalidRequest(
                new InvalidRequestException("Некорректные данные"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
    }
}
