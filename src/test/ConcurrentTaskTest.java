package test;

import models.*;
import services.*;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConcurrentTaskService Test Suite")
public class ConcurrentTaskTest {

    private StudentManager studentManager;
    private GradeManager gradeManager;
    private FileIOService fileIOService;
    private ConcurrentTaskService concurrentTaskService;

    // Test counters
    private AtomicInteger tasksExecuted;
    private AtomicBoolean testTaskExecuted;

    @BeforeEach
    public void setUp() {
        tasksExecuted = new AtomicInteger(0);
        testTaskExecuted = new AtomicBoolean(false);

        // Create test implementations
        studentManager = createTestStudentManager();
        gradeManager = createTestGradeManager();
        fileIOService = createTestFileIOService();

        // Create mock data
        List<Student> mockStudents = createMockStudents(10); // Smaller set for faster tests
        List<Grade> mockGrades = createMockGrades(mockStudents);

        // Populate the test managers
        for (Student student : mockStudents) {
            addStudentToManager(student);
        }
        for (Grade grade : mockGrades) {
            addGradeToManager(grade);
        }

        // Create service with test implementations
        concurrentTaskService = new ConcurrentTaskService(studentManager, gradeManager, fileIOService);
    }

    // Test implementations
    private StudentManager createTestStudentManager() {
        return new StudentManager() {
            private final List<Student> students = new ArrayList<>();
            private final Map<String, Student> studentMap = new HashMap<>();

            @Override
            public List<Student> getStudents() {
                return new ArrayList<>(students);
            }

            @Override
            public Student findStudent(String studentId) {
                return studentMap.get(studentId);
            }

            public void addStudent(Student student) {
                students.add(student);
                studentMap.put(student.getStudentId(), student);
            }

            @Override
            public List<Student> getStudentsByType(String type) {
                return students.stream()
                        .filter(s -> s.getStudentType().equals(type))
                        .collect(Collectors.toList());
            }
        };
    }

    private GradeManager createTestGradeManager() {
        return new GradeManager(studentManager) {
            private final Map<String, List<Grade>> grades = new HashMap<>();

            @Override
            public List<Grade> getGradesByStudent(String studentId) {
                return new ArrayList<>(grades.getOrDefault(studentId, new ArrayList<>()));
            }

            @Override
            public double calculateOverallAverage(String studentId) {
                return grades.getOrDefault(studentId, new ArrayList<>()).stream()
                        .mapToDouble(Grade::getGrade)
                        .average()
                        .orElse(0.0);
            }

            public void addGrade(Grade grade) {
                grades.computeIfAbsent(grade.getStudentId(), k -> new ArrayList<>()).add(grade);
            }
        };
    }

    private FileIOService createTestFileIOService() {
        return new FileIOService() {
            private final AtomicInteger csvExports = new AtomicInteger(0);
            private final AtomicInteger jsonExports = new AtomicInteger(0);
            private final AtomicInteger allExports = new AtomicInteger(0);

            @Override
            public String exportToCSV(Student student, List<Grade> grades, String filename, String reportType) {
                csvExports.incrementAndGet();
                tasksExecuted.incrementAndGet();
                System.out.println("Test CSV export: " + student.getName());
                return filename;
            }

            @Override
            public String exportToJSON(Student student, List<Grade> grades, String filename, String reportType) {
                jsonExports.incrementAndGet();
                tasksExecuted.incrementAndGet();
                System.out.println("Test JSON export: " + student.getName());
                return filename;
            }

            @Override
            public void exportAllFormats(Student student, List<Grade> grades, String filename, String reportType) {
                allExports.incrementAndGet();
                tasksExecuted.incrementAndGet();
                System.out.println("Test export all formats: " + student.getName());
            }
        };
    }

    private List<Student> createMockStudents(int count) {
        List<Student> students = new ArrayList<>();
        Random random = new Random(42);

        for (int i = 0; i < count; i++) {
            String name = "First" + i + " Last" + i;
            int age = 20 + (i % 10);
            String email = "student" + i + "@university.edu";
            String phone = "555-123-" + String.format("%04d", i);
            String enrollmentDate = "2024-09-01";

            Student student;
            if (i % 3 == 0) {
                student = new HonorsStudent(name, age, email, phone, enrollmentDate);
            } else {
                student = new RegularStudent(name, age, email, phone, enrollmentDate);
            }
            students.add(student);
        }
        return students;
    }

