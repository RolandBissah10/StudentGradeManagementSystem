package models;

import java.util.*;
import java.util.stream.Collectors;

public class GradeManager {
    // Optimized collections from PDF requirements
    private Map<String, List<Grade>> studentGradesMap;     // HashMap for O(1) student grade lookup
    private Map<String, Grade> gradeByIdMap;              // HashMap for O(1) grade lookup
    private TreeMap<String, List<Grade>> gradesByDate;    // TreeMap for sorted by date
    private Map<String, List<Grade>> gradesBySubject;     // HashMap for grades by subject
    private LinkedList<Grade> gradeHistory;               // LinkedList for chronological access

    // Caching system
    private Map<String, Double> studentAveragesCache;
    private Map<String, Map<String, Double>> subjectAveragesCache;

    // Performance tracking
    private long cacheHits = 0;
    private long cacheMisses = 0;

    // StudentManager reference for GPA updates
    private StudentManager studentManager;

    public GradeManager(StudentManager studentManager) {
        studentGradesMap = new HashMap<>();
        gradeByIdMap = new HashMap<>();
        gradesByDate = new TreeMap<>(Collections.reverseOrder()); // Newest first
        gradesBySubject = new HashMap<>();
        gradeHistory = new LinkedList<>();

        studentAveragesCache = new HashMap<>();
        subjectAveragesCache = new HashMap<>();
        this.studentManager = studentManager;
    }

    /**
     * Adds a grade to all collections
     * Time Complexity: O(1) for HashMap operations, O(1) amortized for LinkedList
     * Overall: O(1)
     */
    public void addGrade(Grade grade) {
        String studentId = grade.getStudentId();
        String gradeId = grade.getGradeId();
        String date = grade.getDate();
        String subjectName = grade.getSubject().getSubjectName();

        // Add to all collections
        studentGradesMap.computeIfAbsent(studentId, k -> new ArrayList<>()).add(grade);
        gradeByIdMap.put(gradeId, grade);
        gradesByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(grade);
        gradesBySubject.computeIfAbsent(subjectName, k -> new ArrayList<>()).add(grade);
        gradeHistory.addFirst(grade); // Add to beginning for reverse chronological

        // Track course code in HashSet (prevents duplicates)
        String courseCode = grade.getSubject().getSubjectCode();
        studentManager.addCourseCode(courseCode);

        // Invalidate caches
        studentAveragesCache.remove(studentId);
        subjectAveragesCache.clear();

        // Update student GPA and honors eligibility
        updateStudentGPAAndHonors(studentId);

        System.out.println("✓ Grade added successfully!");
        displayCollectionPerformance();
    }

    /**
     * Updates student GPA and honors eligibility
     */
    private void updateStudentGPAAndHonors(String studentId) {
        double overallAvg = calculateOverallAverage(studentId);
        studentManager.updateStudentGPA(studentId, overallAvg, this);
    }

    public void viewGradesByStudent(String studentId, Student student) {
        if (student == null) {
            System.out.println("Student not found!");
            return;
        }

        System.out.println("\n=== GRADE REPORT WITH OPTIMIZED COLLECTIONS ===");
        System.out.println("Student: " + studentId + " - " + student.getName());
        System.out.println("Type: " + student.getStudentType() + " Student");

        // Use cached average if available
        double overallAvg = getCachedAverage(studentId);
        System.out.println("Current Average: " + String.format("%.1f", overallAvg) + "%");
        System.out.println("GPA: " + String.format("%.2f", student.getGpa()));
        System.out.println("Status: " + student.getStatus());

        // Show honors eligibility for honors students
        if (student instanceof HonorsStudent) {
            HonorsStudent honorsStudent = (HonorsStudent) student;
            System.out.println("Honors Eligible: " +
                    (honorsStudent.checkHonorsEligibility() ? "✓ Yes (≥85%)" : "✗ No (<85%)"));
        }

        System.out.println("\nGRADE HISTORY (Newest First)");
        System.out.println("GRD ID | DATE       | SUBJECT     | TYPE    | GRADE | PERFORMANCE");
        System.out.println("------------------------------------------------------------------");

        List<Grade> grades = studentGradesMap.getOrDefault(studentId, new ArrayList<>());

        if (grades.isEmpty()) {
            System.out.println("\nNo grades recorded for this student.");
            return;
        }

        // Display latest grades using stream
        grades.stream()
                .sorted(Comparator.comparing(Grade::getTimestamp).reversed())
                .limit(20)
                .forEach(grade -> {
                    String performance = getPerformanceIndicator(grade.getGrade());
                    System.out.printf("%-6s | %-10s | %-11s | %-7s | %5.1f%% | %s%n",
                            grade.getGradeId(),
                            grade.getDate(),
                            grade.getSubject().getSubjectName(),
                            grade.getSubject().getSubjectType(),
                            grade.getGrade(),
                            performance);
                });

        if (grades.size() > 20) {
            System.out.println("... (showing latest 20 of " + grades.size() + " grades)");
        }

        // Calculate averages using streams
        double coreAvg = calculateCoreAverageWithStream(studentId);
        double electiveAvg = calculateElectiveAverageWithStream(studentId);

        // Get grade distribution
        Map<String, Long> gradeDistribution = getGradeDistribution(studentId);

        System.out.println("\n=== PERFORMANCE METRICS ===");
        System.out.println("Total Grades: " + grades.size());
        System.out.printf("Core Subjects Average:    %6.1f%%%n", coreAvg);
        System.out.printf("Elective Subjects Average: %6.1f%%%n", electiveAvg);
        System.out.printf("Overall Average:          %6.1f%%%n", overallAvg);
        System.out.printf("Current GPA:              %6.2f%n", student.getGpa());

        System.out.println("\n=== GRADE DISTRIBUTION ===");
        gradeDistribution.forEach((range, count) ->
                System.out.printf("%-10s: %2d grades%n", range, count));

        displayCachePerformance();
    }

