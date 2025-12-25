package org.example.service;

import org.example.dao.EmployeeDAO;
import org.example.dao.ShiftDAO;
import org.example.data.January2026DataInitializer;
import org.example.model.Employee;
import org.example.model.Shift;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseInitializer {

    private static final String DB_URL = "jdbc:sqlite:duty_schedule.db";

    public static void initializeDatabaseWithRealData() {
        System.out.println("🔧 Ініціалізація бази даних з реальними графіками...");

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                createTables(conn);
                updateDatabaseSchema(conn);
                checkAndInsertRealData(conn);
                System.out.println("✅ База даних успішно ініціалізована з реальними графіками");
            }
        } catch (SQLException e) {
            System.err.println("❌ Помилка ініціалізації БД: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void checkAndInsertRealData(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM employees");

        if (rs.next() && rs.getInt("count") == 0) {
            // Вставляємо працівників (по 5 в кожному підрозділі)
            insertRealEmployees(conn);

            // Вставляємо реальні графіки на січень 2026
            insertRealShiftsForJanuary2026(conn);

            // Вставляємо порожні графіки для інших місяців
            insertEmptyShiftsForOtherMonths(conn);
        } else {
            System.out.println("ℹ️ База даних вже містить дані, пропускаємо ініціалізацію");
        }

        rs.close();
        stmt.close();
    }

    private static void insertRealEmployees(Connection conn) throws SQLException {
        conn.setAutoCommit(false);

        try {
            List<Employee> employees = createRealEmployeeList();

            String insertEmployeeSQL = """
                INSERT INTO employees (full_name, position, department, education, phone, 
                                     birth_date, hire_date, status, shift_type,
                                     days_off_after, days_off_before, pattern_type,
                                     profkom, children, data,
                                     last_work_code, last_x_count, last_work_day)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            PreparedStatement employeeStmt = conn.prepareStatement(insertEmployeeSQL,
                    Statement.RETURN_GENERATED_KEYS);

            for (Employee employee : employees) {
                employeeStmt.setString(1, employee.getFullName());
                employeeStmt.setString(2, employee.getPosition());
                employeeStmt.setString(3, employee.getDepartment());
                employeeStmt.setString(4, employee.getEducation());
                employeeStmt.setString(5, employee.getPhone());

                if (employee.getBirthDate() != null) {
                    employeeStmt.setDate(6, Date.valueOf(employee.getBirthDate()));
                } else {
                    employeeStmt.setNull(6, Types.DATE);
                }

                if (employee.getHireDate() != null) {
                    employeeStmt.setDate(7, Date.valueOf(employee.getHireDate()));
                } else {
                    employeeStmt.setNull(7, Types.DATE);
                }

                employeeStmt.setString(8, employee.getStatus());
                employeeStmt.setString(9, employee.getShiftType());
                employeeStmt.setInt(10, employee.getDaysOffAfter());
                employeeStmt.setInt(11, employee.getDaysOffBefore());
                employeeStmt.setString(12, employee.getPatternType());

                // Нові поля
                employeeStmt.setString(13, employee.getProfkom());
                employeeStmt.setString(14, employee.getChildren());
                employeeStmt.setString(15, employee.getData());

                // Поля для аналізу
                employeeStmt.setString(16, employee.getLastWorkCode());
                employeeStmt.setInt(17, employee.getLastXCount());
                employeeStmt.setInt(18, employee.getLastWorkDay());

                employeeStmt.executeUpdate();

                ResultSet generatedKeys = employeeStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    employee.setId(generatedKeys.getInt(1));
                }
                generatedKeys.close();
            }

            employeeStmt.close();
            conn.commit();
            System.out.println("✅ Вставлено " + employees.size() + " працівників");

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static List<Employee> createRealEmployeeList() {
        List<Employee> employees = new ArrayList<>();

        // Великорогізнянська (5 осіб)
        employees.add(createEmployee("Верещака Т.Д.", "МНУ 4 р.", "Великорогізнянська"));
        employees.add(createEmployee("Опішнян Г.М.", "МНУ 4 р.", "Великорогізнянська"));
        employees.add(createEmployee("Шкурко С.В.", "МНУ 2 р.", "Великорогізнянська"));
        employees.add(createEmployee("Поварніцина Т.В.", "МНУ 4 р.", "Великорогізнянська"));
        employees.add(createEmployee("Курочка С.М.", "МНУ 4 р.", "Великорогізнянська"));

        // ГКНС (5 осіб)
        employees.add(createEmployee("Мисяк Ю.О.", "МНУ 4 р.", "ГКНС"));
        employees.add(createEmployee("Горбатко Я.В.", "МНУ 2р.", "ГКНС"));
        employees.add(createEmployee("Тесленко П.В.", "МНУ 4 р.", "ГКНС"));
        employees.add(createEmployee("Нестеренко Ю.А.", "МНУ 2 р.", "ГКНС"));
        employees.add(createEmployee("Шаповалова Л.А.", "МНУ 2 р.", "ГКНС"));

        // Пром.район (5 осіб)
        employees.add(createEmployee("Зірка Л.В.", "МНУ 2 р.", "Пром.район"));
        employees.add(createEmployee("Мороз Т.І.", "МНУ 2 р.", "Пром.район"));
        employees.add(createEmployee("Юсковець Т.М.", "МНУ 2 р.", "Пром.район"));
        employees.add(createEmployee("Хорошун Д.М.", "МНУ 2 р.", "Пром.район"));
        employees.add(createEmployee("Міхєєва Л.М.", "МНУ 2 р.", "Пром.район"));

        return employees;
    }

    private static void insertRealShiftsForJanuary2026(Connection conn) throws SQLException {
        conn.setAutoCommit(false);

        try {
            // Отримуємо всіх працівників з їх ID
            Map<String, Integer> employeeIdMap = getEmployeeIdMap(conn);

            // Отримуємо зміни
            List<Shift> shifts = January2026DataInitializer.getJanuary2026Shifts(employeeIdMap);

            // Вставляємо зміни
            String insertShiftSQL = "INSERT OR REPLACE INTO shifts (employee_id, date, code) VALUES (?, ?, ?)";
            PreparedStatement shiftStmt = conn.prepareStatement(insertShiftSQL);

            int batchSize = 0;
            for (Shift shift : shifts) {
                shiftStmt.setInt(1, shift.getEmployeeId());
                shiftStmt.setDate(2, Date.valueOf(shift.getDate()));
                shiftStmt.setString(3, shift.getCode());
                shiftStmt.addBatch();
                batchSize++;

                if (batchSize % 100 == 0) {
                    shiftStmt.executeBatch();
                }
            }

            shiftStmt.executeBatch();
            shiftStmt.close();

            conn.commit();
            System.out.println("✅ Вставлено " + shifts.size() + " змін на січень 2026");

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void insertEmptyShiftsForOtherMonths(Connection conn) throws SQLException {
        conn.setAutoCommit(false);

        try {
            // Отримуємо всіх працівників
            List<Employee> employees = getAllEmployees(conn);

            // Місяці, для яких створюємо порожні графіки
            YearMonth[] months = {
                    YearMonth.of(2026, 2),
                    YearMonth.of(2026, 3),
                    YearMonth.of(2026, 4),
                    YearMonth.of(2026, 5),
                    YearMonth.of(2026, 6),
                    YearMonth.of(2026, 7),
                    YearMonth.of(2026, 8),
                    YearMonth.of(2026, 9),
                    YearMonth.of(2026, 10),
                    YearMonth.of(2026, 11),
                    YearMonth.of(2026, 12)
            };

            String insertShiftSQL = "INSERT OR REPLACE INTO shifts (employee_id, date, code) VALUES (?, ?, ?)";
            PreparedStatement shiftStmt = conn.prepareStatement(insertShiftSQL);

            int totalShifts = 0;

            for (YearMonth month : months) {
                for (Employee employee : employees) {
                    if (employee.isCurrentlyWorking()) {
                        for (int day = 1; day <= month.lengthOfMonth(); day++) {
                            LocalDate date = month.atDay(day);
                            shiftStmt.setInt(1, employee.getId());
                            shiftStmt.setDate(2, Date.valueOf(date));
                            shiftStmt.setString(3, "X"); // Порожній графік - всі X
                            shiftStmt.addBatch();
                            totalShifts++;

                            if (totalShifts % 500 == 0) {
                                shiftStmt.executeBatch();
                            }
                        }
                    }
                }
            }

            shiftStmt.executeBatch();
            shiftStmt.close();
            conn.commit();

            System.out.println("✅ Створено порожні графіки для лютий-грудень 2026: " + totalShifts + " змін");

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Отримує мапу імен працівників до їх ID
     */
    private static Map<String, Integer> getEmployeeIdMap(Connection conn) throws SQLException {
        Map<String, Integer> employeeIdMap = new HashMap<>();

        String sql = "SELECT id, full_name FROM employees";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                employeeIdMap.put(rs.getString("full_name"), rs.getInt("id"));
            }
        }

        return employeeIdMap;
    }

    /**
     * Отримує всіх працівників з БД
     */
    private static List<Employee> getAllEmployees(Connection conn) throws SQLException {
        List<Employee> employees = new ArrayList<>();

        String sql = "SELECT * FROM employees";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Employee employee = new Employee();
                employee.setId(rs.getInt("id"));
                employee.setFullName(rs.getString("full_name"));
                employee.setDepartment(rs.getString("department"));
                employee.setStatus(rs.getString("status"));
                employees.add(employee);
            }
        }

        return employees;
    }

    private static Employee createEmployee(String fullName, String position, String department) {
        Employee employee = new Employee();
        employee.setFullName(fullName);
        employee.setPosition(position);
        employee.setDepartment(department);
        employee.setStatus("працює");
        employee.setShiftType("1");
        employee.setEducation("");
        employee.setPhone("");
        employee.setDaysOffAfter(1);
        employee.setDaysOffBefore(0);
        employee.setPatternType("1_3_1");
        employee.setProfkom("");
        employee.setChildren("");
        employee.setData("");
        employee.setLastWorkCode("");
        employee.setLastXCount(0);
        employee.setLastWorkDay(0);
        return employee;
    }

    private static void createTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();

        String createEmployeesTable = """
            CREATE TABLE IF NOT EXISTS employees (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name TEXT NOT NULL,
                position TEXT,
                department TEXT,
                education TEXT,
                phone TEXT,
                birth_date DATE,
                hire_date DATE,
                status TEXT DEFAULT 'працює',
                shift_type TEXT DEFAULT '1',
                days_off_after INTEGER DEFAULT 1,
                days_off_before INTEGER DEFAULT 0,
                pattern_type TEXT DEFAULT '1_3_1',
                profkom TEXT DEFAULT '',
                children TEXT DEFAULT '',
                data TEXT DEFAULT '',
                last_work_code TEXT,
                last_x_count INTEGER DEFAULT 0,
                last_work_day INTEGER
            )
        """;

        String createShiftsTable = """
            CREATE TABLE IF NOT EXISTS shifts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                employee_id INTEGER NOT NULL,
                date DATE NOT NULL,
                code TEXT DEFAULT 'X',
                notes TEXT,
                FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
                UNIQUE (employee_id, date)
            )
        """;

        stmt.execute(createEmployeesTable);
        stmt.execute(createShiftsTable);
        stmt.close();
    }

    private static void updateDatabaseSchema(Connection conn) {
        try {
            // Перевіряємо наявність нових колонок
            String[] newColumns = {"profkom", "children", "data", "last_work_code", "last_x_count", "last_work_day"};

            for (String column : newColumns) {
                if (!isColumnExists(conn, "employees", column)) {
                    addColumnToEmployees(conn, column);
                }
            }

        } catch (SQLException e) {
            System.err.println("⚠️ Помилка оновлення схеми БД: " + e.getMessage());
        }
    }

    private static boolean isColumnExists(Connection conn, String table, String column) throws SQLException {
        String sql = "PRAGMA table_info(" + table + ")";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                if (column.equals(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void addColumnToEmployees(Connection conn, String column) throws SQLException {
        String sqlType;

        switch (column) {
            case "profkom":
            case "children":
            case "data":
            case "last_work_code":
                sqlType = "TEXT DEFAULT ''";
                break;
            case "last_x_count":
            case "last_work_day":
                sqlType = "INTEGER DEFAULT 0";
                break;
            default:
                return;
        }

        String sql = "ALTER TABLE employees ADD COLUMN " + column + " " + sqlType;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("✅ Додано колонку " + column + " до таблиці employees");
        }
    }

    public static void initializeDatabase() {
        initializeDatabaseWithRealData();
    }

    public static void resetDatabase() {
        System.out.println("🔄 Скидання бази даних...");

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // Видаляємо всі дані
            stmt.execute("DELETE FROM shifts");
            stmt.execute("DELETE FROM employees");

            // Скидаємо автоінкремент
            stmt.execute("DELETE FROM sqlite_sequence WHERE name='employees'");
            stmt.execute("DELETE FROM sqlite_sequence WHERE name='shifts'");

            // Повторно ініціалізуємо
            initializeDatabaseWithRealData();

            System.out.println("✅ База даних скинута та переініціалізована");

        } catch (SQLException e) {
            System.err.println("❌ Помилка скидання БД: " + e.getMessage());
            e.printStackTrace();
        }
    }
}