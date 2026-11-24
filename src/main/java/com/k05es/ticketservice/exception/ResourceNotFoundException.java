package com.k05es.ticketservice.exception;

/** Исключение, возникающее, если запрошенный объект не найден. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}