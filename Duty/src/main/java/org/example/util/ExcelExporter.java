package org.example.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.model.Employee;
import org.example.model.Shift;
import org.example.service.ScheduleService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Утиліта для експорту графіка змін в Excel файл.
 */
public class ExcelExporter {

    /**
     * Експортує графік змін в Excel файл.
     */
    public static void exportSchedule(ScheduleService scheduleService,
                                      YearMonth month,
                                      File outputFile) throws Exception {

        // Отримуємо дані з сервісу
        Map<Employee, List<Shift>> scheduleData = scheduleService.loadScheduleForMonth(month);

        // Створюємо нову книгу Excel
        Workbook workbook = new XSSFWorkbook();

        // Створюємо лист
        Sheet sheet = workbook.createSheet("Графік змін");

        // Налаштовуємо стилі
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle employeeStyle = createEmployeeStyle(workbook);
        CellStyle dayOffStyle = createDayOffStyle(workbook);
        CellStyle workingDayStyle = createWorkingDayStyle(workbook);
        CellStyle specialDayStyle = createSpecialDayStyle(workbook);

        // Заголовок
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Графік змін працівників за " +
                DateUtil.formatMonthYear(month));
        titleCell.setCellStyle(headerStyle);

        // Заголовки стовпців
        Row headerRow = sheet.createRow(2);
        String[] headers = {"ПІБ", "Підрозділ", "Посада"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Дні місяця як заголовки
        int daysInMonth = DateUtil.getDaysInMonthCount(month);
        for (int day = 1; day <= daysInMonth; day++) {
            Cell cell = headerRow.createCell(headers.length + day - 1);
            cell.setCellValue(String.valueOf(day));
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(headers.length + day - 1, 1500);
        }

        // Заповнюємо дані
        int rowIndex = 3;
        for (Map.Entry<Employee, List<Shift>> entry : scheduleData.entrySet()) {
            Employee employee = entry.getKey();
            List<Shift> shifts = entry.getValue();

            // Створюємо мапу дата → код зміни для швидкого пошуку
            Map<LocalDate, String> shiftMap = new java.util.HashMap<>();
            for (Shift shift : shifts) {
                shiftMap.put(shift.getDate(), shift.getCode());
            }

            Row dataRow = sheet.createRow(rowIndex);

            // ПІБ працівника
            Cell nameCell = dataRow.createCell(0);
            nameCell.setCellValue(employee.getFullName());
            nameCell.setCellStyle(employeeStyle);

            // Підрозділ
            Cell deptCell = dataRow.createCell(1);
            deptCell.setCellValue(employee.getDepartment());
            deptCell.setCellStyle(employeeStyle);

            // Посада
            Cell positionCell = dataRow.createCell(2);
            positionCell.setCellValue(employee.getPosition() != null ? employee.getPosition() : "");
            positionCell.setCellStyle(employeeStyle);

            // Зміни по днях
            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate date = month.atDay(day);
                String shiftCode = shiftMap.getOrDefault(date, "X");

                Cell shiftCell = dataRow.createCell(headers.length + day - 1);
                shiftCell.setCellValue(shiftCode);

                // Встановлюємо стиль залежно від типу зміни
                CellStyle cellStyle = getCellStyleForShift(shiftCode,
                        dayOffStyle, workingDayStyle, specialDayStyle);
                shiftCell.setCellStyle(cellStyle);
            }

            rowIndex++;
        }

