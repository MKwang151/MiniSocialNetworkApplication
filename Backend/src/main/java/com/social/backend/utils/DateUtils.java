package com.social.backend.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String format(LocalDateTime date) {
        return date.format(formatter);
    }

    public static String now() {
        return format(LocalDateTime.now());
    }
}
