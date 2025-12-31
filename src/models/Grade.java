package models;

import interfaces.Gradable;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Grade implements Gradable, Serializable {
    private static final long serialVersionUID = 1L;

    private String gradeId;
    private String studentId;
    private Subject subject;
    private double grade;
    private String date;
    private LocalDateTime timestamp;

    private static int gradeCounter = 0;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat DATE_FORMATTER =
            new SimpleDateFormat("dd-MM-yyyy");

    public Grade(String studentId, Subject subject, double grade) {
        this(generateGradeId(), studentId, subject, grade);
    }

    public Grade(String gradeId, String studentId, Subject subject, double grade) {
        this.gradeId = gradeId;
        this.studentId = studentId;
        this.subject = subject;
        this.grade = grade;
        this.date = generateDate();
        this.timestamp = LocalDateTime.now();
    }

    private static String generateGradeId() {
        gradeCounter++;
        return String.format("GRD%03d", gradeCounter);
    }

    private String generateDate() {
        return DATE_FORMATTER.format(new Date());
    }

    @Override
    public boolean recordGrade(double grade) {
        if (validateGrade(grade)) {
            this.grade = grade;
            this.timestamp = LocalDateTime.now();
            return true;
        }
        return false;
    }

    @Override
    public boolean validateGrade(double grade) {
        return grade >= 0 && grade <= 100;
    }

    public void displayGradeDetails() {
        System.out.println("Grade ID: " + gradeId);
        System.out.println("Student ID: " + studentId);
        System.out.println("Subject: " + subject.getSubjectName() + " (" + subject.getSubjectType() + ")");
        System.out.println("Grade: " + grade + "%");
        System.out.println("Date: " + date);
        System.out.println("Timestamp: " + timestamp.format(TIMESTAMP_FORMATTER));
        System.out.println("Letter Grade: " + getLetterGrade());
    }

    public String getLetterGrade() {
        if (grade >= 93) return "A";
        else if (grade >= 90) return "A-";
        else if (grade >= 87) return "B+";
        else if (grade >= 83) return "B";
        else if (grade >= 80) return "B-";
        else if (grade >= 77) return "C+";
        else if (grade >= 73) return "C";
        else if (grade >= 70) return "C-";
        else if (grade >= 67) return "D+";
        else if (grade >= 60) return "D";
        else return "F";
    }

    public String getGradeId() { return gradeId; }
    public String getStudentId() { return studentId; }
    public Subject getSubject() { return subject; }
    public double getGrade() { return grade; }
    public String getDate() { return date; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getTimestampString() {
        return timestamp.format(TIMESTAMP_FORMATTER);
    }

    public static int getGradeCounter() { return gradeCounter; }
}