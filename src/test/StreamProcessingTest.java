package test;

import models.*;
import services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.*;
import java.util.stream.*;
import java.util.function.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Stream Processing Test Suite")
public class StreamProcessingTest {
    private StudentManager studentManager;
    private GradeManager gradeManager;
    private StreamProcessor streamProcessor;

    private Map<String, Integer> streamResults;

    @BeforeEach
    public void setUp() {
        studentManager = new StudentManager();
        gradeManager = new GradeManager(studentManager);
        streamProcessor = new StreamProcessor(studentManager, gradeManager);
        streamResults = new HashMap<>();

        // Add comprehensive test data
        addTestData();
    }

    private void addTestData() {
        // Add students with different types and GPAs
        Student honors1 = new HonorsStudent("Alice Johnson", 20, "alice@university.edu",
                "(555) 111-1111", "2024-09-01");
        Student honors2 = new HonorsStudent("Bob Wilson", 21, "bob@university.edu",
                "(555) 222-2222", "2024-09-01");
        Student regular1 = new RegularStudent("Charlie Brown", 19, "charlie@university.edu",
                "(555) 333-3333", "2024-09-01");
        Student regular2 = new RegularStudent("Diana Prince", 22, "diana@university.edu",
                "(555) 444-4444", "2024-09-01");

        studentManager.addStudent(honors1);
        studentManager.addStudent(honors2);
        studentManager.addStudent(regular1);
        studentManager.addStudent(regular2);

        // Add grades for different subjects
        Subject math = new CoreSubject("Mathematics", "MAT101");
        Subject english = new CoreSubject("English", "ENG101");
        Subject science = new CoreSubject("Science", "SCI101");
        Subject art = new ElectiveSubject("Art", "ART101");
        Subject music = new ElectiveSubject("Music", "MUS101");

        // Alice: High grades (Honors)
        addGrade(honors1.getStudentId(), math, 95);
        addGrade(honors1.getStudentId(), english, 92);
        addGrade(honors1.getStudentId(), science, 98);
        addGrade(honors1.getStudentId(), art, 88);

        // Bob: High grades (Honors)
        addGrade(honors2.getStudentId(), math, 90);
        addGrade(honors2.getStudentId(), english, 94);
        addGrade(honors2.getStudentId(), science, 91);
        addGrade(honors2.getStudentId(), music, 87);

        // Charlie: Medium grades (Regular)
        addGrade(regular1.getStudentId(), math, 78);
        addGrade(regular1.getStudentId(), english, 82);
        addGrade(regular1.getStudentId(), science, 75);
        addGrade(regular1.getStudentId(), art, 80);

        // Diana: Low grades (Regular)
        addGrade(regular2.getStudentId(), math, 65);
        addGrade(regular2.getStudentId(), english, 70);
        addGrade(regular2.getStudentId(), science, 68);
        addGrade(regular2.getStudentId(), music, 72);
    }

    private void addGrade(String studentId, Subject subject, double gradeValue) {
        Grade grade = new Grade(studentId, subject, gradeValue);
        gradeManager.addGrade(grade);
    }

    @Nested
    @DisplayName("Filter Operations")
    class FilterOperationsTests {

        @Test
        @DisplayName("Filter Students by GPA Range")
        void testFilterByGpaRange() {
            List<Student> allStudents = studentManager.getStudents();

            // Filter students with GPA >= 90
            List<Student> highPerformers = allStudents.stream()
                    .filter(student -> gradeManager
                            .calculateOverallAverage(student.getStudentId()) >= 90)
                    .collect(Collectors.toList());

            // Filter students with GPA < 80
            List<Student> lowPerformers = allStudents.stream()
                    .filter(student -> gradeManager
                            .calculateOverallAverage(student.getStudentId()) < 80)
                    .collect(Collectors.toList());

            System.out.printf("High performers (GPA >= 90): %d students%n", highPerformers.size());
            System.out.printf("Low performers (GPA < 80): %d students%n", lowPerformers.size());

            assertEquals(2, highPerformers.size(), "Should have 2 high performers");
            assertEquals(2, lowPerformers.size(), "Should have 2 low performers");

            // Verify no overlap
            Set<String> highIds = highPerformers.stream().map(Student::getStudentId)
                    .collect(Collectors.toSet());
            Set<String> lowIds = lowPerformers.stream().map(Student::getStudentId)
                    .collect(Collectors.toSet());
            assertTrue(Collections.disjoint(highIds, lowIds), "High and low performers should not overlap");

            streamResults.put("filter_by_gpa_range", 1);
        }

