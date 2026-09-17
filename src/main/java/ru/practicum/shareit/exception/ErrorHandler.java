package ru.practicum.shareit.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleMissingHeader(final MissingRequestHeaderException e) {
        log.warn("Отсутствует обязательный заголовок: {}", e.getHeaderName());
        return Map.of("error", "Required request header '" + e.getHeaderName() + "' is not present");
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFoundException(final NotFoundException e) {
        log.warn("Ресурс не найден (404): {}", e.getMessage());
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequestException(final BadRequestException e) {
        log.warn("Некорректный запрос (400): {}", e.getMessage());
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(final NoSuchElementException e) {
        log.warn("Ресурс не найден: {}", e.getMessage());
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleIllegalArgument(final IllegalArgumentException e) {
        log.warn("Ошибка валидации/доступа: {}", e.getMessage());
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleMethodArgumentNotValid(final MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        log.warn("Ошибка валидации данных: {}", message);
        return Map.of("error", message != null ? message : "Ошибка валидации полей");
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleConflict(final ConflictException e) {
        log.warn("Конфликт данных: {}", e.getMessage());
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleGeneralException(final Exception e) {
        log.error("Непредвиденная ошибка сервера", e);
        return Map.of("error", "Произошла внутренняя ошибка сервера.");
    }
}
