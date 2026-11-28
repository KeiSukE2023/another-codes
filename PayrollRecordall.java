import java.math.BigDecimal;
import java.math.RoundingMode;

class PayrollRecord {
    private String employeeName;
    private String employeeId;
    private double basicSalary;
    private double jobDeduction;
    private double netPay;
    private boolean letterSubmitted;
    private double letterDeduction;
    private double overtimePay;
    private String salaryReleaseDay;
    private int daysPresent;
    private double attendanceDeduction;
    private String attendancePeriod;
    private int attendanceWorkdays;

    public PayrollRecord(String employeeName, String employeeId, double basicSalary, double jobDeduction, double netPay,
            boolean letterSubmitted, double letterDeduction, double overtimePay, String salaryReleaseDay) {
        this.employeeName = employeeName;
        this.employeeId = employeeId;
        this.basicSalary = basicSalary;
        this.jobDeduction = jobDeduction;
        this.netPay = netPay;
        this.letterSubmitted = letterSubmitted;
        this.letterDeduction = letterDeduction;
        this.overtimePay = overtimePay;
        this.salaryReleaseDay = salaryReleaseDay;
        this.daysPresent = 28;
        this.attendanceDeduction = 0.0;
        this.attendancePeriod = "Not recorded";
        this.attendanceWorkdays = 28;
    }

    public PayrollRecord(String employeeName, String employeeId, double basicSalary, double jobDeduction, double netPay,
            boolean letterSubmitted, double letterDeduction, double overtimePay, String salaryReleaseDay,
            int daysPresent, double attendanceDeduction, String attendancePeriod, int attendanceWorkdays) {
        this.employeeName = employeeName;
        this.employeeId = employeeId;
        this.basicSalary = basicSalary;
        this.jobDeduction = jobDeduction;
        this.netPay = netPay;
        this.letterSubmitted = letterSubmitted;
        this.letterDeduction = letterDeduction;
        this.overtimePay = overtimePay;
        this.salaryReleaseDay = salaryReleaseDay;
        this.daysPresent = daysPresent;
        this.attendanceDeduction = attendanceDeduction;
        this.attendancePeriod = attendancePeriod;
        this.attendanceWorkdays = attendanceWorkdays;
    }

        public PayrollRecord(String employeeName, String employeeId, double basicSalary, double jobDeduction, double netPay,
            boolean letterSubmitted, double letterDeduction, double overtimePay) {
        this(employeeName, employeeId, basicSalary, jobDeduction, netPay, letterSubmitted, letterDeduction,
            overtimePay, "15th of each month");
    }

    public Object[] toRowData() {
        return new Object[] { employeeId, employeeName, "P" + format(basicSalary), "-P" + format(jobDeduction),
                letterSubmitted ? "Submitted(Waived)" : "Missing(-5% Penalty)", "-P" + format(letterDeduction),
                "+P" + format(overtimePay), "P" + format(netPay), salaryReleaseDay,
                daysPresent, "-P" + format(attendanceDeduction), attendancePeriod, attendanceWorkdays };

    }

    private String format(double value) {
        BigDecimal bd = new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
        return bd.toString();
    }
}