package com.example.CourseWork.addition;

import com.example.CourseWork.exception.InsufficientPointsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.access.AccessDeniedException;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * In production we should not expose internal exception messages to clients.
     * Enable details only in dev/local troubleshooting.
     */
    @Value("${app.errors.expose-details:false}")
    private boolean exposeErrorDetails;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @SuppressWarnings("null")
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });
        errors.put("status", "error");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errors);
    }

    @ExceptionHandler(InsufficientPointsException.class)
    @SuppressWarnings("null")
    public ResponseEntity<Map<String, String>> handleInsufficientPoints(InsufficientPointsException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", exposeErrorDetails ? ex.getMessage() : "Недостатньо балів");
        error.put("status", "error");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @SuppressWarnings("null")
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("message", "Access denied");
        error.put("status", "error");
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    @SuppressWarnings("null")
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", exposeErrorDetails ? ex.getMessage() : "Request failed");
        error.put("status", "error");

        HttpStatus status;
        String message = ex.getMessage();

        if (message != null) {
            if (message.contains("not found")) {
                status = HttpStatus.NOT_FOUND;
            } else if (message.contains("empty")) {
                status = HttpStatus.BAD_REQUEST;
            } else if (message.contains("not available")) {
                status = HttpStatus.SERVICE_UNAVAILABLE;
            } else {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        // Avoid ERROR + full stack for expected client outcomes (404/400/503); keep ERROR for 5xx surprises.
        if (status.is4xxClientError() || status == HttpStatus.SERVICE_UNAVAILABLE) {
            log.warn("Request failed ({}): {}", status.value(), message != null ? message : ex.getClass().getSimpleName());
        } else {
            log.error("Runtime error: ", ex);
        }

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }

    @ExceptionHandler(SignatureException.class)
    @SuppressWarnings("null")
    public ResponseEntity<Map<String, String>> handleSignature(SignatureException ex) {
        log.warn("Signature validation failed: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("message", exposeErrorDetails ? ex.getMessage() : "Invalid signature");
        error.put("status", "error");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }

    @ExceptionHandler(Exception.class)
    @SuppressWarnings("null")
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);
        Map<String, String> error = new HashMap<>();
        error.put("message", exposeErrorDetails ? ("An unexpected error occurred: " + ex.getMessage()) : "An unexpected error occurred");
        error.put("status", "error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }

    @ExceptionHandler(IOException.class)
    public void handleIOException(IOException ex) {
        if (ex.getClass().getName().contains("ClientAbortException") ||
                (ex.getMessage() != null && ex.getMessage().contains("Broken pipe"))) {
            // Demoted to debug to reduce noise in logs, as this is expected when a user closes a tab or refreshes.
            log.debug("Client disconnected (Broken pipe): {}", ex.getMessage());
        } else {
            log.error("IO error: ", ex);
        }
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public void handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex) {
        log.debug("Async request timed out (normal behavior for SSE): {}", ex.getMessage());
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsableException(AsyncRequestNotUsableException ex) {
        // Demoted to debug to reduce noise in logs - common when clients disconnect from SSE.
        log.debug("Async request not usable (client disconnected): {}", ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @SuppressWarnings("null")
    public ResponseEntity<Map<String, String>> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.warn("Static resource not found: {}", ex.getResourcePath());
        Map<String, String> error = new HashMap<>();
        error.put("message", "Resource not found");
        error.put("status", "error");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }
}
