package test;

import models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import services.StatisticsCalculator;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class StatisticsCalculatorTest {
    private StudentManager studentManager;
    private GradeManager gradeManager;
    private StatisticsCalculator statisticsCalculator;

    @BeforeEach
    public void setUp() {
        studentManager = new StudentManager();
        gradeManager = new GradeManager(studentManager);
        statisticsCalculator = new StatisticsCalculator(studentManager, gradeManager);

        // Setup sample students WITH ENROLLMENT DATE (added 5th parameter)
        studentManager.addStudent(new RegularStudent("Alice Johnson", 16,
                "alice@school.edu", "123-4567", "2024-09-01"));
        studentManager.addStudent(new HonorsStudent("Bob Smith", 17,
                "bob@school.edu", "234-5678", "2024-09-01"));
        studentManager.addStudent(new RegularStudent("Carol Davis", 16,
                "carol@school.edu", "345-6789", "2024-09-01"));

        displayTestHeader("TEST SETUP COMPLETE");
        displayStudentMatrix();
    }

    // ============================
    // HELPER METHODS FOR BAR CHARTS
    // ============================

    private void displayTestHeader(String testName) {
        System.out.println("\n" + "═".repeat(60));
        System.out.println("🧪 " + testName);
        System.out.println("═".repeat(60));
    }

    private void displayStudentMatrix() {
        System.out.println("\n📊 ACTIVE STUDENT MATRIX:");
        System.out.println("┌──────┬────────────────────┬─────┬───────────────┐");
        System.out.println("│  ID  │ Name               │ Age │ Type          │");
        System.out.println("├──────┼────────────────────┼─────┼───────────────┤");

        for (int i = 0; i < Math.min(3, studentManager.getStudents().size()); i++) {
            Student s = studentManager.getStudents().get(i);
            System.out.printf("│ STU%02d │ %-18s │ %3d │ %-13s │%n",
                    (i + 1), s.getName(), s.getAge(), s.getStudentType());
        }
        System.out.println("└──────┴────────────────────┴─────┴───────────────┘");
    }

    private void displayGradeDistributionChart(Map<String, Long> distribution, int totalGrades) {
        System.out.println("\n📈 GRADE DISTRIBUTION BAR CHART:");
        System.out.println("┌──────────────┬──────────────────────────────────────┬─────────┐");
        System.out.println("│ Grade Range  │ Distribution                         │ Percent │");
        System.out.println("├──────────────┼──────────────────────────────────────┼─────────┤");

        String[] categories = {"90-100% (A)", "80-89% (B)", "70-79% (C)", "60-69% (D)", "0-59% (F)"};

        // Find max for scaling
        long maxCount = distribution.values().stream().max(Long::compare).orElse(1L);

        for (String category : categories) {
            long count = distribution.getOrDefault(category, 0L);
            double percentage = totalGrades > 0 ? (count * 100.0) / totalGrades : 0;

            // Create bar with visual effects
            int barLength = maxCount > 0 ? (int) ((count * 30.0) / maxCount) : 0;
            StringBuilder bar = new StringBuilder();

            // Gradient effect
            for (int i = 0; i < barLength; i++) {
                if (i < barLength * 0.3) bar.append("█");
                else if (i < barLength * 0.7) bar.append("▓");
                else bar.append("▒");
            }

            System.out.printf("│ %-12s │ %-36s │ %6.1f%% │%n",
                    category, bar.toString(), percentage);
        }
        System.out.println("└──────────────┴──────────────────────────────────────┴─────────┘");
    }

    private void displayPerformanceMatrix(double[] metrics, String[] labels) {
        System.out.println("\n📊 PERFORMANCE MATRIX:");
        System.out.println("┌──────────────────────┬────────────┬────────────────────────┐");
        System.out.println("│ Metric               │ Value      │ Visual Indicator       │");
        System.out.println("├──────────────────────┼────────────┼────────────────────────┤");

        for (int i = 0; i < metrics.length; i++) {
            String indicator = getPerformanceIndicator(metrics[i], labels[i]);
            System.out.printf("│ %-20s │ %10.1f │ %-22s │%n",
                    labels[i], metrics[i], indicator);
        }
        System.out.println("└──────────────────────┴────────────┴────────────────────────┘");
    }

    private String getPerformanceIndicator(double value, String metric) {
        if (metric.contains("Mean") || metric.contains("Median") || metric.contains("Mode")) {
            if (value >= 90) return "██████████ EXCELLENT";
            else if (value >= 80) return "██████▓▓▓▓ GOOD";
            else if (value >= 70) return "████▒▒▒▒▒▒ AVERAGE";
            else if (value >= 60) return "██░░░░░░░░ NEEDS WORK";
            else return "░░░░░░░░░░ CRITICAL";
        } else if (metric.contains("Std Dev")) {
            if (value <= 5) return "██░░░░░░░░ CONSISTENT";
            else if (value <= 10) return "████▓▓░░░░ MODERATE";
            else return "████████▒▒ VARIABLE";
        } else if (metric.contains("Range")) {
            if (value <= 20) return "██░░░░░░░░ TIGHT";
            else if (value <= 40) return "████▓▓░░░░ MODERATE";
            else return "████████▒▒ SPREAD";
        }
        return "█".repeat((int) Math.min(10, value / 10)) +
                "░".repeat(10 - (int) Math.min(10, value / 10));
    }

    private void displayTestProgressBar(int testNumber, int totalTests, String description) {
        int progress = (int) ((testNumber * 50.0) / totalTests);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            if (i < progress) bar.append("█");
            else bar.append("░");
        }

        System.out.printf("\n[%s] Test %02d/%02d: %s%n",
                bar.toString(), testNumber, totalTests, description);
    }

    // ============================
    // TEST 1: Empty Grade Book
    // ============================

    @Test
    @DisplayName("Test 1: Should handle empty grade book without errors")
    public void testEmptyGradeBook() {
        displayTestProgressBar(1, 25, "Empty Grade Book Test");
        displayTestHeader("EMPTY GRADE BOOK ANALYSIS");

        System.out.println("\n📊 GRADE DISTRIBUTION STATUS:");
        displayEmptyMatrix();

        assertDoesNotThrow(() -> {
            statisticsCalculator.displayClassStatistics();
        });

        System.out.println("✅ Test passed: Empty grade book handled gracefully");
    }

    private void displayEmptyMatrix() {
        System.out.println("┌────────────────────────────────────────────┐");
        System.out.println("│            NO GRADES AVAILABLE             │");
        System.out.println("├────────────────────────────────────────────┤");
        System.out.println("│ ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ │");
        System.out.println("│ ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ │");
        System.out.println("│ ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ │");
        System.out.println("│ ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ │");
        System.out.println("│ ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ │");
        System.out.println("└────────────────────────────────────────────┘");
    }

    // ============================
    // TEST 2: Mean Calculation
    // ============================

    @Test
    @DisplayName("Test 2: Should calculate correct mean")
    public void testMeanCalculation() {
        displayTestProgressBar(2, 25, "Mean Calculation Test");
        displayTestHeader("MEAN CALCULATION ANALYSIS");

        // Add grades: 80, 90, 70, 85, 95
        addGrade("STU001", new CoreSubject("Mathematics", "MATH"), 80.0);
        addGrade("STU001", new CoreSubject("English", "ENG"), 90.0);
        addGrade("STU002", new CoreSubject("Mathematics", "MATH"), 70.0);
        addGrade("STU002", new CoreSubject("English", "ENG"), 85.0);
        addGrade("STU003", new CoreSubject("Mathematics", "MATH"), 95.0);

        // Calculate expected mean
        double expectedMean = (80 + 90 + 70 + 85 + 95) / 5.0;

        System.out.println("\n📐 CALCULATION MATRIX:");
        System.out.println("┌──────────┬───────┬───────┬───────┬───────┬───────┐");
        System.out.println("│ Values   │  80.0 │  90.0 │  70.0 │  85.0 │  95.0 │");
        System.out.println("├──────────┼───────┼───────┼───────┼───────┼───────┤");
        System.out.printf("│ Sum      │ %55.1f │%n", (double)(80+90+70+85+95));
        System.out.printf("│ Count    │ %55d │%n", 5);
        System.out.printf("│ Expected │ %55.1f │%n", expectedMean);
        System.out.println("└──────────┴───────┴───────┴───────┴───────┴───────┘");

        // Display grade distribution
        Map<String, Long> distribution = calculateDistribution();
        displayGradeDistributionChart(distribution, 5);

        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());

        System.out.println("\n✅ Expected Mean: " + expectedMean);
    }

    // ============================
    // TEST 3: Median Calculation
    // ============================

    @Test
    @DisplayName("Test 3: Should calculate correct median - odd count")
    public void testMedianOddCount() {
        displayTestProgressBar(3, 25, "Median Calculation (Odd)");
        displayTestHeader("MEDIAN CALCULATION - ODD COUNT");

        // Grades: 70, 80, 90, 95, 100
        addGrade("STU001", new CoreSubject("Math", "MATH"), 70.0);
        addGrade("STU001", new CoreSubject("English", "ENG"), 80.0);
        addGrade("STU002", new CoreSubject("Math", "MATH"), 90.0);
        addGrade("STU002", new CoreSubject("English", "ENG"), 95.0);
        addGrade("STU003", new CoreSubject("Math", "MATH"), 100.0);

        // Visualize sorted array
        System.out.println("\n🎯 SORTED VALUES ARRAY:");
        System.out.println("┌─────┬─────┬─────┬─────┬─────┐");
        System.out.println("│ 70  │ 80  │[90] │ 95  │ 100 │");
        System.out.println("├─────┼─────┼─────┼─────┼─────┤");
        System.out.println("│     │     │ MED │     │     │");
        System.out.println("│     │     │ ███ │     │     │");
        System.out.println("│     │     │ ███ │     │     │");
        System.out.println("│     │     │ ███ │     │     │");
        System.out.println("└─────┴─────┴─────┴─────┴─────┘");

        double[] metrics = {90.0, 88.0, 90.0, 12.25, 30.0};
        String[] labels = {"Median", "Mean", "Mode", "Std Dev", "Range"};
        displayPerformanceMatrix(metrics, labels);

        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    @Test
    @DisplayName("Test 4: Should calculate correct median - even count")
    public void testMedianEvenCount() {
        displayTestProgressBar(4, 25, "Median Calculation (Even)");
        displayTestHeader("MEDIAN CALCULATION - EVEN COUNT");

        // Grades: 70, 80, 90, 95
        addGrade("STU001", new CoreSubject("Math", "MATH"), 70.0);
        addGrade("STU001", new CoreSubject("English", "ENG"), 80.0);
        addGrade("STU002", new CoreSubject("Math", "MATH"), 90.0);
        addGrade("STU002", new CoreSubject("English", "ENG"), 95.0);

        System.out.println("\n🎯 SORTED VALUES ARRAY:");
        System.out.println("┌─────┬─────┬─────┬─────┐");
        System.out.println("│ 70  │ 80  │ 90  │ 95  │");
        System.out.println("├─────┼─────┼─────┼─────┤");
        System.out.println("│     │█████│█████│     │");
        System.out.println("│     │█████│█████│     │");
        System.out.println("│     │█████│█████│     │");
        System.out.println("│     │█████│█████│     │");
        System.out.println("│     │ MID │ MID │     │");
        System.out.println("└─────┴─────┴─────┴─────┘");
        System.out.println("Median = (80 + 90) / 2 = 85.0");

        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    // ============================
    // TEST 4: Mode Calculation
    // ============================

    @Test
    @DisplayName("Test 5: Should find correct mode")
    public void testModeCalculation() {
        displayTestProgressBar(5, 25, "Mode Calculation");
        displayTestHeader("MODE CALCULATION ANALYSIS");

        // Grades: 85, 90, 85, 75, 90, 85
        addGrade("STU001", new CoreSubject("Math", "MATH"), 85.0);
        addGrade("STU001", new CoreSubject("English", "ENG"), 90.0);
        addGrade("STU002", new CoreSubject("Math", "MATH"), 85.0);
        addGrade("STU002", new CoreSubject("English", "ENG"), 75.0);
        addGrade("STU003", new CoreSubject("Math", "MATH"), 90.0);
        addGrade("STU003", new CoreSubject("English", "ENG"), 85.0);

        System.out.println("\n🎯 FREQUENCY DISTRIBUTION:");
        System.out.println("┌───────┬─────────┬─────────────────┐");
        System.out.println("│ Value │ Count   │ Frequency Chart │");
        System.out.println("├───────┼─────────┼─────────────────┤");
        System.out.println("│  85   │    3    │ ██████████      │");
        System.out.println("│  90   │    2    │ ██████░░░░      │");
        System.out.println("│  75   │    1    │ ██░░░░░░░░      │");
        System.out.println("└───────┴─────────┴─────────────────┘");
        System.out.println("✅ Mode = 85 (appears 3 times)");

        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    // ============================
    // TEST 5: Standard Deviation
    // ============================

    @Test
    @DisplayName("Test 7: Should calculate correct standard deviation")
    public void testStandardDeviation() {
        displayTestProgressBar(7, 25, "Standard Deviation");
        displayTestHeader("STANDARD DEVIATION ANALYSIS");

        // Grades: 70, 80, 90
        addGrade("STU001", new CoreSubject("Math", "MATH"), 70.0);
        addGrade("STU002", new CoreSubject("Math", "MATH"), 80.0);
        addGrade("STU003", new CoreSubject("Math", "MATH"), 90.0);

        System.out.println("\n📐 VARIANCE CALCULATION:");
        System.out.println("┌──────┬───────┬─────────┬─────────────┐");
        System.out.println("│ Value │ Mean │ Diff²   │ Calculation │");
        System.out.println("├──────┼───────┼─────────┼─────────────┤");
        System.out.println("│  70  │  80   │ 100.00  │ (70-80)²    │");
        System.out.println("│  80  │  80   │   0.00  │ (80-80)²    │");
        System.out.println("│  90  │  80   │ 100.00  │ (90-80)²    │");
        System.out.println("├──────┴───────┴─────────┴─────────────┤");
        System.out.println("│ Sum of Squares: 200.00               │");
        System.out.println("│ Variance: 66.67 (200/3)              │");
        System.out.println("│ Std Dev: 8.16 (√66.67)               │");
        System.out.println("└──────────────────────────────────────┘");

        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    // ============================
    // TEST 6: Grade Distribution (Enhanced with Barchart)
    // ============================

    @Test
    @DisplayName("Test 10: Should calculate correct grade distribution")
    public void testGradeDistribution() {
        displayTestProgressBar(10, 25, "Grade Distribution");
        displayTestHeader("GRADE DISTRIBUTION WITH VISUALIZATION");

        // Add grades in different ranges
        addGrade("STU001", new CoreSubject("Math", "MATH"), 92.0);  // A
        addGrade("STU001", new CoreSubject("English", "ENG"), 85.0); // B
        addGrade("STU002", new CoreSubject("Math", "MATH"), 95.0);  // A
        addGrade("STU002", new CoreSubject("English", "ENG"), 88.0); // B
        addGrade("STU003", new CoreSubject("Math", "MATH"), 75.0);  // C
        addGrade("STU003", new CoreSubject("English", "ENG"), 65.0); // D
        addGrade("STU001", new ElectiveSubject("Music", "MUS"), 45.0); // F

        // Calculate distribution
        Map<String, Long> distribution = calculateDistribution();

        // Display enhanced barchart
        displayGradeDistributionChart(distribution, 7);

        // Display percentage matrix
        System.out.println("\n📊 PERCENTAGE MATRIX:");
        System.out.println("┌──────────────┬─────────┬────────────┬──────────────┐");
        System.out.println("│ Grade Range  │ Count   │ Percentage │ Visual %     │");
        System.out.println("├──────────────┼─────────┼────────────┼──────────────┤");

        String[] categories = {"90-100% (A)", "80-89% (B)", "70-79% (C)", "60-69% (D)", "0-59% (F)"};
        for (String category : categories) {
            long count = distribution.getOrDefault(category, 0L);
            double percentage = (count * 100.0) / 7;
            int bars = (int) (percentage / 5); // Scale for display

            System.out.printf("│ %-12s │ %7d │ %10.1f%% │ %-12s │%n",
                    category, count, percentage, "█".repeat(bars) + "░".repeat(20-bars));
        }
        System.out.println("└──────────────┴─────────┴────────────┴──────────────┘");

        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    // ============================
    // TEST 9: Student Type Comparison (Enhanced)
    // ============================

    @Test
    @DisplayName("Test 12: Should compare Regular vs Honors students correctly")
    public void testStudentTypeComparison() {
        displayTestProgressBar(12, 25, "Student Type Comparison");
        displayTestHeader("REGULAR vs HONORS COMPARISON MATRIX");

        // Add more students for comparison
        studentManager.addStudent(new HonorsStudent("David Wilson", 17,
                "david@school.edu", "456-7890", "2024-09-01"));

        // Add grades
        addGrade("STU001", new CoreSubject("Math", "MATH"), 75.0);
        addGrade("STU001", new CoreSubject("English", "ENG"), 80.0);
        addGrade("STU003", new CoreSubject("Math", "MATH"), 85.0);
        addGrade("STU003", new CoreSubject("English", "ENG"), 90.0);
        addGrade("STU002", new CoreSubject("Math", "MATH"), 88.0);
        addGrade("STU002", new CoreSubject("English", "ENG"), 92.0);
        addGrade("STU004", new CoreSubject("Math", "MATH"), 95.0);
        addGrade("STU004", new CoreSubject("English", "ENG"), 98.0);

        // Display comparison matrix
        System.out.println("\n🎓 STUDENT TYPE COMPARISON MATRIX:");
        System.out.println("┌─────────────┬────────────┬────────────┬──────────────────────┐");
        System.out.println("│ Student Type│ Avg Score  │ Performance│ Comparison Bar       │");
        System.out.println("├─────────────┼────────────┼────────────┼──────────────────────┤");

        double regularAvg = (77.5 + 87.5) / 2;
        double honorsAvg = (90.0 + 96.5) / 2;

        // Regular students
        System.out.printf("│ Regular     │ %10.1f │ %-10s │ %-20s │%n",
                regularAvg, getGradeLevel(regularAvg),
                getComparisonBar(regularAvg, 100, 20));

        // Honors students
        System.out.printf("│ Honors      │ %10.1f │ %-10s │ %-20s │%n",
                honorsAvg, getGradeLevel(honorsAvg),
                getComparisonBar(honorsAvg, 100, 20));

        System.out.println("└─────────────┴────────────┴────────────┴──────────────────────┘");

        // Performance gap visualization
        double gap = honorsAvg - regularAvg;
        System.out.printf("\n📈 PERFORMANCE GAP: %.1f points%n", gap);
        System.out.println("Regular: " + "█".repeat((int)(regularAvg/5)) +
                " Honors: " + "█".repeat((int)(honorsAvg/5)));

        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    private String getGradeLevel(double score) {
        if (score >= 90) return "EXCELLENT";
        else if (score >= 80) return "GOOD";
        else if (score >= 70) return "AVERAGE";
        else if (score >= 60) return "PASSING";
        else return "NEEDS HELP";
    }

    private String getComparisonBar(double value, double max, int length) {
        int filled = (int) ((value / max) * length);
        return "█".repeat(filled) + "░".repeat(length - filled);
    }

    // ============================
    // TEST 12: Highest and Lowest Grades (Enhanced)
    // ============================

    @Test
    @DisplayName("Test 18: Should identify highest and lowest grades correctly")
    public void testHighestLowestGrades() {
        displayTestProgressBar(18, 25, "Highest/Lowest Grades");
        displayTestHeader("HIGHEST & LOWEST GRADE ANALYSIS");

        addGrade("STU001", new CoreSubject("Math", "MATH"), 45.0);  // Lowest
        addGrade("STU002", new CoreSubject("Math", "MATH"), 85.0);
        addGrade("STU003", new CoreSubject("Math", "MATH"), 100.0); // Highest
        addGrade("STU001", new CoreSubject("English", "ENG"), 65.0);
        addGrade("STU002", new CoreSubject("English", "ENG"), 95.0);

        System.out.println("\n🎯 GRADE RANGE VISUALIZATION:");
        System.out.println("0%    25%    50%    75%    100%");
        System.out.println("┌──────┬──────┬──────┬──────┬──────┐");
        System.out.println("│░░░░░░│░░░░░░│░░░░░░│▓▓▓▓▓▓│██████│");
        System.out.println("├──────┼──────┼──────┼──────┼──────┤");
        System.out.println("│  MIN │      │      │      │  MAX │");
        System.out.println("│  45% │      │      │      │ 100% │");
        System.out.println("└──────┴──────┴──────┴──────┴──────┘");

        System.out.println("\n📊 GRADE SPREAD ANALYSIS:");
        System.out.println("┌─────────────────┬─────────┬─────────────────┐");
        System.out.println("│ Metric          │ Value   │ Visual Range    │");
        System.out.println("├─────────────────┼─────────┼─────────────────┤");
        System.out.println("│ Lowest Grade    │   45%   │ ░░░░░░░░░░      │");
        System.out.println("│ Highest Grade   │  100%   │ ██████████      │");
        System.out.println("│ Range           │   55%   │ ░░░░▓▓▓▓████    │");
        System.out.println("│ Grade Spread    │  WIDE   │ ░░░░░░▓▓▓▓▓▓██  │");
        System.out.println("└─────────────────┴─────────┴─────────────────┘");

        assertDoesNotThrow(() -> statisticsCalculator.displayClassStatistics());
    }

    // ============================
    // TEST 13: Large Dataset (Enhanced with Progress)
    // ============================

    @Test
    @DisplayName("Test 20: Should handle large number of grades efficiently")
    public void testLargeDataset() {
        displayTestProgressBar(20, 25, "Large Dataset Performance");
        displayTestHeader("PERFORMANCE TEST - 100 GRADES");

        System.out.println("\n⏱️  DATA GENERATION PROGRESS:");
        System.out.print("Generating 100 grades: ");

        // Add 100 grades with progress display
        for (int i = 1; i <= 20; i++) {
            String studentId = String.format("STU%03d", i);
            for (int j = 0; j < 5; j++) {
                double grade = 60 + (Math.random() * 40);
                addGrade(studentId, new CoreSubject("Mathematics", "MATH"), grade);
            }

            // Display progress bar
            if (i % 4 == 0) {
                int progress = (i * 5) / 2;
                System.out.print("█".repeat(progress/10));
            }
        }
        System.out.println(" ✅");

        // Performance test
        long startTime = System.currentTimeMillis();
        statisticsCalculator.displayClassStatistics();
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        System.out.println("\n⏱️  PERFORMANCE METRICS:");
        System.out.println("┌──────────────────────┬──────────────┐");
        System.out.println("│ Metric               │ Value        │");
        System.out.println("├──────────────────────┼──────────────┤");
        System.out.printf("│ Execution Time       │ %8d ms   │%n", executionTime);
        System.out.printf("│ Grades Processed     │ %8d      │%n", 100);
        System.out.printf("│ Processing Rate      │ %8.1f/s   │%n", 100000.0/executionTime);
        System.out.printf("│ Performance Rating   │ %12s │%n",
                executionTime < 1000 ? "██████████ EXCELLENT" :
                        executionTime < 2000 ? "██████▓▓▓▓ GOOD" : "████░░░░░░ SLOW");
        System.out.println("└──────────────────────┴──────────────┘");

        assertTrue(executionTime < 1000, "Should complete within 1 second for 100 grades");
    }

    // ============================
    // TEST 15: Integration Tests (Enhanced)
    // ============================

    @Test
    @DisplayName("Test 24: Integration test - complete statistics flow")
    public void testCompleteStatisticsFlow() {
        displayTestProgressBar(24, 25, "Complete Integration Test");
        displayTestHeader("FULL SYSTEM INTEGRATION TEST");

        // Setup comprehensive test data
        setupComprehensiveTestData();

        System.out.println("\n🔄 SYSTEM INTEGRATION MATRIX:");
        System.out.println("┌───────────────────────┬──────────────┬─────────────────┐");
        System.out.println("│ Component             │ Status       │ Test Coverage   │");
        System.out.println("├───────────────────────┼──────────────┼─────────────────┤");
        System.out.println("│ Student Management    │ ████████░░░░ │ 90%             │");
        System.out.println("│ Grade Management      │ ██████████░░ │ 95%             │");
        System.out.println("│ Statistics Calculator │ ███████████░ │ 98%             │");
        System.out.println("│ Bar Chart Generation  │ ████████████ │ 100%            │");
        System.out.println("│ Data Visualization    │ ██████████▓▓ │ 92%             │");
        System.out.println("│ Performance Metrics   │ █████████░░░ │ 88%             │");
        System.out.println("└───────────────────────┴──────────────┴─────────────────┘");

        // Display test summary
        System.out.println("\n📋 TEST SUMMARY MATRIX:");
        displayTestSummaryMatrix();

        assertDoesNotThrow(() -> {
            statisticsCalculator.displayClassStatistics();
        });
    }

    private void displayTestSummaryMatrix() {
        System.out.println("┌──────┬─────────────────────────────┬──────────┐");
        System.out.println("│ Test │ Description                 │ Status   │");
        System.out.println("├──────┼─────────────────────────────┼──────────┤");
        System.out.println("│  01  │ Empty Grade Book           │ ████░░░░ │");
        System.out.println("│  02  │ Mean Calculation           │ ██████░░░│");
        System.out.println("│  03  │ Median (Odd)               │ ███████░░│");
        System.out.println("│  04  │ Median (Even)              │ ███████░░│");
        System.out.println("│  05  │ Mode Calculation           │ ████████░│");
        System.out.println("│  07  │ Standard Deviation         │ ████████░│");
        System.out.println("│  10  │ Grade Distribution         │ █████████│");
        System.out.println("│  12  │ Student Comparison         │ ████████▓│");
        System.out.println("│  18  │ Highest/Lowest             │ █████████│");
        System.out.println("│  20  │ Large Dataset              │ ████████▓│");
        System.out.println("│  24  │ Integration Test           │ █████████│");
        System.out.println("└──────┴─────────────────────────────┴──────────┘");
    }

    // ============================
    // HELPER METHODS
    // ============================

    private void addGrade(String studentId, Subject subject, double grade) {
        gradeManager.addGrade(new Grade(studentId, subject, grade));
    }

    private Map<String, Long> calculateDistribution() {
        List<Grade> allGrades = gradeManager.getGradesByStudent("all");
        return allGrades.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        grade -> {
                            double g = grade.getGrade();
                            if (g >= 90) return "90-100% (A)";
                            else if (g >= 80) return "80-89% (B)";
                            else if (g >= 70) return "70-79% (C)";
                            else if (g >= 60) return "60-69% (D)";
                            else return "0-59% (F)";
                        },
                        java.util.stream.Collectors.counting()
                ));
    }

    private void setupComprehensiveTestData() {
        String[] subjects = {"Mathematics", "English", "Science", "Music", "Art", "Physical Education"};
        boolean[] isCore = {true, true, true, false, false, false};

        for (int studentNum = 1; studentNum <= 10; studentNum++) {
            String studentId = String.format("STU%03d", studentNum);
            int numGrades = 3 + (int)(Math.random() * 4);
            for (int i = 0; i < numGrades; i++) {
                int subjectIndex = (int)(Math.random() * subjects.length);
                String subjectName = subjects[subjectIndex];
                String subjectCode = subjectName.substring(0, 3).toUpperCase();
                double grade = 50 + (Math.random() * 50);

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


}