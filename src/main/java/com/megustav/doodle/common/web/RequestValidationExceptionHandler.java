package com.megustav.doodle.common.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.stream.Collectors;

/**
 * Without this, Spring Boot's own default handling for these two types returns a body with no useful message at all
 * for cases such as email not being valid in the request params
 */
@RestControllerAdvice
public class RequestValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException exception) {
        var message = exception.getAllErrors().stream()
                .map(error -> error instanceof FieldError fieldError
                        ? "%s: %s".formatted(fieldError.getField(), fieldError.getDefaultMessage())
                        : error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(message));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> handleParameterValidation(HandlerMethodValidationException exception) {
        var message = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> "%s: %s".formatted(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(message));
    }
}