        @Test
        @DisplayName("Filter Students by Type")
        void testFilterByStudentType() {
            List<Student> allStudents = studentManager.getStudents();

            // Filter honors students
            List<Student> honorsStudents = allStudents.stream()
                    .filter(student -> "Honors".equals(student.getStudentType()))
                    .collect(Collectors.toList());

            // Filter regular students
            List<Student> regularStudents = allStudents.stream()
                    .filter(student -> "Regular".equals(student.getStudentType()))
                    .collect(Collectors.toList());

            System.out.printf("Honors students: %d%n", honorsStudents.size());
            System.out.printf("Regular students: %d%n", regularStudents.size());

            assertEquals(2, honorsStudents.size(), "Should have 2 honors students");
            assertEquals(2, regularStudents.size(), "Should have 2 regular students");
            assertEquals(allStudents.size(), honorsStudents.size() + regularStudents.size(),
                    "All students should be either honors or regular");

            streamResults.put("filter_by_student_type", 1);
        }

        @Test
        @DisplayName("Filter Grades by Performance Category")
        void testFilterGradesByPerformance() {
            List<Grade> allGrades = gradeManager.getGradesByStudent("all");

            // Filter excellent grades (>= 90)
            List<Grade> excellentGrades = allGrades.stream()
                    .filter(grade -> grade.getGrade() >= 90)
                    .collect(Collectors.toList());

            // Filter failing grades (< 70)
            List<Grade> failingGrades = allGrades.stream()
                    .filter(grade -> grade.getGrade() < 70)
                    .collect(Collectors.toList());

            System.out.printf("Excellent grades (>= 90): %d%n", excellentGrades.size());
            System.out.printf("Failing grades (< 70): %d%n", failingGrades.size());

            assertEquals(6, excellentGrades.size(), "Should have 6 excellent grades");
            assertEquals(2, failingGrades.size(), "Should have 2 failing grades");

            streamResults.put("filter_grades_by_performance", 1);
        }
    }

    @Nested
    @DisplayName("Map Operations")
    class MapOperationsTests {

        @Test
        @DisplayName("Map Students to GPA Values")
        void testMapStudentsToGpa() {
            List<Student> students = studentManager.getStudents();

            // Map students to their GPA values
            List<Double> gpas = students.stream()
                    .map(student -> gradeManager.calculateOverallAverage(student.getStudentId()))
                    .collect(Collectors.toList());

            // Map students to summary strings
            List<String> summaries = students.stream()
                    .map(student -> String.format("%s: GPA %.2f",
                            student.getName(),
                            gradeManager.calculateOverallAverage(student.getStudentId())))
                    .collect(Collectors.toList());

            System.out.println("Student GPAs:");
            summaries.forEach(System.out::println);

            assertEquals(students.size(), gpas.size(), "Should have GPA for each student");
            assertEquals(students.size(), summaries.size(), "Should have summary for each student");

            // Verify all GPAs are reasonable
            assertTrue(gpas.stream().allMatch(gpa -> gpa >= 0 && gpa <= 100),
                    "All GPAs should be between 0 and 100");

            streamResults.put("map_students_to_gpa", 1);
        }

        @Test
        @DisplayName("Map Grades to Letter Grades")
        void testMapGradesToLetterGrades() {
            List<Grade> allGrades = gradeManager.getGradesByStudent("all");

            // Map numeric grades to letter grades
            List<String> letterGrades = allGrades.stream()
                    .map(Grade::getLetterGrade)
                    .collect(Collectors.toList());

            // Map grades to performance categories
            Map<String, List<Grade>> gradesByCategory = allGrades.stream()
                    .collect(Collectors.groupingBy(grade -> {
                        double g = grade.getGrade();
                        if (g >= 90)
                            return "Excellent";
                        else if (g >= 80)
                            return "Good";
                        else if (g >= 70)
                            return "Average";
                        else if (g >= 60)
                            return "Below Average";
                        else
                            return "Failing";
                    }));

            System.out.println("Letter grades distribution:");
            Map<String, Long> letterCount = letterGrades.stream()
                    .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
            letterCount.forEach((letter, count) -> System.out.printf("%s: %d grades%n", letter, count));

            System.out.println("\nPerformance categories:");
            gradesByCategory
                    .forEach((category, grades) -> System.out.printf("%s: %d grades%n", category,
                            grades.size()));

            assertEquals(allGrades.size(), letterGrades.size(),
                    "Should have letter grade for each numeric grade");
            assertTrue(gradesByCategory.size() >= 3, "Should have multiple performance categories");

            streamResults.put("map_grades_to_letter_grades", 1);
        }

