package models;

import java.util.*;

public class StudentManager {
    // Optimized collections from PDF requirements
    private Map<String, Student> studentMap;                 // HashMap for O(1) lookup
    private TreeMap<Double, List<Student>> gpaRankingMap;    // TreeMap for sorted GPA rankings (auto-sorted)
    private Set<String> studentEmailSet;                     // HashSet for unique emails
    private Set<String> courseCodeSet;                       // HashSet for unique course codes
    private List<Student> studentList;                       // ArrayList for insertion order
    private Map<String, List<Student>> studentsByType;       // HashMap for type grouping

    // Performance tracking
    private long totalLookups = 0;
    private long successfulLookups = 0;

    public StudentManager() {
        studentMap = new HashMap<>();
        gpaRankingMap = new TreeMap<>(Collections.reverseOrder()); // Highest GPA first (auto-sorted)
        studentEmailSet = new HashSet<>();
        courseCodeSet = new HashSet<>();
        studentList = new ArrayList<>();
        studentsByType = new HashMap<>();
        studentsByType.put("Regular", new ArrayList<>());
        studentsByType.put("Honors", new ArrayList<>());
    }

    /**
     * Calculates GPA from percentage grade
     * GPA Scale: 90-100 = 4.0, 80-89 = 3.0, 70-79 = 2.0, 60-69 = 1.0, <60 = 0.0
     */
    public double calculateGPA(double percentage) {
        if (percentage >= 90) return 4.0;
        if (percentage >= 80) return 3.0;
        if (percentage >= 70) return 2.0;
        if (percentage >= 60) return 1.0;
        return 0.0;
    }

    /**
     * Adds a student to all collections
     * Time Complexity: O(1) for HashMap, HashSet; O(1) amortized for ArrayList
     * Overall: O(1)
     */
    public void addStudent(Student student) {
        // Check for duplicate email using HashSet
        if (studentEmailSet.contains(student.getEmail())) {
            System.out.println("✗ ERROR: Email '" + student.getEmail() + "' already exists!");
            System.out.println("  HashSet prevents duplicate emails");
            return;
        }

        // Add to all collections
        studentMap.put(student.getStudentId(), student);
        studentEmailSet.add(student.getEmail());
        studentList.add(student);
        studentsByType.get(student.getStudentType()).add(student);

        // Initialize GPA to 0.0, will be updated when grades are added
        student.setGpa(0.0);

        // Add to GPA TreeMap with initial GPA of 0.0 (auto-sorted)
        gpaRankingMap.computeIfAbsent(0.0, k -> new ArrayList<>()).add(student);

        System.out.println("✓ Student added successfully!");
        System.out.println("  Student ID: " + student.getStudentId());
        System.out.println("  Type: " + student.getStudentType());
        System.out.println("  Total students: " + studentMap.size());
        System.out.println("  Unique emails: " + studentEmailSet.size() + " (HashSet prevents duplicates)");
    }

    /**
     * Finds student by ID using HashMap
     * Time Complexity: O(1) average case
     */
    public Student findStudent(String studentId) {
        totalLookups++;
        Student student = studentMap.get(studentId);
        if (student != null) successfulLookups++;
        return student;
    }

    /**
     * Updates GPA ranking in TreeMap (auto-sorted)
     */
    public void updateStudentGPA(String studentId, double percentage, GradeManager gradeManager) {
        Student student = studentMap.get(studentId);
        if (student != null) {
            // Calculate overall average
            double overallAvg = gradeManager.calculateOverallAverage(studentId);

            // Calculate GPA
            double gpa = calculateGPA(overallAvg);
            student.setGpa(gpa);

            // Remove from old GPA position in TreeMap
            gpaRankingMap.values().forEach(list -> list.remove(student));

            // Add to new GPA position (TreeMap auto-sorts)
            gpaRankingMap.computeIfAbsent(gpa, k -> new ArrayList<>()).add(student);

            // Update average grade
            student.setAverageGrade(overallAvg);

            // Update honors eligibility for Honors students
            if (student instanceof HonorsStudent) {
                HonorsStudent honorsStudent = (HonorsStudent) student;
                honorsStudent.setHonorsEligible(overallAvg >= 85.0);
                System.out.println("✓ Honors eligibility updated for " + studentId +
                        ": " + (overallAvg >= 85.0 ? "ELIGIBLE (≥85%)" : "NOT ELIGIBLE (<85%)"));
            }
        }
    }