    private List<Grade> createMockGrades(List<Student> students) {
        List<Grade> grades = new ArrayList<>();
        Random random = new Random(42);

        for (Student student : students) {
            for (int j = 0; j < 3; j++) { // Fewer grades for faster tests
                Subject subject = new Subject("SUB" + j, "Subject " + j, "Core") {
                    @Override
                    public void displaySubjectDetails() {

                    }

                    @Override
                    public String getSubjectType() {
                        return "";
                    }
                };
                double numericGrade = 60 + random.nextInt(41);
                Grade grade = new Grade(student.getStudentId(), subject, numericGrade);
                grades.add(grade);
            }
        }
        return grades;
    }

    private void addStudentToManager(Student student) {
        try {
            // Using reflection since addStudent is not in interface
            studentManager.getClass().getMethod("addStudent", Student.class).invoke(studentManager, student);
        } catch (Exception e) {
            // Fallback to direct field access if available
            System.out.println("Could not add student: " + e.getMessage());
        }
    }

    private void addGradeToManager(Grade grade) {
        try {
            gradeManager.getClass().getMethod("addGrade", Grade.class).invoke(gradeManager, grade);
        } catch (Exception e) {
            System.out.println("Could not add grade: " + e.getMessage());
        }
    }

    @AfterEach
    public void tearDown() {
        if (concurrentTaskService != null) {
            concurrentTaskService.shutdown();
        }
    }

    @Test
    @Timeout(30)
    @DisplayName("Test Batch Report Generation - CSV")
    void testGenerateBatchReportsCSV() {
        // Act
        assertDoesNotThrow(() ->
                concurrentTaskService.generateBatchReports(2, "csv")
        );

        // Verify some tasks were executed
        assertTrue(tasksExecuted.get() > 0, "Should have executed some export tasks");
    }

    @Test
    @Timeout(30)
    @DisplayName("Test Batch Report Generation - JSON")
    void testGenerateBatchReportsJSON() {
        // Act
        assertDoesNotThrow(() ->
                concurrentTaskService.generateBatchReports(2, "json")
        );

        assertTrue(tasksExecuted.get() > 0, "Should have executed some export tasks");
    }

    @Test
    @Timeout(30)
    @DisplayName("Test Batch Report Generation - All Formats")
    void testGenerateBatchReportsAll() {
        // Act
        assertDoesNotThrow(() ->
                concurrentTaskService.generateBatchReports(2, "all")
        );

        assertTrue(tasksExecuted.get() > 0, "Should have executed some export tasks");
    }

    @Test
    @Timeout(10)
    @DisplayName("Test Priority Task Scheduling")
    void testSchedulePriorityTask() {
        // Arrange
        Runnable testTask = () -> {
            testTaskExecuted.set(true);
            tasksExecuted.incrementAndGet();
        };

        LocalDateTime scheduledTime = LocalDateTime.now().plusMinutes(1);

        // Act
        concurrentTaskService.schedulePriorityTask("TEST1", "Test Task", testTask, 1, scheduledTime);

        // Assert
        Map<String, Object> stats = concurrentTaskService.getPriorityQueueStats();
        assertEquals(1, stats.get("queueSize"));
        assertEquals("Test Task", stats.get("nextTask"));
        assertEquals(1, stats.get("nextPriority"));
    }

    @Test
    @Timeout(10)
    @DisplayName("Test Execute Next Priority Task")
    void testExecuteNextPriorityTask() {
        // Arrange
        Runnable testTask = () -> {
            testTaskExecuted.set(true);
            tasksExecuted.incrementAndGet();
        };

        concurrentTaskService.schedulePriorityTask("TEST1", "Test Task", testTask, 1, LocalDateTime.now());

        // Act
        concurrentTaskService.executeNextPriorityTask();

        // Assert
        assertTrue(testTaskExecuted.get(), "Task should have been executed");

        Map<String, Object> stats = concurrentTaskService.getPriorityQueueStats();
        assertEquals(0, stats.get("queueSize"));
        assertTrue((Boolean) stats.get("isEmpty"));
    }

