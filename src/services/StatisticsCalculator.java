package services;

import models.Grade;
import models.GradeManager;
import models.Student;
import models.StudentManager;

import java.util.*;
import java.util.stream.Collectors;

public class StatisticsCalculator {
    private StudentManager studentManager;
    private GradeManager gradeManager;

    public StatisticsCalculator(StudentManager studentManager, GradeManager gradeManager) {
        this.studentManager = studentManager;
        this.gradeManager = gradeManager;
    }

    public void displayClassStatistics() {
        System.out.println("\nCLASS STATISTICS WITH STREAM PROCESSING");
        System.out.println("=========================================");

        int totalStudents = studentManager.getStudentCount();
        int totalGrades = gradeManager.getTotalGradeCount();

        System.out.println("Total Students: " + totalStudents);
        System.out.println("Total Grades Recorded: " + totalGrades);
        System.out.println();

        displayGradeDistribution();
        displayStatisticalAnalysis();
        displaySubjectPerformance();
        displayStudentTypeComparison();
    }

    private void displayGradeDistribution() {
        System.out.println("GRADE DISTRIBUTION (Using Streams)");
        System.out.println();

        // Get all grades using new method
        List<Grade> allGrades = gradeManager.getGradesByStudent("all");

        // Use stream to calculate distribution
        Map<String, Long> distribution = allGrades.stream()
                .collect(Collectors.groupingBy(
                        grade -> getGradeCategory(grade.getGrade()),
                        Collectors.counting()
                ));

        // Ensure all categories exist
        String[] categories = {"90-100% (A)", "80-89% (B)", "70-79% (C)", "60-69% (D)", "0-59% (F)"};
        for (String category : categories) {
            distribution.putIfAbsent(category, 0L);
        }

        // Find maximum count for scaling
        long maxCount = distribution.values().stream()
                .max(Long::compare)
                .orElse(1L);

        // Display bar chart
        for (String category : categories) {
            long count = distribution.get(category);
            double percentage = allGrades.size() > 0 ? (count * 100.0) / allGrades.size() : 0;

            System.out.printf("%-12s: ", category);

            // Draw bar chart
            if (maxCount > 0) {
                int barLength = (int) Math.round((count * 40.0) / maxCount);
                System.out.print("█".repeat(barLength));
            }

            System.out.printf(" %5.1f%% (%d grades)%n", percentage, count);
        }
        System.out.println();
    }

    private String getGradeCategory(double grade) {
        if (grade >= 90) return "90-100% (A)";
        else if (grade >= 80) return "80-89% (B)";
        else if (grade >= 70) return "70-79% (C)";
        else if (grade >= 60) return "60-69% (D)";
        else return "0-59% (F)";
    }

