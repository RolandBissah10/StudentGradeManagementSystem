package services;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class AuditLogger {
    private static final int MAX_QUEUE_SIZE = 5000;
    private static final int LOG_ROTATION_SIZE = 10 * 1024 * 1024; // 10MB
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static AuditLogger instance;

    private final BlockingQueue<AuditEntry> logQueue;
    private final ExecutorService logWriterService;
    private final ScheduledExecutorService logRotatorService;
    private PrintWriter logWriter;
    private Path currentLogFile;
    private final AtomicInteger entryCounter;
    private final AtomicLong totalEntries;
    private final Map<String, AtomicInteger> operationCounts;
    private final Map<String, AtomicLong> operationTimes;

    private static class AuditEntry {
        final String timestamp;
        final String threadId;
        final String operationType;
        final String userAction;
        final long executionTime;
        final String status;
        final String details;
        final String studentId;

        AuditEntry(String operationType, String userAction, long executionTime,
                   String status, String details, String studentId) {
            this.timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            this.threadId = Thread.currentThread().getName();
            this.operationType = operationType;
            this.userAction = userAction;
            this.executionTime = executionTime;
            this.status = status;
            this.details = details;
            this.studentId = studentId;
        }

        String toCSV() {
            return String.format("\"%s\",\"%s\",\"%s\",\"%s\",%d,\"%s\",\"%s\",\"%s\"",
                    timestamp, threadId, operationType,
                    escapeCSV(userAction), executionTime, status, escapeCSV(details), studentId);
        }

        String toJSON() {
            return String.format(
                    "{\"timestamp\":\"%s\",\"threadId\":\"%s\",\"operation\":\"%s\"," +
                            "\"action\":\"%s\",\"executionTime\":%d,\"status\":\"%s\"," +
                            "\"details\":\"%s\",\"studentId\":\"%s\"}",
                    timestamp, threadId, operationType,
                    escapeJSON(userAction), executionTime, status, escapeJSON(details), studentId);
        }

        private String escapeCSV(String text) {
            if (text == null) return "";
            return text.replace("\"", "\"\"");
        }

        private String escapeJSON(String text) {
            if (text == null) return "";
            return text.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
    }

    private AuditLogger() {
        this.logQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        this.logWriterService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Audit-Log-Writer");
            t.setDaemon(true);
            return t;
        });
        this.logRotatorService = Executors.newScheduledThreadPool(1);
        this.entryCounter = new AtomicInteger(0);
        this.totalEntries = new AtomicLong(0);
        this.operationCounts = new ConcurrentHashMap<>();
        this.operationTimes = new ConcurrentHashMap<>();

        initializeLogWriter();
        startLogWriterThread();
        startLogRotator();

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public static synchronized AuditLogger getInstance() {
        if (instance == null) {
            instance = new AuditLogger();
        }
        return instance;
    }

    private void initializeLogWriter() {
        try {
            Path logsDir = Paths.get("logs", "audit");
            Files.createDirectories(logsDir);

            String filename = "audit_" + LocalDateTime.now().format(DATE_FORMATTER) + ".log";
            currentLogFile = logsDir.resolve(filename);

            boolean append = Files.exists(currentLogFile) && Files.size(currentLogFile) < LOG_ROTATION_SIZE;

            logWriter = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    new FileOutputStream(currentLogFile.toFile(), append), "UTF-8"
                            )
                    )
            );

            if (!append || Files.size(currentLogFile) == 0) {
                logWriter.println("timestamp,threadId,operationType,userAction,executionTime(ms),status,details,studentId");
            }

            System.out.println("✓ Audit logging initialized: " + currentLogFile);
        } catch (IOException e) {
            System.err.println("Failed to initialize audit logger: " + e.getMessage());
            logWriter = new PrintWriter(System.out);
        }
    }

    private void rotateLogFile() {
        try {
            if (currentLogFile != null && Files.exists(currentLogFile) &&
                    Files.size(currentLogFile) >= LOG_ROTATION_SIZE) {

                logWriter.flush();
                logWriter.close();

                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
                String newName = "audit_" + LocalDateTime.now().format(DATE_FORMATTER) +
                        "_" + timestamp + ".log";

                Path rotatedFile = currentLogFile.getParent().resolve(newName);
                Files.move(currentLogFile, rotatedFile);

                System.out.println("✓ Rotated audit log to: " + rotatedFile.getFileName());

                initializeLogWriter();
            }
        } catch (IOException e) {
            System.err.println("Failed to rotate log file: " + e.getMessage());
        }
    }

    private void startLogWriterThread() {
        logWriterService.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    List<AuditEntry> batch = new ArrayList<>(100);
                    AuditEntry entry = logQueue.poll(100, TimeUnit.MILLISECONDS);

                    if (entry != null) {
                        batch.add(entry);
                        logQueue.drainTo(batch, 99);

                        for (AuditEntry e : batch) {
                            writeLogEntry(e);
                        }

                        if (entryCounter.incrementAndGet() % 100 == 0) {
                            logWriter.flush();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Drain remaining entries on shutdown
            drainQueue();
            logWriter.close();
        });
    }

    private void startLogRotator() {
        logRotatorService.scheduleAtFixedRate(() -> {
            try {
                rotateLogFile();
            } catch (Exception e) {
                System.err.println("Log rotation error: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    private void writeLogEntry(AuditEntry entry) {
        try {
            logWriter.println(entry.toCSV());
            totalEntries.incrementAndGet();

            // Update statistics
            operationCounts.computeIfAbsent(entry.operationType, k -> new AtomicInteger(0))
                    .incrementAndGet();
            operationTimes.computeIfAbsent(entry.operationType, k -> new AtomicLong(0))
                    .addAndGet(entry.executionTime);

        } catch (Exception e) {
            System.err.println("Failed to write log entry: " + e.getMessage());
        }
    }

    private void drainQueue() {
        List<AuditEntry> remaining = new ArrayList<>();
        logQueue.drainTo(remaining);
        for (AuditEntry entry : remaining) {
            writeLogEntry(entry);
        }
        logWriter.flush();
    }

    // Public API methods
    public void logOperation(String operationType, String userAction,
                             long executionTime, String status, String details, String studentId) {
        AuditEntry entry = new AuditEntry(operationType, userAction, executionTime, status, details, studentId);

        if (!logQueue.offer(entry)) {
            // Queue full - write directly with warning
            System.err.println("Audit log queue full! Writing directly.");
            writeLogEntry(entry);
        }
    }

    public void logSimple(String operationType, String userAction, String studentId) {
        logOperation(operationType, userAction, 0, "SUCCESS", "", studentId);
    }

    public void logWithTime(String operationType, String userAction,
                            long executionTime, String studentId) {
        logOperation(operationType, userAction, executionTime, "SUCCESS", "", studentId);
    }

    public void logError(String operationType, String userAction,
                         String errorMessage, String studentId) {
        logOperation(operationType, userAction, 0, "ERROR", errorMessage, studentId);
    }

    // Audit trail viewer methods with bar charts
    public void displayRecentLogs(int count) throws IOException {
        if (!Files.exists(currentLogFile)) {
            System.out.println("No audit logs found.");
            return;
        }

        System.out.println("\n" + "=".repeat(100));
        System.out.println("                     RECENT AUDIT ENTRIES");
        System.out.println("=".repeat(100));
        System.out.println("File: " + currentLogFile.getFileName());
        System.out.println();
        System.out.printf("%-23s %-15s %-20s %-10s %-30s %s%n",
                "TIMESTAMP", "THREAD", "OPERATION", "STATUS", "ACTION", "STUDENT ID");
        System.out.println("-".repeat(120));

        try (Stream<String> lines = Files.lines(currentLogFile)) {
            lines.skip(1) // Skip header
                    .limit(count)
                    .forEach(line -> {
                        String[] parts = parseCSVLine(line);
                        if (parts.length >= 8) {
                            String timestamp = parts[0].replace("\"", "");
                            String threadId = parts[1].replace("\"", "");
                            String operation = parts[2].replace("\"", "");
                            String status = parts[5].replace("\"", "");
                            String action = parts[3].replace("\"", "");
                            String studentId = parts[7].replace("\"", "");

                            // Truncate for display
                            if (action.length() > 25) {
                                action = action.substring(0, 22) + "...";
                            }

                            String statusSymbol = status.equals("SUCCESS") ? "✓" : "✗";

                            System.out.printf("%-23s %-15s %-20s %-10s %-30s %s%n",
                                    timestamp, threadId, operation, statusSymbol, action, studentId);
                        }
                    });
        }

        long totalLines = Files.lines(currentLogFile).count() - 1;
        System.out.println("\nTotal entries in file: " + totalLines + " (showing " +
                Math.min(count, totalLines) + " most recent)");
    }

    public void displayStatistics() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("                    AUDIT TRAIL STATISTICS");
        System.out.println("=".repeat(80));

        System.out.println("\nOPERATION STATISTICS:");
        System.out.println("-".repeat(80));

        // Calculate total operations and average times
        long totalOps = operationCounts.values().stream()
                .mapToLong(AtomicInteger::get)
                .sum();

        System.out.printf("Total Operations: %d%n", totalEntries.get());
        System.out.printf("Operations in memory: %d%n", totalOps);

        if (totalOps > 0) {
            System.out.println("\nOperation Distribution:");

            // Find max count for bar chart scaling
            int maxCount = operationCounts.values().stream()
                    .mapToInt(AtomicInteger::get)
                    .max()
                    .orElse(1);

            // Display bar chart
            operationCounts.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue().get(), a.getValue().get()))
                    .forEach(entry -> {
                        String operation = entry.getKey();
                        int count = entry.getValue().get();
                        long totalTime = operationTimes.getOrDefault(operation, new AtomicLong(0)).get();
                        double avgTime = count > 0 ? (double) totalTime / count : 0;
                        double percentage = (count * 100.0) / totalOps;

                        // Create bar
                        int barLength = (int) Math.round((count * 40.0) / maxCount);
                        String bar = "█".repeat(Math.max(1, barLength));

                        System.out.printf("  %-25s: %5d ops %s %5.1f%% (avg: %.1fms)%n",
                                operation, count, bar, percentage, avgTime);
                    });
        }

        // Queue status
        System.out.println("\nQUEUE STATUS:");
        System.out.printf("  Current queue size: %d/%d%n", logQueue.size(), MAX_QUEUE_SIZE);
        System.out.printf("  Queue utilization: %.1f%%%n",
                (logQueue.size() * 100.0) / MAX_QUEUE_SIZE);

        // Thread status
        System.out.println("\nTHREAD STATUS:");
        System.out.printf("  Log writer thread: %s%n",
                logWriterService.isShutdown() ? "SHUTDOWN" : "RUNNING");
        System.out.printf("  Rotator thread: %s%n",
                logRotatorService.isShutdown() ? "SHUTDOWN" : "RUNNING");

        // File information
        try {
            if (currentLogFile != null && Files.exists(currentLogFile)) {
                long fileSize = Files.size(currentLogFile);
                System.out.printf("\nCURRENT LOG FILE:%n");
                System.out.printf("  Size: %.2f MB%n", fileSize / (1024.0 * 1024.0));
                System.out.printf("  Rotation threshold: %.2f MB%n", LOG_ROTATION_SIZE / (1024.0 * 1024.0));
                System.out.printf("  Usage: %.1f%%%n",
                        (fileSize * 100.0) / LOG_ROTATION_SIZE);
            }
        } catch (IOException e) {
            System.out.println("  Unable to get file information: " + e.getMessage());
        }
    }

    public void displayPerformanceChart() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("               OPERATION PERFORMANCE CHART");
        System.out.println("=".repeat(80));

        if (operationCounts.isEmpty()) {
            System.out.println("No operation data available.");
            return;
        }

        // Calculate total operations
        long totalOps = operationCounts.values().stream()
                .mapToLong(AtomicInteger::get)
                .sum();

        // Find max average time for scaling
        double maxAvgTime = operationCounts.entrySet().stream()
                .mapToDouble(entry -> {
                    String op = entry.getKey();
                    int count = entry.getValue().get();
                    long totalTime = operationTimes.getOrDefault(op, new AtomicLong(0)).get();
                    return count > 0 ? (double) totalTime / count : 0;
                })
                .max()
                .orElse(1.0);

        System.out.println("\nAverage Execution Time (ms):");
        System.out.println("-".repeat(80));

        operationCounts.entrySet().stream()
                .sorted((a, b) -> {
                    double avgTimeA = operationTimes.getOrDefault(a.getKey(), new AtomicLong(0)).get() /
                            (double) Math.max(1, a.getValue().get());
                    double avgTimeB = operationTimes.getOrDefault(b.getKey(), new AtomicLong(0)).get() /
                            (double) Math.max(1, b.getValue().get());
                    return Double.compare(avgTimeB, avgTimeA);
                })
                .forEach(entry -> {
                    String operation = entry.getKey();
                    int count = entry.getValue().get();
                    long totalTime = operationTimes.getOrDefault(operation, new AtomicLong(0)).get();
                    double avgTime = count > 0 ? (double) totalTime / count : 0;

                    // Create performance bar
                    int barLength = maxAvgTime > 0 ? (int) Math.round((avgTime * 40.0) / maxAvgTime) : 0;
                    String bar = "█".repeat(Math.max(1, barLength));

                    // Color coding based on performance
                    String performance;
                    if (avgTime < 10) {
                        performance = "Excellent";
                    } else if (avgTime < 50) {
                        performance = "Good";
                    } else if (avgTime < 100) {
                        performance = "Average";
                    } else if (avgTime < 500) {
                        performance = "Slow";
                    } else {
                        performance = "Very Slow";
                    }

                    System.out.printf("  %-25s: %6.1fms %s [%s]%n",
                            operation, avgTime, bar, performance);
                });

        // Summary statistics
        System.out.println("\nPERFORMANCE SUMMARY:");
        System.out.println("-".repeat(80));

        long totalTime = operationTimes.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
        double overallAvgTime = totalOps > 0 ? (double) totalTime / totalOps : 0;

        System.out.printf("  Overall average time: %.1fms%n", overallAvgTime);
        System.out.printf("  Total execution time: %.1fs%n", totalTime / 1000.0);

        // Performance recommendations
        System.out.println("\nPERFORMANCE RECOMMENDATIONS:");
        System.out.println("-".repeat(80));

        operationCounts.entrySet().stream()
                .filter(entry -> {
                    String op = entry.getKey();
                    int count = entry.getValue().get();
                    long totalTimeOp = operationTimes.getOrDefault(op, new AtomicLong(0)).get();
                    double avgTime = count > 0 ? (double) totalTimeOp / count : 0;
                    return avgTime > 100; // Flag operations slower than 100ms
                })
                .forEach(entry -> {
                    String operation = entry.getKey();
                    int count = entry.getValue().get();
                    long totalTimeOp = operationTimes.getOrDefault(operation, new AtomicLong(0)).get();
                    double avgTime = (double) totalTimeOp / count;

                    System.out.printf("  Δ %-23s: %.1fms average - Consider optimization%n",
                            operation, avgTime);
                });

        if (operationCounts.entrySet().stream()
                .noneMatch(entry -> {
                    String op = entry.getKey();
                    int count = entry.getValue().get();
                    long totalTimeOp = operationTimes.getOrDefault(op, new AtomicLong(0)).get();
                    double avgTime = count > 0 ? (double) totalTimeOp / count : 0;
                    return avgTime > 100;
                })) {
            System.out.println("  ✓ All operations within acceptable performance range");
        }
    }

    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());

        return result.toArray(new String[0]);
    }

    public void shutdown() {
        logRotatorService.shutdown();
        logWriterService.shutdown();

        try {
            if (!logWriterService.awaitTermination(5, TimeUnit.SECONDS)) {
                logWriterService.shutdownNow();
            }
            if (!logRotatorService.awaitTermination(5, TimeUnit.SECONDS)) {
                logRotatorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logWriterService.shutdownNow();
            logRotatorService.shutdownNow();
        }

        System.out.println("✓ Audit logger shutdown complete.");
    }
}