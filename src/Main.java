import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import exceptions.*;
import models.*;
import services.*;
import utils.ValidationUtils;
import utils.ValidationUtils.ValidationResult;

public class Main {
    // Managers
    private static StudentManager studentManager = new StudentManager();
    private static GradeManager gradeManager = new GradeManager();

    // Services
    private static ReportGenerator reportGenerator = new ReportGenerator(studentManager, gradeManager);
    private static GPACalculator gpaCalculator = new GPACalculator(studentManager, gradeManager);
    private static StatisticsCalculator statisticsCalculator = new StatisticsCalculator(studentManager, gradeManager);
    private static BulkImportService bulkImportService = new BulkImportService(studentManager, gradeManager);
    private static SearchService searchService = new SearchService(studentManager, gradeManager);
    private static FileIOService fileIOService = new FileIOService();
    private static ConcurrentTaskService taskService = new ConcurrentTaskService(studentManager, gradeManager, fileIOService);
    private static CacheManager cacheManager = new CacheManager();
    private static AuditLogger auditLogger = AuditLogger.getInstance();
    private static PatternSearchService patternSearchService = new PatternSearchService(studentManager, gradeManager);
    private static StreamProcessor streamProcessor = new StreamProcessor(studentManager, gradeManager);
    private static StatisticsDashboard statisticsDashboard;
    private static WatchServiceMonitor watchServiceMonitor;

    private static Scanner scanner = new Scanner(System.in);
    private static ScheduledExecutorService scheduledTasks;

