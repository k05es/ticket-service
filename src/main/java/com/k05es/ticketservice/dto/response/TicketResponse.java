package com.k05es.ticketservice.dto.response;

import com.k05es.ticketservice.model.TicketStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Данные заявки для REST-ответа. */
@Getter
@AllArgsConstructor
public class TicketResponse {
    private Long id;
    private String title;
    private String description;
    private TicketStatus status;
    private Long customerId;
    private String customerUsername;
    private Long specialistId;
    private String specialistUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MessageResponse> messages;
}