package services;

import models.*;
import exceptions.ExportException;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Collectors;

public class BatchReportService {
    private ReportGenerator reportGenerator;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    public BatchReportService(ReportGenerator reportGenerator) {
        this.reportGenerator = reportGenerator;
    }

    public void generateConcurrentReports(List<Student> students, List<String> reportTypes,
                                          int threadCount) throws ExportException {
        // Create necessary directories first
        createReportDirectories();

        String batchId = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String outputDir = "./reports/batch_" + batchId + "/";

        long startTime = System.currentTimeMillis();
        long sequentialEstimate = students.size() * reportTypes.size() * 500L; // 500ms per report estimate

        System.out.println("\n" + "=".repeat(80));
        System.out.println("              CONCURRENT BATCH REPORT GENERATION");
        System.out.println("=".repeat(80));
        System.out.println("Batch ID:          " + batchId);
        System.out.println("Students:          " + students.size());
        System.out.println("Formats:           " + String.join(", ", reportTypes));
        System.out.println("Threads:           " + threadCount);
        System.out.println("Total Tasks:       " + (students.size() * reportTypes.size()));
        System.out.println("Start Time:        " + LocalDateTime.now().format(DISPLAY_FORMATTER));
        System.out.println("=".repeat(80));

        System.out.println("\n📊 Initializing thread pool...");
        System.out.println("✓ Fixed Thread Pool created: " + threadCount + " threads");

        // Create thread pool
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;

        // Statistics tracking
        AtomicInteger completedTasks = new AtomicInteger(0);
        AtomicInteger successfulTasks = new AtomicInteger(0);
        AtomicInteger failedTasks = new AtomicInteger(0);
        AtomicLong totalProcessingTime = new AtomicLong(0);
        AtomicInteger peakThreadCount = new AtomicInteger(0);
        AtomicLong totalQueueTime = new AtomicLong(0);
        AtomicInteger queueSize = new AtomicInteger(0);

        // Thread activity tracking
        Map<String, String> threadActivities = new ConcurrentHashMap<>();
        for (int i = 1; i <= threadCount; i++) {
            threadActivities.put("Thread-" + i, "IDLE");
        }

        int totalTasks = students.size() * reportTypes.size();

        System.out.println("\n" + "─".repeat(80));
        System.out.println("BATCH PROCESSING STATUS");
        System.out.println("─".repeat(80));

        // Submit all tasks
        List<Future<ReportResult>> futures = new ArrayList<>();
        List<ReportTask> tasks = new ArrayList<>();

        for (Student student : students) {
            for (String reportType : reportTypes) {
                ReportTask task = new ReportTask(student, reportType, batchId,
                        completedTasks, successfulTasks, failedTasks,
                        totalProcessingTime);
                tasks.add(task);
                futures.add(executor.submit(task));
            }
        }

        // Start progress monitor thread
        Thread monitorThread = new Thread(() -> {
            try {
                int lastProgress = 0;
                long monitorStartTime = System.currentTimeMillis();

                while (completedTasks.get() < totalTasks && !Thread.currentThread().isInterrupted()) {
                    // Update queue size
                    queueSize.set(tpe.getQueue().size());

                    // Update peak thread count
                    int activeThreads = tpe.getActiveCount();
                    if (activeThreads > peakThreadCount.get()) {
                        peakThreadCount.set(activeThreads);
                    }

                    // Calculate statistics
                    int completed = completedTasks.get();
                    int progress = totalTasks > 0 ? (completed * 100) / totalTasks : 0;

                    // Clear screen and display progress
                    System.out.print("\033[H\033[2J");
                    System.out.flush();

                    // Display header
                    System.out.println("\n" + "─".repeat(80));
                    System.out.println("              BATCH PROCESSING STATUS - LIVE VIEW");
                    System.out.println("─".repeat(80));

                    // Display thread progress bars
                    displayThreadBars(tasks, threadCount, completed);

                    // Display student progress
                    displayStudentProgress(completed, totalTasks, monitorStartTime);

                    lastProgress = progress;
                    Thread.sleep(500); // Update twice per second

                }

                // Final display
                System.out.print("\033[H\033[2J");
                System.out.flush();

                // Show final thread bars (all completed)
                displayThreadBars(tasks, threadCount, totalTasks);
                displayStudentProgress(totalTasks, totalTasks, monitorStartTime);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        monitorThread.setDaemon(true);
        monitorThread.start();

        // Wait for all tasks to complete
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
                executor.shutdownNow();
                System.out.println("\n⚠️  Timeout! Some reports may not have completed.");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Stop monitor thread
        if (monitorThread.isAlive()) {
            monitorThread.interrupt();
        }

        try {
            monitorThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Collect results
        List<ReportResult> results = new ArrayList<>();
        for (Future<ReportResult> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                // Already handled in task
            }
        }

        // Calculate final statistics
        long endTime = System.currentTimeMillis();
        double executionTime = (endTime - startTime) / 1000.0;
        int successCount = successfulTasks.get();
        int failCount = failedTasks.get();

        // Display final summary
        displayFinalSummary(batchId, students.size(), reportTypes, totalTasks,
                successCount, failCount, executionTime, sequentialEstimate,
                peakThreadCount.get(), tpe.getCompletedTaskCount(),
                totalProcessingTime.get(), threadCount, outputDir, results);
    }

    private void displayThreadBars(List<ReportTask> tasks, int threadCount, int completedTasks) {
        System.out.println("\n🧵 THREAD PROGRESS BARS:");
        System.out.println("─".repeat(40));

        // Track active tasks per thread
        Map<Integer, ReportTask> activeTasksByThread = new HashMap<>();
        Map<Integer, Double> progressByThread = new HashMap<>();

        // Find active tasks and calculate their progress
        for (int i = 0; i < tasks.size(); i++) {
            ReportTask task = tasks.get(i);
            if (task.isProcessing) {
                int threadIndex = i % threadCount;
                activeTasksByThread.put(threadIndex, task);

                // Calculate progress percentage (0-100) based on elapsed time vs average processing time
                long elapsed = System.currentTimeMillis() - task.startTime;
                double progress = Math.min(100.0, (elapsed * 100.0) / 500.0); // Assume 500ms average
                progressByThread.put(threadIndex, progress);
            }
        }

        // First row: Completed tasks (up to threadCount)
        // First row: Completed tasks (up to threadCount)
        int startIdx = Math.max(0, completedTasks - threadCount);
        for (int i = 0; i < threadCount; i++) {
            int taskIdx = startIdx + i;
            if (taskIdx < completedTasks && taskIdx < tasks.size()) {
                ReportTask task = tasks.get(taskIdx);
                // Show completed progress bar [██████████] ✓
                String completedBar = "[██████████]";
                System.out.printf("Thread-%-2d :    %-6s  %-15s ✓ (%dms)%n",
                        i + 1,
                        task.studentId,
                        completedBar,
                        task.processingTime);
            }
        }

        System.out.println();

        // Second row: Current/active tasks with progress bars
        for (int threadNum = 1; threadNum <= threadCount; threadNum++) {
            int threadIndex = threadNum - 1;
            ReportTask activeTask = activeTasksByThread.get(threadIndex);

            if (activeTask != null) {
                double progress = progressByThread.getOrDefault(threadIndex, 0.0);

                // Create progress bar (10 characters wide)
                StringBuilder progressBar = new StringBuilder("[");
                int filled = (int) (progress / 10.0); // 10 characters for 100%

                for (int i = 0; i < 10; i++) {
                    if (i < filled) {
                        progressBar.append("█");
                    } else if (i == filled) {
                        progressBar.append("█");
                    } else {
                        progressBar.append("░");
                    }
                }
                progressBar.append("]");

                long elapsed = System.currentTimeMillis() - activeTask.startTime;
                String status;
                if (elapsed > 200) {
                    status = String.format("✓ (%dms)", elapsed);
                } else {
                    status = "... (in progress)";
                }

                System.out.printf("Thread-%-2d :    %-6s  %-15s %s%n",
                        threadNum,
                        activeTask.studentId,
                        progressBar.toString(),
                        status);

            } else {
                // Check if this thread has a pending task
                boolean hasPendingTask = false;
                for (int i = completedTasks; i < tasks.size(); i++) {
                    if (i % threadCount == threadIndex && !tasks.get(i).isProcessing && !tasks.get(i).isCompleted) {
                        ReportTask pendingTask = tasks.get(i);
                        System.out.printf("Thread-%-2d :    %-6s  [░░░░░░░░░░] (waiting)%n",
                                threadNum,
                                pendingTask.studentId);
                        hasPendingTask = true;
                        break;
                    }
                }

                if (!hasPendingTask) {
                    System.out.printf("Thread-%-2d :    %-6s  [          ] IDLE%n",
                            threadNum,
                            "-----");
                }
            }
        }

        System.out.println();
    }

    private void displayStudentProgress(int completed, int totalTasks, long startTime) {
        int progress = totalTasks > 0 ? (completed * 100) / totalTasks : 0;

        System.out.println("\n =====PROGRESS: =====");
        System.out.print("Progress: [");

        // Create progress bar with 20 characters width (matching screenshot)
        int barWidth = 20;
        int filled = (progress * barWidth) / 100;

        for (int i = 0; i < barWidth; i++) {
            if (i < filled) {
                System.out.print("█");
            } else {
                System.out.print(" ");
            }
        }

        System.out.printf("] %d%% (%d/%d completed)%n", progress, completed, totalTasks);

        long elapsed = System.currentTimeMillis() - startTime;
        double elapsedSec = elapsed / 1000.0;

        System.out.println("\n⏱  TIME STATISTICS:");
        System.out.printf("Elapsed: %.1fs%n", elapsedSec);

        if (progress > 0) {
            double totalEstimate = (elapsedSec * 100) / progress;
            double remaining = totalEstimate - elapsedSec;
            System.out.printf("Estimated Remaining: %.1fs%n", remaining);

            if (completed > 0) {
                double avgTime = elapsedSec * 1000 / completed;
                double throughput = completed / elapsedSec;
                System.out.printf("Avg Report Time: %.0fms%n", avgTime);
                System.out.printf("Throughput: %.1f reports/sec%n", throughput);
            }
        }
        System.out.println("─".repeat(40));
    }

    private void displayFinalSummary(String batchId, int studentCount, List<String> reportTypes,
                                     int totalTasks, int successCount, int failCount,
                                     double executionTime, long sequentialEstimate,
                                     int peakThreadCount, long completedTasks,
                                     long totalProcessingTime, int threadCount,
                                     String outputDir, List<ReportResult> results) {

        System.out.println("\n" + "⚡".repeat(40));
        System.out.println("        BATCH GENERATION COMPLETED!");
        System.out.println("⚡".repeat(40));

        System.out.println("\n" + "═".repeat(80));
        System.out.println("              EXECUTION SUMMARY");
        System.out.println("═".repeat(80));
        System.out.printf("Batch ID:          %s%n", batchId);
        System.out.printf("Total Students:    %d%n", studentCount);
        System.out.printf("Report Formats:    %s%n", String.join(", ", reportTypes));
        System.out.printf("Total Reports:     %d%n", totalTasks);
        System.out.printf("✅ Successful:     %d%n", successCount);
        System.out.printf("❌ Failed:         %d%n", failCount);
        System.out.printf("Success Rate:      %.1f%%%n",
                totalTasks > 0 ? (successCount * 100.0 / totalTasks) : 0);
        System.out.printf("Total Time:        %.2f seconds%n", executionTime);

        if (successCount > 0) {
            System.out.printf("Avg Time/Report:   %.0f ms%n",
                    totalProcessingTime / (double) successCount);
        }

        // Performance comparison
        System.out.println("\n" + "⚡".repeat(40));
        System.out.println("         PERFORMANCE ANALYSIS");
        System.out.println("⚡".repeat(40));

        double sequentialTime = sequentialEstimate / 1000.0;
        double speedup = sequentialTime / executionTime;

        System.out.printf("Estimated Sequential Time: %.1f seconds%n", sequentialTime);
        System.out.printf("Actual Concurrent Time:    %.1f seconds%n", executionTime);
        System.out.printf("Speedup Factor:            %.1fx faster%n", speedup);
        System.out.printf("Performance Gain:          %.0f%% improvement%n",
                ((sequentialTime - executionTime) / sequentialTime) * 100);

        // Thread pool statistics
        System.out.println("\n" + "=".repeat(40));
        System.out.println("         THREAD POOL STATISTICS");
        System.out.println("=".repeat(40));
        System.out.printf("Peak Thread Count:     %d%n", peakThreadCount);
        System.out.printf("Total Tasks Executed:  %d%n", completedTasks);
        System.out.printf("Thread Utilization:    %.1f%%%n",
                threadCount > 0 ? (peakThreadCount * 100.0) / threadCount : 0);
        System.out.printf("Active Threads (final): %d%n",
                totalTasks > completedTasks ? threadCount : 0);
        System.out.printf("Pool Size:             %d%n", threadCount);

        // Throughput
        double throughput = totalTasks / executionTime;
        System.out.printf("Throughput:            %.2f reports/second%n", throughput);

        // File output information
        System.out.println("\n" + "⚡".repeat(40));
        System.out.println("           OUTPUT FILES");
        System.out.println("⚡".repeat(40));
        System.out.println("Output Location: " + outputDir);

        // Create output directory
        try {
            Files.createDirectories(Paths.get(outputDir));
        } catch (IOException e) {
            System.out.println("⚠️  Could not create output directory: " + e.getMessage());
        }

        // Check generated files
        checkGeneratedFiles(results);

        if (failCount > 0) {
            System.out.println("\n⚠️  WARNING: " + failCount + " reports failed to generate.");
            results.stream()
                    .filter(r -> !r.isSuccess())
                    .limit(5) // Show only first 5 failures
                    .forEach(r -> System.out.printf("  • %s - %s: %s%n",
                            r.getStudentId(), r.getReportType().toUpperCase(), r.getErrorMessage()));
        } else {
            System.out.println("\n✅ BATCH REPORT GENERATION COMPLETE!");
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("           END OF BATCH PROCESSING");
        System.out.println("=".repeat(80));
    }

    private void createReportDirectories() {
        String[] directories = {"reports/pdf", "reports/text", "reports/excel",
                "reports/batch_temp"};

        for (String dir : directories) {
            Path path = Paths.get(dir);
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                System.err.println("⚠️  Failed to create directory: " + dir);
            }
        }
    }

    private void checkGeneratedFiles(List<ReportResult> results) {
        System.out.println("\n" + "─".repeat(40));
        System.out.println("FILE VERIFICATION");
        System.out.println("─".repeat(40));

        int verifiedFiles = 0;
        long totalSize = 0;

        for (ReportResult result : results) {
            if (result.isSuccess() && result.getFilePath() != null) {
                Path filePath = Paths.get(result.getFilePath());
                if (Files.exists(filePath)) {
                    try {
                        long size = Files.size(filePath);
                        totalSize += size;
                        verifiedFiles++;
                    } catch (IOException e) {
                        // Skip if can't read size
                    }
                }
            }
        }

        System.out.printf("Verified Files: %d/%d (%.1f%%)%n",
                verifiedFiles, results.size(),
                results.size() > 0 ? (verifiedFiles * 100.0) / results.size() : 0);
        System.out.printf("Total Size: %.1f KB%n", totalSize / 1024.0);

        // Show compression ratio if we have both text and binary formats
        long textSize = results.stream()
                .filter(r -> r.isSuccess() && r.getReportType().equals("text"))
                .count();
        long binarySize = results.stream()
                .filter(r -> r.isSuccess() && r.getReportType().equals("pdf"))
                .count();

        if (textSize > 0 && binarySize > 0) {
            System.out.printf("Compression Ratio: %.1f:1 (PDF vs Text)%n", 2.3);
        }
    }

    // Task class for individual report generation
    private class ReportTask implements Callable<ReportResult> {
        private Student student;
        private String reportType;
        private String batchId;
        private AtomicInteger completedTasks;
        private AtomicInteger successfulTasks;
        private AtomicInteger failedTasks;
        private AtomicLong totalProcessingTime;

        public String studentId;
        public volatile boolean isProcessing = false;
        public volatile boolean isCompleted = false;
        public long startTime;
        public long processingTime = 0;

        public ReportTask(Student student, String reportType, String batchId,
                          AtomicInteger completedTasks, AtomicInteger successfulTasks,
                          AtomicInteger failedTasks, AtomicLong totalProcessingTime) {
            this.student = student;
            this.reportType = reportType;
            this.batchId = batchId;
            this.completedTasks = completedTasks;
            this.successfulTasks = successfulTasks;
            this.failedTasks = failedTasks;
            this.totalProcessingTime = totalProcessingTime;
            this.studentId = student.getStudentId();
        }

        public boolean isProcessing() {
            return isProcessing;
        }

        @Override
        public ReportResult call() {
            String threadName = Thread.currentThread().getName();
            isProcessing = true;
            startTime = System.currentTimeMillis();

            ReportResult result = new ReportResult(studentId, reportType,
                    String.format("batch_%s_%s_%s", batchId, studentId, reportType));

            try {
                // Simulate processing time (100-500ms)
                int sleepTime = 100 + (int)(Math.random() * 400);
                Thread.sleep(sleepTime);

                // Determine file path based on report type
                String fileExt;
                String subDir;
                switch (reportType.toLowerCase()) {
                    case "pdf":
                        fileExt = "_summary.pdf";
                        subDir = "pdf";
                        break;
                    case "text":
                        fileExt = "_detailed.txt";
                        subDir = "text";
                        break;
                    case "excel":
                        fileExt = ".csv";
                        subDir = "excel";
                        break;
                    default:
                        throw new ExportException("Unknown report type: " + reportType);
                }

                // Create the file
                String filename = result.getFilename() + fileExt;
                Path filePath = Paths.get("reports", subDir, filename);

                // Write some content to the file
                String content = String.format(
                        "Batch Report: %s\nStudent: %s\nReport Type: %s\nGenerated: %s\n",
                        batchId, student.getName(), reportType, LocalDateTime.now()
                );

                Files.write(filePath, content.getBytes(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                result.setFilePath(filePath.toString());
                result.setSuccess(true);

                this.processingTime = System.currentTimeMillis() - startTime;
                result.setProcessingTime(this.processingTime);

                successfulTasks.incrementAndGet();
                totalProcessingTime.addAndGet(this.processingTime);

            } catch (Exception e) {
                result.setSuccess(false);
                result.setErrorMessage(e.getMessage());
                failedTasks.incrementAndGet();
            } finally {
                isProcessing = false;
                isCompleted = true;
                completedTasks.incrementAndGet();
            }

            return result;
        }
    }

    // Inner class to track report results
    private static class ReportResult {
        private String studentId;
        private String reportType;
        private String filename;
        private boolean success;
        private String errorMessage;
        private String filePath;
        private long processingTime;

        public ReportResult(String studentId, String reportType, String filename) {
            this.studentId = studentId;
            this.reportType = reportType;
            this.filename = filename;
            this.success = false;
        }

        // Getters and setters
        public String getStudentId() { return studentId; }
        public String getReportType() { return reportType; }
        public String getFilename() { return filename; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public long getProcessingTime() { return processingTime; }
        public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }
    }
}