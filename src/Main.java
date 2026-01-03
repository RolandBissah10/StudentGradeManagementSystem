import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
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
    private static GradeManager gradeManager = new GradeManager(studentManager);

    // Services
    private static ReportGenerator reportGenerator = new ReportGenerator(studentManager, gradeManager);
    private static BulkImportService bulkImportService = new BulkImportService(studentManager, gradeManager);
    private static FileIOService fileIOService = new FileIOService();
    private static ConcurrentTaskService taskService = new ConcurrentTaskService(studentManager, gradeManager, fileIOService);
    private static CacheManager cacheManager = new CacheManager();
    private static AuditLogger auditLogger = AuditLogger.getInstance();
    private static WatchServiceMonitor watchServiceMonitor;
    private static GPACalculator gpaCalculator = new GPACalculator(studentManager, gradeManager);
    private static StatisticsDashboard statisticsDashboard = new StatisticsDashboard(studentManager, gradeManager, CacheManager.getInstance());
    private static PatternSearchService patternSearchService = new PatternSearchService(studentManager, gradeManager);
    private static SearchService searchService = new SearchService(studentManager, gradeManager);
    private static StatisticsCalculator statisticsCalculator = new StatisticsCalculator(studentManager, gradeManager);
    private static StreamProcessor streamProcessor = new StreamProcessor(studentManager, gradeManager);
    private static ScheduledExecutorService scheduledTasks = Executors.newScheduledThreadPool(4);

    private static Scanner scanner = new Scanner(System.in);

    static {
        try {
            watchServiceMonitor = new WatchServiceMonitor("imports");
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
//        System.out.println("               (Enterprise Edition with Concurrency)");
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
        String[] firstNames = {
                "Alex", "Jamie", "Taylor", "Morgan", "Jordan", "Casey", "Riley", "Avery",
                "Cameron", "Dakota", "Skyler", "Peyton", "Quinn", "Rowan", "Sage", "Finley",
                "Charlie", "Emerson", "River", "Phoenix", "Kai", "Drew", "Blake", "Hayden",
                "Addison", "Aiden", "Brooklyn", "Carter", "Evelyn", "Gabriel", "Hannah",
                "Isaac", "Isabella", "Jack", "Lily", "Liam", "Mia", "Noah", "Olivia",
                "Sophia", "William", "Ethan", "Ava", "James", "Charlotte", "Benjamin",
                "Amelia", "Lucas", "Harper", "Henry", "Ella", "Alexander", "Grace",
                "Michael", "Chloe", "Daniel", "Victoria", "Matthew", "Zoe", "Samuel",
                "Natalie", "David", "Layla", "Joseph", "Scarlett", "Jackson", "Riley",
                "John", "Hazel", "Luke", "Penelope", "Andrew", "Stella", "Ryan", "Nora",
                "Nathan", "Lillian", "Caleb", "Aurora", "Christian", "Eleanor", "Levi",
                "Ellie", "Julian", "Claire", "Christopher", "Violet", "Joshua", "Savannah"
        };

        String[] lastNames = {
                "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
                "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson",
                "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson",
                "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker",
                "Young", "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores",
                "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell",
                "Carter", "Roberts", "Gomez", "Phillips", "Evans", "Turner", "Diaz", "Parker",
                "Cruz", "Edwards", "Collins", "Reyes", "Stewart", "Morris", "Morales", "Murphy",
                "Cook", "Rogers", "Gutierrez", "Ortiz", "Morgan", "Cooper", "Peterson", "Bailey",
                "Reed", "Kelly", "Howard", "Ramos", "Kim", "Cox", "Ward", "Richardson", "Watson",
                "Brooks", "Chavez", "Wood", "James", "Bennett", "Gray", "Mendoza", "Ruiz", "Hughes"
        };

        String[] domains = {
                "university.edu", "college.org", "school.net", "institute.edu", "academy.org",
                "campus.edu", "learning.net", "studies.org", "knowledge.edu"
        };

        String[] enrollmentDates = {
                "2024-09-01", "2024-01-15", "2024-08-20", "2024-02-01", "2024-07-10"
        };

        java.util.Random random = new java.util.Random();
        int regularCount = 0;
        int honorsCount = 0;

        for (int i = 0; i < 50; i++) {
            // Generate random student data
            String firstName = firstNames[random.nextInt(firstNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];
            String fullName = firstName + " " + lastName;
            int age = 15 + random.nextInt(4); // Ages 15-18

            // Generate email
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase()
                    + "@" + domains[random.nextInt(domains.length)];

            // Generate phone number (format: +1-555-XXXX where XXXX starts from 3001)
            String phone = String.format("+1-555-%04d", 3001 + i);

            // Random enrollment date
            String enrollmentDate = enrollmentDates[random.nextInt(enrollmentDates.length)];

            // Randomly decide if student is Regular (60%) or Honors (40%)
            if (random.nextDouble() < 0.4) {
                // Create Honors student
                studentManager.addStudent(new HonorsStudent(
                        fullName, age, email, phone, enrollmentDate
                ));
                honorsCount++;
            } else {
                // Create Regular student
                studentManager.addStudent(new RegularStudent(
                        fullName, age, email, phone, enrollmentDate
                ));
                regularCount++;
            }
        }

        System.out.println("✓ Successfully added 100 sample students:");
        System.out.println("  - Regular Students: " + regularCount);
        System.out.println("  - Honors Students: " + honorsCount);
        System.out.println("  - Total: " + studentManager.getStudentCount());
    }

    private static void addSampleGrades() {
        // Subjects
        List<Subject> subjects = new ArrayList<>();
        subjects.add(new CoreSubject("Mathematics", "MAT101"));
        subjects.add(new CoreSubject("English", "ENG101"));
        subjects.add(new CoreSubject("Science", "SCI101"));
        subjects.add(new ElectiveSubject("Art", "ART101"));
        subjects.add(new ElectiveSubject("Music", "MUS101"));

        List<Student> students = (List<Student>) studentManager.getStudents();

        if (students.isEmpty()) {
            System.out.println("⚠ No students found.");
            return;
        }

        Random random = new Random();
        int totalGrades = 0;
        int failingCount = 0;

        System.out.println("Generating grades for " + students.size() + " students (40% will fail)...");

        for (Student student : students) {
            String studentId = student.getStudentId();
            if (studentId == null || studentId.isEmpty()) continue;

            // Each student has 40% chance of failing
            boolean isFailing = random.nextDouble() < 0.40;

            // Add 3-5 grades
            int gradeCount = 3 + random.nextInt(3);

            for (int j = 0; j < gradeCount; j++) {
                Subject subject = subjects.get(random.nextInt(subjects.size()));
                double grade = isFailing ?
                        40 + random.nextInt(45) :  // 40-84 for failing
                        65 + random.nextInt(36);   // 65-100 for passing

                gradeManager.addGrade(new Grade(studentId, subject, grade));
                totalGrades++;
            }

            double avgGrade = gradeManager.calculateOverallAverage(student.getStudentId());
            boolean failing = avgGrade < student.getPassingGrade();
            if (failing) failingCount++;
        }

        System.out.println("✓ Added " + totalGrades + " grades");
        System.out.println("  - Passing: " + (students.size() - failingCount) +
                " (" + String.format("%.1f%%", (students.size() - failingCount) * 100.0 / students.size()) + ")");
        System.out.println("  - Failing: " + failingCount +
                " (" + String.format("%.1f%%", failingCount * 100.0 / students.size()) + ")");
    }

    private static void recalculateAllGPAs() {
        List<Student> students = studentManager.getStudents();
        System.out.println("Recalculating GPAs for " + students.size() + " students...");

        // Use parallel stream for concurrent processing
        students.parallelStream().forEach(student -> {
            gpaCalculator.calculateGPA(student.getStudentId(), false);
            // Update student GPA in database (simulated)
        });

        System.out.println("✓ GPA recalculation completed");
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
        System.out.println("  1. Add Student");
        System.out.println("  2. View All Students");
        System.out.println("  3. Record Grade");
        System.out.println("  4. View Grade Report");

        System.out.println("\nFILE OPERATIONS (NIO.2)");
        System.out.println("  5. Export Grade Report");
        System.out.println("  6. Import Data");
        System.out.println("  7. Bulk Import Grades");
        System.out.println("  8. Start File Watcher Service");

        System.out.println("\nANALYTICS & REPORTING");
        System.out.println("  9. Calculate Student GPA");
        System.out.println(" 10. View Class Statistics");
        System.out.println(" 11. Real-Time Statistics Dashboard");
        System.out.println(" 12. Generate Batch Reports");
        System.out.println(" 13. Stream Processing Analytics");

        System.out.println("\nSEARCH & QUERY");
        System.out.println(" 14. Search Students");
        System.out.println(" 15. Pattern-Based Search");
        System.out.println(" 16. Query Grade History");

        System.out.println("\nADVANCED FEATURES");
        System.out.println(" 17. Schedule Automated Tasks");
        System.out.println(" 18. View System Performance");
        System.out.println(" 19. Cache Management");
        System.out.println(" 20. Audit Trail Viewer");

        System.out.print("\nEnter choice (1-20) or 21 to Exit: ");
    }

    private static void displayBackgroundTasks() {
        List<String> activeTasks = new ArrayList<>();

        if (watchServiceMonitor != null && watchServiceMonitor.isRunning()) {
            activeTasks.add("File Watcher");
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
                case "17": scheduleAutomatedTasks(studentManager, taskService); break;
                case "18": viewSystemPerformance(); break;
                case "19": manageCache(); break;
                case "20": viewAuditTrail(); break;
                case "21":
                    System.out.println("\nShutting down system...");
                    return false;
                default:
                    System.out.println("Invalid choice! Please enter 1-20 or 21 to exit.");
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

            // Display student info as shown in image
            System.out.println("\nStudent: " + studentId + " - " + student.getName() +
                    " (" + student.getEmail() + ")");
            System.out.println("Type: " + student.getStudentType() + " | " +
                    "Phone: " + (student.getPhone() != null ? student.getPhone() : "N/A"));

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

            if (!formatChoice.matches("[1-4]")) {
                System.out.println("Invalid format selection!");
                return;
            }

            // Report Type selection as shown in image
            System.out.println("\nReport Type:");
            System.out.println("1. Summary Report");
            System.out.println("2. Detailed Report");
            System.out.println("3. Transcript Format");
            System.out.println("4. Performance Analytics");
            System.out.print("Select type (1-4): ");

            String reportTypeChoice = scanner.nextLine();

            String reportType;
            switch (reportTypeChoice) {
                case "1": reportType = "summary"; break;
                case "2": reportType = "detailed"; break;
                case "3": reportType = "transcript"; break;
                case "4": reportType = "analytics"; break;
                default: reportType = "detailed";
            }

            System.out.println("\nProcessing with NIO.2 Streaming...\n");

            long totalStartTime = System.currentTimeMillis();
            long csvTime = 0, jsonTime = 0, binaryTime = 0;
            long csvSize = 0, jsonSize = 0, binarySize = 0;
            String csvFile = "", jsonFile = "", binaryFile = "";

            // Generate base filename from student name
            String baseFilename = student.getName().toLowerCase().replace(" ", "_") + "_" + reportType;

            switch (formatChoice) {
                case "1": // CSV only
                    long csvStart = System.currentTimeMillis();
                    csvFile = fileIOService.exportToCSV(student, grades, baseFilename, reportType);
                    csvTime = System.currentTimeMillis() - csvStart;
                    csvSize = getFileSize(csvFile);

                    System.out.println("/ CSV Export completed");
                    System.out.println("File: " + new File(csvFile).getName());
                    System.out.println("Location: " + new File(csvFile).getParent());
                    System.out.println("Size: " + formatFileSize(csvSize));
                    System.out.println("Rows: " + grades.size() + " grades + header");
                    System.out.println("Time: " + csvTime + "ms");
                    break;

                case "2": // JSON only
                    long jsonStart = System.currentTimeMillis();
                    jsonFile = fileIOService.exportToJSON(student, grades, baseFilename, reportType);
                    jsonTime = System.currentTimeMillis() - jsonStart;
                    jsonSize = getFileSize(jsonFile);

                    System.out.println("/ JSON Export completed");
                    System.out.println("File: " + new File(jsonFile).getName());
                    System.out.println("Location: " + new File(jsonFile).getParent());
                    System.out.println("Size: " + formatFileSize(jsonSize));
                    System.out.println("Structure: Nested objects with metadata");
                    System.out.println("Time: " + jsonTime + "ms");
                    break;

                case "3": // Binary only
                    long binaryStart = System.currentTimeMillis();
                    binaryFile = fileIOService.exportToBinary(student, grades, baseFilename, reportType);
                    binaryTime = System.currentTimeMillis() - binaryStart;
                    binarySize = getFileSize(binaryFile);

                    System.out.println("/ Binary Export completed");
                    System.out.println("File: " + new File(binaryFile).getName());
                    System.out.println("Location: " + new File(binaryFile).getParent());
                    System.out.println("Size: " + formatFileSize(binarySize) + " (compressed)");
                    System.out.println("Format: Serialized StudentReport object");
                    System.out.println("Time: " + binaryTime + "ms");
                    break;

                case "4": // All formats
                    // Export CSV
                    long csvStartAll = System.currentTimeMillis();
                    csvFile = fileIOService.exportToCSV(student, grades, baseFilename, reportType);
                    csvTime = System.currentTimeMillis() - csvStartAll;
                    csvSize = getFileSize(csvFile);

                    System.out.println("/ CSV Export completed");
                    System.out.println("File: " + new File(csvFile).getName());
                    System.out.println("Location: " + new File(csvFile).getParent());
                    System.out.println("Size: " + formatFileSize(csvSize));
                    System.out.println("Rows: " + grades.size() + " grades + header");
                    System.out.println("Time: " + csvTime + "ms\n");

                    // Export JSON
                    long jsonStartAll = System.currentTimeMillis();
                    jsonFile = fileIOService.exportToJSON(student, grades, baseFilename, reportType);
                    jsonTime = System.currentTimeMillis() - jsonStartAll;
                    jsonSize = getFileSize(jsonFile);

                    System.out.println("/ JSON Export completed");
                    System.out.println("File: " + new File(jsonFile).getName());
                    System.out.println("Location: " + new File(jsonFile).getParent());
                    System.out.println("Size: " + formatFileSize(jsonSize));
                    System.out.println("Structure: Nested objects with metadata");
                    System.out.println("Time: " + jsonTime + "ms\n");

                    // Export Binary
                    long binaryStartAll = System.currentTimeMillis();
                    binaryFile = fileIOService.exportToBinary(student, grades, baseFilename, reportType);
                    binaryTime = System.currentTimeMillis() - binaryStartAll;
                    binarySize = getFileSize(binaryFile);

                    System.out.println("/ Binary Export completed");
                    System.out.println("File: " + new File(binaryFile).getName());
                    System.out.println("Location: " + new File(binaryFile).getParent());
                    System.out.println("Size: " + formatFileSize(binarySize) + " (compressed)");
                    System.out.println("Format: Serialized StudentReport object");
                    System.out.println("Time: " + binaryTime + "ms");
                    break;
            }

            long totalTime = System.currentTimeMillis() - totalStartTime;
            long totalSize = csvSize + jsonSize + binarySize;

            // Display export performance summary for all formats
            if (formatChoice.equals("4")) {
                System.out.println("\n# Export Performance Summary:");
                System.out.println("Total Time: " + totalTime + "ms");
                System.out.println("Total Size: " + formatFileSize(totalSize));
                if (jsonSize > 0 && binarySize > 0) {
                    double compressionRatio = (double) jsonSize / binarySize;
                    System.out.println("Compression Ratio: " + String.format("%.1f:1", compressionRatio) +
                            " (binary vs JSON)");
                }
                System.out.println("I/O Operations: 3 parallel writes");
            }

            auditLogger.logWithTime("EXPORT_REPORT",
                    "Exported " + reportType + " report for " + studentId +
                            " in format: " + getFormatName(formatChoice),
                    totalTime, studentId);

        } catch (StudentNotFoundException e) {
            System.out.println("\nERROR: " + e.getMessage());
            auditLogger.logError("EXPORT_REPORT", "Student not found: " + studentId, e.getMessage(), studentId);
        } catch (ExportException | IOException e) {
            System.err.println("Export error: " + e.getMessage());
            auditLogger.logError("EXPORT_REPORT", "Export failed", e.getMessage(), studentId);
        }
    }

    // Helper method to get file size
    private static long getFileSize(String filePath) {
        try {
            File file = new File(filePath);
            return file.exists() ? file.length() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // Helper method to format file size
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }

    // Helper method to get format name
    private static String getFormatName(String choice) {
        switch (choice) {
            case "1": return "CSV";
            case "2": return "JSON";
            case "3": return "Binary";
            case "4": return "All formats";
            default: return "Unknown";
        }
    }

    private static void calculateStudentGPA() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              CALCULATE STUDENT GPA");
        System.out.println("=".repeat(80));

        System.out.print("Enter Student ID: ");
        String studentId = scanner.nextLine().trim().toUpperCase();

        try {
            double gpa = gpaCalculator.calculateGPA(studentId, true);
            System.out.printf("\n✓ GPA calculated: %.2f%n", gpa);
            auditLogger.logSimple("GPA_CALCULATION", "Calculated GPA for " + studentId, studentId);
        } catch (Exception e) {
            System.err.println("Error calculating GPA: " + e.getMessage());
            auditLogger.logError("GPA_CALCULATION", "Failed to calculate GPA", e.getMessage(), studentId);
        }
    }

    private static void viewClassStatistics() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              CLASS STATISTICS");
        System.out.println("=".repeat(80));

        try {
            statisticsCalculator.displayClassStatistics();
            auditLogger.logSimple("CLASS_STATISTICS", "Viewed class statistics", null);
        } catch (Exception e) {
            System.err.println("Error displaying statistics: " + e.getMessage());
            auditLogger.logError("CLASS_STATISTICS", "Failed to display statistics", e.getMessage(), null);
        }
    }

    private static void searchStudents() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              STUDENT SEARCH");
        System.out.println("=".repeat(80));

        System.out.println("Search by:");
        System.out.println("1. Student ID");
        System.out.println("2. Name");
        System.out.println("3. Email Domain");
        System.out.print("Select search type (1-3): ");

        String type = scanner.nextLine();
        System.out.print("Enter search term: ");
        String term = scanner.nextLine().trim();

        try {
            List<Student> results;
            switch (type) {
                case "1":
                    results = searchService.searchByStudentId(term.toUpperCase());
                    break;
                case "2":
                    results = searchService.searchByName(term);
                    break;
                case "3":
                    results = searchService.searchByEmailDomain(term);
                    break;
                default:
                    System.out.println("Invalid search type!");
                    return;
            }
            searchService.displaySearchResults(results);
            auditLogger.logSimple("STUDENT_SEARCH", "Found " + results.size() + " students", null);
        } catch (Exception e) {
            System.err.println("Search error: " + e.getMessage());
            auditLogger.logError("STUDENT_SEARCH", "Search failed", e.getMessage(), null);
        }
    }

    private static void streamProcessingAnalytics() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              STREAM PROCESSING ANALYTICS");
        System.out.println("=".repeat(80));

        try {
            streamProcessor.displayStreamCapabilities();
            auditLogger.logSimple("STREAM_ANALYTICS", "Performed stream processing analytics", null);
        } catch (Exception e) {
            System.err.println("Stream analytics error: " + e.getMessage());
            auditLogger.logError("STREAM_ANALYTICS", "Stream analytics failed", e.getMessage(), null);
        }
    }

    private static void startRealTimeDashboard() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              REAL-TIME DASHBOARD");
        System.out.println("=".repeat(80));

        try {
            // Ensure dashboard has proper dependencies
            CacheManager cacheManager = CacheManager.getInstance(); // Assuming you have this
            statisticsDashboard = new StatisticsDashboard(studentManager, gradeManager, cacheManager);

            statisticsDashboard.startDashboard(5); // Refresh every 5 seconds

            // Wait for dashboard to stop before returning to main menu
            while (statisticsDashboard.isRunning()) {
                Thread.sleep(100);
            }

            System.out.println("✓ Returning to main menu...");
            auditLogger.logSimple("DASHBOARD", "Stopped real-time dashboard", null);

            // Clear any remaining input
            try {
                while (System.in.available() > 0) {
                    System.in.read();
                }
            } catch (Exception e) {
                // Ignore
            }

        } catch (Exception e) {
            System.err.println("Dashboard error: " + e.getMessage());
            auditLogger.logError("DASHBOARD", "Failed to start dashboard", e.getMessage(), null);
        }
    }

    private static void scheduleAutomatedTasks(StudentManager studentManager, ConcurrentTaskService taskService) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              SCHEDULE AUTOMATED TASKS");
        System.out.println("=".repeat(80));

        // Display current scheduled tasks (using existing ones)
        System.out.println("\nCurrent Scheduled Tasks: 3 active\n");
        System.out.println("===== ACTIVE SCHEDULES =====");
        System.out.println("1. [DAILY] Backup Database");
        System.out.println("   Schedule: Every day at 02:00 AM");
        System.out.println("   Next Run: 2025-11-04 02:00:00");
        System.out.println("   Status: ✓ Success");

        System.out.println("\n2. [HOURLY] Update Statistics Cache");
        System.out.println("   Schedule: Every hour at :00");
        System.out.println("   Status: ⚡ Running (23% complete)");

        System.out.println("\n3. [WEEKLY] Generate Progress Reports");
        System.out.println("   Schedule: Every Monday at 08:00 AM");
        System.out.println("   Status: ✓ Success");

        System.out.println("\n" + "-".repeat(80));
        System.out.println("Add New Scheduled Task:");
        System.out.println("1. Daily GPA Recalculation");
        System.out.println("2. Weekly Grade Report Email");
        System.out.println("3. Monthly Performance Summary");
        System.out.println("4. Hourly Data Sync");
        System.out.println("5. Custom Schedule");
        System.out.println("6. Cancel");
        System.out.print("\nSelect option (1-6): ");

        String choice = scanner.nextLine();

        if (choice.equals("6")) {
            System.out.println("Task scheduling cancelled.");
            return;
        }

        // Use effectively final variables for lambda
        final StudentManager finalStudentManager = studentManager;
        final ConcurrentTaskService finalTaskService = taskService;

        switch (choice) {
            case "1":
                scheduleDailyGPARecalculation(finalStudentManager, finalTaskService);
                break;
            case "2":
                scheduleWeeklyReportEmail(finalTaskService);
                break;
            case "3":
                scheduleMonthlyPerformanceSummary(finalTaskService);
                break;
            case "4":
                finalTaskService.scheduleHourlyDataSync();
                System.out.println("✓ Hourly data sync scheduled!");
                break;
            case "5":
                scheduleCustomTask(finalTaskService);
                break;
            default:
                System.out.println("Invalid selection.");
        }
    }

    private static void scheduleDailyGPARecalculation(StudentManager studentManager, ConcurrentTaskService taskService) {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("CONFIGURE: Daily GPA Recalculation");
        System.out.println("-".repeat(80));

        // Execution time
        System.out.print("Enter hour (0-23): ");
        int hour = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter minute (0-59): ");
        int minute = Integer.parseInt(scanner.nextLine());

        // Target students
        System.out.println("\nTarget Students:");
        System.out.println("1. All Students");
        System.out.println("2. Honors Students Only");
        System.out.println("3. Students with Grade Changes");
        System.out.print("Select (1-3): ");
        String target = scanner.nextLine();

        // Thread pool configuration
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        System.out.println("\nRecommended: " + (availableProcessors / 2) + " threads for " + studentManager.getStudentCount() + " students");
        System.out.print("Enter thread count (1-8): ");
        int threadCount = Integer.parseInt(scanner.nextLine());

        // Notification settings
        System.out.println("\nNotification Settings:");
        System.out.println("1. Email summary on completion");
        System.out.println("2. Log to file only");
        System.out.println("3. Both");
        System.out.print("Select (1-3): ");
        String notify = scanner.nextLine();

        String email = "";
        if (notify.equals("1") || notify.equals("3")) {
            System.out.print("Enter notification email: ");
            email = scanner.nextLine();
            if (!ValidationUtils.validateEmail(email).isValid()) {  // FIXED: using validateEmail().isValid()
                System.out.println("Invalid email format. Task not scheduled.");
                return;
            }
        }

        System.out.println("\n" + "-".repeat(80));
        System.out.println("TASK CONFIGURATION SUMMARY");
        System.out.println("-".repeat(80));
        System.out.println("Task: Daily GPA Recalculation");
        System.out.println("Schedule: Every day at " + String.format("%02d:%02d", hour, minute));
        System.out.println("Scope: " + getTargetDescription(target, studentManager));
        System.out.println("Threads: " + threadCount + " (parallel execution)");
        System.out.println("Notifications: " + getNotificationDescription(notify));
        if (!email.isEmpty()) System.out.println("Recipient: " + email);
        System.out.println("Estimated Execution Time: ~2 minutes");
        System.out.println("Resource Usage: LOW");

        System.out.print("\nConfirm schedule? (Y/N): ");
        String confirm = scanner.nextLine();

        if (confirm.equalsIgnoreCase("Y")) {
            // Create final variables for lambda
            final String finalEmail = email;
            final String finalTarget = target;
            final int finalThreadCount = threadCount;

            // Schedule the task using ConcurrentTaskService
            taskService.scheduleCustomTask(
                    "Daily GPA Recalculation",
                    () -> {
                        System.out.println("\n[DAILY GPA TASK] Started at " + LocalDateTime.now());
                        System.out.println("Scope: " + getTargetDescription(finalTarget, studentManager));
                        System.out.println("Threads: " + finalThreadCount);

                        // Simulate task execution with progress bar
                        simulateProgressBar("Recalculating GPAs", 100);

                        // Get target students
                        List<Student> targetStudents = taskService.getTargetStudents(finalTarget, studentManager);
                        System.out.println("Processing " + targetStudents.size() + " students...");

                        System.out.println("\n[DAILY GPA TASK] Completed at " + LocalDateTime.now());

                        // Send email notification if configured
                        if (!finalEmail.isEmpty()) {
                            simulateEmailNotification(finalEmail,
                                    "Daily GPA Recalculation Completed",
                                    "Processed " + targetStudents.size() + " students successfully.");
                        }
                    },
                    calculateInitialDelay(hour, minute),
                    24 * 60 * 60 * 1000L, // Daily
                    TimeUnit.MILLISECONDS
            );

            System.out.println("\n✓ Task scheduled successfully!");
            System.out.println("Task ID: TASK-004");
            System.out.println("Scheduler Thread: RUNNING");

            // Calculate next execution time
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime scheduledTime = now.withHour(hour).withMinute(minute).withSecond(0);
            if (scheduledTime.isBefore(now)) {
                scheduledTime = scheduledTime.plusDays(1);
            }

            System.out.println("Next Execution: " + scheduledTime);
            long initialDelay = Duration.between(now, scheduledTime).toSeconds();
            System.out.println("Initial Delay: " + initialDelay + " seconds");
            System.out.println("\nThe task will run automatically in the background.");
            System.out.println("You can monitor its execution in the Audit Trail.");
        } else {
            System.out.println("Task scheduling cancelled.");
        }
    }

    private static void scheduleWeeklyReportEmail(ConcurrentTaskService taskService) {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("CONFIGURE: Weekly Grade Report Email");
        System.out.println("-".repeat(80));

        System.out.println("Select day of week:");
        System.out.println("1. Sunday");
        System.out.println("2. Monday");
        System.out.println("3. Tuesday");
        System.out.println("4. Wednesday");
        System.out.println("5. Thursday");
        System.out.println("6. Friday");
        System.out.println("7. Saturday");
        System.out.print("Select (1-7): ");
        int dayOfWeek = Integer.parseInt(scanner.nextLine());

        System.out.print("Enter hour (0-23): ");
        int hour = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter minute (0-59): ");
        int minute = Integer.parseInt(scanner.nextLine());

        System.out.print("Enter recipient email: ");
        String email = scanner.nextLine();

        if (!ValidationUtils.validateEmail(email).isValid()) {  // FIXED: using validateEmail().isValid()
            System.out.println("Invalid email format. Task not scheduled.");
            return;
        }

        // Schedule using ConcurrentTaskService
        taskService.scheduleWeeklyReportEmail(dayOfWeek, hour, minute);
    }

    private static void scheduleMonthlyPerformanceSummary(ConcurrentTaskService taskService) {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("CONFIGURE: Monthly Performance Summary");
        System.out.println("-".repeat(80));

        System.out.print("Enter day of month (1-28): ");
        int dayOfMonth = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter hour (0-23): ");
        int hour = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter minute (0-59): ");
        int minute = Integer.parseInt(scanner.nextLine());

        // Schedule using ConcurrentTaskService
        taskService.scheduleMonthlyPerformanceSummary(dayOfMonth, hour, minute);
    }

    private static void scheduleCustomTask(ConcurrentTaskService taskService) {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("CONFIGURE: Custom Task");
        System.out.println("-".repeat(80));

        System.out.print("Enter task description: ");
        String description = scanner.nextLine();

        System.out.print("Enter initial delay in minutes: ");
        long initialDelay = Long.parseLong(scanner.nextLine());

        System.out.print("Enter period in minutes: ");
        long period = Long.parseLong(scanner.nextLine());

        // Create a simple task
        Runnable customTask = () -> {
            System.out.println("[" + LocalDateTime.now() + "] Executing custom task: " + description);
            simulateProgressBar(description, 50);
            System.out.println("[" + LocalDateTime.now() + "] Custom task completed!");
        };

        // Schedule using ConcurrentTaskService
        taskService.scheduleCustomTask(
                description,
                customTask,
                initialDelay * 60 * 1000L,  // Convert minutes to milliseconds
                period * 60 * 1000L,        // Convert minutes to milliseconds
                TimeUnit.MILLISECONDS
        );
    }

    // Helper methods (unchanged from previous):
    private static String getTargetDescription(String target, StudentManager studentManager) {
        switch (target) {
            case "1": return "All Students (" + studentManager.getStudentCount() + ")";  // FIXED: using studentManager
            case "2": return "Honors Students Only (" + studentManager.getStudentsByType("Honors").size() + ")";
            case "3": return "Students with Grade Changes";
            default: return "Unknown";
        }
    }

    private static String getNotificationDescription(String notify) {
        switch (notify) {
            case "1": return "Email only";
            case "2": return "Log file only";
            case "3": return "Email + Log file";
            default: return "None";
        }
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

    private static void simulateProgressBar(String taskName, int steps) {
        System.out.print(taskName + ": [");
        for (int i = 0; i < steps; i++) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.print("=");
            if ((i + 1) % (steps / 10) == 0) {
                System.out.print(" " + ((i + 1) * 100 / steps) + "%");
            }
        }
        System.out.println("] COMPLETED");
    }

    private static void simulateEmailNotification(String to, String subject, String body) {
        System.out.println("[EMAIL SIM] Sent to: " + to);
        System.out.println("Subject: " + subject);
        System.out.println("Body: " + body);
    }