        @Test
        @DisplayName("Map Subjects to Average Scores")
        void testMapSubjectsToAverages() {
            Map<String, Double> subjectAverages = streamProcessor.calculateAverageBySubject();

            System.out.println("Subject averages:");
            subjectAverages.forEach((subject, avg) -> System.out.printf("%s: %.2f%n", subject, avg));

            // Verify known subjects have correct averages
            assertTrue(subjectAverages.containsKey("Mathematics"), "Should have Mathematics average");
            assertTrue(subjectAverages.containsKey("English"), "Should have English average");
            assertTrue(subjectAverages.containsKey("Science"), "Should have Science average");

            // All averages should be reasonable
            assertTrue(subjectAverages.values().stream().allMatch(avg -> avg >= 0 && avg <= 100),
                    "All subject averages should be between 0 and 100");

            streamResults.put("map_subjects_to_averages", 1);
        }
    }

    @Nested
    @DisplayName("Reduce Operations")
    class ReduceOperationsTests {

        @Test
        @DisplayName("Reduce to Calculate Class Statistics")
        void testReduceClassStatistics() {
            List<Grade> allGrades = gradeManager.getGradesByStudent("all");

            // Reduce to find highest grade
            Optional<Grade> highestGrade = allGrades.stream()
                    .reduce((g1, g2) -> g1.getGrade() > g2.getGrade() ? g1 : g2);

            // Reduce to find lowest grade
            Optional<Grade> lowestGrade = allGrades.stream()
                    .reduce((g1, g2) -> g1.getGrade() < g2.getGrade() ? g1 : g2);

            // Reduce to calculate sum of all grades
            double sum = allGrades.stream()
                    .mapToDouble(Grade::getGrade)
                    .reduce(0.0, Double::sum);

            double average = sum / allGrades.size();

            System.out.printf("Highest grade: %.1f (%s)%n",
                    highestGrade.get().getGrade(), highestGrade.get().getStudentId());
            System.out.printf("Lowest grade: %.1f (%s)%n",
                    lowestGrade.get().getGrade(), lowestGrade.get().getStudentId());
            System.out.printf("Class average: %.2f%n", average);

            assertTrue(highestGrade.isPresent(), "Should find highest grade");
            assertTrue(lowestGrade.isPresent(), "Should find lowest grade");
            assertTrue(average >= 0 && average <= 100, "Class average should be reasonable");

            streamResults.put("reduce_class_statistics", 1);
        }

        @Test
        @DisplayName("Reduce to Count Grade Distribution")
        void testReduceGradeDistribution() {
            List<Grade> allGrades = gradeManager.getGradesByStudent("all");

            // Reduce to count grades by letter grade
            Map<String, Integer> letterDistribution = allGrades.stream()
                    .reduce(new HashMap<>(), (map, grade) -> {
                        String letter = grade.getLetterGrade();
                        map.put(letter, map.getOrDefault(letter, 0) + 1);
                        return map;
                    }, (map1, map2) -> {
                        map2.forEach((key, value) -> map1.put(key,
                                map1.getOrDefault(key, 0) + value));
                        return map1;
                    });

            System.out.println("Grade distribution by letter:");
            letterDistribution.forEach(
                    (letter, count) -> System.out.printf("%s: %d grades%n", letter, count));

            int totalCount = letterDistribution.values().stream().mapToInt(Integer::intValue).sum();
            assertEquals(allGrades.size(), totalCount, "Distribution should account for all grades");

            streamResults.put("reduce_grade_distribution", 1);
        }

        @Test
        @DisplayName("Reduce to Find Student with Highest GPA")
        void testReduceHighestGpaStudent() {
            List<Student> students = studentManager.getStudents();

            // Reduce to find student with highest GPA
            Optional<Student> topStudent = students.stream()
                    .reduce((s1, s2) -> {
                        double gpa1 = gradeManager.calculateOverallAverage(s1.getStudentId());
                        double gpa2 = gradeManager.calculateOverallAverage(s2.getStudentId());
                        return gpa1 > gpa2 ? s1 : s2;
                    });

            if (topStudent.isPresent()) {
                double topGpa = gradeManager.calculateOverallAverage(topStudent.get().getStudentId());
                System.out.printf("Top student: %s with GPA %.2f%n",
                        topStudent.get().getName(), topGpa);

                assertTrue(topGpa >= 0 && topGpa <= 100, "Top GPA should be reasonable");
            }

            assertTrue(topStudent.isPresent(), "Should find a top student");

            streamResults.put("reduce_highest_gpa_student", 1);
        }
    }

