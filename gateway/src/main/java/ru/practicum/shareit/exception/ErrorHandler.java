package ru.practicum.shareit.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleMissingHeader(final MissingRequestHeaderException e) {
        log.warn("Отсутствует обязательный заголовок: {}", e.getHeaderName());
        return Map.of("error", "Required request header '" + e.getHeaderName() + "' is not present");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleMethodArgumentNotValid(final MethodArgumentNotValidException e) {
        return validationError(e.getAllErrors());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleHandlerMethodValidation(final HandlerMethodValidationException e) {
        return validationError(e.getAllErrors());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleTypeMismatch(final MethodArgumentTypeMismatchException e) {
        String message = "Некорректное значение параметра '" + e.getName() + "': " + e.getValue();
        log.warn("Некорректный параметр запроса (400): {}", message);
        return Map.of("error", message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgument(final IllegalArgumentException e) {
        log.warn("Некорректный запрос (400): {}", e.getMessage());
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleGeneralException(final Exception e) {
        log.error("Непредвиденная ошибка сервера", e);
        return Map.of("error", "Произошла внутренняя ошибка сервера.");
    }

    private Map<String, String> validationError(List<? extends MessageSourceResolvable> errors) {
        String message = errors.stream()
            .map(MessageSourceResolvable::getDefaultMessage)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse("Ошибка валидации полей");
        log.warn("Ошибка валидации данных: {}", message);
        return Map.of("error", message);
    }
}
