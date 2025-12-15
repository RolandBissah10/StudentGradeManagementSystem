package services;

import models.*;
import exceptions.ExportException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class BatchReportService {
    private StudentManager studentManager;
    private GradeManager gradeManager;
    private ReportGenerator reportGenerator;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public BatchReportService(StudentManager studentManager, GradeManager gradeManager,
                              ReportGenerator reportGenerator) {
        this.studentManager = studentManager;
        this.gradeManager = gradeManager;
        this.reportGenerator = reportGenerator;
    }

    public void generateConcurrentReports(List<Student> students, List<String> reportTypes,
                                          int threadCount) {
        String batchId = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        long startTime = System.currentTimeMillis();

        System.out.println("\n📊 BATCH REPORT GENERATION STATUS");
        System.out.println("Batch ID: " + batchId);
        System.out.println("Students: " + students.size());
        System.out.println("Formats: " + String.join(", ", reportTypes));
        System.out.println("Threads: " + threadCount);
        System.out.println("Total Tasks: " + (students.size() * reportTypes.size()));
        System.out.println("─".repeat(60));

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger completedTasks = new AtomicInteger(0);
        AtomicInteger failedTasks = new AtomicInteger(0);
        AtomicInteger currentStudent = new AtomicInteger(0);

        int totalTasks = students.size() * reportTypes.size();

        // Create tasks for each student and report type
        List<Future<Boolean>> futures = new ArrayList<>();

        for (Student student : students) {
            for (String reportType : reportTypes) {
                Callable<Boolean> task = () -> {
                    try {
                        String studentId = student.getStudentId();
                        String filename = String.format("batch_%s_%s_%s",
                                batchId, studentId, reportType);

                        System.out.printf("[%d/%d] Processing %s - %s... ",
                                completedTasks.get() + 1, totalTasks, studentId, reportType.toUpperCase());

                        switch (reportType.toLowerCase()) {
                            case "pdf":
                                reportGenerator.exportPdfSummary(studentId, filename);
                                break;
                            case "text":
                                reportGenerator.exportDetailedReport(studentId, filename);
                                break;
                            case "excel":
                                reportGenerator.exportExcelSpreadsheet(studentId, filename);
                                break;
                            default:
                                throw new ExportException("Unknown report type: " + reportType);
                        }

                        System.out.println("✓ Done");
                        completedTasks.incrementAndGet();
                        return true;

                    } catch (Exception e) {
                        System.out.println("✗ Failed: " + e.getMessage());
                        failedTasks.incrementAndGet();
                        return false;
                    }
                };
                futures.add(executor.submit(task));
            }
        }

        // Show progress bar
        showProgressBar(executor, totalTasks, completedTasks);

        // Wait for all tasks to complete
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        // Calculate statistics
        int successCount = completedTasks.get() - failedTasks.get();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("📋 BATCH REPORT GENERATION SUMMARY");
        System.out.println("═".repeat(60));
        System.out.printf("Batch ID:          %s%n", batchId);
        System.out.printf("Students:          %d%n", students.size());
        System.out.printf("Report Types:      %s%n", String.join(", ", reportTypes));
        System.out.printf("Total Reports:     %d%n", totalTasks);
        System.out.printf("Successful:        %d%n", successCount);
        System.out.printf("Failed:            %d%n", failedTasks.get());
        System.out.printf("Success Rate:      %.1f%%%n",
                totalTasks > 0 ? (successCount * 100.0 / totalTasks) : 0);
        System.out.printf("Execution Time:    %.2f seconds%n",
                (System.currentTimeMillis() - startTime) / 1000.0);
        System.out.println("═".repeat(60));

        // Show performance comparison
        System.out.println("\n⚡ PERFORMANCE ANALYSIS");
        System.out.println("─".repeat(40));

        double sequentialTime = totalTasks * 0.5; // Estimated 0.5 seconds per report sequentially
        double actualTime = (System.currentTimeMillis() - startTime) / 1000.0;
        double speedup = sequentialTime / actualTime;

        System.out.printf("Estimated Sequential Time: %.1f seconds%n", sequentialTime);
        System.out.printf("Actual Concurrent Time:    %.1f seconds%n", actualTime);
        System.out.printf("Speedup Factor:            %.1fx faster%n", speedup);
        System.out.printf("Thread Efficiency:         %.1f%%%n", (speedup / threadCount) * 100);

        // Show location of generated reports
        System.out.println("\n📁 GENERATED REPORTS LOCATION");
        System.out.println("─".repeat(40));
        System.out.println("PDF Summaries:      reports/pdf/");
        System.out.println("Detailed Reports:   reports/text/");
        System.out.println("Excel Spreadsheets: reports/excel/");
        System.out.println("Filename pattern:   batch_" + batchId + "_[STUDENTID]_[FORMAT]");

        if (failedTasks.get() > 0) {
            System.out.println("\n⚠️  WARNING: " + failedTasks.get() + " reports failed to generate.");
        } else {
            System.out.println("\n✅ BATCH REPORT GENERATION COMPLETE!");
        }
        System.out.printf("Execution Time:    %.2f seconds%n",
                (System.currentTimeMillis() - startTime) / 1000.0);
    }

    private void showProgressBar(ExecutorService executor, int totalTasks,
                                 AtomicInteger completedTasks) {
        Thread progressThread = new Thread(() -> {
            try {
                while (!executor.isTerminated()) {
                    int completed = completedTasks.get();
                    int progress = (int) ((completed * 50.0) / totalTasks);

                    System.out.print("\rProgress: [");
                    for (int i = 0; i < 50; i++) {
                        if (i < progress) {
                            if (progress >= 45) System.out.print("█");
                            else if (progress >= 30) System.out.print("▓");
                            else if (progress >= 15) System.out.print("▒");
                            else System.out.print("░");
                        } else {
                            System.out.print(" ");
                        }
                    }
                    System.out.printf("] %d/%d (%.1f%%)",
                            completed, totalTasks, (completed * 100.0) / totalTasks);

                    Thread.sleep(500);
                }
                System.out.println();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        progressThread.setDaemon(true);
        progressThread.start();
    }

    // Alternative: Simple batch generation without threads
    public void generateSimpleBatchReports(List<Student> students, List<String> reportTypes)
            throws ExportException {

        String batchId = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        int successCount = 0;
        int failCount = 0;

        System.out.println("\nGenerating batch reports (sequential)...");

        for (Student student : students) {
            String studentId = student.getStudentId();
            System.out.print("\nProcessing " + studentId + " - " +
                    truncate(student.getName(), 20) + "... ");

            for (String reportType : reportTypes) {
                try {
                    String filename = String.format("batch_%s_%s_%s",
                            batchId, studentId, reportType);

                    switch (reportType.toLowerCase()) {
                        case "pdf":
                            reportGenerator.exportPdfSummary(studentId, filename);
                            break;
                        case "text":
                            reportGenerator.exportDetailedReport(studentId, filename);
                            break;
                        case "excel":
                            reportGenerator.exportExcelSpreadsheet(studentId, filename);
                            break;
                    }
                    successCount++;

                } catch (ExportException e) {
                    System.out.print("✗ " + reportType + " failed: " + e.getMessage() + " ");
                    failCount++;
                }
            }
            System.out.println("✓");
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("BATCH REPORT SUMMARY");
        System.out.println("=".repeat(60));
        System.out.println("Batch ID:      " + batchId);
        System.out.println("Students:      " + students.size());
        System.out.println("Report Types:  " + String.join(", ", reportTypes));
        System.out.println("Successful:    " + successCount);
        System.out.println("Failed:        " + failCount);
        System.out.println("Total:         " + (successCount + failCount));

        if (failCount > 0) {
            throw new ExportException(failCount + " reports failed to generate");
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

}