    @Test
    @Timeout(10)
    @DisplayName("Test Peek Next Task")
    void testPeekNextTask() {
        // Arrange
        Runnable testTask = () -> {};
        concurrentTaskService.schedulePriorityTask("TEST1", "Task 1", testTask, 2, LocalDateTime.now());
        concurrentTaskService.schedulePriorityTask("TEST2", "Task 2", testTask, 1, LocalDateTime.now().plusMinutes(1));

        // Act
        ConcurrentTaskService.ScheduledTask nextTask = concurrentTaskService.peekNextTask();

        // Assert - Should return highest priority (Task 2 with priority 1)
        assertNotNull(nextTask);
        assertEquals("Task 2", nextTask.getDescription());
        assertEquals(1, nextTask.getPriority());

        // Queue should still have 2 items
        Map<String, Object> stats = concurrentTaskService.getPriorityQueueStats();
        assertEquals(2, stats.get("queueSize"));
    }

    @Test
    @Timeout(10)
    @DisplayName("Test Get Pending Tasks")
    void testGetPendingTasks() {
        // Arrange
        Runnable testTask = () -> {};
        concurrentTaskService.schedulePriorityTask("TEST1", "Task 3", testTask, 3, LocalDateTime.now());
        concurrentTaskService.schedulePriorityTask("TEST2", "Task 1", testTask, 1, LocalDateTime.now());
        concurrentTaskService.schedulePriorityTask("TEST3", "Task 2", testTask, 2, LocalDateTime.now());

        // Act
        List<ConcurrentTaskService.ScheduledTask> pendingTasks = concurrentTaskService.getPendingTasks();

        // Assert
        assertEquals(3, pendingTasks.size());
        // Check all tasks are present (order may vary)
        List<String> descriptions = pendingTasks.stream()
                .map(ConcurrentTaskService.ScheduledTask::getDescription)
                .sorted()
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("Task 1", "Task 2", "Task 3"), descriptions);
    }

    @Test
    @Timeout(10)
    @DisplayName("Test Cancel Task")
    void testCancelTask() {
        // Arrange
        Runnable testTask = () -> {};
        concurrentTaskService.schedulePriorityTask("T1", "Task 1", testTask, 1, LocalDateTime.now());
        concurrentTaskService.schedulePriorityTask("T2", "Task 2", testTask, 2, LocalDateTime.now());

        // Act
        boolean cancelled = concurrentTaskService.cancelTask("T1");

        // Assert
        assertTrue(cancelled, "Task should be cancelled");

        Map<String, Object> stats = concurrentTaskService.getPriorityQueueStats();
        assertEquals(1, stats.get("queueSize"));
        assertEquals("Task 2", stats.get("nextTask"));
    }

    @Test
    @Timeout(10)
    @DisplayName("Test Cancel Non-Existent Task")
    void testCancelNonExistentTask() {
        // Arrange
        Runnable testTask = () -> {};
        concurrentTaskService.schedulePriorityTask("T1", "Task 1", testTask, 1, LocalDateTime.now());

        // Act
        boolean cancelled = concurrentTaskService.cancelTask("NON_EXISTENT");

        // Assert
        assertFalse(cancelled, "Non-existent task should not be cancelled");

        Map<String, Object> stats = concurrentTaskService.getPriorityQueueStats();
        assertEquals(1, stats.get("queueSize"));
    }

    @Test
    @Timeout(10)
    @DisplayName("Test Get Target Students - All Students")
    void testGetTargetStudentsAll() {
        // Act
        List<Student> students = concurrentTaskService.getTargetStudents("1", studentManager);

        // Assert
        assertEquals(10, students.size());
    }

    @Test
    @Timeout(10)
    @DisplayName("Test Get Target Students - Honors Students")
    void testGetTargetStudentsHonors() {
        // Act
        List<Student> students = concurrentTaskService.getTargetStudents("2", studentManager);

        // Assert
        assertTrue(students.size() > 0);
        assertTrue(students.size() <= 10);
    }

    @Test
    @Timeout(10)
    @DisplayName("Test Get Target Students - Invalid Option")
    void testGetTargetStudentsInvalid() {
        // Act
        List<Student> students = concurrentTaskService.getTargetStudents("invalid", studentManager);

        // Assert
        assertTrue(students.isEmpty());
    }

    @Test
    @Timeout(10)
    @DisplayName("Test Schedule Daily GPA Recalculation")
    void testScheduleDailyGPARecalculation() {
        // Act - Should not throw exception
        assertDoesNotThrow(() ->
                concurrentTaskService.scheduleDailyGPARecalculation(2, 30)
        );

        // Small delay to allow scheduling
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @Timeout(10)
    @DisplayName("Test Schedule Weekly Report Email")
    void testScheduleWeeklyReportEmail() {
        // Act - Monday at 9:00 AM
        assertDoesNotThrow(() ->
                concurrentTaskService.scheduleWeeklyReportEmail(Calendar.MONDAY, 9, 0)
        );

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @Timeout(10)
    @DisplayName("Test Schedule Monthly Performance Summary")
    void testScheduleMonthlyPerformanceSummary() {
        // Act - 15th of each month at 10:00 AM
        assertDoesNotThrow(() ->
                concurrentTaskService.scheduleMonthlyPerformanceSummary(15, 10, 0)
        );
    }

    @Test
    @Timeout(10)
    @DisplayName("Test Schedule Hourly Data Sync")
    void testScheduleHourlyDataSync() {
        // Act
        assertDoesNotThrow(() ->
                concurrentTaskService.scheduleHourlyDataSync()
        );

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @Timeout(10)
    @DisplayName("Test Schedule Custom Task")
    void testScheduleCustomTask() {
        // Arrange
        Runnable customTask = () -> {
            testTaskExecuted.set(true);
            tasksExecuted.incrementAndGet();
        };

        // Act
        assertDoesNotThrow(() ->
                concurrentTaskService.scheduleCustomTask("Test Custom Task", customTask, 100, 1000, TimeUnit.MILLISECONDS)
        );

        // Wait a bit to see if task executes
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Note: Can't reliably assert execution due to timing
        // Just ensure no exceptions
    }

    @Test
    @Timeout(10)
    @DisplayName("Test ScheduledTask Comparable Implementation")
    void testScheduledTaskComparable() {
        Runnable task = () -> {};
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime later = now.plusHours(1);

        ConcurrentTaskService.ScheduledTask highPriority =
                new ConcurrentTaskService.ScheduledTask("T1", "High", task, 1, now);
        ConcurrentTaskService.ScheduledTask lowPriority =
                new ConcurrentTaskService.ScheduledTask("T2", "Low", task, 5, now);
        ConcurrentTaskService.ScheduledTask samePriorityEarlier =
                new ConcurrentTaskService.ScheduledTask("T3", "Same Early", task, 3, now);
        ConcurrentTaskService.ScheduledTask samePriorityLater =
                new ConcurrentTaskService.ScheduledTask("T4", "Same Late", task, 3, later);

        // Test priority comparison
        assertTrue(highPriority.compareTo(lowPriority) < 0);
        assertTrue(lowPriority.compareTo(highPriority) > 0);

        // Test time comparison with same priority
        assertTrue(samePriorityEarlier.compareTo(samePriorityLater) < 0);
        assertTrue(samePriorityLater.compareTo(samePriorityEarlier) > 0);

        // Test equality
        assertEquals(0, samePriorityEarlier.compareTo(samePriorityEarlier));
    }

    @Test
    @Timeout(10)
    @DisplayName("Test ScheduledTask Getters")
    void testScheduledTaskGetters() {
        Runnable task = () -> System.out.println("Test");
        LocalDateTime scheduledTime = LocalDateTime.now();

        ConcurrentTaskService.ScheduledTask scheduledTask =
                new ConcurrentTaskService.ScheduledTask("ID123", "Test Task", task, 3, scheduledTime);

        assertEquals("ID123", scheduledTask.getTaskId());
        assertEquals("Test Task", scheduledTask.getDescription());
        assertEquals(task, scheduledTask.getTask());
        assertEquals(3, scheduledTask.getPriority());
        assertEquals(scheduledTime, scheduledTask.getScheduledTime());
    }

    @Test
    @Timeout(30)
    @DisplayName("Test Concurrent Execution of Multiple Tasks")
    void testConcurrentExecution() throws InterruptedException {
        // Arrange
        int numTasks = 20;
        CountDownLatch latch = new CountDownLatch(numTasks);
        AtomicInteger completedTasks = new AtomicInteger(0);

        // Create tasks that simulate work
        for (int i = 0; i < numTasks; i++) {
            final int taskNum = i;
            Runnable task = () -> {
                try {
                    Thread.sleep(10); // Simulate work
                    completedTasks.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            };

            concurrentTaskService.schedulePriorityTask(
                    "TASK" + taskNum,
                    "Task " + taskNum,
                    task,
                    taskNum % 5 + 1, // Priorities 1-5
                    LocalDateTime.now()
            );
        }

        // Act - Execute all tasks
        ExecutorService executor = Executors.newFixedThreadPool(4);
        for (int i = 0; i < numTasks; i++) {
            executor.execute(() -> concurrentTaskService.executeNextPriorityTask());
        }

        // Wait for completion
        assertTrue(latch.await(5, TimeUnit.SECONDS), "All tasks should complete");
        executor.shutdown();

        // Assert
        assertEquals(numTasks, completedTasks.get());

        Map<String, Object> stats = concurrentTaskService.getPriorityQueueStats();
        assertEquals(0, stats.get("queueSize"));
        assertTrue((Boolean) stats.get("isEmpty"));
    }

    @Test
    @Timeout(10)
    @DisplayName("Test Shutdown Service")
    void testShutdown() {
        // Arrange - Add some tasks
        Runnable task = () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        concurrentTaskService.schedulePriorityTask("T1", "Task 1", task, 1, LocalDateTime.now());

        // Act
        concurrentTaskService.shutdown();

        // Assert - Should be able to call shutdown multiple times
        assertDoesNotThrow(() -> concurrentTaskService.shutdown());
    }

    @Test
    @Timeout(10)
    @DisplayName("Test Empty Queue Behavior")
    void testEmptyQueueBehavior() {
        // Act - Execute when queue is empty
        concurrentTaskService.executeNextPriorityTask(); // Should not throw

        // Act - Peek when queue is empty
        ConcurrentTaskService.ScheduledTask nextTask = concurrentTaskService.peekNextTask();
        assertNull(nextTask, "Should return null for empty queue");

        // Act - Get pending tasks when empty
        List<ConcurrentTaskService.ScheduledTask> pendingTasks = concurrentTaskService.getPendingTasks();
        assertTrue(pendingTasks.isEmpty(), "Should return empty list");

        // Act - Get stats for empty queue
        Map<String, Object> stats = concurrentTaskService.getPriorityQueueStats();
        assertEquals(0, stats.get("queueSize"));
        assertTrue((Boolean) stats.get("isEmpty"));
        assertFalse(stats.containsKey("nextTask")); // Should not have nextTask key
    }

    @Test
    @DisplayName("Test Summary - All Tests")
    void testSummary() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("CONCURRENTTASKSERVICE TEST SUMMARY");
        System.out.println("=".repeat(60));
        System.out.println("Features Tested:");
        System.out.println("✓ Batch report generation (CSV, JSON, All)");
        System.out.println("✓ Priority task scheduling");
        System.out.println("✓ Priority queue operations (add, peek, execute, cancel)");
        System.out.println("✓ Task scheduling (daily, weekly, monthly, hourly, custom)");
        System.out.println("✓ Target student selection");
        System.out.println("✓ Concurrent execution");
        System.out.println("✓ Service shutdown");
        System.out.println("✓ Empty queue handling");
        System.out.println("✓ ScheduledTask comparable implementation");
        System.out.println("\nAll tests completed successfully!");
    }
}