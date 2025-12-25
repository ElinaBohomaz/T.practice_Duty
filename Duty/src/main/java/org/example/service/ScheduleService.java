package org.example.service;

import org.example.dao.EmployeeDAO;
import org.example.dao.ShiftDAO;
import org.example.model.Employee;
import org.example.model.Shift;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.*;

public class ScheduleService {

    private final EmployeeDAO employeeDAO;
    private final ShiftDAO shiftDAO;

    public ScheduleService() {
        this.employeeDAO = new EmployeeDAO();
        this.shiftDAO = new ShiftDAO();
    }

    /**
     * Завантажити графік за місяць (з працівниками)
     */
    public Map<Employee, List<Shift>> loadScheduleForMonth(YearMonth month) throws SQLException {
        Map<Employee, List<Shift>> scheduleMap = new HashMap<>();

        // Отримуємо всі зміни за місяць (груповані по працівниках)
        Map<Integer, List<Shift>> shiftsByEmployee = shiftDAO.findShiftsForMonth(month);

        // Отримуємо всіх працівників
        List<Employee> allEmployees = employeeDAO.findAll();

        // Створюємо мапу працівників за ID для швидкого пошуку
        Map<Integer, Employee> employeeMap = new HashMap<>();
        for (Employee emp : allEmployees) {
            employeeMap.put(emp.getId(), emp);
        }

        // Створюємо повну мапу з працівниками та їх змінами
        for (Map.Entry<Integer, List<Shift>> entry : shiftsByEmployee.entrySet()) {
            Employee employee = employeeMap.get(entry.getKey());
            if (employee != null) {
                scheduleMap.put(employee, entry.getValue());
            }
        }

        // Додаємо працівників без змін
        for (Employee employee : allEmployees) {
            if (!scheduleMap.containsKey(employee)) {
                scheduleMap.put(employee, new ArrayList<>());
            }
        }

        return scheduleMap;
    }

    /**
     * Зберегти список змін
     */
    public void saveShifts(List<Shift> shifts) throws SQLException {
        if (shifts == null || shifts.isEmpty()) {
            return;
        }
        shiftDAO.saveBatch(shifts);
    }
}