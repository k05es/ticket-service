package com.k05es.ticketservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Данные для добавления сообщения в заявку. */
@Getter
@Setter
@NoArgsConstructor
public class CreateMessageRequest {
    @NotNull
    @Positive
    private Long senderId;
    @NotBlank
    @Size(max = 2000)
    private String text;
}