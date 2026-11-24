package com.k05es.ticketservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.k05es.ticketservice.dto.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/** Формирует единый JSON-ответ ApiError для ошибок аутентификации и авторизации Spring Security. */
final class JsonErrorWriter {

    private static final ObjectMapper OBJECT_MAPPER = createMapper();

    private JsonErrorWriter() {
    }

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    static void write(HttpServletRequest request,
                      HttpServletResponse response,
                      HttpStatus status,
                      String message,
                      String solution) throws IOException {
        ApiError body = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                solution,
                List.of()
        );
        String json = OBJECT_MAPPER.writeValueAsString(body);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(json);
    }
}
