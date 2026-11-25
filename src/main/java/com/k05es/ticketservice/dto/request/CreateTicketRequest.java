package com.k05es.ticketservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Данные для создания заявки. */
@Getter
@Setter
@NoArgsConstructor
public class CreateTicketRequest {
    @NotBlank
    @Size(max = 50)
    private String title;
    @NotBlank
    @Size(max = 300)
    private String description;
    @NotNull
    @Positive
    private Long customerId;
}