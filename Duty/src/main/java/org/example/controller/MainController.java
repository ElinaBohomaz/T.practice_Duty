package org.example.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.example.model.Employee;
import org.example.model.Shift;
import org.example.service.DatabaseInitializer;
import org.example.service.EmployeeService;
import org.example.service.ScheduleContinuationService;
import org.example.service.ScheduleService;
import org.example.util.ExcelExporter;

import java.io.File;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

@SuppressWarnings("unused")
public class MainController {

    @FXML private ComboBox<String> monthComboBox;
    @FXML private ComboBox<String> yearComboBox;
    @FXML private ComboBox<String> departmentComboBox;
    @FXML private ComboBox<String> dayStatusComboBox;
    @FXML private ComboBox<String> dayFilterComboBox;
    @FXML private TextField searchField;
    @FXML private TableView<EmployeeScheduleRow> scheduleTableView;
    @FXML private Label statusLabel;
    @FXML private TabPane mainTabPane;
    @FXML private Tab employeeTab;

    // Поля для вкладки працівників
    @FXML private TableView<EmployeeFullInfo> employeeTableView;
    @FXML private TextField employeeSearchField;
    @FXML private ComboBox<String> employeeDepartmentFilter;
    @FXML private ComboBox<String> employeeStatusFilter;
    @FXML private Label employeeStatusLabel;

    // Кнопки для працівників
    @FXML private Button addEmployeeButton;
    @FXML private Button editEmployeeButton;
    @FXML private Button deleteEmployeeButton;
    @FXML private Button refreshEmployeeButton;
    @FXML private Button exportEmployeesButton;

    private final ScheduleService scheduleService = new ScheduleService();
    private final EmployeeService employeeService = new EmployeeService();
    private final ScheduleContinuationService continuationService = new ScheduleContinuationService();

    private YearMonth currentMonth;
    private final ObservableList<EmployeeScheduleRow> allScheduleRows = FXCollections.observableArrayList();
    private final FilteredList<EmployeeScheduleRow> filteredScheduleRows = new FilteredList<>(allScheduleRows);
    private final Map<Integer, TableColumn<EmployeeScheduleRow, String>> dayColumns = new HashMap<>();

    // Дані для вкладки працівників
    private final ObservableList<EmployeeFullInfo> employeeData = FXCollections.observableArrayList();
    private final FilteredList<EmployeeFullInfo> filteredEmployees = new FilteredList<>(employeeData);

    private final Map<LocalDate, String> holidays = new HashMap<>();
    private final List<LocalDate> weekendDates = new ArrayList<>();
    private final Map<LocalDate, String> holidayDates = new HashMap<>();

    private final List<Shift> pendingShiftsToSave = new ArrayList<>();
    private boolean hasUnsavedChanges = false;

    private org.example.dao.ShiftDAO shiftDAO = new org.example.dao.ShiftDAO();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @FXML
    private void initialize() {
        System.out.println("✅ MainController ініціалізовано");

        DatabaseInitializer.initializeDatabase();
        initializeHolidays();
        setupComboBoxes();
        setupScheduleTableView();
        setupEmployeeTableViewStyles();

        scheduleTableView.setItems(filteredScheduleRows);
        scheduleTableView.setEditable(true);

        loadCurrentMonth();
        updateDepartmentComboBox();

        // Ініціалізація вкладки працівників
        setupEmployeeTab();

        // Налаштування стилів для гортання
        setupTableScrolling();
    }

    private void setupTableScrolling() {
        // Налаштування гортання для таблиці графіка
        scheduleTableView.setOnScroll(event -> {
            double deltaY = event.getDeltaY();
            scheduleTableView.lookup(".scroll-bar:horizontal").setOnScroll(e -> {
                // Горизонтальне гортання
                double currentValue = scheduleTableView.getScaleX();
                scheduleTableView.scrollToColumnIndex((int) (currentValue + deltaY * 0.1));
                e.consume();
            });
        });

        // Налаштування гортання для таблиці працівників
        employeeTableView.setOnScroll(event -> {
            double deltaY = event.getDeltaY();
            employeeTableView.lookup(".scroll-bar:horizontal").setOnScroll(e -> {
                // Горизонтальне гортання
                double currentValue = employeeTableView.getScaleX();
                employeeTableView.scrollToColumnIndex((int) (currentValue + deltaY * 0.1));
                e.consume();
            });
        });
    }

    private void setupEmployeeTableViewStyles() {

        employeeTableView.setStyle("-fx-background-color: white; -fx-border-color: #c2e6c4; -fx-border-radius: 10;");
    }

    // Ініціалізація вкладки працівників
    private void setupEmployeeTab() {
        if (employeeTableView != null) {
            setupEmployeeTableView();
            loadEmployees();
            setupEmployeeFilters();
        }
    }

    private void setupEmployeeTableView() {
        employeeTableView.getColumns().clear();

        // Колонка ID
        TableColumn<EmployeeFullInfo, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setPrefWidth(50);
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");

        // Колонка ПІБ
        TableColumn<EmployeeFullInfo, String> nameColumn = new TableColumn<>("ПІБ");
        nameColumn.setPrefWidth(200);
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        nameColumn.setStyle("-fx-font-weight: bold; -fx-alignment: CENTER_LEFT;");

        // Колонка Посада
        TableColumn<EmployeeFullInfo, String> positionColumn = new TableColumn<>("Посада");
        positionColumn.setPrefWidth(150);
        positionColumn.setCellValueFactory(new PropertyValueFactory<>("position"));
        positionColumn.setStyle("-fx-alignment: CENTER;");

        // Колонка Підрозділ
        TableColumn<EmployeeFullInfo, String> deptColumn = new TableColumn<>("Підрозділ");
        deptColumn.setPrefWidth(150);
        deptColumn.setCellValueFactory(new PropertyValueFactory<>("department"));
        deptColumn.setStyle("-fx-alignment: CENTER;");

        // Колонка Освіта
        TableColumn<EmployeeFullInfo, String> educationColumn = new TableColumn<>("Освіта");
        educationColumn.setPrefWidth(150);
        educationColumn.setCellValueFactory(new PropertyValueFactory<>("education"));
        educationColumn.setStyle("-fx-alignment: CENTER;");

        // Колонка Телефон
        TableColumn<EmployeeFullInfo, String> phoneColumn = new TableColumn<>("Телефон");
        phoneColumn.setPrefWidth(120);
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        phoneColumn.setStyle("-fx-alignment: CENTER;");

        // Колонка Дата народження
        TableColumn<EmployeeFullInfo, String> birthDateColumn = new TableColumn<>("Дата народження");
        birthDateColumn.setPrefWidth(120);
        birthDateColumn.setCellValueFactory(cellData -> {
            String date = cellData.getValue().getBirthDate();
            return new SimpleStringProperty(date != null ? date : "");
        });
        birthDateColumn.setStyle("-fx-alignment: CENTER;");

        // Колонка Дата прийому
        TableColumn<EmployeeFullInfo, String> hireDateColumn = new TableColumn<>("Дата прийому");
        hireDateColumn.setPrefWidth(120);
        hireDateColumn.setCellValueFactory(cellData -> {
            String date = cellData.getValue().getHireDate();
            return new SimpleStringProperty(date != null ? date : "");
        });
        hireDateColumn.setStyle("-fx-alignment: CENTER;");

        // Колонка Місце проживання
        TableColumn<EmployeeFullInfo, String> residenceColumn = new TableColumn<>("Місце проживання");
        residenceColumn.setPrefWidth(200);
        residenceColumn.setCellValueFactory(new PropertyValueFactory<>("residence"));
        residenceColumn.setStyle("-fx-alignment: CENTER_LEFT;");

        // Колонка Профком
        TableColumn<EmployeeFullInfo, String> profkomColumn = new TableColumn<>("Профком");
        profkomColumn.setPrefWidth(80);
        profkomColumn.setCellValueFactory(new PropertyValueFactory<>("profkom"));
        profkomColumn.setStyle("-fx-alignment: CENTER;");

        // Колонка Діти
        TableColumn<EmployeeFullInfo, String> childrenColumn = new TableColumn<>("Діти");
        childrenColumn.setPrefWidth(100);
        childrenColumn.setCellValueFactory(new PropertyValueFactory<>("children"));
        childrenColumn.setStyle("-fx-alignment: CENTER;");

        // Колонка Інші дані
        TableColumn<EmployeeFullInfo, String> otherDataColumn = new TableColumn<>("Інші дані");
        otherDataColumn.setPrefWidth(150);
        otherDataColumn.setCellValueFactory(new PropertyValueFactory<>("otherData"));
        otherDataColumn.setStyle("-fx-alignment: CENTER_LEFT;");

        employeeTableView.getColumns().addAll(
                idColumn, nameColumn, positionColumn, deptColumn, educationColumn,
                phoneColumn, birthDateColumn, hireDateColumn, residenceColumn,
                profkomColumn, childrenColumn, otherDataColumn
        );
    }

