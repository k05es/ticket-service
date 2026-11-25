package com.k05es.ticketservice.exception;

/** Базовое исключение для случаев, когда запрошенный ресурс не найден. */
public abstract class ResourceNotFoundException extends RuntimeException {
    protected ResourceNotFoundException(String message) {
        super(message);
    }
}