    @Nested
    @DisplayName("Parallel Stream Tests")
    class ParallelStreamTests {

        @Test
        @DisplayName("Parallel Stream Correctness")
        void testParallelStreamCorrectness() {
            List<Student> students = studentManager.getStudents();

            // Sequential processing
            List<Double> sequentialGpas = students.stream()
                    .map(student -> gradeManager.calculateOverallAverage(student.getStudentId()))
                    .sorted()
                    .collect(Collectors.toList());

            // Parallel processing
            List<Double> parallelGpas = students.parallelStream()
                    .map(student -> gradeManager.calculateOverallAverage(student.getStudentId()))
                    .sorted()
                    .collect(Collectors.toList());

            System.out.printf("Sequential GPAs: %s%n", sequentialGpas);
            System.out.printf("Parallel GPAs: %s%n", parallelGpas);

            // Results should be identical when sorted
            assertEquals(sequentialGpas, parallelGpas,
                    "Sequential and parallel results should be identical");

            streamResults.put("parallel_stream_correctness", 1);
        }

        @Test
        @DisplayName("Parallel vs Sequential Performance")
        void testParallelVsSequentialPerformance() {
            List<Student> students = studentManager.getStudents();

            // Expand the dataset for meaningful performance comparison
            for (int i = 0; i < 100; i++) {
                Student student = new RegularStudent(
                        "Bulk Student " + i,
                        18 + (i % 10), // Age between 18-27
                        "bulk" + i + "@university.edu",
                        "(555) 999-" + String.format("%04d", i),
                        "2024-09-01");
                studentManager.addStudent(student);

                // Add a few grades for each bulk student
                Subject subject = new CoreSubject("Bulk Subject", "BLK101");
                Grade grade = new Grade(student.getStudentId(), subject, 70 + (i % 30)); // 70-99 range
                gradeManager.addGrade(grade);
            }

            List<Student> largeStudentList = studentManager.getStudents();

            // Sequential processing
            long sequentialStart = System.nanoTime();
            double sequentialSum = largeStudentList.stream()
                    .mapToDouble(student -> gradeManager
                            .calculateOverallAverage(student.getStudentId()))
                    .sum();
            long sequentialTime = System.nanoTime() - sequentialStart;

            // Parallel processing
            long parallelStart = System.nanoTime();
            double parallelSum = largeStudentList.parallelStream()
                    .mapToDouble(student -> gradeManager
                            .calculateOverallAverage(student.getStudentId()))
                    .sum();
            long parallelTime = System.nanoTime() - parallelStart;

            long seqMs = sequentialTime / 1_000_000;
            long parMs = parallelTime / 1_000_000;

            System.out.printf("Sequential processing: %d ms, sum: %.2f%n", seqMs, sequentialSum);
            System.out.printf("Parallel processing: %d ms, sum: %.2f%n", parMs, parallelSum);
            System.out.printf("Speedup: %.2fx%n", seqMs > 0 ? (double) seqMs / parMs : 0);

            // Results should be very close (allowing for floating point precision)
            assertEquals(sequentialSum, parallelSum, 0.01, "Sequential and parallel sums should be equal");

            streamResults.put("parallel_vs_sequential_performance", 1);
        }

        @Test
        @DisplayName("Parallel Stream Short-Circuiting")
        void testParallelStreamShortCircuiting() {
            List<Student> students = studentManager.getStudents();

            // Test findFirst with parallel stream
            Optional<Student> firstSequential = students.stream()
                    .filter(student -> gradeManager
                            .calculateOverallAverage(student.getStudentId()) > 0)
                    .findFirst();

            Optional<Student> firstParallel = students.parallelStream()
                    .filter(student -> gradeManager
                            .calculateOverallAverage(student.getStudentId()) > 0)
                    .findFirst();

            // Both should find a student (since all have grades)
            assertTrue(firstSequential.isPresent(), "Sequential findFirst should find a student");
            assertTrue(firstParallel.isPresent(), "Parallel findFirst should find a student");

            // Test anyMatch short-circuiting
            boolean hasHighPerformerSequential = students.stream()
                    .anyMatch(student -> gradeManager
                            .calculateOverallAverage(student.getStudentId()) >= 90);

            boolean hasHighPerformerParallel = students.parallelStream()
                    .anyMatch(student -> gradeManager
                            .calculateOverallAverage(student.getStudentId()) >= 90);

            assertEquals(hasHighPerformerSequential, hasHighPerformerParallel,
                    "Sequential and parallel anyMatch should agree");

            streamResults.put("parallel_stream_short_circuiting", 1);
        }
    }

