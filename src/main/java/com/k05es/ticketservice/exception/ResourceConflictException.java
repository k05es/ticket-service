package com.k05es.ticketservice.exception;

/** Исключение конфликта данных или нарушения бизнес-правила. */
public class ResourceConflictException extends RuntimeException {
    public ResourceConflictException(String message) {
        super(message);
    }
}
