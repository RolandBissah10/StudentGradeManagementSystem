package services;

import models.*;
//import utils.ValidationPatterns;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BulkImportService {
    private StudentManager studentManager;
    private GradeManager gradeManager;

    // Regex patterns for validation
    private static final Pattern GRADE_PATTERN = Pattern.compile("^(100|[1-9]?[0-9])$"); // 0-100
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^STU\\d{3}$"); // STU followed by 3 digits
    private static final Pattern SUBJECT_TYPE_PATTERN = Pattern.compile("^(Core|Elective)$", Pattern.CASE_INSENSITIVE);

    // Expected CSV format from PDF: gradeId, studentId, subjectName, subjectType, grade
    // But in your sample: studentId, subjectName, subjectType, grade

    public BulkImportService(StudentManager studentManager, GradeManager gradeManager) {
        this.studentManager = studentManager;
        this.gradeManager = gradeManager;
    }

    /**
     * Import grades from CSV file with multiple format support
     * Supports both formats:
     * 1. With gradeId: gradeId, studentId, subjectName, subjectType, grade
     * 2. Without gradeId: studentId, subjectName, subjectType, grade (auto-generates gradeId)
     */
    public void importGradesFromCSV(String filename) {
        // Ensure imports directory exists
        Path importsDir = Paths.get("imports");
        try {
            if (!Files.exists(importsDir)) {
                Files.createDirectories(importsDir);
                System.out.println("✓ Created imports directory");
            }
        } catch (IOException e) {
            System.out.println("✗ ERROR: Could not create imports directory: " + e.getMessage());
            return;
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String logFilename = "import_log_" + timestamp + ".txt";
        AtomicInteger totalRows = new AtomicInteger(0);
        AtomicInteger successfulImports = new AtomicInteger(0);
        AtomicInteger failedImports = new AtomicInteger(0);
        AtomicInteger skippedRows = new AtomicInteger(0);

        // Try with and without .csv extension
        Path filePath = Paths.get("imports", filename);
        if (!Files.exists(filePath)) {
            filePath = Paths.get("imports", filename + ".csv");
        }

        if (!Files.exists(filePath)) {
            System.out.println("✗ ERROR: File not found: " + filePath);
            System.out.println("  Looking for: " + filename + " or " + filename + ".csv in imports/ directory");
            System.out.println("  Current imports directory contents:");
            try (Stream<Path> list = Files.list(importsDir)) {
                list.forEach(p -> System.out.println("    - " + p.getFileName()));
            } catch (IOException e) {
                System.out.println("    (cannot list directory)");
            }

            // Offer to create sample file
            System.out.print("\nWould you like to create a sample CSV file? (Y/N): ");
            Scanner tempScanner = new Scanner(System.in);
            String response = tempScanner.nextLine();
            if (response.equalsIgnoreCase("Y")) {
                createSampleCSV();
            }
            return;
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("            BULK IMPORT WITH NIO.2 STREAMING");
        System.out.println("=".repeat(80));
        System.out.println("File: " + filePath.getFileName());
        System.out.println("Method: NIO.2 Streaming (memory efficient)");
        System.out.println("Path: " + filePath.toAbsolutePath());

        try {
            long fileSize = Files.size(filePath);
            System.out.println("Size: " + String.format("%,d", fileSize) + " bytes");
        } catch (IOException e) {
            System.out.println("Size: Unknown");
        }

        System.out.println("=".repeat(80));
        System.out.println("\nProcessing with NIO.2 streaming...");

        // First, detect CSV format by reading first line
        int expectedColumns;
        boolean hasHeader = false;
        boolean hasGradeId = false;

        try (Stream<String> testStream = Files.lines(filePath).limit(2)) {
            List<String> firstTwoLines = testStream.collect(Collectors.toList());

            if (firstTwoLines.isEmpty()) {
                System.out.println("✗ ERROR: File is empty");
                return;
            }

            String firstLine = firstTwoLines.get(0);
            String[] firstLineParts = parseCSVLine(firstLine).toArray(new String[0]);

            // Check if first line is a header
            if (firstLine.toLowerCase().contains("student") || firstLine.toLowerCase().contains("grade")) {
                hasHeader = true;
                System.out.println("✓ Detected header row");

                if (firstTwoLines.size() > 1) {
                    String secondLine = firstTwoLines.get(1);
                    String[] secondLineParts = parseCSVLine(secondLine).toArray(new String[0]);
                    expectedColumns = secondLineParts.length;
                    hasGradeId = expectedColumns == 5; // With gradeId column
                } else {
                    expectedColumns = firstLineParts.length;
                    hasGradeId = expectedColumns == 5;
                }
            } else {
                // No header, use first line to determine format
                expectedColumns = firstLineParts.length;
                hasGradeId = expectedColumns == 5;
                System.out.println("✓ No header row detected");
            }

            System.out.println("✓ Detected format: " + expectedColumns + " columns");
            System.out.println("✓ Has Grade ID column: " + (hasGradeId ? "Yes" : "No (will auto-generate)"));

        } catch (IOException e) {
            System.out.println("✗ ERROR reading file: " + e.getMessage());
            return;
        }

        try (Stream<String> lines = Files.lines(filePath);
             PrintWriter logWriter = new PrintWriter(Files.newBufferedWriter(
                     Paths.get("imports", logFilename), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {

            logWriter.println("BULK IMPORT LOG");
            logWriter.println("Timestamp: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            logWriter.println("Source File: " + filePath.getFileName());
            logWriter.println("Format: " + expectedColumns + " columns, " +
                    (hasGradeId ? "with GradeID" : "without GradeID"));
            logWriter.println("Has Header: " + hasHeader);
            logWriter.println("=========================================");
            logWriter.println();

            // Process each line
            AtomicInteger lineNumber = new AtomicInteger(0);

            boolean finalHasHeader = hasHeader;
            boolean finalHasGradeId = hasGradeId;
            lines.forEach(line -> {
                int currentLine = lineNumber.incrementAndGet();

                // Skip header if present
                if (finalHasHeader && currentLine == 1) {
                    logWriter.println("Line 1: SKIPPED - Header row: " + line);
                    return;
                }

                int currentRow = totalRows.incrementAndGet();
                boolean success = processCSVRow(line, logWriter, currentRow, expectedColumns, finalHasGradeId);
                if (success) {
                    successfulImports.incrementAndGet();
                } else {
                    failedImports.incrementAndGet();
                }
            });

            // Write import summary to log
            logWriter.println();
            logWriter.println("=== IMPORT SUMMARY ===");
            logWriter.println("Total Rows Processed: " + totalRows.get());
            logWriter.println("Successfully Imported: " + successfulImports.get());
            logWriter.println("Failed: " + failedImports.get());
            logWriter.println("Skipped (header/empty): " + (hasHeader ? 1 : 0));
            logWriter.println("Success Rate: " +
                    (totalRows.get() > 0 ? String.format("%.1f%%", (successfulImports.get() * 100.0 / totalRows.get())) : "0%"));
            logWriter.println("Completed: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

            // Display summary to user
            System.out.println("\n" + "=".repeat(80));
            System.out.println("              IMPORT COMPLETED");
            System.out.println("=".repeat(80));
            System.out.println("Total Rows: " + totalRows.get());
            System.out.println("✅ Successfully Imported: " + successfulImports.get());
            System.out.println("❌ Failed: " + failedImports.get());

            if (totalRows.get() > 0) {
                double successRate = (successfulImports.get() * 100.0) / totalRows.get();
                System.out.printf("Success Rate: %.1f%%%n", successRate);

                // Visual success indicator
                System.out.print("Success Rate: [");
                int bars = (int) (successRate / 2); // 50 characters max
                for (int i = 0; i < 50; i++) {
                    if (i < bars) {
                        if (successRate >= 90) System.out.print("█");
                        else if (successRate >= 70) System.out.print("▓");
                        else if (successRate >= 50) System.out.print("▒");
                        else System.out.print("░");
                    } else {
                        System.out.print(" ");
                    }
                }
                System.out.printf("] %.1f%%%n", successRate);
            }

            System.out.println("\n✓ Log saved to: imports/" + logFilename);
            System.out.println("✓ NIO.2 streaming completed successfully");
            System.out.println("✓ " + successfulImports.get() + " grades added to system");
            System.out.println("=".repeat(80));

        } catch (IOException e) {
            System.out.println("✗ ERROR reading file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean processCSVRow(String line, PrintWriter logWriter, int rowNumber, int expectedColumns, boolean hasGradeId) {
        // Clean the line
        line = line.trim();
        if (line.isEmpty()) {
            logWriter.println("Row " + rowNumber + ": SKIPPED - Empty line");
            return false;
        }

        // Handle quoted CSV fields
        List<String> parts = parseCSVLine(line);

        if (parts.size() != expectedColumns) {
            logWriter.println("Row " + rowNumber + ": FAILED - Expected " + expectedColumns +
                    " columns, found " + parts.size());
            logWriter.println("  Line: " + line);
            logWriter.println("  Expected format: " +
                    (hasGradeId ? "gradeId,studentId,subjectName,subjectType,grade" :
                            "studentId,subjectName,subjectType,grade"));
            return false;
        }

        try {
            String gradeId;
            String studentId;
            String subjectName;
            String subjectType;
            String gradeStr;

            // Parse based on format
            if (hasGradeId) {
                // Format: gradeId, studentId, subjectName, subjectType, grade
                gradeId = parts.get(0).trim();
                studentId = parts.get(1).trim();
                subjectName = parts.get(2).trim();
                subjectType = parts.get(3).trim();
                gradeStr = parts.get(4).trim();

                // Validate gradeId
                if (gradeId == null || gradeId.isEmpty()) {
                    logWriter.println("Row " + rowNumber + ": FAILED - Grade ID is empty");
                    return false;
                }
            } else {
                // Format: studentId, subjectName, subjectType, grade (auto-generate gradeId)
                gradeId = generateGradeId();
                studentId = parts.get(0).trim();
                subjectName = parts.get(1).trim();
                subjectType = parts.get(2).trim();
                gradeStr = parts.get(3).trim();
            }

            // Validate student ID with regex
            if (!STUDENT_ID_PATTERN.matcher(studentId).matches()) {
                logWriter.println("Row " + rowNumber + ": FAILED - Invalid Student ID format");
                logWriter.println("  Input: " + studentId);
                logWriter.println("  Expected pattern: STU### (STU followed by exactly 3 digits)");
                logWriter.println("  Examples: STU001, STU042, STU999");
                return false;
            }

            // Check student exists
            Student student = studentManager.findStudent(studentId);
            if (student == null) {
                logWriter.println("Row " + rowNumber + ": FAILED - Student not found: " + studentId);
                List<String> availableStudents = studentManager.getStudents().stream()
                        .map(Student::getStudentId)
                        .collect(Collectors.toList());
                if (!availableStudents.isEmpty()) {
                    logWriter.println("  Available students: " + String.join(", ", availableStudents));
                } else {
                    logWriter.println("  No students in system. Add students first.");
                }
                return false;
            }

            // Validate grade with regex
            if (!GRADE_PATTERN.matcher(gradeStr).matches()) {
                logWriter.println("Row " + rowNumber + ": FAILED - Invalid grade format");
                logWriter.println("  Input: " + gradeStr);
                logWriter.println("  Expected pattern: 0-100 (whole numbers only)");
                logWriter.println("  Examples: 85, 92.5, 100");
                return false;
            }

            double grade;
            try {
                grade = Double.parseDouble(gradeStr);
            } catch (NumberFormatException e) {
                logWriter.println("Row " + rowNumber + ": FAILED - Invalid grade value: " + gradeStr);
                return false;
            }

            if (grade < 0 || grade > 100) {
                logWriter.println("Row " + rowNumber + ": FAILED - Grade out of range (0-100): " + grade);
                return false;
            }

            // Validate subject type with regex
            if (!SUBJECT_TYPE_PATTERN.matcher(subjectType).matches()) {
                logWriter.println("Row " + rowNumber + ": FAILED - Invalid subject type");
                logWriter.println("  Input: " + subjectType);
                logWriter.println("  Expected: 'Core' or 'Elective' (case-insensitive)");
                return false;
            }

            // Validate subject name
            if (subjectName == null || subjectName.trim().isEmpty()) {
                logWriter.println("Row " + rowNumber + ": FAILED - Subject name is empty");
                return false;
            }

            // Create subject object
            Subject subject;
            if (subjectType.equalsIgnoreCase("Core")) {
                subject = new CoreSubject(subjectName, getSubjectCode(subjectName));
            } else { // Elective
                subject = new ElectiveSubject(subjectName, getSubjectCode(subjectName));
            }

            // Create and add grade
            Grade newGrade = new Grade(gradeId, studentId, subject, grade);
            gradeManager.addGrade(newGrade);

            logWriter.println("Row " + rowNumber + ": SUCCESS - " +
                    (hasGradeId ? gradeId + ", " : "(auto) " + gradeId + ", ") +
                    studentId + ", " + subjectName + " (" + subjectType + "), " + grade + "%");
            return true;

        } catch (Exception e) {
            logWriter.println("Row " + rowNumber + ": FAILED - Unexpected error: " + e.getMessage());
            e.printStackTrace(logWriter);
            return false;
        }
    }

    private String generateGradeId() {
        // Generate a unique grade ID: GRD + timestamp + random number
        String timestamp = new SimpleDateFormat("HHmmssSSS").format(new Date());
        int random = new Random().nextInt(1000);
        return String.format("GRD%s%03d", timestamp, random);
    }

    // Helper method to parse CSV line with quoted fields
    private List<String> parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean lastCharWasQuote = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (!inQuotes) {
                    inQuotes = true;
                } else {
                    // Check if this is a double quote (escaped quote)
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++; // Skip next quote
                    } else {
                        inQuotes = false;
                    }
                }
                lastCharWasQuote = true;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
                lastCharWasQuote = false;
            } else {
                current.append(c);
                lastCharWasQuote = false;
            }
        }

        // Add the last field
        result.add(current.toString().trim());

        // Remove surrounding quotes from each field
        for (int i = 0; i < result.size(); i++) {
            String field = result.get(i);
            if (field.startsWith("\"") && field.endsWith("\"")) {
                result.set(i, field.substring(1, field.length() - 1));
            }
        }

        return result;
    }

    private String getSubjectCode(String subjectName) {
        if (subjectName == null || subjectName.isEmpty()) {
            return "GEN";
        }

        String lowerName = subjectName.toLowerCase();
        switch (lowerName) {
            case "mathematics":
            case "math":
            case "maths":
                return "MATH";
            case "english":
            case "eng":
                return "ENG";
            case "science":
            case "sci":
                return "SCI";
            case "music":
            case "mus":
                return "MUS";
            case "art":
            case "arts":
                return "ART";
            case "physical education":
            case "pe":
            case "physical ed":
                return "PE";
            case "physics":
            case "phy":
                return "PHY";
            case "chemistry":
            case "chem":
                return "CHEM";
            case "biology":
            case "bio":
                return "BIO";
            case "history":
            case "hist":
                return "HIST";
            case "geography":
            case "geo":
                return "GEO";
            case "computer science":
            case "cs":
            case "comp sci":
                return "COMP";
            case "literature":
            case "lit":
                return "LIT";
            default:
                // Take first 3-4 uppercase characters
                String cleaned = subjectName.replaceAll("[^a-zA-Z]", "").toUpperCase();
                return cleaned.length() >= 3 ? cleaned.substring(0, Math.min(4, cleaned.length())) : "GEN";
        }
    }

    // Create multiple sample CSV files for testing
    public void createSampleCSV() {
        String[] filenames = {
                "sample_grades_simple.csv",
                "sample_grades_with_header.csv",
                "sample_grades_with_gradeid.csv"
        };

        String[][] samples = {
                {  // Simple format (no header, no gradeId)
                        "STU001,Mathematics,Core,85.5",
                        "STU002,English,Core,92.0",
                        "STU001,Music,Elective,78.5",
                        "STU003,Science,Core,88.0",
                        "STU002,Art,Elective,95.5",
                        "STU004,Physics,Core,82.0",
                        "STU005,Biology,Core,91.5",
                        "STU003,History,Elective,76.0"
                },
                {  // With header (no gradeId)
                        "StudentID,SubjectName,SubjectType,Grade",
                        "STU001,Mathematics,Core,85.5",
                        "STU002,English,Core,92.0",
                        "STU001,Music,Elective,78.5",
                        "STU003,Science,Core,88.0",
                        "STU002,Art,Elective,95.5"
                },
                {  // With gradeId and header
                        "GradeID,StudentID,SubjectName,SubjectType,Grade",
                        "GRD001,STU001,Mathematics,Core,85.5",
                        "GRD002,STU002,English,Core,92.0",
                        "GRD003,STU001,Music,Elective,78.5",
                        "GRD004,STU003,Science,Core,88.0",
                        "GRD005,STU002,Art,Elective,95.5"
                }
        };

        Path importsDir = Paths.get("imports");
        try {
            if (!Files.exists(importsDir)) {
                Files.createDirectories(importsDir);
            }

            for (int i = 0; i < filenames.length; i++) {
                Path sampleFile = importsDir.resolve(filenames[i]);
                String content = String.join("\n", samples[i]);
                Files.write(sampleFile, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                System.out.println("✓ Created: imports/" + filenames[i]);
            }

            System.out.println("\n📁 Sample CSV files created in imports/ directory:");
            System.out.println("  • sample_grades_simple.csv - Simple format (no header)");
            System.out.println("  • sample_grades_with_header.csv - With header row");
            System.out.println("  • sample_grades_with_gradeid.csv - With GradeID column");
            System.out.println("\n💡 Use any of these files for testing bulk imports!");

        } catch (IOException e) {
            System.out.println("✗ Could not create sample files: " + e.getMessage());
        }
    }

    /**
     * List available CSV files in imports directory
     */
    public void listAvailableFiles() {
        Path importsDir = Paths.get("imports");
        try {
            if (!Files.exists(importsDir)) {
                System.out.println("📁 imports/ directory does not exist.");
                return;
            }

            System.out.println("\n📁 Available CSV files in imports/ directory:");
            try (Stream<Path> list = Files.list(importsDir)) {
                List<Path> csvFiles = list
                        .filter(p -> p.toString().toLowerCase().endsWith(".csv"))
                        .sorted()
                        .collect(Collectors.toList());

                if (csvFiles.isEmpty()) {
                    System.out.println("  (No CSV files found)");
                } else {
                    csvFiles.forEach(p -> {
                        try {
                            long size = Files.size(p);
                            System.out.printf("  • %-30s (%,d bytes)%n", p.getFileName(), size);
                        } catch (IOException e) {
                            System.out.printf("  • %s%n", p.getFileName());
                        }
                    });
                }
            }
        } catch (IOException e) {
            System.out.println("✗ Cannot list directory: " + e.getMessage());
        }
    }
}