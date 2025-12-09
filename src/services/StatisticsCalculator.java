package services;

import models.Grade;
import models.GradeManager;
import models.Student;
import models.StudentManager;

import java.util.*;

public class StatisticsCalculator {
    private StudentManager studentManager;
    private GradeManager gradeManager;

    public StatisticsCalculator(StudentManager studentManager, GradeManager gradeManager) {
        this.studentManager = studentManager;
        this.gradeManager = gradeManager;
    }

    public void displayClassStatistics() {
        System.out.println("\nCLASS STATISTICS");
        System.out.println("=========================================");

        int totalStudents = studentManager.getStudentCountValue();
        int totalGrades = gradeManager.getGradeCount();

        System.out.println("Total Students: " + totalStudents);
        System.out.println("Total Grades Recorded: " + totalGrades);
        System.out.println();

        displayGradeDistribution();
        displayStatisticalAnalysis();
        displaySubjectPerformance();
        displayStudentTypeComparison();
    }

    private void displayGradeDistribution() {
        System.out.println("GRADE DISTRIBUTION");
        System.out.println();

        int[] distribution = new int[5]; // A, B, C, D, F
        Grade[] grades = gradeManager.getGrades();
        int gradeCount = gradeManager.getGradeCount();

        for (int i = 0; i < gradeCount; i++) {
            double grade = grades[i].getGrade();
            if (grade >= 90) distribution[0]++;      // A
            else if (grade >= 80) distribution[1]++; // B
            else if (grade >= 70) distribution[2]++; // C
            else if (grade >= 60) distribution[3]++; // D
            else distribution[4]++;                  // F
        }

        // Find maximum count for scaling
        int maxCount = 0;
        for (int count : distribution) {
            if (count > maxCount) {
                maxCount = count;
            }
        }

        String[] categories = {"90-100% (A)", "80-89% (B)", "70-79% (C)", "60-69% (D)", "0-59% (F)"};

        // Display matrix/bar chart visualization
        for (int i = 0; i < distribution.length; i++) {
            double percentage = gradeCount > 0 ? (distribution[i] * 100.0) / gradeCount : 0;

            // Display category label
            System.out.printf("%-12s: ", categories[i]);

            // Draw bar chart (scaled to fit in console)
            if (maxCount > 0) {
                int barLength = (int) Math.round((distribution[i] * 40.0) / maxCount);
                for (int j = 0; j < barLength; j++) {
                    System.out.print("█");
                }
            }

            // Display percentage and count
            System.out.printf(" %5.1f%% (%d grades)%n", percentage, distribution[i]);
        }
        System.out.println();
    }

    private void displayStatisticalAnalysis() {
        System.out.println("STATISTICAL ANALYSIS");
        System.out.println();

        Grade[] grades = gradeManager.getGrades();
        int gradeCount = gradeManager.getGradeCount();

        if (gradeCount == 0) {
            System.out.println("No grades available for analysis.");
            return;
        }

        // Calculate mean
        double sum = 0;
        double[] gradeValues = new double[gradeCount];

        for (int i = 0; i < gradeCount; i++) {
            gradeValues[i] = grades[i].getGrade();
            sum += gradeValues[i];
        }
        double mean = sum / gradeCount;

        // Calculate median
        Arrays.sort(gradeValues);
        double median;
        if (gradeCount % 2 == 0) {
            median = (gradeValues[gradeCount/2 - 1] + gradeValues[gradeCount/2]) / 2.0;
        } else {
            median = gradeValues[gradeCount/2];
        }

        // Calculate mode
        double mode = calculateMode(gradeValues);

        // Calculate standard deviation
        double variance = 0;
        for (double grade : gradeValues) {
            variance += Math.pow(grade - mean, 2);
        }
        double stdDev = Math.sqrt(variance / gradeCount);

        // Calculate range
        double range = gradeValues[gradeCount - 1] - gradeValues[0];

        System.out.printf("Mean (Average):    %6.1f%%%n", mean);
        System.out.printf("Median:           %6.1f%%%n", median);
        System.out.printf("Mode:             %6.1f%%%n", mode);
        System.out.printf("Standard Deviation: %5.1f%%%n", stdDev);
        System.out.printf("Range:            %6.1f%% (%.0f%% - %.0f%%)%n",
                range, gradeValues[0], gradeValues[gradeCount - 1]);
        System.out.println();

        // Find highest and lowest grades
        displayHighestLowestGrades();
    }

