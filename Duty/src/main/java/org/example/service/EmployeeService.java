package org.example.service;

import org.example.dao.EmployeeDAO;
import org.example.model.Employee;

import java.sql.SQLException;
import java.util.List;

public class EmployeeService {

    private final EmployeeDAO employeeDAO;

    public EmployeeService() {
        this.employeeDAO = new EmployeeDAO();
    }

    public void saveEmployee(Employee employee) throws SQLException {
        employeeDAO.save(employee);
    }

    public void deleteEmployee(Integer id) throws SQLException {
        employeeDAO.delete(id);
    }

    public Employee getEmployeeById(Integer id) throws SQLException {
        return employeeDAO.findById(id);
    }

    public List<Employee> getAllEmployees() throws SQLException {
        return employeeDAO.findAll();
    }

    public List<Employee> getEmployeesByDepartment(String department) throws SQLException {
        return employeeDAO.findByDepartment(department);
    }

    public List<String> getAllDepartments() throws SQLException {
        // Фільтруємо тільки три підрозділи, які залишилися
        List<String> allDepartments = employeeDAO.findAllDepartments();
        allDepartments.removeIf(dept -> !dept.equals("ГКНС") &&
                !dept.equals("Великорогізнянська") &&
                !dept.equals("Пром.район"));
        return allDepartments;
    }

    public List<Employee> getEmployeesByStatus(String status) throws SQLException {
        return employeeDAO.findByStatus(status);
    }
}