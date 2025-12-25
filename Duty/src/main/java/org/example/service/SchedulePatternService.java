package org.example.service;

import org.example.dao.ShiftDAO;
import org.example.model.Employee;
import org.example.model.Shift;
import org.example.dao.EmployeeDAO;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class SchedulePatternService {

    private final EmployeeDAO employeeDAO;
    private final ShiftDAO shiftDAO;

    public SchedulePatternService() {
        this.employeeDAO = new EmployeeDAO();
        this.shiftDAO = new ShiftDAO();
    }


    public void continueScheduleFromPreviousMonth(YearMonth currentMonth) throws SQLException {
        YearMonth previousMonth = currentMonth.minusMonths(1);

        // Отримуємо всіх працюючих працівників
        List<Employee> employees = employeeDAO.findAll().stream()
                .filter(Employee::isCurrentlyWorking)
                .toList();

        List<Shift> newShifts = new ArrayList<>();

        for (Employee employee : employees) {
            // Отримуємо зміни за попередній місяць
            List<Shift> previousShifts = shiftDAO.findShiftsForEmployeeAndMonth(employee.getId(), previousMonth);

            if (previousShifts != null && !previousShifts.isEmpty()) {
                // Аналізуємо патерн попереднього місяця
                String pattern = detectPattern(previousShifts, previousMonth);

                // Генеруємо зміни для нового місяця на основі патерну
                List<Shift> continuedShifts = generateShiftsByPattern(employee.getId(), currentMonth, pattern, previousShifts);
                newShifts.addAll(continuedShifts);
            }
        }

        // Зберігаємо нові зміни
        saveOnlyEmptyDays(newShifts, currentMonth);
    }

    /**
     * Визначає патерн роботи на основі змін попереднього місяця
     */
    private String detectPattern(List<Shift> shifts, YearMonth month) {
        // Сортуємо за датою
        shifts.sort(Comparator.comparing(Shift::getDate));

        Map<String, Integer> codeCount = new HashMap<>();
        List<String> sequence = new ArrayList<>();

        for (Shift shift : shifts) {
            String code = shift.getCode() == null ? "" : shift.getCode();
            if (!code.isEmpty() && !"X".equals(code)) {
                codeCount.put(code, codeCount.getOrDefault(code, 0) + 1);
            }
            sequence.add(code);
        }

        // Аналізуємо послідовності
        if (sequence.contains("2") || sequence.contains("12")) {
            // Подвійні зміни
            return analyzeDoubleShiftPattern(sequence);
        } else if (sequence.contains("1")) {
            // Одиночні зміни
            return analyzeSingleShiftPattern(sequence);
        } else if (sequence.contains("8.00") || sequence.contains("8.25")) {
            return "8_hours";
        } else if (sequence.contains("11")) {
            return "11_hours";
        }

        return "irregular";
    }

    private String analyzeDoubleShiftPattern(List<String> sequence) {
        // Аналізуємо патерн подвійних змін (2-2-2 або 1-2-1-2)
        int count1 = Collections.frequency(sequence, "1");
        int count2 = Collections.frequency(sequence, "2") + Collections.frequency(sequence, "12");

        if (count2 > count1 * 2) {
            return "2_2_2"; // Подвійні зміни переважають
        } else {
            return "1_2_mixed"; // Змішаний патерн
        }
    }

    private String analyzeSingleShiftPattern(List<String> sequence) {
        int daysBetweenWork = 0;
        boolean counting = false;
        List<Integer> gaps = new ArrayList<>();

        for (int i = 0; i < sequence.size(); i++) {
            if ("1".equals(sequence.get(i))) {
                if (counting && daysBetweenWork > 0) {
                    gaps.add(daysBetweenWork);
                }
                counting = true;
                daysBetweenWork = 0;
            } else if (counting) {
                daysBetweenWork++;
            }
        }

        if (!gaps.isEmpty()) {
            int avgGap = (int) gaps.stream().mapToInt(Integer::intValue).average().orElse(3);
            if (avgGap >= 2 && avgGap <= 4) {
                return "1_" + avgGap + "_1"; // Наприклад, "1_3_1"
            }
        }

        return "irregular";
    }

    /**
     * Генерує зміни для нового місяця на основі патерну
     */
    private List<Shift> generateShiftsByPattern(Integer employeeId, YearMonth month, String pattern, List<Shift> previousShifts) {
        List<Shift> newShifts = new ArrayList<>();
        int daysInMonth = month.lengthOfMonth();

        // Отримуємо останні зміни з попереднього місяця для продовження ритму
        List<String> lastDaysPattern = getLastDaysPattern(previousShifts, 7); // Останні 7 днів

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = month.atDay(day);

            // Генеруємо код зміни на основі патерну та дня місяця
            String shiftCode = generateShiftCodeForDay(day, pattern, lastDaysPattern, date.getDayOfWeek());

            Shift shift = new Shift(employeeId, date, shiftCode);
            newShifts.add(shift);
        }

        return newShifts;
    }

    private List<String> getLastDaysPattern(List<Shift> shifts, int lastDays) {
        List<String> pattern = new ArrayList<>();
        shifts.sort(Comparator.comparing(Shift::getDate).reversed());

        int count = 0;
        for (Shift shift : shifts) {
            if (count >= lastDays) break;
            pattern.add(shift.getCode() == null ? "" : shift.getCode());
            count++;
        }

        // Реверсуємо назад до нормального порядку
        Collections.reverse(pattern);
        return pattern;
    }

    private String generateShiftCodeForDay(int day, String pattern, List<String> lastPattern, java.time.DayOfWeek dayOfWeek) {
        // Вихідні за замовчуванням
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            return "X";
        }

        // Генерація на основі патерну
        switch (pattern) {
            case "1_3_1":
                // 1 день роботи, 3 дні відпочинку
                return (day % 4 == 1) ? "1" : "X";

            case "2_2_2":
                // 2 дні роботи, 2 дні відпочинку (подвійні зміни)
                int cycle = (day - 1) % 4;
                return (cycle < 2) ? "2" : "X";

            case "1_2_mixed":
                // Змішаний патерн: намагаємося продовжити останній патерн
                if (!lastPattern.isEmpty()) {
                    int lastIndex = (day - 1) % lastPattern.size();
                    return lastPattern.get(lastIndex);
                }
                return (day % 3 == 1) ? "1" : "X";

            case "8_hours":
                // Будні дні по 8 годин
                return "8.00";

            case "11_hours":
                return (dayOfWeek.getValue() <= 5) ? "11" : "X";

            default:
                return "X";
        }
    }

    /**
     * Зберігає тільки ті зміни, для яких ще немає даних в БД
     */
    private void saveOnlyEmptyDays(List<Shift> newShifts, YearMonth month) throws SQLException {
        List<Shift> toSave = new ArrayList<>();

        for (Shift newShift : newShifts) {
            // Перевіряємо, чи вже є зміна для цього дня
            Shift existing = shiftDAO.findForEmployeeOnDate(newShift.getEmployeeId(), newShift.getDate());

            if (existing == null || "X".equals(existing.getCode())) {
                toSave.add(newShift);
            }
        }

        if (!toSave.isEmpty()) {
            shiftDAO.saveBatch(toSave);
        }
    }
}