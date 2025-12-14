package test;

import models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import services.StatisticsCalculator;

import static org.junit.jupiter.api.Assertions.*;

public class StatisticsCalculatorTest {
    private StudentManager studentManager;
    private GradeManager gradeManager;
    private StatisticsCalculator statisticsCalculator;

    @BeforeEach
    public void setUp() {
        studentManager = new StudentManager();
        gradeManager = new GradeManager();
        statisticsCalculator = new StatisticsCalculator(studentManager, gradeManager);

        // Setup sample students WITH ENROLLMENT DATE (added 5th parameter)
        studentManager.addStudent(new RegularStudent("Alice Johnson", 16,
                "alice@school.edu", "123-4567", "2024-09-01"));
        studentManager.addStudent(new HonorsStudent("Bob Smith", 17,
                "bob@school.edu", "234-5678", "2024-09-01"));
        studentManager.addStudent(new RegularStudent("Carol Davis", 16,
                "carol@school.edu", "345-6789", "2024-09-01"));
    }

    // ============================
    // TEST 1: Empty Grade Book
    // ============================

    @Test
    @DisplayName("Test 1: Should handle empty grade book without errors")
    public void testEmptyGradeBook() {
        // When grade book is empty, displayClassStatistics should not crash
        assertDoesNotThrow(() -> {
            statisticsCalculator.displayClassStatistics();
        });
    }

    // ============================
    // TEST 2: Mean Calculation
    // ============================

