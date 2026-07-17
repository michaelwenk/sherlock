package org.openscience.sherlock.controller.error;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Error Controller", description = "Handles error responses for the Sherlock backend services.")
@RestControllerAdvice
public class ErrorController {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            final ResponseStatusException exception,
            final HttpServletRequest request) {
        final HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value()) != null
                ? HttpStatus.valueOf(exception.getStatusCode().value())
                : HttpStatus.INTERNAL_SERVER_ERROR;
        final String message = exception.getReason() != null
                ? exception.getReason()
                : status.getReasonPhrase();
        return buildResponse(status, message, request.getRequestURI());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(
            final HttpRequestMethodNotSupportedException exception,
            final HttpServletRequest request) {
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(
            final Exception exception,
            final HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            final Exception exception,
            final HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), request.getRequestURI());
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            final HttpStatus status,
            final String message,
            final String path) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", Instant.now().toString());
        payload.put("status", status.value());
        payload.put("error", status.getReasonPhrase());
        payload.put("message", message != null ? message : status.getReasonPhrase());
        payload.put("path", path);
        return ResponseEntity.status(status).body(payload);
    }
}
