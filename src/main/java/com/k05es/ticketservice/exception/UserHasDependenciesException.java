package com.k05es.ticketservice.exception;

/** Исключение при попытке удалить пользователя, связанного с заявками, сообщениями или администраторами. */
public class UserHasDependenciesException extends ResourceConflictException {
    public UserHasDependenciesException(String message) {
        super(message);
    }
}
