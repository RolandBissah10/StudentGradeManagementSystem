package services;

import models.*;
import java.util.*;
import java.util.stream.*;
import java.util.function.*;
import java.util.concurrent.atomic.AtomicInteger;

public class StreamProcessor {
    private StudentManager studentManager;
    private GradeManager gradeManager;

    public StreamProcessor(StudentManager studentManager, GradeManager gradeManager) {
        this.studentManager = studentManager;
        this.gradeManager = gradeManager;
    }

    // US-10: Stream processing methods

    public Map<String, Double> calculateAverageBySubject() {
        return gradeManager.getGradesByStudent("all").stream()
                .collect(Collectors.groupingBy(
                        grade -> grade.getSubject().getSubjectName(),
                        Collectors.averagingDouble(Grade::getGrade)
                ));
    }

    public Map<String, Long> countGradesByLetterGrade() {
        return gradeManager.getGradesByStudent("all").stream()
                .collect(Collectors.groupingBy(
                        Grade::getLetterGrade,
                        Collectors.counting()
                ));
    }

    public List<Student> getTopStudents(int count, String sortBy) {
        return studentManager.getStudents().stream()
                .sorted((s1, s2) -> {
                    double avg1 = gradeManager.calculateOverallAverage(s1.getStudentId());
                    double avg2 = gradeManager.calculateOverallAverage(s2.getStudentId());
                    return Double.compare(avg2, avg1); // Descending
                })
                .limit(count)
                .collect(Collectors.toList());
    }

    public Map<String, List<Grade>> getGradesByPerformanceCategory() {
        return gradeManager.getGradesByStudent("all").stream()
                .collect(Collectors.groupingBy(grade -> {
                    double g = grade.getGrade();
                    if (g >= 90) return "Excellent";
                    else if (g >= 80) return "Good";
                    else if (g >= 70) return "Average";
                    else if (g >= 60) return "Below Average";
                    else return "Poor";
                }));
    }

    public double calculateOverallClassAverage() {
        return studentManager.getStudents().stream()
                .mapToDouble(s -> gradeManager.calculateOverallAverage(s.getStudentId()))
                .filter(avg -> avg > 0)
                .average()
                .orElse(0.0);
    }

