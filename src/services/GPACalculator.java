package services;

import interfaces.Calculate;
import models.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GPACalculator implements Calculate {
    private StudentManager studentManager;
    private GradeManager gradeManager;

    // Caches for performance (US-8)
    private Map<String, Double> gpaCache;
    private Map<String, Integer> classRankCache;
    private Map<String, Map<String, Double>> subjectAveragesCache;

    // Performance tracking
    private int cacheHits = 0;
    private int cacheMisses = 0;

    public GPACalculator(StudentManager studentManager, GradeManager gradeManager) {
        this.studentManager = studentManager;
        this.gradeManager = gradeManager;

        // Initialize caches
        this.gpaCache = new ConcurrentHashMap<>();
        this.classRankCache = new ConcurrentHashMap<>();
        this.subjectAveragesCache = new ConcurrentHashMap<>();
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

    public String getDetailedLetterGrade(double percentage) {
        if (percentage >= 97) return "A+";
        else if (percentage >= 93) return "A";
        else if (percentage >= 90) return "A-";
        else if (percentage >= 87) return "B+";
        else if (percentage >= 83) return "B";
        else if (percentage >= 80) return "B-";
        else if (percentage >= 77) return "C+";
        else if (percentage >= 73) return "C";
        else if (percentage >= 70) return "C-";
        else if (percentage >= 67) return "D+";
        else if (percentage >= 63) return "D";
        else if (percentage >= 60) return "D-";
        else return "F";
    }

    @Override
    public double calculateGPA(String studentId) {
        // Check cache first
        if (gpaCache.containsKey(studentId)) {
            cacheHits++;
            return gpaCache.get(studentId);
        }

        cacheMisses++;

        // Calculate using streams
        List<Grade> grades = gradeManager.getGradesByStudent(studentId);

        if (grades.isEmpty()) {
            gpaCache.put(studentId, 0.0);
            return 0.0;
        }

        double totalGPA = grades.stream()
                .mapToDouble(grade -> convertToGPA(grade.getGrade()))
                .sum();

        double gpa = totalGPA / grades.size();
        gpaCache.put(studentId, gpa);

        return gpa;
    }

    // Overloaded method with caching control
    public double calculateGPA(String studentId, boolean useCache) {
        if (!useCache) {
            gpaCache.remove(studentId);
        }
        return calculateGPA(studentId);
    }

    @Override
    public double calculateClassRank(String studentId) {
        // Check cache first
        if (classRankCache.containsKey(studentId)) {
            cacheHits++;
            return classRankCache.get(studentId);
        }

        cacheMisses++;

        double studentGPA = calculateGPA(studentId);
        List<Student> students = studentManager.getStudents();

        long betterStudents = students.stream()
                .filter(s -> {
                    double otherGPA = calculateGPA(s.getStudentId());
                    return otherGPA > 0 && otherGPA > studentGPA;
                })
                .count();

        long totalWithGrades = students.stream()
                .filter(s -> calculateGPA(s.getStudentId()) > 0)
                .count();

        int rank = totalWithGrades > 0 ? (int) betterStudents + 1 : 1;
        classRankCache.put(studentId, rank);

        return rank;
    }

    @Override
    public Map<String, Double> calculateSubjectAverages(String studentId) {
        // Check cache first
        if (subjectAveragesCache.containsKey(studentId)) {
            cacheHits++;
            return new HashMap<>(subjectAveragesCache.get(studentId));
        }

        cacheMisses++;

        List<Grade> grades = gradeManager.getGradesByStudent(studentId);

        Map<String, Double> subjectAverages = grades.stream()
                .collect(Collectors.groupingBy(
                        grade -> grade.getSubject().getSubjectName(),
                        Collectors.averagingDouble(Grade::getGrade)
                ));

        subjectAveragesCache.put(studentId, new HashMap<>(subjectAverages));
        return subjectAverages;
    }

    // New method: Calculate weighted GPA (if credits are implemented)
    public double calculateWeightedGPA(String studentId, Map<String, Integer> creditHours) {
        List<Grade> grades = gradeManager.getGradesByStudent(studentId);

        if (grades.isEmpty()) return 0.0;

        double totalQualityPoints = 0;
        int totalCreditHours = 0;

        for (Grade grade : grades) {
            String subject = grade.getSubject().getSubjectName();
            int credits = creditHours.getOrDefault(subject, 3); // Default 3 credits
            double gpaPoints = convertToGPA(grade.getGrade());

            totalQualityPoints += gpaPoints * credits;
            totalCreditHours += credits;
        }

        return totalCreditHours > 0 ? totalQualityPoints / totalCreditHours : 0.0;
    }

    // New method: Calculate semester GPA
    public Map<String, Double> calculateSemesterGPAs(String studentId) {
        List<Grade> grades = gradeManager.getGradesByStudent(studentId);

        return grades.stream()
                .collect(Collectors.groupingBy(
                        Grade::getDate, // Assuming date represents semester
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                semesterGrades -> {
                                    double totalGPA = semesterGrades.stream()
                                            .mapToDouble(grade -> convertToGPA(grade.getGrade()))
                                            .sum();
                                    return totalGPA / semesterGrades.size();
                                }
                        )
                ));
    }

    public void displayGPABreakdown(String studentId) {
        Student student = studentManager.findStudent(studentId);
        if (student == null) {
            System.out.println("Student not found: " + studentId);
            return;
        }

        System.out.println("\n=== GPA CALCULATION (4.0 Scale) ===");
        System.out.println("Student: " + studentId + " - " + student.getName());
        System.out.println("Type: " + student.getStudentType());
        System.out.println("Enrollment Date: " + student.getEnrollmentDateString());
        System.out.println();

        System.out.println("GRADE BREAKDOWN");
        System.out.printf("%-15s | %-10s | %-8s | %-6s | %s%n",
                "SUBJECT", "TYPE", "GRADE", "GPA", "LETTER");
        System.out.println("--------------------------------------------------------");

        List<Grade> grades = gradeManager.getGradesByStudent(studentId);
        double totalGPA = 0.0;
        int count = 0;

        // Sort grades by subject name
        grades.sort(Comparator.comparing(g -> g.getSubject().getSubjectName()));

        for (Grade grade : grades) {
            double gpaPoints = convertToGPA(grade.getGrade());
            String letterGrade = getDetailedLetterGrade(grade.getGrade());
            String gradeValue = String.format("%.1f%%", grade.getGrade());

            System.out.printf("%-15s | %-10s | %-8s | %-6.2f | %s%n",
                    truncate(grade.getSubject().getSubjectName(), 15),
                    grade.getSubject().getSubjectType(),
                    gradeValue,
                    gpaPoints,
                    letterGrade);

            totalGPA += gpaPoints;
            count++;
        }

        if (count > 0) {
            double cumulativeGPA = totalGPA / count;
            double percentageAverage = gradeManager.calculateOverallAverage(studentId);
            int classRank = (int) calculateClassRank(studentId);
            int totalStudents = studentManager.getStudentCount();

            System.out.println();
            System.out.println("SUMMARY");
            System.out.println("Total Courses: " + count);
            System.out.printf("Percentage Average: %6.1f%%%n", percentageAverage);
            System.out.printf("Cumulative GPA:     %6.2f / 4.0%n", cumulativeGPA);
            System.out.printf("Letter Grade:       %6s%n", getLetterGrade(percentageAverage));
            System.out.println("Class Rank:         " + classRank + " of " + totalStudents);
            System.out.println();

            displayPerformanceAnalysis(student, cumulativeGPA, percentageAverage);
            displayCachePerformance();
        } else {
            System.out.println("\nNo grades recorded for this student.");
        }
    }

    private void displayPerformanceAnalysis(Student student, double cumulativeGPA, double percentageAverage) {
        System.out.println("PERFORMANCE ANALYSIS");

        // GPA-based analysis
        if (cumulativeGPA >= 3.7) {
            System.out.println("✓ Outstanding (Dean's List level)");
        } else if (cumulativeGPA >= 3.3) {
            System.out.println("✓ Excellent performance");
        } else if (cumulativeGPA >= 3.0) {
            System.out.println("✓ Good performance");
        } else if (cumulativeGPA >= 2.0) {
            System.out.println("✓ Satisfactory performance");
        } else {
            System.out.println("✗ Academic probation level");
        }

        // Percentage-based analysis
        if (percentageAverage >= 90) {
            System.out.println("✓ A-range performance");
        } else if (percentageAverage >= 80) {
            System.out.println("✓ B-range performance");
        } else if (percentageAverage >= 70) {
            System.out.println("✓ C-range performance");
        } else if (percentageAverage >= 60) {
            System.out.println("✓ D-range performance (minimum passing)");
        } else {
            System.out.println("✗ Failing performance");
        }

        // Honors student specific
        if (student instanceof HonorsStudent) {
            HonorsStudent honorsStudent = (HonorsStudent) student;
            boolean honorsEligible = percentageAverage >= 85.0;
            honorsStudent.setHonorsEligible(honorsEligible);

            if (honorsEligible) {
                System.out.println("✓ Honors eligibility maintained (85%+ required)");
            } else {
                System.out.println("✗ Honors eligibility not met (85%+ required)");
            }
        }

        // Class comparison
        double classAverageGPA = calculateClassAverageGPA();
        if (cumulativeGPA > classAverageGPA) {
            System.out.printf("✓ Above class average (%.2f GPA)%n", classAverageGPA);
        } else if (cumulativeGPA < classAverageGPA) {
            System.out.printf("✗ Below class average (%.2f GPA)%n", classAverageGPA);
        } else {
            System.out.printf("✓ At class average (%.2f GPA)%n", classAverageGPA);
        }
    }

    private void displayCachePerformance() {
        System.out.println("\nCACHE PERFORMANCE");
        int totalRequests = cacheHits + cacheMisses;
        if (totalRequests > 0) {
            double hitRate = (cacheHits * 100.0) / totalRequests;
            System.out.printf("Hit Rate: %.1f%% (%d/%d requests)%n", hitRate, cacheHits, totalRequests);
        }
        System.out.printf("Cached GPAs: %d students%n", gpaCache.size());
        System.out.printf("Cached Ranks: %d students%n", classRankCache.size());
    }

    public double calculateClassAverageGPA() {
        List<Student> students = studentManager.getStudents();

        return students.stream()
                .mapToDouble(s -> calculateGPA(s.getStudentId()))
                .filter(gpa -> gpa > 0)
                .average()
                .orElse(0.0);
    }

    // New method: Get GPA distribution for class
    public Map<String, Integer> getClassGPADistribution() {
        List<Student> students = studentManager.getStudents();

        return students.stream()
                .collect(Collectors.groupingBy(
                        s -> {
                            double gpa = calculateGPA(s.getStudentId());
                            if (gpa >= 3.5) return "3.5-4.0 (Excellent)";
                            else if (gpa >= 3.0) return "3.0-3.49 (Good)";
                            else if (gpa >= 2.0) return "2.0-2.99 (Satisfactory)";
                            else if (gpa > 0) return "0-1.99 (Needs Improvement)";
                            else return "No Grades";
                        },
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                List::size
                        )
                ));
    }

    // New method: Calculate trending GPA (improvement over time)
    public double calculateGPATrend(String studentId) {
        Map<String, Double> semesterGPAs = calculateSemesterGPAs(studentId);
        if (semesterGPAs.size() < 2) return 0.0;

        // Sort semesters chronologically
        List<Double> gpaList = semesterGPAs.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        // Simple trend calculation (latest - earliest)
        return gpaList.get(gpaList.size() - 1) - gpaList.get(0);
    }

    // Cache management methods
    public void clearCache() {
        gpaCache.clear();
        classRankCache.clear();
        subjectAveragesCache.clear();
        cacheHits = 0;
        cacheMisses = 0;
        System.out.println("✓ GPA calculator cache cleared");
    }

    public void warmCache() {
        System.out.println("Warming GPA cache...");
        List<Student> students = studentManager.getStudents();

        students.parallelStream().forEach(student -> {
            calculateGPA(student.getStudentId());
            calculateClassRank(student.getStudentId());
            calculateSubjectAverages(student.getStudentId());
        });

        System.out.printf("✓ Cache warmed with %d students%n", students.size());
    }

    public void displayCacheStatistics() {
        System.out.println("\n=== GPA CALCULATOR CACHE STATISTICS ===");
        System.out.printf("GPA Cache:         %d entries%n", gpaCache.size());
        System.out.printf("Rank Cache:        %d entries%n", classRankCache.size());
        System.out.printf("Subject Avg Cache: %d entries%n", subjectAveragesCache.size());

        int totalRequests = cacheHits + cacheMisses;
        if (totalRequests > 0) {
            double hitRate = (cacheHits * 100.0) / totalRequests;
            System.out.printf("Cache Hit Rate:    %.1f%%%n", hitRate);
            System.out.printf("Total Requests:    %d%n", totalRequests);
        }
    }

    // Helper method
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}