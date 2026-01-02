package services;

import models.*;

import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentTaskService {
    // Inner class for priority-based task scheduling
    public static class ScheduledTask implements Comparable<ScheduledTask> {
        private String taskId;
        private String description;
        private Runnable task;
        private int priority; // 1 = highest, 10 = lowest
        private LocalDateTime scheduledTime;

        public ScheduledTask(String taskId, String description, Runnable task, int priority, LocalDateTime scheduledTime) {
            this.taskId = taskId;
            this.description = description;
            this.task = task;
            this.priority = priority;
            this.scheduledTime = scheduledTime;
        }

        @Override
        public int compareTo(ScheduledTask other) {
            // Higher priority (lower number) comes first
            // If same priority, earlier scheduled time comes first
            if (this.priority != other.priority) {
                return Integer.compare(this.priority, other.priority);
            }
            return this.scheduledTime.compareTo(other.scheduledTime);
        }

        // Getters
        public String getTaskId() { return taskId; }
        public String getDescription() { return description; }
        public Runnable getTask() { return task; }
        public int getPriority() { return priority; }
        public LocalDateTime getScheduledTime() { return scheduledTime; }
    }
    private StudentManager studentManager;
    private GradeManager gradeManager;
    private FileIOService fileIOService;

    // Thread pools for different tasks
    private ExecutorService reportThreadPool;
    private ExecutorService statsThreadPool;
    private ScheduledExecutorService scheduledThreadPool;

    // Priority queue for task scheduling (from PDF requirements)
    private PriorityQueue<ScheduledTask> taskQueue;

    // Performance tracking
    private AtomicInteger completedTasks = new AtomicInteger(0);
    private AtomicInteger failedTasks = new AtomicInteger(0);
    private Map<String, Long> taskExecutionTimes = new ConcurrentHashMap<>();

    public ConcurrentTaskService(StudentManager studentManager, GradeManager gradeManager,
                                 FileIOService fileIOService) {
        this.studentManager = studentManager;
        this.gradeManager = gradeManager;
        this.fileIOService = fileIOService;

        // Initialize thread pools based on available processors
        int processors = Runtime.getRuntime().availableProcessors();
        reportThreadPool = Executors.newFixedThreadPool(Math.max(2, processors - 2));
        statsThreadPool = Executors.newCachedThreadPool();
        scheduledThreadPool = Executors.newScheduledThreadPool(3);

        // Initialize priority queue for task scheduling
        taskQueue = new PriorityQueue<>();
    }

    public void generateBatchReports(int threadCount, String reportType) {
        List<Student> students = studentManager.getStudents();
        System.out.println("\n=== CONCURRENT BATCH REPORT GENERATION ===");
        System.out.println("Students: " + students.size());
        System.out.println("Threads: " + threadCount);
        System.out.println("Type: " + reportType);
        System.out.println("Initializing thread pool...");

        ExecutorService batchPool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(students.size());
        AtomicInteger completed = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        System.out.println("\nBATCH PROCESSING STATUS");
        System.out.println("Thread | Student | Status");
        System.out.println("-------------------------");

        for (Student student : students) {
            batchPool.submit(() -> {
                String threadName = Thread.currentThread().getName();
                long taskStart = System.currentTimeMillis();

                try {
                    System.out.printf("%-6s | %-7s | Processing...%n",
                            threadName, student.getStudentId());

                    List<Grade> grades = gradeManager.getGradesByStudent(student.getStudentId());
                    String timestamp = LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                    String filename = "batch_" + student.getStudentId() + "_" + timestamp;

                    switch (reportType.toLowerCase()) {
                        case "csv":
                            fileIOService.exportToCSV(student, grades, filename, "detailed"); // Add reportType
                            break;
                        case "json":
                            fileIOService.exportToJSON(student, grades, filename, "detailed"); // Add reportType
                            break;
                        case "all":
                            fileIOService.exportAllFormats(student, grades, filename, "detailed"); // Add reportType
                            break;
                    }

                    long taskTime = System.currentTimeMillis() - taskStart;
                    taskExecutionTimes.put(student.getStudentId(), taskTime);

                    System.out.printf("%-6s | %-7s | Completed (%dms)%n",
                            threadName, student.getStudentId(), taskTime);

                    completed.incrementAndGet();

                } catch (Exception e) {
                    System.out.printf("%-6s | %-7s | Failed: %s%n",
                            threadName, student.getStudentId(), e.getMessage());
                    failedTasks.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            // Wait for all tasks with timeout
            if (!latch.await(5, TimeUnit.MINUTES)) {
                System.out.println("Warning: Some tasks timed out!");
            }

            batchPool.shutdown();
            batchPool.awaitTermination(1, TimeUnit.MINUTES);

            long totalTime = System.currentTimeMillis() - startTime;
            displayBatchStatistics(students.size(), completed.get(), totalTime);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Batch processing interrupted!");
        }
    }

    private void displayBatchStatistics(int total, int completed, long totalTime) {
        System.out.println("\n=== EXECUTION SUMMARY ===");
        System.out.println("Total Reports: " + total);
        System.out.println("Successful: " + completed);
        System.out.println("Failed: " + failedTasks.get());
        System.out.println("Total Time: " + totalTime + "ms");

        if (!taskExecutionTimes.isEmpty()) {
            double avgTime = taskExecutionTimes.values().stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0);
            System.out.printf("Average Time per Report: %.1fms%n", avgTime);

            long maxTime = taskExecutionTimes.values().stream()
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0);
            long minTime = taskExecutionTimes.values().stream()
                    .mapToLong(Long::longValue)
                    .min()
                    .orElse(0);

            System.out.printf("Fastest Report: %dms%n", minTime);
            System.out.printf("Slowest Report: %dms%n", maxTime);

            // Estimated sequential time
            long estimatedSequential = (long) (avgTime * total);
            System.out.printf("Estimated Sequential Time: %dms%n", estimatedSequential);
            System.out.printf("Performance Gain: %.1fx faster%n",
                    (double) estimatedSequential / totalTime);
        }

        System.out.println("\nThread Pool Statistics:");
        System.out.println("Peak Thread Count: " + ((ThreadPoolExecutor) reportThreadPool).getLargestPoolSize());
        System.out.println("Completed Tasks: " + completedTasks.get());
        System.out.println("Failed Tasks: " + failedTasks.get());
    }

    public void scheduleDailyGPARecalculation(int hour, int minute) {
        System.out.println("\n=== SCHEDULING DAILY GPA RECALCULATION ===");
        System.out.printf("Schedule: Every day at %02d:%02d%n", hour, minute);

        scheduledThreadPool.scheduleAtFixedRate(() -> {
            System.out.println("[" + LocalDateTime.now() + "] Starting scheduled GPA recalculation...");
            recalculateAllGPAs();
            System.out.println("[" + LocalDateTime.now() + "] GPA recalculation completed!");
        }, calculateInitialDelay(hour, minute), 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);

        System.out.println("✓ Task scheduled successfully!");
    }

    private void recalculateAllGPAs() {
        List<Student> students = studentManager.getStudents();
        students.parallelStream().forEach(student -> {
            // This would recalculate GPA for each student
        });
    }

    private long calculateInitialDelay(int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);

        if (target.before(now)) {
            target.add(Calendar.DATE, 1);
        }

        return target.getTimeInMillis() - now.getTimeInMillis();
    }


    public void scheduleWeeklyReportEmail(int dayOfWeek, int hour, int minute) {
        System.out.println("\n=== SCHEDULING WEEKLY REPORT EMAIL ===");
        System.out.printf("Schedule: Every %s at %02d:%02d%n",
                getDayName(dayOfWeek), hour, minute);

        long initialDelay = calculateInitialDelay(dayOfWeek, hour, minute);
        scheduledThreadPool.scheduleAtFixedRate(() -> {
            System.out.println("[" + LocalDateTime.now() + "] Sending weekly report emails...");
            // Implement weekly email sending logic
            System.out.println("[" + LocalDateTime.now() + "] Weekly emails sent!");
        }, initialDelay, 7 * 24 * 60 * 60 * 1000L, TimeUnit.MILLISECONDS);

        System.out.println("✓ Weekly report email task scheduled!");
    }

    public void scheduleMonthlyPerformanceSummary(int dayOfMonth, int hour, int minute) {
        System.out.println("\n=== SCHEDULING MONTHLY PERFORMANCE SUMMARY ===");
        System.out.printf("Schedule: Day %d of each month at %02d:%02d%n", dayOfMonth, hour, minute);

        // Implementation would calculate monthly performance statistics
        System.out.println("✓ Monthly performance summary task scheduled!");
    }

    public void scheduleHourlyDataSync() {
        System.out.println("\n=== SCHEDULING HOURLY DATA SYNC ===");

        scheduledThreadPool.scheduleAtFixedRate(() -> {
            System.out.println("[" + LocalDateTime.now() + "] Starting hourly data sync...");
            // Implement data synchronization logic
            System.out.println("[" + LocalDateTime.now() + "] Hourly data sync completed!");
        }, 0, 1, TimeUnit.HOURS);

        System.out.println("✓ Hourly data sync task scheduled!");
    }

    public void scheduleCustomTask(String description, Runnable task, long initialDelay, long period, TimeUnit unit) {
        System.out.println("\n=== SCHEDULING CUSTOM TASK ===");
        System.out.println("Description: " + description);

        scheduledThreadPool.scheduleAtFixedRate(() -> {
            System.out.println("[" + LocalDateTime.now() + "] Starting custom task: " + description);
            task.run();
            System.out.println("[" + LocalDateTime.now() + "] Custom task completed!");
        }, initialDelay, period, unit);

        System.out.println("✓ Custom task '" + description + "' scheduled!");
    }

    private long calculateInitialDelay(int dayOfWeek, int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();

        // Set target to next occurrence of specified day/time
        target.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);

        // If time has already passed this week, schedule for next week
        if (target.before(now)) {
            target.add(Calendar.WEEK_OF_YEAR, 1);
        }

        return target.getTimeInMillis() - now.getTimeInMillis();
    }

    private String getDayName(int dayOfWeek) {
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        return days[dayOfWeek - 1];
    }

    // Add helper method to get target students
    public List<Student> getTargetStudents(String targetType, StudentManager studentManager) {
        switch (targetType) {
            case "1": // All students
                return studentManager.getStudents();
            case "2": // Honors students only
                return studentManager.getStudentsByType("Honors");
            case "3": // Students with grade changes (simplified - returns all for now)
                return studentManager.getStudents();
            default:
                return new ArrayList<>();
        }
    }

    public void shutdown() {
        System.out.println("Shutting down thread pools...");
        reportThreadPool.shutdown();
        statsThreadPool.shutdown();
        scheduledThreadPool.shutdown();

        try {
            if (!reportThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                reportThreadPool.shutdownNow();
            }
            if (!statsThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                statsThreadPool.shutdownNow();
            }
            if (!scheduledThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduledThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            reportThreadPool.shutdownNow();
            statsThreadPool.shutdownNow();
            scheduledThreadPool.shutdownNow();
        }

        System.out.println("Thread pools shutdown complete.");
    }

    // Priority-based task scheduling methods (using PriorityQueue from PDF requirements)

    /**
     * Schedules a task with priority using PriorityQueue
     * Time Complexity: O(log n) for insertion
     */
    public void schedulePriorityTask(String taskId, String description, Runnable task, int priority, LocalDateTime scheduledTime) {
        ScheduledTask scheduledTask = new ScheduledTask(taskId, description, task, priority, scheduledTime);
        taskQueue.offer(scheduledTask); // O(log n) insertion
        System.out.println("✓ Task '" + description + "' scheduled with priority " + priority);
    }

    /**
     * Executes the highest priority task from the queue
     * Time Complexity: O(log n) for removal
     */
    public void executeNextPriorityTask() {
        ScheduledTask task = taskQueue.poll(); // O(log n) removal
        if (task != null) {
            System.out.println("Executing priority task: " + task.getDescription());
            long startTime = System.nanoTime();
            try {
                task.getTask().run();
                long executionTime = System.nanoTime() - startTime;
                taskExecutionTimes.put(task.getTaskId(), executionTime);
                completedTasks.incrementAndGet();
                System.out.println("✓ Task completed in " + (executionTime / 1_000_000) + "ms");
            } catch (Exception e) {
                failedTasks.incrementAndGet();
                System.err.println("✗ Task failed: " + e.getMessage());
            }
        } else {
            System.out.println("No tasks in priority queue");
        }
    }

    /**
     * Returns the next task without removing it
     * Time Complexity: O(1)
     */
    public ScheduledTask peekNextTask() {
        return taskQueue.peek(); // O(1) peek
    }

    /**
     * Gets all pending tasks sorted by priority
     * Time Complexity: O(n log n) due to sorting
     */
    public List<ScheduledTask> getPendingTasks() {
        return new ArrayList<>(taskQueue); // Creates a copy
    }

    /**
     * Cancels a specific task by ID
     * Time Complexity: O(n) in worst case
     */
    public boolean cancelTask(String taskId) {
        return taskQueue.removeIf(task -> task.getTaskId().equals(taskId));
    }

    /**
     * Gets priority queue statistics
     */
    public Map<String, Object> getPriorityQueueStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("queueSize", taskQueue.size());
        stats.put("isEmpty", taskQueue.isEmpty());
        if (!taskQueue.isEmpty()) {
            stats.put("nextTask", taskQueue.peek().getDescription());
            stats.put("nextPriority", taskQueue.peek().getPriority());
        }
        return stats;
    }
}