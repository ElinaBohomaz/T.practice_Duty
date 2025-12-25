package org.example.data;

import org.example.model.Shift;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class January2026DataInitializer {

    private static final YearMonth JANUARY_2026 = YearMonth.of(2026, 1);

    private static final Map<String, Integer> employeeIds = new HashMap<>();

    static {
    }

    public static List<Shift> getJanuary2026Shifts(Map<String, Integer> employeeIdMap) {
        List<Shift> shifts = new ArrayList<>();

        employeeIds.putAll(employeeIdMap);
        addShiftsForVerechaka(shifts);
        addShiftsForOpishnyan(shifts);
        addShiftsForShkurko(shifts);
        addShiftsForPovarnitsina(shifts);
        addShiftsForKurochka(shifts);
        addShiftsForMisyak(shifts);
        addShiftsForGorbatko(shifts);
        addShiftsForTeslenko(shifts);
        addShiftsForNesterenko(shifts);
        addShiftsForShapovalova(shifts);
        addShiftsForZirka(shifts);
        addShiftsForMoroz(shifts);
        addShiftsForYuskovets(shifts);
        addShiftsForKhoroshun(shifts);
        addShiftsForMikheeva(shifts);

        return shifts;
    }

    private static void addShiftsForVerechaka(List<Shift> shifts) {
        Integer empId = employeeIds.get("Верещака Т.Д.");
        if (empId == null) return;

        int[] daysWith1 = {2, 6, 10, 14, 18, 22, 26, 30};
        int[] daysWith0 = {19};

        addShiftsForEmployee(shifts, empId, daysWith1, "1");
        addShiftsForEmployee(shifts, empId, daysWith0, "0");
    }

    private static void addShiftsForOpishnyan(List<Shift> shifts) {
        Integer empId = employeeIds.get("Опішнян Г.М.");
        if (empId == null) return;

        int[] daysWith1 = {1, 5, 9, 14, 19, 24, 29};
        int[] daysWith0 = {22};

        addShiftsForEmployee(shifts, empId, daysWith1, "1");
        addShiftsForEmployee(shifts, empId, daysWith0, "0");
    }

    private static void addShiftsForShkurko(List<Shift> shifts) {
        Integer empId = employeeIds.get("Шкурко С.В.");
        if (empId == null) return;

        int[] daysWith1 = {1, 5, 9, 20, 24, 28};
        int[] daysWith0 = {16};

        addShiftsForEmployee(shifts, empId, daysWith1, "1");
        addShiftsForEmployee(shifts, empId, daysWith0, "0");
    }

    private static void addShiftsForPovarnitsina(List<Shift> shifts) {
        Integer empId = employeeIds.get("Поварніцина Т.В.");
        if (empId == null) return;

        // 8 на 1 січня
        shifts.add(new Shift(empId, JANUARY_2026.atDay(1), "8"));

        int[] daysWith1 = {4, 8, 12, 16, 20, 24, 28};

        addShiftsForEmployee(shifts, empId, daysWith1, "1");
    }

    private static void addShiftsForKurochka(List<Shift> shifts) {
        Integer empId = employeeIds.get("Курочка С.М.");
        if (empId == null) return;

        int[] daysWith1 = {3, 7, 10, 14, 18, 22, 26, 30};
        int[] daysWith0 = {29};

        addShiftsForEmployee(shifts, empId, daysWith1, "1");
        addShiftsForEmployee(shifts, empId, daysWith0, "0");
    }

    private static void addShiftsForMisyak(List<Shift> shifts) {
        Integer empId = employeeIds.get("Мисяк Ю.О.");
        if (empId == null) return;

        int[] daysWith1 = {2, 6, 10, 14, 18, 22, 26, 30};
        int[] daysWith2 = {3, 7, 11, 15, 19, 23, 27, 31};

        addShiftsForEmployee(shifts, empId, daysWith1, "1");
        addShiftsForEmployee(shifts, empId, daysWith2, "2");
    }

    private static void addShiftsForGorbatko(List<Shift> shifts) {
        Integer empId = employeeIds.get("Горбатко Я.В.");
        if (empId == null) return;

        int[] daysWith1 = {2, 6, 10, 14, 18, 22, 26, 30};
        int[] daysWith2 = {3, 7, 11, 15, 19, 23, 27, 31};

        addShiftsForEmployee(shifts, empId, daysWith1, "1");
        addShiftsForEmployee(shifts, empId, daysWith2, "2");
    }

    private static void addShiftsForTeslenko(List<Shift> shifts) {
        Integer empId = employeeIds.get("Тесленко П.В.");
        if (empId == null) return;

        // 8 на 1 січня
        shifts.add(new Shift(empId, JANUARY_2026.atDay(1), "8"));

        int[] daysWith1 = {3, 7, 11, 15, 19, 23, 27};
        int[] daysWith2 = {4, 8, 12, 16, 20, 24, 28};
        int[] daysWith0 = {31};

        addShiftsForEmployee(shifts, empId, daysWith1, "1");
        addShiftsForEmployee(shifts, empId, daysWith2, "2");
        addShiftsForEmployee(shifts, empId, daysWith0, "0");
    }

    private static void addShiftsForNesterenko(List<Shift> shifts) {
        Integer empId = employeeIds.get("Нестеренко Ю.А.");
        if (empId == null) return;

        // 8 на 1 січня
        shifts.add(new Shift(empId, JANUARY_2026.atDay(1), "8"));

        int[] daysWith1 = {3, 7, 11, 15, 19, 23, 31};
        int[] daysWith2 = {4, 8, 12, 16, 20, 24, 28};
        int[] daysWith0 = {27};

        addShiftsForEmployee(shifts, empId, daysWith1, "1");
        addShiftsForEmployee(shifts, empId, daysWith2, "2");
        addShiftsForEmployee(shifts, empId, daysWith0, "0");
    }

    private static void addShiftsForShapovalova(List<Shift> shifts) {
        Integer empId = employeeIds.get("Шаповалова Л.А.");
        if (empId == null) return;

        int[] daysWith1 = {4, 8, 12, 20, 24, 28};
        int[] daysWith2 = {1, 5, 9, 13, 17, 21, 25, 29};
        int[] daysWith0 = {16};

        addShiftsForEmployee(shifts, empId, daysWith1, "1");
        addShiftsForEmployee(shifts, empId, daysWith2, "2");
        addShiftsForEmployee(shifts, empId, daysWith0, "0");
    }

    private static void addShiftsForZirka(List<Shift> shifts) {
        Integer empId = employeeIds.get("Зірка Л.В.");
        if (empId == null) return;

        int[] daysWith1 = {3, 7, 11, 15, 19, 23, 27};

        addShiftsForEmployee(shifts, empId, daysWith1, "1");
    }

    private static void addShiftsForMoroz(List<Shift> shifts) {
        Integer empId = employeeIds.get("Мороз Т.І.");
        if (empId == null) return;

        int[] daysWith1 = {4, 8, 12, 20, 24, 28};
        int[] daysWith0 = {16};

        addShiftsForEmployee(shifts, empId, daysWith1, "1");
        addShiftsForEmployee(shifts, empId, daysWith0, "0");
    }

    private static void addShiftsForYuskovets(List<Shift> shifts) {
        Integer empId = employeeIds.get("Юсковець Т.М.");
        if (empId == null) return;

        int[] daysWith1 = {2, 6, 10, 14, 18, 22, 30};
        int[] daysWith0 = {26};

        addShiftsForEmployee(shifts, empId, daysWith1, "1");
        addShiftsForEmployee(shifts, empId, daysWith0, "0");
    }

    private static void addShiftsForKhoroshun(List<Shift> shifts) {
        Integer empId = employeeIds.get("Хорошун Д.М.");
        if (empId == null) return;

        for (int day = 2; day <= 18; day++) {
            shifts.add(new Shift(empId, JANUARY_2026.atDay(day), "В"));
        }

        int[] daysWith1 = {19, 23, 27, 31};

        addShiftsForEmployee(shifts, empId, daysWith1, "1");
    }

    private static void addShiftsForMikheeva(List<Shift> shifts) {
        Integer empId = employeeIds.get("Міхєєва Л.М.");
        if (empId == null) return;

        int[] daysWith1 = {3, 7, 11, 15, 19, 23, 27, 31};

        addShiftsForEmployee(shifts, empId, daysWith1, "1");
    }

    private static void addShiftsForEmployee(List<Shift> shifts, Integer empId, int[] days, String code) {
        for (int day : days) {
            if (day >= 1 && day <= 31) {
                LocalDate date = JANUARY_2026.atDay(day);
                shifts.add(new Shift(empId, date, code));
            }
        }
    }

    public static Map<String, Integer> createEmployeeNameToIdMap() {
        return new HashMap<>();
    }
}