    private void loadEmployees() {
        try {
            List<Employee> employees = employeeService.getAllEmployees();
            employeeData.clear();

            for (Employee emp : employees) {
                employeeData.add(new EmployeeFullInfo(
                        emp.getId(),
                        emp.getFullName(),
                        emp.getPosition(),
                        emp.getDepartment(),
                        emp.getEducation(),
                        emp.getPhone(),
                        emp.getBirthDate() != null ? emp.getBirthDate().format(dateFormatter) : "",
                        emp.getHireDate() != null ? emp.getHireDate().format(dateFormatter) : "",
                        emp.getProfkom(),
                        emp.getChildren(),
                        emp.getData()  // Місце проживання та інші дані
                ));
            }

            // Налаштування фільтрації
            SortedList<EmployeeFullInfo> sortedData = new SortedList<>(filteredEmployees);
            sortedData.comparatorProperty().bind(employeeTableView.comparatorProperty());
            employeeTableView.setItems(sortedData);

            updateEmployeeStatusLabel();

        } catch (SQLException e) {
            showError("Помилка", "Не вдалося завантажити працівників: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupEmployeeFilters() {
        // Налаштування пошуку
        employeeSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterEmployees();
        });

        // Налаштування фільтрів відділів
        try {
            List<String> departments = employeeService.getAllDepartments();
            ObservableList<String> deptOptions = FXCollections.observableArrayList("Всі підрозділи");
            deptOptions.addAll(departments);
            employeeDepartmentFilter.setItems(deptOptions);
            employeeDepartmentFilter.getSelectionModel().selectFirst();
            employeeDepartmentFilter.setOnAction(event -> filterEmployees());
        } catch (SQLException e) {
            employeeDepartmentFilter.setItems(FXCollections.observableArrayList("Всі підрозділи"));
            employeeDepartmentFilter.getSelectionModel().selectFirst();
        }

        employeeStatusFilter.setVisible(false);
        employeeStatusFilter.setManaged(false);
    }

    private void filterEmployees() {
        String searchText = employeeSearchField.getText().toLowerCase();
        String selectedDept = employeeDepartmentFilter.getValue();

        filteredEmployees.setPredicate(employee -> {
            if (employee == null) return false;

            // Фільтр пошуку
            if (!searchText.isEmpty()) {
                boolean matchesSearch =
                        (employee.getFullName() != null && employee.getFullName().toLowerCase().contains(searchText)) ||
                                (employee.getPosition() != null && employee.getPosition().toLowerCase().contains(searchText)) ||
                                (employee.getDepartment() != null && employee.getDepartment().toLowerCase().contains(searchText)) ||
                                (employee.getEducation() != null && employee.getEducation().toLowerCase().contains(searchText)) ||
                                (employee.getPhone() != null && employee.getPhone().contains(searchText)) ||
                                (employee.getResidence() != null && employee.getResidence().toLowerCase().contains(searchText));
                if (!matchesSearch) return false;
            }

            // Фільтр відділу
            if (selectedDept != null && !selectedDept.equals("Всі підрозділи")) {
                if (!selectedDept.equals(employee.getDepartment())) {
                    return false;
                }
            }

            return true;
        });

        updateEmployeeStatusLabel();
    }

    private void updateEmployeeStatusLabel() {
        int total = employeeData.size();
        int filtered = filteredEmployees.size();

        if (employeeStatusLabel != null) {
            if (filtered == total) {
                employeeStatusLabel.setText("Загальна кількість працівників: " + total);
            } else {
                employeeStatusLabel.setText("Знайдено: " + filtered + " з " + total);
            }
        }
    }

    // Методи для кнопок у вкладці працівників
    @FXML
    private void addEmployee() {
        try {
            // Діалог для додавання нового працівника
            Dialog<Employee> dialog = new Dialog<>();
            dialog.setTitle("Додати нового працівника");
            dialog.setHeaderText("Введіть дані нового працівника");

            // Створюємо поля форми
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            TextField fullNameField = new TextField();
            fullNameField.setPromptText("ПІБ");
            TextField positionField = new TextField();
            positionField.setPromptText("Посада");
            TextField departmentField = new TextField();
            departmentField.setPromptText("Підрозділ");
            TextField educationField = new TextField();
            educationField.setPromptText("Освіта");
            TextField phoneField = new TextField();
            phoneField.setPromptText("Телефон");
            DatePicker birthDatePicker = new DatePicker();
            birthDatePicker.setPromptText("Дата народження");
            DatePicker hireDatePicker = new DatePicker();
            hireDatePicker.setPromptText("Дата прийому");
            TextField residenceField = new TextField();
            residenceField.setPromptText("Місце проживання");
            TextField profkomField = new TextField();
            profkomField.setPromptText("Профком (так/ні)");
            TextField childrenField = new TextField();
            childrenField.setPromptText("Діти (кількість/вік)");
            TextField otherDataField = new TextField();
            otherDataField.setPromptText("Інші дані");

            grid.add(new Label("ПІБ:"), 0, 0);
            grid.add(fullNameField, 1, 0);
            grid.add(new Label("Посада:"), 0, 1);
            grid.add(positionField, 1, 1);
            grid.add(new Label("Підрозділ:"), 0, 2);
            grid.add(departmentField, 1, 2);
            grid.add(new Label("Освіта:"), 0, 3);
            grid.add(educationField, 1, 3);
            grid.add(new Label("Телефон:"), 0, 4);
            grid.add(phoneField, 1, 4);
            grid.add(new Label("Дата народження:"), 0, 5);
            grid.add(birthDatePicker, 1, 5);
            grid.add(new Label("Дата прийому:"), 0, 6);
            grid.add(hireDatePicker, 1, 6);
            grid.add(new Label("Місце проживання:"), 0, 7);
            grid.add(residenceField, 1, 7);
            grid.add(new Label("Профком:"), 0, 8);
            grid.add(profkomField, 1, 8);
            grid.add(new Label("Діти:"), 0, 9);
            grid.add(childrenField, 1, 9);
            grid.add(new Label("Інші дані:"), 0, 10);
            grid.add(otherDataField, 1, 10);

            dialog.getDialogPane().setContent(grid);

            // Кнопки діалогу
            ButtonType addButton = new ButtonType("Додати", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(addButton, cancelButton);

            // Обробка результату
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == addButton) {
                    Employee newEmployee = new Employee();
                    newEmployee.setFullName(fullNameField.getText());
                    newEmployee.setPosition(positionField.getText());
                    newEmployee.setDepartment(departmentField.getText());
                    newEmployee.setEducation(educationField.getText());
                    newEmployee.setPhone(phoneField.getText());
                    newEmployee.setBirthDate(birthDatePicker.getValue());
                    newEmployee.setHireDate(hireDatePicker.getValue());
                    newEmployee.setProfkom(profkomField.getText());
                    newEmployee.setChildren(childrenField.getText());

                    // Об'єднуємо місце проживання та інші дані
                    String allData = "Проживання: " + residenceField.getText();
                    if (!otherDataField.getText().isEmpty()) {
                        allData += "; " + otherDataField.getText();
                    }
                    newEmployee.setData(allData);

                    newEmployee.setStatus("працює");
                    return newEmployee;
                }
                return null;
            });

