package com.k05es.ticketservice.exception;

/** Исключение при недопустимом статусе заявки для запрошенной операции. */
public class InvalidTicketStateException extends ResourceConflictException {
    public InvalidTicketStateException(String message) {
        super(message);
    }
}
