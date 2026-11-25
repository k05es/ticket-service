package com.k05es.ticketservice.exception;

/** Исключение, возникающее, если пользователь не найден. */
public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(String message) {
        super(message);
    }

    public static UserNotFoundException byId(Long id) {
        return new UserNotFoundException("Пользователь с ID " + id + " не найден.");
    }
}