// Other task scheduling methods (similarly detailed) would follow...

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
        System.out.println("              BULK IMPORT GRADES (NIO.2 STREAMING)");
        System.out.println("=".repeat(80));

        // List available files first
        bulkImportService.listAvailableFiles();

        System.out.println("\n📋 Supported CSV Formats:");
        System.out.println("1. Simple format (no header):");
        System.out.println("   StudentID,SubjectName,SubjectType,Grade");
        System.out.println("   Example: STU001,Mathematics,Core,85.5");
        System.out.println();
        System.out.println("2. With header row:");
        System.out.println("   StudentID,SubjectName,SubjectType,Grade");
        System.out.println("   STU001,Mathematics,Core,85.5");
        System.out.println();
        System.out.println("3. With GradeID column:");
        System.out.println("   GradeID,StudentID,SubjectName,SubjectType,Grade");
        System.out.println("   GRD001,STU001,Mathematics,Core,85.5");

        System.out.println("\n📝 Validation Rules:");
        System.out.println("  • Student ID: STU### format (STU followed by 3 digits)");
        System.out.println("  • Subject Type: 'Core' or 'Elective' (case-insensitive)");
        System.out.println("  • Grade: 0-100 (whole numbers or decimals)");
        System.out.println("  • Student must exist in system before importing grades");

        System.out.print("\nEnter filename (with or without .csv extension): ");
        String filename = scanner.nextLine().trim();

        if (filename.isEmpty()) {
            System.out.println("⚠️  No filename entered. Operation cancelled.");
            return;
        }

        // Offer to create sample files if none exist
        Path importsDir = Paths.get("imports");
        try {
            if (!Files.exists(importsDir) ||
                    !Files.list(importsDir).anyMatch(p -> p.toString().toLowerCase().endsWith(".csv"))) {
                System.out.print("\nNo CSV files found. Create sample files? (Y/N): ");
                String response = scanner.nextLine();
                if (response.equalsIgnoreCase("Y")) {
                    bulkImportService.createSampleCSV();
                    System.out.println("\nNow try importing one of the sample files.");
                    return;
                }
            }
        } catch (IOException e) {
            // Ignore, just proceed
        }

        try {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("Starting bulk import...");
            bulkImportService.importGradesFromCSV(filename);
            auditLogger.logSimple("BULK_IMPORT", "Bulk import from " + filename, null);
        } catch (Exception e) {
            System.err.println("\n❌ Bulk import error: " + e.getMessage());
            e.printStackTrace();
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

    private static void generateBatchReports() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              CONCURRENT BATCH REPORTS");
        System.out.println("=".repeat(80));

        try {
            System.out.println("\n===== REPORT SCOPE: =====");
            System.out.println("1. All Students (" + studentManager.getStudentCount() + " students)");
            System.out.println("2. Regular Students Only");
            System.out.println("3. Honors Students Only");
            System.out.println("4. By Grade Range");
            System.out.print("Select scope (1-4): ");

            String scopeChoice = scanner.nextLine();

            System.out.println("\n===== REPORT FORMAT: =====");
            System.out.println("1. PDF Summary Report");
            System.out.println("2. Detailed Text Report");
            System.out.println("3. Excel Spreadsheet");
            System.out.println("4. All Formats (PDF, Text, Excel)");
            System.out.print("Select format (1-4): ");

            String formatChoice = scanner.nextLine();

            // Get the list of students based on scope
            List<Student> studentsToProcess = new ArrayList<>();
            List<Student> allStudents = studentManager.getStudents();

            switch (scopeChoice) {
                case "1": // All students
                    studentsToProcess = new ArrayList<>(allStudents);
                    System.out.println("✓ Selected: All Students (" + studentsToProcess.size() + ")");
                    break;
                case "2": // Regular students only
                    studentsToProcess = allStudents.stream()
                            .filter(s -> s instanceof RegularStudent)
                            .collect(Collectors.toList());
                    System.out.println("✓ Selected: Regular Students Only (" + studentsToProcess.size() + ")");
                    break;
                case "3": // Honors students only
                    studentsToProcess = allStudents.stream()
                            .filter(s -> s instanceof HonorsStudent)
                            .collect(Collectors.toList());
                    System.out.println("✓ Selected: Honors Students Only (" + studentsToProcess.size() + ")");
                    break;
                case "4": // By grade range
                    System.out.print("Enter minimum grade (0-100): ");
                    int minGrade = Integer.parseInt(scanner.nextLine());
                    System.out.print("Enter maximum grade (0-100): ");
                    int maxGrade = Integer.parseInt(scanner.nextLine());

                    studentsToProcess = allStudents.stream()
                            .filter(s -> {
                                // Use the GradeManager to get average grade
                                double avgGrade = gradeManager.calculateOverallAverage(s.getStudentId());
                                return avgGrade >= minGrade && avgGrade <= maxGrade;
                            })
                            .collect(Collectors.toList());
                    System.out.println("✓ Selected: Grade Range " + minGrade + "-" + maxGrade +
                            " (" + studentsToProcess.size() + " students)");
                    break;
                default:
                    System.out.println("⚠️  Invalid choice! Using all students.");
                    studentsToProcess = new ArrayList<>(allStudents);
            }

            if (studentsToProcess.isEmpty()) {
                System.out.println("❌ No students found for the selected scope!");
                return;
            }

            // Determine report types based on format choice
            List<String> reportTypes = new ArrayList<>();
            switch (formatChoice) {
                case "1": // PDF only
                    reportTypes.add("pdf");
                    System.out.println("✓ Format: PDF Summary");
                    break;
                case "2": // Text only
                    reportTypes.add("text");
                    System.out.println("✓ Format: Detailed Text");
                    break;
                case "3": // Excel only
                    reportTypes.add("excel");
                    System.out.println("✓ Format: Excel Spreadsheet");
                    break;
                case "4": // All formats
                    reportTypes.add("pdf");
                    reportTypes.add("text");
                    reportTypes.add("excel");
                    System.out.println("✓ Format: All Formats (PDF, Text, Excel)");
                    break;
                default:
                    System.out.println("⚠️  Invalid choice! Using all formats.");
                    reportTypes.add("pdf");
                    reportTypes.add("text");
                    reportTypes.add("excel");
            }

            // Get system information
            int availableProcessors = Runtime.getRuntime().availableProcessors();
            int recommendedMin = Math.max(2, availableProcessors / 2);
            int recommendedMax = Math.min(availableProcessors, 8);

            System.out.println("\n⚡⚡⚡⚡⚡⚡⚡⚡⚡⚡ SYSTEM INFORMATION: ⚡⚡⚡⚡⚡⚡⚡⚡⚡⚡");
            System.out.printf("  Available Processors: %d%n", availableProcessors);
            System.out.printf("  Recommended Threads: %d-%d%n", recommendedMin, recommendedMax);

            System.out.print("\n Enter number of threads (" + recommendedMin + "-" + recommendedMax + "): ");
            int threadCount;
            try {
                threadCount = Integer.parseInt(scanner.nextLine());
                threadCount = Math.max(recommendedMin, Math.min(threadCount, recommendedMax));
                System.out.println("✓ Using " + threadCount + " threads");
            } catch (NumberFormatException e) {
                threadCount = recommendedMin;
                System.out.println("⚠️  Invalid number! Using " + recommendedMin + " threads.");
            }

            // Calculate total tasks
            int totalTasks = studentsToProcess.size() * reportTypes.size();

            System.out.println("\n" + "=".repeat(80));
            System.out.println("⚡⚡⚡⚡⚡⚡⚡⚡⚡⚡ BATCH REPORT CONFIGURATION ⚡⚡⚡⚡⚡⚡⚡⚡⚡⚡");
            System.out.println("=".repeat(80));
            System.out.printf("Scope:           %s%n",
                    scopeChoice.equals("1") ? "All Students" :
                            scopeChoice.equals("2") ? "Regular Only" :
                                    scopeChoice.equals("3") ? "Honors Only" : "By Grade Range");
            System.out.printf("Student Count:   %d students%n", studentsToProcess.size());
            System.out.printf("Report Formats:  %s%n", String.join(", ", reportTypes));
            System.out.printf("Threads:         %d%n", threadCount);
            System.out.printf("Total Reports:   %d%n", totalTasks);
            System.out.printf("Estimated Sequential Time: %.1f seconds%n", totalTasks * 0.5);
            System.out.println("=".repeat(80));

            System.out.print("\n ✓ Proceed with concurrent report generation? (Y/N): ");
            String confirm = scanner.nextLine();
            if (!confirm.equalsIgnoreCase("Y")) {
                System.out.println("⚠️  Report generation cancelled.");
                return;
            }

            // Log the operation
            auditLogger.logSimple("BATCH_REPORTS",
                    String.format("Starting batch reports: %d students, %d threads, formats: %s",
                            studentsToProcess.size(), threadCount, String.join(",", reportTypes)), null);

            System.out.println("\n" + "⚡".repeat(40));
            System.out.println("  STARTING CONCURRENT BATCH PROCESSING");
            System.out.println("⚡".repeat(40));
            System.out.println("Processing " + totalTasks + " reports with " + threadCount + " threads...");
            System.out.println("Live status will update every 0.5 seconds");
            System.out.println("Press Ctrl+C to cancel (not recommended)");

            // Pause for user to see the message
            Thread.sleep(2000);

            // Create and start batch report service
            BatchReportService batchService = new BatchReportService(reportGenerator);

            try {
                batchService.generateConcurrentReports(studentsToProcess, reportTypes, threadCount);

                System.out.println("\n" + "=".repeat(40));
                System.out.println(" 12" +
                        "BATCH REPORTS COMPLETED SUCCESSFULLY");
                System.out.println("=".repeat(40));

                // Log success
                auditLogger.logSimple("BATCH_REPORTS",
                        "Batch reports completed successfully", null);

            } catch (Exception e) {
                System.err.println("\n❌ Error generating batch reports: " + e.getMessage());
                auditLogger.logError("BATCH_REPORTS", "Failed to generate batch reports",
                        e.getMessage(), null);
            }

            System.out.println("\nPress Enter to return to main menu...");
            scanner.nextLine();

        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            auditLogger.logError("BATCH_REPORTS", "Unexpected error in batch reports",
                    e.getMessage(), null);
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
                System.out.printf("\n✓ Search completed in %d ms%n", searchTime);
                System.out.println(" Found " + results.size() + " students");

                // Show action menu for matched students
                boolean stayInSearchMenu = true;
                while (stayInSearchMenu) {
                    System.out.println("\n" + "─".repeat(60));
                    System.out.println("ACTIONS FOR MATCHED STUDENTS");
                    System.out.println("─".repeat(60));
                    System.out.println("1.  Export search results");
                    System.out.println("2.  Generate reports for matched students");
                    System.out.println("3.  Send bulk email to matched students");
                    System.out.println("4.  New search with different pattern");
                    System.out.println("5.  Return to main menu");
                    System.out.print("Select action (1-5): ");

                    String action = scanner.nextLine();

                    switch (action) {
                        case "1": // Export search results
                            System.out.print("\nEnter filename for export: ");
                            String filename = scanner.nextLine().trim();
                            try {
                                reportGenerator.exportSearchResults(results, filename);
                                System.out.println("✓ Search results exported!");
                            } catch (ExportException e) {
                                System.err.println("✗ Export failed: " + e.getMessage());
                            }
                            break;

                        case "2": // Generate reports for matched students
                            generateReportsForMatchedStudents(results);
                            break;

                        case "3": // Send bulk email to matched students
                            sendBulkEmailToMatchedStudents(results);
                            break;

                        case "4": // New search with different pattern
                            stayInSearchMenu = false;
                            patternBasedSearch(); // Recursive call for new search
                            return; // Exit current method

                        case "5": // Return to main menu
                            stayInSearchMenu = false;
                            System.out.println("\nReturning to main menu...");
                            break;

                        default:
                            System.out.println("Invalid option! Please try again.");
                    }
                }

            } else {
                System.out.println("\n No students found matching the pattern.");

                // Offer to try a different pattern
                System.out.print("\nWould you like to try a different pattern? (Y/N): ");
                String tryAgain = scanner.nextLine();
                if (tryAgain.equalsIgnoreCase("Y")) {
                    patternBasedSearch();
                }
            }

            auditLogger.logWithTime("PATTERN_SEARCH",
                    String.format("Pattern search found %d students", results.size()),
                    searchTime, null);

        } catch (Exception e) {
            System.err.println("Pattern search error: " + e.getMessage());
            auditLogger.logError("PATTERN_SEARCH", "Pattern search failed", e.getMessage(), null);
        }
    }

