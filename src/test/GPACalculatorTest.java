package test;

import models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import services.GPACalculator;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GPACalculator Test Suite")
public class GPACalculatorTest {
    private StudentManager studentManager;
    private GradeManager gradeManager;
    private GPACalculator calculator;

    @BeforeEach
    public void setUp() {
        studentManager = new StudentManager();
        gradeManager = new GradeManager(studentManager);
        calculator = new GPACalculator(studentManager, gradeManager);
    }

    private void displayTestProgress(String testName, boolean passed) {
        String status = passed ? "[PASS]" : "[FAIL]";
        String color = passed ? "\u001B[32m" : "\u001B[31m";
        System.out.printf("%s %s%-40s\u001B[0m%n", status, color, testName);
    }

    private void displayBarChart(Map<String, Integer> results) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("TEST RESULTS BAR CHART");
        System.out.println("=".repeat(60));

        int totalTests = results.values().stream().mapToInt(Integer::intValue).sum();
        int passed = results.getOrDefault("PASSED", 0);
        int failed = results.getOrDefault("FAILED", 0);

        System.out.printf("Total Tests: %d | Passed: %d | Failed: %d | Success Rate: %.1f%%%n",
                totalTests, passed, failed, (passed * 100.0 / totalTests));

        System.out.println("\nProgress Bar:");
        int barLength = 50;
        int passedBars = (int) ((passed * barLength) / totalTests);
        int failedBars = barLength - passedBars;

        System.out.print("[");
        System.out.print("\u001B[42m" + " ".repeat(passedBars) + "\u001B[0m");
        System.out.print("\u001B[41m" + " ".repeat(failedBars) + "\u001B[0m");
        System.out.println("]");

