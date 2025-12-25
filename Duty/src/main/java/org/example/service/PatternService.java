package org.example.service;

import org.example.model.Employee;
import org.example.model.Shift;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.DayOfWeek;
import java.util.*;

public class PatternService {

    public String detectPattern(List<Shift> shifts, YearMonth month) {
        if (shifts == null || shifts.isEmpty()) {
            return "1_3_1"; // Шаблон за замовчуванням
        }

        // Сортуємо зміни за датою
        shifts.sort((s1, s2) -> s1.getDate().compareTo(s2.getDate()));

        // Аналізуємо послідовності
        List<String> sequence = new ArrayList<>();
        for (Shift shift : shifts) {
            sequence.add(shift.getCode());
        }

        // Перевіряємо різні шаблони
        if (isPattern1_3_1(sequence)) {
            return "1_3_1";
        } else if (isPattern2_2(sequence)) {
            return "2_2";
        } else if (isPattern12_2_12(sequence)) {
            return "12_2_12";
        } else if (isPatternWeekday(sequence, month)) {
            return "weekday";
        } else if (isPattern8_25(sequence, month)) {
            return "8_25";
        }

        return "1_3_1"; // Шаблон за замовчуванням
    }

    private boolean isPattern1_3_1(List<String> sequence) {
        // Шукаємо патерн: 1, X, X, X, 1, X, X, X, ...
        for (int i = 0; i < sequence.size() - 4; i++) {
            if ("1".equals(sequence.get(i)) &&
                    "X".equals(sequence.get(i+1)) &&
                    "X".equals(sequence.get(i+2)) &&
                    "X".equals(sequence.get(i+3)) &&
                    "1".equals(sequence.get(i+4))) {
                return true;
            }
        }
        return false;
    }

    private boolean isPattern2_2(List<String> sequence) {
        // Шукаємо патерн: 11, 11, X, X, 11, 11, X, X, ...
        for (int i = 0; i < sequence.size() - 3; i++) {
            if ("11".equals(sequence.get(i)) &&
                    "11".equals(sequence.get(i+1)) &&
                    "X".equals(sequence.get(i+2)) &&
                    "X".equals(sequence.get(i+3))) {
                return true;
            }
        }
        return false;
    }

    private boolean isPattern12_2_12(List<String> sequence) {
        // Шукаємо патерн: 12, X, X, 12, X, X, ...
        for (int i = 0; i < sequence.size() - 3; i++) {
            if ("12".equals(sequence.get(i)) &&
                    "X".equals(sequence.get(i+1)) &&
                    "X".equals(sequence.get(i+2)) &&
                    "12".equals(sequence.get(i+3))) {
                return true;
            }
        }
        return false;
    }

    private boolean isPatternWeekday(List<String> sequence, YearMonth month) {
        // Працює тільки в будні (понеділок-п'ятниця)
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        int totalWorkDays = 0;
        int totalWeekdays = 0;

        LocalDate currentDate = startDate;
        int index = 0;

        while (!currentDate.isAfter(endDate) && index < sequence.size()) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            boolean isWeekday = dayOfWeek.getValue() <= 5; // Пн-Пт

            if (isWeekday) {
                totalWeekdays++;
                if ("1".equals(sequence.get(index)) || "12".equals(sequence.get(index))) {
                    totalWorkDays++;
                }
            }

            currentDate = currentDate.plusDays(1);
            index++;
        }

        // Якщо працював більшість буднів
        return totalWeekdays > 0 && totalWorkDays >= totalWeekdays * 0.8;
    }

    private boolean isPattern8_25(List<String> sequence, YearMonth month) {
        // Перевіряємо наявність значень 8.25
        for (String code : sequence) {
            if ("8.25".equals(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Отримує відображуване ім'я для шаблону.
     */
    public String getPatternDisplayName(String patternType) {
        switch (patternType) {
            case "1_3_1": return "1 день роботи / 3 дні відпочинку";
            case "2_2": return "2 дні роботи / 2 дні відпочинку (11)";
            case "12_2_12": return "12 год роботи / 2 дні вдома / 12 год ніч";
            case "weekday": return "Тільки в будні дні";
            case "8_25": return "Тільки в будні по 8.25 год";
            default: return patternType;
        }
    }
}