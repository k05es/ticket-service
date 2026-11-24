package com.k05es.ticketservice.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Оценка клиента при завершении заявки. */
@Getter
@Setter
@NoArgsConstructor
public class CompleteTicketRequest {
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("5.0")
    private Double rating;
}