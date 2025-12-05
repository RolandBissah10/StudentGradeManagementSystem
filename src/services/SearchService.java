package services;

import interfaces.Searchable;
import models.GradeManager;
import models.Student;
import models.StudentManager;

import java.util.ArrayList;
import java.util.List;

public class SearchService implements Searchable {
    private StudentManager studentManager;
    private GradeManager gradeManager;

    public SearchService(StudentManager studentManager, GradeManager gradeManager) {
        this.studentManager = studentManager;
        this.gradeManager = gradeManager;
    }

    @Override
    public List<Student> searchByName(String name) {
        List<Student> results = new ArrayList<>();
        Student[] students = studentManager.getStudents();
        int studentCount = studentManager.getStudentCountValue();

        for (int i = 0; i < studentCount; i++) {
            if (students[i].getName().toLowerCase().contains(name.toLowerCase())) {
                results.add(students[i]);
            }
        }

        return results;
    }

    @Override
    public List<Student> searchByGradeRange(double minGrade, double maxGrade) {
        List<Student> results = new ArrayList<>();
        Student[] students = studentManager.getStudents();
        int studentCount = studentManager.getStudentCountValue();

        for (int i = 0; i < studentCount; i++) {
            double avg = gradeManager.calculateOverallAverage(students[i].getStudentId());
            if (avg >= minGrade && avg <= maxGrade) {
                results.add(students[i]);
            }
        }

        return results;
    }

    @Override
    public List<Student> searchByStudentType(String type) {
        List<Student> results = new ArrayList<>();
        Student[] students = studentManager.getStudents();
        int studentCount = studentManager.getStudentCountValue();

        for (int i = 0; i < studentCount; i++) {
            if (students[i].getStudentType().equalsIgnoreCase(type)) {
                results.add(students[i]);
            }
        }

        return results;
    }

    public List<Student> searchByStudentId(String studentId) {
        List<Student> results = new ArrayList<>();
        Student student = studentManager.findStudent(studentId);
        if (student != null) {
            results.add(student);
        }
        return results;
    }

    public void displaySearchResults(List<Student> students) {
        if (students.isEmpty()) {
            System.out.println("No students found matching your criteria.");
            return;
        }

        System.out.println("\nSEARCH RESULTS (" + students.size() + " found)");
        System.out.println();
        System.out.printf("%-8s | %-20s | %-10s | %s%n", "STU ID", "NAME", "TYPE", "AVG GRADE");
        System.out.println("---------------------------------------------------");

        for (Student student : students) {
            double avg = gradeManager.calculateOverallAverage(student.getStudentId());
            System.out.printf("%-8s | %-20s | %-10s | %.1f%%%n",
                    student.getStudentId(),
                    student.getName(),
                    student.getStudentType(),
                    avg);
        }
    }
}