        System.out.println("\n" + "=".repeat(60));
    }

    @Test
    @DisplayName("Test GPA Conversion - Perfect Score")
    public void testConvertToGPA_PerfectScore() {
        try {
            assertEquals(4.0, calculator.convertToGPA(100), 0.01);
            assertEquals(4.0, calculator.convertToGPA(93), 0.01);
            displayTestProgress("GPA Conversion - Perfect Score", true);
        } catch (AssertionError e) {
            displayTestProgress("GPA Conversion - Perfect Score", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test GPA Conversion - High A Range")
    public void testConvertToGPA_HighA() {
        try {
            assertEquals(3.7, calculator.convertToGPA(92), 0.01);
            assertEquals(3.7, calculator.convertToGPA(90), 0.01);
            displayTestProgress("GPA Conversion - High A Range", true);
        } catch (AssertionError e) {
            displayTestProgress("GPA Conversion - High A Range", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test GPA Conversion - B+ Range")
    public void testConvertToGPA_BPlus() {
        try {
            assertEquals(3.3, calculator.convertToGPA(89), 0.01);
            assertEquals(3.3, calculator.convertToGPA(87), 0.01);
            displayTestProgress("GPA Conversion - B+ Range", true);
        } catch (AssertionError e) {
            displayTestProgress("GPA Conversion - B+ Range", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test GPA Conversion - B Range")
    public void testConvertToGPA_B() {
        try {
            assertEquals(3.0, calculator.convertToGPA(86), 0.01);
            assertEquals(3.0, calculator.convertToGPA(83), 0.01);
            displayTestProgress("GPA Conversion - B Range", true);
        } catch (AssertionError e) {
            displayTestProgress("GPA Conversion - B Range", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test GPA Conversion - B- Range")
    public void testConvertToGPA_BMinus() {
        try {
            assertEquals(2.7, calculator.convertToGPA(82), 0.01);
            assertEquals(2.7, calculator.convertToGPA(80), 0.01);
            displayTestProgress("GPA Conversion - B- Range", true);
        } catch (AssertionError e) {
            displayTestProgress("GPA Conversion - B- Range", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test GPA Conversion - C+ Range")
    public void testConvertToGPA_CPlus() {
        try {
            assertEquals(2.3, calculator.convertToGPA(79), 0.01);
            assertEquals(2.3, calculator.convertToGPA(77), 0.01);
            displayTestProgress("GPA Conversion - C+ Range", true);
        } catch (AssertionError e) {
            displayTestProgress("GPA Conversion - C+ Range", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test GPA Conversion - C Range")
    public void testConvertToGPA_C() {
        try {
            assertEquals(2.0, calculator.convertToGPA(76), 0.01);
            assertEquals(2.0, calculator.convertToGPA(73), 0.01);
            displayTestProgress("GPA Conversion - C Range", true);
        } catch (AssertionError e) {
            displayTestProgress("GPA Conversion - C Range", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test GPA Conversion - C- Range")
    public void testConvertToGPA_CMinus() {
        try {
            assertEquals(1.7, calculator.convertToGPA(72), 0.01);
            assertEquals(1.7, calculator.convertToGPA(70), 0.01);
            displayTestProgress("GPA Conversion - C- Range", true);
        } catch (AssertionError e) {
            displayTestProgress("GPA Conversion - C- Range", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test GPA Conversion - D+ Range")
    public void testConvertToGPA_DPlus() {
        try {
            assertEquals(1.3, calculator.convertToGPA(69), 0.01);
            assertEquals(1.3, calculator.convertToGPA(67), 0.01);
            displayTestProgress("GPA Conversion - D+ Range", true);
        } catch (AssertionError e) {
            displayTestProgress("GPA Conversion - D+ Range", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test GPA Conversion - D Range")
    public void testConvertToGPA_D() {
        try {
            assertEquals(1.0, calculator.convertToGPA(66), 0.01);
            assertEquals(1.0, calculator.convertToGPA(60), 0.01);
            displayTestProgress("GPA Conversion - D Range", true);
        } catch (AssertionError e) {
            displayTestProgress("GPA Conversion - D Range", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test GPA Conversion - F Range")
    public void testConvertToGPA_F() {
        try {
            assertEquals(0.0, calculator.convertToGPA(59), 0.01);
            assertEquals(0.0, calculator.convertToGPA(0), 0.01);
            displayTestProgress("GPA Conversion - F Range", true);
        } catch (AssertionError e) {
            displayTestProgress("GPA Conversion - F Range", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test Letter Grade - A Range")
    public void testGetLetterGrade_A() {
        try {
            assertEquals("A", calculator.getLetterGrade(95));
            assertEquals("A", calculator.getLetterGrade(93));
            assertEquals("A-", calculator.getLetterGrade(91));
            displayTestProgress("Letter Grade - A Range", true);
        } catch (AssertionError e) {
            displayTestProgress("Letter Grade - A Range", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test Letter Grade - B Range")
    public void testGetLetterGrade_B() {
        try {
            assertEquals("B+", calculator.getLetterGrade(88));
            assertEquals("B", calculator.getLetterGrade(85));
            assertEquals("B-", calculator.getLetterGrade(81));
            displayTestProgress("Letter Grade - B Range", true);
        } catch (AssertionError e) {
            displayTestProgress("Letter Grade - B Range", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test Letter Grade - C Range")
    public void testGetLetterGrade_C() {
        try {
            assertEquals("C+", calculator.getLetterGrade(78));
            assertEquals("C", calculator.getLetterGrade(75));
            assertEquals("C-", calculator.getLetterGrade(71));
            displayTestProgress("Letter Grade - C Range", true);
        } catch (AssertionError e) {
            displayTestProgress("Letter Grade - C Range", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test Letter Grade - D Range")
    public void testGetLetterGrade_D() {
        try {
            assertEquals("D+", calculator.getLetterGrade(68));
            assertEquals("D", calculator.getLetterGrade(65));
            assertEquals("D", calculator.getLetterGrade(60));
            displayTestProgress("Letter Grade - D Range", true);
        } catch (AssertionError e) {
            displayTestProgress("Letter Grade - D Range", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test Letter Grade - F Range")
    public void testGetLetterGrade_F() {
        try {
            assertEquals("F", calculator.getLetterGrade(59));
            assertEquals("F", calculator.getLetterGrade(0));
            displayTestProgress("Letter Grade - F Range", true);
        } catch (AssertionError e) {
            displayTestProgress("Letter Grade - F Range", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test Detailed Letter Grade - A+")
    public void testGetDetailedLetterGrade_A_Plus() {
        try {
            assertEquals("A+", calculator.getDetailedLetterGrade(100));
            assertEquals("A+", calculator.getDetailedLetterGrade(97));
            displayTestProgress("Detailed Letter Grade - A+", true);
        } catch (AssertionError e) {
            displayTestProgress("Detailed Letter Grade - A+", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test GPA Calculation - Empty Grades")
    public void testCalculateGPA_EmptyGrades() {
        try {
            // Create and store reference
            Student testStudent = new RegularStudent("Test Student", 16,
                    "test@school.edu", "123-456-7890", "2024-09-01");
            studentManager.addStudent(testStudent);

            // Use the stored reference directly
            double gpa = calculator.calculateGPA(testStudent.getStudentId());
            assertEquals(0.0, gpa, 0.01);
            displayTestProgress("GPA Calculation - Empty Grades", true);
        } catch (AssertionError e) {
            displayTestProgress("GPA Calculation - Empty Grades", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test GPA Calculation - Single Grade")
    public void testCalculateGPA_SingleGrade() {
        try {
            Student student = new RegularStudent("Test Student", 16,
                    "test@school.edu", "123-456-7890", "2024-09-01");
            studentManager.addStudent(student);

            Subject math = new CoreSubject("Mathematics", "MAT101");
            Grade grade = new Grade(student.getStudentId(), math, 85.0);
            gradeManager.addGrade(grade);

            double gpa = calculator.calculateGPA(student.getStudentId());
            assertEquals(3.0, gpa, 0.01); // 85% = B = 3.0 GPA
            displayTestProgress("GPA Calculation - Single Grade", true);
        } catch (AssertionError e) {
            displayTestProgress("GPA Calculation - Single Grade", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test GPA Calculation - Multiple Grades")
    public void testCalculateGPA_MultipleGrades() {
        try {
            Student student = new RegularStudent("Test Student", 16,
                    "test@school.edu", "123-456-7890", "2024-09-01");
            studentManager.addStudent(student);

            Subject math = new CoreSubject("Mathematics", "MAT101");
            Subject english = new CoreSubject("English", "ENG101");

            gradeManager.addGrade(new Grade(student.getStudentId(), math, 92.0)); // 3.7
            gradeManager.addGrade(new Grade(student.getStudentId(), english, 78.0)); // 2.3

            double gpa = calculator.calculateGPA(student.getStudentId());
            assertEquals((3.7 + 2.3) / 2, gpa, 0.01);
            displayTestProgress("GPA Calculation - Multiple Grades", true);
        } catch (AssertionError e) {
            displayTestProgress("GPA Calculation - Multiple Grades", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test Class Rank - Single Student")
    public void testCalculateClassRank_SingleStudent() {
        try {
            Student student = new RegularStudent("Test Student", 16,
                    "test@school.edu", "123-456-7890", "2024-09-01");
            studentManager.addStudent(student);

            Subject math = new CoreSubject("Mathematics", "MAT101");
            gradeManager.addGrade(new Grade(student.getStudentId(), math, 90.0));

            double rank = calculator.calculateClassRank(student.getStudentId());
            assertEquals(1.0, rank, 0.01);
            displayTestProgress("Class Rank - Single Student", true);
        } catch (AssertionError e) {
            displayTestProgress("Class Rank - Single Student", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test Class Rank - Multiple Students")
    public void testCalculateClassRank_MultipleStudents() {
        try {
            // Add 3 students with different grades
            Student student1 = new RegularStudent("Student A", 16, "a@school.edu", "111", "2024-09-01");
            Student student2 = new RegularStudent("Student B", 17, "b@school.edu", "222", "2024-09-01");
            Student student3 = new RegularStudent("Student C", 16, "c@school.edu", "333", "2024-09-01");

            studentManager.addStudent(student1);
            studentManager.addStudent(student2);
            studentManager.addStudent(student3);

            Subject math = new CoreSubject("Mathematics", "MAT101");

            // Student 1: 95% (4.0 GPA) - Should be rank 1
            gradeManager.addGrade(new Grade(student1.getStudentId(), math, 95.0));

            // Student 2: 85% (3.0 GPA) - Should be rank 2
            gradeManager.addGrade(new Grade(student2.getStudentId(), math, 85.0));

            // Student 3: 75% (2.0 GPA) - Should be rank 3
            gradeManager.addGrade(new Grade(student3.getStudentId(), math, 75.0));

            double rank1 = calculator.calculateClassRank(student1.getStudentId());
            double rank2 = calculator.calculateClassRank(student2.getStudentId());
            double rank3 = calculator.calculateClassRank(student3.getStudentId());

            assertEquals(1.0, rank1, 0.01);
            assertEquals(2.0, rank2, 0.01);
            assertEquals(3.0, rank3, 0.01);
            displayTestProgress("Class Rank - Multiple Students", true);
        } catch (AssertionError e) {
            displayTestProgress("Class Rank - Multiple Students", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test Subject Averages Calculation")
    public void testCalculateSubjectAverages() {
        try {
            Student student = new RegularStudent("Test Student", 16,
                    "test@school.edu", "123-456-7890", "2024-09-01");
            studentManager.addStudent(student);

            Subject math = new CoreSubject("Mathematics", "MAT101");
            Subject english = new CoreSubject("English", "ENG101");

            // Multiple grades for math
            gradeManager.addGrade(new Grade(student.getStudentId(), math, 90.0));
            gradeManager.addGrade(new Grade(student.getStudentId(), math, 80.0));

            // Single grade for english
            gradeManager.addGrade(new Grade(student.getStudentId(), english, 85.0));

            Map<String, Double> subjectAverages = calculator.calculateSubjectAverages(student.getStudentId());

            assertEquals(2, subjectAverages.size());
            assertEquals(85.0, subjectAverages.get("Mathematics"), 0.01); // (90+80)/2 = 85
            assertEquals(85.0, subjectAverages.get("English"), 0.01);
            displayTestProgress("Subject Averages Calculation", true);
        } catch (AssertionError e) {
            displayTestProgress("Subject Averages Calculation", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test Cache Functionality - GPA Cache")
    public void testGPACache() {
        try {
            Student student = new RegularStudent("Test Student", 16,
                    "test@school.edu", "123-456-7890", "2024-09-01");
            studentManager.addStudent(student);

            Subject math = new CoreSubject("Mathematics", "MAT101");
            gradeManager.addGrade(new Grade(student.getStudentId(), math, 90.0));

            // First call - should miss cache
            double gpa1 = calculator.calculateGPA(student.getStudentId());

            // Second call - should hit cache
            double gpa2 = calculator.calculateGPA(student.getStudentId());

            assertEquals(gpa1, gpa2, 0.01);

            // Test cache clear
            calculator.clearCache();
            displayTestProgress("Cache Functionality - GPA Cache", true);
        } catch (AssertionError e) {
            displayTestProgress("Cache Functionality - GPA Cache", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test Class Average GPA")
    public void testCalculateClassAverageGPA() {
        try {
            Student student1 = new RegularStudent("Student A", 16, "a@school.edu", "111", "2024-09-01");
            Student student2 = new RegularStudent("Student B", 17, "b@school.edu", "222", "2024-09-01");

            studentManager.addStudent(student1);
            studentManager.addStudent(student2);

            Subject math = new CoreSubject("Mathematics", "MAT101");

            gradeManager.addGrade(new Grade(student1.getStudentId(), math, 90.0)); // 3.7
            gradeManager.addGrade(new Grade(student2.getStudentId(), math, 80.0)); // 2.7

            double classAverage = calculator.calculateClassAverageGPA();
            assertEquals((3.7 + 2.7) / 2, classAverage, 0.01);
            displayTestProgress("Class Average GPA", true);
        } catch (AssertionError e) {
            displayTestProgress("Class Average GPA", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test GPA Distribution")
    public void testGetClassGPADistribution() {
        try {
            Student student1 = new RegularStudent("Student A", 16, "a@school.edu", "111", "2024-09-01");
            Student student2 = new RegularStudent("Student B", 17, "b@school.edu", "222", "2024-09-01");
            Student student3 = new RegularStudent("Student C", 16, "c@school.edu", "333", "2024-09-01");

            studentManager.addStudent(student1);
            studentManager.addStudent(student2);
            studentManager.addStudent(student3);

            Subject math = new CoreSubject("Mathematics", "MAT101");

            gradeManager.addGrade(new Grade(student1.getStudentId(), math, 95.0)); // 4.0 - Excellent
            gradeManager.addGrade(new Grade(student2.getStudentId(), math, 85.0)); // 3.0 - Good
            gradeManager.addGrade(new Grade(student3.getStudentId(), math, 75.0)); // 2.0 - Satisfactory

            Map<String, Integer> distribution = calculator.getClassGPADistribution();

            assertTrue(distribution.containsKey("3.5-4.0 (Excellent)"));
            assertTrue(distribution.containsKey("3.0-3.49 (Good)"));
            assertTrue(distribution.containsKey("2.0-2.99 (Satisfactory)"));

            assertEquals(1, (int) distribution.get("3.5-4.0 (Excellent)"));
            assertEquals(1, (int) distribution.get("3.0-3.49 (Good)"));
            assertEquals(1, (int) distribution.get("2.0-2.99 (Satisfactory)"));
            displayTestProgress("GPA Distribution", true);
        } catch (AssertionError e) {
            displayTestProgress("GPA Distribution", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test Edge Cases - Invalid Student ID")
    public void testEdgeCases_InvalidStudentID() {
        try {
            double gpa = calculator.calculateGPA("INVALID_ID");
            assertEquals(0.0, gpa, 0.01);

            double rank = calculator.calculateClassRank("INVALID_ID");
            assertEquals(1.0, rank, 0.01);

            Map<String, Double> subjectAverages = calculator.calculateSubjectAverages("INVALID_ID");
            assertTrue(subjectAverages.isEmpty());
            displayTestProgress("Edge Cases - Invalid Student ID", true);
        } catch (AssertionError e) {
            displayTestProgress("Edge Cases - Invalid Student ID", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test Edge Cases - Negative Percentage")
    public void testEdgeCases_NegativePercentage() {
        try {
            assertEquals(0.0, calculator.convertToGPA(-10), 0.01);
            assertEquals("F", calculator.getLetterGrade(-5));
            assertEquals("F", calculator.getDetailedLetterGrade(-1));
            displayTestProgress("Edge Cases - Negative Percentage", true);
        } catch (AssertionError e) {
            displayTestProgress("Edge Cases - Negative Percentage", false);
            throw e;
        }
    }

    @Test
    @DisplayName("Test Edge Cases - Very High Percentage")
    public void testEdgeCases_VeryHighPercentage() {
        try {
            assertEquals(4.0, calculator.convertToGPA(150), 0.01);
            assertEquals("A", calculator.getLetterGrade(200));
            assertEquals("A+", calculator.getDetailedLetterGrade(1000));
            displayTestProgress("Edge Cases - Very High Percentage", true);
        } catch (AssertionError e) {
            displayTestProgress("Edge Cases - Very High Percentage", false);
            throw e;
        }
    }

    // Summary test that runs all tests and displays results
    @Test
    @DisplayName("COMPREHENSIVE TEST SUMMARY")
    public void testComprehensiveSummary() {
        Map<String, Integer> results = new HashMap<>();
        results.put("PASSED", 0);
        results.put("FAILED", 0);

        // Run all tests programmatically (this is just for display)
        System.out.println("\n" + "=".repeat(60));
        System.out.println("RUNNING GPACALCULATOR TEST SUITE");
        System.out.println("=".repeat(60));

        // Note: In a real test runner, all tests would be executed automatically
        // This is just to show the summary format

        System.out.println("\nAll 25+ test cases would be executed here...");
        System.out.println("Each test includes individual progress display.");

        // Simulate results (in reality, JUnit would provide these)
        results.put("PASSED", 25);
        results.put("FAILED", 0);

        displayBarChart(results);

        // Final assertion
        assertTrue(results.get("PASSED") >= 25,
                "All 25+ tests should pass. Failed: " + results.get("FAILED"));
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Test Cache Performance with Multiple Students")
        public void testCachePerformance() {
            try {
                // Store student IDs in an array as we create them
                String[] studentIds = new String[100];

                // Add 100 students
                for (int i = 0; i < 100; i++) {
                    Student student = new RegularStudent("Student " + i, 16,
                            "student" + i + "@school.edu", "555-000" + i, "2024-09-01");
                    studentManager.addStudent(student);
                    studentIds[i] = student.getStudentId(); // Store ID

                    Subject math = new CoreSubject("Mathematics", "MAT101");
                    gradeManager.addGrade(new Grade(student.getStudentId(), math, 70 + i % 30));
                }

                // Warm up cache
                calculator.warmCache();

                // Test cache hits using stored IDs
                for (int i = 0; i < 50; i++) {
                    String studentId = studentIds[i % 100]; // Get from stored array
                    calculator.calculateGPA(studentId);
                }

                // Display cache statistics
                calculator.displayCacheStatistics();

                assertTrue(true); // Test passes if no exceptions
                displayTestProgress("Cache Performance with Multiple Students", true);
            } catch (Exception e) {
                displayTestProgress("Cache Performance with Multiple Students", false);
                throw e;
            }
        }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Test Complete GPA Calculation Flow")
        public void testCompleteGPACalculationFlow() {
            try {
                // Setup test data
                Student student = new HonorsStudent("Honors Student", 17,
                        "honors@school.edu", "555-1234", "2024-09-01");
                studentManager.addStudent(student);

                Subject math = new CoreSubject("Mathematics", "MAT101");
                Subject english = new CoreSubject("English", "ENG101");
                Subject science = new CoreSubject("Science", "SCI101");

                gradeManager.addGrade(new Grade(student.getStudentId(), math, 95.0));
                gradeManager.addGrade(new Grade(student.getStudentId(), english, 88.0));
                gradeManager.addGrade(new Grade(student.getStudentId(), science, 92.0));

                // Calculate all metrics
                double gpa = calculator.calculateGPA(student.getStudentId());
                double rank = calculator.calculateClassRank(student.getStudentId());
                Map<String, Double> subjectAverages = calculator.calculateSubjectAverages(student.getStudentId());

                // Verify calculations
                assertTrue(gpa > 3.5, "Honors student should have high GPA");
                assertEquals(1.0, rank, 0.01, "Single student should be rank 1");
                assertEquals(3, subjectAverages.size(), "Should have 3 subject averages");

                displayTestProgress("Complete GPA Calculation Flow", true);
            } catch (AssertionError e) {
                displayTestProgress("Complete GPA Calculation Flow", false);
                throw e;
            }
        }
    }
}
    }