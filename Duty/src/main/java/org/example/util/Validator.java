package org.example.util;

import java.time.LocalDate;
import java.util.regex.Pattern;

public class Validator {

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^(\\+380|0)\\d{9}$");

    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return true;
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    public static boolean isValidFullName(String fullName) {
        return fullName != null && !fullName.trim().isEmpty();
    }

    public static boolean isValidDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !startDate.isAfter(endDate);
    }

    public static boolean isValidPastDate(LocalDate date) {
        if (date == null) {
            return true;
        }
        return !date.isAfter(LocalDate.now());
    }

    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
}