package com.k05es.ticketservice.exception;

/** Исключение конфликта данных, например при повторном логине. */
public class ResourceConflictException extends RuntimeException {
    public ResourceConflictException(String message) { super(message); }
}