    private double calculateMode(double[] grades) {
        Map<Double, Integer> frequency = new HashMap<>();
        for (double grade : grades) {
            frequency.put(grade, frequency.getOrDefault(grade, 0) + 1);
        }

        double mode = grades[0];
        int maxCount = 0;

        for (Map.Entry<Double, Integer> entry : frequency.entrySet()) {
            if (entry.getValue() > maxCount) {
                mode = entry.getKey();
                maxCount = entry.getValue();
            }
        }

        return mode;
    }

    private void displayHighestLowestGrades() {
        Grade[] grades = gradeManager.getGrades();
        int gradeCount = gradeManager.getGradeCount();

        if (gradeCount == 0) return;

        Grade highest = grades[0];
        Grade lowest = grades[0];

        for (int i = 1; i < gradeCount; i++) {
            if (grades[i].getGrade() > highest.getGrade()) {
                highest = grades[i];
            }
            if (grades[i].getGrade() < lowest.getGrade()) {
                lowest = grades[i];
            }
        }

        Student highStudent = studentManager.findStudent(highest.getStudentId());
        Student lowStudent = studentManager.findStudent(lowest.getStudentId());

        System.out.printf("Highest Grade:    %6.1f%% (%s - %s)%n",
                highest.getGrade(),
                highStudent != null ? highStudent.getStudentId() : "Unknown",
                highest.getSubject().getSubjectName());

        System.out.printf("Lowest Grade:     %6.1f%% (%s - %s)%n",
                lowest.getGrade(),
                lowStudent != null ? lowStudent.getStudentId() : "Unknown",
                lowest.getSubject().getSubjectName());
        System.out.println();
    }

    private void displaySubjectPerformance() {
        System.out.println("SUBJECT PERFORMANCE");
        System.out.println();

        Map<String, Double> subjectSums = new HashMap<>();
        Map<String, Integer> subjectCounts = new HashMap<>();

        Grade[] grades = gradeManager.getGrades();
        int gradeCount = gradeManager.getGradeCount();

        for (int i = 0; i < gradeCount; i++) {
            String subjectName = grades[i].getSubject().getSubjectName();
            subjectSums.put(subjectName, subjectSums.getOrDefault(subjectName, 0.0) + grades[i].getGrade());
            subjectCounts.put(subjectName, subjectCounts.getOrDefault(subjectName, 0) + 1);
        }

        // Core subjects
        double coreTotal = 0;
        int coreCount = 0;
        System.out.println("Core Subjects:");
        for (String subject : Arrays.asList("Mathematics", "English", "Science")) {
            if (subjectCounts.containsKey(subject)) {
                double avg = subjectSums.get(subject) / subjectCounts.get(subject);
                System.out.printf("  %-15s: %6.1f%%%n", subject + ":", avg);
                coreTotal += avg;
                coreCount++;
            }
        }
        if (coreCount > 0) {
            System.out.printf("  %-15s: %6.1f%%%n", "Average:", coreTotal / coreCount);
        }
        System.out.println();

        // Elective subjects
        double electiveTotal = 0;
        int electiveCount = 0;
        System.out.println("Elective Subjects:");
        for (String subject : Arrays.asList("Music", "Art", "Physical Education")) {
            if (subjectCounts.containsKey(subject)) {
                double avg = subjectSums.get(subject) / subjectCounts.get(subject);
                System.out.printf("  %-15s: %6.1f%%%n", subject + ":", avg);
                electiveTotal += avg;
                electiveCount++;
            }
        }
        if (electiveCount > 0) {
            System.out.printf("  %-15s: %6.1f%%%n", "Average:", electiveTotal / electiveCount);
        }
        System.out.println();
    }

    private void displayStudentTypeComparison() {
        System.out.println("STUDENT TYPE COMPARISON");
        System.out.println();

        Map<String, Double> typeSums = new HashMap<>();
        Map<String, Integer> typeCounts = new HashMap<>();

        Student[] students = studentManager.getStudents();
        int studentCount = studentManager.getStudentCountValue();

        for (int i = 0; i < studentCount; i++) {
            Student student = students[i];
            double avg = gradeManager.calculateOverallAverage(student.getStudentId());
            if (avg > 0) {
                String type = student.getStudentType();
                typeSums.put(type, typeSums.getOrDefault(type, 0.0) + avg);
                typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
            }
        }

        for (String type : typeCounts.keySet()) {
            double average = typeSums.get(type) / typeCounts.get(type);
            System.out.printf("  %-17s: %6.1f%% average (%d students)%n",
                    type + " Students:", average, typeCounts.get(type));
        }
    }
}