package com.k05es.ticketservice.exception;

/** Исключение при попытке назначить заявку недоступному или заблокированному специалисту. */
public class SpecialistUnavailableException extends ResourceConflictException {
    public SpecialistUnavailableException(String message) {
        super(message);
    }
}