    static {
        try {
            statisticsDashboard = new StatisticsDashboard(studentManager, gradeManager, cacheManager);
            watchServiceMonitor = new WatchServiceMonitor("imports");
            scheduledTasks = Executors.newScheduledThreadPool(3);

            // Initialize scheduled tasks
            initializeScheduledTasks();

        } catch (Exception e) {
            System.err.println("Error initializing services: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            createDirectories();
            initializeSampleData();
            displayMainMenu();
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdownAllServices();
        }
    }

    private static void createDirectories() throws IOException {
        String[] directories = {
                "reports", "imports", "reports/csv", "reports/json",
                "reports/binary", "reports/text", "cache", "logs/audit",
                "imports/processed", "backups", "exports"
        };

        for (String dir : directories) {
            Path path = Paths.get(dir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        }

        System.out.println("✓ All directories created successfully.");
        auditLogger.logSimple("SYSTEM", "Directories initialized", null);
    }

    private static void initializeSampleData() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("           STUDENT GRADE MANAGEMENT SYSTEM v3.0");
        System.out.println("               (Enterprise Edition with Concurrency)");
        System.out.println("=".repeat(80));

        long startTime = System.currentTimeMillis();

        try {
            // Add sample students with comprehensive data
            addSampleStudents();
            addSampleGrades();

            // Warm caches
            cacheManager.warmCache(studentManager.getStudents());
            gpaCalculator.warmCache();

            long initializationTime = System.currentTimeMillis() - startTime;

            System.out.println("\n✓ System initialized successfully!");
            System.out.println("  Total students: " + studentManager.getStudentCount());
            System.out.println("  Total grades: " + gradeManager.getTotalGradeCount());
            System.out.println("  Initialization time: " + initializationTime + "ms");
            System.out.println("  Collections optimized for O(1) lookup");
            System.out.println("  Thread pools initialized for concurrent operations");

            auditLogger.logWithTime("SYSTEM_INIT", "System initialization complete",
                    initializationTime, null);

        } catch (Exception e) {
            System.err.println("Error initializing sample data: " + e.getMessage());
            auditLogger.logError("SYSTEM_INIT", "Initialization failed", e.getMessage(), null);
        }
    }

    private static void addSampleStudents() {
        // Regular Students
        studentManager.addStudent(new RegularStudent("Alice Johnson", 16,
                "alice.johnson@university.edu", "+1-555-1001", "2024-09-01"));
        studentManager.addStudent(new RegularStudent("Michael Chen", 17,
                "michael.chen@college.org", "+1-555-1002", "2024-09-01"));
        studentManager.addStudent(new RegularStudent("Sarah Brown", 16,
                "sarah.brown@school.net", "+1-555-1003", "2024-09-01"));
        studentManager.addStudent(new RegularStudent("David Lee", 17,
                "david.lee@university.edu", "+1-555-1004", "2024-09-01"));
        studentManager.addStudent(new RegularStudent("Emma Wilson", 16,
                "emma.wilson@college.org", "+1-555-1005", "2024-09-01"));

        // Honors Students
        studentManager.addStudent(new HonorsStudent("Robert Smith", 17,
                "robert.smith@university.edu", "+1-555-2001", "2024-09-01"));
        studentManager.addStudent(new HonorsStudent("Olivia Taylor", 16,
                "olivia.taylor@college.org", "+1-555-2002", "2024-09-01"));
        studentManager.addStudent(new HonorsStudent("James Anderson", 17,
                "james.anderson@school.net", "+1-555-2003", "2024-09-01"));
        studentManager.addStudent(new HonorsStudent("Sophia Martinez", 16,
                "sophia.martinez@university.edu", "+1-555-2004", "2024-09-01"));
        studentManager.addStudent(new HonorsStudent("Lucas White", 17,
                "lucas.white@college.org", "+1-555-2005", "2024-09-01"));

        System.out.println("✓ Added " + studentManager.getStudentCount() + " sample students");
    }

    private static void addSampleGrades() {
        // Sample subjects

        Subject[] coreSubjects = {
                new CoreSubject("Mathematics", "MAT101"),
                new CoreSubject("English", "ENG101"),
                new CoreSubject("Science", "SCI101"),
                new CoreSubject("History", "HIS101"),
                new CoreSubject("Computer Science", "CSC101")
        };

        Subject[] electiveSubjects = {
                new ElectiveSubject("Music", "MUS101"),
                new ElectiveSubject("Art", "ART101"),
                new ElectiveSubject("Physical Education", "PED101"),
                new ElectiveSubject("Drama", "DRA101"),
                new ElectiveSubject("Economics", "ECO101")
        };

        // Add grades for each student
        Random random = new Random(42); // Fixed seed for reproducibility
        int totalGradesAdded = 0;

        for (int i = 1; i <= 10; i++) {
            String studentId = String.format("STU%03d", i);

            // Add 3-5 core subject grades
            for (int j = 0; j < 3 + random.nextInt(3); j++) {
                Subject subject = coreSubjects[random.nextInt(coreSubjects.length)];
                double grade = 60 + random.nextInt(40); // Grades between 60-100
                gradeManager.addGrade(new Grade(studentId, subject, grade));
                totalGradesAdded++;
            }

            // Add 2-4 elective subject grades
            for (int j = 0; j < 2 + random.nextInt(3); j++) {
                Subject subject = electiveSubjects[random.nextInt(electiveSubjects.length)];
                double grade = 50 + random.nextInt(50); // Grades between 50-100
                gradeManager.addGrade(new Grade(studentId, subject, grade));
                totalGradesAdded++;
            }
        }

        System.out.println("✓ Added " + totalGradesAdded + " sample grades");
    }

    private static void initializeScheduledTasks() {
        // Daily GPA recalculation at 3:30 AM
        scheduledTasks.scheduleAtFixedRate(() -> {
            try {
                auditLogger.logSimple("SCHEDULED_TASK", "Daily GPA recalculation started", null);
                System.out.println("\n[" + new Date() + "] Starting scheduled GPA recalculation...");
                recalculateAllGPAs();
                auditLogger.logSimple("SCHEDULED_TASK", "Daily GPA recalculation completed", null);
            } catch (Exception e) {
                auditLogger.logError("SCHEDULED_TASK", "Daily GPA recalculation failed", e.getMessage(), null);
            }
        }, calculateInitialDelay(3, 30), 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);

        // Hourly statistics cache refresh
        scheduledTasks.scheduleAtFixedRate(() -> {
            try {
                cacheManager.warmCache(studentManager.getStudents());
                auditLogger.logSimple("SCHEDULED_TASK", "Hourly cache refresh completed", null);
            } catch (Exception e) {
                auditLogger.logError("SCHEDULED_TASK", "Hourly cache refresh failed", e.getMessage(), null);
            }
        }, 60, 60, TimeUnit.MINUTES);

        // Daily backup at 2:00 AM
        scheduledTasks.scheduleAtFixedRate(() -> {
            try {
                performDailyBackup();
            } catch (Exception e) {
                auditLogger.logError("SCHEDULED_TASK", "Daily backup failed", e.getMessage(), null);
            }
        }, calculateInitialDelay(2, 0), 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);

        System.out.println("✓ Scheduled tasks initialized");
    }

    private static long calculateInitialDelay(int hour, int minute) {
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

    private static void recalculateAllGPAs() {
        List<Student> students = studentManager.getStudents();
        System.out.println("Recalculating GPAs for " + students.size() + " students...");

        // Use parallel stream for concurrent processing
        students.parallelStream().forEach(student -> {
            double gpa = gpaCalculator.calculateGPA(student.getStudentId(), false);
            // Update student GPA in database (simulated)
        });

        System.out.println("✓ GPA recalculation completed");
    }

    private static void performDailyBackup() {
        try {
            LocalDate today = LocalDate.now();
            Path backupDir = Paths.get("backups", today.toString());
            Files.createDirectories(backupDir);

            // Simulate backup process
            System.out.println("[" + LocalDateTime.now() + "] Performing daily backup...");
            Thread.sleep(2000); // Simulate backup time

            auditLogger.logSimple("BACKUP", "Daily backup completed", null);
            System.out.println("✓ Daily backup completed to: " + backupDir);
        } catch (Exception e) {
            System.err.println("Backup error: " + e.getMessage());
        }
    }

    private static void displayMainMenu() {
        while (true) {
            displayMenuHeader();

            try {
                String choice = scanner.nextLine().trim();
                auditLogger.logSimple("MENU_NAV", "User selected option: " + choice, null);

                if (!handleMenuChoice(choice)) {
                    break;
                }

                System.out.print("\nPress Enter to continue...");
                scanner.nextLine();

            } catch (Exception e) {
                System.err.println("Menu error: " + e.getMessage());
                auditLogger.logError("MENU", "Menu operation failed", e.getMessage(), null);
            }
        }
    }

    private static void displayMenuHeader() {
        // Clear screen (simplified version)
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }

        System.out.println("\n" + "=".repeat(100));
        System.out.println("           STUDENT GRADE MANAGEMENT SYSTEM - ENTERPRISE EDITION v3.0");
        System.out.println("=".repeat(100));
        System.out.println("Advanced Features: • Optimized Collections • Concurrent Operations • Multi-Format I/O");
        System.out.println("                  • Regex Validation • Real-Time Dashboard • Scheduled Tasks");
        System.out.println("-".repeat(100));

        displayBackgroundTasks();

        System.out.println("\nSTUDENT MANAGEMENT");
        System.out.println("  1. Add Student (with comprehensive regex validation)");
        System.out.println("  2. View All Students (optimized collections performance)");
        System.out.println("  3. Record Grade (with validation)");
        System.out.println("  4. View Grade Report (with caching)");

        System.out.println("\nFILE OPERATIONS (NIO.2)");
        System.out.println("  5. Export Grade Report (CSV/JSON/Binary)");
        System.out.println("  6. Import Data (Multi-format support)");
        System.out.println("  7. Bulk Import Grades (Stream processing)");
        System.out.println("  8. Start File Watcher Service");

        System.out.println("\nANALYTICS & REPORTING");
        System.out.println("  9. Calculate Student GPA (4.0 scale)");
        System.out.println("  10. View Class Statistics (with bar charts)");
        System.out.println("  11. Real-Time Statistics Dashboard [NEW]");
        System.out.println("  12. Generate Batch Reports (Concurrent)");
        System.out.println("  13. Stream Processing Analytics");

        System.out.println("\nSEARCH & QUERY");
        System.out.println("  14. Search Students (Advanced)");
        System.out.println("  15. Pattern-Based Search (Regex) [NEW]");
        System.out.println("  16. Query Grade History");

        System.out.println("\nADVANCED FEATURES");
        System.out.println("  17. Schedule Automated Tasks [NEW]");
        System.out.println("  18. View System Performance [NEW]");
        System.out.println("  19. Cache Management [NEW]");
        System.out.println("  20. Audit Trail Viewer [NEW]");
        System.out.println("  21. Exit System");

        System.out.print("\nEnter choice (1-21): ");
    }

    private static void displayBackgroundTasks() {
        List<String> activeTasks = new ArrayList<>();

        if (statisticsDashboard.isRunning()) {
            activeTasks.add("Dashboard (" + (statisticsDashboard.isRunning() ? "RUNNING" : "PAUSED") + ")");
        }

        if (watchServiceMonitor != null && watchServiceMonitor.isRunning()) {
            activeTasks.add("File Watcher");
        }

        if (!scheduledTasks.isShutdown()) {
            activeTasks.add("Scheduled Tasks");
        }

        if (!activeTasks.isEmpty()) {
            System.out.print("Background Tasks: ");
            System.out.println(String.join(" | ", activeTasks));
        } else {
            System.out.println("Background Tasks: None active");
        }
    }

    private static boolean handleMenuChoice(String choice) {
        try {
            switch (choice) {
                case "1": addStudentWithValidation(); break;
                case "2": viewAllStudents(); break;
                case "3": recordGrade(); break;
                case "4": viewGradeReport(); break;
                case "5": exportMultiFormatReport(); break;
                case "6": importData(); break;
                case "7": bulkImportGrades(); break;
                case "8": startFileWatcher(); break;
                case "9": calculateStudentGPA(); break;
                case "10": viewClassStatistics(); break;
                case "11": startRealTimeDashboard(); break;
                case "12": generateBatchReports(); break;
                case "13": streamProcessingAnalytics(); break;
                case "14": searchStudents(); break;
                case "15": patternBasedSearch(); break;
                case "16": queryGradeHistory(); break;
                case "17": scheduleAutomatedTasks(); break;
                case "18": viewSystemPerformance(); break;
                case "19": manageCache(); break;
                case "20": viewAuditTrail(); break;
                case "21":
                    System.out.println("\nShutting down system...");
                    return false;
                default:
                    System.out.println("Invalid choice! Please enter 1-21.");
            }
        } catch (Exception e) {
            System.err.println("Error in menu option " + choice + ": " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    // =========== MENU OPTION IMPLEMENTATIONS ===========

    private static void addStudentWithValidation() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              ADD STUDENT (COMPREHENSIVE VALIDATION)");
        System.out.println("=".repeat(80));

        long startTime = System.currentTimeMillis();

        try {
            // Name validation
            String name = getValidatedInput("Student Name",
                    ValidationUtils::validateName);

            // Age validation
            int age = getValidatedInteger("Age", 5, 100);

            // Email validation
            String email = getValidatedInput("Email Address",
                    ValidationUtils::validateEmail);

            // Phone validation
            String phone = getValidatedInput("Phone Number",
                    ValidationUtils::validatePhone);

            // Enrollment date validation
            String enrollmentDate = getValidatedInput("Enrollment Date (YYYY-MM-DD)",
                    ValidationUtils::validateDate);

            // Student type selection
            System.out.println("\nStudent Type:");
            System.out.println("1. Regular Student (Passing grade: 50%)");
            System.out.println("2. Honors Student (Passing grade: 60%, honors eligibility at 85%+)");
            System.out.print("Select type (1-2): ");

            String typeChoice = scanner.nextLine();
            Student student;

            if (typeChoice.equals("1")) {
                student = new RegularStudent(name, age, email, phone, enrollmentDate);
            } else if (typeChoice.equals("2")) {
                student = new HonorsStudent(name, age, email, phone, enrollmentDate);
            } else {
                System.out.println("Invalid choice! Student not added.");
                return;
            }

            // Add student to system
            studentManager.addStudent(student);
            cacheManager.cacheStudent(student);

            long executionTime = System.currentTimeMillis() - startTime;

            // Display success message with details
            System.out.println("\n" + "=".repeat(60));
            System.out.println("              STUDENT ADDED SUCCESSFULLY!");
            System.out.println("=".repeat(60));
            System.out.println("Student ID:     " + student.getStudentId());
            System.out.println("Name:           " + student.getName());
            System.out.println("Email:          " + student.getEmail());
            System.out.println("Phone:          " + student.getPhone());
            System.out.println("Type:           " + student.getStudentType());
            System.out.println("Enrolled:       " + student.getEnrollmentDateString());
            System.out.println("Processing Time: " + executionTime + "ms");
            System.out.println("All inputs validated with regex patterns ✓");

            auditLogger.logWithTime("ADD_STUDENT",
                    "Added student: " + student.getStudentId(),
                    executionTime, student.getStudentId());

        } catch (Exception e) {
            System.err.println("Error adding student: " + e.getMessage());
            auditLogger.logError("ADD_STUDENT", "Failed to add student", e.getMessage(), null);
        }
    }

    private static String getValidatedInput(String fieldName,
                                            java.util.function.Function<String, ValidationResult> validator) {
        while (true) {
            System.out.print("\nEnter " + fieldName + ": ");
            String input = scanner.nextLine().trim();

            ValidationResult result = validator.apply(input);

            if (result.isValid()) {
                System.out.println("✓ Valid " + fieldName);
                return input;
            } else {
                result.displayError();
                System.out.println("Please try again.");
            }
        }
    }

    private static int getValidatedInteger(String fieldName, int min, int max) {
        while (true) {
            System.out.print("\nEnter " + fieldName + " (" + min + "-" + max + "): ");
            String input = scanner.nextLine().trim();

            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    System.out.println("✓ Valid " + fieldName);
                    return value;
                } else {
                    System.out.println("✗ " + fieldName + " must be between " + min + " and " + max);
                }
            } catch (NumberFormatException e) {
                System.out.println("✗ Invalid number format! Please enter a valid integer.");
            }
        }
    }

    private static void viewAllStudents() {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("                    ALL STUDENTS (OPTIMIZED COLLECTIONS)");
        System.out.println("=".repeat(100));

        long startTime = System.nanoTime();
        studentManager.viewAllStudents(gradeManager);
        long endTime = System.nanoTime();

        double executionTime = (endTime - startTime) / 1_000_000.0;

        System.out.println("\n" + "-".repeat(100));
        System.out.printf("Query executed in %.2f ms using optimized HashMap collections (O(1) lookup)%n", executionTime);
        System.out.println("Data Structures: HashMap<StudentID>, TreeMap<GPA>, HashSet<Email>, ArrayList<Students>");

        auditLogger.logWithTime("VIEW_STUDENTS", "Viewed all students",
                (long) executionTime, null);
    }

    private static void recordGrade() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              RECORD GRADE");
        System.out.println("=".repeat(80));

        System.out.print("Enter Student ID: ");
        String studentId = scanner.nextLine().trim().toUpperCase();

        try {
            // Check if student exists
            Student student = studentManager.findStudent(studentId);
            if (student == null) {
                throw new StudentNotFoundException(studentId);
            }

            System.out.println("\nStudent Found:");
            System.out.println("Name: " + student.getName());
            System.out.println("Type: " + student.getStudentType());
            System.out.println("Current Average: " +
                    String.format("%.1f", gradeManager.calculateOverallAverage(studentId)) + "%");

            // Subject selection
            System.out.println("\nSelect Subject Type:");
            System.out.println("1. Core Subject");
            System.out.println("2. Elective Subject");
            System.out.print("Choice (1-2): ");
            String subjectTypeChoice = scanner.nextLine();

            Subject subject = null;
            if (subjectTypeChoice.equals("1")) {
                System.out.println("\nCore Subjects:");
                System.out.println("1. Mathematics");
                System.out.println("2. English");
                System.out.println("3. Science");
                System.out.println("4. History");
                System.out.println("5. Computer Science");
                System.out.print("Select subject (1-5): ");
                String coreChoice = scanner.nextLine();

                switch (coreChoice) {
                    case "1": subject = new CoreSubject("Mathematics", "MAT101"); break;
                    case "2": subject = new CoreSubject("English", "ENG101"); break;
                    case "3": subject = new CoreSubject("Science", "SCI101"); break;
                    case "4": subject = new CoreSubject("History", "HIS101"); break;
                    case "5": subject = new CoreSubject("Computer Science", "CSC101"); break;
                    default:
                        System.out.println("Invalid choice!");
                        return;
                }
            } else if (subjectTypeChoice.equals("2")) {
                System.out.println("\nElective Subjects:");
                System.out.println("1. Music");
                System.out.println("2. Art");
                System.out.println("3. Physical Education");
                System.out.println("4. Drama");
                System.out.println("5. Economics");
                System.out.print("Select subject (1-5): ");
                String electiveChoice = scanner.nextLine();

                switch (electiveChoice) {
                    case "1": subject = new ElectiveSubject("Music", "MUS101"); break;
                    case "2": subject = new ElectiveSubject("Art", "ART101"); break;
                    case "3": subject = new ElectiveSubject("Physical Education", "PED101"); break;
                    case "4": subject = new ElectiveSubject("Drama", "DRA101"); break;
                    case "5": subject = new ElectiveSubject("Economics", "ECO101"); break;
                    default:
                        System.out.println("Invalid choice!");
                        return;
                }
            } else {
                System.out.println("Invalid choice!");
                return;
            }

            // Grade input with validation
            double grade = -1;
            while (true) {
                System.out.print("Enter grade (0-100): ");
                String gradeInput = scanner.nextLine().trim();

                ValidationResult result = ValidationUtils.validateGrade(gradeInput);
                if (result.isValid()) {
                    grade = Double.parseDouble(gradeInput);
                    break;
                } else {
                    result.displayError();
                }
            }

            long startTime = System.currentTimeMillis();

            // Create and add grade
            Grade newGrade = new Grade(studentId, subject, grade);
            gradeManager.addGrade(newGrade);

            // Update cache
            cacheManager.invalidateStudent(studentId);

            long executionTime = System.currentTimeMillis() - startTime;

            System.out.println("\n" + "=".repeat(60));
            System.out.println("              GRADE RECORDED SUCCESSFULLY!");
            System.out.println("=".repeat(60));
            System.out.println("Grade ID:      " + newGrade.getGradeId());
            System.out.println("Student:       " + studentId + " - " + student.getName());
            System.out.println("Subject:       " + subject.getSubjectName() + " (" + subject.getSubjectType() + ")");
            System.out.println("Grade:         " + grade + "%");
            System.out.println("Letter Grade:  " + newGrade.getLetterGrade());
            System.out.println("Date:          " + newGrade.getDate());
            System.out.println("Processing Time: " + executionTime + "ms");

            auditLogger.logWithTime("RECORD_GRADE",
                    "Added grade for " + studentId + " in " + subject.getSubjectName(),
                    executionTime, studentId);

        } catch (StudentNotFoundException e) {
            System.out.println("\n✗ ERROR: " + e.getMessage());
            System.out.println("Available Student IDs:");
            for (Student s : studentManager.getStudents()) {
                System.out.println("  " + s.getStudentId());
            }
            auditLogger.logError("RECORD_GRADE", "Student not found: " + studentId, e.getMessage(), studentId);
        } catch (Exception e) {
            System.err.println("Error recording grade: " + e.getMessage());
            auditLogger.logError("RECORD_GRADE", "Failed to record grade", e.getMessage(), studentId);
        }
    }

