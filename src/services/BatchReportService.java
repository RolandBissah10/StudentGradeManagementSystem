package services;

import models.*;
import exceptions.ExportException;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        // Create necessary directories first
        createReportDirectories();

        String batchId = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        long startTime = System.currentTimeMillis();

        System.out.println("\n BATCH REPORT GENERATION STATUS");
        System.out.println("Batch ID: " + batchId);
        System.out.println("Students: " + students.size());
        System.out.println("Formats: " + String.join(", ", reportTypes));
        System.out.println("Threads: " + threadCount);
        System.out.println("Total Tasks: " + (students.size() * reportTypes.size()));
        System.out.println("─".repeat(60));

        // Create a summary list to track what's happening
        List<String> reportLog = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger completedTasks = new AtomicInteger(0);
        AtomicInteger failedTasks = new AtomicInteger(0);
        AtomicInteger currentStudent = new AtomicInteger(0);

        int totalTasks = students.size() * reportTypes.size();

        // Create tasks for each student and report type
        List<Future<ReportResult>> futures = new ArrayList<>();

        for (Student student : students) {
            for (String reportType : reportTypes) {
                Callable<ReportResult> task = () -> {
                    String studentId = student.getStudentId();
                    String filename = String.format("batch_%s_%s_%s",
                            batchId, studentId, reportType);

                    ReportResult result = new ReportResult(studentId, reportType, filename);

                    try {
                        System.out.printf("\n[%d/%d] Processing %s - %s... ",
                                completedTasks.get() + 1, totalTasks, studentId, reportType.toUpperCase());

                        switch (reportType.toLowerCase()) {
                            case "pdf":
                                reportGenerator.exportPdfSummary(studentId, filename);
                                result.setFilePath("reports/pdf/" + filename + "_summary.pdf");
                                break;
                            case "text":
                                reportGenerator.exportDetailedReport(studentId, filename);
                                result.setFilePath("reports/text/" + filename + "_detailed.txt");
                                break;
                            case "excel":
                                reportGenerator.exportExcelSpreadsheet(studentId, filename);
                                result.setFilePath("reports/excel/" + filename + ".csv");
                                break;
                            default:
                                throw new ExportException("Unknown report type: " + reportType);
                        }

                        result.setSuccess(true);
                        System.out.println("✓ Done");
                        completedTasks.incrementAndGet();

                    } catch (Exception e) {
                        result.setSuccess(false);
                        result.setErrorMessage(e.getMessage());
                        System.out.println("✗ Failed: " + e.getMessage());
                        failedTasks.incrementAndGet();
                    }

                    return result;
                };
                futures.add(executor.submit(task));
            }
        }

        // Show progress bar
        Thread progressThread = showProgressBar(executor, totalTasks, completedTasks);

        // Wait for all tasks to complete
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
                executor.shutdownNow();
                System.out.println("\n  Timeout! Some reports may not have completed.");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            System.out.println("\n️  Interrupted! Report generation cancelled.");
        }

        // Stop progress bar thread
        if (progressThread != null && progressThread.isAlive()) {
            progressThread.interrupt();
        }

        // Collect results
        List<ReportResult> results = new ArrayList<>();
        for (Future<ReportResult> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                // Ignore, already handled
            }
        }

        // Calculate statistics
        int successCount = completedTasks.get() - failedTasks.get();
        long endTime = System.currentTimeMillis();
        double executionTime = (endTime - startTime) / 1000.0;

        System.out.println("\n" + "═".repeat(60));
        System.out.println(" BATCH REPORT GENERATION SUMMARY");
        System.out.println("═".repeat(60));
        System.out.printf("Batch ID:          %s%n", batchId);
        System.out.printf("Students:          %d%n", students.size());
        System.out.printf("Report Types:      %s%n", String.join(", ", reportTypes));
        System.out.printf("Total Reports:     %d%n", totalTasks);
        System.out.printf("Successful:        %d%n", successCount);
        System.out.printf("Failed:            %d%n", failedTasks.get());
        System.out.printf("Success Rate:      %.1f%%%n",
                totalTasks > 0 ? (successCount * 100.0 / totalTasks) : 0);
        System.out.printf("Execution Time:    %.2f seconds%n", executionTime);
        System.out.println("═".repeat(60));

        // Show performance comparison
        if (totalTasks > 0) {
            System.out.println("\n⚡ PERFORMANCE ANALYSIS");
            System.out.println("─".repeat(40));

            double sequentialTime = totalTasks * 0.5; // Estimated 0.5 seconds per report sequentially
            double speedup = sequentialTime / executionTime;

            System.out.printf("Estimated Sequential Time: %.1f seconds%n", sequentialTime);
            System.out.printf("Actual Concurrent Time:    %.1f seconds%n", executionTime);
            System.out.printf("Speedup Factor:            %.1fx faster%n", speedup);
            System.out.printf("Thread Efficiency:         %.1f%%%n", (speedup / threadCount) * 100);
        }

        // Show successful reports
        List<ReportResult> successfulReports = results.stream()
                .filter(ReportResult::isSuccess)
                .collect(Collectors.toList());

        if (!successfulReports.isEmpty()) {
            System.out.println("\n SUCCESSFUL REPORTS:");
            System.out.println("─".repeat(40));
            for (ReportResult result : successfulReports) {
                System.out.printf("• %s - %s: %s%n",
                        result.getStudentId(),
                        result.getReportType().toUpperCase(),
                        result.getFilePath());
            }
        }

        // Show failed reports
        List<ReportResult> failedReports = results.stream()
                .filter(r -> !r.isSuccess())
                .collect(Collectors.toList());

        if (!failedReports.isEmpty()) {
            System.out.println("\n FAILED REPORTS:");
            System.out.println("─".repeat(40));
            for (ReportResult result : failedReports) {
                System.out.printf("• %s - %s: %s%n",
                        result.getStudentId(),
                        result.getReportType().toUpperCase(),
                        result.getErrorMessage());
            }
        }

        // Show location of generated reports
        System.out.println("\n GENERATED REPORTS LOCATION");
        System.out.println("─".repeat(40));
        System.out.println("PDF Summaries:      reports/pdf/");
        System.out.println("Detailed Reports:   reports/text/");
        System.out.println("Excel Spreadsheets: reports/excel/");
        System.out.println("Filename pattern:   batch_" + batchId + "_[STUDENTID]_[FORMAT]");

        // Check if files were actually created
        checkGeneratedFiles(batchId, students, reportTypes);

        if (failedTasks.get() > 0) {
            System.out.println("\n  WARNING: " + failedTasks.get() + " reports failed to generate.");
        } else {
            System.out.println("\n BATCH REPORT GENERATION COMPLETE!");
        }

    }

    private Thread showProgressBar(ExecutorService executor, int totalTasks,
                                   AtomicInteger completedTasks) {
        Thread progressThread = new Thread(() -> {
            try {
                int lastProgress = 0;
                while (!executor.isTerminated()) {
                    int completed = completedTasks.get();
                    int progress = totalTasks > 0 ? (completed * 100) / totalTasks : 0;

                    // Only update if progress changed
                    if (progress != lastProgress) {
                        System.out.print("\rProgress: [");
                        int bars = progress / 2; // 50 characters max
                        for (int i = 0; i < 50; i++) {
                            if (i < bars) {
                                System.out.print("█");
                            } else {
                                System.out.print("░");
                            }
                        }
                        System.out.printf("] %d%% (%d/%d)", progress, completed, totalTasks);
                        lastProgress = progress;
                    }

                    Thread.sleep(200);
                }
                // Show 100% at the end
                System.out.print("\rProgress: [");
                for (int i = 0; i < 50; i++) System.out.print("█");
                System.out.printf("] 100%% (%d/%d)%n", totalTasks, totalTasks);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.print("\r"); // Clear the progress bar line
            }
        });
        progressThread.setDaemon(true);
        progressThread.start();
        return progressThread;
    }

    private void createReportDirectories() {
        // FIX: Include pdf directory
        String[] directories = {"reports/pdf", "reports/text", "reports/excel"};

        System.out.println("\n🔧 CREATING REPORT DIRECTORIES:");
        for (String dir : directories) {
            Path path = Paths.get(dir);
            try {
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                    System.out.println("  ✓ Created: " + dir);
                } else {
                    System.out.println("  ✓ Exists: " + dir);
                }
            } catch (IOException e) {
                System.out.println("  ✗ Failed to create " + dir + ": " + e.getMessage());
            }
        }
        System.out.println();
    }

    private void checkGeneratedFiles(String batchId, List<Student> students, List<String> reportTypes) {
        System.out.println("\n VERIFYING GENERATED FILES:");
        System.out.println("─".repeat(40));

        int foundFiles = 0;
        int expectedFiles = students.size() * reportTypes.size();

        for (Student student : students) {
            String studentId = student.getStudentId();

            for (String reportType : reportTypes) {
                String filename = String.format("batch_%s_%s_%s", batchId, studentId, reportType);
                Path filePath = null;

                switch (reportType.toLowerCase()) {
                    case "pdf":
                        filePath = Paths.get("reports/pdf", filename + "_summary.pdf");
                        break;
                    case "text":
                        filePath = Paths.get("reports/text", filename + "_detailed.txt");
                        break;
                    case "excel":
                        filePath = Paths.get("reports/excel", filename + ".csv");
                        break;
                }

                if (filePath != null && Files.exists(filePath)) {
                    try {
                        long fileSize = Files.size(filePath);
                        // Check if PDF is valid
                        if (reportType.equalsIgnoreCase("pdf") && fileSize > 0) {
                            try (PDDocument doc = PDDocument.load(filePath.toFile())) {
                                System.out.printf("  ✓ %-8s %-10s: %s (%.1f KB) ✓ Valid PDF%n",
                                        studentId,
                                        reportType.toUpperCase(),
                                        filePath.getFileName(),
                                        fileSize / 1024.0);
                            } catch (Exception e) {
                                System.out.printf("  ✗ %-8s %-10s: %s (%.1f KB) ✗ Corrupt PDF%n",
                                        studentId,
                                        reportType.toUpperCase(),
                                        filePath.getFileName(),
                                        fileSize / 1024.0);
                            }
                        } else {
                            System.out.printf("  ✓ %-8s %-10s: %s (%.1f KB)%n",
                                    studentId,
                                    reportType.toUpperCase(),
                                    filePath.getFileName(),
                                    fileSize / 1024.0);
                        }
                        foundFiles++;
                    } catch (IOException e) {
                        System.out.printf("  ✗ %-8s %-10s: Error checking file%n",
                                studentId, reportType.toUpperCase());
                    }
                } else {
                    System.out.printf("  ✗ %-8s %-10s: File not found%n",
                            studentId, reportType.toUpperCase());
                }
            }
        }

        System.out.println("─".repeat(40));
        System.out.printf("Files found: %d/%d (%.1f%%)%n",
                foundFiles, expectedFiles,
                expectedFiles > 0 ? (foundFiles * 100.0 / expectedFiles) : 0);
    }

    // Alternative: Simple batch generation without threads
    public void generateSimpleBatchReports(List<Student> students, List<String> reportTypes)
            throws ExportException {

        // Create directories first
        createReportDirectories();

        String batchId = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        int successCount = 0;
        int failCount = 0;

        System.out.println("\nGenerating batch reports (sequential)...");
        System.out.println("Batch ID: " + batchId);
        System.out.println("Students: " + students.size());
        System.out.println("Formats: " + String.join(", ", reportTypes));
        System.out.println("─".repeat(60));

        long startTime = System.currentTimeMillis();

        for (Student student : students) {
            String studentId = student.getStudentId();
            System.out.print("\nProcessing " + studentId + " - " +
                    truncate(student.getName(), 20) + " ");

            int studentSuccess = 0;
            int studentFail = 0;

            for (String reportType : reportTypes) {
                try {
                    String filename = String.format("batch_%s_%s_%s",
                            batchId, studentId, reportType);

                    switch (reportType.toLowerCase()) {
                        case "pdf":
                            reportGenerator.exportPdfSummary(studentId, filename);
                            System.out.print("✓P ");
                            break;
                        case "text":
                            reportGenerator.exportDetailedReport(studentId, filename);
                            System.out.print("✓T ");
                            break;
                        case "excel":
                            reportGenerator.exportExcelSpreadsheet(studentId, filename);
                            System.out.print("✓E ");
                            break;
                    }
                    successCount++;
                    studentSuccess++;

                } catch (ExportException e) {
                    System.out.print("✗" + reportType.charAt(0) + " ");
                    failCount++;
                    studentFail++;
                }
            }

            if (studentFail > 0) {
                System.out.printf("(%d/%d failed)", studentFail, reportTypes.size());
            }
        }

        long endTime = System.currentTimeMillis();
        double executionTime = (endTime - startTime) / 1000.0;

        System.out.println("\n\n" + "=".repeat(60));
        System.out.println("BATCH REPORT SUMMARY");
        System.out.println("=".repeat(60));
        System.out.println("Batch ID:      " + batchId);
        System.out.println("Students:      " + students.size());
        System.out.println("Report Types:  " + String.join(", ", reportTypes));
        System.out.println("Successful:    " + successCount);
        System.out.println("Failed:        " + failCount);
        System.out.println("Total:         " + (successCount + failCount));
        System.out.printf("Execution Time: %.2f seconds%n", executionTime);

        // Verify files
        checkGeneratedFiles(batchId, students, reportTypes);

        if (failCount > 0) {
            throw new ExportException(failCount + " reports failed to generate");
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    // Inner class to track report results
    private static class ReportResult {
        private String studentId;
        private String reportType;
        private String filename;
        private boolean success;
        private String errorMessage;
        private String filePath;

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
    }
}