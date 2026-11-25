package com.k05es.ticketservice.exception;

import com.k05es.ticketservice.dto.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Централизованно обрабатывает исключения и возвращает структурированный ответ ApiError
 * с описанием проблемы, рекомендацией по её устранению и деталями.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ApiError> handleTicketNotFound(TicketNotFoundException exception,
                                                         HttpServletRequest request) {
        log.warn("404 Not Found [{}]: {}", request.getRequestURI(), exception.getMessage());
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage(),
                "Проверьте идентификатор заявки и убедитесь, что заявка существует.",
                List.of(), request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException exception,
                                                       HttpServletRequest request) {
        log.warn("404 Not Found [{}]: {}", request.getRequestURI(), exception.getMessage());
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage(),
                "Проверьте идентификатор пользователя или создайте учётную запись через POST /api/users.",
                List.of(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException exception,
                                                   HttpServletRequest request) {
        log.warn("404 Not Found [{}]: {}", request.getRequestURI(), exception.getMessage());
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage(),
                "Проверьте идентификатор ресурса и убедитесь, что запись существует.",
                List.of(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException exception,
                                                         HttpServletRequest request) {
        log.warn("401 Unauthorized [{}]: {}", request.getRequestURI(), exception.getMessage());
        return buildError(HttpStatus.UNAUTHORIZED, exception.getMessage(),
                "Проверьте правильность логина и пароля и повторите вход через POST /api/auth/login.",
                List.of(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception,
                                                       HttpServletRequest request) {
        log.warn("403 Forbidden [{}]: {}", request.getRequestURI(), exception.getMessage());
        return buildError(HttpStatus.FORBIDDEN,
                "Недостаточно прав для выполнения данной операции.",
                "Выполните вход под пользователем с подходящей ролью (например, ADMIN).",
                List.of(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicateResource(DuplicateResourceException exception,
                                                          HttpServletRequest request) {
        log.warn("409 Conflict [{}]: {}", request.getRequestURI(), exception.getMessage());
        return buildError(HttpStatus.CONFLICT, exception.getMessage(),
                "Укажите уникальное значение логина или email либо обновите существующего пользователя.",
                List.of(), request);
    }

    @ExceptionHandler(UserHasDependenciesException.class)
    public ResponseEntity<ApiError> handleUserDependencies(UserHasDependenciesException exception,
                                                         HttpServletRequest request) {
        log.warn("409 Conflict [{}]: {}", request.getRequestURI(), exception.getMessage());
        return buildError(HttpStatus.CONFLICT, exception.getMessage(),
                "Сначала завершите или удалите связанные заявки, сообщения либо снимите закрепление за специалистами.",
                List.of(), request);
    }

    @ExceptionHandler(CompletedTicketDeletionException.class)
    public ResponseEntity<ApiError> handleCompletedTicketDeletion(CompletedTicketDeletionException exception,
                                                                HttpServletRequest request) {
        log.warn("409 Conflict [{}]: {}", request.getRequestURI(), exception.getMessage());
        return buildError(HttpStatus.CONFLICT, exception.getMessage(),
                "Завершённые заявки сохраняются для статистики специалистов. Вместо удаления используйте поиск и фильтрацию по статусу COMPLETED.",
                List.of(), request);
    }

    @ExceptionHandler(SpecialistUnavailableException.class)
    public ResponseEntity<ApiError> handleSpecialistUnavailable(SpecialistUnavailableException exception,
                                                                  HttpServletRequest request) {
        log.warn("409 Conflict [{}]: {}", request.getRequestURI(), exception.getMessage());
        return buildError(HttpStatus.CONFLICT, exception.getMessage(),
                "Выберите другого активного специалиста с доступным статусом или переведите выбранного специалиста в доступное состояние.",
                List.of(), request);
    }

    @ExceptionHandler(InvalidTicketStateException.class)
    public ResponseEntity<ApiError> handleInvalidTicketState(InvalidTicketStateException exception,
                                                             HttpServletRequest request) {
        log.warn("409 Conflict [{}]: {}", request.getRequestURI(), exception.getMessage());
        return buildError(HttpStatus.CONFLICT, exception.getMessage(),
                "Проверьте текущий статус заявки (PENDING, IN_PROGRESS, COMPLETED, CANCELLED) и допустимость перехода для этого действия.",
                List.of(), request);
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ApiError> handleUnauthorizedAction(UnauthorizedActionException exception,
                                                             HttpServletRequest request) {
        log.warn("403 Forbidden [{}]: {}", request.getRequestURI(), exception.getMessage());
        return buildError(HttpStatus.FORBIDDEN, exception.getMessage(),
                "Убедитесь, что учётная запись активна (статус ACTIVE) и пользователь имеет право выполнять действие с заявкой (например, право MANAGE_TICKETS для администратора).",
                List.of(), request);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ResourceConflictException exception,
                                                   HttpServletRequest request) {
        log.warn("409 Conflict [{}]: {}", request.getRequestURI(), exception.getMessage());
        return buildError(HttpStatus.CONFLICT, exception.getMessage(),
                "Проверьте бизнес-правила и состояние связанных сущностей перед повтором запроса.",
                List.of(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException exception,
                                                                 HttpServletRequest request) {
        log.warn("409 Data Integrity Violation [{}]: {}", request.getRequestURI(), exception.getMostSpecificCause().getMessage());
        return buildError(HttpStatus.CONFLICT,
                "Операция нарушает ограничение целостности базы данных (уникальность, внешний ключ или обязательное поле).",
                "Проверьте уникальность значений и наличие связанных записей в базе данных.",
                List.of(), request);
    }

    @ExceptionHandler(InvalidDomainValueException.class)
    public ResponseEntity<ApiError> handleInvalidDomainValue(InvalidDomainValueException exception,
                                                             HttpServletRequest request) {
        log.warn("400 Bad Request [{}]: {}", request.getRequestURI(), exception.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, exception.getMessage(),
                "Проверьте доменные ограничения полей (формат телефона +7XXXXXXXXXX, оценка от 0.0 до 5.0, длина заголовка и описания).",
                List.of(), request);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(InvalidRequestException exception,
                                                         HttpServletRequest request) {
        log.warn("400 Bad Request [{}]: {}", request.getRequestURI(), exception.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, exception.getMessage(),
                "Исправьте параметры или тело запроса в соответствии с документацией API.",
                List.of(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException exception,
                                                     HttpServletRequest request) {
        String message = (exception.getMessage() == null || exception.getMessage().isBlank())
                ? "Передан некорректный аргумент."
                : exception.getMessage();
        log.warn("400 Bad Request [{}]: {}", request.getRequestURI(), message);
        return buildError(HttpStatus.BAD_REQUEST, message,
                "Проверьте формат и допустимые значения переданных данных и повторите запрос.",
                List.of(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception,
                                                     HttpServletRequest request) {
        List<String> details = new ArrayList<String>();
        try {
            for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
                details.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
            }
            for (ObjectError globalError : exception.getBindingResult().getGlobalErrors()) {
                details.add(globalError.getDefaultMessage());
            }
        } catch (RuntimeException ex) {
            details.add("Не удалось детально разобрать ошибки валидации: " + ex.getMessage());
        } finally {
            if (details.isEmpty()) {
                details.add("Проверка входных данных не пройдена.");
            }
        }
        String message = details.get(0);
        log.warn("400 Validation Error [{}]: {}", request.getRequestURI(), details);
        return buildError(HttpStatus.BAD_REQUEST, message,
                "Проверьте обязательные поля, формат значений (например телефон +7XXXXXXXXXX или оценку 0.0..5.0) и ограничения длины.",
                details, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception,
                                                              HttpServletRequest request) {
        List<String> details = new ArrayList<String>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            details.add(violation.getPropertyPath() + ": " + violation.getMessage());
        }
        log.warn("400 Constraint Violation [{}]: {}", request.getRequestURI(), details);
        return buildError(HttpStatus.BAD_REQUEST,
                "Параметры запроса не прошли проверку ограничений.",
                "Исправьте значения параметров запроса и повторите попытку.",
                details, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException exception,
                                                         HttpServletRequest request) {
        log.warn("400 Unreadable JSON [{}]: {}", request.getRequestURI(), exception.getMessage());
        return buildError(HttpStatus.BAD_REQUEST,
                "Тело запроса имеет некорректный формат JSON или содержит недопустимые значения полей/перечислений.",
                "Проверьте синтаксис JSON, имена свойств, типы чисел и допустимые значения enum (например, TicketStatus, UserRole, UserStatus).",
                List.of(), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException exception,
                                                             HttpServletRequest request) {
        log.warn("400 Missing Parameter [{}]: {}", request.getRequestURI(), exception.getParameterName());
        return buildError(HttpStatus.BAD_REQUEST,
                "Отсутствует обязательный параметр запроса: " + exception.getParameterName() + ".",
                "Добавьте параметр '" + exception.getParameterName() + "' в строку запроса и повторите попытку.",
                List.of(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException exception,
                                                         HttpServletRequest request) {
        log.warn("400 Type Mismatch [{}]: {}", request.getRequestURI(), exception.getName());
        return buildError(HttpStatus.BAD_REQUEST,
                "Параметр '" + exception.getName() + "' имеет недопустимый тип или формат.",
                "Передайте значение корректного типа (например, числовой ID или допустимое значение перечисления).",
                List.of(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception,
                                                     HttpServletRequest request) {
        log.error("500 Internal Server Error [{}]", request.getRequestURI(), exception);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                "Во время обработки запроса произошла непредвиденная внутренняя ошибка сервера.",
                "Повторите запрос позже; если ошибка сохраняется, проверьте логи приложения и состояние подключения к базе данных.",
                List.of(), request);
    }

    private ResponseEntity<ApiError> buildError(HttpStatus status,
                                                String message,
                                                String solution,
                                                List<String> details,
                                                HttpServletRequest request) {
        String path = request != null ? request.getRequestURI() : "";
        ApiError body = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                solution,
                details
        );
        return ResponseEntity.status(status).body(body);
    }
}
