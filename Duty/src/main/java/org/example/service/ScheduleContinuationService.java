package org.example.service;

import org.example.dao.EmployeeDAO;
import org.example.dao.ShiftDAO;
import org.example.model.Employee;
import org.example.model.Shift;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class ScheduleContinuationService {

    private final EmployeeDAO employeeDAO;
    private final ShiftDAO shiftDAO;

    public ScheduleContinuationService() {
        this.employeeDAO = new EmployeeDAO();
        this.shiftDAO = new ShiftDAO();
    }

    public void loadAndContinueSchedule(YearMonth targetMonth) throws SQLException {
        System.out.println("🔄 Продовження графіку на " + targetMonth);

        // Отримуємо попередній місяць
        YearMonth previousMonth = targetMonth.minusMonths(1);
        int daysInTargetMonth = targetMonth.lengthOfMonth();
        int daysInPreviousMonth = previousMonth.lengthOfMonth();

        // Отримуємо всіх працюючих працівників, відсортованих за відділом та ПІБ
        List<Employee> employees = employeeDAO.findAll().stream()
                .filter(Employee::isCurrentlyWorking)
                .sorted(Comparator
                        .comparing(Employee::getDepartment)
                        .thenComparing(Employee::getFullName))
                .toList();

        // Якщо немає працівників, виходимо
        if (employees.isEmpty()) {
            System.out.println("⚠️ Немає працюючих працівників");
            return;
        }

        List<Shift> newShifts = new ArrayList<>();
        int employeeIndex = 0;

        for (Employee employee : employees) {
            Map<Integer, String> lastThreeDays = getLastThreeDaysOfMonth(
                    employee.getId(), previousMonth, daysInPreviousMonth);

            // Генеруємо зміни для лютого
            List<Shift> employeeShifts = generateFebruaryShifts(
                    employee.getId(), targetMonth, employeeIndex, lastThreeDays);

            newShifts.addAll(employeeShifts);
            employeeIndex++;
        }

        // Зберігаємо зміни в БД
        if (!newShifts.isEmpty()) {
            shiftDAO.saveBatch(newShifts);
            System.out.println("✅ Збережено " + newShifts.size() + " змін для " + targetMonth);
        }
    }

    private Map<Integer, String> getLastThreeDaysOfMonth(Integer employeeId,
                                                         YearMonth month,
                                                         int daysInMonth) throws SQLException {
        Map<Integer, String> lastDays = new HashMap<>();

        List<Integer> lastDaysNumbers = Arrays.asList(29, 30, 31);

        for (int dayNum : lastDaysNumbers) {
            if (dayNum <= daysInMonth) {
                LocalDate date = month.atDay(dayNum);
                Shift shift = shiftDAO.findForEmployeeOnDate(employeeId, date);
                String code = (shift != null && shift.getCode() != null) ? shift.getCode() : "X";
                lastDays.put(dayNum, code);
            }
        }

        return lastDays;
    }
    private List<Shift> generateFebruaryShifts(Integer employeeId,
                                               YearMonth februaryMonth,
                                               int employeeIndex,
                                               Map<Integer, String> lastThreeDays) {
        List<Shift> shifts = new ArrayList<>();
        int daysInFebruary = februaryMonth.lengthOfMonth();

        for (int day = 1; day <= daysInFebruary; day++) {
            LocalDate date = februaryMonth.atDay(day);
            String shiftCode;

            if (employeeIndex < 5) {
                shiftCode = generateForFirstFive(day, employeeIndex, lastThreeDays);
            } else {
                // Решта працівників
                shiftCode = generateForRest(day, employeeIndex, lastThreeDays);
            }

            shifts.add(new Shift(employeeId, date, shiftCode));
        }

        return shifts;
    }

    private String generateForFirstFive(int day, int employeeIndex, Map<Integer, String> lastThreeDays) {
        if (day == 1) {
            return "X";
        }

        // Отримуємо останні коди з січня
        String day29 = lastThreeDays.getOrDefault(29, "X");
        String day30 = lastThreeDays.getOrDefault(30, "X");
        String day31 = lastThreeDays.getOrDefault(31, "X");

        // Визначаємо поточну позицію в циклі
        if ("1".equals(day30) && "2".equals(day31)) {
            // Якщо в січні було 1, 2, то продовжуємо
            return continuePatternFromLastDays(day, day29, day30, day31);
        } else {
            // Стандартний патерн: 1 день роботи, 3 вихідних
            return generateStandardPattern(day);
        }
    }

    private String generateForRest(int day, int employeeIndex, Map<Integer, String> lastThreeDays) {
        // Логіка з фото: 1-2 лютого = X X, потім 1 2

        if (day <= 2) {
            // Перші два дні лютого = X
            return "X";
        }

        // Отримуємо останні коди з січня
        String day29 = lastThreeDays.getOrDefault(29, "X");
        String day30 = lastThreeDays.getOrDefault(30, "X");
        String day31 = lastThreeDays.getOrDefault(31, "X");

        // Аналізуємо патерн з січня
        return analyzeAndContinuePattern(day, day29, day30, day31, employeeIndex);
    }

    private String continuePatternFromLastDays(int currentDay, String day29, String day30, String day31) {
        // Аналізуємо послідовність
        if ("X".equals(day29) && "1".equals(day30) && "2".equals(day31)) {
            return switch ((currentDay - 1) % 3) {
                case 0 -> "X";
                case 1 -> "1";
                case 2 -> "2";
                default -> "X";
            };
        } else if ("1".equals(day29) && "2".equals(day30) && "X".equals(day31)) {
            // Патерн: 1, 2, X
            return switch ((currentDay - 1) % 3) {
                case 0 -> "1";
                case 1 -> "2";
                case 2 -> "X";
                default -> "X";
            };
        } else if ("2".equals(day29) && "X".equals(day30) && "X".equals(day31)) {
            // Патерн: 2, X, X
            return switch ((currentDay - 1) % 3) {
                case 0 -> "2";
                case 1 -> "X";
                case 2 -> "X";
                default -> "X";
            };
        }

        // Якщо не визначили патерн, генеруємо стандартний
        return generateStandardPattern(currentDay);
    }

    private String analyzeAndContinuePattern(int currentDay, String day29, String day30,
                                             String day31, int employeeIndex) {
        // Створюємо послідовність
        String[] sequence = {day29, day30, day31};

        // Аналізуємо типи змін
        boolean has1 = Arrays.asList(sequence).contains("1");
        boolean has2 = Arrays.asList(sequence).contains("2");
        boolean has12 = Arrays.asList(sequence).contains("12");

        if (has1 && has2) {
            // Патерн з 1 та 2
            return generate12Pattern(currentDay, employeeIndex);
        } else if (has1) {
            // Тільки 1
            return generate1Pattern(currentDay, employeeIndex);
        } else if (has2 || has12) {
            // Тільки 2 або 12
            return generate2Pattern(currentDay, employeeIndex);
        } else {
            // Всі X або інші коди
            return generateStandardPattern(currentDay);
        }
    }

    /**
     * Генерувати патерн з 1 та 2
     */
    private String generate12Pattern(int day, int employeeIndex) {
        // Патерн: 1, 2, X, X
        int cycle = (day - 1 + employeeIndex) % 4;
        return switch (cycle) {
            case 0 -> "1";
            case 1 -> "2";
            case 2, 3 -> "X";
            default -> "X";
        };
    }

    /**
     * Генерувати патерн тільки з 1
     */
    private String generate1Pattern(int day, int employeeIndex) {
        // Патерн: 1, X, X, X
        return (day % 4 == (employeeIndex % 4)) ? "1" : "X";
    }

    /**
     * Генерувати патерн тільки з 2
     */
    private String generate2Pattern(int day, int employeeIndex) {
        // Патерн: 2, 2, X, X
        int cycle = (day - 1 + employeeIndex) % 4;
        return (cycle < 2) ? "2" : "X";
    }

    /**
     * Стандартний патерн (1 день роботи, 3 вихідних)
     */
    private String generateStandardPattern(int day) {
        return (day % 4 == 1) ? "1" : "X";
    }

    /**
     * Перевірити, чи є зміни для місяця
     */
    public boolean hasShiftsForMonth(YearMonth month) throws SQLException {
        Map<Integer, List<Shift>> shifts = shiftDAO.findShiftsForMonth(month);
        return !shifts.isEmpty();
    }
}