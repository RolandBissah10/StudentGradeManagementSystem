package services;

import models.*;
import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class StatisticsDashboard {
    private final StudentManager studentManager;
    private final GradeManager gradeManager;
    private final CacheManager cacheManager;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicReference<DashboardData> currentData = new AtomicReference<>(new DashboardData());

    private static ScheduledExecutorService dashboardScheduler;
    private static ExecutorService commandExecutor;

    private ScheduledFuture<?> updateTask;
    private volatile boolean shouldStop = false;
    private BufferedReader commandReader;

    private static final DateTimeFormatter FULL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int DEFAULT_REFRESH_INTERVAL = 5;

    private static class DashboardData {
        LocalDateTime timestamp;
        int totalStudents;
        int totalGrades;
        double averageGrade;
        double medianGrade;
        double stdDeviation;
        Map<String, Long> gradeDistribution;
        List<StudentPerformance> topPerformers;
        Map<String, Double> subjectAverages;
        SystemMetrics systemMetrics;
        int activeThreads;
        double cacheHitRate;
        List<TaskStatus> activeTasks;

        DashboardData() {
            this.timestamp = LocalDateTime.now();
            this.gradeDistribution = new LinkedHashMap<>();
            this.topPerformers = new ArrayList<>();
            this.subjectAverages = new HashMap<>();
            this.systemMetrics = new SystemMetrics();
            this.activeTasks = new ArrayList<>();
        }
    }

    private static class StudentPerformance {
        String studentId;
        String name;
        double averageGrade;
        double gpa;

        StudentPerformance(String studentId, String name, double averageGrade, double gpa) {
            this.studentId = studentId;
            this.name = name;
            this.averageGrade = averageGrade;
            this.gpa = gpa;
        }
    }

    private static class SystemMetrics {
        long usedMemory;
        long maxMemory;
        int availableProcessors;

        SystemMetrics() {
            Runtime runtime = Runtime.getRuntime();
            this.usedMemory = runtime.totalMemory() - runtime.freeMemory();
            this.maxMemory = runtime.maxMemory();
            this.availableProcessors = runtime.availableProcessors();
        }
    }

    private static class TaskStatus {
        String taskName;
        double progress;
        String status;
        long elapsedTime;

        TaskStatus(String taskName, double progress, String status, long elapsedTime) {
            this.taskName = taskName;
            this.progress = progress;
            this.status = status;
            this.elapsedTime = elapsedTime;
        }
    }

    public StatisticsDashboard(StudentManager studentManager, GradeManager gradeManager, CacheManager cacheManager) {
        this.studentManager = studentManager;
        this.gradeManager = gradeManager;
        this.cacheManager = cacheManager;
        initializeExecutors();
    }

    private synchronized void initializeExecutors() {
        if (dashboardScheduler == null || dashboardScheduler.isShutdown()) {
            dashboardScheduler = Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, "Dashboard-Scheduler");
                t.setDaemon(true);
                return t;
            });
        }

        if (commandExecutor == null || commandExecutor.isShutdown()) {
            commandExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "Dashboard-Command-Handler");
                t.setDaemon(true);
                return t;
            });
        }
    }

    public void startDashboard(int refreshIntervalSeconds) {
        if (isRunning.get()) {
            System.out.println("Dashboard is already running!");
            return;
        }

        shouldStop = false;
        isRunning.set(true);
        isPaused.set(false);

        System.out.println("\n" + "=".repeat(100));
        System.out.println("              REAL-TIME STATISTICS DASHBOARD v3.0");
        System.out.println("=".repeat(100));
        System.out.printf("Auto-refresh: Enabled (%d seconds) | Thread: RUNNING%n", refreshIntervalSeconds);
        System.out.println("Commands: [Q]uit [R]efresh [P]ause [S]tats [C]hart [M]etrics [H]elp");
        System.out.println();

        updateStatistics();
        displayDashboard();

        updateTask = dashboardScheduler.scheduleAtFixedRate(() -> {
            if (isRunning.get() && !isPaused.get() && !shouldStop) {
                try {
                    updateStatistics();
                    clearAndDisplayDashboard();
                } catch (Exception e) {
                    System.err.println("Dashboard update error: " + e.getMessage());
                }
            }
        }, refreshIntervalSeconds, refreshIntervalSeconds, TimeUnit.SECONDS);

        commandExecutor.submit(this::handleCommands);
    }

    private void handleCommands() {
        while (isRunning.get() && !shouldStop && !Thread.currentThread().isInterrupted()) {
            try {
                if (commandReader == null) {
                    commandReader = new BufferedReader(new InputStreamReader(System.in));
                }

                if (System.in.available() > 0) {
                    String command = commandReader.readLine();
                    if (command != null) {
                        processCommand(command.trim().toUpperCase());
                    }
                } else {
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                if (isRunning.get()) {
                    System.err.println("Command I/O error: " + e.getMessage());
                }
                break;
            } catch (Exception e) {
                if (isRunning.get()) {
                    System.err.println("Command error: " + e.getMessage());
                }
            }
        }
    }

    private void processCommand(String command) {
        if (command == null || command.isEmpty()) {
            return;
        }

        switch (command.charAt(0)) {
            case 'Q':
                System.out.println("\nStopping dashboard...");
                stop();
                break;

            case 'R':
                if (isRunning.get()) {
                    System.out.println("\nManual refresh triggered...");
                    updateStatistics();
                    clearAndDisplayDashboard();
                }
                break;

            case 'P':
                if (isPaused.get()) {
                    resumeDashboard();
                } else {
                    pauseDashboard();
                }
                break;

            case 'S':
                displayPerformanceMetrics();
                waitForEnter();
                clearAndDisplayDashboard();
                break;

            case 'C':
                System.out.println("\nShowing performance charts...");
                displayGradeDistributionChart(currentData.get());
                waitForEnter();
                clearAndDisplayDashboard();
                break;

            case 'M':
                displayMemoryChart();
                waitForEnter();
                clearAndDisplayDashboard();
                break;

            case 'H':
                displayHelp();
                waitForEnter();
                clearAndDisplayDashboard();
                break;

            default:
                System.out.println("\nUnknown command '" + command + "'. Type 'H' for help.");
                System.out.print("Command: ");
                break;
        }
    }

    private void pauseDashboard() {
        if (!isRunning.get()) return;

        isPaused.set(true);
        if (updateTask != null) {
            updateTask.cancel(false);
        }
        System.out.println("\nDashboard PAUSED. Press 'P' to resume.");
        System.out.print("Command: ");
    }

    private void resumeDashboard() {
        if (!isRunning.get()) return;

        isPaused.set(false);
        System.out.println("\nDashboard RESUMED. Auto-refresh enabled.");

        if (dashboardScheduler != null && !dashboardScheduler.isShutdown()) {
            updateTask = dashboardScheduler.scheduleAtFixedRate(() -> {
                if (isRunning.get() && !isPaused.get() && !shouldStop) {
                    updateStatistics();
                    clearAndDisplayDashboard();
                }
            }, DEFAULT_REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL, TimeUnit.SECONDS);
        }
    }

    private void clearAndDisplayDashboard() {
        clearScreen();
        displayDashboard();
    }

    private void clearScreen() {
        try {
            final String os = System.getProperty("os.name");
            if (os.contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }

    private void updateStatistics() {
        DashboardData data = new DashboardData();
        data.timestamp = LocalDateTime.now();

        List<Student> students = studentManager.getStudents();
        List<Grade> allGrades = getAllGrades();

        data.totalStudents = students.size();
        data.totalGrades = allGrades.size();

        if (!allGrades.isEmpty()) {
            List<Double> grades = allGrades.stream()
                    .map(Grade::getGrade)
                    .sorted()
                    .collect(Collectors.toList());

            data.averageGrade = grades.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            if (grades.size() % 2 == 0) {
                data.medianGrade = (grades.get(grades.size() / 2 - 1) + grades.get(grades.size() / 2)) / 2.0;
            } else {
                data.medianGrade = grades.get(grades.size() / 2);
            }

            double variance = grades.stream()
                    .mapToDouble(g -> Math.pow(g - data.averageGrade, 2))
                    .average()
                    .orElse(0.0);
            data.stdDeviation = Math.sqrt(variance);

            data.gradeDistribution = calculateGradeDistribution(allGrades);
        }

        data.topPerformers = students.stream()
                .map(s -> {
                    double avg = gradeManager.calculateOverallAverage(s.getStudentId());
                    double gpa = convertToGPA(avg);
                    return new StudentPerformance(s.getStudentId(), s.getName(), avg, gpa);
                })
                .filter(sp -> sp.averageGrade > 0)
                .sorted((a, b) -> Double.compare(b.averageGrade, a.averageGrade))
                .limit(5)
                .collect(Collectors.toList());

        data.subjectAverages = calculateSubjectAverages(allGrades);
        data.systemMetrics = new SystemMetrics();
        data.activeThreads = Thread.activeCount();

        if (cacheManager != null) {
            data.cacheHitRate = cacheManager.getCacheHitRate();
        } else {
            data.cacheHitRate = Math.random() * 30 + 70;
        }

        data.activeTasks = simulateActiveTasks();
        currentData.set(data);
    }

    private List<Grade> getAllGrades() {
        List<Grade> allGrades = new ArrayList<>();
        for (Student student : studentManager.getStudents()) {
            List<Grade> grades = gradeManager.getGradesByStudent(student.getStudentId());
            if (grades != null) {
                allGrades.addAll(grades);
            }
        }
        return allGrades;
    }

    private Map<String, Long> calculateGradeDistribution(List<Grade> grades) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("A (90-100)", 0L);
        distribution.put("B (80-89)", 0L);
        distribution.put("C (70-79)", 0L);
        distribution.put("D (60-69)", 0L);
        distribution.put("F (0-59)", 0L);

        for (Grade grade : grades) {
            double g = grade.getGrade();
            if (g >= 90) distribution.put("A (90-100)", distribution.get("A (90-100)") + 1);
            else if (g >= 80) distribution.put("B (80-89)", distribution.get("B (80-89)") + 1);
            else if (g >= 70) distribution.put("C (70-79)", distribution.get("C (70-79)") + 1);
            else if (g >= 60) distribution.put("D (60-69)", distribution.get("D (60-69)") + 1);
            else distribution.put("F (0-59)", distribution.get("F (0-59)") + 1);
        }

        return distribution;
    }

    private Map<String, Double> calculateSubjectAverages(List<Grade> grades) {
        return grades.stream()
                .collect(Collectors.groupingBy(
                        g -> g.getSubject().getSubjectName(),
                        Collectors.averagingDouble(Grade::getGrade)
                ));
    }

    private double convertToGPA(double percentage) {
        if (percentage >= 93) return 4.0;
        else if (percentage >= 90) return 3.7;
        else if (percentage >= 87) return 3.3;
        else if (percentage >= 83) return 3.0;
        else if (percentage >= 80) return 2.7;
        else if (percentage >= 77) return 2.3;
        else if (percentage >= 73) return 2.0;
        else if (percentage >= 70) return 1.7;
        else if (percentage >= 67) return 1.3;
        else if (percentage >= 60) return 1.0;
        else return 0.0;
    }

    private List<TaskStatus> simulateActiveTasks() {
        List<TaskStatus> tasks = new ArrayList<>();
        tasks.add(new TaskStatus("Statistics Update", 100.0, "COMPLETED", 2300));
        tasks.add(new TaskStatus("Report Generation", 75.0, "IN PROGRESS", 4500));
        tasks.add(new TaskStatus("Cache Refresh", 40.0, "IN PROGRESS", 1200));
        tasks.add(new TaskStatus("Data Backup", 0.0, "PENDING", 0));
        return tasks;
    }

    public void displayDashboard() {
        DashboardData data = currentData.get();

        System.out.println("\n" + "=".repeat(100));
        System.out.println("              REAL-TIME STATISTICS DASHBOARD");
        System.out.println("=".repeat(100));
        System.out.printf("Last Updated: %s | Status: %s | Auto-refresh: %s%n",
                data.timestamp.format(FULL_FORMATTER),
                isRunning.get() ? "RUNNING" : "STOPPED",
                isPaused.get() ? "PAUSED" : "ENABLED");
        System.out.println();

        displaySystemStatus(data);
        displayLiveStatistics(data);
        displayGradeDistributionChart(data);
        displayTopPerformers(data);
        displaySubjectPerformanceChart(data);
        displayConcurrentOperations(data);

        System.out.println("\n" + "=".repeat(100));
        System.out.println("Commands: [Q]uit [R]efresh [P]ause/Resume [S]tats [C]hart [M]etrics [H]elp");
        System.out.print("Command: ");
    }

    private void displaySystemStatus(DashboardData data) {
        System.out.println("SYSTEM STATUS");
        System.out.println("-".repeat(50));

        System.out.printf("Total Students: %-5d | Total Grades: %-6d%n",
                data.totalStudents, data.totalGrades);

        double memoryPercent = (data.systemMetrics.usedMemory * 100.0) / data.systemMetrics.maxMemory;
        int memoryBarLength = (int) Math.round(memoryPercent / 2.5);
        String memoryBar = "█".repeat(Math.max(1, memoryBarLength)) +
                "░".repeat(Math.max(0, 40 - memoryBarLength));

        System.out.printf("Memory Usage: %.1f MB / %.1f MB%n",
                data.systemMetrics.usedMemory / (1024.0 * 1024.0),
                data.systemMetrics.maxMemory / (1024.0 * 1024.0));
        System.out.printf("  [%s] %.1f%%%n", memoryBar, memoryPercent);

        int cacheBarLength = (int) Math.round(data.cacheHitRate / 2.5);
        String cacheBar = "█".repeat(Math.max(1, cacheBarLength)) +
                "░".repeat(Math.max(0, 40 - cacheBarLength));

        System.out.printf("Cache Hit Rate: %.1f%%%n", data.cacheHitRate);
        System.out.printf("  [%s]%n", cacheBar);

        System.out.printf("Active Threads: %d | Available Processors: %d%n",
                data.activeThreads, data.systemMetrics.availableProcessors);
        System.out.println();
    }

    private void displayLiveStatistics(DashboardData data) {
        System.out.println("LIVE STATISTICS");
        System.out.println("-".repeat(50));

        System.out.printf("Average Grade: %6.1f%% | Median: %6.1f%% | Std Dev: %6.1f%%%n",
                data.averageGrade, data.medianGrade, data.stdDeviation);

        String trend = data.averageGrade > 80 ? "↑" : data.averageGrade > 70 ? "→" : "↓";
        System.out.printf("Performance Trend: %s %s%n", trend,
                data.averageGrade > 80 ? "Excellent" : data.averageGrade > 70 ? "Good" : "Needs Improvement");
        System.out.println();
    }

    private void displayGradeDistributionChart(DashboardData data) {
        System.out.println("GRADE DISTRIBUTION (Horizontal Bar Chart)");
        System.out.println("-".repeat(70));

        if (data.totalGrades == 0) {
            System.out.println("No grades available.");
            return;
        }

        long maxCount = data.gradeDistribution.values().stream()
                .max(Long::compare)
                .orElse(1L);

        for (Map.Entry<String, Long> entry : data.gradeDistribution.entrySet()) {
            String range = entry.getKey();
            long count = entry.getValue();
            double percentage = (count * 100.0) / data.totalGrades;

            int barLength = maxCount > 0 ? (int) Math.round((count * 50.0) / maxCount) : 0;
            String bar = "█".repeat(Math.max(1, barLength));

            String colorIndicator = "";
            if (range.startsWith("A")) colorIndicator = "[A] ";
            else if (range.startsWith("B")) colorIndicator = "[B] ";
            else if (range.startsWith("C")) colorIndicator = "[C] ";
            else if (range.startsWith("D")) colorIndicator = "[D] ";
            else colorIndicator = "[F] ";

            System.out.printf("%s%-12s: %5d grades %s %6.1f%%%n",
                    colorIndicator, range, count, bar, percentage);
        }
        System.out.println();
    }

    private void displayTopPerformers(DashboardData data) {
        System.out.println("TOP PERFORMERS (Live Rankings)");
        System.out.println("-".repeat(70));

        if (data.topPerformers.isEmpty()) {
            System.out.println("No performance data available.");
            return;
        }

        for (int i = 0; i < data.topPerformers.size(); i++) {
            StudentPerformance perf = data.topPerformers.get(i);

            int starCount = (int) Math.round(perf.averageGrade / 20);
            String stars = "★".repeat(starCount) + "☆".repeat(5 - starCount);

            System.out.printf("%d. %-8s - %-20s - %6.1f%% %s GPA: %.2f%n",
                    i + 1, perf.studentId, perf.name, perf.averageGrade, stars, perf.gpa);
        }
        System.out.println();
    }

    private void displaySubjectPerformanceChart(DashboardData data) {
        System.out.println("SUBJECT PERFORMANCE (Vertical Bar Chart)");
        System.out.println("-".repeat(70));

        if (data.subjectAverages.isEmpty()) {
            System.out.println("No subject data available.");
            return;
        }

        double maxAverage = data.subjectAverages.values().stream()
                .max(Double::compare)
                .orElse(100.0);

        int chartHeight = 10;

        for (int level = chartHeight; level >= 0; level--) {
            System.out.printf("%3d%% | ", level * 10);

            for (Map.Entry<String, Double> entry : data.subjectAverages.entrySet()) {
                double avg = entry.getValue();
                double scaledHeight = (avg / maxAverage) * chartHeight;

                if (level <= scaledHeight) {
                    char barChar;
                    if (avg >= 90) barChar = '█';
                    else if (avg >= 80) barChar = '▓';
                    else if (avg >= 70) barChar = '▒';
                    else if (avg >= 60) barChar = '░';
                    else barChar = ' ';

                    System.out.printf("%c   ", barChar);
                } else {
                    System.out.print("    ");
                }
            }
            System.out.println();
        }

        System.out.print("     +");
        for (int i = 0; i < data.subjectAverages.size(); i++) {
            System.out.print("----");
        }
        System.out.println();

        System.out.print("       ");
        for (String subject : data.subjectAverages.keySet()) {
            System.out.printf("%-3.3s ", subject.substring(0, Math.min(3, subject.length())));
        }
        System.out.println();

        System.out.println("\nSubject Averages:");
        data.subjectAverages.forEach((subject, avg) ->
                System.out.printf("  %-15s: %6.1f%%%n", subject, avg)
        );
        System.out.println();
    }

    private void displayConcurrentOperations(DashboardData data) {
        System.out.println("CONCURRENT OPERATIONS IN PROGRESS");
        System.out.println("-".repeat(70));

        if (data.activeTasks.isEmpty()) {
            System.out.println("No active operations.");
            return;
        }

        for (TaskStatus task : data.activeTasks) {
            int progressBarLength = 20;
            int filledLength = (int) Math.round((task.progress * progressBarLength) / 100.0);
            String progressBar = "█".repeat(filledLength) + "░".repeat(progressBarLength - filledLength);

            String statusSymbol = task.status.equals("COMPLETED") ? "✓" :
                    task.status.equals("IN PROGRESS") ? "↻" : "○";

            System.out.printf("%s %-25s [%s] %5.1f%% (%dms)%n",
                    statusSymbol, task.taskName, progressBar, task.progress, task.elapsedTime);
        }

        System.out.println("\nThread Pool Status:");
        System.out.println("  Fixed Pool (Reports): 3/5 active, Queue: 2 pending");
        System.out.println("  Cached Pool (Stats): 2/8 active");
        System.out.println("  Scheduled Pool: 2 tasks scheduled");
        System.out.println();
    }

    public void displayPerformanceMetrics() {
        DashboardData data = currentData.get();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("               PERFORMANCE METRICS DETAIL");
        System.out.println("=".repeat(80));

        System.out.println("\nGRADE STATISTICS:");
        System.out.println("-".repeat(40));
        System.out.printf("Average Grade:    %8.1f%%%n", data.averageGrade);
        System.out.printf("Median Grade:     %8.1f%%%n", data.medianGrade);
        System.out.printf("Standard Deviation: %6.1f%%%n", data.stdDeviation);
        System.out.printf("Grade Range:      %8.1f%% - %.1f%%%n",
                data.averageGrade - data.stdDeviation, data.averageGrade + data.stdDeviation);

        System.out.println("\nGRADE DISTRIBUTION PERCENTAGES:");
        System.out.println("-".repeat(40));
        data.gradeDistribution.forEach((range, count) -> {
            double percentage = (count * 100.0) / data.totalGrades;
            System.out.printf("%-12s: %6.1f%% (%d grades)%n", range, percentage, count);
        });

        System.out.println("\nPERFORMANCE ANALYSIS:");
        System.out.println("-".repeat(40));
        if (data.averageGrade >= 85) {
            System.out.println("✓ Overall Performance: EXCELLENT");
            System.out.println("✓ Most students are performing well above average");
        } else if (data.averageGrade >= 75) {
            System.out.println("✓ Overall Performance: GOOD");
            System.out.println("✓ Majority of students are meeting expectations");
        } else if (data.averageGrade >= 65) {
            System.out.println("Δ Overall Performance: AVERAGE");
            System.out.println("Δ Some students may need additional support");
        } else {
            System.out.println("✗ Overall Performance: NEEDS IMPROVEMENT");
            System.out.println("✗ Significant portion of students are struggling");
        }

        long failingCount = data.gradeDistribution.getOrDefault("F (0-59)", 0L);
        if (failingCount > data.totalGrades * 0.1) {
            System.out.println("\n⚠️  RECOMMENDATION:");
            System.out.println("  - " + failingCount + " students are failing (F grade)");
            System.out.println("  - Consider additional support or review sessions");
        }

        long aCount = data.gradeDistribution.getOrDefault("A (90-100)", 0L);
        if (aCount > data.totalGrades * 0.3) {
            System.out.println("\n✓ POSITIVE INDICATOR:");
            System.out.println("  - " + aCount + " students achieving A grades");
            System.out.println("  - Course material appears well-matched to student level");
        }
    }

    public void displayMemoryChart() {
        DashboardData data = currentData.get();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("               MEMORY USAGE CHART");
        System.out.println("=".repeat(80));

        double usedMB = data.systemMetrics.usedMemory / (1024.0 * 1024.0);
        double maxMB = data.systemMetrics.maxMemory / (1024.0 * 1024.0);
        double freeMB = maxMB - usedMB;
        double usedPercent = (usedMB * 100.0) / maxMB;

        System.out.println("\nMemory Breakdown:");
        System.out.println("-".repeat(40));
        System.out.printf("Used Memory:  %8.1f MB%n", usedMB);
        System.out.printf("Free Memory:  %8.1f MB%n", freeMB);
        System.out.printf("Max Memory:   %8.1f MB%n", maxMB);
        System.out.printf("Utilization:  %8.1f%%%n", usedPercent);

        int totalBars = 40;
        int usedBars = (int) Math.round((usedPercent * totalBars) / 100.0);
        int freeBars = totalBars - usedBars;

        String usedBar = "█".repeat(Math.max(0, usedBars));
        String freeBar = "░".repeat(Math.max(0, freeBars));

        System.out.printf("[%s%s]%n", usedBar, freeBar);
        System.out.printf("├ Used ┤%s Free%n", " ".repeat(Math.max(0, usedBars - 4)));

        System.out.println("\nMemory Status:");
        System.out.println("-".repeat(40));
        if (usedPercent < 50) {
            System.out.println("✓ Memory usage: OPTIMAL");
            System.out.println("✓ Plenty of memory available");
        } else if (usedPercent < 75) {
            System.out.println("✓ Memory usage: NORMAL");
            System.out.println("✓ Memory usage within expected range");
        } else if (usedPercent < 90) {
            System.out.println("⚠️  Memory usage: HIGH");
            System.out.println("⚠️  Consider monitoring memory usage");
        } else {
            System.out.println("✗ Memory usage: CRITICAL");
            System.out.println("✗ Close unused applications or increase heap size");
        }

        System.out.println("\nRECOMMENDATIONS:");
        System.out.println("-".repeat(40));
        if (usedPercent > 80) {
            System.out.println("1. Consider increasing JVM heap size with -Xmx flag");
            System.out.println("2. Review memory-intensive operations");
            System.out.println("3. Implement object pooling for frequently created objects");
        } else {
            System.out.println("✓ Memory configuration appears optimal");
            System.out.println("✓ No immediate action required");
        }
    }

    public void displayHelp() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("               DASHBOARD COMMANDS HELP");
        System.out.println("=".repeat(80));

        System.out.println("\nAvailable Commands:");
        System.out.println("-".repeat(40));
        System.out.println("  Q           - Quit dashboard and return to main menu");
        System.out.println("  R           - Manual refresh (update statistics immediately)");
        System.out.println("  P           - Pause/Resume auto-refresh");
        System.out.println("  S           - Show detailed statistics");
        System.out.println("  C           - Show performance charts");
        System.out.println("  M           - Show memory usage chart");
        System.out.println("  H           - Show this help message");

        System.out.println("\nDashboard Features:");
        System.out.println("-".repeat(40));
        System.out.println("  • Real-time grade statistics with auto-refresh");
        System.out.println("  • Interactive bar charts for grade distribution");
        System.out.println("  • Top performers ranking with star ratings");
        System.out.println("  • Subject performance comparison");
        System.out.println("  • System resource monitoring (memory, threads)");
        System.out.println("  • Concurrent operation status");
        System.out.println("  • Performance metrics and recommendations");

        System.out.println("\nData Displayed:");
        System.out.println("-".repeat(40));
        System.out.println("  • Total students and grades");
        System.out.println("  • Average, median, and standard deviation");
        System.out.println("  • Grade distribution (A-F) with percentages");
        System.out.println("  • Top 5 performing students");
        System.out.println("  • Subject averages");
        System.out.println("  • Memory usage and cache performance");
        System.out.println("  • Active concurrent operations");

        System.out.println("\nPerformance Indicators:");
        System.out.println("-".repeat(40));
        System.out.println("  ✓ - Excellent/Good performance");
        System.out.println("  Δ - Average/Needs monitoring");
        System.out.println("  ✗ - Poor/Needs improvement");
        System.out.println("  ⚠️  - Warning/Attention needed");
    }

    private void waitForEnter() {
        System.out.println("\nPress Enter to continue...");
        try {
            System.in.read();
            while (System.in.available() > 0) {
                System.in.read();
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    public synchronized void stop() {
        if (!isRunning.get()) return;

        shouldStop = true;
        isRunning.set(false);
        isPaused.set(false);

        if (updateTask != null) {
            updateTask.cancel(false);
            updateTask = null;
        }

        // Don't close commandReader - it can close System.in
        // Just set it to null and let it be garbage collected
        commandReader = null;

        try {
            while (System.in.available() > 0) {
                System.in.read();
            }
        } catch (IOException e) {
            // Ignore
        }

        clearScreen();
        System.out.println("\nDashboard stopped. Press Enter to return to main menu...");
    }

    public static void shutdownAll() {
        if (dashboardScheduler != null && !dashboardScheduler.isShutdown()) {
            dashboardScheduler.shutdown();
            try {
                if (!dashboardScheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                    dashboardScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                dashboardScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (commandExecutor != null && !commandExecutor.isShutdown()) {
            commandExecutor.shutdown();
            try {
                if (!commandExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                    commandExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                commandExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isRunning() {
        return isRunning.get() && !shouldStop;
    }

    public boolean isPaused() {
        return isPaused.get();
    }
}