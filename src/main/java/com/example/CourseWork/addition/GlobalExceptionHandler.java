package com.example.CourseWork.addition;

import com.example.CourseWork.exception.BadRequestException;
import com.example.CourseWork.exception.ForbiddenException;
import com.example.CourseWork.exception.InsufficientPointsException;
import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.exception.UnauthorizedException;
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
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        });
        Map<String, Object> details = new HashMap<>();
        details.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiErrorResponse.of("Validation failed", details));
    }

    @ExceptionHandler(InsufficientPointsException.class)
    @SuppressWarnings("null")
    public ResponseEntity<ApiErrorResponse> handleInsufficientPoints(InsufficientPointsException ex) {
        String message = exposeErrorDetails ? ex.getMessage() : "Недостатньо балів";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiErrorResponse.of(message));
    }

    @ExceptionHandler(AccessDeniedException.class)
    @SuppressWarnings("null")
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiErrorResponse.of("Access denied"));
    }

    @ExceptionHandler(UnauthorizedException.class)
    @SuppressWarnings("null")
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        String message = exposeErrorDetails ? ex.getMessage() : "Unauthorized";
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiErrorResponse.of(message));
    }

    @ExceptionHandler(BadRequestException.class)
    @SuppressWarnings("null")
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex) {
        String message = exposeErrorDetails ? ex.getMessage() : "Bad request";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiErrorResponse.of(message));
    }

    @ExceptionHandler(NotFoundException.class)
    @SuppressWarnings("null")
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex) {
        String message = exposeErrorDetails ? ex.getMessage() : "Not found";
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiErrorResponse.of(message));
    }

    @ExceptionHandler(ForbiddenException.class)
    @SuppressWarnings("null")
    public ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenException ex) {
        String message = exposeErrorDetails ? ex.getMessage() : "Forbidden";
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiErrorResponse.of(message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @SuppressWarnings("null")
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String message = exposeErrorDetails ? ex.getMessage() : "Bad request";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiErrorResponse.of(message));
    }

    @ExceptionHandler(SignatureException.class)
    @SuppressWarnings("null")
    public ResponseEntity<ApiErrorResponse> handleSignature(SignatureException ex) {
        log.warn("Signature validation failed: {}", ex.getMessage());
        String message = exposeErrorDetails ? ex.getMessage() : "Invalid signature";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiErrorResponse.of(message));
    }

    @ExceptionHandler(Exception.class)
    @SuppressWarnings("null")
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);
        String message = exposeErrorDetails ? ex.getMessage() : "Internal server error";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiErrorResponse.of(message));
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
    public ResponseEntity<ApiErrorResponse> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.warn("Static resource not found: {}", ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiErrorResponse.of("Resource not found"));
    }
}