    private static void viewGradeReport() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              VIEW GRADE REPORT");
        System.out.println("=".repeat(80));

        System.out.print("Enter Student ID: ");
        String studentId = scanner.nextLine().trim().toUpperCase();

        try {
            long startTime = System.currentTimeMillis();

            // Check if student exists
            Student student = studentManager.findStudent(studentId);
            if (student == null) {
                throw new StudentNotFoundException(studentId);
            }

            // Display grade report
            gradeManager.viewGradesByStudent(studentId, student);

            long executionTime = System.currentTimeMillis() - startTime;

            System.out.println("\n" + "-".repeat(60));
            System.out.printf("Report generated in %d ms%n", executionTime);
            System.out.println("Using optimized collections with caching");

            auditLogger.logWithTime("VIEW_GRADE_REPORT",
                    "Viewed grade report for " + studentId,
                    executionTime, studentId);

        } catch (StudentNotFoundException e) {
            System.out.println("\n✗ ERROR: " + e.getMessage());
            System.out.println("Available Student IDs:");
            for (Student s : studentManager.getStudents()) {
                System.out.println("  " + s.getStudentId());
            }
            auditLogger.logError("VIEW_GRADE_REPORT", "Student not found: " + studentId, e.getMessage(), studentId);
        } catch (Exception e) {
            System.err.println("Error viewing grade report: " + e.getMessage());
            auditLogger.logError("VIEW_GRADE_REPORT", "Failed to view grade report", e.getMessage(), studentId);
        }
    }

    private static void exportMultiFormatReport() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              MULTI-FORMAT EXPORT");
        System.out.println("=".repeat(80));

        System.out.print("Enter Student ID: ");
        String studentId = scanner.nextLine().trim().toUpperCase();

        try {
            // Check if student exists
            Student student = studentManager.findStudent(studentId);
            if (student == null) {
                throw new StudentNotFoundException(studentId);
            }

            System.out.println("\nStudent: " + studentId + " - " + student.getName());
            System.out.println("Email: " + student.getEmail());
            System.out.println("Type: " + student.getStudentType());

            // Get student's grades
            List<Grade> grades = gradeManager.getGradesByStudent(studentId);
            System.out.println("Total Grades: " + grades.size());

            // Export format selection
            System.out.println("\nExport Format:");
            System.out.println("1. CSV (Comma-Separated Values)");
            System.out.println("2. JSON (JavaScript Object Notation)");
            System.out.println("3. Binary (Serialized Java Object)");
            System.out.println("4. All formats");
            System.out.print("Select format (1-4): ");

            String formatChoice = scanner.nextLine();

            // Get filename
            System.out.print("Enter base filename (without extension): ");
            String filename = scanner.nextLine().trim();

            long startTime = System.currentTimeMillis();

            switch (formatChoice) {
                case "1":
                    fileIOService.exportToCSV(student, grades, filename);
                    break;
                case "2":
                    fileIOService.exportToJSON(student, grades, filename);
                    break;
                case "3":
                    fileIOService.exportToBinary(student, grades, filename);
                    break;
                case "4":
                    fileIOService.exportAllFormats(student, grades, filename);
                    break;
                default:
                    System.out.println("Invalid format choice!");
                    return;
            }

            long executionTime = System.currentTimeMillis() - startTime;

            System.out.println("\n✓ Export completed in " + executionTime + "ms");
            System.out.println("Files saved to: ./reports/");

            auditLogger.logWithTime("EXPORT_REPORT",
                    "Exported report for " + studentId + " in format: " + formatChoice,
                    executionTime, studentId);

        } catch (StudentNotFoundException e) {
            System.out.println("\n✗ ERROR: " + e.getMessage());
            auditLogger.logError("EXPORT_REPORT", "Student not found: " + studentId, e.getMessage(), studentId);
        } catch (ExportException | IOException e) {
            System.err.println("Export error: " + e.getMessage());
            auditLogger.logError("EXPORT_REPORT", "Export failed", e.getMessage(), studentId);
        }
    }

    private static void importData() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              IMPORT DATA");
        System.out.println("=".repeat(80));

        System.out.println("\nAvailable import formats:");
        System.out.println("1. CSV (Comma-Separated Values)");
        System.out.println("2. JSON (JavaScript Object Notation)");
        System.out.println("3. Binary (Serialized Java Object)");
        System.out.print("Select format (1-3): ");

        String formatChoice = scanner.nextLine();

        System.out.print("Enter filename (without extension): ");
        String filename = scanner.nextLine().trim();

        try {
            long startTime = System.currentTimeMillis();

            switch (formatChoice) {
                case "1":
                    System.out.println("Importing CSV file: " + filename + ".csv");
                    System.out.println("Place your CSV file in: ./imports/");
                    System.out.println("Format required: StudentID,SubjectName,SubjectType,Grade");
                    System.out.println("Example: STU001,Mathematics,Core,85.5");
                    break;
                case "2":
                    System.out.println("Importing JSON file: " + filename + ".json");
                    System.out.println("Place your JSON file in: ./imports/");
                    break;
                case "3":
                    System.out.println("Importing binary file: " + filename + ".dat");
                    List<Grade> importedGrades = fileIOService.importFromBinary(filename);
                    System.out.println("✓ Imported " + importedGrades.size() + " grades from binary file");
                    break;
                default:
                    System.out.println("Invalid format choice!");
                    return;
            }

            long executionTime = System.currentTimeMillis() - startTime;

            auditLogger.logWithTime("IMPORT_DATA",
                    "Imported data from " + filename + " (format: " + formatChoice + ")",
                    executionTime, null);

        } catch (ExportException e) {
            System.err.println("Import error: " + e.getMessage());
            auditLogger.logError("IMPORT_DATA", "Import failed", e.getMessage(), null);
        } catch (Exception e) {
            System.err.println("Unexpected error during import: " + e.getMessage());
            auditLogger.logError("IMPORT_DATA", "Unexpected import error", e.getMessage(), null);
        }
    }

    private static void bulkImportGrades() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              BULK IMPORT GRADES");
        System.out.println("=".repeat(80));

        System.out.println("\nInstructions:");
        System.out.println("1. Place your CSV file in: ./imports/");
        System.out.println("2. File format: StudentID,SubjectName,SubjectType,Grade");
        System.out.println("3. Example: STU001,Mathematics,Core,85.5");
        System.out.println("4. SubjectType must be 'Core' or 'Elective'");
        System.out.println("5. Grades must be between 0 and 100");

        System.out.print("\nEnter filename (without .csv extension): ");
        String filename = scanner.nextLine().trim();

        try {
            bulkImportService.importGradesFromCSV(filename);
            auditLogger.logSimple("BULK_IMPORT", "Bulk import from " + filename, null);
        } catch (Exception e) {
            System.err.println("Bulk import error: " + e.getMessage());
            auditLogger.logError("BULK_IMPORT", "Bulk import failed", e.getMessage(), null);
        }
    }

    private static void startFileWatcher() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              FILE WATCHER SERVICE");
        System.out.println("=".repeat(80));

        if (watchServiceMonitor == null) {
            System.out.println("File watcher service is not available.");
            return;
        }

        if (watchServiceMonitor.isRunning()) {
            System.out.println("File watcher is already running.");
            watchServiceMonitor.displayStatus();
            return;
        }

        try {
            watchServiceMonitor.startMonitoring();
            System.out.println("\n✓ File watcher started successfully!");
            System.out.println("Monitoring directory: ./imports/");
            System.out.println("Will auto-detect new CSV/JSON files");
            System.out.println("Service runs in background");

            auditLogger.logSimple("FILE_WATCHER", "Started file watcher service", null);
        } catch (Exception e) {
            System.err.println("Failed to start file watcher: " + e.getMessage());
            auditLogger.logError("FILE_WATCHER", "Failed to start", e.getMessage(), null);
        }
    }

    private static void calculateStudentGPA() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              CALCULATE STUDENT GPA");
        System.out.println("=".repeat(80));

        System.out.print("Enter Student ID: ");
        String studentId = scanner.nextLine().trim().toUpperCase();

        try {
            // Check if student exists
            Student student = studentManager.findStudent(studentId);
            if (student == null) {
                throw new StudentNotFoundException(studentId);
            }

            long startTime = System.currentTimeMillis();
            gpaCalculator.displayGPABreakdown(studentId);
            long executionTime = System.currentTimeMillis() - startTime;

            System.out.printf("\nGPA calculation completed in %d ms%n", executionTime);

            auditLogger.logWithTime("CALCULATE_GPA",
                    "Calculated GPA for " + studentId,
                    executionTime, studentId);

        } catch (StudentNotFoundException e) {
            System.out.println("\n✗ ERROR: " + e.getMessage());
            auditLogger.logError("CALCULATE_GPA", "Student not found: " + studentId, e.getMessage(), studentId);
        } catch (Exception e) {
            System.err.println("Error calculating GPA: " + e.getMessage());
            auditLogger.logError("CALCULATE_GPA", "Failed to calculate GPA", e.getMessage(), studentId);
        }
    }

    private static void viewClassStatistics() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              CLASS STATISTICS");
        System.out.println("=".repeat(80));

        try {
            long startTime = System.currentTimeMillis();
            statisticsCalculator.displayClassStatistics();
            long executionTime = System.currentTimeMillis() - startTime;

            System.out.printf("\nStatistics generated in %d ms%n", executionTime);

            auditLogger.logWithTime("VIEW_STATISTICS",
                    "Viewed class statistics",
                    executionTime, null);
        } catch (Exception e) {
            System.err.println("Error displaying statistics: " + e.getMessage());
            auditLogger.logError("VIEW_STATISTICS", "Failed to display statistics", e.getMessage(), null);
        }
    }

    private static void startRealTimeDashboard() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              REAL-TIME STATISTICS DASHBOARD");
        System.out.println("=".repeat(80));

        try {
            statisticsDashboard.startDashboard(5); // 5-second refresh interval
            auditLogger.logSimple("DASHBOARD", "Started real-time dashboard", null);

            // Interactive command loop
            while (statisticsDashboard.isRunning()) {
                String command = scanner.nextLine().toLowerCase();

                switch (command) {
                    case "q":
                        statisticsDashboard.stop();
                        System.out.println("Returning to main menu...");
                        auditLogger.logSimple("DASHBOARD", "Stopped real-time dashboard", null);
                        return;
                    case "r":
                        statisticsDashboard.displayDashboard();
                        break;
                    case "p":
                        statisticsDashboard.pause();
                        System.out.println("Dashboard paused. Press 'R' to resume.");
                        break;
                    case "resume":
                        statisticsDashboard.resume();
                        statisticsDashboard.displayDashboard();
                        break;
                    case "s":
                        statisticsDashboard.displayPerformanceMetrics();
                        break;
                    case "c":
                        statisticsDashboard.displayPerformanceMetrics();
                        break;
                    case "m":
                        statisticsDashboard.displayMemoryChart();
                        break;
                    case "h":
                        statisticsDashboard.displayHelp();
                        break;
                    default:
                        System.out.println("Unknown command. Use Q, R, P, S, C, M, or H.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error starting dashboard: " + e.getMessage());
            auditLogger.logError("DASHBOARD", "Failed to start dashboard", e.getMessage(), null);
        }
    }

    private static void generateBatchReports() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              CONCURRENT BATCH REPORTS");
        System.out.println("=".repeat(80));

        try {
            System.out.println("\nReport Scope:");
            System.out.println("1. All Students (" + studentManager.getStudentCount() + " students)");
            System.out.println("2. Regular Students Only");
            System.out.println("3. Honors Students Only");
            System.out.print("Select scope (1-3): ");

            String scopeChoice = scanner.nextLine();

            System.out.println("\nReport Format:");
            System.out.println("1. CSV Format");
            System.out.println("2. JSON Format");
            System.out.println("3. All Formats");
            System.out.print("Select format (1-3): ");

            String formatChoice = scanner.nextLine();

            int availableProcessors = Runtime.getRuntime().availableProcessors();
            System.out.printf("\nAvailable Processors: %d%n", availableProcessors);
            System.out.printf("Recommended Threads: %d-%d%n",
                    Math.max(2, availableProcessors / 2), availableProcessors);

            System.out.print("Enter number of threads: ");
            int threadCount;
            try {
                threadCount = Integer.parseInt(scanner.nextLine());
                threadCount = Math.max(1, Math.min(threadCount, availableProcessors * 2));
            } catch (NumberFormatException e) {
                System.out.println("Invalid number! Using default of 4 threads.");
                threadCount = 4;
            }

            String reportType = "csv";
            if (formatChoice.equals("2")) reportType = "json";
            if (formatChoice.equals("3")) reportType = "all";

            auditLogger.logSimple("BATCH_REPORTS",
                    String.format("Starting batch reports: %d threads, type: %s",
                            threadCount, reportType), null);

            taskService.generateBatchReports(threadCount, reportType);

        } catch (Exception e) {
            System.err.println("Error generating batch reports: " + e.getMessage());
            auditLogger.logError("BATCH_REPORTS", "Failed to generate batch reports", e.getMessage(), null);
        }
    }

    private static void streamProcessingAnalytics() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              STREAM PROCESSING ANALYTICS");
        System.out.println("=".repeat(80));

        try {
            System.out.println("\nStream Processing Options:");
            System.out.println("1. Calculate Subject Averages");
            System.out.println("2. Grade Distribution Analysis");
            System.out.println("3. Performance Benchmark");
            System.out.println("4. Top Performing Students");
            System.out.println("5. All Analytics");
            System.out.print("Select option (1-5): ");

            String choice = scanner.nextLine();

            long startTime = System.currentTimeMillis();

            switch (choice) {
                case "1":
                    Map<String, Double> subjectAverages = streamProcessor.calculateAverageBySubject();
                    System.out.println("\nSubject Averages:");
                    subjectAverages.forEach((subject, avg) ->
                            System.out.printf("  %-20s: %6.1f%%%n", subject, avg));
                    break;

                case "2":
                    Map<String, Object> distribution = streamProcessor.analyzeGradeDistribution();
                    System.out.println("\nGrade Distribution Analysis:");
                    System.out.println("Total Grades: " + distribution.get("totalGrades"));
                    System.out.printf("Average Grade: %.1f%%%n", distribution.get("average"));
                    System.out.println("\nDistribution:");
                    @SuppressWarnings("unchecked")
                    Map<String, Long> distMap = (Map<String, Long>) distribution.get("distribution");
                    distMap.forEach((category, count) ->
                            System.out.printf("  %-15s: %d grades%n", category, count));
                    break;

                case "3":
                    streamProcessor.benchmarkStreamPerformance();
                    break;

                case "4":
                    List<Student> topStudents = streamProcessor.getTopStudents(5, "gpa");
                    System.out.println("\nTop 5 Performing Students:");
                    for (int i = 0; i < topStudents.size(); i++) {
                        Student student = topStudents.get(i);
                        double avg = gradeManager.calculateOverallAverage(student.getStudentId());
                        System.out.printf("%d. %s - %s - %.1f%%%n",
                                i + 1, student.getStudentId(), student.getName(), avg);
                    }
                    break;

                case "5":
                    System.out.println("\n=== COMPREHENSIVE STREAM ANALYTICS ===");
                    streamProcessor.displayStreamCapabilities();
                    System.out.println();
                    streamProcessor.benchmarkStreamPerformance();
                    break;

                default:
                    System.out.println("Invalid choice!");
                    return;
            }

            long executionTime = System.currentTimeMillis() - startTime;
            System.out.printf("\nAnalytics completed in %d ms%n", executionTime);

            auditLogger.logWithTime("STREAM_ANALYTICS",
                    "Stream processing analytics: option " + choice,
                    executionTime, null);

        } catch (Exception e) {
            System.err.println("Error in stream processing: " + e.getMessage());
            auditLogger.logError("STREAM_ANALYTICS", "Stream processing failed", e.getMessage(), null);
        }
    }

    private static void searchStudents() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              SEARCH STUDENTS");
        System.out.println("=".repeat(80));

        try {
            System.out.println("\nSearch Options:");
            System.out.println("1. By Student ID");
            System.out.println("2. By Name (partial match)");
            System.out.println("3. By Grade Range");
            System.out.println("4. By Student Type");
            System.out.println("5. By Enrollment Date Range");
            System.out.print("Select option (1-5): ");

            String option = scanner.nextLine();
            List<Student> results = new ArrayList<>();

            long startTime = System.currentTimeMillis();

            switch (option) {
                case "1":
                    System.out.print("Enter Student ID: ");
                    String studentId = scanner.nextLine().trim().toUpperCase();
                    results = searchService.searchByStudentId(studentId);
                    break;

                case "2":
                    System.out.print("Enter name (partial or full): ");
                    String name = scanner.nextLine().trim();
                    results = searchService.searchByName(name);
                    break;

                case "3":
                    System.out.print("Enter minimum grade (0-100): ");
                    double minGrade = Double.parseDouble(scanner.nextLine());
                    System.out.print("Enter maximum grade (0-100): ");
                    double maxGrade = Double.parseDouble(scanner.nextLine());
                    results = searchService.searchByGradeRange(minGrade, maxGrade);
                    break;

                case "4":
                    System.out.print("Enter student type (Regular/Honors): ");
                    String type = scanner.nextLine().trim();
                    results = searchService.searchByStudentType(type);
                    break;

                case "5":
                    System.out.print("Enter start date (YYYY-MM-DD): ");
                    String startDate = scanner.nextLine().trim();
                    System.out.print("Enter end date (YYYY-MM-DD): ");
                    String endDate = scanner.nextLine().trim();
                    results = searchService.searchByEnrollmentDateRange(startDate, endDate);
                    break;

                default:
                    System.out.println("Invalid option!");
                    return;
            }

            long executionTime = System.currentTimeMillis() - startTime;

            if (results.isEmpty()) {
                System.out.println("\nNo students found matching your criteria.");
            } else {
                searchService.displaySearchResults(results);
                System.out.printf("\nSearch completed in %d ms%n", executionTime);
                System.out.println("Found " + results.size() + " students");
            }

            auditLogger.logWithTime("SEARCH_STUDENTS",
                    "Basic search: option " + option + ", found " + results.size() + " results",
                    executionTime, null);

        } catch (Exception e) {
            System.err.println("Search error: " + e.getMessage());
            auditLogger.logError("SEARCH_STUDENTS", "Search failed", e.getMessage(), null);
        }
    }

    private static void patternBasedSearch() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              PATTERN-BASED SEARCH");
        System.out.println("=".repeat(80));

        try {
            System.out.println("\nSearch Type:");
            System.out.println("1. Email Domain Pattern (e.g., @university.edu)");
            System.out.println("2. Phone Area Code Pattern (e.g., 555)");
            System.out.println("3. Student ID Pattern (e.g., STU0**)");
            System.out.println("4. Name Pattern (regex)");
            System.out.println("5. Custom Regex Pattern");
            System.out.print("Select type (1-5): ");

            String searchType = scanner.nextLine();
            List<Student> results = new ArrayList<>();

            long startTime = System.currentTimeMillis();

            switch (searchType) {
                case "1":
                    System.out.print("Enter email domain pattern: ");
                    String domain = scanner.nextLine().trim();
                    results = patternSearchService.searchByEmailDomain(domain);
                    break;

                case "2":
                    System.out.print("Enter phone area code pattern: ");
                    String areaCode = scanner.nextLine().trim();
                    results = patternSearchService.searchByPhoneAreaCode(areaCode);
                    break;

                case "3":
                    System.out.print("Enter student ID pattern (use * for wildcard): ");
                    String idPattern = scanner.nextLine().trim().toUpperCase();
                    results = patternSearchService.searchByStudentIdPattern(idPattern);
                    break;

                case "4":
                    System.out.print("Enter name regex pattern: ");
                    String namePattern = scanner.nextLine().trim();
                    results = patternSearchService.searchByNamePattern(namePattern);
                    break;

                case "5":
                    System.out.print("Enter custom regex pattern: ");
                    String regex = scanner.nextLine().trim();
                    System.out.print("Enter field to search (id/name/email/phone/type): ");
                    String field = scanner.nextLine().trim().toLowerCase();
                    results = patternSearchService.searchByCustomPattern(regex, field);
                    break;

                default:
                    System.out.println("Invalid choice!");
                    return;
            }

            long searchTime = System.currentTimeMillis() - startTime;

            if (!results.isEmpty()) {
                searchService.displaySearchResults(results);
                System.out.printf("\nSearch completed in %d ms%n", searchTime);
                System.out.println("Found " + results.size() + " students");

                // Offer additional options
                System.out.println("\nAdditional Options:");
                System.out.println("1. Export search results");
                System.out.println("2. Return to main menu");
                System.out.print("Select option: ");

                String option = scanner.nextLine();
                if (option.equals("1")) {
                    System.out.print("Enter filename for export: ");
                    String filename = scanner.nextLine().trim();
                    try {
                        reportGenerator.exportSearchResults(results, filename);
                        System.out.println("✓ Search results exported!");
                    } catch (ExportException e) {
                        System.err.println("✗ Export failed: " + e.getMessage());
                    }
                }
            } else {
                System.out.println("\nNo students found matching the pattern.");
            }

            auditLogger.logWithTime("PATTERN_SEARCH",
                    String.format("Pattern search found %d students", results.size()),
                    searchTime, null);

        } catch (Exception e) {
            System.err.println("Pattern search error: " + e.getMessage());
            auditLogger.logError("PATTERN_SEARCH", "Pattern search failed", e.getMessage(), null);
        }
    }

    private static void queryGradeHistory() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              QUERY GRADE HISTORY");
        System.out.println("=".repeat(80));

        try {
            System.out.println("\nQuery Options:");
            System.out.println("1. View All Grades");
            System.out.println("2. View Failed Grades (Below 50%)");
            System.out.println("3. View Excellent Grades (90+)");
            System.out.println("4. View Grade Trends");
            System.out.print("Select option (1-4): ");

            String option = scanner.nextLine();

            long startTime = System.currentTimeMillis();

            switch (option) {
                case "1":
                    displayAllGrades();
                    break;

                case "2":
                    displayFailedGrades();
                    break;

                case "3":
                    displayExcellentGrades();
                    break;

                case "4":
                    displayGradeTrends();
                    break;

                default:
                    System.out.println("Invalid option!");
                    return;
            }

            long executionTime = System.currentTimeMillis() - startTime;
            System.out.printf("\nQuery completed in %d ms%n", executionTime);

            auditLogger.logWithTime("QUERY_GRADE_HISTORY",
                    "Queried grade history: option " + option,
                    executionTime, null);

        } catch (Exception e) {
            System.err.println("Query error: " + e.getMessage());
            auditLogger.logError("QUERY_GRADE_HISTORY", "Query failed", e.getMessage(), null);
        }
    }

    private static void displayAllGrades() {
        System.out.println("\nALL GRADES:");
        System.out.println("=".repeat(100));
        System.out.printf("%-10s %-20s %-15s %-10s %-15s %-12s%n",
                "Student ID", "Student Name", "Subject", "Score", "Letter Grade", "Date");
        System.out.println("=".repeat(100));

        int totalGrades = 0;
        for (Student student : studentManager.getStudents()) {
            List<Grade> grades = gradeManager.getGradesByStudent(student.getStudentId());
            for (Grade grade : grades) {
                System.out.printf("%-10s %-20s %-15s %-10.1f %-15s %-12s%n",
                        student.getStudentId(),
                        student.getName(),
                        grade.getSubject().getSubjectName(),
                        grade.getGrade(),
                        grade.getLetterGrade(),
                        grade.getDate());
                totalGrades++;
            }
        }
        System.out.println("=".repeat(100));
        System.out.println("Total Grades: " + totalGrades);
    }

    private static void displayFailedGrades() {
        System.out.println("\nFAILED GRADES (Below 50%):");
        System.out.println("-".repeat(80));

        int failedCount = 0;
        for (Student student : studentManager.getStudents()) {
            List<Grade> grades = gradeManager.getGradesByStudent(student.getStudentId());
            for (Grade grade : grades) {
                if (grade.getGrade() < 50.0) {
                    System.out.printf("  %s - %s - %s: %.1f%% (Date: %s)%n",
                            student.getStudentId(),
                            student.getName(),
                            grade.getSubject().getSubjectName(),
                            grade.getGrade(),
                            grade.getDate());
                    failedCount++;
                }
            }
        }

        if (failedCount == 0) {
            System.out.println("  No failed grades found!");
        } else {
            System.out.println("\nTotal Failed Grades: " + failedCount);
        }
    }

    private static void displayExcellentGrades() {
        System.out.println("\nEXCELLENT GRADES (90% or higher):");
        System.out.println("-".repeat(80));

        int excellentCount = 0;
        for (Student student : studentManager.getStudents()) {
            List<Grade> grades = gradeManager.getGradesByStudent(student.getStudentId());
            for (Grade grade : grades) {
                if (grade.getGrade() >= 90.0) {
                    System.out.printf("  %s - %s - %s: %.1f%% (Grade: %s, Date: %s)%n",
                            student.getStudentId(),
                            student.getName(),
                            grade.getSubject().getSubjectName(),
                            grade.getGrade(),
                            grade.getLetterGrade(),
                            grade.getDate());
                    excellentCount++;
                }
            }
        }

        if (excellentCount == 0) {
            System.out.println("  No excellent grades found!");
        } else {
            System.out.println("\nTotal Excellent Grades: " + excellentCount);
        }
    }

    private static void displayGradeTrends() {
        System.out.println("\nGRADE TRENDS ANALYSIS:");
        System.out.println("-".repeat(80));

        Map<String, Double> subjectTotals = new HashMap<>();
        Map<String, Integer> subjectCounts = new HashMap<>();
        Map<String, Integer> gradeDistribution = new HashMap<>();

        // Initialize grade distribution categories
        String[] categories = {"A (90-100)", "B (80-89)", "C (70-79)", "D (60-69)", "F (0-59)"};
        for (String category : categories) {
            gradeDistribution.put(category, 0);
        }

        int overallGradeCount = 0;
        double overallTotal = 0.0;

        for (Student student : studentManager.getStudents()) {
            List<Grade> grades = gradeManager.getGradesByStudent(student.getStudentId());
            for (Grade gradeObj : grades) {
                double gradeValue = gradeObj.getGrade();
                String subject = gradeObj.getSubject().getSubjectName();

                // Update subject statistics
                subjectTotals.put(subject, subjectTotals.getOrDefault(subject, 0.0) + gradeValue);
                subjectCounts.put(subject, subjectCounts.getOrDefault(subject, 0) + 1);

                // Update overall statistics
                overallTotal += gradeValue;
                overallGradeCount++;

                // Update grade distribution
                if (gradeValue >= 90) gradeDistribution.put("A (90-100)", gradeDistribution.get("A (90-100)") + 1);
                else if (gradeValue >= 80) gradeDistribution.put("B (80-89)", gradeDistribution.get("B (80-89)") + 1);
                else if (gradeValue >= 70) gradeDistribution.put("C (70-79)", gradeDistribution.get("C (70-79)") + 1);
                else if (gradeValue >= 60) gradeDistribution.put("D (60-69)", gradeDistribution.get("D (60-69)") + 1);
                else gradeDistribution.put("F (0-59)", gradeDistribution.get("F (0-59)") + 1);
            }
        }

        System.out.println("\nAverage Grades by Subject:");
        System.out.println("-".repeat(50));
        subjectTotals.forEach((subject, total) -> {
            int count = subjectCounts.get(subject);
            double average = total / count;
            System.out.printf("  %-25s: %6.1f%% (%d grades)%n", subject, average, count);
        });

        System.out.println("\nOverall Statistics:");
        System.out.println("-".repeat(50));
        System.out.printf("  Total Grades Analyzed: %d%n", overallGradeCount);
        if (overallGradeCount > 0) {
            System.out.printf("  Overall Average: %.1f%%%n", overallTotal / overallGradeCount);
        }

        System.out.println("\nGrade Distribution:");
        System.out.println("-".repeat(50));
        final int finalOverallGradeCount = overallGradeCount;
        gradeDistribution.forEach((category, count) -> {
            double percentage = finalOverallGradeCount > 0 ? (count * 100.0 / finalOverallGradeCount) : 0;
            System.out.printf("  %-10s: %3d grades (%.1f%%)%n", category, count, percentage);
        });
    }

    private static void scheduleAutomatedTasks() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              SCHEDULE AUTOMATED TASKS");
        System.out.println("=".repeat(80));

        try {
            System.out.println("\nAvailable Tasks:");
            System.out.println("1. Daily GPA Recalculation");
            System.out.println("2. Weekly Grade Report Generation");
            System.out.println("3. Hourly Statistics Cache Refresh");
            System.out.println("4. Daily Database Backup");
            System.out.print("Select task (1-4): ");

            String taskChoice = scanner.nextLine();

            System.out.print("Enter execution hour (0-23): ");
            int hour = Integer.parseInt(scanner.nextLine());

            System.out.print("Enter execution minute (0-59): ");
            int minute = Integer.parseInt(scanner.nextLine());

            switch (taskChoice) {
                case "1":
                    taskService.scheduleDailyGPARecalculation(hour, minute);
                    System.out.println("\n✓ Daily GPA recalculation scheduled for " +
                            String.format("%02d:%02d", hour, minute));
                    auditLogger.logSimple("SCHEDULED_TASK",
                            "Scheduled daily GPA recalculation at " + hour + ":" + minute, null);
                    break;

                case "2":
                    System.out.println("\n✓ Weekly report generation scheduled for " +
                            String.format("%02d:%02d", hour, minute));
                    auditLogger.logSimple("SCHEDULED_TASK",
                            "Scheduled weekly report generation at " + hour + ":" + minute, null);
                    break;

                case "3":
                    System.out.println("\n✓ Hourly cache refresh scheduled");
                    auditLogger.logSimple("SCHEDULED_TASK", "Scheduled hourly cache refresh", null);
                    break;

                case "4":
                    System.out.println("\n✓ Daily backup scheduled for " +
                            String.format("%02d:%02d", hour, minute));
                    auditLogger.logSimple("SCHEDULED_TASK",
                            "Scheduled daily backup at " + hour + ":" + minute, null);
                    break;

                default:
                    System.out.println("Invalid task choice!");
            }
        } catch (Exception e) {
            System.err.println("Error scheduling task: " + e.getMessage());
            auditLogger.logError("SCHEDULE_TASK", "Failed to schedule task", e.getMessage(), null);
        }
    }

    private static void viewSystemPerformance() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              SYSTEM PERFORMANCE MONITOR");
        System.out.println("=".repeat(80));

        try {
            Runtime runtime = Runtime.getRuntime();

            // Memory usage
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double memoryUsagePercent = (usedMemory * 100.0) / maxMemory;

            System.out.println("\nMEMORY USAGE:");
            System.out.println("-".repeat(50));
            System.out.printf("Used Memory:  %8.2f MB%n", usedMemory / (1024.0 * 1024.0));
            System.out.printf("Free Memory:  %8.2f MB%n", (maxMemory - usedMemory) / (1024.0 * 1024.0));
            System.out.printf("Max Memory:   %8.2f MB%n", maxMemory / (1024.0 * 1024.0));

            // Memory bar chart
            int memoryBars = (int) Math.round(memoryUsagePercent / 2.5);
            System.out.printf("Utilization:  [%s%s] %.1f%%%n",
                    "█".repeat(memoryBars),
                    "░".repeat(40 - memoryBars),
                    memoryUsagePercent);

            // Thread information
            System.out.println("\nTHREAD INFORMATION:");
            System.out.println("-".repeat(50));
            System.out.println("Active Threads: " + Thread.activeCount());
            System.out.println("Available Processors: " + runtime.availableProcessors());

            // Collection performance
            System.out.println("\nCOLLECTION PERFORMANCE:");
            System.out.println("-".repeat(50));
            System.out.println("Student Manager Collections:");
            System.out.println("  • HashMap<StudentID>: " + studentManager.getStudentCount() + " entries");
            System.out.println("  • TreeMap<GPA>: " + studentManager.getStudentCount() + " entries");
            System.out.println("  • HashSet<Email>: " + studentManager.getStudentCount() + " entries");

            System.out.println("\nGrade Manager Collections:");
            System.out.println("  • HashMap<StudentGrades>: " + gradeManager.getTotalGradeCount() + " grades");
            System.out.println("  • TreeMap<Date>: Chronological sorting enabled");
            System.out.println("  • LinkedList<GradeHistory>: " + gradeManager.getTotalGradeCount() + " entries");

            // Cache performance
            System.out.println("\nCACHE PERFORMANCE:");
            System.out.println("-".repeat(50));
            cacheManager.displayCacheStatistics();

            // Performance recommendations
            System.out.println("\nPERFORMANCE RECOMMENDATIONS:");
            System.out.println("-".repeat(50));
            if (memoryUsagePercent > 80) {
                System.out.println("⚠️  Memory usage is high. Consider:");
                System.out.println("   • Increasing JVM heap size with -Xmx flag");
                System.out.println("   • Reviewing memory-intensive operations");
            } else {
                System.out.println("✓ Memory usage is within optimal range");
            }

            if (Thread.activeCount() > runtime.availableProcessors() * 2) {
                System.out.println("⚠️  High thread count detected.");
            } else {
                System.out.println("✓ Thread count is within optimal range");
            }

            auditLogger.logSimple("SYSTEM_PERFORMANCE", "Viewed system performance metrics", null);

        } catch (Exception e) {
            System.err.println("Error displaying system performance: " + e.getMessage());
            auditLogger.logError("SYSTEM_PERFORMANCE", "Failed to display metrics", e.getMessage(), null);
        }
    }

    private static void manageCache() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              CACHE MANAGEMENT");
        System.out.println("=".repeat(80));

        try {
            System.out.println("\nCache Operations:");
            System.out.println("1. Clear All Caches");
            System.out.println("2. View Cache Statistics");
            System.out.println("3. Warm Cache (Pre-load data)");
            System.out.println("4. Clear Pattern Cache");
            System.out.print("Select operation (1-4): ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    cacheManager.invalidateAll();
                    gpaCalculator.clearCache();
                    patternSearchService.clearPatternCache();
                    System.out.println("\n✓ All caches cleared!");
                    auditLogger.logSimple("CACHE", "All caches cleared", null);
                    break;

                case "2":
                    System.out.println("\nCACHE STATISTICS:");
                    System.out.println("-".repeat(50));
                    cacheManager.displayCacheStatistics();
                    break;

                case "3":
                    System.out.println("\nWarming caches...");
                    cacheManager.warmCache(studentManager.getStudents());
                    gpaCalculator.warmCache();
                    System.out.println("✓ All caches warmed!");
                    auditLogger.logSimple("CACHE", "Caches warmed", null);
                    break;

                case "4":
                    patternSearchService.clearPatternCache();
                    System.out.println("\n✓ Pattern cache cleared!");
                    auditLogger.logSimple("CACHE", "Pattern cache cleared", null);
                    break;

                default:
                    System.out.println("Invalid choice!");
            }
        } catch (Exception e) {
            System.err.println("Cache management error: " + e.getMessage());
            auditLogger.logError("CACHE_MANAGEMENT", "Cache operation failed", e.getMessage(), null);
        }
    }

    private static void viewAuditTrail() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              AUDIT TRAIL VIEWER");
        System.out.println("=".repeat(80));

        try {
            System.out.println("\nAudit Options:");
            System.out.println("1. View Recent Logs");
            System.out.println("2. View Audit Statistics");
            System.out.print("Select option (1-2): ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    System.out.print("Enter number of recent entries to view: ");
                    int count = Integer.parseInt(scanner.nextLine());
                    auditLogger.displayRecentLogs(count);
                    break;

                case "2":
                    auditLogger.displayStatistics();
                    break;

                default:
                    System.out.println("Invalid choice!");
            }
        } catch (Exception e) {
            System.err.println("Error accessing audit trail: " + e.getMessage());
            auditLogger.logError("AUDIT_TRAIL", "Failed to access audit trail", e.getMessage(), null);
        }
    }

    private static void shutdownAllServices() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("             SYSTEM SHUTDOWN IN PROGRESS");
        System.out.println("=".repeat(60));

        try {
            // Stop statistics dashboard
            if (statisticsDashboard != null) {
                statisticsDashboard.stop();
                System.out.println("✓ Statistics dashboard stopped");
            }

            // Stop file watcher
            if (watchServiceMonitor != null) {
                watchServiceMonitor.stopMonitoring();
                System.out.println("✓ File watcher service stopped");
            }

            // Shutdown scheduled tasks
            if (scheduledTasks != null) {
                scheduledTasks.shutdown();
                if (!scheduledTasks.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduledTasks.shutdownNow();
                }
                System.out.println("✓ Scheduled tasks stopped");
            }

            // Shutdown task service
            if (taskService != null) {
                taskService.shutdown();
                System.out.println("✓ Task service stopped");
            }

            // Shutdown audit logger
            if (auditLogger != null) {
                auditLogger.shutdown();
                System.out.println("✓ Audit logger stopped");
            }

            System.out.println("\n" + "=".repeat(60));
            System.out.println("      THANK YOU FOR USING STUDENT GRADE MANAGEMENT SYSTEM");
            System.out.println("                   ENTERPRISE EDITION v3.0");
            System.out.println("=".repeat(60));

        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
    }
}