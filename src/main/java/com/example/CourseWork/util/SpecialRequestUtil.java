package com.example.CourseWork.util;

public final class SpecialRequestUtil {
    private SpecialRequestUtil() {}

    public static String normalize(String value) {
        if (value == null) return "";
        String v = value.trim();
        return v.isBlank() ? "" : v;
    }
}
