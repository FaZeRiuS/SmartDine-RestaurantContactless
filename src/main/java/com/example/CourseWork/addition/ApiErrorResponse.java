package com.example.CourseWork.addition;

import java.util.Map;

public record ApiErrorResponse(
        String status,
        String message,
        Map<String, Object> details
) {
    public static ApiErrorResponse of(String message) {
        return new ApiErrorResponse("error", message, null);
    }

    public static ApiErrorResponse of(String message, Map<String, Object> details) {
        return new ApiErrorResponse("error", message, details);
    }
}