    private void displayStatisticalAnalysis() {
        System.out.println("STATISTICAL ANALYSIS (Using Streams)");
        System.out.println();

        List<Grade> allGrades = gradeManager.getGradesByStudent("all");

        if (allGrades.isEmpty()) {
            System.out.println("No grades available for analysis.");
            return;
        }

        // Calculate statistics using streams
        DoubleSummaryStatistics stats = allGrades.stream()
                .mapToDouble(Grade::getGrade)
                .summaryStatistics();

        double mean = stats.getAverage();
        long gradeCount = stats.getCount();

        // Calculate median
        List<Double> sortedGrades = allGrades.stream()
                .map(Grade::getGrade)
                .sorted()
                .collect(Collectors.toList());

        double median;
        if (gradeCount % 2 == 0) {
            median = (sortedGrades.get((int)gradeCount/2 - 1) + sortedGrades.get((int)gradeCount/2)) / 2.0;
        } else {
            median = sortedGrades.get((int)gradeCount/2);
        }

        // Calculate mode using stream
        double mode = allGrades.stream()
                .collect(Collectors.groupingBy(Grade::getGrade, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0.0);

        // Calculate standard deviation
        double variance = allGrades.stream()
                .mapToDouble(grade -> Math.pow(grade.getGrade() - mean, 2))
                .average()
                .orElse(0.0);
        double stdDev = Math.sqrt(variance);

        double range = stats.getMax() - stats.getMin();

        System.out.printf("Mean (Average):    %6.1f%%%n", mean);
        System.out.printf("Median:           %6.1f%%%n", median);
        System.out.printf("Mode:             %6.1f%%%n", mode);
        System.out.printf("Standard Deviation: %5.1f%%%n", stdDev);
        System.out.printf("Range:            %6.1f%% (%.0f%% - %.0f%%)%n",
                range, stats.getMin(), stats.getMax());
        System.out.println();

        // Find highest and lowest grades
        displayHighestLowestGrades();
    }

    private void displayHighestLowestGrades() {
        List<Grade> allGrades = gradeManager.getGradesByStudent("all");

        if (allGrades.isEmpty()) return;

        // Find highest and lowest using streams
        Grade highest = allGrades.stream()
                .max(Comparator.comparingDouble(Grade::getGrade))
                .orElse(null);

        Grade lowest = allGrades.stream()
                .min(Comparator.comparingDouble(Grade::getGrade))
                .orElse(null);

        if (highest != null) {
            Student highStudent = studentManager.findStudent(highest.getStudentId());
            System.out.printf("Highest Grade:    %6.1f%% (%s - %s)%n",
                    highest.getGrade(),
                    highStudent != null ? highStudent.getStudentId() : "Unknown",
                    highest.getSubject().getSubjectName());
        }

        if (lowest != null) {
            Student lowStudent = studentManager.findStudent(lowest.getStudentId());
            System.out.printf("Lowest Grade:     %6.1f%% (%s - %s)%n",
                    lowest.getGrade(),
                    lowStudent != null ? lowStudent.getStudentId() : "Unknown",
                    lowest.getSubject().getSubjectName());
        }
        System.out.println();
    }

    private void displaySubjectPerformance() {
        System.out.println("SUBJECT PERFORMANCE (Using Streams)");
        System.out.println();

        List<Grade> allGrades = gradeManager.getGradesByStudent("all");

        // Calculate subject averages using streams
        Map<String, Double> subjectAverages = allGrades.stream()
                .collect(Collectors.groupingBy(
                        grade -> grade.getSubject().getSubjectName(),
                        Collectors.averagingDouble(Grade::getGrade)
                ));

        // Separate core and elective subjects
        List<String> coreSubjects = Arrays.asList("Mathematics", "English", "Science");
        List<String> electiveSubjects = Arrays.asList("Music", "Art", "Physical Education");

        // Core subjects
        System.out.println("Core Subjects:");
        double coreTotal = 0;
        int coreCount = 0;

        for (String subject : coreSubjects) {
            if (subjectAverages.containsKey(subject)) {
                double avg = subjectAverages.get(subject);
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
        System.out.println("Elective Subjects:");
        double electiveTotal = 0;
        int electiveCount = 0;

        for (String subject : electiveSubjects) {
            if (subjectAverages.containsKey(subject)) {
                double avg = subjectAverages.get(subject);
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
        System.out.println("STUDENT TYPE COMPARISON (Using Streams)");
        System.out.println();

        // Get all students
        List<Student> allStudents = studentManager.getStudents();

        // Calculate averages by student type using streams
        Map<String, List<Double>> gradesByType = allStudents.stream()
                .collect(Collectors.groupingBy(
                        Student::getStudentType,
                        Collectors.mapping(
                                student -> gradeManager.calculateOverallAverage(student.getStudentId()),
                                Collectors.filtering(avg -> avg > 0, Collectors.toList())
                        )
                ));

        // Display results
        for (Map.Entry<String, List<Double>> entry : gradesByType.entrySet()) {
            String type = entry.getKey();
            List<Double> averages = entry.getValue();

            if (!averages.isEmpty()) {
                double average = averages.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);

                System.out.printf("  %-17s: %6.1f%% average (%d students)%n",
                        type + " Students:", average, averages.size());
            }
        }
        System.out.println();
    }

    // New method for real-time statistics (US-5)
    public Map<String, Object> getRealTimeStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<Student> allStudents = studentManager.getStudents();
        List<Grade> allGrades = gradeManager.getGradesByStudent("all");

        // Basic counts
        stats.put("totalStudents", allStudents.size());
        stats.put("totalGrades", allGrades.size());
        stats.put("timestamp", new Date());

        // Grade distribution
        Map<String, Long> distribution = allGrades.stream()
                .collect(Collectors.groupingBy(
                        grade -> getGradeCategory(grade.getGrade()),
                        Collectors.counting()
                ));
        stats.put("gradeDistribution", distribution);

        // Average grade
        double averageGrade = allGrades.stream()
                .mapToDouble(Grade::getGrade)
                .average()
                .orElse(0.0);
        stats.put("averageGrade", averageGrade);

        // Top performers
        List<Student> topPerformers = allStudents.stream()
                .sorted((s1, s2) -> {
                    double avg1 = gradeManager.calculateOverallAverage(s1.getStudentId());
                    double avg2 = gradeManager.calculateOverallAverage(s2.getStudentId());
                    return Double.compare(avg2, avg1); // Descending
                })
                .limit(5)
                .collect(Collectors.toList());
        stats.put("topPerformers", topPerformers);

        // Student type distribution
        Map<String, Long> typeDistribution = allStudents.stream()
                .collect(Collectors.groupingBy(
                        Student::getStudentType,
                        Collectors.counting()
                ));
        stats.put("studentTypeDistribution", typeDistribution);

        return stats;
    }

    // New method for performance metrics (US-10)
    public void displayPerformanceMetrics() {
        System.out.println("\n=== PERFORMANCE METRICS ===");

        List<Student> students = studentManager.getStudents();
        List<Grade> grades = gradeManager.getGradesByStudent("all");

        System.out.println("Processing " + students.size() + " students and " +
                grades.size() + " grades...");

        // Benchmark sequential processing
        long startTime = System.nanoTime();

        double sequentialAverage = grades.stream()
                .mapToDouble(Grade::getGrade)
                .average()
                .orElse(0.0);

        long sequentialTime = System.nanoTime() - startTime;

        // Benchmark parallel processing
        startTime = System.nanoTime();

        double parallelAverage = grades.parallelStream()
                .mapToDouble(Grade::getGrade)
                .average()
                .orElse(0.0);

        long parallelTime = System.nanoTime() - startTime;

        System.out.printf("Sequential processing: %.2f ms%n", sequentialTime / 1_000_000.0);
        System.out.printf("Parallel processing:   %.2f ms%n", parallelTime / 1_000_000.0);
        System.out.printf("Speedup: %.1fx faster%n",
                (double) sequentialTime / parallelTime);
        System.out.println("Results identical: " +
                (Math.abs(sequentialAverage - parallelAverage) < 0.001));
    }
}