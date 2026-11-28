import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

// MAIN GUI APPLICATION CLASS :3
public class EMPManagementGUIall extends JFrame {
    // Central Data Structures (ArrayList & HashMap)
    private final ArrayList<Employee> employeeList = new ArrayList<>();
    private final ArrayList<PayrollRecord> payrollRecords = new ArrayList<>();

    // UI Tables & Models :3
    private DefaultTableModel empTableModel;
    private JTable empTable;
    private JPanel payrollReceiptList;
    private JComboBox<PayrollRecord> comboDeleteReceipt;
    private TableRowSorter<DefaultTableModel> employeeSorter;
    private JTextField txtSearchEmployee;
    private JTextField txtSearchReceipt;

    // Form Inputs GUI components :3
    private JTextField txtId, txtName, txtSalary, txtOvertime, txtDaysPresent;
    private JTextField txtPosition; //new field to display the position
    private JComboBox<String> comboJobType;
    private JComboBox<String> comboEmployeeSelect;
    private JComboBox<String> comboJobDeductionPercent;
    private JComboBox<String> comboSalaryReleaseMonth;
    private JComboBox<Integer> comboSalaryReleaseDay;
    private JSpinner spinnerAttendanceStart;
    private JSpinner spinnerAttendanceEnd;
    private String nextGeneratedEmployeeId;
    private JCheckBox chkLetterSubmitted;
    private static final File PAYROLL_FILE = locateDataFile("Employee Generator Payslip.txt");
    private static final File EMPLOYEE_FILE = locateDataFile("EmployeeFILES.txt");

    private static File locateDataFile(String fileName) {
        Path directory = new File(System.getProperty("user.dir")).toPath().toAbsolutePath();
        while (directory != null) {
            Path candidate = directory.resolve(fileName);
            if (Files.isRegularFile(candidate)) {
                return candidate.toFile();
            }
            directory = directory.getParent();
        }
        return new File(fileName).getAbsoluteFile();
    }

    public EMPManagementGUIall() {
        setTitle("Employee Management System");
        setSize(900, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15));

        // Create Main Tabbed Pane (Matches Architecture Sections in Documentation)
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Employee Management", createEmployeePanel());
        tabbedPane.add("Payroll Processing", createPayrollPanel());
        tabbedPane.add("Payroll Summary Records", createSummaryPanel());

