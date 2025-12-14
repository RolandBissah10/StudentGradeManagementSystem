package models;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StudentManager {
    // Optimized collections from PDF requirements
    private Map<String, Student> studentMap;                 // HashMap for O(1) lookup
    private TreeMap<Double, List<Student>> gpaRankingMap;    // TreeMap for sorted GPA rankings
    private Set<String> studentEmailSet;                     // HashSet for unique emails
    private List<Student> studentList;                       // ArrayList for insertion order
    private Map<String, List<Student>> studentsByType;       // HashMap for type grouping

    // Performance tracking
    private long totalLookups = 0;
    private long successfulLookups = 0;

    public StudentManager() {
        studentMap = new HashMap<>();
        gpaRankingMap = new TreeMap<>(Collections.reverseOrder()); // Highest GPA first
        studentEmailSet = new HashSet<>();
        studentList = new ArrayList<>();
        studentsByType = new HashMap<>();
        studentsByType.put("Regular", new ArrayList<>());
        studentsByType.put("Honors", new ArrayList<>());
    }

    public void addStudent(Student student) {
        // Check capacity (keeping your limit logic)
        if (studentList.size() >= 50) {
            System.out.println("Maximum student capacity reached!");
            return;
        }

        // Add to all collections
        studentMap.put(student.getStudentId(), student);
        studentEmailSet.add(student.getEmail());
        studentList.add(student);
        studentsByType.get(student.getStudentType()).add(student);

        System.out.println("✓ Student added successfully!");
        System.out.println("  Total students: " + studentMap.size());
        System.out.println("  Memory usage: " + calculateMemoryUsage() + " KB");
    }

    public Student findStudent(String studentId) {
        totalLookups++;
        Student student = studentMap.get(studentId);
        if (student != null) successfulLookups++;
        return student;
    }

    public Student findStudentByEmail(String email) {
        // This is O(n) but we can optimize with reverse map if needed
        for (Student student : studentList) {
            if (student.getEmail().equals(email)) {
                return student;
            }
        }
        return null;
    }

    public void updateGPARanking(String studentId, double gpa) {
        Student student = studentMap.get(studentId);
        if (student != null) {
            // Remove from old GPA position
            gpaRankingMap.values().forEach(list -> list.remove(student));

            // Add to new GPA position
            gpaRankingMap.computeIfAbsent(gpa, k -> new ArrayList<>()).add(student);
        }
    }

    public List<Student> getTopPerformers(int count) {
        List<Student> topPerformers = new ArrayList<>();
        int collected = 0;

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

    public void viewAllStudents(GradeManager gradeManager) {
        System.out.println("\n=========================================");
        System.out.println("        STUDENT LISTING (OPTIMIZED)");
        System.out.println("=========================================");
        System.out.println("Data Structure: HashMap<String, Student>");
        System.out.println("Lookup Complexity: O(1)");
        System.out.println("Total Students: " + studentMap.size());
        System.out.println("=========================================\n");

        System.out.println("STU ID | NAME           | TYPE    | EMAIL               | GPA   | STATUS");
        System.out.println("-----------------------------------------------------------------------");

        // Sort by ID for display
        List<Student> sortedStudents = new ArrayList<>(studentList);
        sortedStudents.sort(Comparator.comparing(Student::getStudentId));

        for (Student student : sortedStudents) {
            double avgGrade = gradeManager.calculateOverallAverage(student.getStudentId());
            String status = avgGrade >= student.getPassingGrade() ? "Passing" : "Failing";

            System.out.printf("%-6s | %-14s | %-7s | %-19s | %-5.1f | %s%n",
                    student.getStudentId(),
                    student.getName(),
                    student.getStudentType(),
                    truncateEmail(student.getEmail()),
                    avgGrade,
                    status);

            // Update honors eligibility
            if (student instanceof HonorsStudent) {
                HonorsStudent honorsStudent = (HonorsStudent) student;
                honorsStudent.setHonorsEligible(avgGrade >= 85.0);
            }
        }

        displayPerformanceMetrics(gradeManager);
    }

    private String truncateEmail(String email) {
        return email.length() > 19 ? email.substring(0, 16) + "..." : email;
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

    public Collection<Student> getAllStudents() {
        return new ArrayList<>(studentList);
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

    private double calculateMemoryUsage() {
        // Simplified memory estimation
        return (studentMap.size() * 100 + studentList.size() * 80) / 1024.0;
    }
}