            Optional<Employee> result = dialog.showAndWait();
            result.ifPresent(employee -> {
                try {
                    // Отримуємо наступний доступний ID
                    List<Employee> allEmployees = employeeService.getAllEmployees();
                    int maxId = allEmployees.stream()
                            .mapToInt(Employee::getId)
                            .max()
                            .orElse(0);
                    employee.setId(maxId + 1);

                    employeeService.saveEmployee(employee);
                    loadEmployees();
                    showStatus("Працівника додано: " + employee.getFullName());
                } catch (SQLException e) {
                    showError("Помилка", "Не вдалося зберегти працівника: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            showError("Помилка", "Помилка при додаванні працівника: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void editEmployee() {
        EmployeeFullInfo selected = employeeTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Помилка", "Виберіть працівника для редагування");
            return;
        }

        try {
            // Отримуємо повні дані працівника з БД
            Employee employee = employeeService.getEmployeeById(selected.getId());
            if (employee == null) {
                showError("Помилка", "Не вдалося знайти працівника в БД");
                return;
            }

            // Діалог для редагування працівника
            Dialog<Employee> dialog = new Dialog<>();
            dialog.setTitle("Редагувати працівника");
            dialog.setHeaderText("Редагування даних працівника: " + employee.getFullName());

            // Створюємо поля форми
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            TextField fullNameField = new TextField(employee.getFullName());
            TextField positionField = new TextField(employee.getPosition());
            TextField departmentField = new TextField(employee.getDepartment());
            TextField educationField = new TextField(employee.getEducation());
            TextField phoneField = new TextField(employee.getPhone());
            DatePicker birthDatePicker = new DatePicker(employee.getBirthDate());
            DatePicker hireDatePicker = new DatePicker(employee.getHireDate());

            // Парсимо місце проживання з даних
            String residence = "";
            String otherData = employee.getData();
            if (otherData != null && otherData.contains("Проживання:")) {
                String[] parts = otherData.split(";");
                residence = parts[0].replace("Проживання:", "").trim();
                if (parts.length > 1) {
                    otherData = parts[1].trim();
                } else {
                    otherData = "";
                }
            }

            TextField residenceField = new TextField(residence);
            TextField profkomField = new TextField(employee.getProfkom());
            TextField childrenField = new TextField(employee.getChildren());
            TextField otherDataField = new TextField(otherData);

            grid.add(new Label("ПІБ:"), 0, 0);
            grid.add(fullNameField, 1, 0);
            grid.add(new Label("Посада:"), 0, 1);
            grid.add(positionField, 1, 1);
            grid.add(new Label("Підрозділ:"), 0, 2);
            grid.add(departmentField, 1, 2);
            grid.add(new Label("Освіта:"), 0, 3);
            grid.add(educationField, 1, 3);
            grid.add(new Label("Телефон:"), 0, 4);
            grid.add(phoneField, 1, 4);
            grid.add(new Label("Дата народження:"), 0, 5);
            grid.add(birthDatePicker, 1, 5);
            grid.add(new Label("Дата прийому:"), 0, 6);
            grid.add(hireDatePicker, 1, 6);
            grid.add(new Label("Місце проживання:"), 0, 7);
            grid.add(residenceField, 1, 7);
            grid.add(new Label("Профком:"), 0, 8);
            grid.add(profkomField, 1, 8);
            grid.add(new Label("Діти:"), 0, 9);
            grid.add(childrenField, 1, 9);
            grid.add(new Label("Інші дані:"), 0, 10);
            grid.add(otherDataField, 1, 10);

            dialog.getDialogPane().setContent(grid);

            // Кнопки діалогу
            ButtonType saveButton = new ButtonType("Зберегти", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButton, cancelButton);

            // Обробка результату
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButton) {
                    employee.setFullName(fullNameField.getText());
                    employee.setPosition(positionField.getText());
                    employee.setDepartment(departmentField.getText());
                    employee.setEducation(educationField.getText());
                    employee.setPhone(phoneField.getText());
                    employee.setBirthDate(birthDatePicker.getValue());
                    employee.setHireDate(hireDatePicker.getValue());
                    employee.setProfkom(profkomField.getText());
                    employee.setChildren(childrenField.getText());

                    // Об'єднуємо місце проживання та інші дані
                    String allData = "Проживання: " + residenceField.getText();
                    if (!otherDataField.getText().isEmpty()) {
                        allData += "; " + otherDataField.getText();
                    }
                    employee.setData(allData);

                    return employee;
                }
                return null;
            });

            Optional<Employee> result = dialog.showAndWait();
            result.ifPresent(updatedEmployee -> {
                try {
                    employeeService.saveEmployee(updatedEmployee);
                    loadEmployees();
                    showStatus("Дані працівника оновлено: " + updatedEmployee.getFullName());
                } catch (SQLException e) {
                    showError("Помилка", "Не вдалося оновити дані працівника: " + e.getMessage());
                }
            });

        } catch (SQLException e) {
            showError("Помилка", "Не вдалося завантажити дані працівника: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void deleteEmployee() {
        EmployeeFullInfo selected = employeeTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Помилка", "Виберіть працівника для видалення");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Видалення працівника");
        alert.setHeaderText("Видалити працівника " + selected.getFullName() + "?");
        alert.setContentText("Ця дія видалить всі дані про працівника, включаючи графіки змін.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                employeeService.deleteEmployee(selected.getId());
                loadEmployees();
                showStatus("Працівника видалено: " + selected.getFullName());
            } catch (SQLException e) {
                showError("Помилка", "Не вдалося видалити працівника: " + e.getMessage());
            }
        }
    }

    @FXML
    private void refreshEmployees() {
        loadEmployees();
        showStatus("Список працівників оновлено");
    }

