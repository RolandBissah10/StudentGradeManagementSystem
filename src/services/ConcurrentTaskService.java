package services;

import models.*;
import exceptions.ExportException;

import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentTaskService {
    private StudentManager studentManager;
    private GradeManager gradeManager;
    private FileIOService fileIOService;

    // Thread pools for different tasks
    private ExecutorService reportThreadPool;
    private ExecutorService statsThreadPool;
    private ScheduledExecutorService scheduledThreadPool;

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
}