        // Автоматичне розтягування стовпців
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Збереження файлу
        try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
            workbook.write(fileOut);
        }

        // Закриття книги
        workbook.close();
    }

    public static void exportEmployees(List<Employee> employees, File outputFile) throws Exception {
        // Створюємо нову книгу Excel
        Workbook workbook = new XSSFWorkbook();

        // Створюємо лист
        Sheet sheet = workbook.createSheet("Працівники");

        // Налаштовуємо стилі
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createEmployeeStyle(workbook);

        // Заголовок
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Список працівників станом на " +
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        titleCell.setCellStyle(headerStyle);

        // Заголовки стовпців
        Row headerRow = sheet.createRow(2);
        String[] headers = {
                "ID", "ПІБ", "Посада", "Підрозділ", "Освіта", "Телефон",
                "Дата народження", "Дата прийому", "Профком", "Діти", "Місце проживання", "Інші дані"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000); // Ширина стовпців
        }

        // Заповнюємо дані
        int rowIndex = 3;
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        for (Employee employee : employees) {
            Row dataRow = sheet.createRow(rowIndex);

            // ID
            Cell idCell = dataRow.createCell(0);
            idCell.setCellValue(employee.getId());
            idCell.setCellStyle(dataStyle);

            // ПІБ
            Cell nameCell = dataRow.createCell(1);
            nameCell.setCellValue(employee.getFullName());
            nameCell.setCellStyle(dataStyle);

            // Посада
            Cell positionCell = dataRow.createCell(2);
            positionCell.setCellValue(employee.getPosition() != null ? employee.getPosition() : "");
            positionCell.setCellStyle(dataStyle);

            // Підрозділ
            Cell deptCell = dataRow.createCell(3);
            deptCell.setCellValue(employee.getDepartment() != null ? employee.getDepartment() : "");
            deptCell.setCellStyle(dataStyle);

            // Освіта
            Cell eduCell = dataRow.createCell(4);
            eduCell.setCellValue(employee.getEducation() != null ? employee.getEducation() : "");
            eduCell.setCellStyle(dataStyle);

            // Телефон
            Cell phoneCell = dataRow.createCell(5);
            phoneCell.setCellValue(employee.getPhone() != null ? employee.getPhone() : "");
            phoneCell.setCellStyle(dataStyle);

            // Дата народження
            Cell birthCell = dataRow.createCell(6);
            if (employee.getBirthDate() != null) {
                birthCell.setCellValue(employee.getBirthDate().format(dateFormatter));
            }
            birthCell.setCellStyle(dataStyle);

            // Дата прийому
            Cell hireCell = dataRow.createCell(7);
            if (employee.getHireDate() != null) {
                hireCell.setCellValue(employee.getHireDate().format(dateFormatter));
            }
            hireCell.setCellStyle(dataStyle);

            // Профком
            Cell profkomCell = dataRow.createCell(8);
            profkomCell.setCellValue(employee.getProfkom() != null ? employee.getProfkom() : "");
            profkomCell.setCellStyle(dataStyle);

            // Діти
            Cell childrenCell = dataRow.createCell(9);
            childrenCell.setCellValue(employee.getChildren() != null ? employee.getChildren() : "");
            childrenCell.setCellStyle(dataStyle);

            // Місце проживання та інші дані (парсимо з поля data)
            String residence = "";
            String otherData = "";
            if (employee.getData() != null) {
                String[] parts = employee.getData().split(";");
                for (String part : parts) {
                    if (part.trim().startsWith("Проживання:")) {
                        residence = part.replace("Проживання:", "").trim();
                    } else {
                        otherData += part.trim() + " ";
                    }
                }
            }

            Cell residenceCell = dataRow.createCell(10);
            residenceCell.setCellValue(residence);
            residenceCell.setCellStyle(dataStyle);

            Cell otherDataCell = dataRow.createCell(11);
            otherDataCell.setCellValue(otherData.trim());
            otherDataCell.setCellStyle(dataStyle);

            rowIndex++;
        }

        // Автоматичне розтягування стовпців
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Збереження файлу
        try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
            workbook.write(fileOut);
        }

        // Закриття книги
        workbook.close();
    }

    /**
     * Повертає стиль для клітинки залежно від типу зміни.
     */
    private static CellStyle getCellStyleForShift(String shiftCode,
                                                  CellStyle dayOffStyle,
                                                  CellStyle workingDayStyle,
                                                  CellStyle specialDayStyle) {
        if (shiftCode == null || "X".equals(shiftCode)) {
            return dayOffStyle;
        } else if ("1".equals(shiftCode) || "12".equals(shiftCode)) {
            return workingDayStyle;
        } else {
            return specialDayStyle;
        }
    }

    /**
     * Створює стиль для заголовків.
     */
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Створює стиль для даних працівників.
     */
    private static CellStyle createEmployeeStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Створює стиль для вихідних днів.
     */
    private static CellStyle createDayOffStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Створює стиль для робочих днів.
     */
    private static CellStyle createWorkingDayStyle(Workbook workbook) {
        CellStyle style = createDayOffStyle(workbook);
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    /**
     * Створює стиль для особливих днів.
     */
    private static CellStyle createSpecialDayStyle(Workbook workbook) {
        CellStyle style = createDayOffStyle(workbook);
        style.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    public static void exportToCSV(ScheduleService scheduleService,
                                   YearMonth month,
                                   File outputFile) throws Exception {

        // Отримуємо дані з сервісу
        Map<Employee, List<Shift>> scheduleData = scheduleService.loadScheduleForMonth(month);

        try (java.io.PrintWriter writer = new java.io.PrintWriter(outputFile, StandardCharsets.UTF_8.name())) {
            // Заголовок
            writer.println("Графік змін за " + DateUtil.formatMonthYear(month));
            writer.println();

            // Заголовки стовпців
            writer.print("ПІБ;Підрозділ;Посада;");
            int daysInMonth = DateUtil.getDaysInMonthCount(month);
            for (int day = 1; day <= daysInMonth; day++) {
                writer.print("День " + day + ";");
            }
            writer.println();

            // Дані
            for (Map.Entry<Employee, List<Shift>> entry : scheduleData.entrySet()) {
                Employee employee = entry.getKey();
                List<Shift> shifts = entry.getValue();

                // Створюємо мапу дата → код зміни
                Map<LocalDate, String> shiftMap = new java.util.HashMap<>();
                for (Shift shift : shifts) {
                    shiftMap.put(shift.getDate(), shift.getCode());
                }

                writer.print(employee.getFullName() + ";");
                writer.print(employee.getDepartment() + ";");
                writer.print(employee.getPosition() != null ? employee.getPosition() + ";" : ";");

                for (int day = 1; day <= daysInMonth; day++) {
                    LocalDate date = month.atDay(day);
                    String shiftCode = shiftMap.getOrDefault(date, "X");
                    writer.print(shiftCode + ";");
                }
                writer.println();
            }
        }
    }
}