// ============================================================================
// NEW HELPER METHODS FOR THE ACTIONS
// ============================================================================

    private static void generateReportsForMatchedStudents(List<Student> matchedStudents) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("        GENERATE REPORTS FOR MATCHED STUDENTS");
        System.out.println("=".repeat(80));

        if (matchedStudents.isEmpty()) {
            System.out.println("No students to generate reports for!");
            return;
        }

        System.out.println("\n Matched Students: " + matchedStudents.size());
        System.out.println("─".repeat(40));

        // Show report format options
        System.out.println("\nReport Format:");
        System.out.println("1. PDF Summary Reports");
        System.out.println("2. Detailed Text Reports");
        System.out.println("3. Excel Spreadsheets");
        System.out.println("4. All Formats (PDF, Text, Excel)");
        System.out.print("Select format (1-4): ");

        String formatChoice = scanner.nextLine();
        List<String> reportTypes = new ArrayList<>();

        switch (formatChoice) {
            case "1": reportTypes.add("pdf"); break;
            case "2": reportTypes.add("text"); break;
            case "3": reportTypes.add("excel"); break;
            case "4":
                reportTypes.add("pdf");
                reportTypes.add("text");
                reportTypes.add("excel");
                break;
            default:
                System.out.println("Invalid choice! Using all formats.");
                reportTypes.add("pdf");
                reportTypes.add("text");
                reportTypes.add("excel");
        }

        System.out.println("\nProcessing Options:");
        System.out.println("1. Sequential processing (one at a time)");
        System.out.println("2. Concurrent processing (faster)");
        System.out.print("Select processing mode (1-2): ");

        String processMode = scanner.nextLine();

        // Create BatchReportService
        BatchReportService batchService = new BatchReportService(reportGenerator);

        try {
            if (processMode.equals("2")) {
                // Concurrent processing
                int availableProcessors = Runtime.getRuntime().availableProcessors();
                int threadCount = Math.max(2, availableProcessors / 2);

                System.out.printf("\nUsing %d threads for concurrent processing...%n", threadCount);
                batchService.generateConcurrentReports(matchedStudents, reportTypes, threadCount);
            } else {
                // Sequential processing
                System.out.println("\nProcessing sequentially...");
                batchService.generateConcurrentReports(matchedStudents, reportTypes, 1); // FIXED HERE
            }

            System.out.println("\n All reports generated successfully!");

        } catch (ExportException e) {
            System.err.println("✗ Report generation failed: " + e.getMessage());
        }
    }

    private static void sendBulkEmailToMatchedStudents(List<Student> matchedStudents) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("        SEND BULK EMAIL TO MATCHED STUDENTS");
        System.out.println("=".repeat(80));

        if (matchedStudents.isEmpty()) {
            System.out.println("No students to email!");
            return;
        }

        System.out.println("\n Recipients: " + matchedStudents.size() + " students");
        System.out.println("─".repeat(40));

        // Show preview of recipients
        System.out.println("\n Recipient List:");
        for (int i = 0; i < Math.min(5, matchedStudents.size()); i++) {
            Student s = matchedStudents.get(i);
            System.out.printf("  %d. %s <%s>%n", i + 1, s.getName(), s.getEmail());
        }
        if (matchedStudents.size() > 5) {
            System.out.printf("  ... and %d more%n", matchedStudents.size() - 5);
        }

        System.out.println("\n  Email Options:");
        System.out.println("1. Academic Performance Notification");
        System.out.println("2. Grade Report Availability");
        System.out.println("3. Upcoming Events");
        System.out.println("4. Custom Message");
        System.out.print("Select email type (1-4): ");

        String emailType = scanner.nextLine();
        String subject = "";
        String template = "";

        switch (emailType) {
            case "1":
                subject = "Academic Performance Update";
                template = "Dear {name},\n\nYour current academic performance is being reviewed.\n" +
                        "Overall Average: {average}%\nStatus: {status}\n\n" +
                        "Please check your detailed report for more information.\n\n" +
                        "Best regards,\nAcademic Department";
                break;
            case "2":
                subject = "Grade Report Available";
                template = "Dear {name},\n\nYour grade report for the current term is now available.\n" +
                        "You can access it through the student portal or contact your advisor.\n\n" +
                        "Best regards,\nStudent Records Office";
                break;
            case "3":
                subject = "Upcoming Academic Events";
                template = "Dear {name},\n\nThis is a reminder about upcoming academic events:\n" +
                        "- Parent-Teacher Conference: Next Week\n" +
                        "- Final Exams: Month End\n" +
                        "- Report Cards: Following Week\n\n" +
                        "Best regards,\nAcademic Calendar Committee";
                break;
            case "4":
                System.out.print("Enter email subject: ");
                subject = scanner.nextLine();
                System.out.println("Enter email body (use {name} for student name placeholder):");
                System.out.print("> ");
                template = scanner.nextLine();
                break;
            default:
                System.out.println("Invalid choice! Using academic performance notification.");
                subject = "Academic Performance Update";
                template = "Dear {name},\n\nYour academic performance has been reviewed.\n\nBest regards,\nAcademic Department";
        }

        System.out.print("\nSend email preview before sending to all? (Y/N): ");
        String preview = scanner.nextLine();

        if (preview.equalsIgnoreCase("Y")) {
            System.out.println("\n EMAIL PREVIEW:");
            System.out.println("─".repeat(40));
            System.out.println("Subject: " + subject);
            System.out.println("\nBody (for first student):");
            Student previewStudent = matchedStudents.get(0);
            String previewBody = template
                    .replace("{name}", previewStudent.getName())
                    .replace("{average}", String.format("%.1f", gradeManager.calculateOverallAverage(previewStudent.getStudentId())))
                    .replace("{status}", previewStudent.getStatus());
            System.out.println(previewBody);
            System.out.println("─".repeat(40));
        }

        System.out.print("\nConfirm sending to " + matchedStudents.size() + " students? (Y/N): ");
        String confirm = scanner.nextLine();

        if (confirm.equalsIgnoreCase("Y")) {
            System.out.println("\n Sending emails...");

            int sentCount = 0;
            int failedCount = 0;

            for (Student student : matchedStudents) {
                try {
                    // Personalize the email
                    template
                            .replace("{name}", student.getName())
                            .replace("{average}", String.format("%.1f", gradeManager.calculateOverallAverage(student.getStudentId())))
                            .replace("{status}", student.getStatus());

                    // In a real system, you would send the email here
                    // For simulation, we'll just log it
                    System.out.printf("  ✓ Sent to: %s <%s>%n", student.getName(), student.getEmail());
                    sentCount++;

                    // Simulate sending delay
                    Thread.sleep(100);

                } catch (Exception e) {
                    System.out.printf("  ✗ Failed: %s <%s> - %s%n",
                            student.getName(), student.getEmail(), e.getMessage());
                    failedCount++;
                }
            }

            System.out.println("\n" + "─".repeat(40));
            System.out.println(" EMAIL SENDING SUMMARY");
            System.out.println("─".repeat(40));
            System.out.println("Successful: " + sentCount);
            System.out.println("Failed: " + failedCount);
            System.out.println("Total: " + matchedStudents.size());

            // Log the email sending
            auditLogger.logSimple("BULK_EMAIL",
                    String.format("Sent %d emails to matched students", sentCount), null);

        } else {
            System.out.println("Email sending cancelled.");
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
            System.out.printf("Free Memory:  %8.2f MB%n", runtime.freeMemory() / (1024.0 * 1024.0));
            System.out.printf("Total Memory: %8.2f MB%n", runtime.totalMemory() / (1024.0 * 1024.0));
            System.out.printf("Max Memory:   %8.2f MB%n", maxMemory / (1024.0 * 1024.0));

            // Memory bar chart matrix
            System.out.println("\nMemory Utilization Matrix:");
            System.out.println("-".repeat(50));
            int memoryBars = (int) Math.round(memoryUsagePercent / 2);
            System.out.printf("[%s%s] %3.0f%%%n",
                    "█".repeat(Math.min(50, memoryBars)),
                    "░".repeat(50 - Math.min(50, memoryBars)),
                    memoryUsagePercent);

            // Thread information with bar chart
            System.out.println("\nTHREAD INFORMATION:");
            System.out.println("-".repeat(50));
            int activeThreads = Thread.activeCount();
            int availableProcessors = runtime.availableProcessors();
            System.out.println("Active Threads: " + activeThreads);
            System.out.println("Available Processors: " + availableProcessors);

            // Thread utilization bar chart
            double threadUtilization = (activeThreads * 100.0) / (availableProcessors * 4); // Assume optimal is 4x processors
            int threadBars = (int) Math.round(threadUtilization / 2);
            System.out.println("\nThread Utilization Matrix:");
            System.out.println("-".repeat(50));
            System.out.printf("[%s%s] %3.0f%% (Optimal: %d threads)%n",
                    "█".repeat(Math.min(50, threadBars)),
                    "░".repeat(50 - Math.min(50, threadBars)),
                    threadUtilization,
                    availableProcessors * 2);

            // Collection performance with bar charts
            System.out.println("\nCOLLECTION PERFORMANCE:");
            System.out.println("-".repeat(50));

            // Student collections
            int studentCount = studentManager.getStudentCount();
            System.out.println("Student Manager Collections:");
            System.out.println("  HashMap<StudentID>: " + studentCount + " entries");

            // HashMap load factor visualization
            double hashMapLoad = Math.min(100.0, (studentCount * 100.0) / 100); // Assuming initial capacity 100
            int hashMapBars = (int) Math.round(hashMapLoad / 2);
            System.out.printf("    Load Factor: [%s%s] %3.0f%%%n",
                    "█".repeat(Math.min(50, hashMapBars)),
                    "░".repeat(50 - Math.min(50, hashMapBars)),
                    hashMapLoad);

            // TreeMap balancing visualization
            System.out.println("  TreeMap<GPA>: " + studentCount + " entries");
            double treeBalance = Math.min(100.0, 85.0 + (Math.random() * 15)); // Simulated balance factor
            int treeBars = (int) Math.round(treeBalance / 2);
            System.out.printf("    Balance:     [%s%s] %3.0f%%%n",
                    "█".repeat(Math.min(50, treeBars)),
                    "░".repeat(50 - Math.min(50, treeBars)),
                    treeBalance);

            // HashSet performance
            System.out.println("  HashSet<Email>: " + studentCount + " entries");
            double setPerformance = Math.min(100.0, 90.0 + (Math.random() * 10)); // Simulated performance
            int setBars = (int) Math.round(setPerformance / 2);
            System.out.printf("    Performance: [%s%s] %3.0f%%%n",
                    "█".repeat(Math.min(50, setBars)),
                    "░".repeat(50 - Math.min(50, setBars)),
                    setPerformance);

            // Grade collections
            System.out.println("\nGrade Manager Collections:");
            int gradeCount = gradeManager.getTotalGradeCount();
            System.out.println("  HashMap<StudentGrades>: " + gradeCount + " grades");

            // Grade map performance
            double gradeMapLoad = Math.min(100.0, (gradeCount * 100.0) / 500); // Assuming capacity 500
            int gradeBars = (int) Math.round(gradeMapLoad / 2);
            System.out.printf("    Utilization: [%s%s] %3.0f%%%n",
                    "█".repeat(Math.min(50, gradeBars)),
                    "░".repeat(50 - Math.min(50, gradeBars)),
                    gradeMapLoad);

            System.out.println("  TreeMap<Date>: Chronological sorting enabled");
            double dateTreeBalance = Math.min(100.0, 80.0 + (Math.random() * 20));
            int dateBars = (int) Math.round(dateTreeBalance / 2);
            System.out.printf("    Balance:     [%s%s] %3.0f%%%n",
                    "█".repeat(Math.min(50, dateBars)),
                    "░".repeat(50 - Math.min(50, dateBars)),
                    dateTreeBalance);

            System.out.println("  LinkedList<GradeHistory>: " + gradeCount + " entries");
            double listPerformance = Math.min(100.0, 75.0 + (Math.random() * 25));
            int listBars = (int) Math.round(listPerformance / 2);
            System.out.printf("    Performance: [%s%s] %3.0f%%%n",
                    "█".repeat(Math.min(50, listBars)),
                    "░".repeat(50 - Math.min(50, listBars)),
                    listPerformance);

            // Cache performance with bar chart matrix
            System.out.println("\nCACHE PERFORMANCE:");
            System.out.println("-".repeat(50));

            // Simulated cache hit rates
            double cacheHitRate = 85.0 + (Math.random() * 15); // 85-100%
            double cacheMemoryUsage = 60.0 + (Math.random() * 30); // 60-90%
            double cacheEfficiency = 75.0 + (Math.random() * 20); // 75-95%

            // Cache hit rate bar
            int cacheHitBars = (int) Math.round(cacheHitRate / 2);
            System.out.printf("Hit Rate:      [%s%s] %3.0f%%%n",
                    "█".repeat(Math.min(50, cacheHitBars)),
                    "░".repeat(50 - Math.min(50, cacheHitBars)),
                    cacheHitRate);

            // Cache memory usage bar
            int cacheMemBars = (int) Math.round(cacheMemoryUsage / 2);
            System.out.printf("Memory Usage:  [%s%s] %3.0f%%%n",
                    "█".repeat(Math.min(50, cacheMemBars)),
                    "░".repeat(50 - Math.min(50, cacheMemBars)),
                    cacheMemoryUsage);

            // Cache efficiency bar
            int cacheEffBars = (int) Math.round(cacheEfficiency / 2);
            System.out.printf("Efficiency:    [%s%s] %3.0f%%%n",
                    "█".repeat(Math.min(50, cacheEffBars)),
                    "░".repeat(50 - Math.min(50, cacheEffBars)),
                    cacheEfficiency);

            // Database connection performance
            System.out.println("\nDATABASE PERFORMANCE:");
            System.out.println("-".repeat(50));

            double dbResponseTime = 50.0 + (Math.random() * 150); // 50-200ms
            double dbThroughput = 80.0 + (Math.random() * 20); // 80-100%
            double dbConnectionPool = 70.0 + (Math.random() * 30); // 70-100%

            // Response time (lower is better, inverted visualization)
            double responseScore = Math.max(0, 100 - (dbResponseTime / 2));
            int responseBars = (int) Math.round(responseScore / 2);
            System.out.printf("Response Time: [%s%s] %3.0f ms%n",
                    "█".repeat(Math.min(50, responseBars)),
                    "░".repeat(50 - Math.min(50, responseBars)),
                    dbResponseTime);

            // Throughput bar
            int throughputBars = (int) Math.round(dbThroughput / 2);
            System.out.printf("Throughput:    [%s%s] %3.0f%%%n",
                    "█".repeat(Math.min(50, throughputBars)),
                    "░".repeat(50 - Math.min(50, throughputBars)),
                    dbThroughput);

            // Connection pool utilization bar
            int poolBars = (int) Math.round(dbConnectionPool / 2);
            System.out.printf("Conn Pool:     [%s%s] %3.0f%%%n",
                    "█".repeat(Math.min(50, poolBars)),
                    "░".repeat(50 - Math.min(50, poolBars)),
                    dbConnectionPool);

            // Performance recommendations
            System.out.println("\nPERFORMANCE RECOMMENDATIONS:");
            System.out.println("-".repeat(50));

            if (memoryUsagePercent > 80) {
                System.out.println("WARNING: Memory usage is high. Consider:");
                System.out.println("   • Increasing JVM heap size with -Xmx flag");
                System.out.println("   • Reviewing memory-intensive operations");
                System.out.println("   • Implementing memory caching strategies");
            } else if (memoryUsagePercent > 60) {
                System.out.println("NOTICE: Memory usage is moderate.");
                System.out.println("   • Monitor for memory leaks");
                System.out.println("   • Consider periodic garbage collection");
            } else {
                System.out.println("OK: Memory usage is within optimal range");
            }

            if (threadUtilization > 100) {
                System.out.println("\nWARNING: Thread count exceeds optimal range.");
                System.out.println("   • Review thread pool configurations");
                System.out.println("   • Consider implementing thread limits");
            } else if (threadUtilization > 80) {
                System.out.println("\nNOTICE: High thread utilization.");
                System.out.println("   • Monitor thread creation patterns");
            } else {
                System.out.println("\nOK: Thread utilization is optimal");
            }

            if (cacheHitRate < 80) {
                System.out.println("\nRECOMMENDATION: Consider increasing cache size");
                System.out.println("   • Current hit rate: " + String.format("%.1f", cacheHitRate) + "%");
                System.out.println("   • Target hit rate: >85%");
            }

            auditLogger.logSimple("SYSTEM_PERFORMANCE", "Viewed system performance metrics with bar charts", null);

        } catch (Exception e) {
            System.err.println("Error displaying system performance: " + e.getMessage());
            auditLogger.logError("SYSTEM_PERFORMANCE", "Failed to display metrics", e.getMessage(), null);
        }
    }

    private static void viewAuditTrail() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              AUDIT TRAIL VIEWER");
        System.out.println("=".repeat(80));

        try {
            auditLogger.displayRecentLogs(20); // Show last 20 logs
            auditLogger.logSimple("AUDIT_VIEW", "Viewed audit trail", null);
        } catch (Exception e) {
            System.err.println("Audit trail error: " + e.getMessage());
            auditLogger.logError("AUDIT_VIEW", "Failed to view audit trail", e.getMessage(), null);
        }
    }

    private static void manageCache() {
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