    @FXML
    private void exportEmployeesToExcel() {
        try {
            if (employeeData.isEmpty()) {
                showError("Помилка", "Немає даних для експорту");
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Експорт працівників у Excel");
            fileChooser.setInitialFileName("Працівники_" + LocalDate.now() + ".xlsx");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Excel файли", "*.xlsx")
            );

            Stage stage = (Stage) employeeTableView.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);

            if (file != null) {
                List<Employee> employees = new ArrayList<>();
                for (EmployeeFullInfo info : employeeData) {
                    Employee emp = new Employee();
                    emp.setId(info.getId());
                    emp.setFullName(info.getFullName());
                    emp.setPosition(info.getPosition());
                    emp.setDepartment(info.getDepartment());
                    emp.setEducation(info.getEducation());
                    emp.setPhone(info.getPhone());

                    try {
                        if (!info.getBirthDate().isEmpty()) {
                            emp.setBirthDate(LocalDate.parse(info.getBirthDate(), dateFormatter));
                        }
                        if (!info.getHireDate().isEmpty()) {
                            emp.setHireDate(LocalDate.parse(info.getHireDate(), dateFormatter));
                        }
                    } catch (Exception e) {
                        System.err.println("Помилка парсингу дати: " + e.getMessage());
                    }

                    emp.setProfkom(info.getProfkom());
                    emp.setChildren(info.getChildren());
                    emp.setData(info.getResidence() + "; " + info.getOtherData());
                    employees.add(emp);
                }

                // Експорт в Excel
                ExcelExporter.exportEmployees(employees, file);
                showStatus("Експорт завершено: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            showError("Помилка експорту", "Не вдалося експортувати працівників: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onLoadButtonClick() {
        loadSchedule();
    }

    @FXML
    private void openEmployeeCards() {
        if (mainTabPane != null && employeeTab != null) {
            mainTabPane.getSelectionModel().select(employeeTab);
            showStatus("Відкрито вкладку працівників");
        }
    }

    @FXML
    private void onContinueButtonClick() {
        try {
            if (currentMonth == null) {
                showError("Помилка", "Не вибрано місяць для продовження");
                return;
            }

            boolean isJanuary2026 = currentMonth.equals(YearMonth.of(2026, 1));
            if (isJanuary2026) {
                showStatus("Січень 2026 - основний місяць, продовження не потрібне");
                return;
            }

            boolean hasExistingShifts = continuationService.hasShiftsForMonth(currentMonth);

            if (hasExistingShifts) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Попередження");
                alert.setHeaderText("Увага!");
                alert.setContentText("Для " + getMonthName(currentMonth.getMonthValue()) + " " +
                        currentMonth.getYear() + " вже є дані в БД.\n" +
                        "Продовжити все одно? Це перезапише існуючі зміни.");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.OK) {
                    showStatus("Продовження скасовано");
                    return;
                }
            }

            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Продовження графіку");
            confirmAlert.setHeaderText("Продовжити графік на " +
                    getMonthName(currentMonth.getMonthValue()) + " " + currentMonth.getYear() + "?");
            confirmAlert.setContentText("Ця операція:\n" +
                    "1. Проаналізує останні дні попереднього місяця\n" +
                    "2. Згенерує нові зміни згідно з логікою продовження\n" +
                    "3. Збереже результат у БД");

            ButtonType yesButton = new ButtonType("✅ Так, продовжити");
            ButtonType noButton = new ButtonType("❌ Ні, скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmAlert.getButtonTypes().setAll(yesButton, noButton);

            Optional<ButtonType> confirmResult = confirmAlert.showAndWait();
            if (confirmResult.isPresent() && confirmResult.get() == yesButton) {
                // Виконуємо продовження
                continuationService.loadAndContinueSchedule(currentMonth);

                // Оновлюємо таблицю
                loadSchedule();

                showStatus("✅ Графік успішно продовжено на " +
                        getMonthName(currentMonth.getMonthValue()) + " " + currentMonth.getYear());
            } else {
                showStatus("Продовження скасовано користувачем");
            }

        } catch (SQLException e) {
            showError("Помилка БД", "Не вдалося продовжити графік: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showError("Помилка", "Неочікувана помилка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupScheduleTableView() {
        scheduleTableView.getColumns().clear();

        TableColumn<EmployeeScheduleRow, String> nameColumn = new TableColumn<>("ПІБ");
        nameColumn.setPrefWidth(200);
        nameColumn.setCellValueFactory(cellData -> {
            Employee emp = cellData.getValue().getEmployee();
            return new SimpleStringProperty(emp.getFullName());
        });
        nameColumn.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-alignment: CENTER_LEFT;");

        TableColumn<EmployeeScheduleRow, String> mnuColumn = new TableColumn<>("МНУ");
        mnuColumn.setPrefWidth(120);
        mnuColumn.setCellValueFactory(cellData -> {
            Employee emp = cellData.getValue().getEmployee();
            return new SimpleStringProperty(emp.getPosition());
        });
        mnuColumn.setStyle("-fx-font-size: 13px; -fx-alignment: CENTER;");

        TableColumn<EmployeeScheduleRow, String> departmentColumn = new TableColumn<>("Підрозділ");
        departmentColumn.setPrefWidth(150);
        departmentColumn.setCellValueFactory(cellData -> {
            Employee emp = cellData.getValue().getEmployee();
            return new SimpleStringProperty(emp.getDepartment());
        });
        departmentColumn.setStyle("-fx-font-size: 13px; -fx-alignment: CENTER;");

        scheduleTableView.getColumns().addAll(nameColumn, mnuColumn, departmentColumn);
    }

    private void initializeHolidays() {
        holidays.put(LocalDate.of(2000, 1, 1), "Новий рік");
        holidays.put(LocalDate.of(2000, 1, 7), "Різдво Христове (православне)");
        holidays.put(LocalDate.of(2000, 3, 8), "Міжнародний жіночий день");
        holidays.put(LocalDate.of(2000, 5, 1), "День праці");
        holidays.put(LocalDate.of(2000, 5, 9), "День перемоги над нацизмом");
        holidays.put(LocalDate.of(2000, 6, 28), "День Конституції України");
        holidays.put(LocalDate.of(2000, 8, 24), "День Незалежності України");
        holidays.put(LocalDate.of(2000, 10, 14), "День захисників України");
        holidays.put(LocalDate.of(2000, 12, 25), "Різдво Христове (католицьке)");
        holidays.put(LocalDate.of(2026, 4, 12), "Великдень (православний)");
        holidays.put(LocalDate.of(2026, 5, 31), "Трійця");
    }

    private void setupComboBoxes() {
        ObservableList<String> months = FXCollections.observableArrayList(
                "Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
                "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"
        );
        monthComboBox.setItems(months);
        monthComboBox.setValue("Січень");

        ObservableList<String> years = FXCollections.observableArrayList();
        for (int i = 2024; i <= 2027; i++) {
            years.add(String.valueOf(i));
        }
        yearComboBox.setItems(years);
        yearComboBox.setValue("2026");

        if (dayStatusComboBox != null) {
            ObservableList<String> statuses = FXCollections.observableArrayList(
                    "Всі", "Працює", "Вихідний", "Лікарняний", "Відпустка", "Відрядження",
                    "Відгул", "Перенесення", "Тимчасово непрацездатний"
            );
            dayStatusComboBox.setItems(statuses);
            dayStatusComboBox.setValue("Всі");
        }

        if (dayFilterComboBox != null) {
            dayFilterComboBox.getItems().add("Всі дні");
            dayFilterComboBox.setValue("Всі дні");
        }

        yearComboBox.setOnAction(event -> handlePeriodChange());
        monthComboBox.setOnAction(event -> handlePeriodChange());
        departmentComboBox.setOnAction(event -> filterByDepartment());

        if (dayStatusComboBox != null) {
            dayStatusComboBox.setOnAction(event -> filterByDayAndStatus());
        }

        if (dayFilterComboBox != null) {
            dayFilterComboBox.setOnAction(event -> filterByDayAndStatus());
        }
    }

    private void handlePeriodChange() {
        if (yearComboBox.getValue() != null && monthComboBox.getValue() != null) {
            String monthStr = monthComboBox.getValue();
            String yearStr = yearComboBox.getValue();
            int monthIndex = getMonthIndex(monthStr);
            int year = Integer.parseInt(yearStr);
            YearMonth newMonth = YearMonth.of(year, monthIndex + 1);

            if (currentMonth != null && currentMonth.equals(newMonth)) {
                return;
            }

            if (hasUnsavedChanges && currentMonth != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Незбережені зміни");
                alert.setHeaderText("У вас є незбережені зміни для " +
                        getMonthName(currentMonth.getMonthValue()) + " " + currentMonth.getYear());
                alert.setContentText("Ви хочете зберегти зміни перед переходом до " + monthStr + " " + yearStr + "?");

                ButtonType saveButton = new ButtonType("💾 Зберегти");
                ButtonType discardButton = new ButtonType("🗑️ Скасувати зміни");
                ButtonType cancelButton = new ButtonType("↩️ Повернутися", ButtonBar.ButtonData.CANCEL_CLOSE);

                alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent()) {
                    if (result.get() == saveButton) {
                        saveSchedule();
                        currentMonth = newMonth;
                        loadSchedule();
                    } else if (result.get() == discardButton) {
                        pendingShiftsToSave.clear();
                        hasUnsavedChanges = false;
                        showStatus("Зміни скасовано");
                        currentMonth = newMonth;
                        loadSchedule();
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            } else {
                currentMonth = newMonth;
                loadSchedule();
            }
        }
    }

    @FXML
    private void loadSchedule() {
        try {
            if (currentMonth == null) {
                showError("Помилка", "Не вибрано місяць для завантаження");
                return;
            }

            boolean isJanuary2026 = currentMonth.equals(YearMonth.of(2026, 1));
            boolean hasExistingShifts = continuationService.hasShiftsForMonth(currentMonth);

            if (!hasExistingShifts && !isJanuary2026) {
                String monthName = getMonthName(currentMonth.getMonthValue());
                int year = currentMonth.getYear();

                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Завантаження графіку");
                alert.setHeaderText("Графік на " + monthName + " " + year + " не знайдено");
                alert.setContentText("Що зробити?\n\n" +
                        "✅ Продовжити — спробувати побудувати з попереднього місяця\n" +
                        "📝 Порожній — показати пустий (X) без запису в БД");

                ButtonType continueButton = new ButtonType("✅ Продовжити");
                ButtonType emptyButton = new ButtonType("📝 Порожній");
                ButtonType cancelButton = new ButtonType("❌ Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);

                alert.getButtonTypes().setAll(continueButton, emptyButton, cancelButton);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isEmpty() || result.get() == cancelButton) {
                    showStatus("Завантаження скасовано");
                    return;
                }

                if (result.get() == continueButton) {
                    YearMonth prev = currentMonth.minusMonths(1);
                    int nonX = countNonXShiftsInMonth(prev);

                    if (nonX < 5) {
                        Alert warn = new Alert(Alert.AlertType.INFORMATION);
                        warn.setTitle("Продовження неможливе");
                        warn.setHeaderText("У " + getMonthName(prev.getMonthValue()) + " " + prev.getYear() +
                                " замало заповнених змін (" + nonX + ")");
                        warn.setContentText("Щоб не робило \"X X X 1 X X X 1...\", система відкриє порожній графік.\n\n" +
                                "Заповни кілька днів у попередньому місяці — тоді продовження буде коректне.");
                        warn.showAndWait();

                        showStatus("📝 Відкрито порожній графік (без продовження)");
                    } else {
                        continuationService.loadAndContinueSchedule(currentMonth);
                        showStatus("✅ Графік продовжено на " + monthName + " " + year);
                    }
                } else {
                    showStatus("📝 Відкрито порожній графік на " + monthName + " " + year);
                }
            }

            pendingShiftsToSave.clear();
            hasUnsavedChanges = false;

            calculateWeekendsAndHolidays();

            Map<Employee, List<Shift>> scheduleData = scheduleService.loadScheduleForMonth(currentMonth);

            if (scheduleData == null || scheduleData.isEmpty()) {
                scheduleData = buildEmptyScheduleMapForUIOnly(currentMonth);
            }

            List<EmployeeScheduleRow> rows = new ArrayList<>();
            for (Map.Entry<Employee, List<Shift>> entry : scheduleData.entrySet()) {
                rows.add(new EmployeeScheduleRow(entry.getKey(), entry.getValue(), currentMonth));
            }

            rows.sort((r1, r2) -> {
                int deptCompare = r1.getEmployee().getDepartment().compareTo(r2.getEmployee().getDepartment());
                if (deptCompare != 0) return deptCompare;
                return r1.getEmployee().getFullName().compareTo(r2.getEmployee().getFullName());
            });

            allScheduleRows.setAll(rows);
            addDayColumns();
            updateDepartmentComboBox();
            updateDayFilterComboBox();

            // Додаємо колонку з підсумками годин
            addTotalHoursColumn();

            showStatus("📊 Дані завантажено для " + getMonthName(currentMonth.getMonthValue()) + " " + currentMonth.getYear());

        } catch (SQLException e) {
            showError("Помилка БД", "Не вдалося завантажити графік: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showError("Помилка", "Неочікувана помилка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int countNonXShiftsInMonth(YearMonth month) throws SQLException {
        int nonX = 0;
        List<Employee> employees = employeeService.getAllEmployees();
        for (Employee employee : employees) {
            if (!employee.isCurrentlyWorking()) continue;

            List<Shift> shifts = shiftDAO.findShiftsForEmployeeAndMonth(employee.getId(), month);
            if (shifts == null) continue;

            for (Shift s : shifts) {
                String c = s.getCode() == null ? "" : s.getCode().trim().toUpperCase();
                if (!c.isEmpty() && !"X".equals(c)) nonX++;
            }
        }
        return nonX;
    }

    private Map<Employee, List<Shift>> buildEmptyScheduleMapForUIOnly(YearMonth month) throws SQLException {
        Map<Employee, List<Shift>> map = new LinkedHashMap<>();
        List<Employee> employees = employeeService.getAllEmployees();
        for (Employee e : employees) {
            if (e.isCurrentlyWorking()) {
                map.put(e, new ArrayList<>());
            }
        }
        return map;
    }

    private void updateDayFilterComboBox() {
        if (dayFilterComboBox == null || currentMonth == null) return;

        ObservableList<String> days = FXCollections.observableArrayList();
        days.add("Всі дні");

        int daysInMonth = currentMonth.lengthOfMonth();
        for (int day = 1; day <= daysInMonth; day++) {
            days.add(String.valueOf(day));
        }

        dayFilterComboBox.setItems(days);
        dayFilterComboBox.setValue("Всі дні");
    }

    private void addDayColumns() {
        if (scheduleTableView.getColumns().size() > 3) {
            List<TableColumn<EmployeeScheduleRow, ?>> columnsToRemove =
                    new ArrayList<>(scheduleTableView.getColumns().subList(3, scheduleTableView.getColumns().size()));
            scheduleTableView.getColumns().removeAll(columnsToRemove);
        }
        dayColumns.clear();

        if (currentMonth == null) {
            System.err.println("Помилка: currentMonth є null");
            return;
        }

        int daysInMonth = currentMonth.lengthOfMonth();
        LocalDate today = LocalDate.now();

        TableColumn<EmployeeScheduleRow, String> monthDaysHeader = new TableColumn<>("ДНІ МІСЯЦЯ");
        monthDaysHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1a5c1f; -fx-alignment: CENTER;");
        scheduleTableView.getColumns().add(monthDaysHeader);

        for (int day = 1; day <= daysInMonth; day++) {
            final int dayNumber = day;
            boolean isWeekend = isWeekend(day);
            boolean isHoliday = isHoliday(day);
            boolean isToday = currentMonth.getYear() == today.getYear() &&
                    currentMonth.getMonth() == today.getMonth() &&
                    day == today.getDayOfMonth();

            TableColumn<EmployeeScheduleRow, String> dayColumn = new TableColumn<>(String.valueOf(day));
            dayColumn.setPrefWidth(55);
            dayColumn.setMinWidth(55);
            dayColumn.setMaxWidth(55);
            dayColumn.setResizable(true);
            dayColumn.setSortable(false);
            dayColumn.setEditable(true);

            String headerStyle = "-fx-alignment: CENTER; -fx-font-weight: bold; -fx-font-size: 12px; ";
            if (isToday) {
                headerStyle += "-fx-background-color: #fff3e0; -fx-text-fill: #ff6f00; -fx-border-color: #ffcc80; " +
                        "-fx-border-width: 2; -fx-font-weight: 900;";
            } else if (isHoliday) {
                headerStyle += "-fx-background-color: #ffebee; -fx-text-fill: #d32f2f; -fx-border-color: #ef9a9a;";
            } else if (isWeekend) {
                headerStyle += "-fx-background-color: #eeeeee; -fx-text-fill: #616161; -fx-border-color: #dddddd;";
            } else {
                headerStyle += "-fx-text-fill: #1a5c1f; -fx-background-color: #f0f9f0; -fx-border-color: #c8e6c9;";
            }
            dayColumn.setStyle(headerStyle);

            if (currentMonth != null) {
                LocalDate date = currentMonth.atDay(day);
                String dayInfo = date.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("uk")) +
                        ", " + day + " " +
                        date.getMonth().getDisplayName(TextStyle.FULL, new Locale("uk")) +
                        " " + date.getYear();

                if (isToday) {
                    dayInfo += "\n📅 СЬОГОДНІ";
                }
                if (isHoliday) {
                    String holidayName = holidayDates.get(date);
                    dayInfo += "\n🎉 " + (holidayName != null ? holidayName : "Свято");
                } else if (isWeekend) {
                    dayInfo += "\n🏖️ Вихідний день";
                }

                Tooltip tooltip = new Tooltip(dayInfo);
                tooltip.setShowDelay(javafx.util.Duration.millis(300));
                Tooltip.install(dayColumn.getGraphic(), tooltip);
            }

            dayColumn.setCellValueFactory(cellData -> {
                String code = cellData.getValue().getShiftCodeForDay(dayNumber);
                return new SimpleStringProperty(code != null ? code : "X");
            });

            dayColumn.setCellFactory(column -> {
                TextFieldTableCell<EmployeeScheduleRow, String> cell =
                        new TextFieldTableCell<>(createStringConverter()) {
                            @Override
                            public void commitEdit(String newValue) {
                                if (!isEditing()) return;
                                super.commitEdit(newValue);

                                int rowIndex = getIndex();
                                if (rowIndex >= 0 && rowIndex < scheduleTableView.getItems().size()) {
                                    EmployeeScheduleRow row = scheduleTableView.getItems().get(rowIndex);
                                    String code = newValue == null ? "" : newValue.trim().toUpperCase();

                                    if (isValidShiftCode(code)) {
                                        row.setShiftForDay(dayNumber, code);

                                        // Додаємо перевірку на null
                                        if (currentMonth != null) {
                                            LocalDate date = currentMonth.atDay(dayNumber);
                                            Shift shift = new Shift(row.getEmployee().getId(), date, code);
                                            pendingShiftsToSave.add(shift);
                                            hasUnsavedChanges = true;
                                            showStatus("Зміну оновлено для " + dayNumber + " числа (не збережено)");
                                        }

                                        // Оновлюємо колонку з підсумками
                                        scheduleTableView.refresh();
                                    }
                                }
                            }
                        };

                cell.setAlignment(Pos.CENTER);
                cell.setEditable(true);

                cell.itemProperty().addListener((obs, oldItem, newItem) -> {
                    if (newItem == null) {
                        cell.setText(null);
                        cell.setStyle("");
                        return;
                    }

                    String shiftCode = newItem.trim().toUpperCase();
                    cell.setText(shiftCode);

                    // Додаємо тултіп з описом коду
                    String description = getCodeDescription(shiftCode);
                    Tooltip tooltip = new Tooltip(description);
                    tooltip.setShowDelay(javafx.util.Duration.millis(500));
                    Tooltip.install(cell, tooltip);

                    String style = "-fx-alignment: CENTER; -fx-font-weight: bold; -fx-font-size: 13px; ";

                    switch (shiftCode) {
                        case "1":
                            style += "-fx-background-color: #e8f5e9; -fx-text-fill: #1b5e20; -fx-border-color: #c8e6c9;";
                            break;
                        case "2":
                        case "12":
                            style += "-fx-background-color: #e3f2fd; -fx-text-fill: #0d47a1; -fx-border-color: #bbdefb;";
                            break;
                        case "X":
                            style += "-fx-background-color: #f5f5f5; -fx-text-fill: #616161; -fx-border-color: #e0e0e0;";
                            break;
                        case "0":
                            style += "-fx-background-color: #ffecb3; -fx-text-fill: #ff6f00; -fx-border-color: #ffe082;";
                            break;
                        case "8":
                            style += "-fx-background-color: #d1c4e9; -fx-text-fill: #4527a0; -fx-border-color: #b39ddb;";
                            break;
                        case "Л":
                            style += "-fx-background-color: #f3e5f5; -fx-text-fill: #4a148c; -fx-border-color: #e1bee7;";
                            break;
                        case "В":
                            style += "-fx-background-color: #fce4ec; -fx-text-fill: #880e4f; -fx-border-color: #f8bbd9;";
                            break;
                        case "К":
                            style += "-fx-background-color: #e0f7fa; -fx-text-fill: #006064; -fx-border-color: #b2ebf2;";
                            break;
                        case "ТН":
                            style += "-fx-background-color: #ffccbc; -fx-text-fill: #bf360c; -fx-border-color: #ffab91;";
                            break;
                        case "11":
                            style += "-fx-background-color: #c8e6c9; -fx-text-fill: #2e7d32; -fx-border-color: #a5d6a7;";
                            break;
                        case "7.00":
                            style += "-fx-background-color: #f0f4c3; -fx-text-fill: #827717; -fx-border-color: #e6ee9c;";
                            break;
                        case "8.00":
                        case "8.25":
                            style += "-fx-background-color: #ffcc80; -fx-text-fill: #e65100; -fx-border-color: #ffb74d;";
                            break;
                    }

                    cell.setStyle(style);
                });

                cell.setContextMenu(createContextMenu(cell, dayNumber));
                return cell;
            });

            dayColumn.setOnEditCommit(event -> {
                EmployeeScheduleRow row = event.getRowValue();
                String newCode = event.getNewValue() == null ? "" : event.getNewValue().trim().toUpperCase();

                if (isValidShiftCode(newCode)) {
                    row.setShiftForDay(dayNumber, newCode);
                    scheduleTableView.refresh();

                    // Додаємо перевірку на null
                    if (currentMonth != null) {
                        LocalDate date = currentMonth.atDay(dayNumber);
                        Shift shift = new Shift(row.getEmployee().getId(), date, newCode);
                        pendingShiftsToSave.add(shift);
                        hasUnsavedChanges = true;
                        showStatus("Зміну оновлено для " + dayNumber + " числа (не збережено)");
                    }

                    // Оновлюємо колонку з підсумками
                    scheduleTableView.refresh();
                } else {
                    showError("Помилка", "Недійсний код зміни.");
                    event.consume();
                }
            });

            monthDaysHeader.getColumns().add(dayColumn);
            dayColumns.put(day, dayColumn);
        }
    }

    private String getCodeDescription(String code) {
        switch (code) {
            case "1": return "Денна зміна (8 годин)";
            case "2": return "Подвійна зміна (16 годин)";
            case "12": return "Подвійна зміна (16 годин)";
            case "X": return "Вихідний день";
            case "0": return "Відгул (відпрацьований вихідний)";
            case "8": return "Перенесення робочого дня";
            case "Л": return "Лікарняний лист";
            case "В": return "Щорічна відпустка";
            case "К": return "Відрядження";
            case "ТН": return "Тимчасово непрацездатний";
            case "11": return "Зміна 11 годин";
            case "7.00": return "Скорочений день (7 годин)";
            case "8.00": return "Стандартний день (8 годин)";
            case "8.25": return "Робочий день (8 годин 15 хвилин)";
            default: return "Невідомий код: " + code;
        }
    }

    private StringConverter<String> createStringConverter() {
        return new StringConverter<String>() {
            @Override
            public String toString(String object) {
                return object != null ? object : "";
            }

            @Override
            public String fromString(String string) {
                return string != null ? string.trim().toUpperCase() : "";
            }
        };
    }

    private ContextMenu createContextMenu(TextFieldTableCell<EmployeeScheduleRow, String> cell, int dayNumber) {
        ContextMenu contextMenu = new ContextMenu();

        Map<String, String> shiftOptions = new LinkedHashMap<>();
        shiftOptions.put("1", "1 - Денна зміна");
        shiftOptions.put("2", "2 - Подвійна зміна");
        shiftOptions.put("12", "12 - Подвійна зміна");
        shiftOptions.put("X", "X - Вихідний");
        shiftOptions.put("0", "0 - Відгул");
        shiftOptions.put("8", "8 - Перенесення");
        shiftOptions.put("Л", "Л - Лікарняний");
        shiftOptions.put("В", "В - Відпустка");
        shiftOptions.put("К", "К - Відрядження");
        shiftOptions.put("ТН", "ТН - Тимчасово непрацездатний");
        shiftOptions.put("11", "11 - Зміна 11 год");
        shiftOptions.put("7.00", "7.00 - 7 годин");
        shiftOptions.put("8.00", "8.00 - 8 годин");
        shiftOptions.put("8.25", "8.25 - 8.25 годин");

        for (Map.Entry<String, String> entry : shiftOptions.entrySet()) {
            MenuItem item = new MenuItem(entry.getValue());
            item.setOnAction(e -> {
                if (cell.getTableRow() != null && cell.getTableRow().getItem() != null) {
                    EmployeeScheduleRow row = cell.getTableRow().getItem();
                    row.setShiftForDay(dayNumber, entry.getKey());
                    scheduleTableView.refresh();

                    // Додаємо перевірку на null
                    if (currentMonth != null) {
                        LocalDate date = currentMonth.atDay(dayNumber);
                        Shift shift = new Shift(row.getEmployee().getId(), date, entry.getKey());
                        pendingShiftsToSave.add(shift);
                        hasUnsavedChanges = true;
                        showStatus("Встановлено " + entry.getValue() + " для " + dayNumber + " числа (не збережено)");
                    }

                    // Оновлюємо колонку з підсумками
                    scheduleTableView.refresh();
                }
            });
            contextMenu.getItems().add(item);
        }

        MenuItem clearItem = new MenuItem("Очистити");
        clearItem.setOnAction(e -> {
            if (cell.getTableRow() != null && cell.getTableRow().getItem() != null) {
                EmployeeScheduleRow row = cell.getTableRow().getItem();
                row.setShiftForDay(dayNumber, "");
                scheduleTableView.refresh();

                // Додаємо перевірку на null
                if (currentMonth != null) {
                    LocalDate date = currentMonth.atDay(dayNumber);
                    Shift shift = new Shift(row.getEmployee().getId(), date, "");
                    pendingShiftsToSave.add(shift);
                    hasUnsavedChanges = true;
                    showStatus("Очищено для " + dayNumber + " числа (не збережено)");
                }

                // Оновлюємо колонку з підсумками
                scheduleTableView.refresh();
            }
        });

        contextMenu.getItems().add(new SeparatorMenuItem());
        contextMenu.getItems().add(clearItem);
        return contextMenu;
    }

    private void addTotalHoursColumn() {
        // Перевіряємо, чи колонка вже існує
        for (TableColumn<EmployeeScheduleRow, ?> column : scheduleTableView.getColumns()) {
            if (column.getText().equals("Години")) {
                scheduleTableView.getColumns().remove(column);
                break;
            }
        }

        // Додаємо колонку для підсумків годин
        TableColumn<EmployeeScheduleRow, String> totalColumn = new TableColumn<>("Години");
        totalColumn.setPrefWidth(80);
        totalColumn.setStyle("-fx-font-weight: bold; -fx-alignment: CENTER; -fx-background-color: #e8f5e9;");

        totalColumn.setCellValueFactory(cellData -> {
            EmployeeScheduleRow row = cellData.getValue();
            double totalHours = calculateTotalHours(row);
            return new SimpleStringProperty(String.format("%.0f", totalHours));
        });

        totalColumn.setCellFactory(column -> new TableCell<EmployeeScheduleRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-alignment: CENTER; " +
                            "-fx-background-color: #c8e6c9; -fx-border-color: #a5d6a7;");

                    double hours = item.isEmpty() ? 0 : Double.parseDouble(item);
                    if (hours > 160) { // Більше ніж 160 годин за місяць
                        setStyle("-fx-font-weight: bold; -fx-alignment: CENTER; " +
                                "-fx-background-color: #ffebee; -fx-text-fill: #d32f2f;");
                    }

                    EmployeeScheduleRow row = getTableView().getItems().get(getIndex());
                    String details = getHoursCalculationDetails(row);
                    Tooltip tooltip = new Tooltip(details);
                    Tooltip.install(this, tooltip);
                }
            }
        });

        scheduleTableView.getColumns().add(totalColumn);
    }

    private double calculateTotalHours(EmployeeScheduleRow row) {
        if (currentMonth == null || row == null || row.getEmployee() == null) return 0.0;

        String department = row.getEmployee().getDepartment();
        if (department == null) return 0.0;

        int count1 = 0;
        int count2 = 0;

        for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
            String code = row.getShiftCodeForDay(day);
            if (code != null) {
                switch (code) {
                    case "1":
                        count1++;
                        break;
                    case "2":
                    case "12":
                        count2++;
                        break;
                }
            }
        }

        if (department.contains("Великорогізнянськ") || department.contains("Пром район")) {
            return count1 * 24.0;
        } else if (department.contains("ГКНС")) {

            return (count1 + count2) * 12.0;
        } else {

            double total = 0.0;
            for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
                String code = row.getShiftCodeForDay(day);
                if (code != null) {
                    total += getHoursForCode(code);
                }
            }
            return total;
        }
    }

    private String getHoursCalculationDetails(EmployeeScheduleRow row) {
        if (currentMonth == null || row == null || row.getEmployee() == null) return "";

        String department = row.getEmployee().getDepartment();
        if (department == null) return "";

        int count1 = 0;
        int count2 = 0;

        for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
            String code = row.getShiftCodeForDay(day);
            if (code != null) {
                switch (code) {
                    case "1":
                        count1++;
                        break;
                    case "2":
                    case "12":
                        count2++;
                        break;
                }
            }
        }

        if (department.contains("Великорогізнянськ") || department.contains("Пром район")) {
            return String.format("Підрозділ: %s\n" +
                            "Кількість денних змін (1): %d\n" +
                            "Розрахунок: %d × 24 = %.0f годин",
                    department, count1, count1, count1 * 24.0);
        } else if (department.contains("ГКНС")) {
            return String.format("Підрозділ: %s\n" +
                            "Денних змін (1): %d\n" +
                            "Подвійних змін (2/12): %d\n" +
                            "Розрахунок: (%d + %d) × 12 = %.0f годин",
                    department, count1, count2, count1, count2, (count1 + count2) * 12.0);
        } else {
            double total = calculateTotalHours(row);
            return String.format("Підрозділ: %s\n" +
                            "Денних змін (1): %d\n" +
                            "Подвійних змін (2/12): %d\n" +
                            "Загальна сума: %.0f годин",
                    department, count1, count2, total);
        }
    }

    private double getHoursForCode(String code) {
        switch (code) {
            case "1": return 8.0;
            case "2": case "12": return 16.0;
            case "11": return 11.0;
            case "7.00": return 7.0;
            case "8.00": return 8.0;
            case "8.25": return 8.25;
            default: return 0.0;
        }
    }

    private int getMonthIndex(String monthName) {
        List<String> months = List.of(
                "Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
                "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"
        );
        return months.indexOf(monthName);
    }

    @FXML
    private void saveSchedule() {
        try {
            if (pendingShiftsToSave.isEmpty() && !hasUnsavedChanges) {
                showStatus("ℹ️ Немає змін для збереження");
                return;
            }

            if (currentMonth == null) {
                showError("Помилка", "Немає активного місяця для збереження");
                return;
            }

            scheduleService.saveShifts(pendingShiftsToSave);

            pendingShiftsToSave.clear();
            hasUnsavedChanges = false;

            showStatus("✅ Всі зміни успішно збережено для " +
                    getMonthName(currentMonth.getMonthValue()) + " " + currentMonth.getYear());

        } catch (SQLException e) {
            showError("Помилка збереження", "Не вдалося зберегти зміни: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void exportToExcel() {
        try {
            if (currentMonth == null) {
                showError("Помилка", "Спочатку завантажте графік.");
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Зберегти Excel файл");
            fileChooser.setInitialFileName("Графік_змін_" +
                    monthComboBox.getValue() + "_" + yearComboBox.getValue() + ".xlsx");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Excel файли", "*.xlsx")
            );

            File file = fileChooser.showSaveDialog(scheduleTableView.getScene().getWindow());
            if (file != null) {
                ExcelExporter.exportSchedule(scheduleService, currentMonth, file);
                showStatus("Експорт завершено: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            showError("Помилка експорту", "Не вдалося експортувати в Excel: " + e.getMessage());
        }
    }

    @FXML
    private void showWeekendsAndHolidaysInfo() {
        if (currentMonth == null) {
            showError("Помилка", "Спочатку завантажити графік.");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Вихідні дні та легенда");
        dialog.setHeaderText("📅 " + currentMonth.getMonth().getDisplayName(TextStyle.FULL, new Locale("uk"))
                + " " + currentMonth.getYear());

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");

        // СЕКЦІЯ З ЛЕГЕНДОЮ
        VBox legendSection = new VBox(10);
        legendSection.setStyle("-fx-background-color: #f0f9ff; -fx-padding: 15; -fx-border-radius: 8; -fx-border-color: #bbdefb;");

        Label legendTitle = new Label("📋 ЛЕГЕНДА КОДІВ ЗМІН");
        legendTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1565c0; -fx-padding: 0 0 10 0;");

        GridPane legendGrid = new GridPane();
        legendGrid.setHgap(20);
        legendGrid.setVgap(8);
        legendGrid.setPadding(new Insets(10, 0, 15, 0));

        Map<String, String> shiftLegend = new LinkedHashMap<>();
        shiftLegend.put("1", "Денна зміна");
        shiftLegend.put("2 / 12", "Подвійна зміна");
        shiftLegend.put("11", "Зміна 11 годин");
        shiftLegend.put("7.00", "7 годин");
        shiftLegend.put("8.00 / 8.25", "8 / 8.25 годин");
        shiftLegend.put("X", "Вихідний день");
        shiftLegend.put("0", "Відгул");
        shiftLegend.put("8", "Перенесення");
        shiftLegend.put("Л", "Лікарняний");
        shiftLegend.put("В", "Відпустка");
        shiftLegend.put("К", "Відрядження");
        shiftLegend.put("ТН", "Тимчасово непрацездатний");

        int row = 0;
        for (Map.Entry<String, String> entry : shiftLegend.entrySet()) {
            Label codeLabel = new Label(entry.getKey());
            codeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #0d47a1; -fx-font-size: 13px;");

            Label descLabel = new Label(entry.getValue());
            descLabel.setStyle("-fx-text-fill: #333; -fx-font-size: 13px;");

            legendGrid.add(codeLabel, 0, row);
            legendGrid.add(descLabel, 1, row);
            row++;
        }

        legendSection.getChildren().addAll(legendTitle, legendGrid);
        content.getChildren().add(legendSection);

        VBox holidaysSection = new VBox(10);
        holidaysSection.setStyle("-fx-background-color: #fff3e0; -fx-padding: 15; -fx-border-radius: 8; -fx-border-color: #ffcc80;");

        Label holidaysTitle = new Label("🎉 СВЯТА ТА ВИХІДНІ ДНІ");
        holidaysTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e65100; -fx-padding: 0 0 10 0;");

        GridPane holidaysGrid = new GridPane();
        holidaysGrid.setHgap(15);
        holidaysGrid.setVgap(8);
        holidaysGrid.setPadding(new Insets(10, 0, 15, 0));

        // Додаємо вихідні дні (суботи та неділі) та свята
        int holidayRow = 0;
        for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
            LocalDate date = currentMonth.atDay(day);
            boolean isWeekend = isWeekend(day);
            boolean isHoliday = isHoliday(day);
            boolean isToday = currentMonth.getYear() == LocalDate.now().getYear() &&
                    currentMonth.getMonth() == LocalDate.now().getMonth() &&
                    day == LocalDate.now().getDayOfMonth();

            if (isWeekend || isHoliday || isToday) {
                Label dateLabel = new Label(String.format("%02d %s", day,
                        date.getMonth().getDisplayName(TextStyle.SHORT, new Locale("uk"))));

                Label typeLabel = new Label();

                if (isToday) {
                    dateLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #ff6f00;");
                    typeLabel.setText("СЬОГОДНІ");
                    typeLabel.setStyle("-fx-text-fill: #ff6f00; -fx-font-weight: bold;");
                } else if (isHoliday) {
                    dateLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #d32f2f;");
                    String holidayName = holidayDates.get(date);
                    typeLabel.setText(holidayName != null ? holidayName : "Свято");
                    typeLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
                } else {
                    dateLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #616161;");
                    typeLabel.setText(date.getDayOfWeek() == DayOfWeek.SATURDAY ? "Субота" : "Неділя");
                    typeLabel.setStyle("-fx-text-fill: #616161;");
                }

                holidaysGrid.add(dateLabel, 0, holidayRow);
                holidaysGrid.add(typeLabel, 1, holidayRow);
                holidayRow++;
            }
        }

        if (holidayRow > 0) {
            holidaysSection.getChildren().addAll(holidaysTitle, holidaysGrid);
            content.getChildren().add(holidaysSection);
        } else {
            Label noHolidaysLabel = new Label("У цьому місяці немає вихідних днів або свят");
            noHolidaysLabel.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
            holidaysSection.getChildren().addAll(holidaysTitle, noHolidaysLabel);
            content.getChildren().add(holidaysSection);
        }

        VBox editInfoSection = new VBox(10);
        editInfoSection.setStyle("-fx-background-color: #e8f5e9; -fx-padding: 15; -fx-border-radius: 8; -fx-border-color: #c8e6c9;");

        Label editInfoTitle = new Label("💡 ІНФОРМАЦІЯ ПРО РЕДАГУВАННЯ");
        editInfoTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2e7d32; -fx-padding: 0 0 10 0;");

        TextArea editInfoText = new TextArea(
                "Для редагування змін:\n" +
                        "1. Клацніть двічі по клітинці з кодом зміни\n" +
                        "2. Введіть код з легенди або виберіть з контекстного меню\n" +
                        "3. Натисніть Enter для збереження\n\n" +
                        "Дозволені коди:\n" +
                        "- Будь-які коди з легенди вище\n" +
                        "- Пусте значення або X для вихідного\n" +
                        "Система автоматично перетворить коди на великі літери\n" +
                        "Зміни зберігаються лише після натискання кнопки '💾 Зберегти'"
        );
        editInfoText.setEditable(false);
        editInfoText.setWrapText(true);
        editInfoText.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-font-size: 12px;");
        editInfoText.setPrefHeight(150);

        editInfoSection.getChildren().addAll(editInfoTitle, editInfoText);
        content.getChildren().add(editInfoSection);

        scrollPane.setContent(content);

        ButtonType closeButton = new ButtonType("Закрити", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);
        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setMinWidth(850);
        dialog.getDialogPane().setMinHeight(650);
        dialog.showAndWait();
    }

    @FXML
    private void filterTable(KeyEvent event) {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();

        filteredScheduleRows.setPredicate(row -> {
            if (searchText.isEmpty()) return true;

            String fullName = row.getEmployee().getFullName() != null ?
                    row.getEmployee().getFullName().toLowerCase() : "";
            String department = row.getEmployee().getDepartment() != null ?
                    row.getEmployee().getDepartment().toLowerCase() : "";
            String position = row.getEmployee().getPosition() != null ?
                    row.getEmployee().getPosition().toLowerCase() : "";

            return fullName.contains(searchText)
                    || department.contains(searchText)
                    || position.contains(searchText);
        });
        scheduleTableView.refresh();
    }

    private void filterByDepartment() {
        if (departmentComboBox.getValue() == null) return;

        String selectedDept = departmentComboBox.getValue();
        if (selectedDept.equals("Всі підрозділи")) {
            filteredScheduleRows.setPredicate(row -> true);
        } else {
            filteredScheduleRows.setPredicate(row ->
                    row != null &&
                            row.getEmployee() != null &&
                            selectedDept.equals(row.getEmployee().getDepartment()));
        }
        scheduleTableView.refresh();
    }

    private void filterByDayAndStatus() {
        if (dayStatusComboBox == null || dayFilterComboBox == null) return;

        String selectedStatus = dayStatusComboBox.getValue();
        String selectedDay = dayFilterComboBox.getValue();

        if ((selectedStatus == null || selectedStatus.equals("Всі")) &&
                (selectedDay == null || selectedDay.equals("Всі дні"))) {
            filteredScheduleRows.setPredicate(row -> true);
            scheduleTableView.refresh();
            return;
        }

        filteredScheduleRows.setPredicate(row -> {
            boolean matchesStatus = true;
            boolean matchesDay = true;

            if (selectedStatus != null && !selectedStatus.equals("Всі")) {
                boolean hasMatchingStatus = false;

                if (currentMonth != null) {
                    for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
                        String shiftCode = row.getShiftCodeForDay(day);
                        if (shiftCode != null) {
                            boolean statusMatch = false;

                            switch (selectedStatus) {
                                case "Працює":
                                    statusMatch = "1".equals(shiftCode) || "2".equals(shiftCode) || "12".equals(shiftCode) ||
                                            "11".equals(shiftCode) || "7.00".equals(shiftCode) ||
                                            "8.00".equals(shiftCode) || "8.25".equals(shiftCode);
                                    break;
                                case "Вихідний":
                                    statusMatch = "X".equals(shiftCode);
                                    break;
                                case "Лікарняний":
                                    statusMatch = "Л".equals(shiftCode);
                                    break;
                                case "Відпустка":
                                    statusMatch = "В".equals(shiftCode);
                                    break;
                                case "Відрядження":
                                    statusMatch = "К".equals(shiftCode);
                                    break;
                                case "Відгул":
                                    statusMatch = "0".equals(shiftCode);
                                    break;
                                case "Перенесення":
                                    statusMatch = "8".equals(shiftCode);
                                    break;
                                case "Тимчасово непрацездатний":
                                    statusMatch = "ТН".equals(shiftCode);
                                    break;
                                default:
                                    statusMatch = true;
                            }

                            if (statusMatch) {
                                hasMatchingStatus = true;
                                break;
                            }
                        }
                    }
                }
                matchesStatus = hasMatchingStatus;
            }

            if (selectedDay != null && !selectedDay.equals("Всі дні") && !selectedDay.isEmpty()) {
                try {
                    int dayNumber = Integer.parseInt(selectedDay);
                    if (currentMonth != null && dayNumber >= 1 && dayNumber <= currentMonth.lengthOfMonth()) {
                        String shiftCode = row.getShiftCodeForDay(dayNumber);
                        matchesDay = shiftCode != null && !shiftCode.isEmpty();
                    }
                } catch (NumberFormatException e) {
                    matchesDay = true;
                }
            }

            return matchesStatus && matchesDay;
        });

        scheduleTableView.refresh();
    }

    @FXML
    private void resetFilters() {
        searchField.clear();
        departmentComboBox.getSelectionModel().selectFirst();
        if (dayStatusComboBox != null) {
            dayStatusComboBox.getSelectionModel().selectFirst();
        }
        if (dayFilterComboBox != null) {
            dayFilterComboBox.setValue("Всі дні");
        }

        filteredScheduleRows.setPredicate(row -> true);
        scheduleTableView.refresh();

        showStatus("Фільтри скинуто");
    }

    private void updateDepartmentComboBox() {
        try {
            List<String> departments = employeeService.getAllDepartments();
            ObservableList<String> deptList = FXCollections.observableArrayList(departments);
            deptList.add(0, "Всі підрозділи");
            departmentComboBox.setItems(deptList);
            departmentComboBox.getSelectionModel().selectFirst();
        } catch (SQLException e) {
            departmentComboBox.setItems(FXCollections.observableArrayList("Всі підрозділи"));
            departmentComboBox.getSelectionModel().selectFirst();
        }
    }

    private String getMonthName(int month) {
        String[] months = {
                "Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
                "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"
        };
        return months[month - 1];
    }

    private void loadCurrentMonth() {
        String monthStr = monthComboBox.getValue();
        String yearStr = yearComboBox.getValue();

        if (yearStr != null && monthStr != null) {
            int monthIndex = getMonthIndex(monthStr);
            int year = Integer.parseInt(yearStr);
            currentMonth = YearMonth.of(year, monthIndex + 1);
            loadSchedule();
        } else {
            System.err.println("Помилка: не вибрано місяць або рік");
            showError("Помилка", "Будь ласка, виберіть місяць та рік");
        }
    }

    private void calculateWeekendsAndHolidays() {
        weekendDates.clear();
        holidayDates.clear();

        if (currentMonth == null) return;

        int year = currentMonth.getYear();
        int daysInMonth = currentMonth.lengthOfMonth();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                weekendDates.add(date);
            }
        }

        for (Map.Entry<LocalDate, String> entry : holidays.entrySet()) {
            LocalDate holidayTemplate = entry.getKey();
            LocalDate holidayThisYear = holidayTemplate.withYear(year);
            if (holidayThisYear.getMonth() == currentMonth.getMonth()) {
                holidayDates.put(holidayThisYear, entry.getValue());
            }
        }
    }

    private boolean isWeekend(int day) {
        if (currentMonth == null) return false;
        LocalDate date = currentMonth.atDay(day);
        return weekendDates.contains(date);
    }

    private boolean isHoliday(int day) {
        if (currentMonth == null) return false;
        LocalDate date = currentMonth.atDay(day);
        return holidayDates.containsKey(date);
    }

    private boolean isValidShiftCode(String code) {
        if (code == null || code.isEmpty()) return true;
        return code.matches("[12XЛВКТН]") ||
                code.equals("12") ||
                code.equals("0") ||
                code.equals("8") ||
                code.equals("11") ||
                code.equals("7.00") ||
                code.equals("8.00") ||
                code.equals("8.25");
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText("Статус: " + message);
        }
    }


    public static class EmployeeFullInfo {
        private final Integer id;
        private final String fullName;
        private final String position;
        private final String department;
        private final String education;
        private final String phone;
        private final String birthDate;
        private final String hireDate;
        private final String residence;
        private final String profkom;
        private final String children;
        private final String otherData;

        public EmployeeFullInfo(Integer id, String fullName, String position, String department,
                                String education, String phone, String birthDate,
                                String hireDate, String profkom, String children, String data) {
            this.id = id;
            this.fullName = fullName;
            this.position = position;
            this.department = department;
            this.education = education;
            this.phone = phone;
            this.birthDate = birthDate;
            this.hireDate = hireDate;
            this.profkom = profkom;
            this.children = children;

            String residence = "";
            String otherData = "";
            if (data != null) {
                String[] parts = data.split(";");
                for (String part : parts) {
                    if (part.trim().startsWith("Проживання:")) {
                        residence = part.replace("Проживання:", "").trim();
                    } else {
                        otherData += part.trim() + " ";
                    }
                }
            }
            this.residence = residence.trim();
            this.otherData = otherData.trim();
        }

        public Integer getId() { return id; }
        public String getFullName() { return fullName; }
        public String getPosition() { return position; }
        public String getDepartment() { return department; }
        public String getEducation() { return education; }
        public String getPhone() { return phone; }
        public String getBirthDate() { return birthDate; }
        public String getHireDate() { return hireDate; }
        public String getResidence() { return residence; }
        public String getProfkom() { return profkom; }
        public String getChildren() { return children; }
        public String getOtherData() { return otherData; }
    }

    public static class EmployeeScheduleRow {
        private final Employee employee;
        private final Map<Integer, String> shiftCodes;

        public EmployeeScheduleRow(Employee employee, List<Shift> shifts, YearMonth month) {
            this.employee = employee;
            this.shiftCodes = new HashMap<>();

            for (Shift shift : shifts) {
                int day = shift.getDate().getDayOfMonth();
                shiftCodes.put(day, shift.getCode());
            }

            for (int day = 1; day <= month.lengthOfMonth(); day++) {
                shiftCodes.putIfAbsent(day, "X");
            }
        }

        public Employee getEmployee() {
            return employee;
        }

        public String getShiftCodeForDay(int day) {
            return shiftCodes.get(day);
        }

        public void setShiftForDay(int day, String code) {
            shiftCodes.put(day, code);
        }
    }
}