package org.example.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Утиліти для роботи з датами та місяцями.
 */
public class DateUtil {

    // Форматери для різних форматів дат
    private static final DateTimeFormatter MONTH_YEAR_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter DAY_FORMATTER =
            DateTimeFormatter.ofPattern("d");
    private static final DateTimeFormatter SQL_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Отримує список всіх днів у місяці.
     */
    public static List<LocalDate> getDaysInMonth(YearMonth yearMonth) {
        List<LocalDate> days = new ArrayList<>();
        LocalDate date = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        while (!date.isAfter(endDate)) {
            days.add(date);
            date = date.plusDays(1);
        }

        return days;
    }

    /**
     * Форматує місяць у читабельний вигляд (наприклад, "Грудень 2024").
     */
    public static String formatMonthYear(YearMonth yearMonth) {
        return yearMonth.format(MONTH_YEAR_FORMATTER);
    }

    /**
     * Форматує дату для SQL запитів (YYYY-MM-DD).
     */
    public static String formatDateForSql(LocalDate date) {
        return date.format(SQL_DATE_FORMATTER);
    }

    /**
     * Отримує номер дня місяця як рядок.
     */
    public static String getDayNumber(LocalDate date) {
        return date.format(DAY_FORMATTER);
    }

    /**
     * Отримує кількість днів у місяці.
     */
    public static int getDaysInMonthCount(YearMonth yearMonth) {
        return yearMonth.lengthOfMonth();
    }

    /**
     * Отримує попередній місяць.
     */
    public static YearMonth getPreviousMonth(YearMonth currentMonth) {
        return currentMonth.minusMonths(1);
    }

    /**
     * Отримує наступний місяць.
     */
    public static YearMonth getNextMonth(YearMonth currentMonth) {
        return currentMonth.plusMonths(1);
    }

    /**
     * Перевіряє, чи є дата вихідним днем (субота або неділя).
     */
    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek().getValue() >= 6; // 6 = Субота, 7 = Неділя
    }

    public static YearMonth parseYearMonth(String monthYear) {
        try {
            if (monthYear.contains("-")) {
                return YearMonth.parse(monthYear);
            } else {
                String[] parts = monthYear.split(" ");
                if (parts.length == 2) {
                    int month = Integer.parseInt(parts[0]);
                    int year = Integer.parseInt(parts[1]);
                    return YearMonth.of(year, month);
                }
            }
        } catch (Exception e) {
            System.err.println("Помилка парсингу дати: " + e.getMessage());
        }

        return YearMonth.now();
    }

    /**
     * Отримує поточний місяць.
     */
    public static YearMonth getCurrentMonth() {
        return YearMonth.now();
    }

    /**
     * Генерує список років для вибору (поточний рік ± 2).
     */
    public static List<Integer> generateYears() {
        List<Integer> years = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();

        for (int i = currentYear - 2; i <= currentYear + 2; i++) {
            years.add(i);
        }

        return years;
    }

    /**
     * Отримує українські назви місяців.
     */
    public static String[] getUkrainianMonths() {
        return new String[] {
                "Січень", "Лютий", "Березень", "Квітень",
                "Травень", "Червень", "Липень", "Серпень",
                "Вересень", "Жовтень", "Листопад", "Грудень"
        };
    }
}