    /**
     * Gets top N performers based on GPA from TreeMap
     */
    public List<Student> getTopPerformers(int count) {
        List<Student> topPerformers = new ArrayList<>();
        int collected = 0;

        // TreeMap is already sorted in reverse order (highest GPA first)
        for (Map.Entry<Double, List<Student>> entry : gpaRankingMap.entrySet()) {
            for (Student student : entry.getValue()) {
                if (collected < count) {
                    topPerformers.add(student);
                    collected++;
                } else {
                    break;
                }
            }
            if (collected >= count) break;
        }
        return topPerformers;
    }

    /**
     * Adds a course code to HashSet (prevents duplicates)
     */
    public boolean addCourseCode(String courseCode) {
        if (courseCodeSet.contains(courseCode)) {
            System.out.println("⚠️ Course code '" + courseCode + "' already exists (HashSet prevents duplicates)");
            return false;
        }
        courseCodeSet.add(courseCode);
        System.out.println("✓ Course code '" + courseCode + "' added to HashSet");
        return true;
    }

    /**
     * Gets unique course codes from HashSet
     */
    public Set<String> getUniqueCourseCodes() {
        return new HashSet<>(courseCodeSet);
    }

    public void viewAllStudents(GradeManager gradeManager) {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("        STUDENT LISTING WITH GPA & HONORS ELIGIBILITY");
        System.out.println("=".repeat(100));
        System.out.println("Data Structures:");
        System.out.println("  • HashMap<String, Student> - O(1) lookup");
        System.out.println("  • TreeMap<Double, Student> - Auto-sorted GPA rankings");
        System.out.println("  • HashSet<String> - Unique course codes");
        System.out.println("  • HashSet<String> - Unique student emails");
        System.out.println("=".repeat(100) + "\n");

        if (studentList.isEmpty()) {
            System.out.println("No students in the system.");
            return;
        }

        // Header with GPA and Honors Eligibility
        System.out.println("STU ID | NAME           | TYPE    | GPA  | AVG % | HONORS ELIGIBLE | STATUS    | EMAIL");
        System.out.println("----------------------------------------------------------------------------------------");

        // Get students sorted by GPA (highest first) using TreeMap
        List<Student> sortedByGPA = new ArrayList<>();
        for (List<Student> studentList : gpaRankingMap.values()) {
            sortedByGPA.addAll(studentList);
        }

        for (Student student : sortedByGPA) {
            double avgGrade = gradeManager.calculateOverallAverage(student.getStudentId());
            double gpa = student.getGpa();

            // Determine honors eligibility
            String honorsEligible = "N/A";
            if (student instanceof HonorsStudent) {
                HonorsStudent honorsStudent = (HonorsStudent) student;
                honorsEligible = honorsStudent.checkHonorsEligibility() ? "✓ Yes" : "✗ No";
            }

            // Determine passing status
            String status = student.getStatus();

            // Add special marker for top performers
            String gpaDisplay = String.format("%.2f", gpa);
            if (gpa >= 3.5) {
                gpaDisplay = "★" + gpaDisplay;
            }

            System.out.printf("%-6s | %-14s | %-7s | %-5s | %5.1f%% | %-14s | %-9s | %s%n",
                    student.getStudentId(),
                    student.getName(),
                    student.getStudentType(),
                    gpaDisplay,
                    avgGrade,
                    honorsEligible,
                    status,
                    truncateEmail(student.getEmail(), 20));
        }

        displayPerformanceMetrics(gradeManager);
        displayCourseCodeStatistics();
        displayGPADistribution();
    }