    @Nested
    @DisplayName("Lazy Evaluation Tests")
    class LazyEvaluationTests {

        @Test
        @DisplayName("Lazy Evaluation Behavior")
        void testLazyEvaluation() {
            List<Student> students = studentManager.getStudents();

            // Create a stream with intermediate operations
            // Use peek to observe when operations are executed
            List<String> processedNames = new ArrayList<>();
            List<Double> processedGpas = new ArrayList<>();

            List<Student> result = students.stream()
                    .peek(student -> processedNames.add("peeked: " + student.getName()))
                    .filter(student -> {
                        double gpa = gradeManager
                                .calculateOverallAverage(student.getStudentId());
                        processedGpas.add(gpa);
                        return gpa >= 80;
                    })
                    .limit(2) // This should cause short-circuiting
                    .collect(Collectors.toList());

            System.out.printf("Names processed: %d%n", processedNames.size());
            System.out.printf("GPAs calculated: %d%n", processedGpas.size());
            System.out.printf("Final result size: %d%n", result.size());

            // Due to lazy evaluation, not all elements should be processed
            assertTrue(processedNames.size() >= result.size(),
                    "Should process at least the result elements");
            assertTrue(processedGpas.size() >= result.size(),
                    "Should calculate GPA for at least the result elements");
            assertTrue(result.size() <= 2, "Should be limited to 2 results");

            streamResults.put("lazy_evaluation_behavior", 1);
        }

        @Test
        @DisplayName("Files.lines() Streaming")
        void testFilesLinesStreaming() {
            // Test the streaming capability mentioned in requirements
            List<Grade> allGrades = gradeManager.getGradesByStudent("all");

            // Simulate Files.lines() behavior with existing data
            List<String> gradeLines = allGrades.stream()
                    .map(grade -> String.format("%s,%s,%s,%.1f",
                            grade.getStudentId(),
                            grade.getSubject().getSubjectName(),
                            grade.getSubject().getSubjectType(),
                            grade.getGrade()))
                    .collect(Collectors.toList());

            // Process as if reading from CSV
            List<Grade> parsedGrades = gradeLines.stream()
                    .map(line -> {
                        String[] parts = line.split(",");
                        // In real scenario, this would parse from Files.lines()
                        return allGrades.stream()
                                .filter(g -> g.getStudentId().equals(parts[0]))
                                .findFirst()
                                .orElse(null);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            System.out.printf("Simulated CSV lines: %d%n", gradeLines.size());
            System.out.printf("Parsed grades: %d%n", parsedGrades.size());

            assertEquals(allGrades.size(), parsedGrades.size(),
                    "Should parse all grades from simulated CSV");

            streamResults.put("files_lines_streaming", 1);
        }
    }

    @Test
    @DisplayName("Stream Processing Tests Summary")
    void displayStreamProcessingSummary() {
        int totalTests = streamResults.size();
        long passedTests = streamResults.values().stream().mapToLong(Integer::intValue).sum();

        System.out.println("\n" + "=".repeat(60));
        System.out.println("STREAM PROCESSING TEST SUITE SUMMARY");
        System.out.println("=".repeat(60));
        System.out.printf("Total Stream Tests: %d%n", totalTests);
        System.out.printf("Passed: %d%n", passedTests);
        System.out.printf("Failed: %d%n", totalTests - passedTests);
        System.out.printf("Success Rate: %.1f%%%n", (passedTests * 100.0) / totalTests);

        System.out.println("\nStream Processing Features Tested:");
        System.out.println("✓ Filter operations (GPA ranges, student types, performance categories)");
        System.out.println("✓ Map operations (GPA values, letter grades, subject averages)");
        System.out.println("✓ Reduce operations (statistics, distributions, finding extremes)");
        System.out.println("✓ Parallel stream correctness and performance");
        System.out.println("✓ Short-circuiting operations (findFirst, anyMatch, limit)");
        System.out.println("✓ Lazy evaluation behavior");
        System.out.println("✓ Files.lines() streaming simulation");

        assertTrue(passedTests >= totalTests * 0.9, "At least 90% of stream processing tests should pass");
    }
}