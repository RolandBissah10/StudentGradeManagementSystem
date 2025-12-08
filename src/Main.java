import java.util.*;
import java.util.regex.Pattern;
import java.io.File;
import exceptions.*;
import models.*;
import services.*;

public class Main {
    private static StudentManager studentManager = new StudentManager();
    private static GradeManager gradeManager = new GradeManager();
    private static Scanner scanner = new Scanner(System.in);

    // Service instances with dependency injection
    private static ReportGenerator reportGenerator = new ReportGenerator(studentManager, gradeManager);
    private static GPACalculator gpaCalculator = new GPACalculator(studentManager, gradeManager);
    private static StatisticsCalculator statisticsCalculator = new StatisticsCalculator(studentManager, gradeManager);
    private static BulkImportService bulkImportService = new BulkImportService(studentManager, gradeManager);
    private static SearchService searchService = new SearchService(studentManager, gradeManager);

    // Validation patterns
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+\\-\\s\\d()]+$");

    public static void main(String[] args) {
        createDirectories();
        initializeSampleData();
        displayMainMenu();
    }

    private static void createDirectories() {
        new File("reports").mkdirs();
        new File("imports").mkdirs();
    }

    private static void initializeSampleData() {
        //Add sample Students
        gradeManager.addGrade(new Grade("STU005", new ElectiveSubject("Physical Education", "PE"), 82.0));
        studentManager.addStudent(new RegularStudent("Alice Johnson", 16, "alice.johnson@school.edu", "+1-555-1001"));
        studentManager.addStudent(new HonorsStudent("Bob Smith", 17, "bob.smith@school.edu", "+1-555-1002"));
        studentManager.addStudent(new RegularStudent("Carol Martinez", 16, "carol.martinez@school.edu", "+1-555-1003"));
        studentManager.addStudent(new HonorsStudent("David Chen", 17, "david.chen@school.edu", "+1-555-1004"));
        studentManager.addStudent(new RegularStudent("Emma Wilson", 16, "emma.wilson@school.edu", "+1-555-1005"));

        studentManager.addStudent(new HonorsStudent("Alice Alison", 16, "alice.alison@school.edu", "+1-555-1901"));
        studentManager.addStudent(new RegularStudent("Melendez Praise", 18,"praiseM@school.edu", "+1-666-8790"));
        studentManager.addStudent(new HonorsStudent("David Frank", 19,  "david.frank@school.edu", "+1-666-8791"));
        studentManager.addStudent(new RegularStudent("John Williams ", 19,  "johnWill@school.edu", "+1-686-8594"));
        studentManager.addStudent(new HonorsStudent("John Bill", 18,  "billjohn@school.edu", "+1-877-0246"));

        // Add sample grades
        gradeManager.addGrade(new Grade("STU001", new CoreSubject("Mathematics", "MATH"), 85.0));
        gradeManager.addGrade(new Grade("STU001", new CoreSubject("English", "ENG"), 78.0));
        gradeManager.addGrade(new Grade("STU001", new CoreSubject("Science", "SCI"), 92.0));
        gradeManager.addGrade(new Grade("STU001", new ElectiveSubject("Music", "MUS"), 88.0));
        gradeManager.addGrade(new Grade("STU001", new ElectiveSubject("Art", "ART"), 63.0));

        gradeManager.addGrade(new Grade("STU002", new CoreSubject("Mathematics", "MATH"), 90.0));
        gradeManager.addGrade(new Grade("STU002", new CoreSubject("English", "ENG"), 85.0));
        gradeManager.addGrade(new Grade("STU002", new CoreSubject("Science", "SCI"), 88.0));
        gradeManager.addGrade(new Grade("STU002", new ElectiveSubject("Music", "MUS"), 91.0));
        gradeManager.addGrade(new Grade("STU002", new ElectiveSubject("Art", "ART"), 81.0));

        gradeManager.addGrade(new Grade("STU003", new CoreSubject("Mathematics", "MATH"), 45.0));
        gradeManager.addGrade(new Grade("STU003", new CoreSubject("English", "ENG"), 60.0));
        gradeManager.addGrade(new Grade("STU003", new CoreSubject("Science", "SCI"), 55.0));

        gradeManager.addGrade(new Grade("STU004", new CoreSubject("Mathematics", "MATH"), 95.0));
        gradeManager.addGrade(new Grade("STU004", new CoreSubject("English", "ENG"), 92.0));
        gradeManager.addGrade(new Grade("STU004", new CoreSubject("Science", "SCI"), 100.0));
        gradeManager.addGrade(new Grade("STU004", new ElectiveSubject("Music", "MUS"), 88.0));

        gradeManager.addGrade(new Grade("STU005", new CoreSubject("Mathematics", "MATH"), 75.0));
        gradeManager.addGrade(new Grade("STU005", new CoreSubject("English", "ENG"), 82.0));
        gradeManager.addGrade(new Grade("STU005", new ElectiveSubject("Art", "ART"), 75.0));
        gradeManager.addGrade(new Grade("STU005", new ElectiveSubject("Physical Education", "PE"), 82.0));
//added students
        gradeManager.addGrade(new Grade("STU006", new CoreSubject("Mathematics", "MATH"), 80.0));
        gradeManager.addGrade(new Grade("STU006", new CoreSubject("English", "ENG"), 70.0));
        gradeManager.addGrade(new Grade("STU006", new CoreSubject("Science", "SCI"), 90.0));
        gradeManager.addGrade(new Grade("STU006", new ElectiveSubject("Music", "MUS"), 88.0));
        gradeManager.addGrade(new Grade("STU006", new ElectiveSubject("Art", "ART"), 64.0));

        gradeManager.addGrade(new Grade("STU007", new CoreSubject("Mathematics", "MATH"), 40.0));
        gradeManager.addGrade(new Grade("STU007", new CoreSubject("English", "ENG"), 45.0));
        gradeManager.addGrade(new Grade("STU007", new CoreSubject("Science", "SCI"), 38.0));
        gradeManager.addGrade(new Grade("STU007", new ElectiveSubject("Music", "MUS"), 28.0));
        gradeManager.addGrade(new Grade("STU007", new ElectiveSubject("Art", "ART"), 32.0));

        gradeManager.addGrade(new Grade("STU008", new CoreSubject("Mathematics", "MATH"), 70.0));
        gradeManager.addGrade(new Grade("STU008", new CoreSubject("English", "ENG"), 85.0));
        gradeManager.addGrade(new Grade("STU008", new CoreSubject("Science", "SCI"), 98.0));
        gradeManager.addGrade(new Grade("STU008", new ElectiveSubject("Music", "MUS"), 78.0));
        gradeManager.addGrade(new Grade("STU008", new ElectiveSubject("Art", "ART"), 82.0));

        gradeManager.addGrade(new Grade("STU009", new CoreSubject("Mathematics", "MATH"), 49.0));
        gradeManager.addGrade(new Grade("STU009", new CoreSubject("English", "ENG"), 45.0));
        gradeManager.addGrade(new Grade("STU009", new CoreSubject("Science", "SCI"), 37.0));
        gradeManager.addGrade(new Grade("STU009", new ElectiveSubject("Music", "MUS"), 88.0));
        gradeManager.addGrade(new Grade("STU009", new ElectiveSubject("Art", "ART"), 36.0));

        gradeManager.addGrade(new Grade("STU0010", new CoreSubject("Mathematics", "MATH"), 89.0));
        gradeManager.addGrade(new Grade("STU0010", new CoreSubject("English", "ENG"), 90.0));
        gradeManager.addGrade(new Grade("STU0010", new CoreSubject("Science", "SCI"), 91.0));
        gradeManager.addGrade(new Grade("STU0010", new ElectiveSubject("Music", "MUS"), 92.0));
        gradeManager.addGrade(new Grade("STU0010", new ElectiveSubject("Art", "ART"), 93.0));

        // Update honors eligibility
        updateHonorsEligibility();
    }

    private static void updateHonorsEligibility() {
        Student[] students = studentManager.getStudents();
        int studentCount = studentManager.getStudentCountValue();

        for (int i = 0; i < studentCount; i++) {
            if (students[i] instanceof HonorsStudent) {
                HonorsStudent honorsStudent = (HonorsStudent) students[i];
                double avg = gradeManager.calculateOverallAverage(students[i].getStudentId());
                honorsStudent.setHonorsEligible(avg >= 85.0);
            }
        }
    }

    private static void displayMainMenu() {
        while (true) {
            System.out.println("\n=========================================");
            System.out.println("    STUDENT GRADE MANAGEMENT - MAIN MENU");
            System.out.println("=========================================");
            System.out.println("1. Add Student");
            System.out.println("2. View Students");
            System.out.println("3. Record Grade");
            System.out.println("4. View Grade Report");
            System.out.println("5. Export Grade Report");
            System.out.println("6. Calculate Student GPA");
            System.out.println("7. Bulk Import Grades");
            System.out.println("8. View Class Statistics");
            System.out.println("9. Search Students");
            System.out.println("10. Exit");
            System.out.print("\nEnter choice: ");

            String choice = scanner.nextLine();

            try {
                switch (choice) {
                    case "1": addStudent(); break;
                    case "2": viewStudents(); break;
                    case "3": recordGrade(); break;
                    case "4": viewGradeReport(); break;
                    case "5": exportGradeReport(); break;
                    case "6": calculateStudentGPA(); break;
                    case "7": bulkImportGrades(); break;
                    case "8": viewClassStatistics(); break;
                    case "9": searchStudents(); break;
                    case "10":
                        System.out.println("\nThank you for using Student Grade Management System!");
                        System.out.println("Goodbye!");
                        return;
                    default:
                        System.out.println("Invalid choice! Please enter 1-10.");
                }
            } catch (Exception e) {
                System.out.println("An unexpected error occurred: " + e.getMessage());
            }

            System.out.print("\nPress Enter to continue...");
            scanner.nextLine();
        }
    }

    private static void addStudent() {
        System.out.println("\n=========================================");
        System.out.println("              ADD STUDENT");
        System.out.println("=========================================");

        // Name validation
        String name;
        while (true) {
            System.out.print("Enter student name: ");
            name = scanner.nextLine().trim();
            if (name.isEmpty()) {
                System.out.println("Name cannot be empty. Please try again.");
            } else if (!NAME_PATTERN.matcher(name).matches()) {
                System.out.println("Invalid name! Name should contain only letters and spaces.");
            } else {
                break;
            }
        }

        // Age validation
        int age = 0;
        while (true) {
            System.out.print("Enter student age: ");
            String ageInput = scanner.nextLine();
            try {
                age = Integer.parseInt(ageInput);
                if (age < 5 || age > 100) {
                    System.out.println("Age must be between 5 and 100. Please try again.");
                } else {
                    break;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid age! Please enter a valid number.");
            }
        }

        // Email validation
        String email;
        while (true) {
            System.out.print("Enter student email: ");
            email = scanner.nextLine().trim();
            if (email.isEmpty()) {
                System.out.println("Email cannot be empty. Please try again.");
            } else if (!EMAIL_PATTERN.matcher(email).matches()) {
                System.out.println("Invalid email format! Email must contain @ symbol.");
            } else {
                break;
            }
        }

        // Phone validation
        String phone;
        while (true) {
            System.out.print("Enter student phone: ");
            phone = scanner.nextLine().trim();
            if (phone.isEmpty()) {
                System.out.println("Phone cannot be empty. Please try again.");
            } else if (!PHONE_PATTERN.matcher(phone).matches()) {
                System.out.println("Invalid phone number! Please enter a valid phone number.");
            } else if (phone.length() < 7) {
                System.out.println("Phone number too short! Please enter a valid phone number.");
            } else {
                break;
            }
        }

        System.out.println("\nStudent type:");
        System.out.println("1. Regular Student (Passing grade: 50%)");
        System.out.println("2. Honors Student (Passing grade: 60%, honors recognition)");
        System.out.print("Select type (1-2): ");

        String typeChoice = scanner.nextLine();
        Student student;

        if (typeChoice.equals("1")) {
            student = new RegularStudent(name, age, email, phone);
        } else if (typeChoice.equals("2")) {
            student = new HonorsStudent(name, age, email, phone);
        } else {
            System.out.println("Invalid choice! Student not added.");
            return;
        }

        studentManager.addStudent(student);

        System.out.println("\n✓ Student added successfully!");
        System.out.println("Student ID: " + student.getStudentId());
        System.out.println("Name: " + student.getName());
        System.out.println("Type: " + student.getStudentType());
        System.out.println("Age: " + student.getAge());
        System.out.println("Email: " + student.getEmail());
        System.out.println("Passing Grade: " + student.getPassingGrade() + "%");

        if (student instanceof HonorsStudent) {
            HonorsStudent honorsStudent = (HonorsStudent) student;
            System.out.println("Honors Eligible: " + (honorsStudent.checkHonorsEligibility() ? "Yes" : "No"));
        }

        System.out.println("Status: " + student.getStatus());
    }

    private static void viewStudents() {
        studentManager.viewAllStudents(gradeManager);
    }

    private static void recordGrade() {
        System.out.println("\n=========================================");
        System.out.println("              RECORD GRADE");
        System.out.println("=========================================");

        System.out.print("Enter Student ID: ");
        String studentId = scanner.nextLine();

        try {
            Student student = studentManager.findStudent(studentId);
            if (student == null) {
                throw new StudentNotFoundException(studentId);
            }

            System.out.println("\nStudent Details:");
            System.out.println("Name: " + student.getName());
            System.out.println("Type: " + student.getStudentType() + " Student");
            System.out.println("Current Average: " + String.format("%.1f", gradeManager.calculateOverallAverage(studentId)) + "%");

            System.out.println("\nSubject type:");
            System.out.println("1. Core Subject (Mathematics, English, Science)");
            System.out.println("2. Elective Subject (Music, Art, Physical Education)");
            System.out.print("Select type (1-2): ");

            String subjectType = scanner.nextLine();
            Subject subject = null;

            if (subjectType.equals("1")) {
                System.out.println("\nAvailable Core Subjects:");
                System.out.println("1. Mathematics");
                System.out.println("2. English");
                System.out.println("3. Science");
                System.out.print("Select subject (1-3): ");

                String coreChoice = scanner.nextLine();
                switch (coreChoice) {
                    case "1": subject = new CoreSubject("Mathematics", "MATH"); break;
                    case "2": subject = new CoreSubject("English", "ENG"); break;
                    case "3": subject = new CoreSubject("Science", "SCI"); break;
                    default:
                        System.out.println("Invalid choice!");
                        return;
                }
            } else if (subjectType.equals("2")) {
                System.out.println("\nAvailable Elective Subjects:");
                System.out.println("1. Music");
                System.out.println("2. Art");
                System.out.println("3. Physical Education");
                System.out.print("Select subject (1-3): ");

                String electiveChoice = scanner.nextLine();
                switch (electiveChoice) {
                    case "1": subject = new ElectiveSubject("Music", "MUS"); break;
                    case "2": subject = new ElectiveSubject("Art", "ART"); break;
                    case "3": subject = new ElectiveSubject("Physical Education", "PE"); break;
                    default:
                        System.out.println("Invalid choice!");
                        return;
                }
            } else {
                System.out.println("Invalid choice!");
                return;
            }

            // Grade validation
            double grade = -1;
            while (true) {
                System.out.print("Enter grade (0-100): ");
                String gradeInput = scanner.nextLine();
                try {
                    grade = Double.parseDouble(gradeInput);
                    if (grade < 0 || grade > 100) {
                        System.out.println("Invalid grade! Must be between 0 and 100.");
                    } else {
                        break;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid grade! Please enter a valid number.");
                }
            }

            Grade newGrade = new Grade(studentId, subject, grade);

            System.out.println("\nGRADE CONFIRMATION");
            System.out.println("Grade ID: " + newGrade.getGradeId());
            System.out.println("Student: " + studentId + " - " + student.getName());
            System.out.println("Subject: " + subject.getSubjectName() + " (" + subject.getSubjectType() + ")");
            System.out.println("Grade: " + grade + "%");
            System.out.println("Date: " + newGrade.getDate());

            System.out.print("\nConfirm grade? (Y/N): ");
            String confirm = scanner.nextLine();

            if (confirm.equalsIgnoreCase("Y")) {
                gradeManager.addGrade(newGrade);

                // Update honors eligibility for honors students
                if (student instanceof HonorsStudent) {
                    HonorsStudent honorsStudent = (HonorsStudent) student;
                    double avg = gradeManager.calculateOverallAverage(studentId);
                    honorsStudent.setHonorsEligible(avg >= 85.0);
                }

                System.out.println("\n✓ Grade recorded successfully!");
            } else {
                System.out.println("Grade recording cancelled.");
            }

        } catch (StudentNotFoundException e) {
            System.out.println("\n✗ ERROR: " + e.getMessage());
            displayAvailableStudentIds();
        }
    }

    private static void viewGradeReport() {
        System.out.println("\n=========================================");
        System.out.println("           VIEW GRADE REPORT");
        System.out.println("=========================================");

        System.out.print("Enter Student ID: ");
        String studentId = scanner.nextLine();

        try {
            Student student = studentManager.findStudent(studentId);
            if (student == null) {
                throw new StudentNotFoundException(studentId);
            }

            gradeManager.viewGradesByStudent(studentId, student);

        } catch (StudentNotFoundException e) {
            System.out.println("\n✗ ERROR: " + e.getMessage());
            displayAvailableStudentIds();
        }
    }

    private static void exportGradeReport() {
        System.out.println("\n=========================================");
        System.out.println("           EXPORT GRADE REPORT");
        System.out.println("=========================================");

        System.out.print("Enter Student ID: ");
        String studentId = scanner.nextLine();

        try {
            Student student = studentManager.findStudent(studentId);
            if (student == null) {
                throw new StudentNotFoundException(studentId);
            }

            System.out.println("\nStudent: " + studentId + " - " + student.getName());
            System.out.println("Type: " + student.getStudentType() + " Student");
            System.out.println("Total Grades: " + gradeManager.getGradeCountForStudent(studentId));

            System.out.println("\nExport options:");
            System.out.println("1. Summary Report (overview only)");
            System.out.println("2. Detailed Report (all grades)");
            System.out.println("3. Both");
            System.out.print("Select option (1-3): ");

            String option = scanner.nextLine();
            System.out.print("Enter filename (without extension): ");
            String filename = scanner.nextLine();

            switch (option) {
                case "1":
                    reportGenerator.exportSummaryReport(studentId, filename);
                    break;
                case "2":
                    reportGenerator.exportDetailedReport(studentId, filename);
                    break;
                case "3":
                    reportGenerator.exportSummaryReport(studentId, filename);
                    reportGenerator.exportDetailedReport(studentId, filename);
                    break;
                default:
                    System.out.println("Invalid option!");
                    return;
            }

            System.out.println("\n✓ Report exported successfully!");
            System.out.println("File: " + filename + ".txt");
            System.out.println("Location: ./reports/");

            // Get file info
            File summaryFile = new File("reports/" + filename + "_summary.txt");
            File detailedFile = new File("reports/" + filename + "_detailed.txt");

            if (option.equals("1") && summaryFile.exists()) {
                System.out.println("Size: " + (summaryFile.length() / 1024.0) + " KB");
                System.out.println("Contains: Student overview and performance summary");
            } else if (option.equals("2") && detailedFile.exists()) {
                System.out.println("Size: " + (detailedFile.length() / 1024.0) + " KB");
                System.out.println("Contains: " + gradeManager.getGradeCountForStudent(studentId) +
                        " grades, averages, performance summary");
            } else if (option.equals("3")) {
                long totalSize = (summaryFile.exists() ? summaryFile.length() : 0) +
                        (detailedFile.exists() ? detailedFile.length() : 0);
                System.out.println("Total Size: " + (totalSize / 1024.0) + " KB");
                System.out.println("Contains: Both summary and detailed reports");
            }

        } catch (StudentNotFoundException e) {
            System.out.println("\n✗ ERROR: " + e.getMessage());
            displayAvailableStudentIds();
        } catch (ExportException e) {
            System.out.println("\n✗ ERROR: " + e.getMessage());
        }
    }

    private static void calculateStudentGPA() {
        System.out.println("\n=========================================");
        System.out.println("          CALCULATE STUDENT GPA");
        System.out.println("=========================================");

        System.out.print("Enter Student ID: ");
        String studentId = scanner.nextLine();

        try {
            Student student = studentManager.findStudent(studentId);
            if (student == null) {
                throw new StudentNotFoundException(studentId);
            }

            System.out.println("\nStudent: " + studentId + " - " + student.getName());
            System.out.println("Type: " + student.getStudentType() + " Student");
            System.out.println("Overall Average: " +
                    String.format("%.1f%%", gradeManager.calculateOverallAverage(studentId)));

            gpaCalculator.displayGPABreakdown(studentId);

        } catch (StudentNotFoundException e) {
            System.out.println("\n✗ ERROR: " + e.getMessage());
            displayAvailableStudentIds();
        }
    }

    private static void bulkImportGrades() {
        System.out.println("\n=========================================");
        System.out.println("            BULK IMPORT GRADES");
        System.out.println("=========================================");

        System.out.println("\nPlace your CSV file in: ./imports/");
        System.out.println();
        System.out.println("CSV Format Required:");
        System.out.println("StudentID,SubjectName,SubjectType,Grade");
        System.out.println("Example: STU001,Mathematics,Core,85");
        System.out.println();
        System.out.print("Enter filename (without extension): ");
        String filename = scanner.nextLine();

        try {
            bulkImportService.importGradesFromCSV(filename);
            updateHonorsEligibility();
        } catch (Exception e) {
            System.out.println("\n✗ ERROR during import: " + e.getMessage());
        }
    }

    private static void viewClassStatistics() {
        statisticsCalculator.displayClassStatistics();
    }

    private static void searchStudents() {
        System.out.println("\n=========================================");
        System.out.println("              SEARCH STUDENTS");
        System.out.println("=========================================");

        System.out.println("\nSearch options:");
        System.out.println("1. By Student ID");
        System.out.println("2. By Name (partial match)");
        System.out.println("3. By Grade Range");
        System.out.println("4. By Student Type");
        System.out.print("Select option (1-4): ");

        String option = scanner.nextLine();
        List<Student> results = new ArrayList<>();

        try {
            switch (option) {
                case "1":
                    System.out.print("Enter Student ID: ");
                    String studentId = scanner.nextLine();
                    results = searchService.searchByStudentId(studentId);
                    break;
                case "2":
                    System.out.print("Enter name (partial or full): ");
                    String name = scanner.nextLine();
                    results = searchService.searchByName(name);
                    break;
                case "3":
                    System.out.print("Enter minimum grade (0-100): ");
                    double minGrade = Double.parseDouble(scanner.nextLine());
                    System.out.print("Enter maximum grade (0-100): ");
                    double maxGrade = Double.parseDouble(scanner.nextLine());
                    if (minGrade < 0 || maxGrade > 100 || minGrade > maxGrade) {
                        System.out.println("Invalid grade range! Minimum must be 0-100 and less than maximum.");
                        return;
                    }
                    results = searchService.searchByGradeRange(minGrade, maxGrade);
                    break;
                case "4":
                    System.out.print("Enter student type (Regular/Honors): ");
                    String type = scanner.nextLine();
                    if (!type.equalsIgnoreCase("Regular") && !type.equalsIgnoreCase("Honors")) {
                        System.out.println("Invalid student type! Must be 'Regular' or 'Honors'.");
                        return;
                    }
                    results = searchService.searchByStudentType(type);
                    break;
                default:
                    System.out.println("Invalid option!");
                    return;
            }

            searchService.displaySearchResults(results);

            if (!results.isEmpty()) {
                System.out.println("\nActions:");
                System.out.println("1. View full details for a student");
                System.out.println("2. Export search results");
                System.out.println("3. New search");
                System.out.println("4. Return to main menu");
                System.out.print("Enter choice: ");

                String action = scanner.nextLine();

                switch (action) {
                    case "1":
                        System.out.print("Enter Student ID from results: ");
                        String selectedId = scanner.nextLine();
                        Student selectedStudent = studentManager.findStudent(selectedId);
                        if (selectedStudent != null && results.contains(selectedStudent)) {
                            gradeManager.viewGradesByStudent(selectedId, selectedStudent);
                        } else {
                            System.out.println("Invalid Student ID or student not in search results.");
                        }
                        break;
                    case "2":
                        System.out.print("Enter filename for export: ");
                        String exportFile = scanner.nextLine();
                        try {
                            reportGenerator.exportSearchResults(results, exportFile);
                            System.out.println("✓ Search results exported successfully!");
                        } catch (ExportException e) {
                            System.out.println("✗ Export failed: " + e.getMessage());
                        }
                        break;
                    case "3":
                        searchStudents();
                        break;
                    case "4":
                        // Return to main menu
                        break;
                    default:
                        System.out.println("Invalid choice!");
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format! Please enter valid numbers for grade range.");
        } catch (Exception e) {
            System.out.println("An error occurred during search: " + e.getMessage());
        }
    }

    private static void displayAvailableStudentIds() {
        Student[] students = studentManager.getStudents();
        int studentCount = studentManager.getStudentCountValue();

        if (studentCount > 0) {
            System.out.print("Available student IDs: ");
            for (int i = 0; i < studentCount; i++) {
                System.out.print(students[i].getStudentId());
                if (i < studentCount - 1) System.out.print(", ");
            }
            System.out.println();
        }
    }
}