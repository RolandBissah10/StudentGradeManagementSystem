package services;

import models.*;
import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class StatisticsDashboard {
    private final StudentManager studentManager;
    private final GradeManager gradeManager;
    private final CacheManager cacheManager;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicReference<DashboardData> currentData = new AtomicReference<>(new DashboardData());
    private ScheduledExecutorService dashboardScheduler;
    private ScheduledFuture<?> updateTask;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FULL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static class DashboardData {
        LocalDateTime timestamp;
        int totalStudents;
        int totalGrades;
        double averageGrade;
        double medianGrade;
        double stdDeviation;
        Map<String, Long> gradeDistribution;
        List<StudentPerformance> topPerformers;
        Map<String, Long> studentTypeDistribution;
        Map<String, Double> subjectAverages;
        SystemMetrics systemMetrics;
        int activeThreads;
        double cacheHitRate;
        List<TaskStatus> activeTasks;

        DashboardData() {
            this.timestamp = LocalDateTime.now();
            this.gradeDistribution = new LinkedHashMap<>();
            this.topPerformers = new ArrayList<>();
            this.studentTypeDistribution = new HashMap<>();
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
        int threadCount;
        long gcCount;

        SystemMetrics() {
            Runtime runtime = Runtime.getRuntime();
            this.usedMemory = runtime.totalMemory() - runtime.freeMemory();
            this.maxMemory = runtime.maxMemory();
            this.availableProcessors = runtime.availableProcessors();
            this.threadCount = Thread.activeCount();
            this.gcCount = 0; // Would need GC monitoring to update this
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

    public StatisticsDashboard(StudentManager studentManager, GradeManager gradeManager,
                               CacheManager cacheManager) {
        this.studentManager = studentManager;
        this.gradeManager = gradeManager;
        this.cacheManager = cacheManager;
    }

    public void startDashboard(int refreshIntervalSeconds) {
        if (isRunning.get()) {
            System.out.println("Dashboard is already running!");
            return;
        }

        isRunning.set(true);
        dashboardScheduler = Executors.newScheduledThreadPool(1);

        System.out.println("\n" + "=".repeat(100));
        System.out.println("              REAL-TIME STATISTICS DASHBOARD v3.0");
        System.out.println("=".repeat(100));
        System.out.printf("Auto-refresh: Enabled (%d seconds) | Thread: RUNNING%n", refreshIntervalSeconds);
        System.out.println("Commands: [Q]uit [R]efresh [P]ause [S]tats [C]hart [M]etrics");
        System.out.println();

        // Initial update
        updateStatistics();
        displayDashboard();

        // Schedule periodic updates
        updateTask = dashboardScheduler.scheduleAtFixedRate(() -> {
            if (isRunning.get()) {
                updateStatistics();
                displayDashboard();
            }
        }, refreshIntervalSeconds, refreshIntervalSeconds, TimeUnit.SECONDS);
    }

    private void updateStatistics() {
        DashboardData data = new DashboardData();
        data.timestamp = LocalDateTime.now();

        // Basic statistics
        List<Student> students = studentManager.getStudents();
        List<Grade> allGrades = getAllGrades();

        data.totalStudents = students.size();
        data.totalGrades = allGrades.size();

        // Grade statistics
        if (!allGrades.isEmpty()) {
            List<Double> grades = allGrades.stream()
                    .map(Grade::getGrade)
                    .sorted()
                    .collect(Collectors.toList());

            // Average
            data.averageGrade = grades.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            // Median
            if (grades.size() % 2 == 0) {
                data.medianGrade = (grades.get(grades.size() / 2 - 1) + grades.get(grades.size() / 2)) / 2.0;
            } else {
                data.medianGrade = grades.get(grades.size() / 2);
            }

            // Standard deviation
            double variance = grades.stream()
                    .mapToDouble(g -> Math.pow(g - data.averageGrade, 2))
                    .average()
                    .orElse(0.0);
            data.stdDeviation = Math.sqrt(variance);

            // Grade distribution with bar chart data
            data.gradeDistribution = calculateGradeDistribution(allGrades);
        }

        // Top performers
        data.topPerformers = students.stream()
                .map(s -> {
                    double avg = gradeManager.calculateOverallAverage(s.getStudentId());
                    double gpa = convertToGPA(avg);
                    return new StudentPerformance(s.getStudentId(), s.getName(), avg, gpa);
                })
                .sorted((a, b) -> Double.compare(b.averageGrade, a.averageGrade))
                .limit(5)
                .collect(Collectors.toList());

        // Student type distribution
        data.studentTypeDistribution = students.stream()
                .collect(Collectors.groupingBy(
                        Student::getStudentType,
                        Collectors.counting()
                ));

        // Subject averages
        data.subjectAverages = calculateSubjectAverages(allGrades);

        // System metrics
        data.systemMetrics = new SystemMetrics();
        data.activeThreads = Thread.activeCount();

        // Simulated cache hit rate (in real implementation, get from cache manager)
        data.cacheHitRate = Math.random() * 30 + 70; // 70-100% for demo

        // Simulated active tasks
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

        // Clear screen (simulated)
        System.out.print("\033[H\033[2J");
        System.out.flush();

        System.out.println("\n" + "=".repeat(100));
        System.out.println("              REAL-TIME STATISTICS DASHBOARD");
        System.out.println("=".repeat(100));
        System.out.printf("Last Updated: %s | Status: %s%n",
                data.timestamp.format(FULL_FORMATTER),
                isRunning.get() ? "RUNNING" : "PAUSED");
        System.out.println();

        // SYSTEM STATUS with bar charts
        displaySystemStatus(data);

        // LIVE STATISTICS with bar charts
        displayLiveStatistics(data);

        // GRADE DISTRIBUTION with horizontal bar chart
        displayGradeDistributionChart(data);

        // TOP PERFORMERS
        displayTopPerformers(data);

        // SUBJECT PERFORMANCE with bar chart
        displaySubjectPerformanceChart(data);

        // CONCURRENT OPERATIONS
        displayConcurrentOperations(data);

        // Commands
        System.out.println("\n" + "=".repeat(100));
        System.out.println("Commands: [Q]uit [R]efresh [P]ause/Resume [S]tats [C]hart [M]etrics [H]elp");
        System.out.print("Command: ");
    }

    private void displaySystemStatus(DashboardData data) {
        System.out.println("SYSTEM STATUS");
        System.out.println("-".repeat(50));

        System.out.printf("Total Students: %-5d | Total Grades: %-6d%n",
                data.totalStudents, data.totalGrades);

        // Memory usage bar chart
        double memoryPercent = (data.systemMetrics.usedMemory * 100.0) / data.systemMetrics.maxMemory;
        int memoryBarLength = (int) Math.round(memoryPercent / 2.5); // Scale to 40 chars
        String memoryBar = "█".repeat(Math.max(1, memoryBarLength)) +
                "░".repeat(Math.max(0, 40 - memoryBarLength));

        System.out.printf("Memory Usage: %.1f MB / %.1f MB%n",
                data.systemMetrics.usedMemory / (1024.0 * 1024.0),
                data.systemMetrics.maxMemory / (1024.0 * 1024.0));
        System.out.printf("  [%s] %.1f%%%n", memoryBar, memoryPercent);

        // Cache hit rate bar chart
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

        // Trend indicator
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

        // Find max count for scaling
        long maxCount = data.gradeDistribution.values().stream()
                .max(Long::compare)
                .orElse(1L);

        // Display bar chart for each grade range
        for (Map.Entry<String, Long> entry : data.gradeDistribution.entrySet()) {
            String range = entry.getKey();
            long count = entry.getValue();
            double percentage = (count * 100.0) / data.totalGrades;

            // Create bar with proper scaling
            int barLength = maxCount > 0 ? (int) Math.round((count * 50.0) / maxCount) : 0;
            String bar = "█".repeat(Math.max(1, barLength));

            // Color coding
            String colorCode = "";
            if (range.startsWith("A")) colorCode = "🟢"; // Green for A
            else if (range.startsWith("B")) colorCode = "🟡"; // Yellow for B
            else if (range.startsWith("C")) colorCode = "🟠"; // Orange for C
            else if (range.startsWith("D")) colorCode = "🔴"; // Red for D
            else colorCode = "⚫"; // Black for F

            System.out.printf("%s %-12s: %5d grades %s %6.1f%%%n",
                    colorCode, range, count, bar, percentage);
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

            // Performance indicator bars
            int starCount = (int) Math.round(perf.averageGrade / 20); // 5 stars max
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

        // Find max average for scaling
        double maxAverage = data.subjectAverages.values().stream()
                .max(Double::compare)
                .orElse(100.0);

        // Display vertical bar chart
        int chartHeight = 10;

        for (int level = chartHeight; level >= 0; level--) {
            System.out.printf("%3d%% | ", level * 10);

            for (Map.Entry<String, Double> entry : data.subjectAverages.entrySet()) {
                double avg = entry.getValue();
                double scaledHeight = (avg / maxAverage) * chartHeight;

                if (level <= scaledHeight) {
                    // Choose bar character based on performance
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

        // X-axis labels
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

        // Subject averages
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
            // Progress bar
            int progressBarLength = 20;
            int filledLength = (int) Math.round((task.progress * progressBarLength) / 100.0);
            String progressBar = "█".repeat(filledLength) + "░".repeat(progressBarLength - filledLength);

            String statusSymbol = task.status.equals("COMPLETED") ? "✓" :
                    task.status.equals("IN PROGRESS") ? "↻" : "○";

            System.out.printf("%s %-25s [%s] %5.1f%% (%dms)%n",
                    statusSymbol, task.taskName, progressBar, task.progress, task.elapsedTime);
        }

        // Thread pool simulation
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

        // Grade distribution percentages
        System.out.println("\nGRADE DISTRIBUTION PERCENTAGES:");
        System.out.println("-".repeat(40));
        data.gradeDistribution.forEach((range, count) -> {
            double percentage = (count * 100.0) / data.totalGrades;
            System.out.printf("%-12s: %6.1f%% (%d grades)%n", range, percentage, count);
        });

        // Performance analysis
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

        // Recommendations based on distribution
        long failingCount = data.gradeDistribution.getOrDefault("F (0-59)", 0L);
        if (failingCount > data.totalGrades * 0.1) { // More than 10% failing
            System.out.println("\n⚠️  RECOMMENDATION:");
            System.out.println("  - " + failingCount + " students are failing (F grade)");
            System.out.println("  - Consider additional support or review sessions");
        }

        long aCount = data.gradeDistribution.getOrDefault("A (90-100)", 0L);
        if (aCount > data.totalGrades * 0.3) { // More than 30% getting A
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

        // Stacked bar chart
        System.out.println("\nMemory Allocation:");
        System.out.println("-".repeat(40));

        int totalBars = 40;
        int usedBars = (int) Math.round((usedPercent * totalBars) / 100.0);
        int freeBars = totalBars - usedBars;

        String usedBar = "█".repeat(Math.max(0, usedBars));
        String freeBar = "░".repeat(Math.max(0, freeBars));

        System.out.printf("[%s%s]%n", usedBar, freeBar);
        System.out.printf("├ Used ┤%s Free%n", " ".repeat(usedBars - 4));

        // Memory status
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

        // Recommendations
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

    public void pause() {
        isRunning.set(false);
        if (updateTask != null) {
            updateTask.cancel(false);
        }
        System.out.println("\nDashboard paused. Press 'R' to resume.");
    }

    public void resume() {
        if (!isRunning.get()) {
            isRunning.set(true);
            System.out.println("Dashboard resumed.");
            updateStatistics();
            displayDashboard();
        }
    }

    public void stop() {
        isRunning.set(false);
        if (updateTask != null) {
            updateTask.cancel(false);
        }
        if (dashboardScheduler != null) {
            dashboardScheduler.shutdown();
            try {
                if (!dashboardScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    dashboardScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                dashboardScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Dashboard stopped.");
    }

    public boolean isRunning() {
        return isRunning.get();
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

        System.out.println("\nPress Enter to return to dashboard...");
    }
}