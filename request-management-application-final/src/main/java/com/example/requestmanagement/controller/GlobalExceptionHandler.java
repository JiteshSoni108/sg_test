package com.example.requestmanagement.controller;

import com.example.requestmanagement.exception.BusinessRuleViolationException;
import com.example.requestmanagement.exception.ErrorResponse;
import com.example.requestmanagement.exception.InvalidStateTransitionException;
import com.example.requestmanagement.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;

import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(NotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex, req);
    }

    @ExceptionHandler({BusinessRuleViolationException.class, InvalidStateTransitionException.class, IllegalArgumentException.class})
    ResponseEntity<ErrorResponse> badRequest(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex, req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream().map(error -> error.getField() + ": " + error.getDefaultMessage()).findFirst().orElse("Validation failed");
        return new ResponseEntity<>(new ErrorResponse(OffsetDateTime.now(), 400, "Bad Request", message, req.getRequestURI()), HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, Exception ex, HttpServletRequest req) {
        return new ResponseEntity<>(new ErrorResponse(OffsetDateTime.now(), status.value(), status.getReasonPhrase(), ex.getMessage(), req.getRequestURI()), status);
    }
}
