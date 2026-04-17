package com.example.CourseWork.exception;

/**
 * Signals a user-action conflict (e.g. trying to modify an already paid order).
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}