        add(tabbedPane);
        loadEmployees();
        loadPayrollData();
        refreshPayrollReceiptList();
        refreshEmployeeDropdown();
        refreshEmployeeTable();
        updateEmployeeIdPreview();
    }

    private JPanel createEmployeePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel inputPanel = new JPanel(new GridLayout(8, 1, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Add / Update / Remove Employee"));

        inputPanel.add(new JLabel("Employee ID (Auto-generated):"));
        txtId = new JTextField();
        txtId.setEditable(false);
        inputPanel.add(txtId);

        inputPanel.add(new JLabel("Full Name:"));
        txtName = new JTextField();
        inputPanel.add(txtName);

        inputPanel.add(new JLabel("Job Position:"));
        comboJobType = new JComboBox<>(new String[] { "Manager", "Developer", "Staff" });
        inputPanel.add(comboJobType);

        inputPanel.add(new JLabel("Basic Salary (P):"));
        txtSalary = new JTextField();
        inputPanel.add(txtSalary);

        JButton btnAdd = new JButton("Add Employee");
        JButton btnUpdate = new JButton("Update Employee");
        JButton btnRemove = new JButton("Delete Employee");
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnRemove);

        JPanel westContainer = new JPanel(new BorderLayout());
        westContainer.add(inputPanel, BorderLayout.CENTER);
        westContainer.add(btnPanel, BorderLayout.SOUTH);

        empTableModel = new DefaultTableModel(new String[] { "ID", "Name", "Job Position", "Basic Salary (P)" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table cells non-editable
            }
        };
        empTable = new JTable(empTableModel);

        // SEARCH FEATURE: Filters the employee table by ID, name, or job position while the user types.
        employeeSorter = new TableRowSorter<>(empTableModel);
        empTable.setRowSorter(employeeSorter);
        empTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedEmployeeIntoForm();
            }
        });
        txtSearchEmployee = new JTextField(18);
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search Employee:"));
        searchPanel.add(txtSearchEmployee);
        txtSearchEmployee.getDocument().addDocumentListener(new DocumentListener() {
            private void filterEmployeeTable() {
                String searchText = txtSearchEmployee.getText().trim();
                if (searchText.isEmpty()) {
                    employeeSorter.setRowFilter(null);
                } else {
                    employeeSorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(searchText), 0, 1, 2));
                }
            }
            @Override public void insertUpdate(DocumentEvent e) { filterEmployeeTable(); }
            @Override public void removeUpdate(DocumentEvent e) { filterEmployeeTable(); }
            @Override public void changedUpdate(DocumentEvent e) { filterEmployeeTable(); }
        });

        JScrollPane scrollPane = new JScrollPane(empTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Employee Master List"));

        panel.add(westContainer, BorderLayout.WEST);
        JPanel tableContainer = new JPanel(new BorderLayout(5, 5));
        tableContainer.add(searchPanel, BorderLayout.NORTH);
        tableContainer.add(scrollPane, BorderLayout.CENTER);
        panel.add(tableContainer, BorderLayout.CENTER);

        btnAdd.addActionListener(e -> addEmployee());
        btnUpdate.addActionListener(e -> updateEmployee());
        btnRemove.addActionListener(e -> removeEmployee());

        return panel;

    }

    private JPanel createPayrollPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Compute Attendance, Letters & Payroll Formula"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Select Employee:"), gbc);
        comboEmployeeSelect = new JComboBox<>();
        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(comboEmployeeSelect, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Position:"), gbc);
        txtPosition = new JTextField();
        txtPosition.setEditable(false);
        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(txtPosition, gbc);

        comboEmployeeSelect.addActionListener(e -> updateSelectedEmployeePosition());

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Job Deduction Rate:"), gbc);
        String[] deductionChoices = { "1%", "2%", "3%", "4%", "5%", "6%", "7%", "8%", "9%", "10%" };
        comboJobDeductionPercent = new JComboBox<>(deductionChoices);
        comboJobDeductionPercent.setSelectedItem("5%");
        gbc.gridx = 1;
        panel.add(comboJobDeductionPercent, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Overtime Hours Worked:"), gbc);
        txtOvertime = new JTextField("0");
        gbc.gridx = 1;
        gbc.gridy = 3;
        panel.add(txtOvertime, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("Attendance Start Date:"), gbc);
        java.time.YearMonth currentMonth = java.time.YearMonth.now();
        spinnerAttendanceStart = createDateSpinner(currentMonth.atDay(1));
        gbc.gridx = 1;
        panel.add(spinnerAttendanceStart, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(new JLabel("Attendance End Date:"), gbc);
        spinnerAttendanceEnd = createDateSpinner(currentMonth.atEndOfMonth());
        gbc.gridx = 1;
        panel.add(spinnerAttendanceEnd, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        panel.add(new JLabel("Days Present:"), gbc);
        txtDaysPresent = new JTextField();
        txtDaysPresent.setText(Integer.toString(countAttendanceWorkdays(
            currentMonth.atDay(1), currentMonth.atEndOfMonth())));
        spinnerAttendanceStart.addChangeListener(e -> updateAttendanceDaysLimit());
        spinnerAttendanceEnd.addChangeListener(e -> updateAttendanceDaysLimit());
        gbc.gridx = 1;
        panel.add(txtDaysPresent, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        panel.add(new JLabel("Excuse Letter Status:"), gbc);
        chkLetterSubmitted = new JCheckBox("Excuse Letter Submitted (Waives 2% penalty)");
        chkLetterSubmitted.setSelected(true);
        gbc.gridx = 1;
        gbc.gridy = 7;
        panel.add(chkLetterSubmitted, gbc);

        gbc.gridx = 0;
        gbc.gridy = 8;
        panel.add(new JLabel("Salary Release Month:"), gbc);
        String[] releaseMonths = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        };
        comboSalaryReleaseMonth = new JComboBox<>(releaseMonths);
        comboSalaryReleaseMonth.setSelectedIndex(java.time.LocalDate.now().getMonthValue() - 1);
        gbc.gridx = 1;
        gbc.gridy = 8;
        panel.add(comboSalaryReleaseMonth, gbc);

        gbc.gridx = 0;
        gbc.gridy = 9;
        panel.add(new JLabel("Salary Release Day:"), gbc);
        comboSalaryReleaseDay = new JComboBox<>();
        refreshSalaryReleaseDays();
        comboSalaryReleaseMonth.addActionListener(e -> refreshSalaryReleaseDays());
        gbc.gridx = 1;
        gbc.gridy = 9;
        panel.add(comboSalaryReleaseDay, gbc);

        JButton btnCompute = new JButton("Compute & Generate Payslip");
        gbc.gridx = 1;
        gbc.gridy = 10;
        panel.add(btnCompute, gbc);

        btnCompute.addActionListener(e -> computePayroll());
        return panel;

    }

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        payrollReceiptList = new JPanel();
        payrollReceiptList.setLayout(new BoxLayout(payrollReceiptList, BoxLayout.Y_AXIS));
        payrollReceiptList.setBackground(new Color(235, 235, 235));
        JScrollPane scrollPane = new JScrollPane(payrollReceiptList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Payroll Receipts"));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search Payslips (ID, name, or date):"));
        txtSearchReceipt = new JTextField(22);
        searchPanel.add(txtSearchReceipt);
        txtSearchReceipt.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshPayrollReceiptList();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshPayrollReceiptList();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshPayrollReceiptList();
            }
        });

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel summaryActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        comboDeleteReceipt = new JComboBox<>();
        comboDeleteReceipt.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PayrollRecord) {
                    Object[] row = ((PayrollRecord) value).toRowData();
                    setText(row[0] + " - " + row[1] + " - " + row[8]);
                }
                return this;
            }
        });
        summaryActions.add(new JLabel("Delete payslip:"));
        summaryActions.add(comboDeleteReceipt);
        JButton btnDeleteSelected = new JButton("Delete Selected Payslip");
        btnDeleteSelected.addActionListener(e -> deleteSelectedPayrollReceipt());
        summaryActions.add(btnDeleteSelected);
        JButton btnExport = new JButton("Export Payroll Summary");
        btnExport.addActionListener(e -> SavePayrollToTextFileGUI());
        summaryActions.add(btnExport);
        panel.add(summaryActions, BorderLayout.SOUTH);
        return panel;

    }
