package com.k05es.ticketservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Данные для изменения заявки. */
@Getter
@Setter
@NoArgsConstructor
public class UpdateTicketRequest {
    @NotBlank
    @Size(max = 50)
    private String title;
    @NotBlank
    @Size(max = 300)
    private String description;
}