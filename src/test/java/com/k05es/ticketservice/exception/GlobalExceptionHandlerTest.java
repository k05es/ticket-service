package com.k05es.ticketservice.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Отсутствующий ресурс преобразуется в HTTP 404")
    void notFoundMapsTo404() {
        assertEquals(HttpStatus.NOT_FOUND,
                handler.handleNotFound(new ResourceNotFoundException("Не найдено")).getStatusCode());
    }

    @Test
    @DisplayName("Конфликт бизнес-правил преобразуется в HTTP 409")
    void conflictMapsTo409() {
        assertEquals(HttpStatus.CONFLICT,
                handler.handleConflict(new ResourceConflictException("Конфликт")).getStatusCode());
    }

    @Test
    @DisplayName("Некорректный аргумент преобразуется в HTTP 400")
    void badRequestMapsTo400() {
        assertEquals(HttpStatus.BAD_REQUEST,
                handler.handleBadRequest(new IllegalArgumentException("Некорректно" )).getStatusCode());
    }

    @Test
    @DisplayName("Недопустимое состояние ресурса преобразуется в HTTP 409")
    void invalidStateMapsTo409() {
        assertEquals(HttpStatus.CONFLICT,
                handler.handleInvalidState(new IllegalStateException("Недопустимый статус")).getStatusCode());
    }
}