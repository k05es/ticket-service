package com.k05es.ticketservice.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Данные сообщения для REST-ответа. */
@Getter
@AllArgsConstructor
public class MessageResponse {
    private Long id;
    private String text;
    private Long senderId;
    private String senderUsername;
    private LocalDateTime sentAt;
}