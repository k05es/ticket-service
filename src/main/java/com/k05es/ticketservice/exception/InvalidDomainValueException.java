package com.k05es.ticketservice.exception;

/** Исключение при нарушении доменных ограничений полей модели (оценка, телефон, заголовок, описание). */
public class InvalidDomainValueException extends InvalidRequestException {
    public InvalidDomainValueException(String message) {
        super(message);
    }
}
