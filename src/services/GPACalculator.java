package services;

import interfaces.Calculate;
import models.*;

import java.util.HashMap;
import java.util.Map;

public class GPACalculator implements Calculate {
    private GradeManager gradeManager;
    private StudentManager studentManager;

    public GPACalculator(StudentManager studentManager, GradeManager gradeManager) {
        this.studentManager = studentManager;
        this.gradeManager = gradeManager;
    }

    public double convertToGPA(double percentage) {
        if (percentage >= 93) return 4.0;
        else if (percentage >= 90) return 3.7;
        else if (percentage >= 87) return 3.3;
        else if (percentage >= 83) return 3.0;
        else if (percentage >= 80) return 2.7;
        else if (percentage >= 77) return 2.3;
        else if (percentage >= 73) return 2.0;
        else if (percentage >= 70) return 1.7;
        else if (percentage >= 67) return 1.3;
        else if (percentage >= 60) return 1.0;
        else return 0.0;
    }

    public String getLetterGrade(double percentage) {
        if (percentage >= 93) return "A";
        else if (percentage >= 90) return "A-";
        else if (percentage >= 87) return "B+";
        else if (percentage >= 83) return "B";
        else if (percentage >= 80) return "B-";
        else if (percentage >= 77) return "C+";
        else if (percentage >= 73) return "C";
        else if (percentage >= 70) return "C-";
        else if (percentage >= 67) return "D+";
        else if (percentage >= 60) return "D";
        else return "F";
    }

    @Override
    public double calculateGPA(String studentId) {
        Grade[] grades = gradeManager.getGrades();
        int gradeCount = gradeManager.getGradeCount();
        double totalGPA = 0.0;
        int count = 0;

        for (int i = 0; i < gradeCount; i++) {
            if (grades[i].getStudentId().equals(studentId)) {
                totalGPA += convertToGPA(grades[i].getGrade());
                count++;
            }
        }

        return count > 0 ? totalGPA / count : 0.0;
    }

    @Override
    public double calculateClassRank(String studentId) {
        double studentGPA = calculateGPA(studentId);
        Student[] students = studentManager.getStudents();
        int studentCount = studentManager.getStudentCountValue();

        int betterStudents = 0;
        int totalWithGrades = 0;

        for (int i = 0; i < studentCount; i++) {
            double otherGPA = calculateGPA(students[i].getStudentId());
            if (otherGPA > 0) { // Only count students with grades
                totalWithGrades++;
                if (otherGPA > studentGPA) {
                    betterStudents++;
                }
            }
        }

        return totalWithGrades > 0 ? betterStudents + 1 : 1; // Rank is position (1st, 2nd, etc.)
    }

    @Override
    public Map<String, Double> calculateSubjectAverages(String studentId) {
        Map<String, Double> subjectAverages = new HashMap<>();
        Grade[] grades = gradeManager.getGrades();
        int gradeCount = gradeManager.getGradeCount();

        Map<String, Double> subjectSums = new HashMap<>();
        Map<String, Integer> subjectCounts = new HashMap<>();

        for (int i = 0; i < gradeCount; i++) {
            if (grades[i].getStudentId().equals(studentId)) {
                String subjectName = grades[i].getSubject().getSubjectName();
                double grade = grades[i].getGrade();

                subjectSums.put(subjectName, subjectSums.getOrDefault(subjectName, 0.0) + grade);
                subjectCounts.put(subjectName, subjectCounts.getOrDefault(subjectName, 0) + 1);
            }
        }

        for (String subject : subjectSums.keySet()) {
            double average = subjectSums.get(subject) / subjectCounts.get(subject);
            subjectAverages.put(subject, average);
        }

        return subjectAverages;
    }

    public void displayGPABreakdown(String studentId) {
        Student student = studentManager.findStudent(studentId);
        if (student == null) return;

        System.out.println("\nGPA CALCULATION (4.0 Scale)");
        System.out.println();
        System.out.printf("%-15s | %-8s | %s%n", "SUBJECT", "GRADE", "GPA POINTS");
        System.out.println("--------------------------------------");

        Grade[] grades = gradeManager.getGrades();
        int gradeCount = gradeManager.getGradeCount();
        double totalGPA = 0.0;
        int count = 0;

        for (int i = 0; i < gradeCount; i++) {
            if (grades[i].getStudentId().equals(studentId)) {
                Grade grade = grades[i];
                double gpaPoints = convertToGPA(grade.getGrade());
                String letterGrade = getLetterGrade(grade.getGrade());

                System.out.printf("%-15s | %-8s | %.1f (%s)%n",
                        grade.getSubject().getSubjectName(),
                        String.format("%.1f%%", grade.getGrade()),
                        gpaPoints, letterGrade);

                totalGPA += gpaPoints;
                count++;
            }
        }

        if (count > 0) {
            double cumulativeGPA = totalGPA / count;
            int classRank = (int) calculateClassRank(studentId);
            int totalStudents = studentManager.getStudentCountValue();

            System.out.println();
            System.out.println("Cumulative GPA: " + String.format("%.2f / 4.0", cumulativeGPA));
            System.out.println("Letter Grade: " + getLetterGrade(gradeManager.calculateOverallAverage(studentId)));
            System.out.println("Class Rank: " + classRank + " of " + totalStudents);
            System.out.println();

            System.out.println("Performance Analysis:");
            if (cumulativeGPA >= 3.5) {
                System.out.println("✓ Excellent performance (3.5+ GPA)");
            } else if (cumulativeGPA >= 3.0) {
                System.out.println("✓ Good performance (3.0+ GPA)");
            } else if (cumulativeGPA >= 2.0) {
                System.out.println("✓ Satisfactory performance (2.0+ GPA)");
            } else {
                System.out.println("✗ Needs improvement (<2.0 GPA)");
            }

            if (student instanceof HonorsStudent) {
                HonorsStudent honorsStudent = (HonorsStudent) student;
                if (honorsStudent.checkHonorsEligibility()) {
                    System.out.println("✓ Honors eligibility maintained");
                } else {
                    System.out.println("✗ Honors eligibility not met");
                }
            }

            double classAverageGPA = calculateClassAverageGPA();
            if (cumulativeGPA > classAverageGPA) {
                System.out.println("✓ Above class average (" + String.format("%.2f", classAverageGPA) + " GPA)");
            }
        }
    }

    private double calculateClassAverageGPA() {
        Student[] students = studentManager.getStudents();
        int studentCount = studentManager.getStudentCountValue();
        double totalGPA = 0.0;
        int count = 0;

        for (int i = 0; i < studentCount; i++) {
            double gpa = calculateGPA(students[i].getStudentId());
            if (gpa > 0) {
                totalGPA += gpa;
                count++;
            }
        }

        return count > 0 ? totalGPA / count : 0.0;
    }
}