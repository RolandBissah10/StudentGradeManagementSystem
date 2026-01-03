package models;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public abstract class Student {
    private static int idCounter = 0;

    private String studentId;
    private String name;
    private int age;
    private String email;
    private String phone;
    private LocalDate enrollmentDate;
    private double averageGrade;
    private double gpa;

    public Student(String name, int age, String email, String phone, String enrollmentDate) {
        this.studentId = generateStudentId();
        this.name = name;
        this.age = age;
        this.email = email;
        this.phone = phone;
        this.enrollmentDate = LocalDate.parse(enrollmentDate);
        this.averageGrade = 0.0;
        this.gpa = 0.0;
    }

    private String generateStudentId() {
        idCounter++;
        return String.format("STU%03d", idCounter);
    }

    // Abstract methods
    public abstract void displayStudentDetails();
    public abstract String getStudentType();
    public abstract double getPassingGrade();
    public abstract String getStatus();

    // GPA methods
    public double getGpa() {
        return gpa;
    }

    public void setGpa(double gpa) {
        this.gpa = gpa;
    }

    // Getters and setters
    public String getStudentId() {
        return studentId;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDate getEnrollmentDate() {
        return enrollmentDate;
    }

    public String getEnrollmentDateString() {
        return enrollmentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public double getAverageGrade() {
        return averageGrade;
    }

    public void setAverageGrade(double averageGrade) {
        this.averageGrade = averageGrade;
    }

    public String getDetailedStatus() {
        return String.format("Average: %.1f%% | GPA: %.2f | Status: %s",
                averageGrade, gpa, getStatus());
    }
}