package com.k05es.ticketservice.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/** Единый формат ответа об ошибке REST API с рекомендацией по устранению и деталями. */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        String solution,
        List<String> details
) {
    public ApiError {
        details = details == null ? List.of() : List.copyOf(details);
    }
}