    private double getCachedAverage(String studentId) {
        if (studentAveragesCache.containsKey(studentId)) {
            cacheHits++;
            return studentAveragesCache.get(studentId);
        } else {
            cacheMisses++;
            double avg = calculateOverallAverage(studentId);
            studentAveragesCache.put(studentId, avg);
            return avg;
        }
    }

    private double calculateCoreAverageWithStream(String studentId) {
        List<Grade> grades = studentGradesMap.getOrDefault(studentId, new ArrayList<>());
        return grades.stream()
                .filter(g -> g.getSubject() instanceof CoreSubject)
                .mapToDouble(Grade::getGrade)
                .average()
                .orElse(0.0);
    }

    private double calculateElectiveAverageWithStream(String studentId) {
        List<Grade> grades = studentGradesMap.getOrDefault(studentId, new ArrayList<>());
        return grades.stream()
                .filter(g -> g.getSubject() instanceof ElectiveSubject)
                .mapToDouble(Grade::getGrade)
                .average()
                .orElse(0.0);
    }

    public double calculateOverallAverage(String studentId) {
        List<Grade> grades = studentGradesMap.getOrDefault(studentId, new ArrayList<>());
        if (grades.isEmpty()) return 0.0;

        return grades.stream()
                .mapToDouble(Grade::getGrade)
                .average()
                .orElse(0.0);
    }

    public double calculateCoreAverage(String studentId) {
        return calculateCoreAverageWithStream(studentId);
    }

    public double calculateElectiveAverage(String studentId) {
        return calculateElectiveAverageWithStream(studentId);
    }

    private Map<String, Long> getGradeDistribution(String studentId) {
        List<Grade> grades = studentGradesMap.getOrDefault(studentId, new ArrayList<>());
        return grades.stream()
                .collect(Collectors.groupingBy(
                        g -> getGradeRange(g.getGrade()),
                        Collectors.counting()
                ));
    }

    private String getGradeRange(double grade) {
        if (grade >= 90) return "90-100 (A)";
        if (grade >= 80) return "80-89 (B)";
        if (grade >= 70) return "70-79 (C)";
        if (grade >= 60) return "60-69 (D)";
        return "0-59 (F)";
    }

    private String getPerformanceIndicator(double grade) {
        if (grade >= 90) return "★★★★★";
        if (grade >= 80) return "★★★★☆";
        if (grade >= 70) return "★★★☆☆";
        if (grade >= 60) return "★★☆☆☆";
        return "★☆☆☆☆";
    }

    private void displayCachePerformance() {
        System.out.println("\n=== CACHE PERFORMANCE ===");
        System.out.printf("Cache Hit Rate: %.1f%% (%d/%d requests)%n",
                (cacheHits + cacheMisses) > 0 ? (cacheHits * 100.0 / (cacheHits + cacheMisses)) : 0,
                cacheHits, cacheHits + cacheMisses);
        System.out.println("Cached Students: " + studentAveragesCache.size());
    }

    private void displayCollectionPerformance() {
        System.out.println("\n=== COLLECTION PERFORMANCE ===");
        System.out.println("Data Structure            | Size | Complexity");
        System.out.println("------------------------------------------------");
        System.out.printf("HashMap<StudentID, Grades> | %4d | O(1) lookup%n",
                studentGradesMap.size());
        System.out.printf("TreeMap<Date, Grades>      | %4d | O(log n) sorted access%n",
                gradesByDate.size());
        System.out.printf("LinkedList<GradeHistory>   | %4d | O(1) add/remove%n",
                gradeHistory.size());
        System.out.printf("HashMap<Subject, Grades>   | %4d | O(1) subject grouping%n",
                gradesBySubject.size());
    }

    public List<Grade> getGradesByStudent(String studentId) {
        if ("all".equals(studentId)) {
            // Special case: return all grades from all students
            List<Grade> allGrades = new ArrayList<>();
            for (List<Grade> grades : studentGradesMap.values()) {
                allGrades.addAll(grades);
            }
            return allGrades;
        }
        return new ArrayList<>(studentGradesMap.getOrDefault(studentId, new ArrayList<>()));
    }

    public List<Grade> getGradesByDateRange(String startDate, String endDate) {
        return gradesByDate.subMap(startDate, true, endDate, true)
                .values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public List<Grade> getGradesBySubject(String subjectName) {
        return new ArrayList<>(gradesBySubject.getOrDefault(subjectName, new ArrayList<>()));
    }

    public int getTotalGradeCount() {
        return gradeHistory.size();
    }

    public int getGradeCountForStudent(String studentId) {
        return studentGradesMap.getOrDefault(studentId, new ArrayList<>()).size();
    }

    public Set<String> getUniqueCourses() {
        Set<String> courses = new HashSet<>();
        for (String subject : gradesBySubject.keySet()) {
            courses.add(subject);
        }
        return courses;
    }

    public void clearCache() {
        studentAveragesCache.clear();
        subjectAveragesCache.clear();
        cacheHits = 0;
        cacheMisses = 0;
        System.out.println("✓ All caches cleared!");
    }
}