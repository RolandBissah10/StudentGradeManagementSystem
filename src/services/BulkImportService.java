package services;

import exceptions.InvalidFileFormatException;
import exceptions.StudentNotFoundException;
import exceptions.InvalidGradeException;
import models.*;
import utils.ValidationUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class BulkImportService {
    private StudentManager studentManager;
    private GradeManager gradeManager;

    public BulkImportService(StudentManager studentManager, GradeManager gradeManager) {
        this.studentManager = studentManager;
        this.gradeManager = gradeManager;
    }

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
            return;
        }

        System.out.println("\n=== BULK IMPORT STARTED ===");
        System.out.println("File: " + filePath.getFileName());
        System.out.println("Method: NIO.2 Streaming");
        System.out.println("Processing...");

        try (Stream<String> lines = Files.lines(filePath);
             PrintWriter logWriter = new PrintWriter(Files.newBufferedWriter(
                     Paths.get("imports", logFilename)))) {

            logWriter.println("BULK IMPORT LOG");
            logWriter.println("Timestamp: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            logWriter.println("Source File: " + filePath.getFileName());
            logWriter.println("=========================================");
            logWriter.println();

            // Skip header and process each line
            lines.skip(1) // Skip header row
                    .forEach(line -> {
                        int currentRow = totalRows.incrementAndGet();
                        boolean success = processCSVRow(line, logWriter, currentRow);
                        if (success) {
                            successfulImports.incrementAndGet();
                        } else {
                            failedImports.incrementAndGet();
                        }
                    });

            logWriter.println();
            logWriter.println("=== IMPORT SUMMARY ===");
            logWriter.println("Total Rows Processed: " + totalRows.get());
            logWriter.println("Successfully Imported: " + successfulImports.get());
            logWriter.println("Failed: " + failedImports.get());
            logWriter.println("Success Rate: " +
                    (totalRows.get() > 0 ? String.format("%.1f%%", (successfulImports.get() * 100.0 / totalRows.get())) : "0%"));
            logWriter.println("Completed: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

            // Display summary to user
            System.out.println("\n=== IMPORT COMPLETED ===");
            System.out.println("Total Rows: " + totalRows.get());
            System.out.println("Successfully Imported: " + successfulImports.get());
            System.out.println("Failed: " + failedImports.get());

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
            System.out.println("✓ " + successfulImports.get() + " grades added to system");

        } catch (IOException e) {
            System.out.println("✗ ERROR reading file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean processCSVRow(String line, PrintWriter logWriter, int rowNumber) {
        // Clean the line
        line = line.trim();
        if (line.isEmpty()) {
            logWriter.println("Row " + rowNumber + ": SKIPPED - Empty line");
            return false;
        }

        // Handle quoted CSV fields
        List<String> parts = parseCSVLine(line);

        if (parts.size() != 4) {
            logWriter.println("Row " + rowNumber + ": FAILED - Expected 4 columns, found " + parts.size());
            logWriter.println("  Line: " + line);
            return false;
        }

        String studentId = parts.get(0).trim();
        String subjectName = parts.get(1).trim();
        String subjectType = parts.get(2).trim();
        String gradeStr = parts.get(3).trim();

        try {
            // Validate student ID
            if (studentId == null || studentId.isEmpty()) {
                logWriter.println("Row " + rowNumber + ": FAILED - Student ID is empty");
                return false;
            }

            // Check student exists
            Student student = studentManager.findStudent(studentId);
            if (student == null) {
                logWriter.println("Row " + rowNumber + ": FAILED - Student not found: " + studentId);
                logWriter.println("  Available students: " +
                        studentManager.getStudents().stream()
                                .map(Student::getStudentId)
                                .collect(java.util.stream.Collectors.joining(", ")));
                return false;
            }

            // Validate grade
            double grade;
            try {
                grade = Double.parseDouble(gradeStr);
            } catch (NumberFormatException e) {
                logWriter.println("Row " + rowNumber + ": FAILED - Invalid grade format: " + gradeStr);
                return false;
            }

            if (grade < 0 || grade > 100) {
                logWriter.println("Row " + rowNumber + ": FAILED - Grade out of range (0-100): " + grade);
                return false;
            }

            // Validate subject type
            Subject subject;
            if (subjectType.equalsIgnoreCase("Core")) {
                subject = new CoreSubject(subjectName, getSubjectCode(subjectName));
            } else if (subjectType.equalsIgnoreCase("Elective")) {
                subject = new ElectiveSubject(subjectName, getSubjectCode(subjectName));
            } else {
                logWriter.println("Row " + rowNumber + ": FAILED - Invalid subject type: " + subjectType);
                logWriter.println("  Must be 'Core' or 'Elective'");
                return false;
            }

            // Create and add grade
            Grade newGrade = new Grade(studentId, subject, grade);
            gradeManager.addGrade(newGrade);

            logWriter.println("Row " + rowNumber + ": SUCCESS - " +
                    studentId + ", " + subjectName + " (" + subjectType + "), " + grade + "%");
            return true;

        } catch (Exception e) {
            logWriter.println("Row " + rowNumber + ": FAILED - Unexpected error: " + e.getMessage());
            e.printStackTrace(logWriter);
            return false;
        }
    }

    // Helper method to parse CSV line with quoted fields
    private List<String> parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

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

        return result;
    }

    private String getSubjectCode(String subjectName) {
        if (subjectName == null || subjectName.isEmpty()) {
            return "GEN";
        }

        switch (subjectName.toLowerCase()) {
            case "mathematics": return "MATH";
            case "math": return "MATH";
            case "english": return "ENG";
            case "science": return "SCI";
            case "music": return "MUS";
            case "art": return "ART";
            case "physical education": return "PE";
            case "pe": return "PE";
            case "physics": return "PHY";
            case "chemistry": return "CHEM";
            case "biology": return "BIO";
            case "history": return "HIST";
            case "geography": return "GEO";
            default:
                // Take first 3-4 characters
                return subjectName.substring(0, Math.min(4, subjectName.length())).toUpperCase();
        }
    }

    // Test method to create a sample CSV file
    public void createSampleCSV() {
        Path sampleFile = Paths.get("imports", "sample_grades.csv");
        try {
            if (!Files.exists(sampleFile.getParent())) {
                Files.createDirectories(sampleFile.getParent());
            }

            String sampleContent = "studentId,subjectName,subjectType,grade\n" +
                    "STU001,Mathematics,Core,85.5\n" +
                    "STU002,English,Core,92.0\n" +
                    "STU001,Music,Elective,78.5\n" +
                    "STU003,Science,Core,88.0\n" +
                    "STU002,Art,Elective,95.5\n";

            Files.write(sampleFile, sampleContent.getBytes());
            System.out.println("✓ Sample CSV created: imports/sample_grades.csv");
            System.out.println("  You can use this file for testing imports");

        } catch (IOException e) {
            System.out.println("✗ Could not create sample file: " + e.getMessage());
        }
    }
}