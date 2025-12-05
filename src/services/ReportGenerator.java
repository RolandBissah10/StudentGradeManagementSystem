package services;

import exceptions.ExportException;
import interfaces.Exportable;
import models.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ReportGenerator implements Exportable {
    private StudentManager studentManager;
    private GradeManager gradeManager;

    public ReportGenerator(StudentManager studentManager, GradeManager gradeManager) {
        this.studentManager = studentManager;
        this.gradeManager = gradeManager;
    }

    @Override
    public void exportSummaryReport(String studentId, String filename) throws ExportException {
        try (PrintWriter writer = new PrintWriter(new FileWriter("reports/" + filename + "_summary.txt"))) {
            Student student = studentManager.findStudent(studentId);
            if (student == null) {
                throw new ExportException("Student not found: " + studentId);
            }

            writer.println("STUDENT GRADE SUMMARY REPORT");
            writer.println("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            writer.println("=========================================");
            writer.println();
            writer.println("Student ID: " + student.getStudentId());
            writer.println("Name: " + student.getName());
            writer.println("Type: " + student.getStudentType() + " Student");
            writer.println("Status: " + student.getStatus());
            writer.println();

            double overallAvg = gradeManager.calculateOverallAverage(studentId);
            double coreAvg = gradeManager.calculateCoreAverage(studentId);
            double electiveAvg = gradeManager.calculateElectiveAverage(studentId);

            writer.println("PERFORMANCE SUMMARY");
            writer.println("Overall Average: " + String.format("%.1f%%", overallAvg));
            writer.println("Core Subjects Average: " + String.format("%.1f%%", coreAvg));
            writer.println("Elective Subjects Average: " + String.format("%.1f%%", electiveAvg));
            writer.println("Passing Status: " + (overallAvg >= student.getPassingGrade() ? "PASSING" : "FAILING"));

            if (student instanceof HonorsStudent) {
                HonorsStudent honorsStudent = (HonorsStudent) student;
                writer.println("Honors Eligible: " + (honorsStudent.checkHonorsEligibility() ? "YES" : "NO"));
            }

        } catch (IOException e) {
            throw new ExportException("Cannot write to file: " + e.getMessage());
        }
    }

    @Override
    public void exportDetailedReport(String studentId, String filename) throws ExportException {
        try (PrintWriter writer = new PrintWriter(new FileWriter("reports/" + filename + "_detailed.txt"))) {
            Student student = studentManager.findStudent(studentId);
            if (student == null) {
                throw new ExportException("Student not found: " + studentId);
            }

            writer.println("DETAILED GRADE REPORT");
            writer.println("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            writer.println("=========================================");
            writer.println();
            writer.println("STUDENT INFORMATION");
            writer.println("ID: " + student.getStudentId());
            writer.println("Name: " + student.getName());
            writer.println("Type: " + student.getStudentType());
            writer.println("Age: " + student.getAge());
            writer.println("Email: " + student.getEmail());
            writer.println("Phone: " + student.getPhone());
            writer.println("Passing Grade: " + student.getPassingGrade() + "%");
            writer.println();

            writer.println("GRADE HISTORY");
            writer.println("---------------------------------------------------");
            writer.printf("%-8s | %-10s | %-15s | %-8s | %s%n",
                    "GRD ID", "DATE", "SUBJECT", "TYPE", "GRADE");
            writer.println("---------------------------------------------------");

            Grade[] grades = gradeManager.getGrades();
            int gradeCount = gradeManager.getGradeCount();
            int studentGradeCount = 0;

            for (int i = gradeCount - 1; i >= 0; i--) {
                if (grades[i].getStudentId().equals(studentId)) {
                    Grade grade = grades[i];
                    writer.printf("%-8s | %-10s | %-15s | %-8s | %.1f%%%n",
                            grade.getGradeId(),
                            grade.getDate(),
                            grade.getSubject().getSubjectName(),
                            grade.getSubject().getSubjectType(),
                            grade.getGrade());
                    studentGradeCount++;
                }
            }

            writer.println();
            writer.println("STATISTICS");
            writer.println("Total Grades: " + studentGradeCount);
            writer.println("Overall Average: " + String.format("%.1f%%", gradeManager.calculateOverallAverage(studentId)));
            writer.println("Core Subjects Average: " + String.format("%.1f%%", gradeManager.calculateCoreAverage(studentId)));
            writer.println("Elective Subjects Average: " + String.format("%.1f%%", gradeManager.calculateElectiveAverage(studentId)));

        } catch (IOException e) {
            throw new ExportException("Cannot write to file: " + e.getMessage());
        }
    }

    @Override
    public void exportSearchResults(List<Student> students, String filename) throws ExportException {
        try (PrintWriter writer = new PrintWriter(new FileWriter("reports/" + filename + "_search.txt"))) {
            writer.println("STUDENT SEARCH RESULTS");
            writer.println("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            writer.println("=========================================");
            writer.println();
            writer.printf("%-8s | %-20s | %-10s | %s%n", "ID", "NAME", "TYPE", "AVERAGE");
            writer.println("---------------------------------------------------");

            for (Student student : students) {
                double avg = gradeManager.calculateOverallAverage(student.getStudentId());
                writer.printf("%-8s | %-20s | %-10s | %.1f%%%n",
                        student.getStudentId(),
                        student.getName(),
                        student.getStudentType(),
                        avg);
            }

        } catch (IOException e) {
            throw new ExportException("Cannot write to file: " + e.getMessage());
        }
    }
}