package com.k05es.ticketservice.exception;

/** Исключение при попытке удалить завершённую заявку, участвующую в статистике. */
public class CompletedTicketDeletionException extends ResourceConflictException {
    public CompletedTicketDeletionException(String message) {
        super(message);
    }
}
