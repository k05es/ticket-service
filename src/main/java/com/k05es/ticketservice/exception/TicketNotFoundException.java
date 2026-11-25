package com.k05es.ticketservice.exception;

/** Исключение, возникающее, если заявка не найдена. */
public class TicketNotFoundException extends ResourceNotFoundException {
    public TicketNotFoundException(String message) {
        super(message);
    }

    public static TicketNotFoundException byId(Long id) {
        return new TicketNotFoundException("Заявка с ID " + id + " не найдена.");
    }
}