// receipt instead of table row kay wala lang
    private void addPayrollReceipt(PayrollRecord record) { 
        Object[] rowData = record.toRowData();
        String receiptText = String.format(
                     "             PAYROLL RECEIPT%n" +
                        "--------------------------------%n" +
                        "Employee ID:      %s%n" +
                        "Employee Name:    %s%n" +
                        "Salary Release:   %s%n" +
                        "Attendance Period: %s%n" +
                        "Attendance:       %s / %s workdays%n" +
                        "--------------------------------%n" +
                        "Basic Salary:     %s%n" +
                        "Job Deduction:    %s%n" +
                        "Letter Status:    %s%n" +
                        "Letter Penalty:   %s%n" +
                        "Attendance Deduction: %s%n" +
                        "Overtime Pay:     %s%n" +
                        "--------------------------------%n" +
                        "NET PAY:          %s%n",
                rowData[0], rowData[1], rowData[8], rowData[11], rowData[9], rowData[12],
                rowData[2], rowData[3], rowData[4], rowData[5], rowData[10], rowData[6], rowData[7]);

        JTextArea receipt = new JTextArea(receiptText, 12, 34);
        receipt.setEditable(false);
        receipt.setFocusable(false);
        receipt.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        receipt.setBackground(Color.WHITE);
        receipt.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        JPanel receiptContainer = new JPanel(new BorderLayout(0, 4));
        receiptContainer.setBackground(payrollReceiptList.getBackground());
        receiptContainer.add(receipt, BorderLayout.CENTER);
        payrollReceiptList.add(receiptContainer);
        payrollReceiptList.revalidate();
        payrollReceiptList.repaint();
    }

    private void refreshPayrollReceiptList() {
        String query = txtSearchReceipt.getText().trim().toLowerCase(java.util.Locale.ROOT);
        payrollReceiptList.removeAll();
        comboDeleteReceipt.removeAllItems();
        int matchingReceipts = 0;
        for (PayrollRecord record : payrollRecords) {
            String receiptData = java.util.Arrays.toString(record.toRowData()).toLowerCase(java.util.Locale.ROOT);
            if (receiptData.contains(query)) {
                addPayrollReceipt(record);
                comboDeleteReceipt.addItem(record);
                matchingReceipts++;
            }
        }
        if (matchingReceipts == 0) {
            payrollReceiptList.add(new JLabel("No payslips found."));
        }
        payrollReceiptList.revalidate();
        payrollReceiptList.repaint();
    }

    private void deleteSelectedPayrollReceipt() {
        PayrollRecord record = (PayrollRecord) comboDeleteReceipt.getSelectedItem();
        if (record == null) {
            JOptionPane.showMessageDialog(this, "There are no payslips to delete.", "No Payslip Selected",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirmation = JOptionPane.showConfirmDialog(this,
                "Delete the payslip for employee " + record.toRowData()[0] + "?",
                "Confirm Payslip Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }

        int recordIndex = payrollRecords.indexOf(record);
        if (recordIndex < 0) {
            return;
        }
        List<PayrollRecord> remainingRecords = new ArrayList<>(payrollRecords);
        remainingRecords.remove(recordIndex);
        try {
            writeAtomically(PAYROLL_FILE, formatPayrollReceipts(remainingRecords));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save payslip changes: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        payrollRecords.remove(recordIndex);
    refreshPayrollReceiptList();
    }

    private void loadEmployees() {
        if (!EMPLOYEE_FILE.isFile()) {
            return;
        }
        try {
            for (String line : Files.readAllLines(EMPLOYEE_FILE.toPath(), StandardCharsets.UTF_8)) {
                String[] data = line.split("\\s*\\|\\s*", -1);
                if (data.length != 4) {
                    throw new IOException("Invalid employee record: " + line);
                }
                employeeList.add(new Employee(data[1], data[0], data[2], Double.parseDouble(data[3])));
            }
        } catch (IOException | NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Error reading employee file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadPayrollData() {
        if (!PAYROLL_FILE.isFile()) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(PAYROLL_FILE.toPath(), StandardCharsets.UTF_8);
            boolean receiptFormat = lines.stream().anyMatch(line -> line.contains("PAYROLL RECEIPT"));
            if (receiptFormat) {
                List<String> receiptLines = new ArrayList<>();
                for (String line : lines) {
                    if (line.contains("PAYROLL RECEIPT")) {
                        loadPayrollReceipt(receiptLines);
                        receiptLines.clear();
                    } else if (!line.trim().isEmpty() && !line.trim().startsWith("---")) {
                        receiptLines.add(line);
                    }
                }
                loadPayrollReceipt(receiptLines);
            } else {
                for (String line : lines) {
                    String[] data = line.split("\\s*\\|\\s*", -1);
                    if (data.length != 8) {
                        throw new IOException("Invalid payroll record: " + line);
                    }
                    boolean letterSubmitted = data[4].contains("(Waived)");
                    PayrollRecord record = new PayrollRecord(data[1], data[0], parseAmount(data[2]),
                            Math.abs(parseAmount(data[3])), parseAmount(data[7]), letterSubmitted,
                            Math.abs(parseAmount(data[5])), parseAmount(data[6]));
                    payrollRecords.add(record);
                    addPayrollReceipt(record);
                }
            }
        } catch (IOException | NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Error reading payroll file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadPayrollReceipt(List<String> receiptLines) {
        HashMap<String, String> fields = new HashMap<>();
        for (String line : receiptLines) {
            int separator = line.indexOf(':');
            if (separator >= 0) {
                fields.put(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
            }
        }
        if (!fields.containsKey("Employee ID") || !fields.containsKey("Employee Name")) {
            return;
        }

        boolean letterSubmitted = fields.getOrDefault("Letter Status", "").contains("(Waived)");
        String salaryReleaseDate = fields.getOrDefault("Salary Release", "15th of each month");
        String[] attendanceValues = fields.getOrDefault("Attendance", "28 / 28 workdays").split("/");
        int daysPresent = Integer.parseInt(attendanceValues[0].trim());
        int attendanceWorkdays = attendanceValues.length > 1
            ? Integer.parseInt(attendanceValues[1].replace("workdays", "").replace("days", "").trim())
            : 28;
        double attendanceDeduction = fields.containsKey("Attendance Deduction")
            ? Math.abs(parseAmount(fields.get("Attendance Deduction"))) : 0.0;
        String attendancePeriod = fields.getOrDefault("Attendance Period", "Not recorded");
        PayrollRecord record = new PayrollRecord(fields.get("Employee Name"), fields.get("Employee ID"),
                parseAmount(fields.get("Basic Salary")), Math.abs(parseAmount(fields.get("Job Deduction"))),
                parseAmount(fields.get("NET PAY")), letterSubmitted,
                Math.abs(parseAmount(fields.get("Letter Penalty"))), parseAmount(fields.get("Overtime Pay")),
                salaryReleaseDate, daysPresent, attendanceDeduction, attendancePeriod, attendanceWorkdays);
        payrollRecords.add(record);
        addPayrollReceipt(record);
    }

    private double parseAmount(String amount) {
        return Double.parseDouble(amount.trim().replace("P", "").replace("+", ""));
    }

    private void saveEmployees() throws IOException {
        List<String> lines = new ArrayList<>();
        for (Employee employee : employeeList) {
            lines.add(String.format("%s | %s | %s | %.2f", employee.getId(), employee.getName(),
                    employee.getJobType(), employee.getBasicSalary()));
        }
        writeAtomically(EMPLOYEE_FILE, lines);
    }

    private void writeAtomically(File file, List<String> lines) throws IOException {
        Path target = file.toPath().toAbsolutePath();
        Path temporaryFile = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private void SavePayrollToTextFileGUI() {
        if (payrollRecords.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No payroll records available to export.", "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;

        }
        try {
            writeAtomically(PAYROLL_FILE, formatPayrollReceipts(payrollRecords));
            JOptionPane.showMessageDialog(this, "Payroll summary exported successfully to "
                    + PAYROLL_FILE.getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error exporting payroll summary: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);

        }

    }

    private List<String> formatPayrollReceipts(List<PayrollRecord> records) {
        List<String> lines = new ArrayList<>();
        for (PayrollRecord payrollRecord : records) {
            Object[] rowData = payrollRecord.toRowData();
            lines.add(String.format(
                    "             PAYROLL RECEIPT%n" +
                            "--------------------------------%n" +
                            "Employee ID:      %s%n" +
                            "Employee Name:    %s%n" +
                            "Salary Release:   %s%n" +
                            "Attendance Period: %s%n" +
                            "Attendance:       %s / %s workdays%n" +
                            "--------------------------------%n" +
                            "Basic Salary:     %s%n" +
                            "Job Deduction:    %s%n" +
                            "Letter Status:    %s%n" +
                            "Letter Penalty:   %s%n" +
                            "Attendance Deduction: %s%n" +
                            "Overtime Pay:     %s%n" +
                            "--------------------------------%n" +
                            "NET PAY:          %s%n",
                            rowData[0], rowData[1], rowData[8], rowData[11], rowData[9], rowData[12],
                            rowData[2], rowData[3], rowData[4], rowData[5], rowData[10], rowData[6], rowData[7]));
        }
        return lines;
    }

    private void addEmployee() {
        String id = nextGeneratedEmployeeId;
        String name = txtName.getText().trim();
        String jobType = (String) comboJobType.getSelectedItem();
        String salaryStr = txtSalary.getText().trim();

        if (name.isEmpty() || salaryStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double basicSalary;
        try {
            basicSalary = Double.parseDouble(salaryStr);
            if (basicSalary < 0) {
                throw new NumberFormatException("Negative salary");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive number for salary.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check for duplicate ID
        for (Employee emp : employeeList) {
            if (emp.getId().equals(id)) {
                JOptionPane.showMessageDialog(this, "Employee ID already exists. Please use a unique ID.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        Employee newEmployee = new Employee(name, id, jobType, basicSalary);
        employeeList.add(newEmployee);
        try {
            saveEmployees();
        } catch (IOException ex) {
            employeeList.remove(newEmployee);
            JOptionPane.showMessageDialog(this, "Error saving employee file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        refreshEmployeeDropdown();
        refreshEmployeeTable();
        updateEmployeeIdPreview();
        JOptionPane.showMessageDialog(this, "Employee added successfully.");

    }

    private String generateNextEmployeeId() {
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 900; attempt++) {
            int candidateNumber = random.nextInt(100, 1000);
            String candidate = String.format(java.util.Locale.ROOT, "EMP-%03d", candidateNumber);
            if (!containsEmployeeId(candidate)) {
                return candidate;
            }
        }

        String candidate;
        do {
            int candidateNumber = random.nextInt(1000, 10000);
            candidate = String.format(java.util.Locale.ROOT, "EMP-%04d", candidateNumber);
        } while (containsEmployeeId(candidate));
        return candidate;
    }

    private boolean containsEmployeeId(String id) {
        for (Employee employee : employeeList) {
            if (employee.getId().equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    private void updateEmployeeIdPreview() {
        nextGeneratedEmployeeId = generateNextEmployeeId();
        txtId.setText(nextGeneratedEmployeeId);
    }

    private void loadSelectedEmployeeIntoForm() {
        int selectedRow = empTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        int modelRow = empTable.convertRowIndexToModel(selectedRow);
        txtName.setText(empTableModel.getValueAt(modelRow, 1).toString());
        comboJobType.setSelectedItem(empTableModel.getValueAt(modelRow, 2).toString());
        txtSalary.setText(empTableModel.getValueAt(modelRow, 3).toString().replace("P", ""));
    }

    private void updateEmployee() {
        int selectedRow = empTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an employee row to update.", "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = empTable.convertRowIndexToModel(selectedRow);
        String originalId = empTableModel.getValueAt(modelRow, 0).toString();
        int employeeIndex = -1;
        for (int i = 0; i < employeeList.size(); i++) {
            if (employeeList.get(i).getId().equals(originalId)) {
                employeeIndex = i;
                break;
            }
        }
        if (employeeIndex < 0) {
            return;
        }

        String id = originalId;
        String name = txtName.getText().trim();
        String jobType = (String) comboJobType.getSelectedItem();
        String salaryText = txtSalary.getText().trim();
        if (id.isEmpty() || name.isEmpty() || salaryText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double basicSalary;
        try {
            basicSalary = Double.parseDouble(salaryText);
            if (basicSalary < 0) {
                throw new NumberFormatException("Negative salary");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive number for salary.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (int i = 0; i < employeeList.size(); i++) {
            if (i != employeeIndex && employeeList.get(i).getId().equalsIgnoreCase(id)) {
                JOptionPane.showMessageDialog(this, "Employee ID already exists. Please use a unique ID.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        Employee previousEmployee = employeeList.set(employeeIndex,
                new Employee(name, id, jobType, basicSalary));
        try {
            saveEmployees();
        } catch (IOException ex) {
            employeeList.set(employeeIndex, previousEmployee);
            JOptionPane.showMessageDialog(this, "Error saving employee file: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        refreshEmployeeTable();
        refreshEmployeeDropdown();
        JOptionPane.showMessageDialog(this, "Employee updated successfully.");
    }

    private void removeEmployee() {
        int selectedRow = empTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an employee row to delete.", "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Convert the visible sorted/filtered row index to the original table-model row index.
        int modelRow = empTable.convertRowIndexToModel(selectedRow);
        String targetId = empTableModel.getValueAt(modelRow, 0).toString();
        int employeeIndex = -1;
        for (int i = 0; i < employeeList.size(); i++) {
            if (employeeList.get(i).getId().equals(targetId)) {
                employeeIndex = i;
                break;
            }
        }
        if (employeeIndex < 0) {
            return;
        }
        Employee removedEmployee = employeeList.remove(employeeIndex);
        try {
            saveEmployees();
        } catch (IOException ex) {
            employeeList.add(employeeIndex, removedEmployee);
            JOptionPane.showMessageDialog(this, "Error saving employee file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        refreshEmployeeTable();
        refreshEmployeeDropdown();
        updateEmployeeIdPreview();
        JOptionPane.showMessageDialog(this, "Employee removed successfully.");

    }

    private void computePayroll() {
        if (employeeList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No employees available to compute payroll.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;

        }
        String selectedSelection = (String) comboEmployeeSelect.getSelectedItem();
        if (selectedSelection == null)
            return;

        String empId = selectedSelection.split(" - ")[0];
        Employee target = null;
        for (Employee e : employeeList) {
            if (e.getId().equalsIgnoreCase(empId)) {
                target = e;
                break;
            }
        }
        if (target == null)
            return;
        try {
            double otHours = Double.parseDouble(txtOvertime.getText().trim());
            int daysPresent = Integer.parseInt(txtDaysPresent.getText().trim());
            java.time.LocalDate attendanceStart = getAttendanceDate(spinnerAttendanceStart);
            java.time.LocalDate attendanceEnd = getAttendanceDate(spinnerAttendanceEnd);
            if (attendanceEnd.isBefore(attendanceStart)) {
                JOptionPane.showMessageDialog(this, "Attendance end date must be on or after the start date.",
                        "Invalid Attendance Period", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int attendanceWorkdays = countAttendanceWorkdays(attendanceStart, attendanceEnd);
            if (attendanceWorkdays == 0) {
                JOptionPane.showMessageDialog(this, "The selected attendance period has no weekdays.",
                        "Invalid Attendance Period", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (daysPresent < 0 || daysPresent > attendanceWorkdays) {
                JOptionPane.showMessageDialog(this,
                        "Days present must be from 0 to " + attendanceWorkdays + " for the selected period.",
                        "Invalid Attendance",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            boolean letterSubmitted = chkLetterSubmitted.isSelected();

            // Payroll Calculation Logic
            double basic = target.getBasicSalary();
                double jobPercentage = Integer.parseInt(
                    ((String) comboJobDeductionPercent.getSelectedItem()).replace("%", "")) / 100.0;
            double jobDeduction = basic * jobPercentage;
            double attendanceDeduction = (basic / attendanceWorkdays) * (attendanceWorkdays - daysPresent);

            double letterDeduction = 0.0;// Initialize letter deduction
            if (!letterSubmitted) {
                letterDeduction = basic * 0.02; // 2% penalty

            }
            // Overtime Pay Calculation: 1.25x the hourly rate for each hour worked beyond 8
            // hours/day (assuming 28 working days in a month)
            double hourlyRate = (basic / 28.0) / 8.0;
            double overtimePay = otHours * (hourlyRate * 1.25);

            // Net Pay Calculation
            double totalDeductions = jobDeduction + letterDeduction + attendanceDeduction;
            double netPay = (basic + overtimePay) - totalDeductions;

            PayrollRecord record = new PayrollRecord(target.getName(), target.getId(), basic, jobDeduction, netPay,
                    letterSubmitted, letterDeduction, overtimePay,
                    ordinalDay((Integer) comboSalaryReleaseDay.getSelectedItem()) + " of "
                            + comboSalaryReleaseMonth.getSelectedItem(), daysPresent, attendanceDeduction,
                        attendanceStart + " to " + attendanceEnd, attendanceWorkdays);
            payrollRecords.add(record);
            refreshPayrollReceiptList();

            JOptionPane.showMessageDialog(this, String.format("Payroll computed for %s (ID: %s)\nNet Pay: P%.2f",
                    target.getName(), target.getId(), netPay), "Payroll Computed", JOptionPane.INFORMATION_MESSAGE);
            JOptionPane.showMessageDialog(this, "Payslip generated.", "Payslip Generated",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Enter valid numbers for overtime hours and days present.", "Error",
                    JOptionPane.ERROR_MESSAGE);

        }

    }

    private void refreshEmployeeDropdown() {
        comboEmployeeSelect.removeAllItems();
        for (Employee emp : employeeList) {
            comboEmployeeSelect.addItem(emp.getId() + " - " + emp.getName());
        }
        updateSelectedEmployeePosition();
    }

    private void refreshSalaryReleaseDays() {
        int selectedDay = comboSalaryReleaseDay.getSelectedItem() instanceof Integer
                ? (Integer) comboSalaryReleaseDay.getSelectedItem() : 15;
        int month = comboSalaryReleaseMonth.getSelectedIndex() + 1;
        int year = java.time.LocalDate.now().getYear();
        int daysInMonth = java.time.YearMonth.of(year, month).lengthOfMonth();

        comboSalaryReleaseDay.removeAllItems();
        for (int day = 1; day <= daysInMonth; day++) {
            comboSalaryReleaseDay.addItem(day);
        }
        comboSalaryReleaseDay.setSelectedItem(Math.min(selectedDay, daysInMonth));
    }

    private JSpinner createDateSpinner(java.time.LocalDate date) {
        java.util.Date initialDate = java.util.Date.from(
                date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        JSpinner spinner = new JSpinner(new SpinnerDateModel(initialDate, null, null, java.util.Calendar.DAY_OF_MONTH));
        spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));
        return spinner;
    }

    private java.time.LocalDate getAttendanceDate(JSpinner spinner) {
        java.util.Date date = (java.util.Date) spinner.getValue();
        return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private int countAttendanceWorkdays(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        int workdays = 0;
        for (java.time.LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            java.time.DayOfWeek day = date.getDayOfWeek();
            if (day != java.time.DayOfWeek.SATURDAY && day != java.time.DayOfWeek.SUNDAY) {
                workdays++;
            }
        }
        return workdays;
    }

    private void updateAttendanceDaysLimit() {
        java.time.LocalDate startDate = getAttendanceDate(spinnerAttendanceStart);
        java.time.LocalDate endDate = getAttendanceDate(spinnerAttendanceEnd);
        if (endDate.isBefore(startDate)) {
            return;
        }

        int workdays = countAttendanceWorkdays(startDate, endDate);
        int daysPresent;
        try {
            daysPresent = Integer.parseInt(txtDaysPresent.getText().trim());
        } catch (NumberFormatException ex) {
            daysPresent = workdays;
        }
        txtDaysPresent.setText(Integer.toString(Math.max(0, Math.min(daysPresent, workdays))));
    }

    private String ordinalDay(int day) {
        int lastTwoDigits = day % 100;
        if (lastTwoDigits >= 11 && lastTwoDigits <= 13) {
            return day + "th";
        }
        switch (day % 10) {
            case 1:
                return day + "st";
            case 2:
                return day + "nd";
            case 3:
                return day + "rd";
            default:
                return day + "th";
        }
    }

    private void updateSelectedEmployeePosition() {
        if (txtPosition == null) {
            return;
        }
        String selectedSelection = (String) comboEmployeeSelect.getSelectedItem();
        if (selectedSelection == null) {
            txtPosition.setText("");
            return;
        }
        String empId = selectedSelection.split(" - ")[0];
        for (Employee employee : employeeList) {
            if (employee.getId().equalsIgnoreCase(empId)) {
                txtPosition.setText(employee.getJobType());
                return;
            }
        }
        txtPosition.setText("");
    }

    private void refreshEmployeeTable() {
        empTableModel.setRowCount(0); // Clear existing rows
        for (Employee emp : employeeList) {
            empTableModel.addRow(new Object[] { emp.getId(), emp.getName(), emp.getJobType(),
                    String.format("P%.2f", emp.getBasicSalary()) });
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EMPManagementGUIall gui = new EMPManagementGUIall();
            gui.setVisible(true);
        });
    }

}