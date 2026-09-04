package com.megustav.doodle.common.web;

import com.megustav.doodle.common.exceptions.DomainException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class DomainExceptionHandler {

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiError> handleDomainException(DomainException exception) {
        return ResponseEntity.status(exception.getStatus()).body(new ApiError(exception.getMessage()));
    }
}
