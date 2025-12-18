package models;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public abstract class Student implements Serializable {
    private static final long serialVersionUID = 1L;

    private String studentId;
    private String name;
    private int age;
    private String email;
    private String phone;
    private String status;
    private LocalDate enrollmentDate;

    private static int studentCounter = 0;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public Student(String name, int age, String email, String phone, String enrollmentDate) {
        this.studentId = generateStudentId();
        this.name = name;
        this.age = age;
        this.email = email;
        this.phone = phone;
        this.status = "Active";
        this.enrollmentDate = LocalDate.parse(enrollmentDate, DATE_FORMATTER);
    }

    private String generateStudentId() {
        studentCounter++;
        return String.format("STU%05d", studentCounter);
    }

    public abstract void displayStudentDetails();
    public abstract String getStudentType();
    public abstract double getPassingGrade();

    public String getStudentId() { return studentId; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getStatus() { return status; }
    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public String getEnrollmentDateString() {
        return enrollmentDate.format(DATE_FORMATTER);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setEnrollmentDate(String date) {
        this.enrollmentDate = LocalDate.parse(date, DATE_FORMATTER);
    }

    public static int getStudentCounter() {
        return studentCounter;
    }
}