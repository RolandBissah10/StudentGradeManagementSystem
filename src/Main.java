import java.util.Scanner;
import java.util.regex.Pattern;

public class Main {
    private static StudentManager studentManager = new StudentManager();
    private static GradeManager gradeManager = new GradeManager();
    private static Scanner scanner = new Scanner(System.in);

    // Validation patterns
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+\\-\\s\\d()]+$");

    public static void main(String[] args) {
        initializeSampleData();
        displayMainMenu();
    }

    private static void initializeSampleData() {
        // Add 5 sample students (3 Regular, 2 Honors)
        studentManager.addStudent(new RegularStudent("Alice Johnson", 16, "alice.johnson@school.edu", "+1-555-1001"));
        studentManager.addStudent(new HonorsStudent("Bob Smith", 17, "bob.smith@school.edu", "+1-555-1002"));
        studentManager.addStudent(new RegularStudent("Carol Martinez", 16, "carol.martinez@school.edu", "+1-555-1003"));
        studentManager.addStudent(new HonorsStudent("David Chen", 17, "david.chen@school.edu", "+1-555-1004"));
        studentManager.addStudent(new RegularStudent("Emma Wilson", 16, "emma.wilson@school.edu", "+1-555-1005"));

        // Add sample grades
        gradeManager.addGrade(new Grade("STU001", new CoreSubject("Mathematics", "MATH"), 85.0));
        gradeManager.addGrade(new Grade("STU001", new CoreSubject("English", "ENG"), 78.0));
        gradeManager.addGrade(new Grade("STU001", new CoreSubject("Science", "SCI"), 92.0));
        gradeManager.addGrade(new Grade("STU002", new CoreSubject("Mathematics", "MATH"), 90.0));
        gradeManager.addGrade(new Grade("STU002", new ElectiveSubject("Music", "MUS"), 88.0));
        gradeManager.addGrade(new Grade("STU003", new CoreSubject("English", "ENG"), 45.0));
        gradeManager.addGrade(new Grade("STU004", new CoreSubject("Science", "SCI"), 95.0));
        gradeManager.addGrade(new Grade("STU005", new ElectiveSubject("Art", "ART"), 75.0));
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
            System.out.println("5. Exit");
            System.out.print("\nEnter choice: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    addStudent();
                    break;
                case "2":
                    viewStudents();
                    break;
                case "3":
                    recordGrade();
                    break;
                case "4":
                    viewGradeReport();
                    break;
                case "5":
                    System.out.println("\nThank you for using Student Grade Management System!");
                    System.out.println("Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice! Please enter 1-5.");
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
            // Update honors eligibility based on current average
            double avg = gradeManager.calculateOverallAverage(student.getStudentId());
            honorsStudent.setHonorsEligible(avg >= 85.0);
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

        Student student = studentManager.findStudent(studentId);
        if (student == null) {
            System.out.println("Student not found!");
            return;
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
    }

    private static void viewGradeReport() {
        System.out.println("\n=========================================");
        System.out.println("           VIEW GRADE REPORT");
        System.out.println("=========================================");

        System.out.print("Enter Student ID: ");
        String studentId = scanner.nextLine();

        Student student = studentManager.findStudent(studentId);
        if (student == null) {
            System.out.println("Student not found!");
            return;
        }

        gradeManager.viewGradesByStudent(studentId, student);
    }
}