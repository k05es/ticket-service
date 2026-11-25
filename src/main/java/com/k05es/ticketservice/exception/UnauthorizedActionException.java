package com.k05es.ticketservice.exception;

/** Исключение при попытке выполнить действие без необходимых прав или активного статуса. */
public class UnauthorizedActionException extends RuntimeException {
    public UnauthorizedActionException(String message) {
        super(message);
    }
}