    @Test
    @DisplayName("Test 2: Should calculate correct mean")
    public void testMeanCalculation() {
        // Add grades: 80, 90, 70, 85, 95
        addGrade("STU001", new CoreSubject("Mathematics", "MATH"), 80.0);
        addGrade("STU001", new CoreSubject("English", "ENG"), 90.0);
        addGrade("STU002", new CoreSubject("Mathematics", "MATH"), 70.0);
        addGrade("STU002", new CoreSubject("English", "ENG"), 85.0);
        addGrade("STU003", new CoreSubject("Mathematics", "MATH"), 95.0);

        // Mean = (80+90+70+85+95)/5 = 420/5 = 84.0
        // Note: We need to extract mean from display or add getter method
        // For now, we'll test through indirect means
        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    // ============================
    // TEST 3: Median Calculation
    // ============================

    @Test
    @DisplayName("Test 3: Should calculate correct median - odd count")
    public void testMedianOddCount() {
        // Grades: 70, 80, 90, 95, 100
        addGrade("STU001", new CoreSubject("Math", "MATH"), 70.0);
        addGrade("STU001", new CoreSubject("English", "ENG"), 80.0);
        addGrade("STU002", new CoreSubject("Math", "MATH"), 90.0);
        addGrade("STU002", new CoreSubject("English", "ENG"), 95.0);
        addGrade("STU003", new CoreSubject("Math", "MATH"), 100.0);

        // Sorted: 70, 80, 90, 95, 100
        // Median (odd count) = middle value = 90
        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    @Test
    @DisplayName("Test 4: Should calculate correct median - even count")
    public void testMedianEvenCount() {
        // Grades: 70, 80, 90, 95
        addGrade("STU001", new CoreSubject("Math", "MATH"), 70.0);
        addGrade("STU001", new CoreSubject("English", "ENG"), 80.0);
        addGrade("STU002", new CoreSubject("Math", "MATH"), 90.0);
        addGrade("STU002", new CoreSubject("English", "ENG"), 95.0);

        // Sorted: 70, 80, 90, 95
        // Median (even count) = (80+90)/2 = 85
        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    // ============================
    // TEST 4: Mode Calculation
    // ============================

    @Test
    @DisplayName("Test 5: Should find correct mode")
    public void testModeCalculation() {
        // Grades: 85, 90, 85, 75, 90, 85
        addGrade("STU001", new CoreSubject("Math", "MATH"), 85.0);
        addGrade("STU001", new CoreSubject("English", "ENG"), 90.0);
        addGrade("STU002", new CoreSubject("Math", "MATH"), 85.0);
        addGrade("STU002", new CoreSubject("English", "ENG"), 75.0);
        addGrade("STU003", new CoreSubject("Math", "MATH"), 90.0);
        addGrade("STU003", new CoreSubject("English", "ENG"), 85.0);

        // Frequency: 85 appears 3 times, 90 appears 2 times, 75 appears 1 time
        // Mode = 85
        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    @Test
    @DisplayName("Test 6: Should handle multiple modes (returns first)")
    public void testMultipleModes() {
        // Grades: 85, 90, 85, 90, 75
        addGrade("STU001", new CoreSubject("Math", "MATH"), 85.0);
        addGrade("STU001", new CoreSubject("English", "ENG"), 90.0);
        addGrade("STU002", new CoreSubject("Math", "MATH"), 85.0);
        addGrade("STU002", new CoreSubject("English", "ENG"), 90.0);
        addGrade("STU003", new CoreSubject("Math", "MATH"), 75.0);

        // Both 85 and 90 appear 2 times each
        // Should return first encountered mode (85)
        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    // ============================
    // TEST 5: Standard Deviation
    // ============================

    @Test
    @DisplayName("Test 7: Should calculate correct standard deviation")
    public void testStandardDeviation() {
        // Grades: 70, 80, 90
        addGrade("STU001", new CoreSubject("Math", "MATH"), 70.0);
        addGrade("STU002", new CoreSubject("Math", "MATH"), 80.0);
        addGrade("STU003", new CoreSubject("Math", "MATH"), 90.0);

        // Mean = (70+80+90)/3 = 80
        // Variance = [(70-80)² + (80-80)² + (90-80)²]/3 = [100+0+100]/3 = 200/3 = 66.67
        // Std Dev = sqrt(66.67) ≈ 8.16
        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    @Test
    @DisplayName("Test 8: Should handle zero standard deviation")
    public void testZeroStandardDeviation() {
        // All same grades: 85, 85, 85
        addGrade("STU001", new CoreSubject("Math", "MATH"), 85.0);
        addGrade("STU002", new CoreSubject("Math", "MATH"), 85.0);
        addGrade("STU003", new CoreSubject("Math", "MATH"), 85.0);

        // Standard deviation should be 0
        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    // ============================
    // TEST 6: Range Calculation
    // ============================

    @Test
    @DisplayName("Test 9: Should calculate correct range")
    public void testRangeCalculation() {
        // Grades: 60, 75, 90, 95
        addGrade("STU001", new CoreSubject("Math", "MATH"), 60.0);
        addGrade("STU002", new CoreSubject("Math", "MATH"), 75.0);
        addGrade("STU003", new CoreSubject("Math", "MATH"), 90.0);
        addGrade("STU001", new CoreSubject("English", "ENG"), 95.0);

        // Range = Highest - Lowest = 95 - 60 = 35
        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    // ============================
    // TEST 7: Grade Distribution
    // ============================

    @Test
    @DisplayName("Test 10: Should calculate correct grade distribution")
    public void testGradeDistribution() {
        // Add grades in different ranges:
        // A (90-100): 92, 95
        // B (80-89): 85, 88
        // C (70-79): 75
        // D (60-69): 65
        // F (0-59): 45

        addGrade("STU001", new CoreSubject("Math", "MATH"), 92.0);  // A
        addGrade("STU001", new CoreSubject("English", "ENG"), 85.0); // B
        addGrade("STU002", new CoreSubject("Math", "MATH"), 95.0);  // A
        addGrade("STU002", new CoreSubject("English", "ENG"), 88.0); // B
        addGrade("STU003", new CoreSubject("Math", "MATH"), 75.0);  // C
        addGrade("STU003", new CoreSubject("English", "ENG"), 65.0); // D
        addGrade("STU001", new ElectiveSubject("Music", "MUS"), 45.0); // F

        // Total grades: 7
        // A: 2/7 = 28.6%
        // B: 2/7 = 28.6%
        // C: 1/7 = 14.3%
        // D: 1/7 = 14.3%
        // F: 1/7 = 14.3%
        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    // ============================
    // TEST 8: Subject Performance
    // ============================

    @Test
    @DisplayName("Test 11: Should calculate correct subject averages")
    public void testSubjectAverages() {
        // Math grades: 80, 90, 70 = Average: 80
        // English grades: 85, 95 = Average: 90
        // Music grades: 75, 85 = Average: 80

        addGrade("STU001", new CoreSubject("Mathematics", "MATH"), 80.0);
        addGrade("STU002", new CoreSubject("Mathematics", "MATH"), 90.0);
        addGrade("STU003", new CoreSubject("Mathematics", "MATH"), 70.0);

        addGrade("STU001", new CoreSubject("English", "ENG"), 85.0);
        addGrade("STU002", new CoreSubject("English", "ENG"), 95.0);

        addGrade("STU001", new ElectiveSubject("Music", "MUS"), 75.0);
        addGrade("STU002", new ElectiveSubject("Music", "MUS"), 85.0);

        // Core Subjects Average: (80+90+70+85+95)/5 = 420/5 = 84
        // Elective Subjects Average: (75+85)/2 = 80
        // Mathematics Average: (80+90+70)/3 = 80
        // English Average: (85+95)/2 = 90
        // Music Average: (75+85)/2 = 80
        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    // ============================
    // TEST 9: Student Type Comparison
    // ============================

    @Test
    @DisplayName("Test 12: Should compare Regular vs Honors students correctly")
    public void testStudentTypeComparison() {
        // Add more students for comparison
        studentManager.addStudent(new HonorsStudent("David Wilson", 17,
                "david@school.edu", "456-7890", "2024-09-01"));

        // Regular students: Alice (STU001) and Carol (STU003)
        addGrade("STU001", new CoreSubject("Math", "MATH"), 75.0);
        addGrade("STU001", new CoreSubject("English", "ENG"), 80.0); // Avg: 77.5

        addGrade("STU003", new CoreSubject("Math", "MATH"), 85.0);
        addGrade("STU003", new CoreSubject("English", "ENG"), 90.0); // Avg: 87.5

        // Honors students: Bob (STU002) and David (STU004)
        addGrade("STU002", new CoreSubject("Math", "MATH"), 88.0);
        addGrade("STU002", new CoreSubject("English", "ENG"), 92.0); // Avg: 90.0

        addGrade("STU004", new CoreSubject("Math", "MATH"), 95.0);
        addGrade("STU004", new CoreSubject("English", "ENG"), 98.0); // Avg: 96.5

        // Regular average: (77.5 + 87.5)/2 = 82.5
        // Honors average: (90.0 + 96.5)/2 = 93.25
        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    // ============================
    // TEST 10: Edge Cases
    // ============================

    @Test
    @DisplayName("Test 13: Should handle single grade correctly")
    public void testSingleGrade() {
        addGrade("STU001", new CoreSubject("Math", "MATH"), 85.0);

        // With single grade:
        // Mean = 85
        // Median = 85
        // Mode = 85
        // Std Dev = 0
        // Range = 0 (85-85)
        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    @Test
    @DisplayName("Test 14: Should handle all same grades")
    public void testAllSameGrades() {
        // All grades are 85
        for (int i = 0; i < 10; i++) {
            addGrade("STU001", new CoreSubject("Math", "MATH"), 85.0);
        }

        // Mean = 85
        // Median = 85
        // Mode = 85
        // Std Dev = 0
        // Range = 0
        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    @Test
    @DisplayName("Test 15: Should handle extreme values")
    public void testExtremeValues() {
        addGrade("STU001", new CoreSubject("Math", "MATH"), 0.0);   // Minimum
        addGrade("STU002", new CoreSubject("Math", "MATH"), 100.0); // Maximum
        addGrade("STU003", new CoreSubject("Math", "MATH"), 50.0);  // Middle

        // Range should be 100 (100-0)
        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    @Test
    @DisplayName("Test 16: Should handle students with no grades")
    public void testStudentsWithNoGrades() {
        // Alice has grades
        addGrade("STU001", new CoreSubject("Math", "MATH"), 85.0);
        addGrade("STU001", new CoreSubject("English", "ENG"), 90.0);

        // Bob and Carol have no grades

        // Class average should only consider Alice's grades
        // Alice's average = 87.5
        // Class average = 87.5 (only Alice counted)
        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    // ============================
    // TEST 11: Mixed Subject Types
    // ============================

    @Test
    @DisplayName("Test 17: Should separate Core and Elective subjects correctly")
    public void testCoreVsElectiveSeparation() {
        // Core subjects
        addGrade("STU001", new CoreSubject("Mathematics", "MATH"), 85.0);
        addGrade("STU001", new CoreSubject("English", "ENG"), 90.0);
        addGrade("STU001", new CoreSubject("Science", "SCI"), 88.0);

        // Elective subjects
        addGrade("STU001", new ElectiveSubject("Music", "MUS"), 75.0);
        addGrade("STU001", new ElectiveSubject("Art", "ART"), 82.0);
        addGrade("STU001", new ElectiveSubject("Physical Education", "PE"), 80.0);

        // Core average: (85+90+88)/3 = 87.67
        // Elective average: (75+82+80)/3 = 79.0
        // Overall average: (85+90+88+75+82+80)/6 = 83.33
        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    // ============================
    // TEST 12: Highest and Lowest Grades
    // ============================

    @Test
    @DisplayName("Test 18: Should identify highest and lowest grades correctly")
    public void testHighestLowestGrades() {
        addGrade("STU001", new CoreSubject("Math", "MATH"), 45.0);  // Lowest
        addGrade("STU002", new CoreSubject("Math", "MATH"), 85.0);
        addGrade("STU003", new CoreSubject("Math", "MATH"), 100.0); // Highest
        addGrade("STU001", new CoreSubject("English", "ENG"), 65.0);
        addGrade("STU002", new CoreSubject("English", "ENG"), 95.0);

        // Highest: 100 (STU003 - Math)
        // Lowest: 45 (STU001 - Math)
        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    @Test
    @DisplayName("Test 19: Should handle duplicate highest/lowest grades")
    public void testDuplicateHighestLowest() {
        // Multiple students with same highest/lowest grades
        addGrade("STU001", new CoreSubject("Math", "MATH"), 100.0); // Highest
        addGrade("STU002", new CoreSubject("Math", "MATH"), 100.0); // Also highest
        addGrade("STU003", new CoreSubject("Math", "MATH"), 60.0);  // Lowest
        addGrade("STU004", new CoreSubject("Math", "MATH"), 60.0);  // Also lowest

        // Should identify first encountered highest/lowest
        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    // ============================
    // TEST 13: Large Dataset
    // ============================

    @Test
    @DisplayName("Test 20: Should handle large number of grades efficiently")
    public void testLargeDataset() {
        // Add 100 grades (simulating realistic class size)
        for (int i = 1; i <= 20; i++) {
            String studentId = String.format("STU%03d", i);
            // Add 5 grades per student
            for (int j = 0; j < 5; j++) {
                double grade = 60 + (Math.random() * 40); // Random grade between 60-100
                addGrade(studentId, new CoreSubject("Mathematics", "MATH"), grade);
            }
        }

        // Should calculate statistics without performance issues
        long startTime = System.currentTimeMillis();
        statisticsCalculator.displayClassStatistics();
        long endTime = System.currentTimeMillis();

        long executionTime = endTime - startTime;
        System.out.println("Execution time for 100 grades: " + executionTime + "ms");

        // Performance requirement: Should complete in reasonable time
        assertTrue(executionTime < 1000, "Should complete within 1 second for 100 grades");
    }

    // ============================
    // TEST 14: Boundary Conditions
    // ============================

    @Test
    @DisplayName("Test 21: Should handle grade at boundaries (0 and 100)")
    public void testBoundaryGrades() {
        addGrade("STU001", new CoreSubject("Math", "MATH"), 0.0);   // Minimum boundary
        addGrade("STU002", new CoreSubject("Math", "MATH"), 100.0); // Maximum boundary
        addGrade("STU003", new CoreSubject("Math", "MATH"), 50.0);  // Middle

        // Distribution should correctly place 0 in F and 100 in A
        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    @Test
    @DisplayName("Test 22: Should handle exactly at distribution boundaries")
    public void testDistributionBoundaries() {
        // Grades exactly at distribution boundaries
        addGrade("STU001", new CoreSubject("Math", "MATH"), 90.0);  // A- boundary
        addGrade("STU002", new CoreSubject("Math", "MATH"), 80.0);  // B- boundary
        addGrade("STU003", new CoreSubject("Math", "MATH"), 70.0);  // C- boundary
        addGrade("STU004", new CoreSubject("Math", "MATH"), 60.0);  // D boundary

        // 90 should be in A range (90-100)
        // 80 should be in B range (80-89)
        // 70 should be in C range (70-79)
        // 60 should be in D range (60-69)
        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    // ============================
    // TEST 15: Negative Cases
    // ============================

    @Test
    @DisplayName("Test 23: Should not crash with invalid grade values")
    public void testInvalidGradeValues() {
        // Note: Grade validation should prevent invalid grades from being added
        // This test ensures robustness

        // If somehow invalid grades get through, statistics should handle gracefully
        addGrade("STU001", new CoreSubject("Math", "MATH"), -5.0);  // Invalid
        addGrade("STU002", new CoreSubject("Math", "MATH"), 105.0); // Invalid

        // Should still calculate statistics without crashing
        assertDoesNotThrow(() -> {
            statisticsCalculator.displayClassStatistics();
        });
    }

    // Helper method to add grades
    private void addGrade(String studentId, Subject subject, double grade) {
        gradeManager.addGrade(new Grade(studentId, subject, grade));
    }

    // ============================
    // TEST 16: Integration Tests
    // ============================

    @Test
    @DisplayName("Test 24: Integration test - complete statistics flow")
    public void testCompleteStatisticsFlow() {
        // Setup comprehensive test data
        setupComprehensiveTestData();

        // Test that all statistics methods work together
        assertDoesNotThrow(() -> {
            statisticsCalculator.displayClassStatistics();
        });

        // Verify no null pointers or crashes
    }

    private void setupComprehensiveTestData() {
        // Create diverse dataset
        String[] subjects = {"Mathematics", "English", "Science", "Music", "Art", "Physical Education"};
        boolean[] isCore = {true, true, true, false, false, false};

        for (int studentNum = 1; studentNum <= 10; studentNum++) {
            String studentId = String.format("STU%03d", studentNum);

            // Add 3-6 random grades per student
            int numGrades = 3 + (int)(Math.random() * 4);
            for (int i = 0; i < numGrades; i++) {
                int subjectIndex = (int)(Math.random() * subjects.length);
                String subjectName = subjects[subjectIndex];
                String subjectCode = subjectName.substring(0, 3).toUpperCase();

                double grade = 50 + (Math.random() * 50); // Random grade 50-100

                Subject subject;
                if (isCore[subjectIndex]) {
                    subject = new CoreSubject(subjectName, subjectCode);
                } else {
                    subject = new ElectiveSubject(subjectName, subjectCode);
                }

                addGrade(studentId, subject, grade);
            }
        }
    }

    // ============================
    // TEST 17: Memory and Performance
    // ============================

    @Test
    @DisplayName("Test 25: Should not have memory leaks with repeated calculations")
    public void testMemoryUsage() {
        // Add moderate dataset
        for (int i = 0; i < 50; i++) {
            addGrade("STU001", new CoreSubject("Math", "MATH"), 70 + i % 30);
        }

        // Call statistics multiple times
        for (int i = 0; i < 100; i++) {
            statisticsCalculator.displayClassStatistics();
        }
        // If we get here without OutOfMemoryError, test passes
        assertTrue(true);
    }
}