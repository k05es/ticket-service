package com.k05es.ticketservice.exception;

/** Исключение при некорректных входных данных или параметрах запроса. */
public class InvalidRequestException extends IllegalArgumentException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
