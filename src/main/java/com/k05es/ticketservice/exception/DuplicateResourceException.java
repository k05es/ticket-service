package com.k05es.ticketservice.exception;

/** Исключение при попытке создать или обновить ресурс с уже занятым уникальным значением. */
public class DuplicateResourceException extends ResourceConflictException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
