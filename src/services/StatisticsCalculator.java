package services;

import models.*;
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

        // Add GPA Rankings as part of statistics
        System.out.println("\n" + "=".repeat(100));
        System.out.println("ADDITIONAL ANALYSIS: GPA RANKINGS");
        System.out.println("=".repeat(100));
        viewGPARankings();  // Call the GPA rankings method
    }

    // NEW METHOD: Add GPA Rankings to statistics
    public void viewGPARankings() {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("                  GPA RANKINGS (TreeMap Auto-Sorted)");
        System.out.println("=".repeat(100));
        System.out.println("📊 TreeMap automatically maintains sorted order by GPA (highest first)");
        System.out.println("📈 GPA Scale: 90-100% = 4.0 | 80-89% = 3.0 | 70-79% = 2.0 | 60-69% = 1.0 | <60% = 0.0");
        System.out.println("🏆 Honors Eligibility: Requires ≥85% average grade");
        System.out.println("✓ Passing: ≥60% for Honors students, ≥50% for Regular students");
        System.out.println("=".repeat(100));

        // Get all students sorted by GPA using the existing TreeMap
        List<Student> allStudents = studentManager.getStudents();

        if (allStudents.isEmpty()) {
            System.out.println("\n⚠️ No students available for GPA rankings.");
            System.out.println("  Add students and grades first to calculate GPAs.");
            return;
        }

        // Sort students by GPA (highest first)
        List<Student> sortedByGPA = allStudents.stream()
                .sorted((s1, s2) -> Double.compare(s2.getGpa(), s1.getGpa()))
                .collect(Collectors.toList());

        System.out.println("\n🏆 GPA RANKINGS:");
        System.out.println("RANK | STU ID   | NAME           | GPA  | AVG %  | HONORS ELIGIBLE | TYPE    | STATUS");
        System.out.println("---------------------------------------------------------------------------------------");

        for (int i = 0; i < sortedByGPA.size(); i++) {
            Student student = sortedByGPA.get(i);
            double avgGrade = gradeManager.calculateOverallAverage(student.getStudentId());
            double gpa = student.getGpa();

            // Determine honors eligibility
            String honorsEligible = "N/A";
            if (student instanceof HonorsStudent) {
                HonorsStudent honorsStudent = (HonorsStudent) student;
                honorsEligible = honorsStudent.checkHonorsEligibility() ? "✓ Yes" : "✗ No";
            }

            // Determine status (use getStatus() method)
            String status = getStudentStatus(student);

            // Add ranking symbols
            String rankSymbol;
            if (i == 0) rankSymbol = "🥇 1st";
            else if (i == 1) rankSymbol = "🥈 2nd";
            else if (i == 2) rankSymbol = "🥉 3rd";
            else rankSymbol = String.format("%2d.", i + 1);

            System.out.printf("%-6s | %-8s | %-14s | %4.2f | %6.1f%% | %-14s | %-7s | %s%n",
                    rankSymbol,
                    student.getStudentId(),
                    student.getName(),
                    gpa,
                    avgGrade,
                    honorsEligible,
                    student.getStudentType(),
                    status);
        }

        // Display GPA distribution
        displayGPADistributionAnalysis();
        displayHonorsStatistics();
    }

    private String getStudentStatus(Student student) {
        double avgGrade = gradeManager.calculateOverallAverage(student.getStudentId());

        if (student instanceof HonorsStudent) {
            HonorsStudent honorsStudent = (HonorsStudent) student;
            boolean isPassing = avgGrade >= honorsStudent.getPassingGrade();
            boolean isHonorsEligible = honorsStudent.checkHonorsEligibility();

            if (isHonorsEligible && isPassing) {
                return "PASSING WITH HONORS";
            } else if (isPassing) {
                return "PASSING";
            } else {
                return "FAILING";
            }
        } else {
            // Regular student
            return avgGrade >= student.getPassingGrade() ? "PASSING" : "FAILING";
        }
    }

    private void displayGPADistributionAnalysis() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("GPA DISTRIBUTION ANALYSIS");
        System.out.println("=".repeat(60));

        List<Student> allStudents = studentManager.getStudents();
        Map<String, Integer> gpaDistribution = new LinkedHashMap<>();
        gpaDistribution.put("4.0 (A - Excellent)", 0);
        gpaDistribution.put("3.0-3.9 (B - Good)", 0);
        gpaDistribution.put("2.0-2.9 (C - Average)", 0);
        gpaDistribution.put("1.0-1.9 (D - Below Average)", 0);
        gpaDistribution.put("0.0 (F - Failing)", 0);

        for (Student student : allStudents) {
            double gpa = student.getGpa();
            if (gpa == 4.0) gpaDistribution.put("4.0 (A - Excellent)", gpaDistribution.get("4.0 (A - Excellent)") + 1);
            else if (gpa >= 3.0) gpaDistribution.put("3.0-3.9 (B - Good)", gpaDistribution.get("3.0-3.9 (B - Good)") + 1);
            else if (gpa >= 2.0) gpaDistribution.put("2.0-2.9 (C - Average)", gpaDistribution.get("2.0-2.9 (C - Average)") + 1);
            else if (gpa >= 1.0) gpaDistribution.put("1.0-1.9 (D - Below Average)", gpaDistribution.get("1.0-1.9 (D - Below Average)") + 1);
            else gpaDistribution.put("0.0 (F - Failing)", gpaDistribution.get("0.0 (F - Failing)") + 1);
        }

        System.out.println("GPA RANGE          | COUNT | PERCENTAGE");
        System.out.println("-----------------------------------------");
        gpaDistribution.forEach((range, count) -> {
            double percentage = allStudents.size() > 0 ? (count * 100.0) / allStudents.size() : 0;
            System.out.printf("%-20s | %5d | %6.1f%%%n", range, count, percentage);
        });

        // Calculate average GPA
        double totalGPA = allStudents.stream().mapToDouble(Student::getGpa).sum();
        double averageGPA = allStudents.size() > 0 ? totalGPA / allStudents.size() : 0.0;
        System.out.printf("\n📊 Average Class GPA: %.2f%n", averageGPA);
    }

    private void displayHonorsStatistics() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("HONORS STUDENT STATISTICS");
        System.out.println("=".repeat(60));

        List<Student> allStudents = studentManager.getStudents();

        // Count honors eligible students
        long honorsEligibleCount = allStudents.stream()
                .filter(s -> s instanceof HonorsStudent)
                .filter(s -> ((HonorsStudent) s).checkHonorsEligibility())
                .count();

        long honorsStudentsCount = allStudents.stream()
                .filter(s -> s instanceof HonorsStudent)
                .count();

        long regularStudentsCount = allStudents.stream()
                .filter(s -> !(s instanceof HonorsStudent))
                .count();

        if (honorsStudentsCount > 0) {
            double honorsPercentage = (honorsEligibleCount * 100.0) / honorsStudentsCount;
            System.out.printf("Honors Students:          %d/%d (%.1f%%)%n",
                    honorsEligibleCount, honorsStudentsCount, honorsPercentage);
            System.out.printf("  Eligible (≥85%%):        %d students%n", honorsEligibleCount);
            System.out.printf("  Not Eligible (<85%%):    %d students%n", honorsStudentsCount - honorsEligibleCount);
        }

        System.out.printf("Regular Students:        %d students%n", regularStudentsCount);
        System.out.printf("Total Students:          %d students%n", allStudents.size());

        // Top 5 GPA students
        System.out.println("\n🏆 TOP 5 STUDENTS BY GPA:");
        List<Student> top5 = allStudents.stream()
                .sorted((s1, s2) -> Double.compare(s2.getGpa(), s1.getGpa()))
                .limit(5)
                .collect(Collectors.toList());

        for (int i = 0; i < top5.size(); i++) {
            Student s = top5.get(i);
            System.out.printf("  %d. %s - %s (GPA: %.2f, Type: %s)%n",
                    i + 1, s.getStudentId(), s.getName(), s.getGpa(), s.getStudentType());
        }
    }

    // Rest of your existing methods remain unchanged...
    public void displayGradeDistribution() {
        System.out.println("GRADE DISTRIBUTION (Using Streams)");
        System.out.println();

        List<Grade> allGrades = gradeManager.getGradesByStudent("all");

        Map<String, Long> distribution = allGrades.stream()
                .collect(Collectors.groupingBy(
                        grade -> getGradeCategory(grade.getGrade()),
                        Collectors.counting()
                ));

        String[] categories = {"90-100% (A)", "80-89% (B)", "70-79% (C)", "60-69% (D)", "0-59% (F)"};
        for (String category : categories) {
            distribution.putIfAbsent(category, 0L);
        }

        long maxCount = distribution.values().stream()
                .max(Long::compare)
                .orElse(1L);

        for (String category : categories) {
            long count = distribution.get(category);
            double percentage = allGrades.size() > 0 ? (count * 100.0) / allGrades.size() : 0;

            System.out.printf("%-12s: ", category);

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

    public void displayStatisticalAnalysis() {
        System.out.println("STATISTICAL ANALYSIS (Using Streams)");
        System.out.println();

        List<Grade> allGrades = gradeManager.getGradesByStudent("all");

        if (allGrades.isEmpty()) {
            System.out.println("No grades available for analysis.");
            return;
        }

        DoubleSummaryStatistics stats = allGrades.stream()
                .mapToDouble(Grade::getGrade)
                .summaryStatistics();

        double mean = stats.getAverage();
        long gradeCount = stats.getCount();

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

        double mode = allGrades.stream()
                .collect(Collectors.groupingBy(Grade::getGrade, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0.0);

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

        displayHighestLowestGrades();
    }

    private void displayHighestLowestGrades() {
        List<Grade> allGrades = gradeManager.getGradesByStudent("all");

        if (allGrades.isEmpty()) return;

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

    public void displaySubjectPerformance() {
        System.out.println("SUBJECT PERFORMANCE (Using Streams)");
        System.out.println();

        List<Grade> allGrades = gradeManager.getGradesByStudent("all");

        Map<String, Double> subjectAverages = allGrades.stream()
                .collect(Collectors.groupingBy(
                        grade -> grade.getSubject().getSubjectName(),
                        Collectors.averagingDouble(Grade::getGrade)
                ));

        List<String> coreSubjects = Arrays.asList("Mathematics", "English", "Science");
        List<String> electiveSubjects = Arrays.asList("Music", "Art", "Physical Education");

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

    public void displayStudentTypeComparison() {
        System.out.println("STUDENT TYPE COMPARISON (Using Streams)");
        System.out.println();

        List<Student> allStudents = studentManager.getStudents();

        Map<String, List<Double>> gradesByType = allStudents.stream()
                .collect(Collectors.groupingBy(
                        Student::getStudentType,
                        Collectors.mapping(
                                student -> gradeManager.calculateOverallAverage(student.getStudentId()),
                                Collectors.filtering(avg -> avg > 0, Collectors.toList())
                        )
                ));

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

    public Map<String, Object> getRealTimeStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<Student> allStudents = studentManager.getStudents();
        List<Grade> allGrades = gradeManager.getGradesByStudent("all");

        stats.put("totalStudents", allStudents.size());
        stats.put("totalGrades", allGrades.size());
        stats.put("timestamp", new Date());

        Map<String, Long> distribution = allGrades.stream()
                .collect(Collectors.groupingBy(
                        grade -> getGradeCategory(grade.getGrade()),
                        Collectors.counting()
                ));
        stats.put("gradeDistribution", distribution);

        double averageGrade = allGrades.stream()
                .mapToDouble(Grade::getGrade)
                .average()
                .orElse(0.0);
        stats.put("averageGrade", averageGrade);

        List<Student> topPerformers = allStudents.stream()
                .sorted((s1, s2) -> {
                    double avg1 = gradeManager.calculateOverallAverage(s1.getStudentId());
                    double avg2 = gradeManager.calculateOverallAverage(s2.getStudentId());
                    return Double.compare(avg2, avg1);
                })
                .limit(5)
                .collect(Collectors.toList());
        stats.put("topPerformers", topPerformers);

        Map<String, Long> typeDistribution = allStudents.stream()
                .collect(Collectors.groupingBy(
                        Student::getStudentType,
                        Collectors.counting()
                ));
        stats.put("studentTypeDistribution", typeDistribution);

        return stats;
    }

    public void displayPerformanceMetrics() {
        System.out.println("\n=== PERFORMANCE METRICS ===");

        List<Student> students = studentManager.getStudents();
        List<Grade> grades = gradeManager.getGradesByStudent("all");

        System.out.println("Processing " + students.size() + " students and " +
                grades.size() + " grades...");

        long startTime = System.nanoTime();

        double sequentialAverage = grades.stream()
                .mapToDouble(Grade::getGrade)
                .average()
                .orElse(0.0);

        long sequentialTime = System.nanoTime() - startTime;

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