    private void displayCourseCodeStatistics() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("COURSE CODE STATISTICS (HashSet prevents duplicates)");
        System.out.println("=".repeat(60));
        System.out.println("Unique Course Codes: " + courseCodeSet.size());
        System.out.println("Course Codes: " + (courseCodeSet.isEmpty() ? "None" : String.join(", ", courseCodeSet)));
        System.out.println("HashSet Operations: O(1) add/lookup, prevents duplicates");
    }

    private void displayGPADistribution() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("GPA DISTRIBUTION (TreeMap auto-sorted)");
        System.out.println("=".repeat(60));

        Map<String, Integer> gpaRanges = new TreeMap<>();
        gpaRanges.put("4.0 (A)", 0);
        gpaRanges.put("3.0-3.9 (B)", 0);
        gpaRanges.put("2.0-2.9 (C)", 0);
        gpaRanges.put("1.0-1.9 (D)", 0);
        gpaRanges.put("0.0 (F)", 0);

        for (Student student : studentList) {
            double gpa = student.getGpa();
            if (gpa == 4.0) gpaRanges.put("4.0 (A)", gpaRanges.get("4.0 (A)") + 1);
            else if (gpa >= 3.0) gpaRanges.put("3.0-3.9 (B)", gpaRanges.get("3.0-3.9 (B)") + 1);
            else if (gpa >= 2.0) gpaRanges.put("2.0-2.9 (C)", gpaRanges.get("2.0-2.9 (C)") + 1);
            else if (gpa >= 1.0) gpaRanges.put("1.0-1.9 (D)", gpaRanges.get("1.0-1.9 (D)") + 1);
            else gpaRanges.put("0.0 (F)", gpaRanges.get("0.0 (F)") + 1);
        }

        gpaRanges.forEach((range, count) -> {
            if (studentList.size() > 0) {
                double percentage = (count * 100.0) / studentList.size();
                System.out.printf("%-15s: %2d students (%.1f%%)%n", range, count, percentage);
            }
        });

        // Show top 3 students
        List<Student> top3 = getTopPerformers(3);
        if (!top3.isEmpty()) {
            System.out.println("\n🏆 TOP 3 STUDENTS BY GPA:");
            for (int i = 0; i < top3.size(); i++) {
                Student s = top3.get(i);
                System.out.printf("  %d. %s - %s (GPA: %.2f, Type: %s)%n",
                        i + 1, s.getStudentId(), s.getName(), s.getGpa(), s.getStudentType());
            }
        }
    }

    private void displayPerformanceMetrics(GradeManager gradeManager) {
        System.out.println("\n=== COLLECTION PERFORMANCE METRICS ===");
        System.out.println("Data Structure        | Size | Access Time");
        System.out.println("------------------------------------------");

        // Simulate access times
        long startTime = System.nanoTime();
        studentMap.get("STU001");
        long hashMapTime = System.nanoTime() - startTime;

        startTime = System.nanoTime();
        studentList.stream().filter(s -> s.getStudentId().equals("STU001")).findFirst();
        long listTime = System.nanoTime() - startTime;

        System.out.printf("HashMap<String,Student> | %4d | %6.2f ms (O(1))%n",
                studentMap.size(), hashMapTime / 1_000_000.0);
        System.out.printf("ArrayList<Student>      | %4d | %6.2f ms (O(n))%n",
                studentList.size(), listTime / 1_000_000.0);
        System.out.printf("HashSet<Email>          | %4d | O(1) uniqueness check%n",
                studentEmailSet.size());
        System.out.printf("HashSet<CourseCode>     | %4d | O(1) duplicate prevention%n",
                courseCodeSet.size());

        System.out.printf("\nLookup Statistics: %d/%d successful (%.1f%% hit rate)%n",
                successfulLookups, totalLookups,
                totalLookups > 0 ? (successfulLookups * 100.0 / totalLookups) : 0);
    }

    public double getAverageClassGrade(GradeManager gradeManager) {
        double total = 0;
        int count = 0;

        for (Student student : studentList) {
            double avg = gradeManager.calculateOverallAverage(student.getStudentId());
            if (avg > 0) {
                total += avg;
                count++;
            }
        }

        return count > 0 ? total / count : 0.0;
    }

    public int getStudentCount() {
        return studentMap.size();
    }

    public List<Student> getStudents() {
        return new ArrayList<>(studentList);
    }

    public List<Student> getStudentsByType(String type) {
        return new ArrayList<>(studentsByType.getOrDefault(type, new ArrayList<>()));
    }

    public Map<String, Integer> getStudentTypeDistribution() {
        Map<String, Integer> distribution = new HashMap<>();
        for (Student student : studentList) {
            distribution.merge(student.getStudentType(), 1, Integer::sum);
        }
        return distribution;
    }

    public Set<String> getUniqueCourses(GradeManager gradeManager) {
        return gradeManager.getUniqueCourses();
    }

    private String truncateEmail(String email, int maxLength) {
        if (email.length() <= maxLength) return email;
        return email.substring(0, maxLength - 3) + "...";
    }

    private double calculateMemoryUsage() {
        // Simplified memory estimation
        return (studentMap.size() * 100 + studentList.size() * 80) / 1024.0;
    }
}