    public Map<String, Double> calculateStandardDeviationBySubject() {
        Map<String, List<Double>> gradesBySubject = gradeManager.getGradesByStudent("all").stream()
                .collect(Collectors.groupingBy(
                        grade -> grade.getSubject().getSubjectName(),
                        Collectors.mapping(Grade::getGrade, Collectors.toList())
                ));

        return gradesBySubject.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> calculateStandardDeviation(entry.getValue())
                ));
    }

    private double calculateStandardDeviation(List<Double> values) {
        if (values.isEmpty()) return 0.0;

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);

        return Math.sqrt(variance);
    }

    public List<Map<String, Object>> generateStudentPerformanceReport() {
        return studentManager.getStudents().stream()
                .map(student -> {
                    Map<String, Object> report = new HashMap<>();
                    report.put("studentId", student.getStudentId());
                    report.put("name", student.getName());
                    report.put("type", student.getStudentType());

                    double overallAvg = gradeManager.calculateOverallAverage(student.getStudentId());
                    report.put("overallAverage", overallAvg);
                    report.put("letterGrade", getLetterGrade(overallAvg));

                    List<Grade> grades = gradeManager.getGradesByStudent(student.getStudentId());
                    report.put("totalGrades", grades.size());

                    long coreCount = grades.stream()
                            .filter(g -> g.getSubject() instanceof CoreSubject)
                            .count();
                    report.put("coreSubjectsCount", coreCount);

                    long electiveCount = grades.stream()
                            .filter(g -> g.getSubject() instanceof ElectiveSubject)
                            .count();
                    report.put("electiveSubjectsCount", electiveCount);

                    boolean isPassing = overallAvg >= student.getPassingGrade();
                    report.put("passingStatus", isPassing ? "PASSING" : "FAILING");

                    if (student instanceof HonorsStudent) {
                        boolean honorsEligible = overallAvg >= 85.0;
                        report.put("honorsEligible", honorsEligible);
                    }

                    return report;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> analyzeGradeDistribution() {
        List<Grade> allGrades = gradeManager.getGradesByStudent("all");

        Map<String, Object> analysis = new HashMap<>();
        analysis.put("totalGrades", allGrades.size());

        DoubleSummaryStatistics stats = allGrades.stream()
                .mapToDouble(Grade::getGrade)
                .summaryStatistics();

        analysis.put("min", stats.getMin());
        analysis.put("max", stats.getMax());
        analysis.put("average", stats.getAverage());
        analysis.put("sum", stats.getSum());

        // Grade distribution
        Map<String, Long> distribution = allGrades.stream()
                .collect(Collectors.groupingBy(
                        grade -> {
                            double g = grade.getGrade();
                            if (g >= 90) return "A (90-100)";
                            else if (g >= 80) return "B (80-89)";
                            else if (g >= 70) return "C (70-79)";
                            else if (g >= 60) return "D (60-69)";
                            else return "F (0-59)";
                        },
                        Collectors.counting()
                ));
        analysis.put("distribution", distribution);

        // Subject performance
        Map<String, Double> subjectAverages = allGrades.stream()
                .collect(Collectors.groupingBy(
                        grade -> grade.getSubject().getSubjectName(),
                        Collectors.averagingDouble(Grade::getGrade)
                ));
        analysis.put("subjectAverages", subjectAverages);

        return analysis;
    }

    public List<Student> filterStudents(Predicate<Student> predicate) {
        return studentManager.getStudents().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    public List<Grade> filterGrades(Predicate<Grade> predicate) {
        return gradeManager.getGradesByStudent("all").stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    public <T> List<T> transformStudents(Function<Student, T> transformer) {
        return studentManager.getStudents().stream()
                .map(transformer)
                .collect(Collectors.toList());
    }

    public <T> List<T> transformGrades(Function<Grade, T> transformer) {
        return gradeManager.getGradesByStudent("all").stream()
                .map(transformer)
                .collect(Collectors.toList());
    }

    // Parallel stream processing methods

    public Map<String, Double> calculateAverageBySubjectParallel() {
        return gradeManager.getGradesByStudent("all").parallelStream()
                .collect(Collectors.groupingByConcurrent(
                        grade -> grade.getSubject().getSubjectName(),
                        Collectors.averagingDouble(Grade::getGrade)
                ));
    }

    public List<Student> processInParallel(int batchSize, Consumer<List<Student>> processor) {
        List<Student> students = studentManager.getStudents();
        AtomicInteger counter = new AtomicInteger(0);

        return IntStream.range(0, (students.size() + batchSize - 1) / batchSize)
                .parallel()
                .mapToObj(i -> students.subList(
                        i * batchSize,
                        Math.min(students.size(), (i + 1) * batchSize)
                ))
                .peek(batch -> {
                    processor.accept(batch);
                    counter.addAndGet(batch.size());
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public void benchmarkStreamPerformance() {
        List<Grade> grades = gradeManager.getGradesByStudent("all");

        System.out.println("\n=== STREAM PROCESSING BENCHMARK ===");
        System.out.println("Dataset size: " + grades.size() + " grades");

        // Sequential benchmark
        long startTime = System.nanoTime();
        Map<String, Double> seqResult = grades.stream()
                .collect(Collectors.groupingBy(
                        grade -> grade.getSubject().getSubjectName(),
                        Collectors.averagingDouble(Grade::getGrade)
                ));
        long seqTime = System.nanoTime() - startTime;

        // Parallel benchmark
        startTime = System.nanoTime();
        Map<String, Double> parResult = grades.parallelStream()
                .collect(Collectors.groupingByConcurrent(
                        grade -> grade.getSubject().getSubjectName(),
                        Collectors.averagingDouble(Grade::getGrade)
                ));
        long parTime = System.nanoTime() - startTime;

        System.out.printf("Sequential processing: %8.2f ms%n", seqTime / 1_000_000.0);
        System.out.printf("Parallel processing:   %8.2f ms%n", parTime / 1_000_000.0);
        System.out.printf("Speedup factor:       %8.2fx%n", (double) seqTime / parTime);
        System.out.println("Results identical: " + seqResult.equals(parResult));

        // Memory comparison (estimated)
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

        grades.stream()
                .collect(Collectors.groupingBy(
                        grade -> grade.getSubject().getSubjectName(),
                        Collectors.averagingDouble(Grade::getGrade)
                ));

        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long seqMemory = afterMemory - beforeMemory;

        runtime.gc();
        beforeMemory = runtime.totalMemory() - runtime.freeMemory();

        grades.parallelStream()
                .collect(Collectors.groupingByConcurrent(
                        grade -> grade.getSubject().getSubjectName(),
                        Collectors.averagingDouble(Grade::getGrade)
                ));

        afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long parMemory = afterMemory - beforeMemory;

        System.out.printf("Sequential memory:    %8.2f KB%n", seqMemory / 1024.0);
        System.out.printf("Parallel memory:      %8.2f KB%n", parMemory / 1024.0);
        System.out.printf("Memory overhead:      %8.2f%%%n",
                (parMemory - seqMemory) * 100.0 / seqMemory);
    }

    private String getLetterGrade(double percentage) {
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

    public void displayStreamCapabilities() {
        System.out.println("\n=== STREAM PROCESSING CAPABILITIES ===");
        System.out.println("Available Operations:");
        System.out.println("1. Filter - Filter students/grades based on predicates");
        System.out.println("2. Map - Transform data elements");
        System.out.println("3. Reduce - Aggregate values (sum, average, etc.)");
        System.out.println("4. Collect - Group data into collections");
        System.out.println("5. Sort - Sort data based on criteria");
        System.out.println("6. Parallel Processing - Process data concurrently");
        System.out.println("7. Statistical Analysis - Min, max, average, distribution");
        System.out.println("8. Performance Benchmarking - Compare sequential vs parallel");

        System.out.println("\nExample Use Cases:");
        System.out.println("- Calculate subject averages");
        System.out.println("- Generate grade distribution");
        System.out.println("- Find top performing students");
        System.out.println("- Filter students by criteria");
        System.out.println("- Transform student data for reports");
        System.out.println("- Process large datasets in parallel");
    }
}